package com.dickimawbooks.datatooltk.io;

import java.io.File;
import javax.swing.filechooser.FileFilter;

import com.dickimawbooks.datatooltk.DatatoolTk;

public class CsvTxtFileFilter extends FileFilter
{
   public CsvTxtFileFilter()
   {
      super();
      description = DatatoolTk.getLabelWithValue("filter.csvtxt", "*.csv, *.txt");
   }

   public boolean accept(File file)
   {
      if (file.isDirectory())
      {
         return true;
      }

      String name = file.getName();

      int idx = name.lastIndexOf(".");

      if (idx == -1)
      {
         return false;
      }

      String suffix = name.substring(idx+1).toLowerCase();

      return suffix.equals("csv") || suffix.equals("txt");
   }

   public String getDescription()
   {
      return description;
   }

   private String description;
}
