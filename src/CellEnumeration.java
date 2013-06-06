package com.dickimawbooks.datatooltk;

import java.util.*;

public class CellEnumeration implements Enumeration<DatatoolCell>
{
   public CellEnumeration(DatatoolRow cells)
   {
      this(cells, 0);
   }

   public CellEnumeration(DatatoolRow cells, int offset)
   {
      this.cells = cells;
      currentIdx = 0;
      this.offset = offset;

      if (this.offset >= cells.size() || this.offset < 0)
      {
         this.offset = 0;
      }
   }

   public boolean hasMoreElements()
   {
      return currentIdx < cells.size();
   }

   public DatatoolCell nextElement()
   {
      int columnIdx = currentIdx+1;

      // assume the columns are defined in order

      DatatoolCell cell = null;

      int start = currentIdx+offset;

      for (int i = start; i < cells.size(); i++)
      {
         DatatoolCell thisCell = cells.get(i);

         if (thisCell.getIndex() == columnIdx)
         {
            cell = thisCell;
            break;
         }
      }

      if (cell == null)
      {
         // wrap round to beginning

         for (int i = 0; i < start; i++)
         {
            DatatoolCell thisCell = cells.get(i);

            if (thisCell.getIndex() == columnIdx)
            {
               cell = thisCell;
               break;
            }
         }
      }

      currentIdx++;

      return cell;
   }

   private int currentIdx=0;
   private int offset = 0;
   private DatatoolRow cells;
}
