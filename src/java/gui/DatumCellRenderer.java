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
 * Cell renderer for Datum values.
 */
public class DatumCellRenderer implements TableCellRenderer
{
   private DatatoolGuiResources resources;

   private JTextField typeField, currencyField, numField;
   private Component panel;
   private JComponent datumInfoComponent;
   private JTextComponent textComp;

   private DatumType type;

   private static final Color NULL_BG = Color.LIGHT_GRAY;

   public DatumCellRenderer(DatatoolGuiResources resources, DatumType type)
   {
      this.resources = resources;
      this.type = type;

      switch (type)
      {
         case STRING:
           panel = createStringComp();
         break;
         case UNKNOWN: 
           panel = createNullComp();
         break;      
         default:
           panel = createNumericComp();
      }
   }

   protected JComponent createStringComp()
   {
      JTextArea textArea = new JTextArea();
      textArea.setLineWrap(true);
      textArea.setWrapStyleWord(true);

      textArea.setEditable(false);
      textArea.setOpaque(true);

      textComp = textArea;

      return textComp;
   }

   protected JComponent createNullComp()
   {
      JPanel nullComp = new JPanel();
      nullComp.setOpaque(true);

      nullComp.add(resources.createJLabel("celledit.NULL"));

      return nullComp;
   }

   protected JComponent createNumericComp()
   {
      JComponent numComp = new JPanel(new BorderLayout());
      numComp.setOpaque(true);

      datumInfoComponent = Box.createVerticalBox();
      datumInfoComponent.setOpaque(false);
      numComp.add(datumInfoComponent, BorderLayout.CENTER);

      JComponent rowComp;

      JTextField textField = createField();
      textField.setHorizontalAlignment(JTextField.TRAILING);
      textComp = textField;

      textField.setText("\\DTLcurrency{0.00}");

      numComp.add(textComp, BorderLayout.NORTH);

      rowComp = createRow();
      datumInfoComponent.add(rowComp);

      JLabel typeLabel = resources.createJLabel("celledit.type");
      rowComp.add(typeLabel);
      typeField = createField();
      typeField.setText(resources.getTypeLabel(type));

      rowComp.add(typeField);

      JComponent valueRow = createRow();
      datumInfoComponent.add(valueRow);

      JLabel valueLabel = resources.createJLabel("celledit.numeric");
      valueRow.add(valueLabel);
      numField = createField();
      valueRow.add(numField);

      if (type == DatumType.CURRENCY)
      {
         JComponent currencyRow = createRow();
         datumInfoComponent.add(currencyRow);

         currencyRow.add(resources.createJLabel("celledit.currency"));
         currencyField = createField();

         currencyRow.add(currencyField);
      }

      datumInfoComponent.add(Box.createVerticalGlue());

      datumInfoComponent.setVisible(resources.isCellDatumVisible());

      return numComp;
   }

   protected JComponent createRow()
   {
      return createRow(FlowLayout.LEADING);
   }

   protected JComponent createRow(int align)
   {
      JComponent comp = new JPanel(new FlowLayout(align, 2, 1));
      comp.setOpaque(false);

      return comp;
   }

   protected JTextField createField()
   {
      JTextField field = new JTextField();
      field.setEditable(false);
      field.setBorder(BorderFactory.createEmptyBorder());
      field.setOpaque(false);

      return field;
   }

   public Component getTableCellRendererComponent(JTable table,
     Object value, boolean isSelected, boolean hasFocus,
     int row, int column)
   {
      String text = value.toString();

      if (textComp != null)
      {
         textComp.setText(text);

         if (table != null)
         {
            textComp.setFont(table.getFont());
         }
      }

      if (table == null)
      {
         return panel;
      }

      String currencySym = null;
      Number num = null;
      boolean isNull = false;
      DatumType valType = type;

      if (value instanceof Datum)
      {
         Datum datum = (Datum)value;
         valType = datum.getDatumType();

         if (valType.isNumeric())
         {
            currencySym = datum.getCurrencySymbol();
            num = datum.getNumber();
         }

         isNull = datum.isNull();
      }

      Color bg, fg;

      if (isSelected)
      {
         bg = table.getSelectionBackground();
         fg = table.getSelectionForeground();
      }
      else
      {
         bg = isNull ? NULL_BG : table.getBackground();
         fg = table.getForeground();
      }

      panel.setBackground(bg);
      panel.setForeground(fg);

      if (typeField != null)
      {
         typeField.setText(resources.getTypeLabel(valType));
      }

      if (currencyField != null)
      {
         if (currencySym == null)
         {
            currencyField.setText("");
         }
         else
         { 
            currencyField.setText(currencySym);
         }
      }

      if (numField != null)
      {
         if (num == null)
         {
            numField.setText("");
         }
         else
         { 
            numField.setText(num.toString());
         }

         datumInfoComponent.setVisible(resources.isCellDatumVisible());
      }

      return panel;
   }
}
