package com.dickimawbooks.datatooltk;

import java.util.Vector;

public class DatatoolColumn
{
   public DatatoolColumn(DatatoolHeader header, int colIdx,
     int numRows)
   {
      this.header = header;
      this.colIdx = colIdx;

      elements = new String[numRows];

      for (int i = 0; i < numRows; i++)
      {
         elements[i] = "";
      }
   }

   public DatatoolColumn(DatatoolHeader header, int colIdx, 
      Vector<DatatoolRow> data)
   {
      this(header, colIdx, data, false);
   }

   public DatatoolColumn(DatatoolHeader header, int colIdx, 
      Vector<DatatoolRow> data, boolean removeFromData)
   {
      this.header = header;
      this.colIdx = colIdx;

      int n = data.size();

      elements = new String[n];

      for (int i = 0; i < n; i++)
      {
         if (removeFromData)
         {
            elements[i] = data.get(i).remove(colIdx);
         }
         else
         {
            elements[i] = data.get(i).get(colIdx);
         }
      }
   }

   public void insertIntoData(Vector<DatatoolHeader> headers,
      Vector<DatatoolRow> data)
   {
      headers.add(colIdx, header);

      int n = elements.length;

      for (int i = 0; i < n; i++)
      {
         data.get(i).add(colIdx, elements[i]);
      }
   }

   public int getColumnIndex()
   {
      return colIdx;
   }

   public DatatoolHeader getHeader()
   {
      return header;
   }

   private DatatoolHeader header;

   private String[] elements;

   private int colIdx;
}
