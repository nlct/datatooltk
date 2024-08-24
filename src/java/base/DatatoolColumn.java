/*
    Copyright (C) 2013-2024 Nicola L.C. Talbot
    www.dickimaw-books.com

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package com.dickimawbooks.datatooltk.base;

import java.util.Vector;

/**
 * Class representing a given column in a database.
 */

public class DatatoolColumn
{
   public DatatoolColumn(DatatoolHeader header, int colIdx,
     int numRows)
   {
      this.header = header;
      this.colIdx = colIdx;

      elements = new Datum[numRows];

      for (int i = 0; i < numRows; i++)
      {
         elements[i] = new Datum(header.getDb().getSettings());
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

      elements = new Datum[n];

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

   private Datum[] elements;

   private int colIdx;
}
