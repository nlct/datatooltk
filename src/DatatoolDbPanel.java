package com.dickimawbooks.datatooltk;

import java.io.*;
import javax.swing.*;
import javax.swing.table.*;

public class DatatoolDbPanel extends JPanel
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

         public Object getValueAt(int row, int col)
         {
            return db.getRow(row+1).getCell(col+1).getValue();
         }

         public void setValueAt(Object value, int row, int col)
         {
            db.getRow(row+1).setCell(col+1, value.toString());
            isModified = true;
            fireTableCellUpdated(row, col);
         }
      });

      JScrollPane sp = new JScrollPane(table);

      add(sp);
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
            "Overwrite "+file+"?", "Confirm Overwrite",
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
         DatatoolGUI.error(this, e);
      }
   }

   public boolean isModified()
   {
      return isModified;
   }

   private DatatoolDb db;

   private boolean isModified = false;
}

