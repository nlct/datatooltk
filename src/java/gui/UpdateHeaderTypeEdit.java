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

import javax.swing.undo.*;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.datatooltk.*;

/**
 * Update the column data type undoable edit.
 */
public class UpdateHeaderTypeEdit extends AbstractUndoableEdit
{
   public UpdateHeaderTypeEdit(DatatoolDbPanel panel, int col, DatatoolHeader header,
     DatumType oldType, DatumType newType)
   {
      super();
      this.panel = panel;
      this.col = col;
      this.newType = newType;
      this.oldType = oldType;
      this.header = header;

      if (NAME == null)
      {
         NAME = panel.getMessageHandler().getLabel("undo.header_type");
      }

      panel.setModified(true);
      panel.updateColumnHeader(col);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      header.setType(oldType);
      panel.setModified(true);
      panel.updateColumnHeader(col);
   }

   public void redo() throws CannotUndoException
   {
      header.setType(newType);
      panel.setModified(true);
      panel.updateColumnHeader(col);
   }

   public String getPresentationName()
   {
      return NAME;
   }

   private DatatoolDbPanel panel;
   private int col;
   private DatumType oldType, newType;
   private DatatoolHeader header;

   private static String NAME = null;

}
