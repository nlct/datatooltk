package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class UpdateCellEdit extends AbstractUndoableEdit
{
   public UpdateCellEdit(DatatoolDbPanel panel, 
     int row, int col,
     DatatoolCell cell, String newText)
   {
      super();
      this.panel = panel;
      this.cell = cell;
      this.row = row;
      this.col = col;
      this.newText = newText;
      this.oldText = cell.getValue();

      this.cell.setValue(newText);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.setModified(true);
      cell.setValue(oldText);
      panel.selectCell(row, col);
   }

   public void redo() throws CannotRedoException
   {
      panel.setModified(true);
      cell.setValue(newText);
      panel.selectCell(row, col);
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolCell cell;
   private int row, col;
   private String newText, oldText;
   private static final String name = DatatoolTk.getLabel("undo.cell_edit");
   private DatatoolDbPanel panel;
}
