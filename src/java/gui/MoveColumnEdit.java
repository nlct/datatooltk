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
package com.dickimawbooks.datatooltk.gui;

import java.util.Vector;
import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

/**
 * Move column undoable edit.
 */
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
