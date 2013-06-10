// Based on DefaultCellRenderer

package com.dickimawbooks.datatooltk.gui;

import java.util.EventObject;
import java.awt.Component;
import java.awt.event.*;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class DbCellRenderer implements TableCellRenderer
{
   private JTextArea rendererComponent;

   public DbCellRenderer()
   {
      rendererComponent = new JTextArea();

      rendererComponent.setLineWrap(true);
      rendererComponent.setWrapStyleWord(true);
      rendererComponent.setEditable(false);
   }

   public Component getTableCellRendererComponent(JTable table, 
     Object value, boolean isSelected, boolean hasFocus, int row, int column)
   {
      if (table == null)
      {
         return rendererComponent;
      }

      rendererComponent.setText(value.toString());

      if (isSelected)
      {
         rendererComponent.setBackground(table.getSelectionBackground());
         rendererComponent.setForeground(table.getSelectionForeground());
      }
      else
      {
         rendererComponent.setBackground(table.getBackground());
         rendererComponent.setForeground(table.getForeground());
      }

      rendererComponent.setFont(table.getFont());

      return rendererComponent;
   }

}
