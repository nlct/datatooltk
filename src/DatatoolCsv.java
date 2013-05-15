package com.dickimawbooks.datatooltk;

import java.io.*;
import au.com.bytecode.opencsv.CSVWriter;

public class DatatoolCsv implements DatatoolImport,DatatoolExport
{
   public DatatoolCsv()
   {
      this(",", "\"");
   }

   public DatatoolCsv(String separator, String delimiter)
   {
      setSeparator(separator);
      setDelimiter(delimiter);
   }

   public void setSeparator(String separator)
   {
      sep = separator;
   }

   public String getSeparator()
   {
      return sep;
   }

   public void setDelimiter(String delimiter)
   {
      delim = delimiter;
   }

   public String getDelimiter()
   {
      return delim;
   }

   public void exportData(DatatoolDb db, File file)
      throws IOException
   {
      BufferedWriter writer = null;

      try
      {
         writer = new BufferedWriter(new FileWriter(file));

         CSVWriter csvWriter = new CSVWriter(writer, sep, delim);

         csvWriter.writeAll(db.getColumnTitles());
      }
      finally
      {
         if (writer != null)
         {
            writer.close();
         }
      }
   }

   public DatatoolDb importData(File file)
      throws IOException
   {
      DatatoolDb db = null;

      return db;
   }

   public String sep, delim;
}
