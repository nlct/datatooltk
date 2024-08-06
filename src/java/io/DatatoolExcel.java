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
package com.dickimawbooks.datatooltk.io;

import java.io.*;
import java.util.Iterator;
import java.util.Vector;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

import com.dickimawbooks.texparserlib.latex.datatool.CsvBlankOption;
import com.dickimawbooks.texparserlib.latex.datatool.IOSettings;
import com.dickimawbooks.datatooltk.*;

/**
 * Class handling importing Excel data.
 */
public class DatatoolExcel implements DatatoolSpreadSheetImport
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

   public String[] getSheetNames(File file)
    throws IOException
   {
      if (!file.exists())
      {
         throw new IOException(
            getMessageHandler().getLabelWithValues("error.io.file_not_found", 
             file.toString()));
      }

      Workbook workBook = WorkbookFactory.create(file);

      int numSheets = workBook.getNumberOfSheets();

      String[] names = new String[numSheets];

      for (int i = 0; i < numSheets; i++)
      {
         names[i] = workBook.getSheetName(i);
      }

      return names;
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

         if (file.getName().toLowerCase().endsWith(".xlsx"))
         {
            throw new IOException(
              getMessageHandler().getLabel("error.xlsx_not_supported"));
         }

         Workbook workBook = WorkbookFactory.create(file);
         Sheet sheet;

         Object sheetRef = importSettings.getSheetRef();

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

         Vector<String> fields = new Vector<String>();
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
               String field = fields.get(colIdx);

               if (field.isEmpty())
               {
                  field = getMessageHandler().getLabelWithValues(
                     "default.field", (colIdx+1));
               }

               db.addColumn(new DatatoolHeader(db, field));
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
               DatatoolHeader header = new DatatoolHeader(db,
                 getMessageHandler().getLabelWithValues("default.field", 
                   (colIdx+1)));
               db.addColumn(header);

               db.addCell(rowIdx, colIdx, fields.get(colIdx));
            }

            rowIdx++;
         }
   
         while (readRow(rowIter, fields, blankOpt) != null)
         {
            for (int colIdx = 0, n = fields.size(); colIdx < n; colIdx++)
            {
               db.addCell(rowIdx, colIdx, fields.get(colIdx));
            }
            
            rowIdx++;
         }
      }
      catch (Exception e)
      {
         throw new DatatoolImportException(
          getMessageHandler().getLabelWithValues("error.import.failed", 
           file), e);
      }

      return db;
   }

   public Vector<String> readRow(Iterator<Row> rowIter, Vector<String> fields,
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
               fields.add(getCellValue(cell));

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
            fields.add(getCellValue(cell));
         }
      }

      return fields;
   }

   private String getCellValue(Cell cell)
   {
      switch (cell.getCellType())
      {
         case NUMERIC:
         case FORMULA:
           return ""+cell.getNumericCellValue();
         case BLANK:
           return "";
      }

      String value = cell.toString();

      if (value == null)
      {
         return "\\@dtlnovalue ";
      }

      return mapFieldIfRequired(value);
   }

   public String mapFieldIfRequired(String field)
   {
      if (field.isEmpty())
      {
         return field;
      }

      if (!importSettings.isMapCharsOn())
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
}
