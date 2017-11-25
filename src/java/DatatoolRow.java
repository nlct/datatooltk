/*
    Copyright (C) 2013 Nicola L.C. Talbot
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

import java.util.Vector;
import java.util.Locale;
import java.text.Collator;

/**
 * Class representing a row of data.
 */
public class DatatoolRow extends Vector<String>
   implements Comparable<DatatoolRow>
{
   private DatatoolRow()
   {
      super();
   }

   public DatatoolRow(DatatoolDb db)
   {
      super();
      this.db = db;
   }

   public DatatoolRow(DatatoolDb db, int capacity)
   {
      super(capacity);
      this.db = db;
   }

   public void setCell(int colIdx, String value)
   {
      if (colIdx >= size())
      {
         addCell(colIdx, value);
      }
      else
      {
         set(colIdx, value);
      }
   }

   public void addCell(int colIdx, String value)
   {
      int n = size();

      if (colIdx == n)
      {
         add(value);
      }
      else if (colIdx > n)
      {
         for (int i = n; i < colIdx; i++)
         {
            add(new String());
         }

         add(value);
      }
      else
      {
         add(colIdx, value);
      }
   }

   public int compareTo(DatatoolRow row)
   {
      int sortColumn = db.getSortColumn();
      int columnType = db.getColumnType(sortColumn);

      try
      {
         if (columnType == DatatoolDb.TYPE_REAL)
         {
            Float x = new Float(get(sortColumn));

            Float y = new Float(row.get(sortColumn));

            return db.isSortAscending() ? x.compareTo(y) : y.compareTo(x);
         }
         else if (columnType == DatatoolDb.TYPE_INTEGER)
         {

            Integer x = new Integer(get(sortColumn));

            Integer y = new Integer(row.get(sortColumn));

            return db.isSortAscending() ? x.compareTo(y) : y.compareTo(x);
         }
         else if (columnType == DatatoolDb.TYPE_CURRENCY)
         {
            Currency x = db.parseCurrency(get(sortColumn));

            Currency y = db.parseCurrency(row.get(sortColumn));

            return db.isSortAscending() ? x.compareTo(y) : y.compareTo(x);
         }
      }
      catch (NumberFormatException e)
      {
      }

      String x = get(sortColumn);
      String y = row.get(sortColumn);

      Locale locale = db.getSortLocale();

      if (locale != null)
      {
         Collator collator = Collator.getInstance(locale);

         int result = collator.compare(x, y);

         return db.isSortAscending() ? result : -result;
      }

      if (!db.isSortCaseSensitive())
      {
         x = x.toLowerCase();
         y = y.toLowerCase();
      }

      return db.isSortAscending() ? x.compareTo(y) : y.compareTo(x);
   }

   public void setDatabase(DatatoolDb db)
   {
      this.db = db;
   }

   public DatatoolDb getDatabase()
   {
      return db;
   }

   private DatatoolDb db;
}
