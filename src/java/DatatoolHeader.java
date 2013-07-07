package com.dickimawbooks.datatooltk;

import java.io.*;
import java.util.regex.*;

import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;

import com.dickimawbooks.datatooltk.io.*;
import com.dickimawbooks.datatooltk.gui.*;

public class DatatoolHeader extends TableColumn
{
   public DatatoolHeader(DatatoolDb db)
   {
      this(db, null);
   }

   public DatatoolHeader(DatatoolDb db, String key)
   {
      this(db, key, key);
   }

   public DatatoolHeader(DatatoolDb db, String key, String title)
   {
      this(db, key, title, DatatoolDb.TYPE_UNKNOWN);
   }

   public DatatoolHeader(DatatoolDb db, String key, String title, int type)
   {
      super();
      this.db = db;
      setKey(key);
      setTitle(title);
      setType(type);
   }

   public int getType()
   {
      return type;
   }

   public String getKey()
   {
      return key;
   }

   public String getTitle()
   {
      return title;
   }

   public void setType(int type)
   {
      if (type < DatatoolDb.TYPE_UNKNOWN || type > DatatoolDb.TYPE_CURRENCY)
      {
         throw new IllegalArgumentException(
            DatatoolTk.getLabelWithValue("error.invalid_data_type", type));
      }

      this.type = type;

      setPreferredWidth(db.getSettings().getCellWidth(type));
   }

   public void setKey(String key)
   {
      this.key = key;
   }

   public void setTitle(String title)
   {
      this.title = title;
   }

   public String toString()
   {
      return title;
   }

   public Object clone()
   {
      return new DatatoolHeader(db, key, title, type);
   }

   public String getHeaderValue()
   {
      return title;
   }

   public String getIdentifier()
   {
      return key;
   }

   public DatatoolDb getDb()
   {
      return db;
   }

   public void setDb(DatatoolDb db)
   {
      this.db = db;
   }

   private DatatoolDb db;

   private String key;
   private String title;
   private int type = DatatoolDb.TYPE_UNKNOWN;

}
