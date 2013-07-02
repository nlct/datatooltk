package com.dickimawbooks.datatooltk.gui;

import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.*;
import javax.swing.event.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

public class ReplaceAllDialog extends JDialog
 implements ActionListener
{
   public ReplaceAllDialog(DatatoolGUI gui)
   {
      super(gui, DatatoolTk.getLabel("replace.title"), true);
      this.gui = gui;

      JComponent mainPanel = Box.createVerticalBox();
      getContentPane().add(mainPanel, BorderLayout.CENTER);

      JComponent panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      searchField = new JTextField();
      JLabel searchLabel = DatatoolGuiResources.createJLabel(
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
      JLabel replaceLabel = DatatoolGuiResources.createJLabel(
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

      caseBox = DatatoolGuiResources.createJCheckBox("find",
        "case", null);
      panel.add(caseBox);

      regexBox = DatatoolGuiResources.createJCheckBox("find",
        "regex", null);
      panel.add(regexBox);

      panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      panel.setBorder(
        BorderFactory.createTitledBorder(
           BorderFactory.createEtchedBorder(),
           DatatoolTk.getLabel("replace.all_in")));

      ButtonGroup bg = new ButtonGroup();

      replaceInRowButton =
        DatatoolGuiResources.createJRadioButton("replace", "in_row",
         bg, null);

      panel.add(replaceInRowButton);

      replaceInColButton =
        DatatoolGuiResources.createJRadioButton("replace", "in_column",
         bg, null);

      panel.add(replaceInColButton);

      replaceInDbButton =
        DatatoolGuiResources.createJRadioButton("replace", "in_db",
         bg, null);

      replaceInDbButton.setSelected(true);

      panel.add(replaceInDbButton);

      panel = new JPanel();
      getContentPane().add(panel, BorderLayout.SOUTH);

      replaceAllButton = DatatoolGuiResources.createActionButton("replace",
         "replace_all", this, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
      panel.add(replaceAllButton);

      getRootPane().setDefaultButton(replaceAllButton);

      panel.add(DatatoolGuiResources.createActionButton("find",
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

      replaceInRowButton.setEnabled(panel.getSelectedRow() != -1);
      replaceInColButton.setEnabled(panel.getSelectedColumn() != -1);

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
         dbPanel.replaceAllInRow(dbPanel.getSelectedRow(),
           searchField.getText(), replaceField.getText(),
           caseBox.isSelected(), regexBox.isSelected());
      }
      else if (replaceInColButton.isSelected())
      {
         dbPanel.replaceAllInColumn(dbPanel.getSelectedColumn(),
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
