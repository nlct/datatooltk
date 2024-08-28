/*
    Copyright (C) 2013-2024 Nicola L.C. Talbot
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
package com.dickimawbooks.datatooltk.extra;

import java.io.*;
import java.util.Iterator;
import java.util.Vector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.text.NumberFormat;

import javax.swing.JOptionPane;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

import com.dickimawbooks.texparserlib.TeXParser;
import com.dickimawbooks.texparserlib.latex.datatool.CsvBlankOption;
import com.dickimawbooks.texparserlib.latex.datatool.DatumType;

import com.dickimawbooks.datatooltk.*;
import com.dickimawbooks.datatooltk.io.*;

/**
 * Class handling importing Excel data.
 */
public class DatatoolExcel implements DatatoolImport
{
   public DatatoolExcel(DatatoolSettings settings)
   {
      this.settings = settings;
      this.importSettings = settings.getImportSettings();
   }

   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
   }

   @Override
   public DatatoolDb importData(ImportSettings importSettings, String source)
      throws DatatoolImportException
   {
      this.importSettings = importSettings;
      return importData(new File(source));
   }

   @Override
   public DatatoolDb importData(String source)
      throws DatatoolImportException
   {
      return importData(settings.getImportSettings(), source);
   }

   public DatatoolDb importData(File file)
      throws DatatoolImportException
   {
      DatatoolDb db = new DatatoolDb(settings);

      try
      {
         if (!file.exists())
         {
            throw new IOException(
               getMessageHandler().getLabelWithValues("error.io.file_not_found", ""+file));
         }

         Workbook workBook = WorkbookFactory.create(file);
         Sheet sheet;

         Object sheetRef = importSettings.getSheetRef();

         if (sheetRef == null)
         {
            int numSheets = workBook.getNumberOfSheets();

            if (numSheets == 1 || getMessageHandler().isBatchMode())
            {
               sheetRef = workBook.getSheetName(0);
            }
            else
            {
               String[] names = new String[numSheets];

               for (int i = 0; i < numSheets; i++)
               {
                  names[i] = workBook.getSheetName(i);
               }

               sheetRef = JOptionPane.showInputDialog(null,
                  getMessageHandler().getLabel("importspread.sheet"),
                  getMessageHandler().getLabel("importspread.title"),
                  JOptionPane.PLAIN_MESSAGE,
                  null, names,
                  null);

               if (sheetRef == null)
               {
                  throw new UserCancelledException(getMessageHandler());
               }
            }
         }

         if (sheetRef instanceof Number)
         {
            sheet = workBook.getSheetAt(((Number)sheetRef).intValue());
            db.setName(sheet.getSheetName());
         }
         else
         {
            String sheetName = sheetRef.toString();
            sheet = workBook.getSheet(sheetName);
            db.setName(sheetName);
         }

         Iterator<Row> rowIter = sheet.rowIterator();
         int rowIdx = 0;

         if (!rowIter.hasNext())
         {
            return db;
         }

         Vector<Datum> fields = new Vector<Datum>();
         CsvBlankOption blankOpt = importSettings.getBlankRowAction();

         int skipLines = importSettings.getSkipLines();

         for (int i = 0; i < skipLines; i++)
         {
            if (readRow(rowIter, fields, CsvBlankOption.EMPTY_ROW) == null)
            {
               return db;
            }
         }

         if (importSettings.hasHeaderRow())
         {
            // First row is header

            if (readRow(rowIter, fields, blankOpt) == null)
            {
               return db;
            }

            for (int colIdx = 0, n = fields.size(); colIdx < n; colIdx++)
            {
               Datum field = fields.get(colIdx);
               String text = (field == null ? null : field.getText());

               db.addColumn(importSettings.createHeader(db, colIdx, text));
            }
         }
         else
         {
            // First row of data
   
            if (readRow(rowIter, fields, blankOpt) == null)
            {
               return db;
            }

            for (int colIdx = 0, n = fields.size(); colIdx < n; colIdx++)
            {
               db.addColumn(importSettings.createHeader(db, colIdx, null));

               Datum datum = fields.get(colIdx);

               if (datum == null)
               {
                  if (settings.isImportEmptyToNullOn())
                  {
                     datum = Datum.createNull(settings);
                  }
                  else
                  {
                     datum = new Datum(settings);
                  }
               }

               if (colIdx >= db.getColumnCount())
               {
                  for (int i = db.getColumnCount(); i <= colIdx; i++)
                  {
                     db.addColumn(importSettings.createHeader(db, i, null),
                       importSettings.isImportEmptyToNullOn());
                  }
               }

               db.addCell(rowIdx, colIdx, datum);
            }

            rowIdx++;
         }
   
         while (readRow(rowIter, fields, blankOpt) != null)
         {
            for (int colIdx = 0, n = fields.size(); colIdx < n; colIdx++)
            {
               Datum datum = fields.get(colIdx);

               if (datum == null)
               {
                  if (settings.isImportEmptyToNullOn())
                  {
                     datum = Datum.createNull(settings);
                  }
                  else
                  {
                     datum = new Datum(settings);
                  }
               }

               if (colIdx >= db.getColumnCount())
               {
                  for (int i = db.getColumnCount(); i <= colIdx; i++)
                  {
                     db.addColumn(importSettings.createHeader(db, i, null),
                       importSettings.isImportEmptyToNullOn());
                  }
               }

               db.addCell(rowIdx, colIdx, datum);
            }

            if (importSettings.isImportEmptyToNullOn())
            {
               for (int i = fields.size(); i < db.getColumnCount(); i++)
               {
                  db.setToNull(rowIdx, i);
               }
            }
            
            rowIdx++;
         }
      }
      catch (Exception e)
      {
         throw new DatatoolImportException(
          getMessageHandler().getLabelWithValues("error.import.failed", 
           file, getMessageHandler().getMessage(e)), e);
      }

      return db;
   }

   public Vector<Datum> readRow(Iterator<Row> rowIter, Vector<Datum> fields,
     CsvBlankOption blankOpt)
   {
      if (!rowIter.hasNext()) return null;

      Row row = rowIter.next();

      if (blankOpt != CsvBlankOption.EMPTY_ROW)
      {
         boolean empty = true;

         while (empty)
         {
            fields.clear();

            for (Cell cell : row)
            {
               int colIdx = cell.getColumnIndex();

               if (colIdx >= fields.size())
               {
                  fields.setSize(colIdx+1);
               }

               fields.set(colIdx, getCellValue(cell));

               empty = false;
            }

            if (empty)
            {
               if (!rowIter.hasNext() || blankOpt == CsvBlankOption.END)
               {
                  return null;
               }

               row = rowIter.next();
            }
         }
      }
      else
      {
         fields.clear();

         for (Cell cell : row)
         {
            int colIdx = cell.getColumnIndex();

            if (colIdx >= fields.size())
            {
               fields.setSize(colIdx+1);
            }

            fields.set(colIdx, getCellValue(cell));
         }
      }

      return fields;
   }

   protected Datum getCellValue(Cell cell)
   {
      String strValue = null;
      Number num = null;
      String currencySym = null;
      DatumType type = DatumType.UNKNOWN;

      CellType cellType = cell.getCellType();

      switch (cellType)
      {
         case NUMERIC:
         case FORMULA:
           double doubleValue = cell.getNumericCellValue();
           num = Double.valueOf(doubleValue);
           type = DatumType.DECIMAL;

           String fmtStr = cell.getCellStyle().getDataFormatString();

           Matcher m = CURRENCY_PATTERN.matcher(fmtStr);

           if (m.matches())
           {
              currencySym = m.group(1);
              type = DatumType.CURRENCY;
              strValue = settings.formatCurrency(currencySym, doubleValue);

              currencySym = mapFieldIfRequired(currencySym);
           }
           else if (DATE_TIME_PATTERN.matcher(fmtStr).matches())
           {
              type = DatumType.STRING;
              num = null;
              strValue = settings.formatDateTime(cell.getDateCellValue());
           }
           else if (DATE_PATTERN.matcher(fmtStr).matches())
           {
              type = DatumType.STRING;
              num = null;
              strValue = settings.formatDate(cell.getDateCellValue());
           }
           else if (TIME_PATTERN.matcher(fmtStr).matches())
           {
              type = DatumType.STRING;
              num = null;
              strValue = settings.formatTime(cell.getDateCellValue());
           }
           else
           {
              NumberFormat numFmt = settings.getNumericFormatter(type);

              if ((doubleValue == Math.floor(doubleValue))
                   && doubleValue >= -TeXParser.MAX_TEX_INT
                   && doubleValue <= TeXParser.MAX_TEX_INT)
              {
                 type = DatumType.INTEGER;
                 num = Integer.valueOf((int)doubleValue);
                 numFmt = settings.getNumericFormatter(type);
              }

              strValue = numFmt.format(doubleValue);
           }
         break;
         case STRING:
           strValue = cell.getStringCellValue();
           type = DatumType.STRING;
         break;
         case BOOLEAN:
           strValue = cell.toString();
           num = Integer.valueOf(cell.getBooleanCellValue() ? 1 : 0);
           type = DatumType.INTEGER;
         break;
         case BLANK:
           if (!settings.isImportEmptyToNullOn())
           {
              strValue = "";
           }
         break;
      }

      if (strValue == null)
      {
         return Datum.createNull(settings);
      }

      strValue = mapFieldIfRequired(strValue);

      return new Datum(type, strValue, currencySym, num, settings);
   }

   public String mapFieldIfRequired(String field)
   {
      if (field.isEmpty())
      {
         return field;
      }

      if (!importSettings.isLiteralContent())
      {
         return DatatoolDb.PATTERN_PARAGRAPH.matcher(field)
            .replaceAll("\\\\DTLpar ");
      }

      String value = field.replaceAll("\\\\DTLpar *", String.format("%n%n"));

      int n = value.length();

      StringBuilder builder = new StringBuilder(n);

      for (int j = 0; j < n; )
      {
         int c = value.codePointAt(j);
         j += Character.charCount(c);

         String map = importSettings.getTeXMap(c);

         if (map == null)
         {
            builder.appendCodePoint(c);
         }
         else
         {
            builder.append(map);
         }
      }

      return DatatoolDb.PATTERN_PARAGRAPH.matcher(builder.toString())
         .replaceAll("\\\\DTLpar ");
   }

   private DatatoolSettings settings;
   private ImportSettings importSettings;

   // Not implementing style formatting, but try to pick out
   // currency symbol from the format:
   static final Pattern CURRENCY_PATTERN 
      = Pattern.compile(".*\\[\\$([^-]+)-\\d+\\].*");
   // Likewise, try to detect if the format indicates a date/time
   static final Pattern DATE_TIME_PATTERN
      = Pattern.compile(".*(yy.*hh?|hh?.*yy).*");
   static final Pattern DATE_PATTERN
      = Pattern.compile(".*yy.*");
   static final Pattern TIME_PATTERN
      = Pattern.compile(".*hh?.*");

}
