package com.dickimawbooks.datatooltk.gui;

import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class InsertRowEdit extends AbstractUndoableEdit
{
   public InsertRowEdit(DatatoolDbPanel panel, int rowIdx)
   {
      super();

      this.panel = panel;
      this.rowIdx = rowIdx;

      if (panel.getRowCount() == 0)
      {
         undoInfo = DatatoolTk.getLabelWithValue("info.no_rows",
           DatatoolTk.getLabel("edit")+"->"
             +DatatoolTk.getLabel("edit.row"));
      }
      else
      {
         undoInfo = "";
      }

      selectedIdx = panel.getSelectedRow();

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
         undoInfo = DatatoolTk.getLabelWithValue("info.no_rows",
           DatatoolTk.getLabel("edit")+"->"
             +DatatoolTk.getLabel("edit.row"));
      }
      else
      {
         undoInfo = "";
      }

      selectedIdx = panel.getSelectedRow();

      initOldTypes();

      panel.db.insertRow(rowIdx, this.row);

      init();
   }

   private void initOldTypes()
   {
      oldTypes = new int[panel.getColumnCount()];

      for (int i = 0, n = panel.getColumnCount(); i < n; i++)
      {
         oldTypes[i] = panel.db.getHeader(i).getType();
      }
   }

   private void init()
   {
      panel.addRowButton();

      newTypes = new int[panel.getColumnCount()];

      typesChanged = false;

      for (int i = 0, n = panel.getColumnCount(); i < n; i++)
      {
         newTypes[i] = panel.db.getHeader(i).getType();

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
         String editLabel = DatatoolTk.getLabel("edit");

         redoInfo = DatatoolTk.getLabelWithValues("info.not_empty_db",
           editLabel+"->"+DatatoolTk.getLabel("edit.column"),
           editLabel+"->"+DatatoolTk.getLabel("edit.row"));
      }
      else
      {
         redoInfo = DatatoolTk.getLabel("info.move_row");
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
      panel.selectRow(selectedIdx);
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

      if (newHeaders != null)
      {
         for (int i = oldTypes.length; i < newTypes.length; i++)
         {
            panel.db.insertColumn(i, newHeaders[i]);
         }
      }

      panel.dataUpdated(typesChanged);
      panel.setInfo(redoInfo);
   }

   public String getPresentationName()
   {
      return name;
   }

   private DatatoolDbPanel panel;
   private int rowIdx, selectedIdx=-1;
   private DatatoolRow row;

   private DatatoolHeader[] newHeaders;
   private int[] oldTypes, newTypes;
   private boolean typesChanged;

   private String undoInfo, redoInfo;

   private static final String name = DatatoolTk.getLabel("undo.add_row");
}
