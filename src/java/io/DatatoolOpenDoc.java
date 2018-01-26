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
import java.awt.Point;

import org.jopendocument.dom.spreadsheet.*;

import com.dickimawbooks.datatooltk.*;

/**
 * Class handling importing ODS data.
 */
public class DatatoolOpenDoc implements DatatoolSpreadSheetImport
{
   public DatatoolOpenDoc(DatatoolSettings settings)
   {
      this.settings = settings;
   }

   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
   }

   public DatatoolDb importData(String source)
      throws DatatoolImportException
   {
      return importData(new File(source));
   }

   public String[] getSheetNames(File file)
    throws IOException
   {
      if (!file.exists())
      {
         throw new IOException(
            getMessageHandler().getLabelWithValues("error.io.file_not_found", ""+file));
      }

      SpreadSheet spreadSheet = SpreadSheet.createFromFile(file);

      int numSheets = spreadSheet.getSheetCount();

      String[] names = new String[numSheets];

      for (int i = 0; i < numSheets; i++)
      {
         names[i] = spreadSheet.getSheet(i).getName();
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

         SpreadSheet spreadSheet = SpreadSheet.createFromFile(file);

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
            sheet = spreadSheet.getSheet(sheetIdx);
            db.setName(sheet.getName());
         }
         else
         {
            sheet = spreadSheet.getSheet(sheetName);
            db.setName(sheetName);
         }

         Point endPt = sheet.getUsedRange().getEndPoint();

         int rowCount = endPt.y+1;
         int colCount = endPt.x+1;

         if (rowCount == 0 || colCount == 0)
         {
            return db;
         }

         int rowIdx = 0;

         String[] fields = new String[colCount];

         int skipLines = settings.getCSVskiplines();

         for (int i = 0; i < skipLines && rowIdx < rowCount; i++)
         {
            rowIdx = readRow(sheet, rowIdx, fields, false, rowCount);
         }

         if (rowIdx >= rowCount) return db;

         boolean skipEmptyRows = settings.isSkipEmptyRowsOn();

         if (settings.hasCSVHeader())
         {
            // First row is header

            rowIdx = readRow(sheet, rowIdx, fields, skipEmptyRows, rowCount);
         }
         else
         {
            for (int colIdx = 0; colIdx < colCount; colIdx++)
            {
               fields[colIdx] = getMessageHandler().getLabelWithValues(
                  "default.field", ""+(colIdx+1));
            }
         }

         for (int i = 0; i < fields.length; i++)
         {
            DatatoolHeader header = new DatatoolHeader(db, fields[i]);
            db.addColumn(header);
         }

         int dataRowIdx = 0;

         while (rowIdx < rowCount)
         {
            rowIdx = readRow(sheet, rowIdx, fields, skipEmptyRows, rowCount);

            for (int colIdx = 0; colIdx < fields.length; colIdx++)
            {
               db.addCell(dataRowIdx, colIdx, fields[colIdx]);
            }

            dataRowIdx++;
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

   private int readRow(Sheet sheet, int rowIdx, String[] fields, 
     boolean skipEmpty, int rowCount)
   {
      if (skipEmpty)
      {
         boolean isEmpty = true;

         while (isEmpty && rowIdx < rowCount)
         {
            for (int colIdx = 0; colIdx < fields.length; colIdx++)
            {
               fields[colIdx] = 
                  getCellValue(sheet.getImmutableCellAt(colIdx, rowIdx));

               if (!fields[colIdx].isEmpty())
               {
                  isEmpty = false;
               }
            }

            rowIdx++;
         }
      }
      else
      {
         for (int colIdx = 0; colIdx < fields.length; colIdx++)
         {
            fields[colIdx] = 
               getCellValue(sheet.getImmutableCellAt(colIdx, rowIdx));
         }

         rowIdx++;
      }

      return rowIdx;
   }

   private String getCellValue(Cell cell)
   {
      String value = cell.getTextValue();

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

      if (!settings.isTeXMappingOn())
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

         String map = settings.getTeXMap(c);

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
}
