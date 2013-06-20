package com.dickimawbooks.datatooltk;

import java.util.Vector;

public class DatatoolRow extends Vector<String>
   implements Comparable<DatatoolRow>
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
      if (colIdx >= size())
      {
         addCell(colIdx, value);
      }
      else
      {
         set(colIdx, value);
      }
   }

   public void addCell(int colIdx, String value)
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

   public static void setSortColumn(int columnIndex)
   {
      sortColumn = columnIndex;
   }

   public static int getSortColumn()
   {
      return sortColumn;
   }

   public static boolean isSortNumerical()
   {
      return sortNumerical;
   }

   public static void setSortNumerical(boolean enable)
   {
      sortNumerical = enable;
   }

   public int compareTo(DatatoolRow row)
   {
      if (sortNumerical)
      {
         Float x, y;

         try
         {
            x = new Float(get(sortColumn));
         }
         catch (NumberFormatException e)
         {
            x = new Float(0.0f);
         }

         try
         {
            y = new Float(row.get(sortColumn));
         }
         catch (NumberFormatException e)
         {
            y = new Float(0.0f);
         }

         return x.compareTo(y);
      }

      return get(sortColumn).compareTo(row.get(sortColumn));
   }

   private static int sortColumn = 0;

   private static boolean sortNumerical = false;
}
