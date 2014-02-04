/*
    Copyright (C) 2013 Nicola L.C. Talbot
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
package com.dickimawbooks.datatooltk.gui;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextLayout;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.undo.*;
import javax.swing.event.*;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import javax.swing.border.Border;

import com.dickimawbooks.datatooltk.*;

/**
 * Panel used in the main GUI window tabs.
 */

public class DatatoolDbPanel extends JPanel
{
   public DatatoolDbPanel(DatatoolGUI gui, DatatoolDb db)
   {
      super(new BorderLayout());

      this.db = db;
      this.gui = gui;
      setName(db.getName());
      buttonTabComponent = new ButtonTabComponent(this);

      initTable();

      infoField = new JTextField();
      infoField.setEditable(false);

      add(infoField, BorderLayout.SOUTH);

      String editLabel = DatatoolTk.getLabel("edit");

      setInfo(db.getColumnCount() == 0 ?
         DatatoolTk.getLabelWithValue("info.empty_db",
          editLabel+"->"+DatatoolTk.getLabel("edit.column")) :
         DatatoolTk.getLabelWithValues("info.not_empty_db",
          editLabel+"->"+DatatoolTk.getLabel("edit.column"),
          editLabel+"->"+DatatoolTk.getLabel("edit.row")));
   }

   private void initTable()
   {
      undoManager = new UndoManager();

      table = new JTable(new DatatoolDbTableModel(db, this))
      {
         public Object getValueAt(int row, int column)
         {
            return db.getRow(row).get(column);
         }
      };

      table.setTableHeader(new DatatoolTableHeader(table.getColumnModel(),
         this));

      table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      table.getTableHeader().setReorderingAllowed(false);

      table.setDefaultEditor(Object.class, new DbNumericalCellEditor());
      table.setDefaultRenderer(Object.class, new DatatoolCellRenderer(db));

      table.setColumnSelectionAllowed(true);
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      table.addMouseListener(new MouseAdapter()
       {
          public void mouseClicked(MouseEvent evt)
          {
             int viewRow = table.getSelectedRow();
             int viewCol = table.getSelectedColumn();

             rowHeaderComponent.updateRowSelection(viewRow);
             table.getTableHeader().repaint();

             if (viewRow == -1 || viewCol == -1)
             {
                // nothing selected

                setInfo("");
                return;
             }

             int modelRow = getModelSelectedRow();
             int modelCol = getModelSelectedColumn();

             int type = db.getHeader(modelCol).getType();

             setInfo(type == DatatoolDb.TYPE_STRING ?
               DatatoolTk.getLabel("info.view_or_edit") :
               DatatoolTk.getLabel("info.edit"));

             if (evt.getClickCount() == 2)
             {
                if (type == DatatoolDb.TYPE_STRING)
                {
                   requestCellEditor(modelRow, modelCol);
                }
                else
                {
                   if (!table.editCellAt(viewRow, viewCol))
                   {
                      requestCellEditor(modelRow, modelCol);
                   }
                }
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

      updateTableSettings();

      updateColumnHeaders();
   }

   protected void selectionUpdated()
   {
      rowHeaderComponent.updateRowSelection(getViewSelectedRow());
      table.getTableHeader().repaint();

      gui.enableEditItems(getModelSelectedRow() > -1,
        getModelSelectedColumn() > -1);
   }

   public void updateTableSettings()
   {
      table.setFont(gui.getCellFont());

      FontMetrics fm = table.getFontMetrics(table.getFont());

      table.setRowHeight(gui.getCellHeight()*(fm.getHeight()+fm.getLeading()));

      rowHeaderComponent.revalidate();
   }

   public int getViewSelectedRow()
   {
      return table.getSelectedRow();
   }

   public int getViewSelectedColumn()
   {
      return table.getSelectedColumn();
   }

   public int getModelSelectedRow()
   {
      return table.convertRowIndexToModel(table.getSelectedRow());
   }

   public int getModelSelectedColumn()
   {
      return table.convertColumnIndexToModel(table.getSelectedColumn());
   }

   public synchronized void addUndoEvent(UndoableEditEvent event)
   {
      addUndoEdit(event.getEdit());
   }

   public synchronized void addUndoEdit(UndoableEdit edit)
   {
      if (compoundEdit == null)
      {
         undoManager.addEdit(edit);
         gui.updateUndoRedoItems(this, edit.getPresentationName(), null);
         setModified(true);
      }
      else
      {
         compoundEdit.addEdit(edit);
      }
   }

   public synchronized void startCompoundEdit()
   {
      compoundEdit = new CompoundEdit();
   }

   public synchronized void startCompoundEdit(final String name)
   {
      compoundEdit = new CompoundEdit()
      {
         public String getPresentationName()
         {
            return name;
         }
      };
   }

   public synchronized void commitCompoundEdit()
   {
      commitCompoundEdit(compoundEdit.getPresentationName());
   }

   public synchronized void commitCompoundEdit(String editName)
   {
      compoundEdit.end();
      undoManager.addEdit(compoundEdit);
      gui.updateUndoRedoItems(this, editName, null);
      setModified(true);
      compoundEdit = null;
   }

   public synchronized void cancelCompoundEdit()
   {
      compoundEdit = null;
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
         int[] columnIndexes = new int[db.getColumnCount()];
         int[] rowIndexes = new int[db.getRowCount()];

         for (int i = 0; i < columnIndexes.length; i++)
         {
            columnIndexes[i] = table.convertColumnIndexToView(i);
         }

         for (int i = 0; i < rowIndexes.length; i++)
         {
            rowIndexes[i] = table.convertRowIndexToView(i);
         }

         db.save(columnIndexes, rowIndexes);
         setModified(false);
         gui.addRecentFile(db.getFile());
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
      buttonTabComponent.updateLabel();
   }

   public String getToolTipText()
   {
      File file = db.getFile();

      return file == null ? null : file.toString();
   }

   public void sortData()
   {
      // If a column is selected, use that as the default

      int colIdx = getModelSelectedColumn();

      if (colIdx > -1)
      {
         db.setSortColumn(colIdx);
      }

      if (!gui.requestSortDialog(db))
      {
         return;
      }

      gui.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      addUndoEdit(new SortEdit(this));
      gui.setCursor(Cursor.getDefaultCursor());
   }

   public void shuffleData(Random random)
   {
      gui.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      addUndoEdit(new ShuffleEdit(this, random));
      gui.setCursor(Cursor.getDefaultCursor());
   }

   public void requestSelectedHeaderEditor()
   {
      int colIndex = getModelSelectedColumn();

      if (colIndex > -1)
      {
         requestHeaderEditor(colIndex);
      }
   }

   public void requestHeaderEditor(int colIdx)
   {
      DatatoolHeader header = gui.requestHeaderEditor(colIdx, this);

      if (header != null)
      {
         addUndoEdit(new UpdateHeaderEdit(this, colIdx, header));
      }
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
      requestCellEditor(getModelSelectedRow(),
           getModelSelectedColumn());
   }

   public void updateCell(String text)
   {
      updateCell(getModelSelectedRow(),
        getModelSelectedColumn(), text);
   }

   public void updateCell(int row, int col, String text)
   {
      addUndoEdit(new UpdateCellEdit(this, row, col, text));
   }

   public synchronized void replaceRow(int index, DatatoolRow row)
   {
      addUndoEdit(new ReplaceRowEdit(this, index, row));
   }

   public void replaceAllInRow(int row, String search,
     String replacement, boolean isCaseSensitive, boolean isRegEx)
   {
      int count = 0;
      CompoundEdit ce = new CompoundEdit();

      for (int i = 0, n = getColumnCount(); i < n; i++)
      {
         UndoableEdit edit = replaceAllInCell(row, i, search,
            replacement, isCaseSensitive, isRegEx);

         if (edit != null)
         {
            ce.addEdit(edit);
            count++;
         }
      }

      if (count > 0)
      {
         ce.end();
         addUndoEdit(ce);
         JOptionPane.showMessageDialog(this,
            count == 1 ?
            DatatoolTk.getLabel("message.one_cell_updated") :
            DatatoolTk.getLabelWithValue("message.cells_updated", count)
         );
         dataUpdated();
      }
      else
      {
         JOptionPane.showMessageDialog(this,
            DatatoolTk.getLabel("find.not_found"));
      }
   }

   public void replaceAllInColumn(int column, String search,
     String replacement, boolean isCaseSensitive, boolean isRegEx)
   {
      int count = 0;
      CompoundEdit ce = new CompoundEdit();

      for (int i = 0, n = getRowCount(); i < n; i++)
      {
         UndoableEdit edit = replaceAllInCell(i, column, search,
            replacement, isCaseSensitive, isRegEx);

         if (edit != null)
         {
            ce.addEdit(edit);
            count++;
         }
      }

      if (count > 0)
      {
         ce.end();
         addUndoEdit(ce);
         JOptionPane.showMessageDialog(this,
            count == 1 ?
            DatatoolTk.getLabel("message.one_cell_updated") :
            DatatoolTk.getLabelWithValue("message.cells_updated", count)
         );
         dataUpdated();
      }
      else
      {
         JOptionPane.showMessageDialog(this,
            DatatoolTk.getLabel("find.not_found"));
      }
   }

   public void replaceAll(String search, String replacement,
      boolean isCaseSensitive, boolean isRegEx)
   {
      int count = 0;
      CompoundEdit ce = new CompoundEdit();

      for (int row = 0, rowCount = getRowCount(); row < rowCount; row++)
      {
         for (int column = 0, columnCount = getColumnCount();
              column < columnCount; column++)
         {
            UndoableEdit edit = replaceAllInCell(row, column, search,
               replacement, isCaseSensitive, isRegEx);

            if (edit != null)
            {
               ce.addEdit(edit);
               count++;
            }
         }
      }

      if (count > 0)
      {
         ce.end();
         addUndoEdit(ce);
         JOptionPane.showMessageDialog(this,
            count == 1 ?
            DatatoolTk.getLabel("message.one_cell_updated") :
            DatatoolTk.getLabelWithValue("message.cells_updated", count)
         );
         dataUpdated();
      }
      else
      {
         JOptionPane.showMessageDialog(this,
            DatatoolTk.getLabel("find.not_found"));
      }
   }

   public UndoableEdit replaceAllInCell(int row, int col,
     String search, String replacement, boolean isCaseSensitive,
     boolean isRegEx)
   {
      String newText = null;
      String oldText = db.getRow(row).get(col);

      if (isRegEx)
      {
         Pattern pattern = isCaseSensitive ? Pattern.compile(search) :
            Pattern.compile(search, Pattern.CASE_INSENSITIVE);

         newText = pattern.matcher(oldText).replaceAll(replacement);
      }
      else
      {
         String matcherText = oldText;

         if (!isCaseSensitive)
         {
            matcherText = matcherText.toLowerCase();
         }

         StringBuilder builder = new StringBuilder(matcherText.length());

         int index;
         int pos = 0;

         int searchLength = search.length();

         while ((index = matcherText.indexOf(search, pos)) != -1)
         {
            builder.append(oldText.substring(pos, index));
            builder.append(replacement);
            pos = index + searchLength;
         }

         if (pos < oldText.length())
         {
            builder.append(oldText.substring(pos));
         }

         newText = builder.toString();
      }

      return oldText.equals(newText) ? null :
         new UpdateCellEdit(this, row, col, newText);
   }

   public void requestNewColumnAfter()
   {
      DatatoolHeader header = gui.requestNewHeader(this);

      if (header == null)
      {
         return;
      }

      int colIdx = getViewSelectedColumn()+1;

      if (colIdx > 0)
      {
         startCompoundEdit(DatatoolTk.getLabel("undo.add_column"));

         addUndoEdit(new InsertColumnEdit(this, header));

         int fromIndex = table.getColumnCount()-1;
         table.moveColumn(fromIndex, colIdx);
         addUndoEdit(new MoveColumnEdit(this, fromIndex, colIdx));

         commitCompoundEdit();
      }
      else
      {
         addUndoEdit(new InsertColumnEdit(this, header));
      }
   }

   public void requestNewColumnBefore()
   {
      DatatoolHeader header = gui.requestNewHeader(this);

      if (header == null)
      {
         return;
      }

      startCompoundEdit(DatatoolTk.getLabel("undo.add_column"));

      addUndoEdit(new InsertColumnEdit(this, header));

      int colIdx = getViewSelectedColumn();

      if (colIdx < 0)
      {
         colIdx = 0;
      }

      int fromIndex = table.getColumnCount()-1;
      table.moveColumn(fromIndex, colIdx);
      addUndoEdit(new MoveColumnEdit(this, fromIndex, colIdx));

      commitCompoundEdit();
   }

   public TableColumn insertViewColumn(int modelIdx)
   {
      DefaultTableColumnModel model = 
         (DefaultTableColumnModel)table.getColumnModel();

      TableColumn newColumn = new TableColumn();
      model.addColumn(newColumn);
      newColumn.setModelIndex(modelIdx);

      return newColumn;
   }

   public void addViewColumn(TableColumn column)
   {
      DefaultTableColumnModel model = 
         (DefaultTableColumnModel)table.getColumnModel();
      model.addColumn(column);
   }

   public void removeViewColumn(TableColumn column)
   {
      DefaultTableColumnModel model = 
         (DefaultTableColumnModel)table.getColumnModel();
      model.removeColumn(column);
   }

   protected void addColumn(DatatoolColumn dbColumn, 
     TableColumn tableColumn)
   {
      addViewColumn(tableColumn);
      db.insertColumn(dbColumn);
      dataUpdated();
   }

   public void removeSelectedRow()
   {
      removeRow(getModelSelectedRow());
   }

   public void removeRow(int modelIndex)
   {
      addUndoEdit(new RemoveRowEdit(this, modelIndex));
   }

   public TableColumn getColumn(int viewIdx)
   {
      TableColumnModel model = table.getTableHeader().getColumnModel();
      return model.getColumn(viewIdx);
   }

   public DatatoolColumn removeColumn(int idx)
   {
      TableColumnModel model = table.getTableHeader().getColumnModel();

      int viewIdx = table.convertColumnIndexToView(idx);
      int n = getColumnCount()-1;

      moveViewColumn(viewIdx, n);
      model.removeColumn(model.getColumn(n));
      DatatoolColumn oldCol = db.removeColumn(n);

      dataUpdated();

      return oldCol;
   }

   public void removeSelectedColumn()
   {
      int colIdx = getModelSelectedColumn();
      addUndoEdit(new RemoveColumnEdit(this, colIdx));
   }

   public synchronized void insertRow(int index, DatatoolRow row)
   {
      addUndoEdit(new InsertRowEdit(this, index, row));
   }

   public synchronized void appendRow(DatatoolRow row)
   {
      int index = getRowCount();

      addUndoEdit(new InsertRowEdit(this, index, row));
   }

   public void insertNewRowAfter()
   {
      // insert new row after selected row or after last row if none
      // selected.

      int rowIdx = getModelSelectedRow()+1;

      if (rowIdx == 0)
      {
         rowIdx = db.getRowCount();
      }

      addUndoEdit(new InsertRowEdit(this, rowIdx));
   }

   public void insertNewRowBefore()
   {
      // insert new row before selected row or before first row if none
      // selected.

      int rowIdx = getModelSelectedRow();

      if (rowIdx < 0)
      {
         rowIdx = 0;
      }

      addUndoEdit(new InsertRowEdit(this, rowIdx));
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
      dataUpdated();
   }

   public void fireRowDeleted(int rowIdx)
   {
      ((AbstractTableModel)table.getModel()).fireTableRowsDeleted(rowIdx, rowIdx);
      dataUpdated();
   }

   public void selectModelRow(int modelRow)
   {
      int modelCol = getModelSelectedColumn();

      if (modelRow >= getRowCount() || modelRow < 0)
      {
         gui.enableEditItems(modelRow > -1, modelCol > -1);
         return;
      }

      if (modelCol == -1)
      {
         modelCol = 0;
      }

      selectModelCell(modelRow, modelCol);
   }

   public void selectViewRow(int viewRow)
   {
      int viewCol = getViewSelectedColumn();

      if (viewRow >= getRowCount() || viewRow < 0)
      {
         gui.enableEditItems(viewRow > -1, viewCol > -1);
         return;
      }

      if (viewCol == -1)
      {
         viewCol = 0;
      }

      selectViewCell(viewRow, viewCol);
   }

   public void selectViewColumn(int col)
   {
      int row = getViewSelectedRow();

      if (col >= db.getColumnCount() || col < 0)
      {
         gui.enableEditItems(row > -1, col > -1);
         return;
      }

      if (row == -1)
      {
         row = 0;
      }

      selectViewCell(row, col);
   }

   public void selectViewCell(int viewRow, int viewCol)
   {
      int oldRow = table.getSelectedRow();
      int oldCol = table.getSelectedColumn();

      if (viewRow >= getRowCount())
      {
         viewRow = getRowCount()-1;
      }

      if (viewCol >= getColumnCount())
      {
         viewCol = getColumnCount()-1;
      }

      gui.enableEditItems(viewRow > -1, viewCol > -1);

      if (oldRow == viewRow && oldCol == viewCol)
      {
         return; // already selected
      }

      table.clearSelection();

      if (viewRow > -1)
      {
         table.setRowSelectionInterval(viewRow, viewRow);
      }

      if (viewCol > -1)
      {
         table.setColumnSelectionInterval(viewCol, viewCol);
      }

      if (viewRow != oldRow)
      {
         rowHeaderComponent.updateRowSelection(viewRow);
      }

      if (viewCol != oldCol)
      {
         table.getTableHeader().repaint();
      }
   }

   public void selectModelCell(int row, int col)
   {
      int oldRow = getModelSelectedRow();
      int oldCol = getModelSelectedColumn();

      if (row >= getRowCount())
      {
         row = getRowCount()-1;
      }

      if (col >= getColumnCount())
      {
         col = getColumnCount()-1;
      }

      gui.enableEditItems(row > -1, col > -1);

      if (oldRow == row && oldCol == col)
      {
         return; // already selected
      }

      table.clearSelection();

      oldRow = table.convertRowIndexToView(oldRow);
      oldCol = table.convertColumnIndexToView(oldCol);

      row = table.convertRowIndexToView(row);
      col = table.convertColumnIndexToView(col);

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

   public int getColumnCount()
   {
      return db.getColumnCount();
   }

   public int getRowCount()
   {
      return table.getRowCount();
   }

   public int getRowHeight(int row)
   {
      return table.getRowHeight(row);
   }

   public void scrollToModelCell(int row, int column)
   {
      Rectangle rect = table.getCellRect(
        table.convertRowIndexToView(row),
        table.convertColumnIndexToView(column), true);
      table.scrollRectToVisible(rect);
   }

   public void scrollToViewCell(int row, int column)
   {
      Rectangle rect = table.getCellRect(
        row, column, true);
      table.scrollRectToVisible(rect);
   }

   public void updateTools()
   {
      gui.updateTools();
   }

   public void updateColumnHeader(int column)
   {
      TableColumnModel model = table.getTableHeader().getColumnModel();

      if (column < model.getColumnCount())
      {
         TableColumn tableColumn = model.getColumn(column);

         tableColumn.setHeaderValue(db.getHeader(column).getTitle());
         tableColumn.setIdentifier(db.getHeader(column).getKey());

         sp.getColumnHeader().repaint();
         table.repaint();
      }
   }

   public void updateColumnHeaders()
   {
      updateColumnHeaders(true);
   }

   public void updateColumnHeaders(boolean adjustWidths)
   {
      TableColumnModel model = table.getTableHeader().getColumnModel();

      for (int i = 0, n = getColumnCount(); i < n; i++)
      {
         TableColumn column = model.getColumn(i);
         DatatoolHeader header = db.getHeader(i);

         column.setHeaderValue(header.getTitle());
         column.setIdentifier(header.getKey());

         if (adjustWidths)
         {
            column.setPreferredWidth(Math.max(column.getPreferredWidth(),
                gui.getCellWidth(header.getType())));
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

      for (int i = index, n = getColumnCount(); i < n; i++)
      {
         model.getColumn(i).setHeaderValue(db.getHeader(i).getTitle());
         model.getColumn(i).setIdentifier(db.getHeader(i).getKey());
      }

      sp.getColumnHeader().repaint();
   }

   public void moveColumnEdit(int fromIndex, int toIndex)
   {
      if (fromIndex != toIndex)
      {
         addUndoEdit(new MoveColumnEdit(this, fromIndex, toIndex));
      }
   }

   public void moveViewColumn(int fromIndex, int toIndex)
   {
      if (fromIndex != toIndex)
      {
         int modelFromIndex = table.convertColumnIndexToModel(fromIndex);
         int modelToIndex = table.convertColumnIndexToModel(toIndex);
         db.moveColumn(modelFromIndex, modelToIndex);
         selectViewColumn(toIndex);
         dataUpdated();
      }
   }

   public void moveRow(int fromIndex, int toIndex)
   {
      if (fromIndex != toIndex)
      {
         addUndoEdit(new MoveRowEdit(this, fromIndex, toIndex));
      }
   }

   public void dataUpdated()
   {
      dataUpdated(true);
   }

   public void dataUpdated(boolean adjustWidths)
   {
      setModified(true);
      int rowIdx = getModelSelectedRow();
      int colIdx = getModelSelectedColumn();

      if (rowIdx != -1 && colIdx != -1)
      {
         if (rowIdx > getRowCount()) rowIdx = db.getRowCount()-1;
         if (colIdx > getColumnCount()) colIdx = db.getColumnCount()-1;

         selectModelCell(rowIdx, colIdx);
      }

      updateColumnHeaders(adjustWidths);
      table.revalidate();
      repaint();
   }

   public JComponent getButtonTabComponent()
   {
      return buttonTabComponent;
   }

   public void close()
   {
      gui.close(this);
   }

   public void requestName()
   {
      String currentName = db.getName();

      String newName = JOptionPane.showInputDialog(gui,
         DatatoolTk.getLabel("message.input_database_name"), currentName);

      if (newName == null || currentName.equals(newName))
      {
         return;
      }

      boolean invalid = newName.isEmpty();

      if (!invalid)
      {
         if (newName.indexOf("$") > -1)
         {
            newName = newName.replaceAll("\\x24", "\\\\u0024");

            invalid = true;
         }
         else
         {
            invalid = newName.matches(".*[\\\\%#{}\\&^].*");
         }
      }

      if (invalid)
      {
         DatatoolGuiResources.error(gui,
            DatatoolTk.getLabelWithValue("error.invalid_name", newName));
         return;
      }

      addUndoEdit(new DbNameEdit(this, newName));
   }

   public void requestSelectedTab()
   {
      gui.selectTab(this);
   }

   public void setInfo(String info)
   {
      infoField.setText(info);
   }

   public Object getModelValueAt(int modelRow, int modelColumn)
   {
      return db.getRow(modelRow).get(modelColumn);
   }

   public Object getValueAtView(int viewRow, int viewColumn)
   {
      return db.getRow(table.convertRowIndexToModel(viewRow))
        .get(table.convertColumnIndexToModel(viewColumn));
   }

   public Object getValueAtModel(int modelRow, int modelColumn)
   {
      return db.getRow(modelRow).get(modelColumn);
   }

   public String getPerl()
   {
      return db.getSettings().getPerl();
   }

   protected DatatoolDb db;

   protected RowHeaderComponent rowHeaderComponent;

   private boolean isModified = false;

   protected DatatoolGUI gui;

   protected JTable table;

   private JScrollPane sp;

   private UndoManager undoManager;

   private CompoundEdit compoundEdit = null;

   private ButtonTabComponent buttonTabComponent;

   private JTextField infoField;
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

/*
   public Class<?> getColumnClass(int column)
   {
      switch (db.getHeader(column).getType())
      {
         case DatatoolDb.TYPE_INTEGER:
            return Integer.class;
         case DatatoolDb.TYPE_REAL:
            return Float.class;
         case DatatoolDb.TYPE_CURRENCY:
            return com.dickimawbooks.datatooltk.Currency.class;
      }


      return String.class;
   }
*/

   public int getRowIndex(int viewIndex)
   {
      return panel.table.convertRowIndexToModel(viewIndex);
   }

   public int getColumnIndex(int viewIndex)
   {
      return panel.table.convertColumnIndexToModel(viewIndex);
   }

   public Object getValueAt(int row, int col)
   {
      return db.getRow(getRowIndex(row)).get(getColumnIndex(col));
   }

   public void setValueAt(Object value, int row, int col)
   {
      panel.updateCell(row, col, value.toString());
      fireTableCellUpdated(row, col);
   }

   public boolean isCellEditable(int row, int column)
   {
      return (db.getHeader(column).getType() 
        != DatatoolDb.TYPE_STRING);
   }

}

class DatatoolTableHeader extends JTableHeader
{
   private DatatoolDbPanel panel;

   private int fromIndex=-1, mouseOverIndex=-1;

   private JLabel rendererComponent;

   private MoveIndicatorIcon moveRightIcon = null;
   private MoveIndicatorIcon moveLeftIcon = null;

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
            int viewCol = ((JTableHeader)event.getSource())
                 .columnAtPoint(event.getPoint());

            int clickCount = event.getClickCount();

            panel.setInfo("");

            if (clickCount == 1)
            {
               panel.setInfo(DatatoolTk.getLabel("info.edit_header"));
               panel.selectViewColumn(viewCol);
            }
            else if (clickCount == 2)
            {
               int modelCol = panel.table.convertColumnIndexToModel(
                  viewCol);

               panel.requestHeaderEditor(modelCol);

               event.consume();
            }
         }

         public void mousePressed(MouseEvent event)
         {
            JTableHeader header = (JTableHeader)event.getSource();

            fromIndex = header.columnAtPoint(event.getPoint());

            if (fromIndex != -1)
            {
               panel.selectViewColumn(fromIndex);

               header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
         }

         public void mouseReleased(MouseEvent event)
         {
            JTableHeader header = (JTableHeader)event.getSource();

            if (fromIndex != -1)
            {
               int toIndex = header.columnAtPoint(event.getPoint());

               if (toIndex != -1)
               {
                  panel.moveColumnEdit(fromIndex, toIndex);
               }

               fromIndex = -1;
            }

            mouseOverIndex = -1;

            header.setCursor(Cursor.getDefaultCursor());
         }

      });

      addMouseMotionListener(new MouseMotionListener()
      {
         public void mouseDragged(MouseEvent event)
         {
            if (fromIndex != -1)
            {
               JTableHeader header = (JTableHeader)event.getSource();

               int oldIndex = mouseOverIndex;

               mouseOverIndex = header.columnAtPoint(event.getPoint());

               if (oldIndex != mouseOverIndex)
               {
                  header.repaint();
               }
            }
         }

         public void mouseMoved(MouseEvent event)
         {
         }
      });

      setDefaultRenderer(new DefaultTableCellRenderer()
      {
         public Component getTableCellRendererComponent(JTable table,
            Object value, boolean isSelected, boolean hasFocus,
            int row, int column)
         {
            rendererComponent.setText(value == null ? "" : value.toString());

            rendererComponent.setIcon(null);

            if (table == null) return rendererComponent;

            if (isSelected || (mouseOverIndex == column))
            {
               rendererComponent.setBackground(table.getSelectionBackground());
               rendererComponent.setOpaque(true);

               if (mouseOverIndex == column && column != fromIndex)
               {
                  if (moveRightIcon == null)
                  {
                     moveRightIcon = new MoveIndicatorIcon(false);
                     moveRightIcon.updateBounds(rendererComponent);
                  }

                  if (moveLeftIcon == null)
                  {
                     moveLeftIcon = new MoveIndicatorIcon(true);
                     moveLeftIcon.updateBounds(rendererComponent);
                  }

                  if (column < fromIndex)
                  {
                     rendererComponent.setIcon(moveLeftIcon);
                     rendererComponent.setHorizontalTextPosition(
                       SwingConstants.RIGHT);
                  }
                  else
                  {
                     rendererComponent.setIcon(moveRightIcon);
                     rendererComponent.setHorizontalTextPosition(
                       SwingConstants.LEFT);
                  }
               }
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
      int idx = panel.table.convertColumnIndexToModel
         (columnAtPoint(event.getPoint()));

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
         header.getKey(), DatatoolDb.TYPE_LABELS[header.getType()+1]);
   }

}

class ButtonTabComponent extends JPanel
   implements ActionListener,MouseListener
{
   public ButtonTabComponent(final DatatoolDbPanel panel)
   {
      super(new FlowLayout(FlowLayout.LEFT, 2, 0));

      this.panel = panel;
      label = new JLabel(panel.getName());
      button = DatatoolGuiResources.createActionButton
        ("button", "close_panel", this, null);
      label.setToolTipText(panel.db.getFileName());

      button.setText(null);
      button.setRolloverEnabled(true);
      button.setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
      button.setContentAreaFilled(false);
      button.setFocusable(false);

      add(label);
      add(button);

      setOpaque(false);

      addMouseListener(this);
      label.addMouseListener(this);
   }

   public void mouseClicked(MouseEvent evt)
   {
      panel.requestSelectedTab();

      if (evt.getClickCount() == 2)
      {
         panel.setInfo("");
         panel.requestName();
      }
      else
      {
         panel.setInfo(DatatoolTk.getLabel("info.edit_name"));
      }
   }

   public void mousePressed(MouseEvent evt)
   {
   }

   public void mouseExited(MouseEvent evt)
   {
   }

   public void mouseEntered(MouseEvent evt)
   {
   }

   public void mouseReleased(MouseEvent evt)
   {
   }

   public void updateLabel()
   {
      String name = panel.getName();

      if (panel.isModified())
      {
         name += " *";
      }

      label.setText(name);
      label.setToolTipText(panel.db.getFileName());
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("close_panel"))
      {
         panel.close();
      }
   }

   private DatatoolDbPanel panel;
   private JLabel label;
   private JButton button;
}

class DatatoolCellRenderer implements TableCellRenderer
{
   private DatatoolDb db;
   private DbNumericalCellRenderer numericalCellRenderer;
   private DbCellRenderer cellRenderer;
   private DefaultTableCellRenderer defRenderer;

   public DatatoolCellRenderer(DatatoolDb db)
   {
      super();
      this.db = db;
      numericalCellRenderer = new DbNumericalCellRenderer();
      cellRenderer = new DbCellRenderer();
      defRenderer = new DefaultTableCellRenderer();
   }

   public Component getTableCellRendererComponent(JTable table,
     Object value, boolean isSelected, boolean hasFocus,
     int row, int column)
   {
      int modelIndex = table.convertColumnIndexToModel(column);

      if (modelIndex >= table.getColumnCount())
      {
         return null;
      }

      int type = db.getHeader(modelIndex).getType();

      if (type == DatatoolDb.TYPE_INTEGER || type == DatatoolDb.TYPE_REAL)
      {
         return numericalCellRenderer.getTableCellRendererComponent(table,
           value, isSelected, hasFocus, row, column);
      }
      else if (type == DatatoolDb.TYPE_STRING)
      {
         return cellRenderer.getTableCellRendererComponent(table,
           value, isSelected, hasFocus, row, column);
      }

      return defRenderer.getTableCellRendererComponent(table,
           value, isSelected, hasFocus, row, column);
   }
}

class MoveIndicatorIcon implements Icon
{
   public MoveIndicatorIcon(boolean isLeft)
   {
      font = new Font("Serif", Font.BOLD, 12);
      left = isLeft;
   }

   public int getIconWidth()
   {
      return width;
   }

   public int getIconHeight()
   {
      return height;
   }

   public void paintIcon(Component c, Graphics g, int x, int y)
   {
      Graphics2D g2 = (Graphics2D)g;
      g2.setFont(font);
      g2.setColor(Color.BLACK);

      Rectangle bounds = c.getBounds();

      if (left)
      {
         g2.drawString(leftSymbol, x, (int)bounds.getHeight()-y);
      }
      else
      {
         g2.drawString(rightSymbol, 
           (int)bounds.getWidth()-getIconWidth(), 
           (int)bounds.getHeight()-y);
      }
   }

   public void updateBounds(JComponent c)
   {
      Graphics2D g2 = (Graphics2D)c.getGraphics();

      if (g2 != null)
      {
        g2.setFont(font);
        FontRenderContext frc = g2.getFontRenderContext();
        TextLayout layout = new TextLayout(left ? leftSymbol : rightSymbol,
           font, frc);

        Rectangle2D bounds = layout.getBounds();

        width = (int)Math.ceil(bounds.getX()+bounds.getWidth());
        height = (int)Math.ceil(bounds.getY()+bounds.getHeight());
        
        g2.dispose();
      }
   }

   private static final String rightSymbol = "↷";

   private static final String leftSymbol = "↶";

   private boolean left;

   private int width = 12, height = 10;

   private Font font;
}

