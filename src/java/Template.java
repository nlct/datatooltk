package com.dickimawbooks.datatooltk;

import java.io.File;

public class Template
{
   public Template(File file)
   {
      this.file = file;

      name = file.getName();

      int index = name.lastIndexOf(".");

      if (index != -1)
      {
         name = name.substring(0, index);
      }
   }

   public String toString()
   {
      return name;
   }

   public File getFile()
   {
      return file;
   }

   private File file;
   private String name;
}
