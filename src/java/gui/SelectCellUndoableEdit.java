package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class SelectCellUndoableEdit extends AbstractUndoableEdit
{
   public SelectCellUndoableEdit(DatatoolDbPanel panel,
     int oldRowIdx, int oldColIdx, DatatoolCell oldCell,
     int newRowIdx, int newColIdx, DatatoolCell newCell)
   {
      super();
      this.panel = panel;
      this.oldRowIdx = oldRowIdx;
      this.oldColIdx = oldColIdx;
      this.oldCell = oldCell;
      this.newRowIdx = newRowIdx;
      this.newColIdx = newColIdx;
      this.newCell = newCell;
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.setCurrentCell(oldRowIdx, oldColIdx, oldCell);
   }

   public void redo() throws CannotRedoException
   {
      panel.setCurrentCell(newRowIdx, newColIdx, newCell);
   }

   private DatatoolDbPanel panel;

   private DatatoolCell oldCell, newCell;

   private int oldRowIdx, oldColIdx, newRowIdx, newColIdx;

   private static final String name = DatatoolTk.getLabel("undo.select_cell");
}
