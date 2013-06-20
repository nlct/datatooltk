package com.dickimawbooks.datatooltk;

import java.util.Enumeration;
import java.util.Vector;

public class ColumnEnumeration implements Enumeration<String>
{
   public ColumnEnumeration(Vector<DatatoolRow> data, int colIdx)
   {
      this.data        = data;
      this.columnIndex = colIdx;
      this.rowIndex    = 0;
   }

   public boolean hasMoreElements()
   {
      return rowIndex < data.size();
   }

   public String nextElement()
   {
      String element = data.get(rowIndex).get(columnIndex);

      rowIndex++;

      return element;
   }

   private Vector<DatatoolRow> data;
   private int columnIndex, rowIndex;
}
