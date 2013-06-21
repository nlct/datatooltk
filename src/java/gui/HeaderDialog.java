package com.dickimawbooks.datatooltk.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.*;

public class HeaderDialog extends JDialog
  implements ActionListener
{
   public HeaderDialog(DatatoolGUI gui)
   {
      super(gui, DatatoolTk.getLabel("header.title"), true);

      Box box = Box.createVerticalBox();
      getContentPane().add(box, BorderLayout.CENTER);

      JPanel p = new JPanel();
      box.add(p);

      JLabel label = DatatoolGuiResources.createJLabel("header.column_title");
      p.add(label);

      titleField = new JTextField(10);
      label.setLabelFor(titleField);

      p.add(titleField);

      p = new JPanel();
      box.add(p);

      label = DatatoolGuiResources.createJLabel("header.column_label");
      p.add(label);

      labelField = new JTextField(10);
      label.setLabelFor(labelField);
      p.add(labelField);

      p = new JPanel();
      box.add(p);

      label = DatatoolGuiResources.createJLabel("header.column_type");
      p.add(label);

      typeBox = new JComboBox<String>(DatatoolDb.TYPE_LABELS);
      label.setLabelFor(typeBox);
      p.add(typeBox);

      getContentPane().add(
       DatatoolGuiResources.createOkayCancelPanel(this),
       BorderLayout.SOUTH);

      pack();

      setLocationRelativeTo(null);
   }

   public DatatoolHeader requestEdit(int colIdx, DatatoolDb db)
   {
      this.colIdx = colIdx;
      return requestEdit((DatatoolHeader)db.getHeader(colIdx).clone(), db, false);
   }

   public DatatoolHeader requestEdit(DatatoolHeader header, DatatoolDb db)
   {
      return requestEdit(header, db, true);
   }

   public DatatoolHeader requestEdit(DatatoolHeader aHeader, DatatoolDb db, boolean checkUnique)
   {
      this.db = db;
      this.header = aHeader;
      this.checkUnique = checkUnique;

      modified = false;
      setTitle(DatatoolTk.getLabelWithValue("header.title", header.getKey()));

      titleField.setText(header.getTitle());
      labelField.setText(header.getKey());
      typeBox.setSelectedIndex(header.getType()+1);

      titleField.requestFocusInWindow();

      setVisible(true);

      return modified ? this.header : null;
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
         colIdx = -1;
         setVisible(false);
      }
   }

   private void okay()
   {
      int type = typeBox.getSelectedIndex()-1;

      if (colIdx > -1 && type != DatatoolDb.TYPE_UNKNOWN
           && type != DatatoolDb.TYPE_STRING)
      {
         // Is the requested data type valid for this column?

         int rowIdx = 0;

         for (ColumnEnumeration en = db.getColumnEnumeration(colIdx);
            en.hasMoreElements(); )
         {
            rowIdx++;

            String element = en.nextElement();

            int thisType = DatatoolDb.getType(element);

            switch (type)
            {
               case DatatoolDb.TYPE_INTEGER:
                  if (thisType == DatatoolDb.TYPE_REAL
                     || thisType == DatatoolDb.TYPE_CURRENCY
                     || thisType == DatatoolDb.TYPE_STRING)
                  {
                     DatatoolGuiResources.error(this, 
                        DatatoolTk.getLabelWithValues("error.invalid_header_choice",
                          new String[]
                          {
                            DatatoolDb.TYPE_LABELS[type+1],
                            ""+rowIdx,
                            DatatoolDb.TYPE_LABELS[thisType+1]
                          }));

                     return;
                  }
               break;
               case DatatoolDb.TYPE_REAL:
                  if (thisType == DatatoolDb.TYPE_CURRENCY
                   || thisType == DatatoolDb.TYPE_STRING)
                  {
                     DatatoolGuiResources.error(this, 
                        DatatoolTk.getLabelWithValues("error.invalid_header_choice",
                          new String[]
                          {
                            DatatoolDb.TYPE_LABELS[type+1],
                            ""+rowIdx,
                            DatatoolDb.TYPE_LABELS[thisType+1]
                          }));

                     return;
                  }
               break;
               case DatatoolDb.TYPE_CURRENCY:
                  if (thisType == DatatoolDb.TYPE_STRING)
                  {
                     DatatoolGuiResources.error(this, 
                        DatatoolTk.getLabelWithValues("error.invalid_header_choice",
                          new String[]
                          {
                            DatatoolDb.TYPE_LABELS[type+1],
                            ""+rowIdx,
                            DatatoolDb.TYPE_LABELS[thisType+1]
                          }));

                     return;
                  }
               break;
            }
         }
      }

      String key = labelField.getText();

      if (key.isEmpty())
      {
         DatatoolGuiResources.error(this, 
            DatatoolTk.getLabel("error.missing_key"));
         return;
      }

      if (checkUnique || !header.getKey().equals(key))
      {
         // Only test if key has been changed unless checkUnique
         // set.

         if (db.getHeader(key) != null)
         {
            DatatoolGuiResources.error(this, 
               DatatoolTk.getLabelWithValue("error.key_exists", key));

            return;
         }
      }

      header.setTitle(titleField.getText());
      header.setKey(key);
      header.setType(type);
      modified = true;

      colIdx = -1;
      setVisible(false);
   }

   private JTextField titleField, labelField;

   private JComboBox<String> typeBox;

   private DatatoolHeader header;

   private DatatoolDb db;

   private boolean checkUnique;

   private boolean modified;

   private int colIdx = -1;
}
