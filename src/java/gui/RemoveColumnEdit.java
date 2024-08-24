/*
    Copyright (C) 2013-2024 Nicola L.C. Talbot
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
package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;
import javax.swing.table.TableColumn;

import com.dickimawbooks.datatooltk.base.*;

/**
 * Remove column undoable edit.
 */
public class RemoveColumnEdit extends AbstractUndoableEdit
{
   public RemoveColumnEdit(DatatoolDbPanel panel, int colIdx)
   {
      super();
      this.panel = panel;

      if (NAME == null)
      {
         NAME = panel.getMessageHandler().getLabel("undo.remove_column");
      }

      this.columnIndex = colIdx;

      selectedIdx = panel.getViewSelectedColumn();

      panel.selectViewColumn(selectedIdx-1);
      column = panel.removeColumn(colIdx);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      int n = panel.db.getColumnCount();
      panel.db.insertColumn(column);
      panel.db.moveColumn(n, columnIndex);
      panel.insertViewColumn(n);
      panel.dataUpdated();
      panel.selectViewColumn(selectedIdx);
   }

   public void redo() throws CannotRedoException
   {
      panel.removeColumn(columnIndex);
      panel.selectViewColumn(selectedIdx-1);
   }

   public String getPresentationName()
   {
      return NAME;
   }

   private DatatoolDbPanel panel;

   private int selectedIdx=-1, columnIndex;

   private DatatoolColumn column;

   private static String NAME = null;
}
