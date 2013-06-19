package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class InsertColumnEdit extends AbstractUndoableEdit
{
   public InsertColumnEdit(DatatoolDbPanel panel,
     DatatoolHeader header, int colIdx)
   {
      super();

      this.panel = panel;

      selectedIdx = panel.getSelectedColumn();

      column = new DatatoolColumn(header, colIdx, panel.getRowCount());

      panel.db.insertColumn(column);
      panel.dataUpdated();
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.removeColumn(column);
      panel.dataUpdated();
      panel.selectColumn(selectedIdx);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.insertColumn(column);
      panel.dataUpdated();
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolDbPanel panel;
   private int selectedIdx;

   private DatatoolColumn column;

   private static final String name = DatatoolTk.getLabel("undo.add_column");
}
