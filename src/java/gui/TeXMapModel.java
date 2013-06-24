package com.dickimawbooks.datatooltk.gui;

import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import com.dickimawbooks.datatooltk.*;

public class TeXMapModel extends AbstractTableModel
{
   public TeXMapModel(PropertiesDialog dialog,
      JTable table, DatatoolSettings settings)
   {
      super();
      this.settings = settings;
      this.table = table;

      table.setModel(this);

      keyField = new CharField();
      valueField = new JTextField(20);

      texMapDialog = new TeXMapDialog(dialog);

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

   public static final String COL_KEY 
      = DatatoolTk.getLabel("texmap.character");
   public static final String COL_VAL 
      = DatatoolTk.getLabel("texmap.replacement");

}

class TeXMapDialog extends JDialog implements ActionListener
{
   public TeXMapDialog(JDialog parent)
   {
      super(parent, "", true);

      JPanel panel = new JPanel();
      getContentPane().add(panel, BorderLayout.CENTER);

      keyField   = new CharField();
      valueField = new JTextField(20);

      panel.add(DatatoolGuiResources.createJLabel("texmap.character",
         keyField));
      panel.add(keyField);

      panel.add(DatatoolGuiResources.createJLabel("texmap.replacement",
         valueField));
      panel.add(valueField);
      
      getContentPane().add(
        DatatoolGuiResources.createOkayCancelPanel(this),
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
         DatatoolGuiResources.error(this, 
            DatatoolTk.getLabel("error.missing_texmap_key"));
         return;
      }

      modified = true;
      setVisible(false);
   }

   public Character getKey() { return new Character(keyField.getValue());}

   public String getValue() { return valueField.getText(); }

   private CharField keyField;

   private JTextField valueField;

   private boolean modified;

   private static final String TITLE_ADD 
      = DatatoolTk.getLabel("texmap.add_mapping");

   private static final String TITLE_EDIT 
      = DatatoolTk.getLabel("texmap.edit_mapping");
}
