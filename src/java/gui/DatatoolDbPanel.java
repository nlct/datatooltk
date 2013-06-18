package com.dickimawbooks.datatooltk.gui;

import java.io.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.undo.*;
import javax.swing.event.*;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;

import com.dickimawbooks.datatooltk.*;

public class DatatoolDbPanel extends JPanel
{
   public DatatoolDbPanel(DatatoolGUI gui, DatatoolDb db)
   {
      super(new BorderLayout());

      this.db = db;
      this.gui = gui;
      setName(db.getName());

      initTable();
   }

   private void initTable()
   {
      undoManager = new UndoManager();

      table = new JTable(new DatatoolDbTableModel(db, this));

      table.setRowHeight(100);
      table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

      DbNumericalCellEditor editor = new DbNumericalCellEditor();

      table.setDefaultEditor(Number.class, editor);
      table.setDefaultRenderer(Number.class, new DbNumericalCellRenderer());
      table.setDefaultRenderer(String.class, new DbCellRenderer());
      table.setTableHeader(new DatatoolTableHeader(table.getColumnModel(),
         this));
      table.setColumnSelectionAllowed(true);
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      table.addMouseListener(new MouseAdapter()
       {
          public void mouseClicked(MouseEvent evt)
          {
             createSelectCurrentCellUndoable();

             if (currentCell == null)
             {
                // nothing selected

                return;
             }

             if (evt.getClickCount() == 2)
             {
                int type = db.getHeader(currentCol+1).getType();

                if (type == DatatoolDb.TYPE_INTEGER
                 || type == DatatoolDb.TYPE_REAL)
                {
                   if (table.editCellAt(currentRow, currentCol))
                   {
                      return;
                   }
                   else
                   {
                      DatatoolTk.debug(
                        "Can't edit cell at col="+currentCol+", row="+currentRow);
                   }
                }

                requestCellEditor(currentCell);
             }
          }
       });

      table.getSelectionModel().addListSelectionListener(
        new ListSelectionListener()
        {
           public void valueChanged(ListSelectionEvent e)
           {
              int col = table.getSelectedColumn();
              int row = table.getSelectedRow();

              if (row != currentRow && col != currentCol)
              {
                 createSelectCurrentCellUndoable();
              }

              if (!e.getValueIsAdjusting())
              {
                 if (col == -1 || row == -1)
                 {
                    gui.enableEditCellItem(false);
                    return;
                 }

                 gui.enableEditCellItem(currentCell != null);
              }
           }
        });

      for (int i = 0; i < table.getColumnCount(); i++)
      {
         if (db.getHeader(i+1).getType() == DatatoolDb.TYPE_STRING)
         {
            TableColumn column = table.getColumnModel().getColumn(i);

            column.setPreferredWidth(Math.max(column.getPreferredWidth(),
              STRING_MIN_WIDTH));
         }
      }

      JScrollPane sp = new JScrollPane(table);
      sp.setRowHeaderView(new RowHeaderComponent(this));

      add(sp, BorderLayout.CENTER);
   }

   public int getSelectedRow()
   {
      return table.getSelectedRow();
   }

   public int getSelectedColumn()
   {
      return table.getSelectedColumn();
   }

   public void addUndoEvent(UndoableEditEvent event)
   {
      UndoableEdit edit = event.getEdit();
      undoManager.addEdit(edit);
      gui.updateUndoRedoItems(this, edit.getPresentationName(), null);
      isModified = true;
   }

   public boolean canUndo()
   {
      return undoManager.canUndo();
   }

   public boolean canRedo()
   {
      return undoManager.canRedo();
   }

   public void undo()
   {
      try
      {
         String name = undoManager.getPresentationName();
         undoManager.undo();
         gui.updateUndoRedoItems(this, 
            undoManager.canUndo() ? undoManager.getPresentationName():"", 
            name);
         repaint();
      }
      catch (CannotUndoException e)
      {
         DatatoolTk.debug(e);
      }
   }

   public void redo()
   {
      try
      {
         String name = undoManager.getPresentationName();
         undoManager.redo();

         gui.updateUndoRedoItems(this, name,
           undoManager.canRedo() ? undoManager.getPresentationName():"");

         repaint();
      }
      catch (CannotRedoException e)
      {
         DatatoolTk.debug(e);
      }
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
      if (db.getFile() == null)
      {
         gui.saveAs();
         return;
      }

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

   public void requestHeaderEditor(int colIdx)
   {
      gui.requestHeaderEditor(colIdx, this);
   }

   public void requestCellEditor(DatatoolCell cell)
   {
      if (cell != null)
      {
         gui.requestCellEditor(cell, this);
      }
   }

   public boolean hasSelectedCell()
   {
       return (table.getSelectedColumn() != -1 
            && table.getSelectedRow() != -1);
   }

   public void requestSelectedCellEdit()
   {
      if (currentCell != null)
      {
        requestCellEditor(currentCell);
      }
   }

   public void updateCell(String text)
   {
      updateCell(currentCell, text);
   }

   public void updateCell(int row, int col, String text)
   {
      updateCell(db.getRow(row+1).getCell(col+1), text);
   }

   public void updateCell(DatatoolCell cell, String text)
   {
      addUndoEvent(new UndoableEditEvent(cell, 
         new UpdateCellEdit(this, cell, text)));
   }

   public int getRowCount()
   {
      return table.getRowCount();
   }

   public int getRowHeight(int row)
   {
      return table.getRowHeight(row);
   }

   protected void setCurrentCell(int row, int col, DatatoolCell cell)
   {
      if (cell != currentCell)
      {
         table.clearSelection();

         if (col > -1 && row > -1)
         {
            table.setColumnSelectionInterval(col, col);
            table.setRowSelectionInterval(row, row);
         }
         currentCell = cell;
      }

      currentRow = row;
      currentCol = col;
   }

   private void createSelectCurrentCellUndoable()
   {
      int newRow = table.getSelectedRow();
      int newCol = table.getSelectedColumn();

      // Ignore if no change to current selection

      if (newRow == currentRow && newCol == currentCol)
      {
         return;
      }


      DatatoolCell newCell = null;

      if (newRow != -1 && newCol != -1)
      {
         newCell = db.getRow(newRow+1).getCell(newCol+1);
      }

      addUndoEvent(new UndoableEditEvent(this, 
         new SelectCellUndoableEdit(this, currentRow,
           currentCol, currentCell,
           newRow, newCol, newCell)));

      currentRow = newRow;
      currentCol = newCol;
      currentCell = newCell;
   }

   protected DatatoolDb db;

   private boolean isModified = false;

   protected DatatoolGUI gui;

   private JTable table;

   private UndoManager undoManager;

   private DatatoolCell currentCell;
   private int currentRow=-1, currentCol=-1;

   public static final int STRING_MIN_WIDTH=300;
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
      panel.updateCell(row, col, value.toString());
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
               panel.requestHeaderEditor(((JTableHeader)event.getSource())
                 .columnAtPoint(event.getPoint()));

               event.consume();
            }
         }
      });
   }

   public String getToolTipText(MouseEvent event)
   {
      int idx = columnAtPoint(event.getPoint());

      if (idx == -1)
      {
         return null;
      }

      DatatoolHeader header = panel.db.getHeader(idx+1);

      if (header == null)
      {
         return null;
      }

      return DatatoolTk.getLabelWithValues("header.tooltip_format", 
         header.getKey(), DatatoolHeader.TYPE_LABELS[header.getType()+1]);
   }
}
