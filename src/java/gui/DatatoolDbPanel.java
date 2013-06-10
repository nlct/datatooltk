package com.dickimawbooks.datatooltk.gui;

import java.io.*;
import javax.swing.*;
import javax.swing.table.*;

import com.dickimawbooks.datatooltk.DatatoolDb;
import com.dickimawbooks.datatooltk.DatatoolTk;

public class DatatoolDbPanel extends JScrollPane
{
   public DatatoolDbPanel(DatatoolDb db)
   {
      super();

      this.db = db;
      setName(db.getName());

      initTable();
   }

   private void initTable()
   {
      JTable table = new JTable(new AbstractTableModel()
      {
         public String getColumnName(int col)
         {
            return db.getHeader(col+1).getTitle();
         }

         public int getRowCount()
         {
            return db.getRowCount();
         }

         public int getColumnCount()
         {
            return db.getColumnCount();
         }

         public Class<?> getColumnClass(int column)
         {
            switch (db.getHeader(column+1).getType())
            {
               case DatatoolDb.TYPE_INTEGER:
                  return Integer.class;
               case DatatoolDb.TYPE_REAL:
                  return Float.class;
            }

            return String.class;
         }

         public Object getValueAt(int row, int col)
         {
            return db.getValue(row+1, col+1);
         }

         public void setValueAt(Object value, int row, int col)
         {
            db.getRow(row+1).setCell(col+1, value.toString());
            isModified = true;
            fireTableCellUpdated(row, col);
         }

         public boolean isCellEditable(int row, int column)
         {
            return true;
         }
      });

      table.setRowHeight(100);
      table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
      table.setDefaultRenderer(String.class, new DbCellRenderer());
      table.setDefaultEditor(String.class, new DbCellEditor());

      setViewportView(table);
   }

   public void save(String filename)
   {
      save(new File(filename));
   }

   public void save(File file)
   {
      if (file.exists())
      {
         if (JOptionPane.showConfirmDialog(this,
             DatatoolTk.getLabelWithValue("message.overwrite_query",
               file.toString()),
             DatatoolTk.getLabel("message.confirm_overwrite"),
             JOptionPane.YES_NO_OPTION,
             JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
         {
            return;
         }
      }

      db.setFile(file);

      save();
   }

   public void save()
   {
      try
      {
         db.save();
         isModified = false;
      }
      catch (IOException e)
      {
         DatatoolGuiResources.error(this, e);
      }
   }

   public boolean isModified()
   {
      return isModified;
   }

   public String getToolTipText()
   {
      File file = db.getFile();

      return file == null ? null : file.toString();
   }

   private DatatoolDb db;

   private boolean isModified = false;
}

