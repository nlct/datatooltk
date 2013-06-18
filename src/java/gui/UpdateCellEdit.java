package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class UpdateCellEdit extends AbstractUndoableEdit
{
   public UpdateCellEdit(DatatoolDbPanel panel, 
     DatatoolCell cell, String newText)
   {
      super();
      this.panel = panel;
      this.cell = cell;
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
   }

   public void redo() throws CannotRedoException
   {
      panel.setModified(true);
      cell.setValue(newText);
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolCell cell;
   private String newText, oldText;
   private static final String name = DatatoolTk.getLabel("undo.cell_edit");
   private DatatoolDbPanel panel;
}
