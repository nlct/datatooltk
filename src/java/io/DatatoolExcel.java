/*
    Copyright (C) 2013 Nicola L.C. Talbot
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

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

import com.dickimawbooks.datatooltk.*;

/**
 * Class handling importing and exporting Excel data.
 */
public class DatatoolExcel implements DatatoolImport
{
   public DatatoolExcel(DatatoolSettings settings)
   {
      this.settings = settings;
   }

   public DatatoolDb importData(String source)
      throws DatatoolImportException
   {
      return importData(new File(source));
   }

   public DatatoolDb importData(File file)
      throws DatatoolImportException
   {
      DatatoolDb db = new DatatoolDb(settings);

      InputStream input = null;

      try
      {
         Workbook workBook = WorkbookFactory.create(file);
         Sheet sheet;

         String sheetRef = settings.getSheetRef();

         int sheetIdx = 0;
         String sheetName = null;

         try
         {
            sheetIdx = Integer.parseInt(sheetRef);
         }
         catch (NumberFormatException e)
         {
            sheetName = sheetRef;
         }

         if (sheetName == null)
         {
            sheet = workBook.getSheetAt(sheetIdx);
            db.setName(sheet.getSheetName());
         }
         else
         {
            sheet = workBook.getSheet(sheetName);
            db.setName(sheetName);
         }

         Iterator<Row> rowIter = sheet.rowIterator();
         int rowIdx = 0;

         if (!rowIter.hasNext())
         {
            return db;
         }

         Row row = rowIter.next();

         if (settings.hasCSVHeader())
         {
            // First row is header

            boolean empty = true;

            while (empty)
            {
               for (Cell cell : row)
               {
                  DatatoolHeader header 
                    = new DatatoolHeader(db, cell.toString());
                  db.addColumn(header);

                  empty = false;
               }

               if (empty)
               {
                  if (!rowIter.hasNext())
                  {
                     return db;
                  }

                  row = rowIter.next();
               }
            }
         }
         else
         {
            // First row of data
   
            int cellIdx = 0;

            for (Cell cell : row)
            {
               DatatoolHeader header = new DatatoolHeader(db,
                 DatatoolTk.getLabelWithValue("default.field", (cellIdx+1)));
               db.addColumn(header);

               db.addCell(rowIdx, cellIdx, getCellValue(cell));

               cellIdx++;
            }

            if (cellIdx > 0)
            {
               rowIdx++;
            }
         }
   
         while (rowIter.hasNext())
         {
            row = rowIter.next();

            int cellIdx = 0;

            for (Cell cell : row)
            {
               db.addCell(rowIdx, cellIdx, getCellValue(cell));

               cellIdx++;
            }

            if (cellIdx > 0)
            {
               rowIdx++;
            }
         }
      }
      catch (Exception e)
      {
         throw new DatatoolImportException(
          DatatoolTk.getLabelWithValue("error.import.failed", 
           file.toString()), e);
      }

      return db;
   }

   private String getCellValue(Cell cell)
   {
      switch (cell.getCellType())
      {
         case Cell.CELL_TYPE_NUMERIC:
         case Cell.CELL_TYPE_FORMULA:
           return ""+cell.getNumericCellValue();
         case Cell.CELL_TYPE_BLANK:
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
      if (!settings.isTeXMappingOn())
      {
         return field.replaceAll("\n\n+", "\\\\DTLpar ");
      }

      if (field.isEmpty())
      {
         return field;
      }

      String value = field.replaceAll("\\\\DTLpar *", "\n\n");

      int n = value.length();

      StringBuilder builder = new StringBuilder(n);

      for (int j = 0; j < n; j++)
      {
         char c = value.charAt(j);

         String map = settings.getTeXMap(c);

         if (map == null)
         {
            builder.append(c);
         }
         else
         {
            builder.append(map);
         }
      }

      return builder.toString().replaceAll("\n\n+", "\\\\DTLpar ");
   }

   private DatatoolSettings settings;
}
