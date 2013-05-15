package com.dickimawbooks.datatooltk;

public class DatatoolSettings
{
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

   protected String sep = ",";
   protected String delim = "\"";
}
