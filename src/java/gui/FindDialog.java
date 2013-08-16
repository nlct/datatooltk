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

import java.awt.event.*;
import java.awt.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

/**
 * Dialog box for searching for text within displayed cell editor dialog.
 */
public class FindDialog extends JDialog
  implements ActionListener,CaretListener
{
   public FindDialog(JDialog parent, JTextComponent textComp)
   {
      super(parent, DatatoolTk.getLabel("find.title"), false);

      this.textComp = textComp;

      JComponent mainPanel = Box.createVerticalBox();
      getContentPane().add(mainPanel, BorderLayout.CENTER);

      JComponent panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      searchField = new JTextField();
      JLabel searchLabel = DatatoolGuiResources.createJLabel(
         "find.search_for", searchField);

      panel.add(searchLabel);
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
      JLabel replaceLabel = DatatoolGuiResources.createJLabel(
         "replace.replace_text", replaceField);

      replacePanel.add(replaceLabel);
      replacePanel.add(replaceField);

      dim = searchLabel.getPreferredSize();

      dim.width = Math.max(dim.width, 
        (int)replaceLabel.getPreferredSize().getWidth());

      searchLabel.setPreferredSize(dim);
      replaceLabel.setPreferredSize(dim);

      panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      caseBox = DatatoolGuiResources.createJCheckBox("find",
        "case", null);
      panel.add(caseBox);

      regexBox = DatatoolGuiResources.createJCheckBox("find",
        "regex", null);
      panel.add(regexBox);

      wrapBox = DatatoolGuiResources.createJCheckBox("find",
        "wrap", null);
      panel.add(wrapBox);
      wrapBox.setSelected(true);

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

      textComp.addCaretListener(this);

      pack();
      setLocationRelativeTo(parent);
   }

   public void caretUpdate(CaretEvent evt)
   {
      if (isVisible() && !updating)
      {
         found = false;
         updateButtons();
      }
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("find"))
      {
         find();
      }
      else if (action.equals("replace"))
      {
         replace();
      }
      else if (action.equals("replace_all"))
      {
         replaceAll();
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
      updating = false;
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

   public void replace()
   {
      updating = true;
      if (regexBox.isSelected())
      {
         replaceRegEx();
      }
      else
      {
         replaceNoRegEx();
      }
      updating = false;

      find();
   }

   public void replaceNoRegEx()
   {
      String replacement = replaceField.getText();
      int start = textComp.getSelectionStart();

      textComp.replaceSelection(replacement);

      textComp.setCaretPosition(start+replacement.length());
   }

   public void replaceRegEx()
   {
      int start = textComp.getSelectionStart();

      String selectedText = textComp.getSelectedText();

      Pattern pattern;

      if (caseBox.isSelected())
      {
         pattern = Pattern.compile(searchField.getText());
      }
      else
      {
         pattern = Pattern.compile(searchField.getText(),
            Pattern.CASE_INSENSITIVE);
      }

      Matcher matcher = pattern.matcher(selectedText);

      String replacement = matcher.replaceFirst(replaceField.getText());

      textComp.replaceSelection(replacement);
      textComp.setCaretPosition(start+replacement.length());
   }

   public void replaceAll()
   {
      updating = true;
      if (regexBox.isSelected())
      {
         replaceAllRegEx();
      }
      else
      {
         replaceAllNoRegEx();
      }

      updateButtons();
      updating = false;
   }

   public void replaceAllRegEx()
   {
      Pattern pattern;

      if (caseBox.isSelected())
      {
         pattern = Pattern.compile(searchField.getText());
      }
      else
      {
         pattern = Pattern.compile(searchField.getText(),
            Pattern.CASE_INSENSITIVE);
      }

      Matcher matcher = pattern.matcher(textComp.getText());

      textComp.setText(matcher.replaceAll(replaceField.getText()));
   }

   public void replaceAllNoRegEx()
   {
      String text = textComp.getText();
      String matcherText = text;
      String searchText = searchField.getText();
      int searchLength = searchText.length();

      String replaceText = replaceField.getText();

      if (!caseBox.isSelected())
      {
         searchText = searchText.toLowerCase();
         matcherText = text.toLowerCase();
      }

      int pos = 0;
      int index;

      int count = 0;

      StringBuilder builder = new StringBuilder(text.length());

      while ((index = matcherText.indexOf(searchText, pos)) != -1)
      {
         builder.append(text.substring(pos, index));
         builder.append(replaceText);
         pos = index+searchLength;
         count++;
      }

      if (pos < text.length())
      {
         builder.append(text.substring(pos));
      }

      textComp.setText(builder.toString());

      JOptionPane.showMessageDialog(this, 
        count == 1 ?
        DatatoolTk.getLabel("replace.one_replaced") :
        DatatoolTk.getLabelWithValue("replace.num_replaced", count));
   }

   public void find()
   {
      updating = true;
      found = regexBox.isSelected() ?  findRegEx() : findNoRegEx();

      updateButtons();
      updating = false;
   }

   public boolean findNoRegEx()
   {
      String searchText = searchField.getText();

      int pos = textComp.getCaretPosition();

      String text = textComp.getText();

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
            if (wrapBox.isSelected())
            {
               index = text.indexOf(searchText);
            }
         }

         if (index == -1)
         {
            JOptionPane.showMessageDialog(this,
               DatatoolTk.getLabel("find.not_found_in_cell"));
            return false;
         }
      }


      textComp.setSelectionStart(index);
      textComp.setSelectionEnd(index+searchText.length());
      textComp.requestFocus();

      return true;
   }

   public boolean findRegEx()
   {
      Pattern pattern;

      int pos = textComp.getCaretPosition();

      String text = textComp.getText();

      if (caseBox.isSelected())
      {
         pattern = Pattern.compile(searchField.getText());
      }
      else
      {
         pattern = Pattern.compile(searchField.getText(),
            Pattern.CASE_INSENSITIVE);
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
            if (wrapBox.isSelected())
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
               DatatoolTk.getLabel("find.not_found_in_cell"));
            return false;
         }
      }


      textComp.setSelectionStart(index);
      textComp.setSelectionEnd(matcher.end());
      textComp.requestFocus();

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
   private JTextComponent textComp;
   private JCheckBox caseBox, regexBox, wrapBox;

   private JComponent replacePanel;

   private boolean found = false, updating=false;
}
