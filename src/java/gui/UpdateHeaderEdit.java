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

import com.dickimawbooks.datatooltk.*;

/**
 * Update the header information undoable edit.
 */
public class UpdateHeaderEdit extends AbstractUndoableEdit
{
   public UpdateHeaderEdit(DatatoolDbPanel panel, int col,
     DatatoolHeader header)
   {
      super();
      this.panel = panel;
      this.col = col;
      this.newHeader = header;
      this.oldHeader = panel.db.getHeader(col);

      MessageHandler messageHandler = panel.getMessageHandler();

      if (NAME == null)
      {
         NAME = messageHandler.getLabel("undo.header_edit");
      }

      panel.db.setHeader(col, newHeader);
      panel.setModified(true);
      panel.updateColumnHeader(col);

      if (DatatoolDb.checkForVerbatim(newHeader.getTitle()))
      {
         messageHandler.warning(
            messageHandler.getLabel("warning.verb_detected"));
      }
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.setHeader(col, oldHeader);
      panel.setModified(true);
      panel.updateColumnHeader(col);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.setHeader(col, newHeader);
      panel.setModified(true);
      panel.updateColumnHeader(col);
   }

   public String getPresentationName()
   {
      return NAME;
   }

   private DatatoolDbPanel panel;
   private int col;
   private DatatoolHeader oldHeader, newHeader;
   private static String NAME = null;
}
