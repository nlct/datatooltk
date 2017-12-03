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
 * Sort data undoable edit.
 */
public class SortEdit extends AbstractUndoableEdit
{
   public SortEdit(DatatoolDbPanel panel)
   {
      super();
      this.panel = panel;

      if (NAME == null)
      {
         NAME = panel.getMessageHandler().getLabel("undo.sort");
      }

      oldData = panel.db.dataToArray();

      panel.db.sort();
      panel.setModified(true);
      panel.repaint();

      newData = panel.db.dataToArray();
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.dataFromArray(oldData);
      panel.setModified(false);
      panel.repaint();
   }

   public void redo() throws CannotRedoException
   {
      panel.db.dataFromArray(newData);
      panel.setModified(false);
      panel.repaint();
   }

   public String getPresentationName()
   {
      return NAME;
   }

   private DatatoolDbPanel panel;

   private DatatoolRow[] oldData, newData;

   private static String NAME=null;
}
