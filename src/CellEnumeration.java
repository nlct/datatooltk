package com.dickimawbooks.datatooltk;

import java.util.*;

public class CellEnumeration implements Enumeration<DatatoolCell>
{
   public CellEnumeration(DatatoolRow cells)
   {
      this.cells = cells;
      currentIdx = 0;
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

      for (int i = currentIdx; i < cells.size(); i++)
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

         for (int i = 0; i < currentIdx; i++)
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
   private DatatoolRow cells;
}
