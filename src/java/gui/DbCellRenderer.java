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
import java.awt.Component;
import java.awt.event.*;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.text.*;

 /**
 * Cell renderer based on DefaultCellRenderer.
 */
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

      rendererComponent.setText(value.toString().replaceAll("\\\\DTLpar *",
        "\n\n"));

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
