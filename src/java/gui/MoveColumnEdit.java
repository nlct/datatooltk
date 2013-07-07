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

      // table has already switched rows but not headers

      panel.db.moveColumn(fromIndex, toIndex);
      panel.dataUpdated();
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.moveColumn(toIndex, fromIndex);
      panel.dataUpdated();
   }

   public void redo() throws CannotRedoException
   {
      panel.db.moveColumn(fromIndex, toIndex);
      panel.dataUpdated();
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolDbPanel panel;
   private int fromIndex, toIndex;

   private static final String name = DatatoolTk.getLabel("undo.move_column");
}
