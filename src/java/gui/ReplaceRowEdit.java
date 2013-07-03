package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class ReplaceEdit extends AbstractUndoableEdit
{
   public ReplaceEdit(DatatoolDbPanel panel, 
     int row, DatatoolRow newRow)
   {
      super();
      this.panel = panel;
      this.row = row;

      this.newRow = newRow;
      this.oldRow = panel.db.getRow(row);

      panel.db.replaceRow(row, newRow);

   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.setModified(true);
      panel.db.replaceRow(row, oldRow);
   }

   public void redo() throws CannotRedoException
   {
      panel.setModified(true);
      panel.db.replaceRow(row, newRow);
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
