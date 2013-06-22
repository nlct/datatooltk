package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class UpdateCellEdit extends AbstractUndoableEdit
{
   public UpdateCellEdit(DatatoolDbPanel panel, 
     int row, int col, String newText)
   {
      super();
      this.panel = panel;
      this.row = row;
      this.col = col;

      this.newText = newText;
      this.oldText = panel.db.getRow(row).get(col);

      panel.db.setValue(row, col, newText);

      if (DatatoolDb.checkForVerbatim(newText))
      {
         DatatoolTk.warning(DatatoolTk.getLabel("warning.verb_detected"));
      }
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.setModified(true);
      panel.db.setValue(row, col, oldText);
      panel.selectCell(row, col);
   }

   public void redo() throws CannotRedoException
   {
      panel.setModified(true);
      panel.db.setValue(row, col, newText);
      panel.selectCell(row, col);
   }

   public String getPresentationName()
   {
      return name;
   }

   private int row, col;
   private String newText, oldText;
   private static final String name = DatatoolTk.getLabel("undo.cell_edit");
   private DatatoolDbPanel panel;
}
