/*
    Copyright (C) 2024 Nicola L.C. Talbot
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

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.datatooltk.Datum;

/**
 * Cell editor for datum value.
 */

public class DatumCellEditor extends DefaultCellEditor
{
   private JPanel panel;

   public DatumCellEditor()
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

      DatumType type = DatumType.UNKNOWN;
      String text;

      if (value instanceof Datum)
      {
         Datum datum = (Datum)value;
         type = datum.getDatumType();
         text = datum.getText();
      }
      else
      {
         text = value.toString();
      }

      JTextField textField = 
        (JTextField)super.getTableCellEditorComponent(table,
           text, isSelected, row, column);

      textField.setHorizontalAlignment(JTextField.TRAILING);

      panel.add(textField, BorderLayout.NORTH);

      return panel;
   }
}
