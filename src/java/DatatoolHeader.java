package com.dickimawbooks.datatooltk;

import java.io.*;
import java.util.regex.*;

import com.dickimawbooks.datatooltk.io.*;

public class DatatoolHeader
{
   public DatatoolHeader()
   {
      this(null);
   }

   public DatatoolHeader(String key)
   {
      this(key, key);
   }

   public DatatoolHeader(String key, String title)
   {
      this(key, title, DatatoolDb.TYPE_UNKNOWN);
   }

   public DatatoolHeader(String key, String title, int type)
   {
      setKey(key);
      setTitle(title);
      setType(type);
   }

   public int getType()
   {
      return type;
   }

   public String getKey()
   {
      return key;
   }

   public String getTitle()
   {
      return title;
   }

   public void setType(int type)
   {
      if (type < DatatoolDb.TYPE_UNKNOWN || type > DatatoolDb.TYPE_CURRENCY)
      {
         throw new IllegalArgumentException(
            DatatoolTk.getLabelWithValue("error.invalid_data_type", type));
      }

      this.type = type;
   }

   public void setKey(String key)
   {
      this.key = key;
   }

   public void setTitle(String title)
   {
      this.title = title;
   }

   public String toString()
   {
      return title;
   }

   private String key;
   private String title;
   private int type = DatatoolDb.TYPE_UNKNOWN;

}
