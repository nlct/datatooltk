package com.dickimawbooks.datatooltk;

import java.io.*;
import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.CSVReader;

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

      try
      {
         try
         {
            writer = new BufferedWriter(new FileWriter(file));
   
            csvWriter = new CSVWriter(writer, settings.getSeparator(), settings.getDelimiter());
   
            if (settings.hasCSVHeader())
            {
               csvWriter.writeNext(db.getColumnTitles());
            }
   
            for (RowEnumeration en = db.rowElements();
                 en.hasMoreElements(); )
            {
               DatatoolRow row = en.nextElement();
   
               csvWriter.writeNext(row.getValues());
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
         throw new DatatoolExportException("Failed to export to '"
           +target+"'", e);
      }
   }

   public DatatoolDb importData(String source)
      throws DatatoolImportException
   {
      File file = new File(source);

      DatatoolDb db = new DatatoolDb();

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
   
            csvReader = new CSVReader(reader, settings.getSeparator(), settings.getDelimiter());
   
            String[] fields = csvReader.readNext();
   
            if (fields == null)
            {
               // empty database
   
               return db;
            }
   
            int rowIdx = 1;
   
            if (settings.hasCSVHeader())
            {
               for (int i = 0; i < fields.length; i++)
               {
                  DatatoolHeader header = new DatatoolHeader(fields[i]);
                  header.setColumnIndex(i+1);
                  db.addColumn(header);
               }
            }
            else
            {
               // fields is the first row of data
   
               for (int i = 1; i <= fields.length; i++)
               {
                  DatatoolHeader header = new DatatoolHeader("Field"+i);
                  header.setColumnIndex(i+1);
                  db.addColumn(header);
   
                  db.addCell(rowIdx, i, fields[i-1]);
               }
   
               rowIdx++;
            }
   
            while ((fields = csvReader.readNext()) != null)
            {
               for (int i = 0; i < fields.length; i++)
               {
                  db.addCell(rowIdx, i+1, fields[i]);
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
         throw new DatatoolImportException("Failed to import '"
          +source+"'", e);
      }

      return db;
   }

   private DatatoolSettings settings;
}
