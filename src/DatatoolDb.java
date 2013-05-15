package com.dickimawbooks.datatooltk;

import java.io.File;
import java.util.Vector;

public class DatatoolDb
{
   public DatatoolDb()
   {
   }

   public void setFile(File file)
   {
      this.file = file;
   }

   public void setFile(String filename)
   {
      setFile(new File(filename));
   }

   public File getFile()
   {
      return file;
   }

   private Vector<DatatoolHeader> headers;

   private Vector<DatatoolRow> data;

   private File file;

   public static final int TYPE_STRING = 0, TYPE_INTEGER=1,
     TYPE_REAL=2, TYPE_CURRENCY=3;
}
