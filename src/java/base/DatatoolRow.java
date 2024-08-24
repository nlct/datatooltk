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

import java.text.CollationKey;
import java.text.Collator;

import java.util.Locale;
import java.util.Vector;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;

/**
 * Class representing a row of data.
 */
public class DatatoolRow extends Vector<Datum>
   implements Comparable<DatatoolRow>
{
   private DatatoolRow()
   {
      super();
   }

   public DatatoolRow(DatatoolDb db)
   {
      super(db.getSettings().getInitialColumnCapacity());
      this.db = db;
   }

   public DatatoolRow(DatatoolDb db, int capacity)
   {
      super(capacity > 0 ? capacity : db.getSettings().getInitialColumnCapacity());
      this.db = db;
   }

   public static DatatoolRow createEmptyRow(DatatoolDb db)
   {
      int numCols = db.getColumnCount();

      DatatoolRow row = new DatatoolRow(db, numCols);

      for (int i = 0; i < numCols; i++)
      {
         row.add(new Datum(db.getSettings()));
      }

      return row;
   }

   public void setCell(int colIdx, String value)
   {
      Datum datum;

      if (colIdx < size())
      {
         datum = get(colIdx);
         DatumType headerType = db.getColumnDatumType(colIdx);

         if (datum != null && datum.getDatumType() == headerType
              && headerType == DatumType.STRING)
         {
            datum.setText(value);
            return;
         }
      }

      datum = Datum.valueOf(value, db.getSettings());
      setCell(colIdx, datum);
   }

   public void setCell(int colIdx, Datum value)
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
      Datum datum = Datum.valueOf(value, db.getSettings());
      addCell(colIdx, datum);
   }

   public void addCell(int colIdx, Datum value)
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
            add(Datum.createNull(db.getSettings()));
         }

         add(value);
      }
      else
      {
         add(colIdx, value);
      }
   }

   @Override
   public int compareTo(DatatoolRow row)
   {
      Vector<SortCriteria> criteriaList = db.getSortCriteria();
      boolean nullFirst = db.getSettings().isNullFirst();

      int result = 0;

      for (SortCriteria criteria : criteriaList)
      {
         int columnIdx = criteria.getColumnIndex();
         boolean ascending = criteria.isAscending();

         if (columnIdx == -1) continue;

         Datum x = get(columnIdx);
         Datum y = row.get(columnIdx);

         Number xNum = x.getNumber();
         Number yNum = y.getNumber();

         if (xNum != null && yNum != null && !xNum.equals(yNum))
         {
            if (xNum instanceof Double || yNum instanceof Double)
            {
               result = (xNum.doubleValue() < yNum.doubleValue() ? -1 : 1);
            }
            else
            {
               result = (xNum.intValue() < yNum.intValue() ? -1 : 1);
            }
         }

         if (result == 0)
         {
            CollationKey xKey = x.getCollationKey();
            CollationKey yKey = y.getCollationKey();

            if (xKey != null && yKey != null)
            {
               result = xKey.compareTo(yKey);
            }

            if (result == 0)
            {
               String xStr = x.getStringSort();
               String yStr = y.getStringSort();

               if (xStr != null && yStr != null)
               {
                  result = xStr.compareTo(yStr);
               }
               else if (xStr == null)
               {
                  if (yStr != null)
                  {
                     result = nullFirst ? -1 : 1;
                  }
               }
               else if (yStr == null)
               {
                  result = nullFirst ? 1 : -1;
               }
            }
         }

         if (!ascending)
         {
            result = -result;
         }

         if (result != 0) return result;
      }

      return result;
   }

   public void setDatabase(DatatoolDb db)
   {
      this.db = db;
   }

   public DatatoolDb getDatabase()
   {
      return db;
   }

   public boolean isNullOrEmptyRow()
   {
      for (int i = 0; i < size(); i++)
      {
         if (!get(i).isNullOrEmpty())
         {
            return false;
         }
      }

      return true;
   }

   public void trimTrailingNullOrEmpty()
   {
      for (int i = size()-1; i >= 0; i--)
      {
         if (get(i).isNullOrEmpty())
         {
            remove(i);
         }
         else
         {
            break;
         }
      }
   }

   private DatatoolDb db;
}
