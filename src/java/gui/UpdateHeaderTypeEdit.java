package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class UpdateHeaderTypeEdit extends AbstractUndoableEdit
{
   public UpdateHeaderTypeEdit(DatatoolDbPanel panel, int col, DatatoolHeader header, int oldType, int newType)
   {
      super();
      this.panel = panel;
      this.col = col;
      this.newType = newType;
      this.oldType = oldType;
      this.header = header;

      panel.setModified(true);
      panel.updateColumnHeader(col);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      header.setType(oldType);
      panel.setModified(true);
      panel.updateColumnHeader(col);
   }

   public void redo() throws CannotUndoException
   {
      header.setType(newType);
      panel.setModified(true);
      panel.updateColumnHeader(col);
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolDbPanel panel;
   private int col, oldType, newType;
   private DatatoolHeader header;

   private static final String name = DatatoolTk.getLabel("undo.header_type");

}
