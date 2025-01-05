/*
    Copyright (C) 2024-2025 Nicola L.C. Talbot
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.NumberFormat;

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
import com.dickimawbooks.texparserlib.latex.datatool.Julian;

import com.dickimawbooks.texjavahelplib.MessageSystem;

import com.dickimawbooks.datatooltk.*;

/**
 * Class for reading Open Office XML files. 
 * As with OpenDocReader this only picks out the data of interest to
 * datatooltk rather than parsing everything.
 */
public class OfficeOpenReader extends XMLReaderAdapter
{
   protected OfficeOpenReader(ImportSettings importSettings)
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

   public void parseWorkBook(InputSource in)
   throws SAXException,IOException
   {
      parsingWorkbook = true;
      parsingRelationships = false;
      parsingStrings = false;
      parsingStyles = false;
      parsingData = false;

      try
      {
         parse(in);
      }
      catch (ParsingTerminatedException e)
      {
      }
   }

   public void parseRelationships(InputSource in)
   throws SAXException,IOException
   {
      parsingWorkbook = false;
      parsingRelationships = true;
      parsingStrings = false;
      parsingStyles = false;
      parsingData = false;

      try
      {
         parse(in);
      }
      catch (ParsingTerminatedException e)
      {
      }
   }

   public void parseStyles(InputSource in)
   throws SAXException,IOException
   {
      parsingWorkbook = false;
      parsingRelationships = false;
      parsingStrings = false;
      parsingStyles = true;
      parsingData = false;

      try
      {
         parse(in);
      }
      catch (ParsingTerminatedException e)
      {
      }
   }

   public void parseSharedStrings(InputSource in)
   throws SAXException,IOException
   {
      parsingWorkbook = false;
      parsingRelationships = false;
      parsingStrings = true;
      parsingStyles = false;
      parsingData = false;

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
      parsingWorkbook = false;
      parsingRelationships = false;
      parsingStrings = false;
      parsingStyles = false;
      parsingData = true;

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

      if (parsingWorkbook)
      {
         if (qName.equals("workbookView"))
         {
            String value = attrs.getValue("firstSheet");

            if (value != null)
            {
               try
               {
                  firstSheetIdx = Integer.parseInt(value);
               }
               catch (NumberFormatException e)
               {
               }
            }

            value = attrs.getValue("activeTab");

            if (value != null)
            {
               try
               {
                  activeTabIdx = Integer.parseInt(value);
               }
               catch (NumberFormatException e)
               {
               }
            }
         }
         else if (qName.equals("sheets"))
         {
            sheetRefs = new HashMap<String,WorkSheetRef>();
         }
         else if (qName.equals("sheet"))
         {
            if (sheetRefs == null)
            {
               throw new SAXException(
                 messageSystem.getMessageWithFallback(
                 "error.xml.tag_found_outside",
                  "<{0}> found outside <{1}>", qName, "sheets"));
            }

            String name = attrs.getValue("name");

            if (name == null)
            {
               throw new SAXException(messageSystem.getMessageWithFallback(
                "error.xml.missing_attr_in_tag",
                "Missing ''{0}'' attribute in <{1}>", "name", qName));
            }

            String sheetId = attrs.getValue("sheetId");

            if (sheetId == null)
            {
               throw new SAXException(messageSystem.getMessageWithFallback(
                "error.xml.missing_attr_in_tag",
                "Missing ''{0}'' attribute in <{1}>", "sheetId", qName));
            }

            String rId = attrs.getValue("r:id");

            if (rId == null)
            {
               throw new SAXException(messageSystem.getMessageWithFallback(
                "error.xml.missing_attr_in_tag",
                "Missing ''{0}'' attribute in <{1}>", "rId", qName));
            }

            WorkSheetRef ws = new WorkSheetRef();
            ws.setName(name);
            ws.setSheetId(sheetId);
            ws.setRefId(rId);

            sheetRefs.put(rId, ws);
         }
      }
      else if (parsingRelationships)
      {
         if (qName.equals("Relationship"))
         {
            String rId = attrs.getValue("Id");

            if (rId == null)
            {
               throw new SAXException(messageSystem.getMessageWithFallback(
                "error.xml.missing_attr_in_tag",
                "Missing ''{0}'' attribute in <{1}>", "Id", qName));
            }

            WorkSheetRef ws = sheetRefs.get(rId);

            if (ws != null)
            {
               String target = attrs.getValue("Target");

               if (target == null)
               {
                  throw new SAXException(messageSystem.getMessageWithFallback(
                   "error.xml.missing_attr_in_tag",
                   "Missing ''{0}'' attribute in <{1}>", "Target", qName));
               }

               ws.setTarget("xl/"+target);

               if (firstSheet == null && ws.getIndex() == firstSheetIdx)
               {
                  firstSheet = ws;
               }

               if (activeSheet == null && ws.getIndex() == activeTabIdx)
               {
                  activeSheet = ws;
               }

               if (selectedWorkSheet == null)
               {
                  if (ws.isReferencedSheet(sheet))
                  {
                     selectedWorkSheet = ws;

                     throw new ParsingTerminatedException();
                  }
               }
            }
         }
      }
      else if (parsingStyles)
      {
         if (qName.equals("numFmt"))
         {
            if (numFmts == null)
            {
               numFmts = new Vector<NumberStyle>();
            }

            numFmts.add(new NumberStyle(this, attrs.getValue("numFmtId"),
             attrs.getValue("formatCode")));
         }
      }
      else if (parsingStrings)
      {
         if (qName.equals("sst"))
         {
            String value = attrs.getValue("uniqueCount");
            int n = 0;

            if (value != null)
            {
               try
               {
                  n = Integer.parseInt(value);
               }
               catch (NumberFormatException e)
               {
               }
            }

            if (n > 0)
            {
               sharedStrings = new Vector<String>(n);
            }
            else
            {
               sharedStrings = new Vector<String>();
            }
         }
         else if (qName.equals("si"))
         {
            if (sharedStrings == null)
            {
               throw new SAXException(
                 messageSystem.getMessageWithFallback(
                 "error.xml.tag_found_outside",
                  "<{0}> found outside <{1}>", qName, "sst"));
            }

            currentString = new StringBuilder();
         }
         else if (qName.equals("t"))
         {
            currentBuilder = new StringBuilder();
         }
      }
      else if (parsingData)
      {
         if (db == null && qName.equals("dimension"))
         {
            String value = attrs.getValue("ref");

            if (value == null)
            {
               throw new SAXException(messageSystem.getMessageWithFallback(
                "error.xml.missing_attr_in_tag",
                "Missing ''{0}'' attribute in <{1}>", "ref", qName));
            }

            Matcher m = DIMENSION_PATTERN.matcher(value);

            if (m.matches())
            {
               String startCol = m.group(1);
               String startRow = m.group(2);
               String endCol = m.group(3);
               String endRow = m.group(4);

               try
               {
                  startColIdx = getColumnIndex(startCol) + 1;
                  endColIdx = getColumnIndex(endCol) + 1;

                  startRowIdx = Integer.parseInt(startRow);
                  endRowIdx = Integer.parseInt(endRow);
               }
               catch (Exception e)
               {
                  importSettings.getMessageHandler().debug(e);

                  throw new SAXException(
                    messageSystem.getMessage("error.xml.invalid_attr_in_tag",
                     "ref", value, qName), e);
               }
               catch (Throwable e)
               {
                  importSettings.getMessageHandler().debug(e);

                  String msg = e.getMessage();

                  if (msg == null)
                  {
                     throw new SAXException(
                       messageSystem.getMessage("error.xml.invalid_attr_in_tag",
                        "ref", value, qName));
                  }
                  else
                  {
                     throw new SAXException(
                       messageSystem.getMessage("error.xml.invalid_attr_in_tag_cause",
                        "ref", value, qName, msg));
                  }
               }

               columnCount = endColIdx - startColIdx + 1;
               rowCount = endRowIdx - startRowIdx + 1;
            }
            else
            {
               throw new SAXException(
                 messageSystem.getMessage("error.xml.invalid_attr_in_tag",
                     "ref", value, qName));
            }
         }
         else if (qName.equals("sheetData"))
         {
            rowIdxOffset = startLine-1;

            if (importSettings.hasHeaderRow())
            {
               rowIdxOffset++;
            }

            db = new DatatoolDb(importSettings.getSettings(), rowCount, columnCount);
         }
         else if (qName.equals("row"))
         {
            if (db == null)
            {
               throw new SAXException(
                 messageSystem.getMessageWithFallback(
                 "error.xml.tag_found_outside",
                  "<{0}> found outside <{1}>", qName, "sheetData"));
            }

            String value = attrs.getValue("r");

            try
            {
               lineNumber = Integer.parseInt(value);
               rowIdx = lineNumber - 1 - rowIdxOffset;
            }
            catch (NumberFormatException e)
            {
               throw new SAXException(
                 messageSystem.getMessageWithFallback(
                 "error.xml.int_attr_required_in_tag",
                  "Integer ''{0}'' attribute required in <{1}> (found ''{2}'')",
                  "r", qName, value));
            }

            if (startRowIdx == 0 // dimensions not found
                 || lineNumber >= startRowIdx && lineNumber <= endRowIdx)
            {
               inTable = true;

               if (lineNumber >= startLine)
               {
                  currentRow = new DatatoolRow(db, columnCount);

                  if (rowIdx == -1)
                  {
                     inHeader = true;
                  }
               }
            }

         }
         else if (currentRow != null && qName.equals("c"))
         {
            String value = attrs.getValue("r");

            if (value == null)
            {
               throw new SAXException(messageSystem.getMessageWithFallback(
                "error.xml.missing_attr_in_tag",
                "Missing ''{0}'' attribute in <{1}>", "r", qName));
            }

            Matcher m = CELL_PATTERN.matcher(value);

            if (m.matches())
            {
               try
               {
                  columnIdx = getColumnIndex(m.group(1));
               }
               catch (IllegalArgumentException e)
               {
                  throw new SAXException(
                    messageSystem.getMessage("error.xml.invalid_attr_in_tag_cause",
                        "r", value, qName, e.getMessage()));
               }

               if (startColIdx > 1)
               {
                  columnIdx = columnIdx - (startColIdx-1);
               }
            }
            else
            {
               throw new SAXException(
                 messageSystem.getMessage("error.xml.invalid_attr_in_tag",
                     "r", value, qName));
            }

            value = attrs.getValue("t");

            if ("n".equals(value))
            {
               String styleId = attrs.getValue("s");
               String currency = null;
               DatumType type = DatumType.DECIMAL;

               if (styleId != null && !styleId.isEmpty())
               {
                  if (numFmts != null)
                  {
                     try
                     {
                        int sid = Integer.parseInt(styleId);

                        if (sid >= 0 && sid < numFmts.size())
                        {
                           NumberStyle numStyle = numFmts.get(sid);

                           currency = numStyle.getCurrency();

                           if (currency != null)
                           {
                              type = DatumType.CURRENCY;
                           }
                           else if (numStyle.hasDate())
                           {
                              if (numStyle.hasTime())
                              {
                                 type = DatumType.DATETIME;
                              }
                              else
                              {
                                 type = DatumType.DATE;
                              }
                           }
                           else if (numStyle.hasTime())
                           {
                              type = DatumType.TIME;
                           }
                        }
                     }
                     catch (NumberFormatException e)
                     {
                        importSettings.getMessageHandler().debug(e);
                     }
                  }
               }

               currentCell = new Datum(type,
                "", currency, Integer.valueOf(0), importSettings.getSettings());
            }
            else
            {
               currentCell = new Datum("", importSettings.getSettings());
            }

            currentRow.setCell(columnIdx, currentCell);
         }
         else if (currentRow != null && qName.equals("v"))
         {
            currentBuilder = new StringBuilder();
         }
      }
   }

   @Override
   public void endElement(String uri, String localName, String qName)
    throws SAXException
   {
      super.endElement(uri, localName, qName);

      if (parsingStrings)
      {
         if (qName.equals("si"))
         {
            sharedStrings.add(currentString.toString());
            currentString = null;
         }
         else if (qName.equals("t"))
         {
            currentString.append(currentBuilder);
            currentBuilder = null;
         }
      }
      else if (parsingData)
      {
         if (currentBuilder != null && currentCell !=null && qName.equals("v"))
         {
            String value = currentBuilder.toString();

            currentBuilder = null;

            if (currentCell.isNumeric())
            {
               Number num = null;

               try
               {
                  num = Integer.valueOf(value);

                  if (currentCell.getDatumType() == DatumType.DECIMAL)
                  {
                     currentCell.setDatumType(DatumType.INTEGER);
                  }
               }
               catch (NumberFormatException e)
               {
                  try
                  {
                     num = Double.valueOf(value);
                  }
                  catch (NumberFormatException e2)
                  {
                     importSettings.getMessageHandler().warning(
                       messageSystem.getMessage("error.number_expected", value), e2);
                  }
               }

               Julian julian = null;

               if (num == null)
               {
                  currentCell.setText(processText(value));
               }
               else
               {
                  DatatoolSettings ds = importSettings.getSettings();
                  DatumType type = currentCell.getDatumType();

                  if (type.isTemporal())
                  {
                     if (type == DatumType.DATE)
                     {
                        julian = Julian.createDay(num.intValue()+2415019);
                     }
                     else if (type == DatumType.DATETIME)
                     {
                        julian = Julian.createDate(num.doubleValue()+2415018.5);
                     }
                     else
                     {
                        double time = num.doubleValue()+2415018.5;
                        julian = Julian.createTime(time-(int)time);
                     }

                     currentCell.setText(ds.formatTemporal(julian));
                  }
                  else if (type == DatumType.CURRENCY)
                  {
                     currentCell.setText(ds.formatCurrency(
                       currentCell.getCurrencySymbol(), num.doubleValue()));
                  }
                  else
                  {
                     NumberFormat numFmt = ds.getNumericFormatter(type);

                     if (type == DatumType.DECIMAL && ds.useSIforDecimals())
                     {
                        currentCell.setText(String.format("\\num{%s}", processText(value)));
                     }
                     else
                     {
                        currentCell.setText(numFmt.format(num));
                     }
                  }
               }

               if (julian == null)
               {
                  currentCell.setNumeric(num);
               }
               else
               {
                  currentCell.setJulian(julian);
               }
            }
            else
            {
               try
               {
                  int i = Integer.valueOf(value);

                  if (sharedStrings != null && i >= 0 && i < sharedStrings.size())
                  {
                     currentCell.setText(processText(sharedStrings.get(i)));
                  }
                  else
                  {
                     currentCell.setText(value);

                     importSettings.getMessageHandler().warning(
                       messageSystem.getMessage(
                         "error.xlsx.unknown_shared_string_ref", value));
                  }
               }
               catch (NumberFormatException e)
               {
                  importSettings.getMessageHandler().warning(
                    messageSystem.getMessage("error.number_expected", value), e);

                  currentCell.setText(processText(value));
               }
            }
         }
         else if (qName.equals("c"))
         {
            currentCell = null;
         }
         else if (currentRow != null && qName.equals("row"))
         {
            if (inHeader)
            {
               for (int i = 0,
                     n = Math.max(currentRow.size(),
                       Math.max(importSettings.getColumnKeyCount(),
                                importSettings.getColumnHeaderCount()));
                     i < n; i++)
               {
                  String text = null;

                  if (i < currentRow.size())
                  {
                     Datum datum = currentRow.get(i);
                     text = datum.getText();
                  }

                  db.addColumn(importSettings.createHeader(db, i, text));
               }

               inHeader = false;
            }
            else
            {
               if (rowIdx - prevRowIdx > 1)
               {
                  switch (importSettings.getBlankRowAction())
                  {
                     case IGNORE:
                        rowIdxOffset++;
                        rowIdx--;
                     break;
                     case EMPTY_ROW:
                       for (int i = 0, n = rowIdx-prevRowIdx-1; i < n; i++)
                       {
                          db.appendRow(DatatoolRow.createEmptyRow(db));
                       }
                     break;
                     case END:
                        throw new ParsingTerminatedException();
                  }
               }

               db.appendRow(currentRow);
            }

            prevRowIdx = rowIdx;
            currentRow = null;
         }
      }
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

   public String getSelectedWorkBookPath()
   throws UserCancelledException
   {
      if (selectedWorkSheet == null)
      {
         if (sheetRefs == null || sheetRefs.size() < 2
             || importSettings.isBatchMode())
         {
            if (activeSheet != null)
            {
               selectedWorkSheet = activeSheet;
            }
            else if (firstSheet != null)
            {
               selectedWorkSheet = firstSheet;
            }
            else if (sheetRefs != null)
            {
               for (Iterator<String> it = sheetRefs.keySet().iterator();
                   it.hasNext(); )
               {
                  String key = it.next();

                  WorkSheetRef ws = sheetRefs.get(key);

                  if (ws.getTarget() != null)
                  {
                     selectedWorkSheet = ws;
                     break;
                  }
               }
            }
         }
         else
         {
            WorkSheetRef[] sheets = new WorkSheetRef[sheetRefs.size()];
            int i = 0;

            for (Iterator<String> it = sheetRefs.keySet().iterator();
                it.hasNext() && i < sheets.length; i++)
            {
               String key = it.next();
               sheets[i] = sheetRefs.get(key);
            }

            selectedWorkSheet = (WorkSheetRef)JOptionPane.showInputDialog(null,
                     messageSystem.getMessage("importspread.sheet"),
                     messageSystem.getMessage("importspread.title"),
                     JOptionPane.PLAIN_MESSAGE,
                     null, sheets, activeSheet);
         }
      }

      return selectedWorkSheet == null ? null : selectedWorkSheet.getTarget();
   }

   public DatatoolDb getDataBase()
   {
      return db;
   }

   public Object getSheetRef()
   {
      return sheet;
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

      if (!inHeader && importSettings.isTrimElementOn())
      {
         text = text.trim();
      }

      return text;
   }

   public boolean verbatimFound()
   {
      return verbatimFound;
   }

   /**
    * Converts column reference (A, B, ...Z, AA, AB, etc) to index.
    */
   public int getColumnIndex(String ref)
     throws IllegalArgumentException
   {
      int index = 0;

      for (int i = ref.length()-1, j = 0; i >= 0; i--, j++)
      {
         char c = ref.charAt(i);

         if (c < 'A' || c > 'Z')
         {
           throw new IllegalArgumentException(
             messageSystem.getMessage("error.xlsx.invalid_column_ref", ref));
         }

         int digit = c - 'A';

         switch (j)
         {
            case 0:
              index = digit;
            break;
            case 1:
              index += digit * 26;
            break;
            case 2:
              index += digit * 26 * 26;
            break;
            case 3:
              index += digit * 26 * 26 * 26;
            break;
            default:
              // infeasibly large!
              throw new IllegalArgumentException(
                messageSystem.getMessage("error.xlsx.invalid_column_ref", ref));
         }
      }

      return index;
   }

   protected ImportSettings importSettings;
   protected MessageSystem messageSystem;
   protected DataToolTeXParserListener listener;
   protected boolean checkForVerbatim;

   protected DatatoolDb db;
   protected boolean parsingWorkbook, parsingRelationships,
    parsingStrings, parsingData, parsingStyles;

   protected boolean inTable = false, inHeader = false;

   protected StringBuilder currentBuilder, currentString;

   protected int startLine;
   protected Object sheet;
   protected String activeTable;

   protected WorkSheetRef selectedWorkSheet, firstSheet, activeSheet;
   protected HashMap<String,WorkSheetRef> sheetRefs;
   protected Vector<String> sharedStrings;
   protected int firstSheetIdx=0, activeTabIdx=0;
   protected Vector<NumberStyle> numFmts;

   // used for capacity, may not be actual values
   protected int rowCount=0, columnCount=0;

   // 0-based
   protected int columnIdx=0, rowIdx=0, prevRowIdx, rowIdxOffset = 0;

   // 1-based
   protected int lineNumber=0,
    startColIdx=0, endColIdx=0, startRowIdx=0, endRowIdx=0;

   protected DatatoolRow currentRow;
   protected Datum currentCell;

   protected boolean verbatimFound = false;

   static final Pattern DIMENSION_PATTERN
    = Pattern.compile("([A-Z]+)(\\d+):([A-Z]+)(\\d+)");

   static final Pattern CELL_PATTERN
    = Pattern.compile("([A-Z]+)(\\d+)(?:([A-Z]+)(\\d+))?");
}

class WorkSheetRef
{
   public WorkSheetRef()
   {
      index = indexCount;
      indexCount++;
   }

   @Override
   public String toString()
   {
      return name;
   }

   public void setName(String name)
   {
      this.name = name;
   }

   public String getName()
   {
      return name;
   }

   public void setSheetId(String id)
   {
      this.sheetId = id;
   }

   public String getSheetId()
   {
      return sheetId;
   }

   public void setRefId(String id)
   {
      this.rId = id;
   }

   public String getRefId()
   {
      return rId;
   }

   public void setTarget(String target)
   {
      this.target = target;
   }

   public String getTarget()
   {
      return target;
   }

   public int getIndex()
   {
      return index;
   }

   public boolean isReferencedSheet(Object sheet)
   {
      if (sheet != null)
      {
         if (sheet instanceof Number)
         {
            if (((Number)sheet).intValue() == index)
            {
               return true;
            }
         }
         else if (sheet.toString().equals(name))
         {
            return true;
         }
      }

      return false;
   }

   String name, sheetId, rId, target;
   private int index;
   private static int indexCount=0;
}

class NumberStyle
{
   public NumberStyle(OfficeOpenReader reader, String fmtId, String code)
   {
      this.fmtId = fmtId;
      this.code = code;
      index = indexCount;
      indexCount++;

      if (code != null && !code.isEmpty())
      {
         Matcher m = CURRENCY_PATTERN.matcher(code);

         if (m.matches())
         {
            currency = reader.processText(m.group(1));
         }
         else
         {
            hasDate = DATE_PATTERN.matcher(code).matches();
            hasTime = TIME_PATTERN.matcher(code).matches();
         }
      }
   }

   public String getCurrency()
   {
      return currency;
   }

   public boolean hasDate()
   {
      return hasDate;
   }

   public boolean hasTime()
   {
      return hasTime;
   }

   String fmtId, code, currency;
   boolean hasDate, hasTime;
   private int index;
   private static int indexCount=0;

   static final Pattern TIME_PATTERN = Pattern.compile(
    ".*(h{1,2}.*?m{1,2}(?:.*?s{1,2})?).*");
   static final Pattern DATE_PATTERN = Pattern.compile(
    ".*(y+.*?m{1,3}(?:.?d{1,2})?|(?:d{1,2}+.*?)?m{1,3}.?y|(?:m{1,3}+.*?)?d{1,2}.?y).*");

   // Windows language code identifiers: 
   // https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-lcid/70feba9f-294e-491e-b6eb-56532684c37f
   static final Pattern CURRENCY_PATTERN = Pattern.compile(
    ".*\\[\\$(.+?)-([0-9a-fA-F]+|[a-z]{2}(?:-[a-zA-Z_\\-])?)\\].*");
}
