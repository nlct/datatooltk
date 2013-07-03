package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class ReplaceRowEdit extends AbstractUndoableEdit
{
   public ReplaceRowEdit(DatatoolDbPanel panel, 
     int row, DatatoolRow newRow)
   {
      super();
      this.panel = panel;
      this.row = row;

      this.newRow = newRow;
      this.oldRow = panel.db.getRow(row);

      panel.db.replaceRow(row, newRow);
      panel.dataUpdated();
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.setModified(true);
      panel.db.replaceRow(row, oldRow);
      panel.dataUpdated();
   }

   public void redo() throws CannotRedoException
   {
      panel.setModified(true);
      panel.db.replaceRow(row, newRow);
      panel.dataUpdated();
   }

   public String getPresentationName()
   {
      return name;
   }

   private int row;
   private DatatoolRow newRow, oldRow;
   private static final String name = DatatoolTk.getLabel("undo.replace_row");
   private DatatoolDbPanel panel;
}
