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
import javax.swing.table.TableColumn;

import com.dickimawbooks.datatooltk.*;

/**
 * Insert a new column undoable edit.
 */
public class InsertColumnEdit extends AbstractUndoableEdit
{
   public InsertColumnEdit(DatatoolDbPanel panel,
     DatatoolHeader header)
   {
      super();

      this.panel = panel;
      MessageHandler messageHandler = panel.getMessageHandler();

      if (NAME == null)
      {
         NAME = messageHandler.getLabel("undo.add_column");
      }

      selectedIdx = panel.getViewSelectedColumn();

      if (panel.getColumnCount() == 0)
      {
         undoInfo = messageHandler.getLabelWithValues("info.empty_db",
           messageHandler.getLabel("edit")+"->"
            + messageHandler.getLabel("edit.column"));
      }
      else
      {
         undoInfo = "";
      }

      int n = panel.db.getColumnCount();
      column = new DatatoolColumn(header, n, panel.getRowCount());

      panel.db.insertColumn(column);
      viewColumn = panel.insertViewColumn(n);
      panel.dataUpdated();
      panel.updateTools();

      if (panel.getRowCount() == 0)
      {
         redoInfo = messageHandler.getLabelWithValues("info.no_rows",
           messageHandler.getLabel("edit")+"->"
            + messageHandler.getLabel("edit.row"));
      }
      else
      {
         redoInfo = "";
      }

      panel.setInfo(redoInfo);
   }

   public boolean canUndo() {return true;}
   public boolean canRedo() {return true;}

   public void undo() throws CannotUndoException
   {
      panel.db.removeColumn(column);
      panel.removeViewColumn(viewColumn);
      panel.dataUpdated();
      panel.selectViewColumn(selectedIdx);
      panel.updateTools();
      panel.setInfo(undoInfo);
   }

   public void redo() throws CannotRedoException
   {
      panel.db.insertColumn(column);
      panel.addViewColumn(viewColumn);
      panel.dataUpdated();
      panel.updateTools();

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
   private int selectedIdx;

   private DatatoolColumn column;

   private TableColumn viewColumn;

   private String undoInfo, redoInfo;

   private static String NAME = null;
}
