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
 * Change database name undoable edit.
 */
public class DbNameEdit extends AbstractUndoableEdit
{
   public DbNameEdit(DatatoolDbPanel panel, String name)
   {
      super();
      this.panel = panel;

      presentationName = panel.getMessageHandler().getLabel("undo.edit_name");

      newName = name;
      oldName = panel.db.getName();

      panel.db.setName(newName);
      panel.setName(newName);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.setName(oldName);
      panel.setName(oldName);
      panel.setModified(true);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.setName(newName);
      panel.setName(newName);
      panel.setModified(true);
   }

   public String getPresentationName()
   {
      return presentationName;
   }

   private DatatoolDbPanel panel;

   private String oldName, newName;

   private final String presentationName;
}
