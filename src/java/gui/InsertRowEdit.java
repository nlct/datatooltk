package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class InsertRowEdit extends AbstractUndoableEdit
{
   public InsertRowEdit(DatatoolDbPanel panel, int rowIdx)
   {
      super();

      this.panel = panel;
      this.rowIdx = rowIdx;

      if (panel.getRowCount() == 0)
      {
         undoInfo = DatatoolTk.getLabelWithValue("info.no_rows",
           DatatoolTk.getLabel("edit")+"->"
             +DatatoolTk.getLabel("edit.row"));
      }
      else
      {
         undoInfo = "";
      }

      selectedIdx = panel.getSelectedRow();

      row = panel.db.insertRow(rowIdx);

      init();
   }

   public InsertRowEdit(DatatoolDbPanel panel, int rowIdx, DatatoolRow row)
   {
      super();

      this.panel = panel;
      this.rowIdx = rowIdx;
      this.row = row;

      if (panel.getRowCount() == 0)
      {
         undoInfo = DatatoolTk.getLabelWithValue("info.no_rows",
           DatatoolTk.getLabel("edit")+"->"
             +DatatoolTk.getLabel("edit.row"));
      }
      else
      {
         undoInfo = "";
      }

      selectedIdx = panel.getSelectedRow();

      panel.db.insertRow(rowIdx, this.row);

      init();
   }

   private void init()
   {
      panel.addRowButton();
      panel.fireRowInserted(rowIdx);

      if (panel.getRowCount() == 1)
      {
         String editLabel = DatatoolTk.getLabel("edit");

         redoInfo = DatatoolTk.getLabelWithValues("info.not_empty_db",
           editLabel+"->"+DatatoolTk.getLabel("edit.column"),
           editLabel+"->"+DatatoolTk.getLabel("edit.row"));
      }
      else
      {
         redoInfo = DatatoolTk.getLabel("info.move_row");
      }

      panel.setInfo(redoInfo);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.removeRow(rowIdx);
      panel.removeRowButton();
      panel.fireRowDeleted(rowIdx);
      panel.selectRow(selectedIdx);
      panel.setInfo(undoInfo);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.insertRow(rowIdx, row);
      panel.addRowButton();
      panel.fireRowInserted(rowIdx);
      panel.setInfo(redoInfo);
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolDbPanel panel;
   private int rowIdx, selectedIdx=-1;
   private DatatoolRow row;

   private String undoInfo, redoInfo;

   private static final String name = DatatoolTk.getLabel("undo.add_row");
}
