package com.dickimawbooks.datatooltk.gui;

import java.io.File;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.*;

public class PropertiesDialog extends JDialog
  implements ActionListener
{
   public PropertiesDialog(DatatoolGUI gui)
   {
      super(gui, DatatoolTk.getLabel("preferences.title"), true);

      tabbedPane = new JTabbedPane();

      getContentPane().add(tabbedPane, BorderLayout.CENTER);

      // General tab

      JComponent generalTab = addTab("general");

      ButtonGroup bg = new ButtonGroup();

      homeButton = createRadioButton("preferences.general", "home", bg);
      generalTab.add(homeButton);

      cwdButton = createRadioButton("preferences.general", "cwd", bg);
      generalTab.add(cwdButton);

      lastButton = createRadioButton("preferences.general", "last", bg);
      generalTab.add(lastButton);

      Box box = Box.createHorizontalBox();
      box.setAlignmentX(0);
      generalTab.add(box);

      customButton = createRadioButton("preferences.general", "custom", bg);
      box.add(customButton);

      fileChooser = new JFileChooser();

      customFileField = new FileField(generalTab, fileChooser,
         JFileChooser.DIRECTORIES_ONLY);

      box.add(customFileField);

      // CSV tab

      JComponent csvTab = addTab("csv");

      // SQL tab

      JComponent sqlTab = addTab("sql");

      JPanel buttonPanel = new JPanel();

      buttonPanel.add(DatatoolGuiResources.createOkayButton(this));
      buttonPanel.add(DatatoolGuiResources.createCancelButton(this));
      buttonPanel.add(gui.createHelpButton("properties"));

      getContentPane().add(buttonPanel, BorderLayout.SOUTH);
      pack();

      setLocationRelativeTo(null);
   }

   private JComponent addTab(String label)
   {
      JComponent tab = Box.createVerticalBox();

      int index = tabbedPane.getTabCount();

      tabbedPane.addTab(DatatoolTk.getLabel("preferences", label), 
         new JScrollPane(tab));

      String tooltip = DatatoolTk.getToolTip("preferences", label);

      if (tooltip != null)
      {
         tabbedPane.setToolTipTextAt(index, tooltip);
      }

      tabbedPane.setMnemonicAt(index,
         DatatoolTk.getMnemonic("preferences", label));

      return tab;
   }

   private JRadioButton createRadioButton(String parentLabel,
      String label, ButtonGroup bg)
   {
      JRadioButton button = new JRadioButton(
        DatatoolTk.getLabel(parentLabel, label));

      button.setMnemonic(DatatoolTk.getMnemonic(parentLabel, label));
      button.setAlignmentX(0);
      button.setOpaque(false);

      String tooltip = DatatoolTk.getToolTip(parentLabel, label);

      if (tooltip != null)
      {
         button.setToolTipText(tooltip);
      }

      button.setActionCommand(label);
      button.addActionListener(this);

      bg.add(button);

      return button;
   }

   public void display(DatatoolSettings settings)
   {
      this.settings = settings;

      switch (settings.getStartUp())
      {
         case DatatoolSettings.STARTUP_HOME:
           homeButton.setSelected(true);
           customFileField.setEnabled(false);
         break;
         case DatatoolSettings.STARTUP_CWD:
           cwdButton.setSelected(true);
           customFileField.setEnabled(false);
         break;
         case DatatoolSettings.STARTUP_LAST:
           lastButton.setSelected(true);
           customFileField.setEnabled(false);
         break;
         case DatatoolSettings.STARTUP_CUSTOM:
           customButton.setSelected(true);
           customFileField.setEnabled(true);
           customFileField.setFile(settings.getStartUpDirectory());
         break;
      }

      setVisible(true);
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
         setVisible(false);
      }
      else if (action.equals("home") || action.equals("cwd")
        || action.equals("last"))
      {
         customFileField.setEnabled(false);
      }
      else if (action.equals("custom"))
      {
         customFileField.setEnabled(true);
         customFileField.requestFocusInWindow();
      }
   }

   private void okay()
   {
      if (homeButton.isSelected())
      {
         settings.setStartUp(DatatoolSettings.STARTUP_HOME);
      }
      else if (cwdButton.isSelected())
      {
         settings.setStartUp(DatatoolSettings.STARTUP_CWD);
      }
      else if (lastButton.isSelected())
      {
         settings.setStartUp(DatatoolSettings.STARTUP_LAST);
      }
      else if (customButton.isSelected())
      {
         File file = customFileField.getFile();

         if (file == null)
         {
            DatatoolGuiResources.error(this, 
               DatatoolTk.getLabel("error.missing_custom_file"));

            return;
         }

         settings.setCustomStartUp(file);
      }

      setVisible(false);
   }

   private DatatoolSettings settings;

   private JRadioButton homeButton, cwdButton, lastButton, customButton;

   private JRadioButton sepTabButton, sepCharButton;

   private JCheckBox hasHeaderBox;

   private FileField customFileField;

   private JFileChooser fileChooser;

   private JTabbedPane tabbedPane;
}
