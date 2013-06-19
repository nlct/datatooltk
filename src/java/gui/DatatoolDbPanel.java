package com.dickimawbooks.datatooltk.gui;

import java.io.*;
import java.util.*;
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

      table.setRowHeight(DEFAULT_ROW_HEIGHT);
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
             int row = table.getSelectedRow();
             int col = table.getSelectedColumn();

             rowHeaderComponent.updateRowSelection(row);
             table.getTableHeader().repaint();

             if (row == -1 || col == -1)
             {
                // nothing selected

                return;
             }

             if (evt.getClickCount() == 2)
             {
                int type = db.getHeader(col).getType();

                if (type == DatatoolDb.TYPE_INTEGER
                 || type == DatatoolDb.TYPE_REAL)
                {
                   if (table.editCellAt(row, col))
                   {
                      return;
                   }
                   else
                   {
                      DatatoolTk.debug(
                        "Can't edit cell at col="+col+", row="+row);
                   }
                }

                requestCellEditor(row, col);
             }
          }
       });

      table.getSelectionModel().addListSelectionListener(
        new ListSelectionListener()
        {
           public void valueChanged(ListSelectionEvent e)
           {
              if (!e.getValueIsAdjusting())
              {
                 selectionUpdated();
              }
           }
        });

      sp = new JScrollPane(table);

      rowHeaderComponent = new RowHeaderComponent(this);
      sp.setRowHeaderView(rowHeaderComponent);

      add(sp, BorderLayout.CENTER);

      updateColumnHeaders();
   }

   protected void selectionUpdated()
   {
      int col = table.getSelectedColumn();
      int row = table.getSelectedRow();

      rowHeaderComponent.updateRowSelection(row);
      table.getTableHeader().repaint();

      gui.enableEditItems(col != -1 && row != -1);
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

   public void requestSelectedHeaderEditor()
   {
      int colIndex = table.getSelectedColumn();

      if (colIndex > -1)
      {
         requestHeaderEditor(colIndex);
      }
   }

   public void requestHeaderEditor(int colIdx)
   {
      gui.requestHeaderEditor(colIdx, this);
   }

   public void requestCellEditor(int row, int col)
   {
      if (row != -1 && col != -1)
      {
         gui.requestCellEditor(row, col, this);
      }
   }

   public boolean hasSelectedCell()
   {
       return (table.getSelectedColumn() != -1 
            && table.getSelectedRow() != -1);
   }

   public void requestSelectedCellEdit()
   {
      requestCellEditor(table.getSelectedRow(),
           table.getSelectedColumn());
   }

   public void updateCell(String text)
   {
      updateCell(table.getSelectedRow(),
        table.getSelectedColumn(), text);
   }

   public void updateCell(int row, int col, String text)
   {
      addUndoEvent(new UndoableEditEvent(this, 
         new UpdateCellEdit(this, row, col, text)));
   }

   public void requestNewColumnAfter()
   {
      DatatoolHeader header = gui.requestNewHeader(this);

      if (header == null)
      {
         return;
      }

      int colIdx = table.getSelectedColumn()+1;

      if (colIdx == 0)
      {
         colIdx = db.getColumnCount();
      }

      addUndoEvent(new UndoableEditEvent(this, 
         new InsertColumnEdit(this, header, colIdx)));
   }

   public void requestNewColumnBefore()
   {
      DatatoolHeader header = gui.requestNewHeader(this);

      if (header == null)
      {
         return;
      }

      int colIdx = table.getSelectedColumn();

      if (colIdx < 0)
      {
         colIdx = 0;
      }

      addUndoEvent(new UndoableEditEvent(this, 
         new InsertColumnEdit(this, header, colIdx)));
   }

   public void removeSelectedColumn()
   {
      addUndoEvent(new UndoableEditEvent(this, 
         new RemoveColumnEdit(this, table.getSelectedColumn())));
   }

   public void insertNewRowAfter()
   {
      // insert new row after selected row or after last row if none
      // selected.

      int rowIdx = table.getSelectedRow()+1;

      if (rowIdx == 0)
      {
         rowIdx = db.getRowCount();
      }

      addUndoEvent(new UndoableEditEvent(this, 
         new InsertRowEdit(this, rowIdx)));
   }

   public void insertNewRowBefore()
   {
      // insert new row before selected row or before first row if none
      // selected.

      int rowIdx = table.getSelectedRow();

      if (rowIdx < 0)
      {
         rowIdx = 0;
      }

      addUndoEvent(new UndoableEditEvent(this, 
         new InsertRowEdit(this, rowIdx)));
   }

   public void addRowButton()
   {
      rowHeaderComponent.addButton();
   }

   public void removeRowButton()
   {
      rowHeaderComponent.removeButton();
   }

   public void fireRowInserted(int rowIdx)
   {
      ((AbstractTableModel)table.getModel()).fireTableRowsInserted(rowIdx, rowIdx);
   }

   public void fireRowDeleted(int rowIdx)
   {
      ((AbstractTableModel)table.getModel()).fireTableRowsDeleted(rowIdx, rowIdx);
   }

   public void selectRow(int row)
   {
      if (row >= getRowCount() || row < 0)
      {
         gui.enableEditItems(hasSelectedCell());
         return;
      }

      int col = table.getSelectedColumn();

      if (col == -1)
      {
         col = 0;
      }

      selectCell(row, col);
   }

   public void selectColumn(int col)
   {
      if (col >= db.getColumnCount() || col < 0)
      {
         gui.enableEditItems(hasSelectedCell());
         return;
      }

      int row = table.getSelectedRow();

      if (row == -1)
      {
         row = 0;
      }

      selectCell(row, col);
   }

   public void selectCell(int row, int col)
   {
      int oldRow = table.getSelectedRow();
      int oldCol = table.getSelectedColumn();

      gui.enableEditItems(row != -1 && col != -1);

      if (oldRow == row && oldCol == col)
      {
         return; // already selected
      }

      table.clearSelection();

      if (row > -1)
      {
         table.setRowSelectionInterval(row, row);
      }

      if (col > -1)
      {
         table.setColumnSelectionInterval(col, col);
      }

      if (row != oldRow)
      {
         rowHeaderComponent.updateRowSelection(row);
      }

      if (col != oldCol)
      {
         table.getTableHeader().repaint();
      }
   }

   public Color getSelectionBackground()
   {
      return table.getSelectionBackground();
   }

   public int getRowCount()
   {
      return table.getRowCount();
   }

   public int getRowHeight(int row)
   {
      return table.getRowHeight(row);
   }

   public void updateColumnHeader(int column)
   {
      TableColumn tableColumn 
         = table.getTableHeader().getColumnModel().getColumn(column);

      tableColumn.setHeaderValue(db.getHeader(column).getTitle());
      tableColumn.setIdentifier(db.getHeader(column).getKey());

      sp.getColumnHeader().repaint();
   }

   public void updateColumnHeaders()
   {
      TableColumnModel model = table.getTableHeader().getColumnModel();

      for (int i = 0, n = db.getColumnCount(); i < n; i++)
      {
         TableColumn column = model.getColumn(i);

         column.setHeaderValue(db.getHeader(i).getTitle());
         column.setIdentifier(db.getHeader(i).getKey());

         if (db.getHeader(i).getType() == DatatoolDb.TYPE_STRING)
         {
            column.setPreferredWidth(Math.max(column.getPreferredWidth(),
              STRING_MIN_WIDTH));
         }
      }

      if (sp.getColumnHeader() != null)
      {
         sp.getColumnHeader().repaint();
      }
      else
      {
         repaint();
      }
   }

   public void insertColumnHeader(int index, DatatoolHeader header)
   {
      TableColumnModel model = table.getTableHeader().getColumnModel();

      model.addColumn(new TableColumn());

      for (int i = index, n = db.getColumnCount(); i < n; i++)
      {
         model.getColumn(i).setHeaderValue(db.getHeader(i).getTitle());
         model.getColumn(i).setIdentifier(db.getHeader(i).getKey());
      }

      sp.getColumnHeader().repaint();
   }

   public void moveColumn(int fromIndex, int toIndex)
   {
      addUndoEvent(new UndoableEditEvent(this, 
         new MoveColumnEdit(this, fromIndex, toIndex)));
   }

   public void moveRow(int fromIndex, int toIndex)
   {
      addUndoEvent(new UndoableEditEvent(this, 
         new MoveRowEdit(this, fromIndex, toIndex)));
   }

   public void dataUpdated()
   {
      setModified(true);
      table.setModel(new DatatoolDbTableModel(db, this));

      updateColumnHeaders();
      repaint();
   }

   protected DatatoolDb db;

   protected RowHeaderComponent rowHeaderComponent;

   private boolean isModified = false;

   protected DatatoolGUI gui;

   private JTable table;

   private JScrollPane sp;

   private UndoManager undoManager;

   public static final int STRING_MIN_WIDTH=300;

   public static final int DEFAULT_ROW_HEIGHT=100;
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
      return db.getHeader(col).getTitle();
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
      switch (db.getHeader(column).getType())
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
      return db.getValue(row, col);
   }

   public void setValueAt(Object value, int row, int col)
   {
      panel.updateCell(row, col, value.toString());
      fireTableCellUpdated(row, col);
   }

   public boolean isCellEditable(int row, int column)
   {
      return (db.getHeader(column).getType() != DatatoolDb.TYPE_STRING);
   }

}

class DatatoolTableHeader extends JTableHeader
{
   private DatatoolDbPanel panel;

   private JLabel rendererComponent;

   private int fromIndex=-1;

   public DatatoolTableHeader(TableColumnModel model, 
     DatatoolDbPanel p)
   {
      super(model);
      panel = p;

      rendererComponent = new JLabel();
      rendererComponent.setBorder(BorderFactory.createRaisedBevelBorder());

      model.getSelectionModel().addListSelectionListener(
         new ListSelectionListener()
      {
         public void valueChanged(ListSelectionEvent event)
         {
            if (!event.getValueIsAdjusting())
            {
               panel.selectionUpdated();
            }
         }
      });

      addMouseListener(new MouseAdapter()
      {
         public void mouseClicked(MouseEvent event)
         {
            int col = ((JTableHeader)event.getSource())
                 .columnAtPoint(event.getPoint());

            int clickCount = event.getClickCount();

            if (clickCount == 1)
            {
               panel.selectColumn(col);
            }
            else if (clickCount == 2)
            {
               panel.requestHeaderEditor(col);

               event.consume();
            }
         }

         public void mousePressed(MouseEvent event)
         {
            fromIndex =((JTableHeader)event.getSource())
                 .columnAtPoint(event.getPoint());
         }

         public void mouseReleased(MouseEvent event)
         {
            if (fromIndex != -1)
            {
               int toIndex =((JTableHeader)event.getSource())
                    .columnAtPoint(event.getPoint());

               if (toIndex != -1 && fromIndex != toIndex)
               {
                  panel.moveColumn(fromIndex, toIndex);
               }
            }

            fromIndex = -1;
         }
      });

      setDefaultRenderer(new DefaultTableCellRenderer()
      {
         public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus,
            int row, int column)
         {
            rendererComponent.setText(value.toString());

            if (table == null) return rendererComponent;

            if (table.getSelectedColumn() == column)
            {
               rendererComponent.setBackground(table.getSelectionBackground());
               rendererComponent.setOpaque(true);
            }
            else
            {
               rendererComponent.setOpaque(false);
            }

            return rendererComponent;
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

      DatatoolHeader header = panel.db.getHeader(idx);

      if (header == null)
      {
         return null;
      }

      return DatatoolTk.getLabelWithValues("header.tooltip_format", 
         header.getKey(), DatatoolHeader.TYPE_LABELS[header.getType()+1]);
   }

}
