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

import java.util.EventObject;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * Cell editor for data in numerical columns.
 */

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
