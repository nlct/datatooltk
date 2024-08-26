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
import com.dickimawbooks.datatooltk.*;

/**
 * Insert a new row undoable edit.
 */
public class InsertRowEdit extends AbstractUndoableEdit
{
   public InsertRowEdit(DatatoolDbPanel panel, int rowIdx)
   {
      super();

      this.panel = panel;
      this.rowIdx = rowIdx;

      if (panel.getRowCount() == 0)
      {
         MessageHandler messageHandler = panel.getMessageHandler();

         undoInfo = messageHandler.getLabelWithValues("info.no_rows",
           messageHandler.getLabel("menu.edit")+"->"
             +messageHandler.getLabel("menu.edit.row"));
      }
      else
      {
         undoInfo = "";
      }

      selectedIdx = panel.getModelSelectedRow();

      initOldTypes();

      row = panel.db.insertRow(rowIdx);

      init();
   }

   public InsertRowEdit(DatatoolDbPanel panel, int rowIdx, DatatoolRow row)
   {
      super();

      this.panel = panel;
      this.rowIdx = rowIdx;
      this.row = row;

      if (panel.getRowCount() == 0)
      {
         MessageHandler messageHandler = panel.getMessageHandler();

         undoInfo = messageHandler.getLabelWithValues("info.no_rows",
           messageHandler.getLabel("menu.edit")+"->"
             +messageHandler.getLabel("menu.edit.row"));
      }
      else
      {
         undoInfo = "";
      }

      selectedIdx = panel.getModelSelectedRow();

      initOldTypes();

      panel.db.insertRow(rowIdx, this.row);

      init();
   }

   private void initOldTypes()
   {
      oldTypes = new DatumType[panel.getColumnCount()];

      for (int i = 0, n = panel.getColumnCount(); i < n; i++)
      {
         oldTypes[i] = panel.db.getHeader(i).getDatumType();
      }
   }

   private void init()
   {
      MessageHandler messageHandler = panel.getMessageHandler();

      if (NAME == null)
      {
         NAME = messageHandler.getLabel("undo.add_row");
      }

      panel.addRowButton();

      newTypes = new DatumType[panel.getColumnCount()];

      typesChanged = false;

      for (int i = 0, n = panel.getColumnCount(); i < n; i++)
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

      panel.dataUpdated(typesChanged);

      if (panel.getRowCount() == 1)
      {
         String editLabel = messageHandler.getLabel("menu.edit");

         redoInfo = messageHandler.getLabelWithValues("info.not_empty_db",
           editLabel+"->"+messageHandler.getLabel("menu.edit.column"),
           editLabel+"->"+messageHandler.getLabel("menu.edit.row"));
      }
      else
      {
         redoInfo = messageHandler.getLabel("info.move_row");
      }

      panel.setInfo(redoInfo);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.removeRow(rowIdx);
      panel.removeRowButton();

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

      panel.dataUpdated(typesChanged);
      panel.selectModelRow(selectedIdx);
      panel.setInfo(undoInfo);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.insertRow(rowIdx, row);
      panel.addRowButton();

      if (typesChanged)
      {
         for (int i = 0; i < newTypes.length; i++)
         {
            panel.db.getHeader(i).setType(newTypes[i]);
         }
      }

      panel.dataUpdated(typesChanged);
      panel.setInfo(redoInfo);
   }

   public String getPresentationName()
   {
      return NAME;
   }

   public MessageHandler getMessageHandler()
   {
      return panel.getMessageHandler();
   }

   private DatatoolDbPanel panel;
   private int rowIdx, selectedIdx=-1;
   private DatatoolRow row;

   private DatatoolHeader[] newHeaders;
   private DatumType[] oldTypes, newTypes;
   private boolean typesChanged;

   private String undoInfo, redoInfo;

   private static String NAME = null;
}
