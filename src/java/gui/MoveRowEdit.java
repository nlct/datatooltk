package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class MoveRowEdit extends AbstractUndoableEdit
{
   public MoveRowEdit(DatatoolDbPanel panel, int fromIndex, int toIndex)
   {
      super();
      this.panel = panel;
      this.fromIndex = fromIndex;
      this.toIndex = toIndex;

      panel.db.moveRow(fromIndex, toIndex);
      panel.selectModelRow(toIndex);
      panel.dataUpdated();
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.moveRow(toIndex, fromIndex);
      panel.selectModelRow(fromIndex);
      panel.dataUpdated();
   }

   public void redo() throws CannotRedoException
   {
      panel.db.moveRow(fromIndex, toIndex);
      panel.selectModelRow(toIndex);
      panel.dataUpdated();
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolDbPanel panel;
   private int fromIndex, toIndex;

   private static final String name = DatatoolTk.getLabel("undo.move_row");
}
