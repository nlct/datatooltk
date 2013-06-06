package com.dickimawbooks.datatooltk.io;

import java.io.File;
import javax.swing.filechooser.FileFilter;

import com.dickimawbooks.datatooltk.DatatoolTk;

public class TeXFileFilter extends FileFilter
{
   public TeXFileFilter()
   {
      super();
      description = DatatoolTk.getLabelWithValue("filter.tex", "*.tex, *.ltx");
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

      return suffix.equals("tex") || suffix.equals("ltx");
   }

   public String getDescription()
   {
      return description;
   }

   private String description;
}
