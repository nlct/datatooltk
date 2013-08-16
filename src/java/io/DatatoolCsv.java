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
import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.CSVReader;

import com.dickimawbooks.datatooltk.*;

/**
 * Class handling importing and exporting CSV data.
 */
public class DatatoolCsv implements DatatoolImport,DatatoolExport
{
   public DatatoolCsv(DatatoolSettings settings)
   {
      this.settings = settings;
   }

   public void exportData(DatatoolDb db, String target)
      throws DatatoolExportException
   {
      File file = new File(target);

      BufferedWriter writer = null;
      CSVWriter csvWriter = null;

      String[] rowArray = new String[db.getColumnCount()];

      try
      {
         try
         {
            writer = new BufferedWriter(new FileWriter(file));
   
            csvWriter = new CSVWriter(writer, settings.getSeparator(),
              settings.getDelimiter());
   
            if (settings.hasCSVHeader())
            {
               csvWriter.writeNext(db.getColumnTitles());
            }
   
            for (int i = 0, n = db.getRowCount(); i < n; i++)
            {
               DatatoolRow row = db.getRow(i);
   
               csvWriter.writeNext(row.toArray(rowArray));
            }
         }
         finally
         {
            if (csvWriter != null)
            {
               csvWriter.close();
            }
   
            if (writer != null)
            {
               writer.close();
            }
         }
      }
      catch (IOException e)
      {
         throw new DatatoolExportException(
           DatatoolTk.getLabelWithValue("error.export.failed", target), e);
      }
   }

   public DatatoolDb importData(String source)
      throws DatatoolImportException
   {
      return importData(new File(source));
   }

   public DatatoolDb importData(File file)
      throws DatatoolImportException
   {
      boolean hasVerbatim = false;

      DatatoolDb db = new DatatoolDb(settings);

      String name = file.getName();

      int idx = name.lastIndexOf(".");

      if (idx != -1)
      {
         name = name.substring(0, idx);
      }

      db.setName(name);

      BufferedReader reader = null;
      CSVReader csvReader = null;

      try
      {
         try
         {
            reader = new BufferedReader(new FileReader(file));
   
            csvReader = new CSVReader(reader, settings.getSeparator(),
              settings.getDelimiter());
   
            String[] fields = csvReader.readNext();

            hasVerbatim = mapFieldsIfRequired(fields, !hasVerbatim) || hasVerbatim;
   
            if (fields == null)
            {
               // empty database
   
               return db;
            }
   
            int rowIdx = 0;
   
            if (settings.hasCSVHeader())
            {
               for (int i = 0; i < fields.length; i++)
               {
                  DatatoolHeader header = new DatatoolHeader(db, fields[i]);
                  db.addColumn(header);
               }
            }
            else
            {
               // fields is the first row of data
   
               for (int i = 0; i < fields.length; i++)
               {
                  DatatoolHeader header = new DatatoolHeader(db,
                    DatatoolTk.getLabelWithValue("default.field", (i+1)));
                  db.addColumn(header);
   
                  db.addCell(rowIdx, i, fields[i].replaceAll("\n\n+", "\\\\DTLpar "));
               }
   
               rowIdx++;
            }
   
            while ((fields = csvReader.readNext()) != null)
            {
               hasVerbatim = mapFieldsIfRequired(fields, !hasVerbatim) || hasVerbatim;

               for (int i = 0; i < fields.length; i++)
               {
                  db.addCell(rowIdx, i, fields[i]);
               }
   
               rowIdx++;
            }
         }
         finally
         {
            if (csvReader != null)
            {
               csvReader.close();
            }
   
            if (reader != null)
            {
               reader.close();
            }
         }
      }
      catch (IOException e)
      {
         throw new DatatoolImportException(
          DatatoolTk.getLabelWithValue("error.import.failed", 
           file.toString()), e);
      }

      if (hasVerbatim)
      {
         DatatoolTk.warning(DatatoolTk.getLabel("warning.verb_detected"));
      }

      return db;
   }

   public boolean mapFieldsIfRequired(String[] fields, boolean checkForVerbatim)
   {
      if (!settings.isTeXMappingOn())
      {
         boolean hasVerbatim = !checkForVerbatim;

         for (int i = 0; i < fields.length; i++)
         {
            fields[i].replaceAll("\n\n+", "\\\\DTLpar ");

            if (!hasVerbatim)
            {
               if (DatatoolDb.checkForVerbatim(fields[i]))
               {
                  hasVerbatim = true;
               }
            }
         }

         return hasVerbatim;
      }

      for (int i = 0; i < fields.length; i++)
      {
         if (fields[i].isEmpty())
         {
            continue;
         }

         String value = fields[i].replaceAll("\\\\DTLpar *", "\n\n");

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

         fields[i] = builder.toString().replaceAll("\n\n+", "\\\\DTLpar ");
      }

      return false;
   }

   private DatatoolSettings settings;
}
