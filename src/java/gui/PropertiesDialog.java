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
      this.gui = gui;

      tabbedPane = new JTabbedPane();

      getContentPane().add(tabbedPane, BorderLayout.CENTER);

      // General tab

      JComponent startupTab = addTab("startup");

      ButtonGroup bg = new ButtonGroup();

      homeButton = createRadioButton("preferences.startup", "home", bg);
      startupTab.add(homeButton);

      cwdButton = createRadioButton("preferences.startup", "cwd", bg);
      startupTab.add(cwdButton);

      lastButton = createRadioButton("preferences.startup", "last", bg);
      startupTab.add(lastButton);

      JComponent box = Box.createHorizontalBox();
      box.setAlignmentX(0);
      startupTab.add(box);

      customButton = createRadioButton("preferences.startup", "custom", bg);
      box.add(customButton);

      fileChooser = new JFileChooser();

      customFileField = new FileField(startupTab, fileChooser,
         JFileChooser.DIRECTORIES_ONLY);

      box.add(customFileField);

      // CSV tab

      JComponent csvTab = addTab("csv");

      box = Box.createHorizontalBox();
      box.setAlignmentX(0);
      csvTab.add(box);

      box.add(new JLabel(DatatoolTk.getLabel("preferences.csv.sep")));

      bg = new ButtonGroup();

      sepTabButton = createRadioButton("preferences.csv", "tabsep", bg);
      box.add(sepTabButton);

      box.add(new JLabel(DatatoolTk.getLabel("preferences.csv.or")));

      sepCharButton = createRadioButton("preferences.csv", "sepchar", bg);
      box.add(sepCharButton);

      sepCharField = new CharField(',');
      box.add(sepCharField);

      box = Box.createHorizontalBox();
      box.setAlignmentX(0);
      csvTab.add(box);

      delimCharField = new CharField('"');

      box.add(createLabel("preferences.csv.delim", delimCharField));
      box.add(delimCharField);

      hasHeaderBox = createCheckBox("preferences.csv", "hasheader");
      csvTab.add(hasHeaderBox);

      // SQL tab

      JComponent sqlTab = addTab("sql");

      JLabel[] labels = new JLabel[5];
      int idx = 0;
      int maxWidth = 0;
      Dimension dim;

      box = createNewRow(sqlTab);

      hostField = new JTextField(16);

      labels[idx] = createLabel("preferences.sql.host", hostField);
      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);
      box.add(labels[idx++]);
      box.add(hostField);

      box = createNewRow(sqlTab);

      portField = new NonNegativeIntField(3306);

      labels[idx] = createLabel("preferences.sql.port", portField);
      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);
      box.add(labels[idx++]);

      box.add(portField);

      box = createNewRow(sqlTab);

      prefixField = new JTextField(16);

      labels[idx] = createLabel("preferences.sql.prefix", prefixField);
      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);
      box.add(labels[idx++]);
      box.add(prefixField);

      box = createNewRow(sqlTab);

      databaseField = new JTextField(16);

      labels[idx] = createLabel("preferences.sql.database", databaseField);
      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);
      box.add(labels[idx++]);
      box.add(databaseField);

      box = createNewRow(sqlTab);

      userField = new JTextField(16);

      labels[idx] = createLabel("preferences.sql.user", userField);
      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);
      box.add(labels[idx++]);
      box.add(userField);

      for (idx = 0; idx < labels.length; idx++)
      {
         dim = labels[idx].getPreferredSize();
         dim.width = maxWidth;
         labels[idx].setPreferredSize(dim);
      }

      wipeBox = createCheckBox("preferences.sql", "wipe");
      sqlTab.add(wipeBox);

      // TeX Tab

      JComponent texTab = addTab("tex");

      mapTeXBox = createCheckBox("preferences.tex", "map");
      texTab.add(mapTeXBox);

      box = createNewRow(texTab);
      latexFileField = new FileField(this, "latex", fileChooser);
      box.add(createLabel("preferences.tex.latexapp", latexFileField));
      box.add(latexFileField);

      // Display Tab

      JComponent displayTab =
         addTab(new JPanel(new FlowLayout(FlowLayout.LEFT)), "display");

      displayTab.setAlignmentY(0);
      JComponent leftPanel = Box.createVerticalBox();
      leftPanel.setAlignmentY(0);
      displayTab.add(leftPanel);

      labels = new JLabel[3];
      idx = 0;
      maxWidth = 0;

      box = createNewRow(leftPanel);

      GraphicsEnvironment env =
         GraphicsEnvironment.getLocalGraphicsEnvironment();

      fontBox = new JComboBox<String>(env.getAvailableFontFamilyNames());

      labels[idx] = createLabel("preferences.display.font", fontBox);
      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);
      box.add(labels[idx++]);
      box.add(fontBox);

      box = createNewRow(leftPanel);
      sizeField = new NonNegativeIntField(10);

      labels[idx] = createLabel("preferences.display.fontsize", sizeField);
      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);
      box.add(labels[idx++]);
      box.add(sizeField);

      box = createNewRow(leftPanel);
      cellHeightField = new NonNegativeIntField(4);

      labels[idx] = createLabel("preferences.display.cellheight", cellHeightField);
      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);
      box.add(labels[idx++]);
      box.add(cellHeightField);

      for (idx = 0; idx < labels.length; idx++)
      {
         dim = labels[idx].getPreferredSize();
         dim.width = maxWidth;
         labels[idx].setPreferredSize(dim);
      }

      JComponent rightPanel = Box.createVerticalBox();
      rightPanel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), 
        DatatoolTk.getLabel("preferences.display.cellwidths")));
      rightPanel.setAlignmentY(0);
      displayTab.add(rightPanel);

      cellWidthFields = new NonNegativeIntField[DatatoolDb.TYPE_LABELS.length];
      labels = new JLabel[DatatoolDb.TYPE_LABELS.length];

      for (int i = 0; i < cellWidthFields.length; i++)
      {
         box = createNewRow(rightPanel);
         cellWidthFields[i] = new NonNegativeIntField(0);
         labels[i] = new JLabel(DatatoolDb.TYPE_LABELS[i]);
         if (DatatoolDb.TYPE_MNEMONICS[i] != -1)
         {
            labels[i].setDisplayedMnemonic(DatatoolDb.TYPE_MNEMONICS[i]);
            labels[i].setLabelFor(cellWidthFields[i]);
         }

         dim = labels[i].getPreferredSize();
         maxWidth = Math.max(maxWidth, dim.width);
         box.add(labels[i]);
         box.add(cellWidthFields[i]);
      }

      for (idx = 0; idx < labels.length; idx++)
      {
         dim = labels[idx].getPreferredSize();
         dim.width = maxWidth;
         labels[idx].setPreferredSize(dim);
      }

      getContentPane().add(
        DatatoolGuiResources.createOkayCancelHelpPanel(this, gui, "properties"),
        BorderLayout.SOUTH);
      pack();

      setLocationRelativeTo(null);
   }

   private JComponent addTab(String label)
   {
      return addTab(Box.createVerticalBox(), label);
   }

   private JComponent addTab(JComponent tab, String label)
   {
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

   private JComponent createNewRow(JComponent tab)
   {
      JComponent comp = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
      comp.setAlignmentX(0);
      tab.add(comp);

      return comp;
   }

   private JRadioButton createRadioButton(String parentLabel,
      String label, ButtonGroup bg)
   {
      JRadioButton button = DatatoolGuiResources.createJRadioButton(parentLabel,
         label, bg, this);


      button.setAlignmentX(0);
      button.setOpaque(false);

      return button;
   }

   private JLabel createLabel(String label, JComponent comp)
   {
      return DatatoolGuiResources.createJLabel(label, comp);
   }

   private JCheckBox createCheckBox(String parentLabel, String label)
   {
      JCheckBox checkBox = DatatoolGuiResources.createJCheckBox(parentLabel, label, this);

      checkBox.setAlignmentX(0);

      return checkBox;
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

      char sep = settings.getSeparator();

      if (sep == '\t')
      {
         sepTabButton.setSelected(true);
         sepCharField.setEnabled(false);
      }
      else
      {
         sepCharButton.setSelected(true);
         sepCharField.setEnabled(true);
      }

      delimCharField.setValue(settings.getDelimiter());

      hasHeaderBox.setSelected(settings.hasCSVHeader());

      hostField.setText(settings.getSqlHost());
      prefixField.setText(settings.getSqlPrefix());
      portField.setValue(settings.getSqlPort());
      wipeBox.setSelected(settings.isWipePasswordEnabled());

      String user = settings.getSqlUser();

      userField.setText(user == null ? "" : user);

      String db = settings.getSqlDbName();

      databaseField.setText(db == null ? "" : db);

      mapTeXBox.setSelected(settings.isTeXMappingOn());

      latexFileField.setFileName(settings.getLaTeX());

      sizeField.setValue(settings.getFontSize());
      fontBox.setSelectedItem(settings.getFontName());
      cellHeightField.setValue(settings.getCellHeight());

      for (int i = 0; i < cellWidthFields.length; i++)
      {
         cellWidthFields[i].setValue(settings.getCellWidth(i-1));
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
      else if (action.equals("tabsep"))
      {
         sepCharField.setEnabled(false);
      }
      else if (action.equals("sepchar"))
      {
         sepCharField.setEnabled(true);
         sepCharField.requestFocusInWindow();
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

      if (sepTabButton.isSelected())
      {
         settings.setSeparator('\t');
      }
      else
      {
         char sep = sepCharField.getValue();

         if (sep == (char)0)
         {
            DatatoolGuiResources.error(this, 
               DatatoolTk.getLabel("error.missing_sep"));
            return;
         }

         settings.setSeparator(sep);
      }

      char delim = delimCharField.getValue();

      if (delim == (char)0)
      {
         DatatoolGuiResources.error(this, 
            DatatoolTk.getLabel("error.missing_delim"));
         return;
      }

      settings.setDelimiter(delim);

      settings.setHasCSVHeader(hasHeaderBox.isSelected());

      String host = hostField.getText();

      if (host.isEmpty())
      {
         DatatoolGuiResources.error(this, 
            DatatoolTk.getLabel("error.missing_host"));
         return;
      }

      settings.setSqlHost(host);

      String prefix = prefixField.getText();

      if (prefix.isEmpty())
      {
         DatatoolGuiResources.error(this,
            DatatoolTk.getLabel("error.missing_prefix"));
         return;
      }

      settings.setSqlPrefix(prefix);

      if (portField.getText().isEmpty())
      {
         DatatoolGuiResources.error(this,
            DatatoolTk.getLabel("error.missing_port"));
         return;
      }

      settings.setSqlPort(portField.getValue());

      settings.setSqlUser(userField.getText());
      settings.setSqlDbName(databaseField.getText());

      settings.setTeXMapping(mapTeXBox.isSelected());

      settings.setLaTeX(latexFileField.getFileName());

      settings.setFontName(fontBox.getSelectedItem().toString());
      settings.setFontSize(sizeField.getValue());
      settings.setCellHeight(cellHeightField.getValue());

      for (int i = 0; i < cellWidthFields.length; i++)
      {
         settings.setCellWidth(cellWidthFields[i].getValue(), i-1);
      }

      gui.updateTableSettings();

      setVisible(false);
   }

   private DatatoolSettings settings;

   private JRadioButton homeButton, cwdButton, lastButton, customButton;

   private JRadioButton sepTabButton, sepCharButton;

   private CharField sepCharField, delimCharField;

   private JCheckBox hasHeaderBox, wipeBox, mapTeXBox;

   private FileField customFileField, latexFileField;

   private JFileChooser fileChooser;

   private NonNegativeIntField portField, sizeField, cellHeightField;

   private NonNegativeIntField[] cellWidthFields;

   private JTextField hostField, prefixField, databaseField, userField;

   private JComboBox<String> fontBox;

   private JTabbedPane tabbedPane;

   private DatatoolGUI gui;
}
