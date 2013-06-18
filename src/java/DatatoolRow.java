package com.dickimawbooks.datatooltk;

import java.util.Vector;

public class DatatoolRow extends Vector<String>
{
   public DatatoolRow()
   {
      super();
   }

   public DatatoolRow(int capacity)
   {
      super(capacity);
   }

   public void setCell(int colIdx, String value)
   {
      int n = size();

      if (colIdx == n)
      {
         add(value);
      }
      else if (colIdx > n)
      {
         for (int i = n; i < colIdx; i++)
         {
            add(new String());
         }

         add(value);
      }
      else
      {
         add(colIdx, value);
      }
   }
}
