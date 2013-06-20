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

      typeBox = new JComboBox<String>(DatatoolHeader.TYPE_LABELS);
      label.setLabelFor(typeBox);
      p.add(typeBox);

      p = new JPanel();

      p.add(DatatoolGuiResources.createOkayButton(this));
      p.add(DatatoolGuiResources.createCancelButton(this));

      getContentPane().add(p, BorderLayout.SOUTH);

      pack();

      setLocationRelativeTo(null);
   }

   public boolean requestEdit(int colIdx, DatatoolDb db)
   {
      return requestEdit(db.getHeader(colIdx), db, false);
   }

   public boolean requestEdit(DatatoolHeader header, DatatoolDb db)
   {
      return requestEdit(header, db, true);
   }

   public boolean requestEdit(DatatoolHeader header, DatatoolDb db, boolean checkUnique)
   {
      this.db = db;
      this.header = header;
      this.checkUnique = checkUnique;

      modified = false;
      setTitle(DatatoolTk.getLabelWithValue("header.title", header.getKey()));

      titleField.setText(header.getTitle());
      labelField.setText(header.getKey());
      typeBox.setSelectedIndex(header.getType()+1);

      titleField.requestFocusInWindow();

      setVisible(true);

      return modified;
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("okay"))
      {
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
         header.setType(typeBox.getSelectedIndex()-1);
         modified = true;

         setVisible(false);
      }
      else if (action.equals("cancel"))
      {
         setVisible(false);
      }
   }

   private JTextField titleField, labelField;

   private JComboBox<String> typeBox;

   private DatatoolHeader header;

   private DatatoolDb db;

   private boolean checkUnique;

   private boolean modified;
}
