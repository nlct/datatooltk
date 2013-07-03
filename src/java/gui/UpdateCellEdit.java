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

      header = panel.db.getHeader(col);
      this.oldType = header.getType();

      panel.db.setValue(row, col, newText);

      this.newType = header.getType();

      if (DatatoolDb.checkForVerbatim(newText))
      {
         DatatoolTk.warning(DatatoolTk.getLabelWithValues(
           "warning.verb_detected_in_cell",
           ""+(row+1), ""+(col+1)));
      }
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.setModified(true);
      panel.db.getRow(row).set(col, oldText);
      header.setType(oldType);

      if (oldType != newType)
      {
         panel.updateColumnHeader(col);
      }

      panel.selectCell(row, col);
   }

   public void redo() throws CannotRedoException
   {
      panel.setModified(true);
      panel.db.getRow(row).set(col, newText);
      header.setType(newType);

      if (oldType != newType)
      {
         panel.updateColumnHeader(col);
      }

      panel.selectCell(row, col);
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolHeader header;
   private int row, col, oldType, newType;
   private String newText, oldText;
   private static final String name = DatatoolTk.getLabel("undo.cell_edit");
   private DatatoolDbPanel panel;
}
