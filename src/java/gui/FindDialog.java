package com.dickimawbooks.datatooltk.gui;

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

public class FindDialog extends JDialog
  implements ActionListener
{
   public FindDialog(JDialog parent, JTextComponent component)
   {
      super(parent, DatatoolTk.getLabel("find.title"), false);

      this.component = component;

      JComponent mainPanel = Box.createVerticalBox();
      getContentPane().add(mainPanel, BorderLayout.CENTER);

      JComponent panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      searchField = new JTextField();

      panel.add(DatatoolGuiResources.createJLabel(
         "find.search_for", searchField));
      panel.add(searchField);

      searchField.getDocument().addDocumentListener(new DocumentListener()
      {
         public void changedUpdate(DocumentEvent e)
         {
            updateButtons();
         }

         public void insertUpdate(DocumentEvent e)
         {
            updateButtons();
         }

         public void removeUpdate(DocumentEvent e)
         {
            updateButtons();
         }
      });

      caseBox = DatatoolGuiResources.createJCheckBox("find",
        "case", null);
      mainPanel.add(caseBox);

      JPanel buttonPanel = new JPanel();
      getContentPane().add(buttonPanel, BorderLayout.SOUTH);

      findButton = DatatoolGuiResources.createActionButton(
        "find", "find", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));

      buttonPanel.add(findButton);

      buttonPanel.add(DatatoolGuiResources.createActionButton(
        "find", "close", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));

      updateButtons();
      pack();
      setLocationRelativeTo(parent);
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("find"))
      {
         find();
      }
      else if (action.equals("close"))
      {
         setVisible(false);
      }
   }

   public void setSearchText(String searchText)
   {
      searchField.setText(searchText);
   }

   public void display()
   {
      searchField.requestFocusInWindow();
      setVisible(true);
   }

   public void find()
   {
      String searchText = searchField.getText();

      int pos = component.getCaretPosition();

      String text = component.getText();

      if (!caseBox.isSelected())
      {
         searchText = searchText.toLowerCase();
         text = text.toLowerCase();
      }

      int index = text.indexOf(searchText, pos);

      if (index == -1)
      {
         if (pos > 0)
         {
            if (JOptionPane.showConfirmDialog(this, 
                 DatatoolTk.getLabel("find.query_wrap"),
                 DatatoolTk.getLabel("find.query_wrap.title"),
                 JOptionPane.YES_NO_OPTION)
               == JOptionPane.YES_OPTION)
            {
               index = text.indexOf(searchText);
            }
         }

         if (index == -1)
         {
            JOptionPane.showMessageDialog(this,
               DatatoolTk.getLabel("find.not_found"));
            return;
         }
      }


      component.setCaretPosition(index);
      component.setSelectionStart(index);
      component.setSelectionEnd(index+searchText.length());
   }

   private void updateButtons()
   {
      findButton.setEnabled(!searchField.getText().isEmpty());
   }

   private JButton findButton;
   private JTextField searchField;
   private JTextComponent component;
   private JCheckBox caseBox;
}
