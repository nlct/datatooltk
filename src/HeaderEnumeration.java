package com.dickimawbooks.datatooltk;

// With a bit of luck, the datatool database that was read in had
// the columns defined in order of index, but there's no absolute
// guarantee that actually happened. This enumeration will return
// each header according to its column index.

import java.util.*;

public class HeaderEnumeration implements Enumeration<DatatoolHeader>
{
   public HeaderEnumeration(Vector<DatatoolHeader> headers)
   {
      this.headers = headers;
      currentIdx = 0;
   }

   public boolean hasMoreElements()
   {
      return currentIdx < headers.size();
   }

   public DatatoolHeader nextElement()
   {
      int columnIdx = currentIdx+1;

      // assume the columns are defined in order

      DatatoolHeader header = null;

      for (int i = currentIdx; i < headers.size(); i++)
      {
         DatatoolHeader thisHeader = headers.get(i);

         if (thisHeader.getColumnIndex() == columnIdx)
         {
            header = thisHeader;
            break;
         }
      }

      if (header == null)
      {
         // wrap round to beginning

         for (int i = 0; i < currentIdx; i++)
         {
            DatatoolHeader thisHeader = headers.get(i);

            if (thisHeader.getColumnIndex() == columnIdx)
            {
               header = thisHeader;
               break;
            }
         }
      }

      currentIdx++;

      return header;
   }

   private int currentIdx=0;
   private int offset=0;
   private Vector<DatatoolHeader> headers;
}
