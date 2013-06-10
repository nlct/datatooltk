package com.dickimawbooks.datatooltk.gui;

import java.io.*;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.*;
import javax.swing.table.*;

import com.dickimawbooks.datatooltk.*;

public class DatatoolDbPanel extends JPanel
{
   public DatatoolDbPanel(DatatoolGUI gui, DatatoolDb db)
   {
      super(new BorderLayout());

      this.db = db;
      this.gui = gui;
      setName(db.getName());

      headerDialog = new HeaderDialog(gui, db, this);

      initTable();
   }

   private void initTable()
   {
      table = new JTable(new DatatoolDbTableModel(db, this));

      table.setRowHeight(100);
      table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
      table.setDefaultRenderer(String.class, new DbCellRenderer());
      table.setTableHeader(new DatatoolTableHeader(table.getColumnModel(),
         this));

      add(new JScrollPane(table), BorderLayout.CENTER);
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

   public void setModified(boolean modified)
   {
      isModified = modified;
   }

   public String getToolTipText()
   {
      File file = db.getFile();

      return file == null ? null : file.toString();
   }

   protected DatatoolDb db;

   private boolean isModified = false;

   protected HeaderDialog headerDialog;

   protected DatatoolGUI gui;

   private JTable table;
}

class DatatoolDbTableModel extends AbstractTableModel
{
   private DatatoolDb db;
   private DatatoolDbPanel panel;

   public DatatoolDbTableModel(DatatoolDb db, DatatoolDbPanel panel)
   {
      super();
      this.db = db;
      this.panel = panel;
   }

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
      panel.setModified(true);
      fireTableCellUpdated(row, col);
   }

   public boolean isCellEditable(int row, int column)
   {
      return (db.getHeader(column+1).getType() != DatatoolDb.TYPE_STRING);
   }
}

class DatatoolTableHeader extends JTableHeader
{
   private DatatoolDbPanel panel;

   public DatatoolTableHeader(TableColumnModel model, 
     DatatoolDbPanel p)
   {
      super(model);
      panel = p;

      addMouseListener(new MouseAdapter()
      {
         public void mouseClicked(MouseEvent event)
         {
            if (event.getClickCount() == 2)
            {
               panel.headerDialog.display(((JTableHeader)event.getSource())
                 .columnAtPoint(new Point(event.getX(), event.getY())));
               event.consume();
            }
         }
      });
   }

   public String getToolTipText(MouseEvent event)
   {
      int idx = columnAtPoint(new Point(event.getX(), event.getY()));

      DatatoolHeader header = panel.db.getHeader(idx+1);

      return DatatoolTk.getLabelWithValues("header.tooltip_format", 
         header.getKey(), DatatoolHeader.TYPE_LABELS[header.getType()+1]);
   }
}
