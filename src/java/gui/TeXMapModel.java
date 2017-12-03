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

import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import com.dickimawbooks.datatooltk.*;

/**
 * Table model for TeX mappings (used in properties dialog box).
 */
public class TeXMapModel extends AbstractTableModel
{
   public TeXMapModel(PropertiesDialog dialog,
      JTable table, DatatoolSettings settings)
   {
      super();
      this.settings = settings;
      this.table = table;

      MessageHandler messageHandler = settings.getMessageHandler();

      if (COL_KEY == null)
      {
         COL_KEY = messageHandler.getLabel("texmap.character");
         COL_VAL = messageHandler.getLabel("texmap.replacement");
      }

      table.setModel(this);

      keyField = new CharField();
      valueField = new JTextField(20);

      texMapDialog = new TeXMapDialog(messageHandler.getDatatoolGuiResources(),
         dialog);

      keyList = new Vector<Character>();
      valueList = new Vector<String>();

      for (Enumeration en=settings.keys(); en.hasMoreElements();)
      {
         String key = (String)en.nextElement();

         Matcher m = PATTERN_KEY.matcher(key);

         if (m.matches())
         {
            char c = m.group(1).charAt(0);
            keyList.add(new Character(c));
            valueList.add(settings.getTeXMap(c));
         }
      }

      originals = keyList.toArray();

      table.getColumn(COL_VAL).setPreferredWidth(
        (int)valueField.getPreferredSize().getWidth());

      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
   }

   public String getColumnName(int columnIndex)
   {
      return columnIndex == 0 ? COL_KEY : COL_VAL;
   }

   public boolean isCellEditable(int rowIndex, int columnIndex)
   {
      return false;
   }

   public void setValueAt(Object aValue, int rowIndex, int columnIndex)
   {
      if (columnIndex == 0)
      {
         keyList.set(rowIndex, new Character(aValue.toString().charAt(0)));
      }
      else
      {
         valueList.add(aValue.toString());
      }
   }

   public int getColumnCount() {return 2;}

   public int getRowCount() {return keyList == null ? 0 : keyList.size();}

   public Object getValueAt(int rowIndex, int columnIndex)
   {
      return columnIndex == 0 ? keyList.get(rowIndex) : valueList.get(rowIndex);
   }

   public void updateSettings()
   {
      for (int i = 0; i < originals.length; i++)
      {
         Character c = (Character)originals[i];

         int index = keyList.indexOf(c);

         if (index == -1)
         {
            // User has removed this mapping.

            settings.removeTeXMap(c.charValue());
         }
         else
         {
            Character key = keyList.remove(index);
            String value = valueList.remove(index);

            settings.setTeXMap(key.charValue(), value);
         }
      }

      // Remaining entries are new mappings

      for (int i = 0, n = keyList.size(); i < n; i++)
      {
         settings.setTeXMap(keyList.get(i).charValue(), valueList.get(i));
      }
   }

   public void removeRow(int index)
   {
      keyList.remove(index);
      valueList.remove(index);

      table.tableChanged(new TableModelEvent(this, index, index,
         TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));

      if (index < keyList.size())
      {
         table.setRowSelectionInterval(index, index);
      }
   }

   public void addRow()
   {
      if (texMapDialog.displayNew())
      {
         keyList.add(texMapDialog.getKey());
         valueList.add(texMapDialog.getValue());

         int index = keyList.size()-1;

         table.tableChanged(new TableModelEvent(this, index, index,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));

         table.setRowSelectionInterval(index, index);
      }
   }

   public void editRow(int rowIndex)
   {
      if (texMapDialog.displayEdit(keyList.get(rowIndex),
            valueList.get(rowIndex)))
      {
         keyList.set(rowIndex, texMapDialog.getKey());
         valueList.set(rowIndex, texMapDialog.getValue());

         table.tableChanged(new TableModelEvent(this, rowIndex, rowIndex,
            TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));

         table.setRowSelectionInterval(rowIndex, rowIndex);
      }
   }

   private DatatoolSettings settings;

   private JTable table;

   private CharField keyField;

   private JTextField valueField;

   private TeXMapDialog texMapDialog;

   private Vector<Character> keyList;
   private Vector<String> valueList;

   private Object[] originals;

   private static final Pattern PATTERN_KEY 
     = Pattern.compile("tex\\.(.)");

   public static String COL_KEY = null;
   public static String COL_VAL = null;

}

class TeXMapDialog extends JDialog implements ActionListener
{
   public TeXMapDialog(DatatoolGuiResources resources, JDialog parent)
   {
      super(parent, "", true);

      this.resources = resources;

      if (TITLE_ADD == null)
      {
         TITLE_ADD 
            = resources.getMessageHandler().getLabel("texmap.add_mapping");

         TITLE_EDIT 
            = resources.getMessageHandler().getLabel("texmap.edit_mapping");
      }

      JPanel panel = new JPanel();
      getContentPane().add(panel, BorderLayout.CENTER);

      keyField   = new CharField();
      valueField = new JTextField(20);

      panel.add(resources.createJLabel("texmap.character",
         keyField));
      panel.add(keyField);

      panel.add(resources.createJLabel("texmap.replacement",
         valueField));
      panel.add(valueField);
      
      getContentPane().add(
        resources.createOkayCancelPanel(this),
        BorderLayout.SOUTH);

      pack();
      setLocationRelativeTo(null);
   }

   public boolean displayNew()
   {
      keyField.setText("");
      valueField.setText("");

      setTitle(TITLE_ADD);

      return display();
   }

   
   public boolean displayEdit(Character c, String value)
   {
      if (c != null) keyField.setValue(c.charValue());
      if (value != null) valueField.setText(value);

      setTitle(TITLE_EDIT);

      return display();
   }

   private boolean display()
   {
      keyField.requestFocusInWindow();

      modified = false;

      setVisible(true);

      return modified;
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("okay"))
      {
         okay();
      }
      else if (action.equals("cancel"))
      {
         modified = false;
         setVisible(false);
      }
   }

   private void okay()
   {
      if (keyField.getText().isEmpty())
      {
         resources.error(this, 
            getMessageHandler().getLabel("error.missing_texmap_key"));
         return;
      }

      modified = true;
      setVisible(false);
   }

   public MessageHandler getMessageHandler()
   {
      return resources.getMessageHandler();
   }

   public Character getKey() { return new Character(keyField.getValue());}

   public String getValue() { return valueField.getText(); }

   private CharField keyField;

   private JTextField valueField;

   private boolean modified;

   private DatatoolGuiResources resources;

   private static String TITLE_ADD = null;

   private static String TITLE_EDIT = null;
}
