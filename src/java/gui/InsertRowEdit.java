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

      selectedIdx = panel.getSelectedRow();

      row = panel.db.insertRow(rowIdx);
      panel.addRowButton();
      panel.fireRowInserted(rowIdx);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.removeRow(rowIdx);
      panel.removeRowButton();
      panel.fireRowDeleted(rowIdx);
      panel.selectRow(selectedIdx);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.insertRow(rowIdx, row);
      panel.addRowButton();
      panel.fireRowInserted(rowIdx);
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolDbPanel panel;
   private int rowIdx, selectedIdx=-1;
   private DatatoolRow row;

   private static final String name = DatatoolTk.getLabel("undo.add_row");
}
