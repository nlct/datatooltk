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

import com.dickimawbooks.datatooltk.*;

/**
 * Update the contents of a cell undoable edit.
 */
public class UpdateCellEdit extends AbstractUndoableEdit
{
   public UpdateCellEdit(DatatoolDbPanel panel, 
     int row, int col, String newText)
   {
      super();
      this.panel = panel;
      this.row = row;
      this.col = col;

      this.newText = newText;
      this.oldText = panel.db.getRow(row).get(col);

      header = panel.db.getHeader(col);
      this.oldType = header.getType();

      panel.db.setValue(row, col, newText);

      this.newType = header.getType();

      if (DatatoolDb.checkForVerbatim(newText))
      {
         DatatoolTk.warning(DatatoolTk.getLabelWithValues(
           "warning.verb_detected_in_cell",
           ""+(row+1), ""+(col+1)));
      }
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.setModified(true);
      panel.db.getRow(row).set(col, oldText);
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
      panel.db.getRow(row).set(col, newText);
      header.setType(newType);

      if (oldType != newType)
      {
         panel.updateColumnHeader(col);
      }

      panel.selectModelCell(row, col);
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolHeader header;
   private int row, col, oldType, newType;
   private String newText, oldText;
   private static final String name = DatatoolTk.getLabel("undo.cell_edit");
   private DatatoolDbPanel panel;
}
