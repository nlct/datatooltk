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
import com.dickimawbooks.datatooltk.DatatoolSettings;

/**
 * Cell renderer for Datum values.
 */
public class DatumCellRenderer implements TableCellRenderer
{
   private DatatoolSettings settings;

   private JTextField typeField, currencyField, numField;
   private Component panel;
   private JTextComponent textComp;

   private DatumType type;

   private static final Color NULL_BG = Color.LIGHT_GRAY;

   public DatumCellRenderer(DatatoolSettings settings, DatumType type)
   {
      this.settings = settings;
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

      nullComp.add(new JLabel("NULL"));

      return nullComp;
   }

   protected JComponent createNumericComp()
   {
      DatatoolGuiResources resources = settings.getDatatoolGuiResources();

      JComponent numericComp = new JPanel(new BorderLayout());
      numericComp.setOpaque(true);

      JTextField textField = createField();
      textField.setHorizontalAlignment(JTextField.TRAILING);
      textComp = textField;

      numericComp.add(textComp, BorderLayout.NORTH);

      JComponent mainComp = Box.createVerticalBox();
      mainComp.setOpaque(false);
      numericComp.add(mainComp, BorderLayout.CENTER);

      JComponent rowComp;

      rowComp = createRow();
      mainComp.add(rowComp);

      JLabel typeLabel = resources.createJLabel("celledit.type");
      rowComp.add(typeLabel);
      typeField = createField();
      rowComp.add(typeField);

      Dimension prefDim = typeLabel.getPreferredSize();
      Dimension maxDim = rowComp.getMaximumSize();
      maxDim.height = 2*prefDim.height;
      rowComp.setMaximumSize(maxDim);

      JComponent valueRow = createRow();
      mainComp.add(valueRow);

      JLabel valueLabel = resources.createJLabel("celledit.numeric");
      valueRow.add(valueLabel);
      numField = createField();
      valueRow.add(numField);

      prefDim = valueLabel.getPreferredSize();
      maxDim = valueRow.getMaximumSize();
      maxDim.height = 2*prefDim.height;
      valueRow.setMaximumSize(maxDim);

      if (type == DatumType.CURRENCY)
      {
         JComponent currencyRow = createRow();
         mainComp.add(currencyRow);

         currencyRow.add(resources.createJLabel("celledit.currency"));
         currencyField = createField();

         currencyRow.add(currencyField);
      }

      mainComp.add(Box.createVerticalGlue());

      return numericComp;
   }

   protected JComponent createRow()
   {
      JComponent comp = new JPanel(new FlowLayout(FlowLayout.LEADING));
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
      if (textComp != null)
      {
         textComp.setText(value.toString());

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
         typeField.setText(settings.getTypeLabel(valType));
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
      }

      return panel;
   }
}
