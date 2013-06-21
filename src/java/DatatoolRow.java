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

      if (columnType == DatatoolDb.TYPE_REAL)
      {
         Float x, y;

         try
         {
            x = new Float(get(sortColumn));
         }
         catch (NumberFormatException e)
         {
            x = new Float(0.0f);
         }

         try
         {
            y = new Float(row.get(sortColumn));
         }
         catch (NumberFormatException e)
         {
            y = new Float(0.0f);
         }

         return x.compareTo(y);
      }
      else if (columnType == DatatoolDb.TYPE_INTEGER)
      {
         Integer x, y;

         try
         {
            x = new Integer(get(sortColumn));
         }
         catch (NumberFormatException e)
         {
            x = new Integer(0);
         }

         try
         {
            y = new Integer(row.get(sortColumn));
         }
         catch (NumberFormatException e)
         {
            y = new Integer(0);
         }

         return x.compareTo(y);
      }


      return get(sortColumn).compareTo(row.get(sortColumn));
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
