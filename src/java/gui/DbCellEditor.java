// Based on DefaultCellEditor

package com.dickimawbooks.datatooltk.gui;

import java.util.EventObject;
import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class DbCellEditor extends AbstractCellEditor
  implements TableCellEditor
{
   protected JTextArea editorComponent;

   protected int clickCountToStart = 2;

   public DbCellEditor()
   {
      editorComponent = new JTextArea();
      editorComponent.setEditable(true);
      editorComponent.setLineWrap(true);
      editorComponent.setWrapStyleWord(true);
   }

   public Component getComponent()
   {
      return editorComponent;
   }

   public void setClickCountToStart(int count)
   {
      clickCountToStart = count;
   }

   public int getClickCountToStart()
   {
      return clickCountToStart;
   }

   public Object getCellEditorValue()
   {
      return editorComponent.getText();
   }

   public boolean isCellEditable(EventObject anEvent)
   {
      if (anEvent instanceof MouseEvent)
      {
         return ((MouseEvent)anEvent).getClickCount() >= clickCountToStart;
      }

      return true;
   }

   public Component getTableCellEditorComponent(JTable table, 
     Object value, boolean isSelected, int row, int column)
   {
      editorComponent.setText(value.toString());

      return editorComponent;
   }

}
