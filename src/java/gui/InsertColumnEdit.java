package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class InsertColumnEdit extends AbstractUndoableEdit
{
   public InsertColumnEdit(DatatoolDbPanel panel,
     DatatoolHeader header, int colIdx)
   {
      super();

      this.panel = panel;

      selectedIdx = panel.getModelSelectedColumn();

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

      column = new DatatoolColumn(header, colIdx, panel.getRowCount());

      panel.db.insertColumn(column);
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
      panel.dataUpdated();
      panel.selectColumn(selectedIdx);
      panel.updateTools();
      panel.setInfo(undoInfo);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.insertColumn(column);
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

   private String undoInfo, redoInfo;

   private static final String name = DatatoolTk.getLabel("undo.add_column");
}
