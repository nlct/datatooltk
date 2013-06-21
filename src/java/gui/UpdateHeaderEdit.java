package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class UpdateHeaderEdit extends AbstractUndoableEdit
{
   public UpdateHeaderEdit(DatatoolDbPanel panel, int col,
     DatatoolHeader header)
   {
      super();
      this.panel = panel;
      this.col = col;
      this.newHeader = header;
      this.oldHeader = panel.db.getHeader(col);

      panel.db.setHeader(col, newHeader);
      panel.setModified(true);
      panel.updateColumnHeader(col);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.setHeader(col, oldHeader);
      panel.setModified(true);
      panel.updateColumnHeader(col);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.setHeader(col, newHeader);
      panel.setModified(true);
      panel.updateColumnHeader(col);
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolDbPanel panel;
   private int col;
   private DatatoolHeader oldHeader, newHeader;
   private static final String name = DatatoolTk.getLabel("undo.header_edit");
}
