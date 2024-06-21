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

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.datatooltk.Datum;
import com.dickimawbooks.datatooltk.DatatoolSettings;

/**
 * Cell renderer for data in numerical columns.
 */
public class DbNumericalCellRenderer implements TableCellRenderer
{
   private JTextField textField, typeField, currencyField, numField;
   private JPanel panel;
   private DatatoolSettings settings;
   private JComponent currencyRow;

   public DbNumericalCellRenderer(DatatoolSettings settings)
   {
      this.settings = settings;
      DatatoolGuiResources resources = settings.getDatatoolGuiResources();

      textField = new JTextField();
      textField.setEditable(false);
      textField.setHorizontalAlignment(JTextField.TRAILING);
      panel = new JPanel(new BorderLayout());

      panel.add(textField, BorderLayout.NORTH);

      JComponent mainComp = Box.createVerticalBox();
      panel.add(mainComp, BorderLayout.CENTER);

      JComponent rowComp;

      rowComp = createRow();
      mainComp.add(rowComp);

      rowComp.add(resources.createJLabel("celledit.type"));
      typeField = createField();
      rowComp.add(typeField);

      currencyRow = createRow();
      mainComp.add(currencyRow);

      currencyRow.add(resources.createJLabel("celledit.currency"));
      currencyField = createField();
      currencyRow.add(currencyField);

      rowComp = createRow();
      mainComp.add(rowComp);

      rowComp.add(resources.createJLabel("celledit.numeric"));
      numField = createField();
      rowComp.add(numField);

      mainComp.add(Box.createVerticalGlue());
   }

   protected JComponent createRow()
   {
      JComponent comp = new JPanel(new FlowLayout(FlowLayout.LEADING));

      return comp;
   }

   protected JTextField createField()
   {
      JTextField field = new JTextField();
      field.setEditable(false);
      field.setBorder(BorderFactory.createEmptyBorder());

      return field;
   }

   public Component getTableCellRendererComponent(JTable table,
     Object value, boolean isSelected, boolean hasFocus,
     int row, int column)
   {
      if (table == null)
      {
         return textField;
      }

      DatumType type = DatumType.UNKNOWN;
      String currencySym = null;
      Number num = null;

      if (value instanceof Datum)
      {
         Datum datum = (Datum)value;
         type = datum.getDatumType();

         if (type.isNumeric())
         {
            currencySym = datum.getCurrencySymbol();
            num = datum.getNumber();
         }
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

      typeField.setText(settings.getTypeLabel(type));

      if (currencySym == null)
      {
         currencyField.setText("");
         currencyRow.setVisible(false);
      }
      else
      {
         currencyField.setText(currencySym);
         currencyRow.setVisible(true);
      }

      if (num == null)
      {
         numField.setText("");
      }
      else
      {
         numField.setText(num.toString());
      }

      return panel;
   }
}
