package com.dickimawbooks.datatooltk;

import java.io.File;
import javax.swing.filechooser.FileFilter;

public class CsvFileFilter extends FileFilter
{
   public CsvFileFilter()
   {
      super();
      description = DatatoolTk.getLabel("filter.csv");
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

      return suffix.equals("csv");
   }

   public String getDescription()
   {
      return description;
   }

   private String description;
}
