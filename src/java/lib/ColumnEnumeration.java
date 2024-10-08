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
package com.dickimawbooks.datatooltk;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Enumeration over columns of a database.
 */

public class ColumnEnumeration implements Enumeration<Datum>
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

   public Datum nextElement()
   {
      Datum element = data.get(rowIndex).get(columnIndex);

      rowIndex++;

      return element;
   }

   private Vector<DatatoolRow> data;
   private int columnIndex, rowIndex;
}
