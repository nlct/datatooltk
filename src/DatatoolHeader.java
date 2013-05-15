package com.dickimawbooks.datatooltk;

public class DatatoolHeader
{
   public DatatoolHeader(String key)
   {
      this(key, key);
   }

   public DatatoolHeader(String key, String title)
   {
      this(key, title, DatatoolDb.TYPE_STRING);
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
      if (type < DatatoolDb.TYPE_STRING || type > DatatoolDb.TYPE_CURRENCY)
      {
         throw new IllegalArgumentException("Invalid data type "+type);
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

   private String key;
   private String title;
   private int type;
}
