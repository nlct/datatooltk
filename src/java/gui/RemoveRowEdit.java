package  com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class RemoveRowEdit extends AbstractUndoableEdit
{
   public RemoveRowEdit(DatatoolDbPanel panel, int rowIdx)
   {
      super();
      this.panel = panel;
      this.rowIdx = rowIdx;

      selectedIdx = panel.getModelSelectedRow();

      row = panel.db.removeRow(rowIdx);
      panel.removeRowButton();
      panel.dataUpdated();
      panel.selectRow(selectedIdx-1);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.insertRow(rowIdx, row);
      panel.addRowButton();
      panel.dataUpdated();
      panel.selectRow(selectedIdx);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.removeRow(rowIdx);
      panel.removeRowButton();
      panel.dataUpdated();
      panel.selectRow(selectedIdx-1);
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolDbPanel panel;

   private int rowIdx, selectedIdx=-1;

   private DatatoolRow row;

   private static final String name = DatatoolTk.getLabel("undo.remove_row");
}
