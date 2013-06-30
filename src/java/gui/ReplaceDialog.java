package com.dickimawbooks.datatooltk.gui;

import java.awt.event.*;
import java.awt.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

public class ReplaceDialog extends JDialog
  implements ActionListener
{
   public ReplaceDialog(JDialog parent, JTextComponent component)
   {
      super(parent, DatatoolTk.getLabel("replace.title"), false);

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

      panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      replaceField = new JTextField();

      panel.add(DatatoolGuiResources.createJLabel(
         "replace.replace_text", replaceField));
      panel.add(replaceField);

      panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      caseBox = DatatoolGuiResources.createJCheckBox("find",
        "case", null);
      panel.add(caseBox);

      regexBox = DatatoolGuiResources.createJCheckBox("find",
        "regex", null);
      panel.add(regexBox);

      JPanel buttonPanel = new JPanel();
      getContentPane().add(buttonPanel, BorderLayout.SOUTH);

      replaceButton = DatatoolGuiResources.createActionButton(
        "replace", "replace", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));

      buttonPanel.add(replaceButton);

      replaceAllButton = DatatoolGuiResources.createActionButton(
        "replace", "replace_all", this, null);

      buttonPanel.add(replaceButton);

      buttonPanel.add(DatatoolGuiResources.createActionButton(
        "find", "close", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));

      getRootPane().setDefaultButton(replaceButton);

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
      if (regexBox.isSelected())
      {
         findRegEx();
      }
      else
      {
         findNoRegEx();
      }
   }

   public void findNoRegEx()
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


      component.setSelectionStart(index);
      component.setSelectionEnd(index+searchText.length());
      component.requestFocus();
   }

   public void findRegEx()
   {
      Pattern pattern;

      int pos = component.getCaretPosition();

      String text = component.getText();

      if (caseBox.isSelected())
      {
         pattern = Pattern.compile(searchField.getText());
      }
      else
      {
         pattern = Pattern.compile(searchField.getText(),
            Pattern.CASE_INSENSITIVE);
         text = text.toLowerCase();
      }

      int index = -1;

      Matcher matcher = pattern.matcher(text);

      if (matcher.find(pos))
      {
         index = matcher.start();
      }

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
               if (matcher.find())
               {
                  index = matcher.start();
               }
            }
         }

         if (index == -1)
         {
            JOptionPane.showMessageDialog(this,
               DatatoolTk.getLabel("find.not_found"));
            return;
         }
      }


      component.setSelectionStart(index);
      component.setSelectionEnd(matcher.end());
      component.requestFocus();
   }

   private void updateButtons()
   {
      replaceButton.setEnabled(!searchField.getText().isEmpty());
      replaceAllButton.setEnabled(replaceButton.isEnabled());
   }

   private JButton replaceButton, replaceAllButton;
   private JTextField searchField, replaceField;
   private JTextComponent component;
   private JCheckBox caseBox, regexBox;
}
