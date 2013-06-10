package com.dickimawbooks.datatooltk.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.*;

public class HeaderDialog extends JDialog
  implements ActionListener
{
   public HeaderDialog(DatatoolGUI gui, DatatoolDb db, DatatoolDbPanel panel)
   {
      super(gui, DatatoolTk.getLabel("header.title"), true);

      this.panel = panel;

      this.db = db;

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

      typeBox = new JComboBox(DatatoolHeader.TYPE_LABELS);
      label.setLabelFor(typeBox);
      p.add(typeBox);

      p = new JPanel();

      p.add(DatatoolGuiResources.createOkayButton(this));
      p.add(DatatoolGuiResources.createCancelButton(this));

      getContentPane().add(p, BorderLayout.SOUTH);

      pack();

      setLocationRelativeTo(null);
   }

   public void display(int colIdx)
   {
      header = db.getHeader(colIdx+1);

      titleField.setText(header.getTitle());
      labelField.setText(header.getKey());
      typeBox.setSelectedIndex(header.getType()+1);

      setVisible(true);
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("okay"))
      {
         header.setTitle(titleField.getText());
         header.setKey(labelField.getText());
         header.setType(typeBox.getSelectedIndex()-1);
         panel.setModified(true);

         setVisible(false);
      }
      else if (action.equals("cancel"))
      {
         setVisible(false);
      }
   }

   private DatatoolDb db;

   private JTextField titleField, labelField;

   private JComboBox typeBox;

   private DatatoolHeader header;

   private DatatoolDbPanel panel;
}
