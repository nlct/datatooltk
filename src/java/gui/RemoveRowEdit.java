/*
    Copyright (C) 2013 Nicola L.C. Talbot
    www.dickimaw-books.com

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package  com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

/**
 * Remove row undoable edit.
 */
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
      panel.selectModelRow(selectedIdx-1);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.insertRow(rowIdx, row);
      panel.addRowButton();
      panel.dataUpdated();
      panel.selectModelRow(selectedIdx);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.removeRow(rowIdx);
      panel.removeRowButton();
      panel.dataUpdated();
      panel.selectModelRow(selectedIdx-1);
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
