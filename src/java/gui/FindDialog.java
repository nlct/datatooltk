package com.dickimawbooks.datatooltk.gui;

import java.awt.event.*;
import java.awt.*;
import java.util.regex.*;
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

      Dimension dim = searchField.getMaximumSize();
      dim.height = (int)searchField.getPreferredSize().getHeight();
      searchField.setMaximumSize(dim);

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

      replacePanel = Box.createHorizontalBox();
      mainPanel.add(replacePanel);

      replaceField = new JTextField();

      replacePanel.add(DatatoolGuiResources.createJLabel(
         "replace.replace_text", replaceField));
      replacePanel.add(replaceField);

      panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      caseBox = DatatoolGuiResources.createJCheckBox("find",
        "case", null);
      panel.add(caseBox);

      regexBox = DatatoolGuiResources.createJCheckBox("find",
        "regex", null);
      panel.add(regexBox);

      mainPanel.add(Box.createVerticalGlue());

      JPanel buttonPanel = new JPanel();
      getContentPane().add(buttonPanel, BorderLayout.SOUTH);

      findButton = DatatoolGuiResources.createActionButton(
        "find", "find", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));

      buttonPanel.add(findButton);

      replaceButton = DatatoolGuiResources.createActionButton(
        "replace", "replace", this, null);

      buttonPanel.add(replaceButton);

      replaceAllButton = DatatoolGuiResources.createActionButton(
        "replace", "replace_all", this, null);

      buttonPanel.add(replaceAllButton);

      buttonPanel.add(DatatoolGuiResources.createActionButton(
        "find", "close", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));

      getRootPane().setDefaultButton(findButton);
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

   public String getSearchText()
   {
      return searchField.getText();
   }

   public void display(boolean isReplaceAllowed)
   {
      searchField.requestFocusInWindow();

      replacePanel.setVisible(isReplaceAllowed);
      replaceButton.setVisible(isReplaceAllowed);
      replaceAllButton.setVisible(isReplaceAllowed);

      setTitle(isReplaceAllowed ? DatatoolTk.getLabel("replace.title") :
        DatatoolTk.getLabel("find.title"));

      found = false;

      updateButtons();

      setVisible(true);
   }

   public void find()
   {
      found = regexBox.isSelected() ?  findRegEx() : findNoRegEx();

      updateButtons();
   }

   public boolean findNoRegEx()
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
            return false;
         }
      }


      component.setSelectionStart(index);
      component.setSelectionEnd(index+searchText.length());
      component.requestFocus();

      return true;
   }

   public boolean findRegEx()
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
            return false;
         }
      }


      component.setSelectionStart(index);
      component.setSelectionEnd(matcher.end());
      component.requestFocus();

      return true;
   }

   private void updateButtons()
   {
      findButton.setEnabled(!searchField.getText().isEmpty());
      replaceButton.setEnabled(found);
      replaceAllButton.setEnabled(findButton.isEnabled());
   }

   private JButton findButton, replaceButton, replaceAllButton;
   private JTextField searchField, replaceField;
   private JTextComponent component;
   private JCheckBox caseBox, regexBox;

   private JComponent replacePanel;

   private boolean found = false;
}
