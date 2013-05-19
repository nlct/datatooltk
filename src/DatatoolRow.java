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

   public CellEnumeration cellElements()
   {
      return new CellEnumeration(this);
   }

   public String[] getValues()
   {
      String[] cells = new String[size()];

      int i = 0;

      for (CellEnumeration en=cellElements();
          en.hasMoreElements(); )
      {
         i++;

         DatatoolCell cell = en.nextElement();

         if (cell == null)
         {
            cells[i] = "";
         }
         else
         {
            cells[i] = cell.getValue();
         }
      }

      return cells;
   }

   private int rowIndex=-1;
}
