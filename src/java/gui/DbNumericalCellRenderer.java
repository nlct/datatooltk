package com.dickimawbooks.datatooltk.gui;

import java.util.EventObject;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class DbNumericalCellRenderer implements TableCellRenderer
{
   private JTextField textField;
   private JPanel panel;

   public DbNumericalCellRenderer()
   {
      textField = new JTextField();
      textField.setEditable(false);
      textField.setHorizontalAlignment(JTextField.TRAILING);
      panel = new JPanel(new BorderLayout());

      panel.add(textField, BorderLayout.NORTH);
   }

   public Component getTableCellRendererComponent(JTable table,
     Object value, boolean isSelected, boolean hasFocus,
     int row, int column)
   {
      if (table == null)
      {
         return textField;
      }

      textField.setText(value.toString());

      if (isSelected)
      {
         textField.setBackground(table.getSelectionBackground());
         textField.setForeground(table.getSelectionForeground());
      }
      else
      {
         textField.setBackground(table.getBackground());
         textField.setForeground(table.getForeground());
      }

      textField.setFont(table.getFont());

      return panel;
   }
}
