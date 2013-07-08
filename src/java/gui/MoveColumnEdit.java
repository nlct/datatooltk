package com.dickimawbooks.datatooltk.gui;

import java.util.Vector;
import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class MoveColumnEdit extends AbstractUndoableEdit
{
   public MoveColumnEdit(DatatoolDbPanel panel, int fromIndex, int toIndex)
   {
      super();
      this.panel = panel;
      this.fromIndex = fromIndex;
      this.toIndex = toIndex;
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.moveViewColumn(toIndex, fromIndex);
   }

   public void redo() throws CannotRedoException
   {
      panel.moveViewColumn(fromIndex, toIndex);
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolDbPanel panel;
   private int fromIndex, toIndex;

   private static final String name = DatatoolTk.getLabel("undo.move_column");
}
