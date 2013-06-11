package com.dickimawbooks.datatooltk.gui;

import java.util.EventObject;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.text.*;

public class DbNumericalCellEditor extends DefaultCellEditor
{
   private JPanel panel;

   public DbNumericalCellEditor()
   {
      super(new JTextField());

      panel = new JPanel(new BorderLayout());
   }

   public JTextField getTextField()
   {
      return (JTextField)getComponent();
   }

   public Component getTableCellEditorComponent(JTable table,
     Object value, boolean isSelected, int row, int column)
   {
      panel.removeAll();

      JTextField textField = 
        (JTextField)super.getTableCellEditorComponent(table,
           value, isSelected, row, column);

      textField.setHorizontalAlignment(JTextField.TRAILING);

      panel.add(textField, BorderLayout.NORTH);

      return panel;
   }
}
