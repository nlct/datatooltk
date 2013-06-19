package  com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class RemoveColumnEdit extends AbstractUndoableEdit
{
   public RemoveColumnEdit(DatatoolDbPanel panel, int colIdx)
   {
      super();
      this.panel = panel;

      selectedIdx = panel.getSelectedColumn();

      column = panel.db.removeColumn(colIdx);
      panel.dataUpdated();
      panel.selectColumn(selectedIdx-1);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.insertColumn(column);
      panel.dataUpdated();
      panel.selectColumn(selectedIdx);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.removeColumn(column);
      panel.dataUpdated();
      panel.selectColumn(selectedIdx-1);
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolDbPanel panel;

   private int selectedIdx=-1;

   private DatatoolColumn column;

   private static final String name = DatatoolTk.getLabel("undo.remove_column");
}
