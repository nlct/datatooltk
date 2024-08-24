/*
    Copyright (C) 2013-2024 Nicola L.C. Talbot
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
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.event.*;

import com.dickimawbooks.datatooltk.base.*;

/**
 * Dialog box for replacing all instances of one string with
 * another for the entire database.
 */
public class ReplaceAllDialog extends JDialog
 implements ActionListener
{
   public ReplaceAllDialog(DatatoolGUI gui)
   {
      super(gui, gui.getMessageHandler().getLabel("replace.title"), true);
      this.gui = gui;

      CombinedMessageHandler messageHandler = gui.getMessageHandler();
      DatatoolGuiResources resources = gui.getResources();

      JComponent mainPanel = Box.createVerticalBox();
      getContentPane().add(mainPanel, BorderLayout.CENTER);

      JComponent panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      searchField = new JTextField();
      JLabel searchLabel = resources.createJLabel(
         "find.search_for", searchField);

      panel.add(searchLabel);
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
      JLabel replaceLabel = resources.createJLabel(
         "replace.replace_text", replaceField);

      panel.add(replaceLabel);
      panel.add(replaceField);

      Dimension dim = searchLabel.getPreferredSize();
      dim.width = Math.max(dim.width, 
         (int)replaceLabel.getPreferredSize().getWidth());

      searchLabel.setPreferredSize(dim);
      replaceLabel.setPreferredSize(dim);

      panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      caseBox = resources.createJCheckBox("find",
        "case", null);
      panel.add(caseBox);

      regexBox = resources.createJCheckBox("find",
        "regex", null);
      panel.add(regexBox);

      panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      panel.setBorder(
        BorderFactory.createTitledBorder(
           BorderFactory.createEtchedBorder(),
           messageHandler.getLabel("replace.all_in")));

      ButtonGroup bg = new ButtonGroup();

      replaceInRowButton =
        resources.createJRadioButton("replace", "in_row",
         bg, null);

      panel.add(replaceInRowButton);

      replaceInColButton =
        resources.createJRadioButton("replace", "in_column",
         bg, null);

      panel.add(replaceInColButton);

      replaceInDbButton =
        resources.createJRadioButton("replace", "in_db",
         bg, null);

      replaceInDbButton.setSelected(true);

      panel.add(replaceInDbButton);

      panel = new JPanel();
      getContentPane().add(panel, BorderLayout.SOUTH);

      replaceAllButton = resources.createActionButton("replace",
         "replace_all", this, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
      panel.add(replaceAllButton);

      getRootPane().setDefaultButton(replaceAllButton);

      panel.add(resources.createActionButton("find",
        "close", this, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));

      pack();
      setLocationRelativeTo(gui);
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("replace_all"))
      {
         replaceAll();
      }
      else if (action.equals("close"))
      {
         setVisible(false);
      }
   }

   public void display(DatatoolDbPanel panel)
   {
      dbPanel = panel;

      replaceInRowButton.setEnabled(panel.getModelSelectedRow() != -1);
      replaceInColButton.setEnabled(panel.getModelSelectedColumn() != -1);

      if ((!replaceInRowButton.isEnabled() && replaceInRowButton.isSelected())
        ||(!replaceInColButton.isEnabled() && replaceInColButton.isSelected()))
      {
         replaceAllButton.setSelected(true);
      }

      updateButtons();

      setVisible(true);
   }

   public void updateButtons()
   {
      replaceAllButton.setEnabled(!searchField.getText().isEmpty());
   }

   private void replaceAll()
   {
      if (replaceInRowButton.isSelected())
      {
         dbPanel.replaceAllInRow(dbPanel.getModelSelectedRow(),
           searchField.getText(), replaceField.getText(),
           caseBox.isSelected(), regexBox.isSelected());
      }
      else if (replaceInColButton.isSelected())
      {
         dbPanel.replaceAllInColumn(dbPanel.getModelSelectedColumn(),
           searchField.getText(), replaceField.getText(),
           caseBox.isSelected(), regexBox.isSelected());
      }
      else
      {
         dbPanel.replaceAll(searchField.getText(), replaceField.getText(),
            caseBox.isSelected(), regexBox.isSelected());
      }
   }

   private DatatoolGUI gui;

   private DatatoolDbPanel dbPanel;

   private JTextField searchField, replaceField;

   private JCheckBox regexBox, caseBox;

   private JRadioButton replaceInRowButton, replaceInColButton,
     replaceInDbButton;

   private JButton replaceAllButton;
}
