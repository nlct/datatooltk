package com.dickimawbooks.datatooltk.enumeration;

// With a bit of luck, the datatool database that was read in had
// the rows defined in order of index, but there's no absolute
// guarantee that actually happened. This enumeration will return
// each row according to its row index.

import java.util.*;

import com.dickimawbooks.datatooltk.*;

public class RowEnumeration implements Enumeration<DatatoolRow>
{
   public RowEnumeration(Vector<DatatoolRow> rows)
   {
      this(rows, 0);
   }

   public RowEnumeration(Vector<DatatoolRow> rows, int offset)
   {
      this.rows = rows;
      currentIdx = 0;
      this.offset = offset;

      if (this.offset >= rows.size() || this.offset < 0)
      {
         this.offset = 0;
      }
   }

   public boolean hasMoreElements()
   {
      return currentIdx < rows.size();
   }

   public DatatoolRow nextElement()
   {
      int rowIdx = currentIdx+1;

      // assume the rows are defined in order

      DatatoolRow row = null;

      int start = currentIdx+offset;

      for (int i = start; i < rows.size(); i++)
      {
         DatatoolRow thisRow = rows.get(i);

         if (thisRow.getRowIndex() == rowIdx)
         {
            row = thisRow;
            break;
         }
      }

      if (row == null)
      {
         // wrap round to beginning

         for (int i = 0; i < start; i++)
         {
            DatatoolRow thisRow = rows.get(i);

            if (thisRow.getRowIndex() == rowIdx)
            {
               row = thisRow;
               break;
            }
         }
      }

      currentIdx++;

      return row;
   }

   private int currentIdx=0;
   private int offset=0;
   private Vector<DatatoolRow> rows;
}
