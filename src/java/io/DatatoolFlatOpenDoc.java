/*
    Copyright (C) 2024 Nicola L.C. Talbot
    www.dickimaw-books.com

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package com.dickimawbooks.datatooltk.io;

import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.*;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderAdapter;

import com.dickimawbooks.texparserlib.TeXObject;
import com.dickimawbooks.texparserlib.TeXSyntaxException;

import com.dickimawbooks.texparserlib.latex.datatool.IOSettings;
import com.dickimawbooks.texparserlib.latex.datatool.CsvBlankOption;
import com.dickimawbooks.texparserlib.latex.datatool.DatumType;

import com.dickimawbooks.texjavahelplib.MessageSystem;

import com.dickimawbooks.datatooltk.*;

/**
 * Class handling importing flat Open Document spreadsheets (FODS).
 */
public class DatatoolFlatOpenDoc implements DatatoolImport
{
   public DatatoolFlatOpenDoc(DatatoolSettings settings)
   {
      this.settings = settings;
   }

   public DatatoolDb importData(String source)
      throws DatatoolImportException
   {
      return importData(source, true);
   }

   public DatatoolDb importData(File file)
      throws DatatoolImportException
   {
      return importData(file, true);
   }

   public DatatoolDb importData(String source, boolean checkForVerbatim)
      throws DatatoolImportException
   {
      return importData(new File(source), checkForVerbatim);
   }

   public DatatoolDb importData(File file, boolean checkForVerbatim)
      throws DatatoolImportException
   {
      try
      {
         DataToolTeXParserListener listener = settings.getTeXParserListener();

         listener.applyCurrentCsvSettings();

         IOSettings ioSettings = listener.getIOSettings();

         return importData(ioSettings, file, checkForVerbatim);
      }
      catch (TeXSyntaxException e)
      {
         throw new DatatoolImportException(
           String.format("%s%n%s",
            getMessageHandler().getLabelWithValues(
              "error.import.failed", file),
              e.getMessage(getMessageHandler().getTeXApp())), e);
      }
      catch (IOException e)
      {
         throw new DatatoolImportException(
           getMessageHandler().getLabelWithValues(
             "error.import.failed", file), e);
      }
   }

   @Override
   public DatatoolDb importData(IOSettings ioSettings, String source)
      throws DatatoolImportException
   {
      return importData(ioSettings, new File(source), true);
   }

   public DatatoolDb importData(IOSettings ioSettings, File file,
        boolean checkForVerbatim)
      throws DatatoolImportException
   {
      BufferedReader in = null;
      DatatoolDb db;
      MessageHandler messageHandler = getMessageHandler();

      try
      {
         in = Files.newBufferedReader(file.toPath());
         in.mark(256);

         String line = in.readLine();

         Matcher m = XML_PATTERN.matcher(line);

         if (m.matches())
         {
            String encoding = m.group(1);

            if (!encoding.equals("UTF-8"))
            {
               Charset charset = Charset.forName(encoding);

               if (!charset.equals(StandardCharsets.UTF_8))
               {
                  in.close();
                  in = null;
                  in = Files.newBufferedReader(file.toPath(), charset);
               }
            }
         }

         in.reset();

         FlatOpenDocReader reader = new FlatOpenDocReader(this,
          ioSettings, checkForVerbatim);

         reader.parse(new InputSource(in));

         db = reader.getDataBase();

         if (db == null)
         {
            throw new DatatoolImportException(messageHandler.getLabelWithValues(
              "error.dbload.missing_data", file));
         }

         if (reader.verbatimFound())
         {
            messageHandler.warning(
              messageHandler.getLabel("warning.verb_detected"));
         }
      }
      catch (SAXException e)
      {
         Throwable cause = e.getCause();

         if (cause instanceof UserCancelledException)
         {
            throw new DatatoolImportException(
              messageHandler.getLabelWithValues(
                "error.import.failed", file, cause.getMessage()), cause);
         }
         else
         {
            throw new DatatoolImportException(
              messageHandler.getLabelWithValues(
                "error.import.failed", file, e.getMessage()), e);
         }
      }
      catch (Throwable e)
      {
         throw new DatatoolImportException(
           messageHandler.getLabelWithValues(
             "error.import.failed", file, e.getMessage()), e);
      }
      finally
      {
         if (in != null)
         {
            try
            {
               in.close();
            }
            catch (IOException e)
            {
               messageHandler.warning(e);
            }
         }
      }

      return db;
   }

   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
   }

   public DatatoolSettings getSettings()
   {
      return settings;
   }

   public Object getSheetRef()
   {
      return sheetRef;
   }

   protected Object sheetRef;
   protected DatatoolSettings settings;
   public static final Pattern XML_PATTERN
    = Pattern.compile("<\\?xml.*\\s+encoding\\s*=\\s*\"([^\"]+)\".*\\?>");
}

class FlatOpenDocReader extends XMLReaderAdapter
{
   protected FlatOpenDocReader(DatatoolFlatOpenDoc imp,
      IOSettings ioSettings,
      boolean checkForVerbatim)
   throws SAXException,IOException
   {
      super();
      this.imp = imp;
      this.ioSettings = ioSettings;
      this.checkForVerbatim = checkForVerbatim;

      messageSystem = imp.getMessageHandler().getHelpLib().getMessageSystem();
      sheet = imp.getSheetRef();
      startLine = Math.max(0, ioSettings.getSkipLines()) + 1;

      listener = imp.getSettings().getTeXParserListener();
   }

   @Override
   public void startElement(String uri, String localName, String qName,
     Attributes attrs)
   throws SAXException
   {
      super.startElement(uri, localName, qName, attrs);

      if ("office:document".equals(qName))
      {
         if (db != null)
         {
            throw new SAXException(
              messageSystem.getMessageWithFallback(
              "error.xml.more_than_one_tag", "more than 1 <{0}> found", qName));
         }

         String mimetype = attrs.getValue("office:mimetype");

         if (mimetype != null
          && !mimetype.equals("application/vnd.oasis.opendocument.spreadsheet"))
         {
            throw new SAXException(
              messageSystem.getMessageWithFallback(
              "error.expected_mimetype_but_found",
              "Expected mimetype ''{0}'' but found ''{1}''",
              "application/vnd.oasis.opendocument.spreadsheet",
              mimetype));
         }

      }
      else if (finished)
      {
         throw new SAXException(
           messageSystem.getMessageWithFallback(
           "error.xml.tag_found_outside",
           "<{0}> found outside <{1}>",
           qName, "office:document"));

      }
      else if (dataFound)
      {
         // do nothing
      }
      else if (inSettings)
      {
         if ("config:config-item-map-named".equals(qName))
         {
            String type = attrs.getValue("config:name");

            if ("Tables".equals(type))
            {
               inMappedNamed = true;
               tableNames = new Vector<String>();
            }
         }
         else if (inMappedNamed && "config:config-item-map-entry".equals(qName))
         {
            String name = attrs.getValue("config:name");

            if (name != null)
            {
               tableNames.add(name);
            }
         }
         else if (sheet == null && "config:config-item".equals(qName))
         {
            String name = attrs.getValue("config:name");

            if ("ActiveTable".equals(name)
                 && "string".equals(attrs.getValue("config:type")))
            {
               readingActiveTable = true;
               currentBuilder = new StringBuilder();
            }
         }
      }
      else if ("office:settings".equals(qName))
      {
         inSettings = true;
      }
      else if (!dataFound && "table:table".equals(qName))
      {
         tableCount++;

         String name = attrs.getValue("table:name");

         if (sheet == null)
         {
            if (tableNames == null || tableNames.size() < 2
                 || imp.getMessageHandler().isBatchMode())
            {
               inTable = true;

               if (name != null)
               {
                  sheet = name;
               }
            }
            else
            {
               sheet = JOptionPane.showInputDialog(null,
                  messageSystem.getMessage("importspread.sheet"),
                  messageSystem.getMessage("importspread.title"),
                  JOptionPane.PLAIN_MESSAGE,
                  null, tableNames.toArray(), activeTable);

               if (sheet == null)
               {
                  throw new SAXException(new UserCancelledException(messageSystem));
               }

               if (sheet.toString().equals(name))
               {
                  inTable = true;
               }
            }
         }
         else if (sheet instanceof Number)
         {
            if (tableCount == ((Number)sheet).intValue())
            {
               inTable = true;

               if (name != null)
               {
                  sheet = name;
               }
            }
         }
         else if (sheet.toString().equals(name))
         {
            inTable = true;
         }
      }
      else if (inTable)
      {
         if ("table:table-column".equals(qName))
         {
            columnCount++;
         }
         else if ("table:table-row".equals(qName))
         {
            lineCount++;
            columnIdx=0;

            if (lineCount < startLine)
            {
               // do nothing
            }
            else if (db != null)
            {
               currentRow = new DatatoolRow(db, columnCount);
            }
            else
            {
               db = new DatatoolDb(imp.getSettings(), columnCount);

               if (sheet != null)
               {
                  String key = sheet.toString();
                  Matcher m = DatatoolDb.INVALID_LABEL_CONTENT.matcher(key);

                  key = m.replaceAll("");

                  if (key.isEmpty())
                  {
                     key = messageSystem.getMessageWithFallback(
                      "default.untitled", "Untitled");
                  }

                  db.setName(key);
               }

               if (ioSettings.isHeaderIncluded())
               {
                  inHeader = true;
               }
               else
               {
                  int n = Math.max(ioSettings.getColumnKeyCount(),
                                   ioSettings.getColumnHeaderCount());

                  if (n > 0)
                  {
                     columnCount = n;
                  }

                  for (int i = 1; i <= columnCount; i++)
                  {
                     String key = ioSettings.getColumnKey(i);
                     String title = key;

                     if (key != null)
                     {
                        Matcher m = DatatoolDb.INVALID_LABEL_CONTENT.matcher(key);

                        key = m.replaceAll("");
                     }

                     if (key == null || key.isEmpty())
                     {
                        key = messageSystem.getMessageWithFallback(
                         "default.field", "Field{0,number}", i);
                     }

                     TeXObject header = ioSettings.getColumnHeader(i);

                     if (header != null)
                     {
                        title = header.toString(listener.getParser());
                     }

                     if (title == null || title.isEmpty())
                     {
                        title = key;
                     }

                     db.addColumn(new DatatoolHeader(db, key, title));
                  }

                  currentRow = new DatatoolRow(db, columnCount);
               }
            }
         }
         else if ("table:table-cell".equals(qName))
         {
            columnIdx++;

            String type = attrs.getValue("office:value-type");
            Number num = null;
            String strVal = attrs.getValue("office:value");
            String currency = attrs.getValue("office:currency");

            DatumType datumType = DatumType.UNKNOWN;

            if ("string".equals(type))
            {
               datumType = DatumType.STRING;
            }
            else if ("int".equals(type))
            {
               datumType = DatumType.INTEGER;
            }
            else if ("float".equals(type))
            {
               datumType = DatumType.DECIMAL;
            }
            else if ("currency".equals(type))
            {
               datumType = DatumType.CURRENCY;

               if (currency == null)
               {
                  currency = "\\DTLCurrencySymbol";
               }
               else
               {
                  currency = "\\DTLcurr{" + currency + "}";
               }
            }

            if (strVal != null)
            {
               try
               {
                  switch (datumType)
                  {
                     case CURRENCY:
                     case DECIMAL:
                        num = Double.valueOf(strVal);
                     break;
                     case INTEGER:
                        num = Integer.valueOf(strVal);
                     break;
                  }
               }
               catch (NumberFormatException e)
               {
                  imp.getMessageHandler().debug(e);
                  datumType = DatumType.STRING;
               }
            }

            currentCell = new Datum(datumType, 
              datumType == DatumType.UNKNOWN ? DatatoolDb.NULL_VALUE : "",
              currency, num, imp.getSettings());

            tableCellRepetition = 1;

            try
            {
               String val = attrs.getValue("table:number-columns-repeated");

               if (val != null)
               {
                  tableCellRepetition = Integer.parseInt(val);
               }
            }
            catch (NumberFormatException e)
            {
               imp.getMessageHandler().debug(e);
            }
         }
         else if (currentCell != null)
         {
            if ("text:list".equals(qName))
            {
               startEnvironment(qName, "itemize");
            }
            else if ("text:numbered-paragraph".equals(qName))
            {
               startEnvironment(qName, "enumerate");
            }
            else if ("text:list-header".equals(qName)
                  || "text:list-item".equals(qName))
            {
               addControlSequence("item");
            }
            else if ("text:p".equals(qName))
            {
               String text = currentCell.getText();

               if (!(text.equals(DatatoolDb.NULL_VALUE) || text.isEmpty()))
               {
                  addControlSequence("DTLpar");
               }
            }

            if (qName.startsWith("text:") && currentBuilder == null)
            {
               currentBuilder = new StringBuilder();
            }

         }
      }
   }

   @Override
   public void endElement(String uri, String localName, String qName)
    throws SAXException
   {
      super.endElement(uri, localName, qName);

      if ("office:document".equals(qName))
      {
         finished = true;
      }
      else if ("office:settings".equals(qName))
      {
         inSettings = false;
      }
      else if (inSettings)
      {
         if ("config:config-item-map-named".equals(qName))
         {
            inMappedNamed = false;
         }
         else if ("config:config-item".equals(qName))
         {
            if (readingActiveTable)
            {
               if (currentBuilder != null)
               {
                  activeTable = currentBuilder.toString();
                  currentBuilder = null;
               }

               readingActiveTable = false;
            }
         }
      }
      else if (inTable)
      {
         if ("table:table".equals(qName))
         {
            inTable = false;
            dataFound = true;
         }
         else if ("table:table-row".equals(qName))
         {
            if (inHeader && db.getColumnCount() > 0)
            {
               inHeader = false;
            }
            else if (currentRow == null)
            {// do nothing
            }
            else if (currentRow.isEmpty())
            {
               switch (ioSettings.getCsvBlankOption())
               {
                  case IGNORE:
                  break;
                  case END:
                     dataFound = true;
                  break;
                  case EMPTY_ROW:
                    db.appendRow(currentRow);
                  break;
               }
            }
            else
            {
               db.appendRow(currentRow);
            }

            currentRow = null;
         }
         else if (currentCell != null && "table:table-cell".equals(qName))
         {
            // end of cell

            updateCurrentCell();

            String text = currentCell.getText();

            if (ioSettings.isTrimElementOn())
            {
               text = text.trim();
            }

            if (DatatoolDb.NULL_VALUE.equals(text))
            {
               text = null;

               if (columnIdx == 1 && tableCellRepetition > columnCount)
               {
                  // blank row

                  if (ioSettings.getCsvBlankOption() != CsvBlankOption.EMPTY_ROW)
                  {
                     columnIdx += tableCellRepetition;
                     return;
                  }
               }
            }

            if (inHeader)
            {
               int maxProvided = Math.max(ioSettings.getColumnKeyCount(),
                                 ioSettings.getColumnHeaderCount());

               if (columnIdx == columnCount && columnCount > maxProvided
                   && (text == null || text.isEmpty()))
               {
                  columnCount--;
               }
               else
               {
                  for (int i = columnIdx, 
                       n = Math.min(columnCount, columnIdx+tableCellRepetition);
                       i < n; i++)
                  {
                     String key = ioSettings.getColumnKey(i);
                     String title = text;

                     if (key == null || key.isEmpty())
                     {
                        if (ioSettings.isAutoKeysOn()
                            || text == null || text.isEmpty()
                            || db.getColumnIndex(text) != -1)
                        {
                           key = messageSystem.getMessageWithFallback(
                            "default.field", "Field{0,number}", i);
                        }
                        else
                        {
                           key = text;
                        }
                     }

                     Matcher m = DatatoolDb.INVALID_LABEL_CONTENT.matcher(key);

                     key = m.replaceAll("");

                     if (key.isEmpty())
                     {
                        key = messageSystem.getMessageWithFallback(
                         "default.field", "Field{0,number}", i);
                     }

                     TeXObject header = ioSettings.getColumnHeader(i);

                     if (header != null)
                     {
                        title = header.toString(listener.getParser());
                     }

                     if (title == null || title.isEmpty())
                     {
                        title = text;
                     }

                     db.addColumn(new DatatoolHeader(db, key, title));
                  }
               }
            }
            else
            {
               boolean repeated = false;

               for (int i = columnIdx, 
                    n = Math.min(columnCount, columnIdx+tableCellRepetition);
                    i < n; i++)
               {
                  if (repeated)
                  {
                     currentCell = new Datum(currentCell.getDatumType(),
                      currentCell.getText(), currentCell.getCurrencySymbol(),
                      currentCell.getNumber(), imp.getSettings());
                  }

                  currentRow.add(currentCell);
                  repeated = true;
               }
            }

            currentCell = null;
            currentBuilder = null;
         }
         else if (currentCell != null && currentBuilder != null)
         {
            updateCurrentCell();

            if (qName.equals(currentEnvironment)
                 && cellEnvironments != null
                 && !cellEnvironments.isEmpty())
            {
               String text = currentCell.getText();

               String env = cellEnvironments.remove(cellEnvironments.size()-1);
               text += "\\end{" + env + "}";
               currentEnvironment = null;

               currentCell.setText(text);
            }
         }
      }
   }

   protected void startEnvironment(String qName, String env)
   {
      updateCurrentCell();

      if (cellEnvironments == null)
      {
         cellEnvironments = new Vector<String>();
      }

      currentEnvironment = qName;
      cellEnvironments.add(env);

      String text = currentCell.getText();

      if (text.equals(DatatoolDb.NULL_VALUE) || text.isEmpty())
      {
         text = "";
      }

      text += "\\begin{"+env+"}";

      currentCell.setText(text);

      if (currentCell.getDatumType() == DatumType.UNKNOWN)
      {
         currentCell.setDatumType(DatumType.STRING);
      }
   }

   protected void addControlSequence(String csname)
   {
      updateCurrentCell();

      String text = currentCell.getText();

      if (text.equals(DatatoolDb.NULL_VALUE) || text.isEmpty())
      {
         text = "";
      }

      text += "\\"+csname+" ";

      currentCell.setText(text);

      if (currentCell.getDatumType() == DatumType.UNKNOWN)
      {
         currentCell.setDatumType(DatumType.STRING);
      }
   }

   protected void updateCurrentCell()
   {
      if (currentBuilder != null && currentBuilder.length() > 0)
      {
         String text = currentCell.getText();

         if (text.equals(DatatoolDb.NULL_VALUE) || text.isEmpty())
         {
            text = "";
         }

         text += processText(currentBuilder.toString());

         currentBuilder.setLength(0);

         currentCell.setText(text);

         if (currentCell.getDatumType() == DatumType.UNKNOWN
             && !text.equals(DatatoolDb.NULL_VALUE))
         {
            currentCell.setDatumType(DatumType.STRING);
         }
      }
   }

   protected String processText(CharSequence charSeq)
   {
      String text = charSeq.toString();

      DatatoolSettings settings = imp.getSettings();

      if (ioSettings.isCsvLiteral())
      {
         StringBuilder builder = new StringBuilder(Math.max(10, text.length()));

         for (int i = 0; i < text.length(); )
         {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);

            String mapped = settings.getTeXMap(cp);

            if (mapped == null)
            {
               builder.appendCodePoint(cp);
            }
            else
            {
               builder.append(mapped);
            }
         }

         text = builder.toString();
      }

      if (checkForVerbatim && !verbatimFound)
      {
         verbatimFound = DatatoolDb.checkForVerbatim(text);
      }

      return text;
   }

   @Override
   public void characters(char[] ch, int start, int length)
    throws SAXException
   {
      super.characters(ch, start, length);

      if (currentBuilder != null)
      {
         currentBuilder.append(ch, start, length);
      }
   }

   public DatatoolDb getDataBase()
   {
      return db;
   }

   public Object getSheetRef()
   {
      return sheet;
   }

   public boolean verbatimFound()
   {
      return verbatimFound;
   }

   protected IOSettings ioSettings;
   protected boolean checkForVerbatim;
   protected DatatoolFlatOpenDoc imp;
   protected MessageSystem messageSystem;
   protected DataToolTeXParserListener listener;

   protected DatatoolDb db;
   protected boolean finished = false, inMappedNamed = false,
      readingActiveTable = false, inSettings = false,
      inTable = false, dataFound = false,
      inHeader = false;

   protected StringBuilder currentBuilder;

   protected int startLine;
   protected Object sheet;
   protected String activeTable;
   protected Vector<String> tableNames;
   protected int tableCount=0, lineCount=0, columnCount=0, columnIdx=0;
   protected DatatoolRow currentRow;
   protected Datum currentCell;
   protected int tableCellRepetition;
   protected Vector<String> cellEnvironments;
   protected String currentEnvironment;

   protected boolean verbatimFound = false;
}
