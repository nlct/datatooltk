/*
    Copyright (C) 2024 Nicola L.C. Talbot
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

public class SortCriteria
{
   public SortCriteria(int columnIndex)
   {
      this(columnIndex, true);
   }

   public SortCriteria(int columnIndex, boolean ascending)
   {
      this.columnIndex = columnIndex;
      this.ascending = ascending;
   }

   public SortCriteria(String columnKey)
   {
      this(columnKey, true);
   }

   public SortCriteria(String columnKey, boolean ascending)
   {
      this.columnKey = columnKey;
      this.ascending = ascending;
   }

   public void setColumnIndex(int columnIndex)
   {
      this.columnIndex = columnIndex;
   }

   public void setAscending(boolean ascending)
   {
      this.ascending = ascending;
   }

   public int getColumnIndex()
   {
      return columnIndex;
   }

   public String getColumnKey()
   {
      return columnKey;
   }

   public boolean isAscending()
   {
      return ascending;
   }

   public void setFallbackColumns(int[] fallbackColumnIndexes)
   {
      this.fallbackColumnIndexes = fallbackColumnIndexes;
   }

   public int[] getFallbackColumnIndexes()
   {
      return fallbackColumnIndexes;
   }

   public void setFallbackColumns(String[] fallbackColumnKeys)
   {
      this.fallbackColumnKeys = fallbackColumnKeys;
   }

   public String[] getFallbackColumnKeys()
   {
      return fallbackColumnKeys;
   }

   int columnIndex=-1;
   String columnKey=null;
   boolean ascending=true;
   int[] fallbackColumnIndexes=null;
   String[] fallbackColumnKeys=null;
}
