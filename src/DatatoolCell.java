package com.dickimawbooks.datatooltk;

public class DatatoolCell
{
   public DatatoolCell()
   {
      this(null, -1);
   }

   public DatatoolCell(String value)
   {
      this(value, -1);
   }

   public DatatoolCell(String value, int index)
   {
      data = value;
      colIndex = index;
   }

   public String toString()
   {
      return data;
   }

   public void setValue(String value)
   {
      data = value;
   }

   public void setIndex(int index)
   {
      colIndex = index;
   }

   public int getIndex()
   {
      return colIndex;
   }

   private String data=null;
   private int colIndex=-1;
}
