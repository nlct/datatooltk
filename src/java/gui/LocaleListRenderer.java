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

import java.util.Locale;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * List renderer for Locale choice. Based on BasicComboBoxRenderer.
 */
public class LocaleListRenderer extends JLabel
 implements ListCellRenderer<Locale>
{
   protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

   public LocaleListRenderer()
   {
      setOpaque(true);
      setBorder(noFocusBorder);
   }

   @Override
   public Dimension getPreferredSize()
   {
      if (this.getText() != null && ! this.getText().equals(""))
      {
         return super.getPreferredSize();
      }
      else
      {
         String oldText = this.getText();
         this.setText(" ");
         Dimension d = super.getPreferredSize();
         this.setText(oldText);
         return d;
      }
   }

   @Override
   public Component getListCellRendererComponent(JList<? extends Locale> list,
     Locale value, int index, boolean isSelected, boolean cellHasFocus)
   {
      if (isSelected)
      {
         setBackground(list.getSelectionBackground());
         setForeground(list.getSelectionForeground());
      }
      else
      {
         setBackground(list.getBackground());
         setForeground(list.getForeground());
      }

      setFont(list.getFont());
 
      setText(value == null ? "" : value.getDisplayName());
 
      return this;
   }
}
