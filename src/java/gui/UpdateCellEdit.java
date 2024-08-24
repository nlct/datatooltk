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

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.datatooltk.base.*;

/**
 * Update the contents of a cell undoable edit.
 */
public class UpdateCellEdit extends AbstractUndoableEdit
{
   public UpdateCellEdit(DatatoolDbPanel panel, 
     int row, int col, String newText)
   {
      this(panel, Datum.valueOf(newText, panel.db.getSettings()), row, col);
   }

   public UpdateCellEdit(DatatoolDbPanel panel, Datum newValue, 
     int row, int col)
   {
      super();
      this.panel = panel;
      this.row = row;
      this.col = col;

      this.newValue = newValue;
      this.oldValue = panel.db.getRow(row).get(col);

      MessageHandler messageHandler = panel.getMessageHandler();

      if (NAME == null)
      {
         NAME = messageHandler.getLabel("undo.cell_edit");
      }

      header = panel.db.getHeader(col);
      this.oldType = header.getDatumType();

      panel.db.setValue(row, col, newValue);

      this.newType = header.getDatumType();

      if (DatatoolDb.checkForVerbatim(newValue.getText()))
      {
         messageHandler.warning(messageHandler.getLabelWithValues(
           "warning.verb_detected_in_cell",
           (row+1), (col+1)));
      }
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.setModified(true);
      panel.db.getRow(row).set(col, oldValue);
      header.setType(oldType);

      if (oldType != newType)
      {
         panel.updateColumnHeader(col);
      }

      panel.selectModelCell(row, col);
   }

   public void redo() throws CannotRedoException
   {
      panel.setModified(true);
      panel.db.getRow(row).set(col, newValue);
      header.setType(newType);

      if (oldType != newType)
      {
         panel.updateColumnHeader(col);
      }

      panel.selectModelCell(row, col);
   }

   public String getPresentationName()
   {
      return NAME;
   }

   private DatatoolHeader header;
   private int row, col;
   private DatumType oldType, newType;
   private Datum newValue, oldValue;
   private static String NAME = null;
   private DatatoolDbPanel panel;
}
