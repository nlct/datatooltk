package com.dickimawbooks.datatooltk;

import java.util.Vector;

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
