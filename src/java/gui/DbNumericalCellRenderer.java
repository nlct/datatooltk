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
 * Cell renderer for data in numerical columns.
 */
public class DbNumericalCellRenderer implements TableCellRenderer
{
   private JTextField textField, typeField, currencyField, numField;
   private JPanel panel;
   private DatatoolSettings settings;
   private CardLayout cardLayout;
   private JComponent currencyRow, valueRow,
      nullCard, numericCard;
   private JTextComponent textCard;
   private static final Color NULL_BG = Color.LIGHT_GRAY;

   public DbNumericalCellRenderer(DatatoolSettings settings)
   {
      this.settings = settings;
      DatatoolGuiResources resources = settings.getDatatoolGuiResources();

      cardLayout = new CardLayout();
      panel = new JPanel(cardLayout);
      panel.setOpaque(true);

      textCard = new JTextArea();
      textCard.setEditable(false);
      textCard.setOpaque(false);
      panel.add(textCard, "string");
      
      nullCard = new JPanel();
      nullCard.setOpaque(false);
      panel.add(textCard, "null");

      nullCard.add(new JLabel("NULL"));
      
      numericCard = new JPanel(new BorderLayout());
      numericCard.setOpaque(false);
      panel.add(numericCard, "numeric");

      textField = new JTextField();
      textField.setEditable(false);
      textField.setHorizontalAlignment(JTextField.TRAILING);

      numericCard.add(textField, BorderLayout.NORTH);

      JComponent mainComp = Box.createVerticalBox();
      mainComp.setOpaque(false);
      numericCard.add(mainComp, BorderLayout.CENTER);

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

      valueRow = createRow();
      mainComp.add(valueRow);

      JLabel valueLabel = resources.createJLabel("celledit.numeric");
      valueRow.add(valueLabel);
      numField = createField();
      valueRow.add(numField);

      prefDim = valueLabel.getPreferredSize();
      maxDim = valueRow.getMaximumSize();
      maxDim.height = 2*prefDim.height;
      valueRow.setMaximumSize(maxDim);

      currencyRow = createRow();
      mainComp.add(currencyRow);

      currencyRow.add(resources.createJLabel("celledit.currency"));
      currencyField = createField();
      currencyRow.add(currencyField);

      mainComp.add(Box.createVerticalGlue());
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
      if (table == null)
      {
         return textField;
      }

      DatumType type = DatumType.UNKNOWN;
      String currencySym = null;
      Number num = null;
      boolean isNull = false;

      if (value instanceof Datum)
      {
         Datum datum = (Datum)value;
         type = datum.getDatumType();

         if (type.isNumeric())
         {
            currencySym = datum.getCurrencySymbol();
            num = datum.getNumber();
         }

         isNull = datum.isNull();
      }

      JTextComponent textComp = textField;
      String text = value.toString();

      switch (type)
      {
         case STRING:
            cardLayout.show(panel, "string");
            textComp = textCard;
         break;
         case UNKNOWN:
            if (isNull)
            {
               cardLayout.show(panel, "null");
               textComp = null;
            }
            else
            {
               cardLayout.show(panel, "string");
               textComp = textCard;
            }
         break;
         default:
            cardLayout.show(panel, "numeric");
         break;
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

      if (textComp != null)
      {
         textComp.setText(text);

         textComp.setBackground(bg);
         textComp.setForeground(fg);

         textComp.setFont(table.getFont());
      }

      if (type.isNumeric())
      {
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
            valueRow.setVisible(false);
         }
         else
         {
            numField.setText(num.toString());
            valueRow.setVisible(true);
         }
      }

      return panel;
   }
}
