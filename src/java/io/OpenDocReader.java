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

import com.dickimawbooks.texparserlib.latex.datatool.CsvBlankOption;
import com.dickimawbooks.texparserlib.latex.datatool.DatumType;

import com.dickimawbooks.texjavahelplib.MessageSystem;

import com.dickimawbooks.datatooltk.*;

/**
 * Class for reading ODS and FODS files. This only picks out the
 * data pertinent to datatooltk. The principle elements of interest
 * are "config:config-item-map-named" with the "config:name"
 * attribute set to "Tables". Within this element are the available
 * table names provided the "config:config-item-map-entry" in 
 * the "config:name" attribute. If the user hasn't already supplied
 * a table reference (either by name or by index) and there is more
 * that one table identified in this part of the XML, then the user
 * will be prompted to selected the preferred table, if GUI is
 * supported. Otherwise the first table is assumed. The config part
 * is therefore useful as a prompt for the user but is not essential
 * if the user knows the table name beforehand.
 *
 * The part that is of the most interest is the "table:table"
 * element. The "table:name" attribute identifies the name.
 * It's at this point that the user will be prompted for a table
 * reference if not already provided.
 * 
 * The "table:table-column" elements are used to estimate the total
 * number of columns. This can sometimes lead to a blank final
 * column, so if there's no header for the final column, it will be
 * omitted. Any row that has an element for that column will
 * automatically trigger the creation of the column.
 *
 * The "table:table-row" element provides the data for a row. With
 * each cell in the "table:table-cell" element. The cell element
 * attributes "office:value-type", "office:value" and "office:currency"
 * are used to map to the closest matching datatool.sty datum
 * information.
 *
 * The contents of the cell are contained within child elements.
 * Most of the styling is ignored. The "text:p" element will
 * automatically add <code>\\DTLpar</code>, except at the start.
 * The "text:numbered-paragraph" element will be mapped to the
 * enumerate environment and "text:list" will be mapped to the
 * itemize environment. The is no style check to determine whether
 * or not "text:list" represents a numbered or unnumbered list.
 * Both "text:list-header" and "text:list-item" will automatically
 * insert <code>\\item</code>. There is no attempt to convert any
 * other type of element.
 */
public class OpenDocReader extends XMLReaderAdapter
{
   protected OpenDocReader(ImportSettings importSettings)
   throws SAXException,IOException
   {
      super();
      this.importSettings = importSettings;
      this.checkForVerbatim = importSettings.isCheckForVerbatimOn();

      messageSystem = importSettings.getMessageHandler().getHelpLib()
                    .getMessageSystem();

      sheet = importSettings.getSheetRef();
      startLine = Math.max(0, importSettings.getSkipLines()) + 1;

      listener = importSettings.getTeXParserListener();
   }

   public void parseFlat(InputSource in)
   throws SAXException,IOException
   {
      parseSettings = true;
      parseData = true;

      try
      {
         parse(in);
      }
      catch (ParsingTerminatedException e)
      {
      }
   }

   public void parseSettings(InputSource in)
   throws SAXException,IOException
   {
      parseSettings = true;
      parseData = false;

      try
      {
         parse(in);
      }
      catch (ParsingTerminatedException e)
      {
      }
   }

   public void parseData(InputSource in)
   throws SAXException,IOException
   {
      parseSettings = false;
      parseData = true;

      try
      {
         parse(in);
      }
      catch (ParsingTerminatedException e)
      {
      }
   }

   @Override
   public void startElement(String uri, String localName, String qName,
     Attributes attrs)
   throws SAXException
   {
      super.startElement(uri, localName, qName, attrs);

      if (parseSettings)
      {
         if (inSettings)
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
      }

      if (parseData && !inSettings)
      {
         if ("table:table".equals(qName))
         {
            String name = attrs.getValue("table:name");

            if (sheet == null)
            {
               if (tableNames == null || tableNames.size() < 2
                    || importSettings.isBatchMode())
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

            tableCount++;
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

               if (db != null)
               {
                  if (lineCount >= startLine)
                  {
                     currentRow = new DatatoolRow(db, columnCount);
                  }
               }
               else
               {
                  db = new DatatoolDb(importSettings.getSettings(), columnCount);

                  String dblabel = "";

                  if (sheet != null && !(sheet instanceof Number))
                  {
                     dblabel = sheet.toString();
                     Matcher m = DatatoolDb.INVALID_LABEL_CONTENT.matcher(dblabel);
                     dblabel = m.replaceAll("");

                     if (importSettings.isTrimLabelsOn())
                     {
                        dblabel = dblabel.trim();
                     }
                  }

                  if (dblabel.isEmpty())
                  {
                     dblabel = messageSystem.getMessageWithFallback(
                      "default.untitled", "Untitled");
                  }

                  db.setName(dblabel);

                  if (importSettings.hasHeaderRow())
                  {
                     inHeader = true;
                  }
                  else
                  {
                     int n = Math.max(importSettings.getColumnKeyCount(),
                                      importSettings.getColumnHeaderCount());

                     if (n > 0)
                     {
                        columnCount = n;
                     }

                     for (int i = 1; i <= columnCount; i++)
                     {
                        String key = importSettings.getColumnKey(i);
                        String originalKey = key;

                        if (key != null)
                        {
                           Matcher m = DatatoolDb.INVALID_LABEL_CONTENT.matcher(key);

                           key = m.replaceAll("");

                           if (importSettings.isTrimLabelsOn())
                           {
                              key = key.trim();
                           }
                        }

                        if (key == null || key.isEmpty())
                        {
                           key = messageSystem.getMessageWithFallback(
                            "default.field", "Field{0,number}", i);
                        }

                        String title = importSettings.getColumnHeader(i);

                        if (title == null || title.isEmpty())
                        {
                           title = originalKey;
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
                     importSettings.getMessageHandler().debug(e);
                     datumType = DatumType.STRING;
                  }
               }

               currentCell = new Datum(datumType, 
                 datumType == DatumType.UNKNOWN ? DatatoolDb.NULL_VALUE : "",
                 currency, num, importSettings.getSettings());

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
                  importSettings.getMessageHandler().debug(e);
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
   }

   @Override
   public void endElement(String uri, String localName, String qName)
    throws SAXException
   {
      super.endElement(uri, localName, qName);

      if (parseSettings)
      {
         if ("office:settings".equals(qName))
         {
            inSettings = false;
            parseSettings = false;

            if (!parseData)
            {
               throw new ParsingTerminatedException();
            }
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
      }

      if (parseData && inTable)
      {
         if ("table:table".equals(qName))
         {
            throw new ParsingTerminatedException();
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
               switch (importSettings.getBlankRowAction())
               {
                  case IGNORE:
                  break;
                  case EMPTY_ROW:
                    db.appendRow(currentRow);
                  break;
                  case END:
                     throw new ParsingTerminatedException();
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

            if (importSettings.isTrimElementOn())
            {
               text = text.trim();
            }

            if (DatatoolDb.NULL_VALUE.equals(text))
            {
               text = null;

               if (columnIdx == 1 && tableCellRepetition > columnCount)
               {
                  // blank row

                  if (importSettings.getBlankRowAction() != CsvBlankOption.EMPTY_ROW)
                  {
                     columnIdx += tableCellRepetition;
                     return;
                  }
               }
            }

            if (inHeader)
            {
               int maxProvided = Math.max(importSettings.getColumnKeyCount(),
                                 importSettings.getColumnHeaderCount());

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
                     String key = importSettings.getColumnKey(i);

                     if (key == null || key.isEmpty())
                     {
                        if (importSettings.isAutoKeysOn()
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

                     if (importSettings.isTrimLabelsOn())
                     {
                        key = key.trim();
                     }

                     if (key.isEmpty())
                     {
                        key = messageSystem.getMessageWithFallback(
                         "default.field", "Field{0,number}", i);
                     }

                     String title = importSettings.getColumnHeader(i);

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
                      currentCell.getNumber(), importSettings.getSettings());
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

      if (importSettings.isLiteralContent())
      {
         StringBuilder builder = new StringBuilder(Math.max(10, text.length()));

         for (int i = 0; i < text.length(); )
         {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);

            String mapped = importSettings.getTeXMap(cp);

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
      else if (checkForVerbatim && !verbatimFound)
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

   protected ImportSettings importSettings;
   protected MessageSystem messageSystem;
   protected DataToolTeXParserListener listener;
   protected boolean checkForVerbatim;

   protected DatatoolDb db;
   protected boolean parseSettings = true, parseData = true;

   protected boolean inMappedNamed = false,
      readingActiveTable = false, inSettings = false,
      inTable = false, inHeader = false;

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
