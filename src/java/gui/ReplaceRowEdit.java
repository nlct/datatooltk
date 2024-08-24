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
 * Replace an entire row undoable edit.
 */
public class ReplaceRowEdit extends AbstractUndoableEdit
{
   public ReplaceRowEdit(DatatoolDbPanel panel, 
     int row, DatatoolRow newRow)
   {
      super();
      this.panel = panel;
      this.row = row;

      this.newRow = newRow;
      this.oldRow = panel.db.getRow(row);

      if (NAME == null)
      {
         NAME = panel.getMessageHandler().getLabel("undo.replace_row");
      }

      oldTypes = new DatumType[panel.getColumnCount()];

      for (int i = 0, n = oldTypes.length; i < n; i++)
      {
         oldTypes[i] = panel.db.getHeader(i).getDatumType();
      }

      panel.db.replaceRow(row, newRow);
      panel.dataUpdated(false);

      newTypes = new DatumType[panel.getColumnCount()];

      typesChanged = false;

      for (int i = 0, n = newTypes.length; i < n; i++)
      {
         newTypes[i] = panel.db.getHeader(i).getDatumType();

         if (!typesChanged && i < oldTypes.length)
         {
            typesChanged = (newTypes[i] != oldTypes[i]);
         }
      }

      // Have new columns been inserted?

      if (newTypes.length > oldTypes.length)
      {
         int n = newTypes.length-oldTypes.length;

         newHeaders = new DatatoolHeader[n];

         for (int i = 0; i < n; i++)
         {
            newHeaders[i] = panel.db.getHeader(oldTypes.length+i);
         }
      }
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.setModified(true);
      panel.db.replaceRow(row, oldRow);

      if (typesChanged)
      {
         for (int i = 0; i < oldTypes.length; i++)
         {
            panel.db.getHeader(i).setType(oldTypes[i]);
         }
      }

      if (newHeaders != null)
      {
         for (int i = newTypes.length-1; i >= oldTypes.length; i--)
         {
            panel.db.removeColumn(i);
         }
      }

      panel.dataUpdated(false);
   }

   public void redo() throws CannotRedoException
   {
      panel.setModified(true);
      panel.db.replaceRow(row, newRow);

      if (typesChanged)
      {
         for (int i = 0; i < newTypes.length; i++)
         {
            panel.db.getHeader(i).setType(newTypes[i]);
         }
      }

      if (newHeaders != null)
      {
         for (int i = oldTypes.length; i < newTypes.length; i++)
         {
            panel.db.insertColumn(i, newHeaders[i]);
         }
      }

      panel.dataUpdated(false);
   }

   public String getPresentationName()
   {
      return NAME;
   }

   private int row;
   private DatumType[] oldTypes, newTypes;
   private boolean typesChanged;
   private DatatoolRow newRow, oldRow;
   private DatatoolDbPanel panel;
   private DatatoolHeader[] newHeaders;

   private static String NAME=null;
}
