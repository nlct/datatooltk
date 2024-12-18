/*
    Copyright (C) 2013-2024 Nicola L.C. Talbot
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

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.texparserlib.latex.datatool.FileFormatType;
import com.dickimawbooks.texjavahelplib.InvalidSyntaxException;

import com.dickimawbooks.datatooltk.*;

/**
 * Panel used in the main GUI window tabs.
 */

public class DatatoolDbPanel extends JPanel implements ActionListener
{
   public DatatoolDbPanel(DatatoolGUI gui, DatatoolDb db)
   {
      super(new BorderLayout());

      this.db = db;
      this.gui = gui;
      messageHandler = gui.getMessageHandler();

      setName(db.getName());
      buttonTabComponent = new ButtonTabComponent(this);
      initTable();

      infoField = new JTextField();
      infoField.setEditable(false);

      add(infoField, BorderLayout.SOUTH);

      String editLabel = messageHandler.getLabel("menu.edit");

      setInfo(db.getColumnCount() == 0 ?
         messageHandler.getLabelWithValues("info.empty_db",
          String.format("%s->%s", editLabel, 
           messageHandler.getLabel("menu.edit.column"))) :
         messageHandler.getLabelWithValues("info.not_empty_db",
          String.format("%s->%s", 
            editLabel, messageHandler.getLabel("menu.edit.column")),
          String.format("%s->%s", 
            editLabel, messageHandler.getLabel("menu.edit.row"))));

   }

   public MessageHandler getMessageHandler()
   {
      return messageHandler;
   }

   public DatatoolProperties getSettings()
   {
      return gui.getSettings();
   }

   public DatatoolGuiResources getDatatoolGuiResources()
   {
      return gui.getResources();
   }

   private void initTable()
   {
      undoManager = new UndoManager();

      dbTableModel = new DatatoolDbTableModel(db, this);
      table = new JTable(dbTableModel);

      table.setTableHeader(new DatatoolTableHeader(table.getColumnModel(),
         this));

      table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      table.getTableHeader().setReorderingAllowed(false);

      table.setDefaultRenderer(Object.class,
         new DatatoolCellRenderer(gui.getResources(), db));

      table.setDefaultEditor(Datum.class, new DatumCellEditor(gui));

      table.setColumnSelectionAllowed(true);
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      table.addMouseListener(new MouseAdapter()
       {
          @Override
          public void mousePressed(MouseEvent e)
          {
             checkForPopup(e);
          }

          @Override
          public void mouseReleased(MouseEvent e)
          {
             checkForPopup(e);
          }

          @Override
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

             DatumType type = db.getHeader(modelCol).getDatumType();

             setInfo(type == DatumType.STRING ?
               messageHandler.getLabel("info.view_or_edit") :
               messageHandler.getLabel("info.edit"));

             if (evt.getClickCount() == 2)
             {
                if (type == DatumType.STRING)
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

      initPopupMenu();

      sp.addMouseListener(new MouseAdapter()
       {
          @Override
         public void mousePressed(MouseEvent event)
         {
            checkForPopup(event);
         }

         @Override
         public void mouseReleased(MouseEvent event)
         {
            checkForPopup(event);
         }
       });
   }

   private void initPopupMenu()
   {
      popupMenu = new JPopupMenu();

      editCellItem = createJMenuItem("edit_cell");
      editCellItem.setEnabled(false);
      popupMenu.add(editCellItem);

      cellToNullItem = createJMenuItem("cell_to_null");
      cellToNullItem.setEnabled(false);
      popupMenu.add(cellToNullItem);

      popupMenu.add(createJMenuItem("append_col"));

      appendRowItem = createJMenuItem("append_row");
      appendRowItem.setEnabled(false);
      popupMenu.add(appendRowItem);
   }

   private JMenuItem createJMenuItem(String action)
   {
      return createJMenuItem(null, action);
   }

   private JMenuItem createJMenuItem(String parent, String action)
   {
      if (parent == null)
      {
         parent = "tablemenu";
      }
      else
      {
         parent = "tablemenu."+parent;
      }

      return gui.getResources().createJMenuItem(parent, action, this);
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("edit_cell"))
      {
         requestSelectedCellEdit();
      }
      else if (action.equals("cell_to_null"))
      {
         updateCell(DatatoolDb.NULL_VALUE);
      }
      else if (action.equals("append_row"))
      {
         addUndoEdit(new InsertRowEdit(this, db.getRowCount()));
         scrollToViewCell(getRowCount()-1, 0);
      }
      else if (action.equals("append_col"))
      {
         DatatoolHeader header = gui.requestNewHeader(this);

         if (header != null)
         {
            addUndoEdit(new InsertColumnEdit(this, header));
         }
      }
      else
      {
         System.err.println("Unknown action: '"+action+"'");
      }
   }

   public boolean checkForPopup(MouseEvent evt)
   {
      if (evt.isPopupTrigger())
      {
         appendRowItem.setEnabled(getColumnCount() > 0);
         popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
         return true;
      }

      return false;
   }

   public void enableEditItems(boolean hasSelectedRow, boolean hasSelectedColumn)
   {
      gui.enableEditItems(hasSelectedRow, hasSelectedColumn);
      boolean hasBoth = hasSelectedRow && hasSelectedColumn;
      editCellItem.setEnabled(hasBoth);
      cellToNullItem.setEnabled(hasBoth);
   }

   protected void selectionUpdated()
   {
      rowHeaderComponent.updateRowSelection(getViewSelectedRow());
      table.getTableHeader().repaint();

      enableEditItems(getModelSelectedRow() > -1,
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
         messageHandler.getDatatoolTk().debug(e);
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
         messageHandler.getDatatoolTk().debug(e);
      }
   }

   public void save(String filename, FileFormatType fmtType, String version)
   {
      save(new File(filename), fmtType, version);
   }

   public void save(File file, FileFormatType fmtType, String version)
   {
      if (file.exists())
      {
         if (JOptionPane.showConfirmDialog(this,
             messageHandler.getLabelWithValues("message.overwrite_query",
               file.toString()),
             messageHandler.getLabel("message.confirm_overwrite"),
             JOptionPane.YES_NO_OPTION,
             JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION)
         {
            return;
         }
      }

      db.setFile(file);

      save(fmtType, version);
   }

   public void save()
   {
      if (db.getSettings().getOverrideInputFormat())
      {
         db.updateDefaultFormat();
      }

      save(db.getDefaultFormat(), db.getDefaultFileVersion());
   }

   public void save(FileFormatType fmtType, String version)
   {
      if (db.getFile() == null)
      {
         gui.saveAs();
         return;
      }

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

      DatatoolFileWriter writer = new DatatoolFileWriter(this,
        columnIndexes, rowIndexes, fmtType, version);
      writer.execute();
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

      return file == null ? gui.getDefaultUntitled() : file.toString();
   }

   public void sortData()
   {
      // If a column is selected, use that as the default

      int colIdx = getModelSelectedColumn();

      if (!gui.requestSortDialog(db, colIdx))
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

   public void updateCell(Datum datum, int row, int col)
   {
      addUndoEdit(new UpdateCellEdit(this, datum, row, col));
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
            messageHandler.getLabelWithValues("message.cells_updated", count)
         );
         dataUpdated();
      }
      else
      {
         JOptionPane.showMessageDialog(this,
            messageHandler.getLabel("find.not_found"));
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
            messageHandler.getLabel("message.one_cell_updated") :
            messageHandler.getLabelWithValues("message.cells_updated", count)
         );
         dataUpdated();
      }
      else
      {
         JOptionPane.showMessageDialog(this,
            messageHandler.getLabel("find.not_found"));
      }
   }

   public void emptyToNull()
   {
      int numRows = db.getRowCount();
      int numCols = db.getColumnCount();
      int numChanged = 0;

      startCompoundEdit(
         messageHandler.getLabel("undo.empty_to_null"));

      for (int rowIdx = 0; rowIdx < numRows; rowIdx++)
      {
         for (int colIdx = 0; colIdx < numCols; colIdx++)
         {
            Datum datum = db.getDatum(rowIdx, colIdx);

            if (datum.getText().isEmpty())
            {
               updateCell(rowIdx, colIdx, DatatoolDb.NULL_VALUE);
               numChanged++;
            }
         }
      }

      if (numChanged > 0)
      {
         commitCompoundEdit();

         JOptionPane.showMessageDialog(this,
            messageHandler.getLabelWithValues("message.cells_updated",
              numChanged)
         );

         dbTableModel.fireTableDataChanged();
      }
      else
      {
         cancelCompoundEdit();

         JOptionPane.showMessageDialog(this,
           messageHandler.getLabel("message.no_empty_cells_found"));
      }
   }

   public void nullToEmpty()
   {
      int numRows = db.getRowCount();
      int numCols = db.getColumnCount();
      int numChanged = 0;

      startCompoundEdit(
         messageHandler.getLabel("undo.empty_to_null"));

      for (int rowIdx = 0; rowIdx < numRows; rowIdx++)
      {
         for (int colIdx = 0; colIdx < numCols; colIdx++)
         {
            Datum datum = db.getDatum(rowIdx, colIdx);

            if (datum.isNull())
            {
               updateCell(rowIdx, colIdx, "");
               numChanged++;
            }
         }
      }

      if (numChanged > 0)
      {
         commitCompoundEdit();

         JOptionPane.showMessageDialog(this,
            messageHandler.getLabelWithValues("message.cells_updated",
              numChanged)
         );

         dbTableModel.fireTableDataChanged();
      }
      else
      {
         cancelCompoundEdit();

         JOptionPane.showMessageDialog(this,
           messageHandler.getLabel("message.no_null_cells_found"));
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
            messageHandler.getLabel("message.one_cell_updated") :
            messageHandler.getLabelWithValues("message.cells_updated", count)
         );
         dataUpdated();
      }
      else
      {
         JOptionPane.showMessageDialog(this,
            messageHandler.getLabel("find.not_found"));
      }
   }

   public UndoableEdit replaceAllInCell(int row, int col,
     String search, String replacement, boolean isCaseSensitive,
     boolean isRegEx)
   {
      String newText = null;
      String oldText = db.getRow(row).get(col).getText();

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

   public void merge(DatatoolDb otherDb, String key)
    throws InvalidSyntaxException
   {
      int colIdx1 = db.getColumnIndex(key);

      if (colIdx1 == -1)
      {
         throw new InvalidSyntaxException(
           messageHandler.getLabelWithValues("error.db.unknown_key",
             key, db.getName()));
      }

      int colIdx2 = otherDb.getColumnIndex(key);

      if (colIdx2 == -1)
      {
         throw new InvalidSyntaxException(
           messageHandler.getLabelWithValues("error.db.unknown_key",
             key, otherDb.getName()));
      }

      merge(otherDb, colIdx1, colIdx2);
   }

   public void merge(DatatoolDb otherDb, int thisColIdx, int otherColIdx)
   {
      startCompoundEdit(messageHandler.getLabel("undo.merge"));

      for (DatatoolHeader header: otherDb.getHeaders())
      {
         if (db.getHeader(header.getKey()) == null)
         {
            addUndoEdit(new InsertColumnEdit(this, header));
         }
      }

      for (DatatoolRow dbRow : otherDb.getData())
      {
         Datum dbValue = dbRow.get(otherColIdx);

         DatatoolRow thisRow = null;

         int rowIdx;
         int rowCount = db.getRowCount();

         for (rowIdx = 0; rowIdx < rowCount; rowIdx++)
         {
            DatatoolRow row = db.getRow(rowIdx);
            Datum value = row.get(thisColIdx);

            if (value.equals(dbValue))
            {
               thisRow = row;
               break;
            }
         }

         if (thisRow == null)
         {
            addUndoEdit(new InsertRowEdit(this, rowIdx));
         }

         for (int i = 0, n = otherDb.getColumnCount(); i < n; i++)
         {
            DatatoolHeader header = otherDb.getHeader(i);

            int idx = db.getColumnIndex(header.getKey());

            updateCell(dbRow.get(i), rowIdx, idx);
         }
      }

      commitCompoundEdit();
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
         startCompoundEdit(messageHandler.getLabel("undo.add_column"));

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

      startCompoundEdit(messageHandler.getLabel("undo.add_column"));

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
         enableEditItems(modelRow > -1, modelCol > -1);
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
         enableEditItems(viewRow > -1, viewCol > -1);
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
         enableEditItems(row > -1, col > -1);
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

      enableEditItems(viewRow > -1, viewCol > -1);

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

      enableEditItems(row > -1, col > -1);

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
                gui.getCellWidth(header.getDatumType())));
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
         messageHandler.getLabel("message.input_database_name"), currentName);

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
         messageHandler.error(gui,
            messageHandler.getLabelWithValues("error.invalid_name", newName));
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
      return getSettings().getPerl();
   }

   public DatatoolDb getDatabase()
   {
      return db;
   }

   public DatatoolGUI getDatatoolGUI()
   {
      return gui;
   }

   public DatatoolGuiResources getResources()
   {
      return gui.getResources();
   }

   protected DatatoolDb db;

   protected RowHeaderComponent rowHeaderComponent;

   private boolean isModified = false;

   private MessageHandler messageHandler;

   protected DatatoolGUI gui;

   protected JTable table;
   protected DatatoolDbTableModel dbTableModel;

   private JScrollPane sp;

   private UndoManager undoManager;

   private CompoundEdit compoundEdit = null;

   private ButtonTabComponent buttonTabComponent;

   private JTextField infoField;

   private JPopupMenu popupMenu;
   private JMenuItem editCellItem, cellToNullItem, appendRowItem;
}

class DatatoolDbTableModel extends AbstractTableModel
{
   public DatatoolDbTableModel(DatatoolDb db, DatatoolDbPanel panel)
   {
      super();
      this.db = db;
      this.panel = panel;
   }

   @Override
   public String getColumnName(int col)
   {
      return db.getHeader(col).getTitle();
   }

   @Override
   public int getRowCount()
   {
      return db.getRowCount();
   }

   @Override
   public int getColumnCount()
   {
      return db.getColumnCount();
   }

   @Override
   public Class<?> getColumnClass(int column)
   {
      return Datum.class;
   }

   public int getRowIndex(int viewIndex)
   {
      return panel.table.convertRowIndexToModel(viewIndex);
   }

   public int getColumnIndex(int viewIndex)
   {
      return panel.table.convertColumnIndexToModel(viewIndex);
   }

   @Override
   public Object getValueAt(int row, int col)
   {
      return db.getRow(getRowIndex(row)).get(getColumnIndex(col));
   }

   @Override
   public void setValueAt(Object value, int row, int col)
   {
      if (value instanceof Datum)
      {
         panel.updateCell((Datum)value, row, col);
      }
      else
      {
         panel.updateCell(row, col, value.toString());
      }

      fireTableCellUpdated(row, col);
   }

   @Override
   public boolean isCellEditable(int row, int column)
   {
      if (db.getHeader(column).getDatumType() == DatumType.STRING)
      {
         return panel.getSettings().isStringCellEditable();
      }
      else
      {
         return true;
      }
   }

   private DatatoolDb db;
   private DatatoolDbPanel panel;
}

class DatatoolTableHeader extends JTableHeader
{
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
         @Override
         public void mouseClicked(MouseEvent event)
         {
            int viewCol = ((JTableHeader)event.getSource())
                 .columnAtPoint(event.getPoint());

            int clickCount = event.getClickCount();

            panel.setInfo("");

            if (clickCount == 1)
            {
               panel.setInfo(panel.getMessageHandler().getLabel(
                 "info.edit_header"));
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
            if (!panel.checkForPopup(event))
            {
               JTableHeader header = (JTableHeader)event.getSource();

               fromIndex = header.columnAtPoint(event.getPoint());

               if (fromIndex != -1)
               {
                  panel.selectViewColumn(fromIndex);

                  header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
               }
            }
         }

         public void mouseReleased(MouseEvent event)
         {
            if (!panel.checkForPopup(event))
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

      return panel.getMessageHandler().getLabelWithValues(
        "header.tooltip_format", header.getKey(),
        panel.getResources().getTypeLabel(header.getDatumType()));
   }

   private DatatoolDbPanel panel;

   private int fromIndex=-1, mouseOverIndex=-1;

   private JLabel rendererComponent;

   private MoveIndicatorIcon moveRightIcon = null;
   private MoveIndicatorIcon moveLeftIcon = null;

}

class ButtonTabComponent extends JPanel
   implements ActionListener,MouseListener
{
   public ButtonTabComponent(final DatatoolDbPanel panel)
   {
      super(new FlowLayout(FlowLayout.LEFT, 2, 0));

      this.panel = panel;
      label = new JLabel(panel.getName());
      button = panel.getDatatoolGuiResources().createActionButton
        ("button", "close_panel", true, this, null, true);
      label.setToolTipText(panel.getToolTipText());

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
         panel.setInfo(panel.getMessageHandler().getLabel("info.edit_name"));
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
      label.setToolTipText(panel.getToolTipText());
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
   private DatumCellRenderer strCellRenderer, numCellRenderer,
    currCellRenderer, nullCellRenderer, temporalCellRenderer;

   public DatatoolCellRenderer(DatatoolGuiResources resources, DatatoolDb db)
   {
      super();
      this.db = db;
      strCellRenderer = new DatumCellRenderer(resources, DatumType.STRING);
      numCellRenderer = new DatumCellRenderer(resources, DatumType.DECIMAL);
      currCellRenderer = new DatumCellRenderer(resources, DatumType.CURRENCY);
      nullCellRenderer = new DatumCellRenderer(resources, DatumType.UNKNOWN);
      temporalCellRenderer = new DatumCellRenderer(resources, DatumType.DATETIME);
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

      DatumCellRenderer renderer = strCellRenderer;

      if (value instanceof Datum)
      {
         Datum datum = (Datum)value;

         if (datum.isNull())
         {
            renderer = nullCellRenderer;
         }
         else
         {
            switch (datum.getDatumType())
            {
               case CURRENCY:
                  renderer = currCellRenderer;
               break;
               case INTEGER:
               case DECIMAL:
                  renderer = numCellRenderer;
               break;
               case DATE:
               case TIME:
               case DATETIME:
                 renderer = temporalCellRenderer;
            }
         }
      }

      return renderer.getTableCellRendererComponent(table,
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
         g2.drawString(new String(Character.toChars(LEFT_SYMBOL)),
            x, (int)bounds.getHeight()-y);
      }
      else
      {
         g2.drawString(new String(Character.toChars(RIGHT_SYMBOL)), 
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

        int cp = left ? LEFT_SYMBOL : RIGHT_SYMBOL;

        TextLayout layout = new TextLayout(new String(Character.toChars(cp)),
           font, frc);

        Rectangle2D bounds = layout.getBounds();

        width = (int)Math.ceil(bounds.getX()+bounds.getWidth());
        height = (int)Math.ceil(bounds.getY()+bounds.getHeight());
        
        g2.dispose();
      }
   }

   private static final int RIGHT_SYMBOL = 0x21B7;

   private static final int LEFT_SYMBOL = 0x21B6;

   private boolean left;

   private int width = 12, height = 10;

   private Font font;

}

