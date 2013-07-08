package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;
import javax.swing.table.TableColumn;

import com.dickimawbooks.datatooltk.*;

public class InsertColumnEdit extends AbstractUndoableEdit
{
   public InsertColumnEdit(DatatoolDbPanel panel,
     DatatoolHeader header)
   {
      super();

      this.panel = panel;

      selectedIdx = panel.getViewSelectedColumn();

      if (panel.getColumnCount() == 0)
      {
         undoInfo = DatatoolTk.getLabelWithValue("info.empty_db",
           DatatoolTk.getLabel("edit")+"->"
            + DatatoolTk.getLabel("edit.column"));
      }
      else
      {
         undoInfo = "";
      }

      int n = panel.db.getColumnCount();
      column = new DatatoolColumn(header, n, panel.getRowCount());

      panel.db.insertColumn(column);
      viewColumn = panel.insertViewColumn(n);
      panel.dataUpdated();
      panel.updateTools();

      if (panel.getRowCount() == 0)
      {
         redoInfo = DatatoolTk.getLabelWithValue("info.no_rows",
           DatatoolTk.getLabel("edit")+"->"
            + DatatoolTk.getLabel("edit.row"));
      }
      else
      {
         redoInfo = "";
      }

      panel.setInfo(redoInfo);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.removeColumn(column);
      panel.removeViewColumn(viewColumn);
      panel.dataUpdated();
      panel.selectViewColumn(selectedIdx);
      panel.updateTools();
      panel.setInfo(undoInfo);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.insertColumn(column);
      panel.addViewColumn(viewColumn);
      panel.dataUpdated();
      panel.updateTools();

      panel.setInfo(redoInfo);
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolDbPanel panel;
   private int selectedIdx;

   private DatatoolColumn column;

   private TableColumn viewColumn;

   private String undoInfo, redoInfo;

   private static final String name = DatatoolTk.getLabel("undo.add_column");
}
