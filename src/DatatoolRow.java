package com.dickimawbooks.datatooltk;

import java.util.Vector;

public class DatatoolRow extends Vector<DatatoolCell>
{
   public DatatoolRow()
   {
      super();
   }

   public DatatoolRow(int capacity)
   {
      super(capacity);
   }

   public void setRowIndex(int index)
   {
      rowIndex = index;
   }

   public int getRowIndex()
   {
      return rowIndex;
   }

   public DatatoolCell getCell(int colIdx)
   {
      for (DatatoolCell cell : this)
      {
         if (cell.getIndex() == colIdx)
         {
            return cell;
         }
      }

      return null;
   }

   public void setCell(int colIdx, String value)
   {
      DatatoolCell cell = getCell(colIdx);

      if (cell == null)
      {
         cell = new DatatoolCell(value, colIdx);
         add(cell);
      }
      else
      {
         cell.setValue(value);
      }
   }

   private int rowIndex=-1;
}
