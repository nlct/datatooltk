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

import java.io.File;
import java.nio.charset.Charset;
import java.net.URISyntaxException;

import java.util.*;
import java.util.regex.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;

import com.dickimawbooks.texjavahelplib.HelpSetLocale;
import com.dickimawbooks.texjavahelplib.IconSet;
import com.dickimawbooks.texjavahelplib.JLabelGroup;
import com.dickimawbooks.texjavahelplib.TeXJavaHelpLib;

import com.dickimawbooks.datatooltk.*;
import com.dickimawbooks.datatooltk.io.DatatoolFileFormat;
import com.dickimawbooks.datatooltk.io.UnsupportedFileFormatException;

/**
 * Dialog box to allow user to edit application settings.
 */
public class PropertiesDialog extends JDialog
  implements ActionListener,ListSelectionListener,MouseListener
{
   public PropertiesDialog(DatatoolGUI gui)
   {
      super(gui, gui.getMessageHandler().getLabel("preferences.title"), true);
      this.gui = gui;

      settings = gui.getSettings();
      DatatoolGuiResources resources = gui.getResources();

      tabbedPane = new JTabbedPane();

      getContentPane().add(tabbedPane, BorderLayout.CENTER);

      // General tab

      createGeneralTab();

      // CSV tab

      createCsvTab();

      // SQL tab

      if (gui.getDatatoolTk().isSQLSupported())
      {
         createSqlTab();
      }

      // TeX Tab

      createTeXTab();

      // Currencies Tab

      createCurrenciesTab();

      // Display Tab

      createDisplayTab();

      // Cell Editor Tab

      createEditorTab();

      // Language Tab

      createLanguageTab();

      // Plugins tab

      createPluginsTab();

      getContentPane().add(
        resources.createDialogOkayCancelHelpPanel(this, this, gui, "preferences"),
        BorderLayout.SOUTH);
      pack();

      setLocationRelativeTo(null);
   }

   private JComponent createGeneralTab()
   {
      DatatoolGuiResources resources = gui.getResources();
      MessageHandler messageHandler = gui.getMessageHandler();

      JComponent generalTab = addTab("general");

      JComponent startupComp = Box.createVerticalBox();
      startupComp.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(),
        messageHandler.getLabel("preferences.startup")));
      generalTab.add(startupComp);

      ButtonGroup bg = new ButtonGroup();

      homeButton = createRadioButton("preferences.startup", "home", bg);
      startupComp.add(homeButton);

      cwdButton = createRadioButton("preferences.startup", "cwd", bg);
      startupComp.add(cwdButton);

      lastButton = createRadioButton("preferences.startup", "last", bg);
      startupComp.add(lastButton);

      JComponent box = Box.createHorizontalBox();
      box.setAlignmentX(0);
      startupComp.add(box);

      customButton = createRadioButton("preferences.startup", "custom", bg);
      box.add(customButton);

      fileChooser = new JFileChooser();

      customFileField = new FileField(resources, startupComp, fileChooser,
         JFileChooser.DIRECTORIES_ONLY);

      box.add(customFileField);

      autoTrimBox = resources.createJCheckBox("preferences", "autotrim",null,0);
      generalTab.add(autoTrimBox);

      JComponent shuffleComp = Box.createVerticalBox();
      shuffleComp.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(),
        messageHandler.getLabel("preferences.shuffle")
      ));

      generalTab.add(shuffleComp);

      box = new JPanel(new FlowLayout(FlowLayout.LEFT));
      box.setAlignmentX(0);
      shuffleComp.add(box);

      hasSeedBox = resources.createJCheckBox("preferences.shuffle",
         "seed", this);
      box.add(hasSeedBox);

      seedField = new NonNegativeIntField(0);
      seedField.setText("");
      seedField.setEnabled(false);
      box.add(seedField);

      JComponent capacityBox = Box.createVerticalBox();
      capacityBox.setAlignmentX(0);
      generalTab.add(capacityBox);

      capacityBox.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(),
        messageHandler.getLabel("preferences.initial.capacities")));

      box = new JPanel(new FlowLayout(FlowLayout.LEFT));
      box.setAlignmentX(0);
      capacityBox.add(box);

      initialRowCapacitySpinner = new JSpinner(
         new SpinnerNumberModel(16, 10, Integer.MAX_VALUE, 1));

      JLabelGroup labelGrp = new JLabelGroup();

      JLabel rowCapacityLabel = resources.createJLabel(labelGrp,
        "preferences.initial.row.capacity", 
        initialRowCapacitySpinner);

      box.add(rowCapacityLabel);
      box.add(initialRowCapacitySpinner);

      box = new JPanel(new FlowLayout(FlowLayout.LEFT));
      box.setAlignmentX(0);
      capacityBox.add(box);

      initialColumnCapacitySpinner = new JSpinner(
         new SpinnerNumberModel(16, 10, Integer.MAX_VALUE, 1));

      JLabel columnCapacityLabel = resources.createJLabel(labelGrp,
        "preferences.initial.column.capacity", 
        initialColumnCapacitySpinner);

      box.add(columnCapacityLabel);
      box.add(initialColumnCapacitySpinner);

      return generalTab;
   }

   private JComponent createCsvTab()
   {
      DatatoolGuiResources resources = gui.getResources();

      JComponent csvTab = addTab("csv");

      csvSettingsPanel = new IOSettingsPanel(this, resources, "preferences",
        IOSettingsPanel.IO_IN, 
        DatatoolFileFormat.FILE_FORMAT_CSV_OR_TSV
      | DatatoolFileFormat.FILE_FORMAT_ANY_SPREADSHEET,
        true, false, false);

      try
      {
         csvSettingsPanel.setSelectedFileFormat(
           DatatoolFileFormat.FILE_FORMAT_FLAG_CSV);
         csvSettingsPanel.setFileFormatComponentVisible(false);
      }
      catch (UnsupportedFileFormatException e)
      {// shouldn't happen
         resources.error(null, e);
      }

      csvTab.add(csvSettingsPanel);

      return csvTab;
   }

   private JComponent createSqlTab()
   {
      JComponent sqlTab = addTab("sql");

      sqlSettingsPanel = new SqlPanel(gui, "preferences");
      sqlTab.add(sqlSettingsPanel);

      return sqlTab;
   }

   private JComponent createTeXTab()
   {
      DatatoolGuiResources resources = gui.getResources();
      MessageHandler messageHandler = gui.getMessageHandler();

      JComponent texTab = addTab("tex");

      JComponent box = createNewRow(texTab);
      texEncodingBox = new JComboBox<Charset>(
        Charset.availableCharsets().values().toArray(new Charset[0]));
      box.add(createLabel("preferences.tex.encoding", texEncodingBox));
      box.add(texEncodingBox);

      JComponent outputComp = Box.createVerticalBox();
      texTab.add(outputComp);

      outputComp.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), 
        messageHandler.getLabel("preferences.tex.output")));

      box = createNewRow(outputComp);
      outputFormatBox = new JComboBox<String>(
       new String[] {"DBTEX 3.0", "DTLTEX 3.0", "DBTEX 2.0", "DTLTEX 2.0"});

      box.add(createLabel("preferences.tex.format", outputFormatBox));
      box.add(outputFormatBox);

      overrideInputFormatBox = createCheckBox("preferences", "tex.override_format", null);
      box.add(overrideInputFormatBox);

      box = createNewRow(outputComp);
      box.add(createLabel("preferences.tex.save_datum"));

      box = createNewRow(outputComp);

      ButtonGroup bg = new ButtonGroup();

      saveDatumNoneBox = createRadioButton("preferences.tex", "save_datum.none", bg);
      box.add(saveDatumNoneBox);

      saveDatumAllBox = createRadioButton("preferences.tex", "save_datum.all", bg);
      box.add(saveDatumAllBox);

      saveDatumHeaderBox = createRadioButton("preferences.tex", "save_datum.header_type", bg); 
      box.add(saveDatumHeaderBox);

      saveDatumEntryBox = createRadioButton("preferences.tex", "save_datum.entry_type", bg);
      box.add(saveDatumEntryBox);

      datumTypeComp = createNewRow(outputComp);
      datumTypeComp.add(createLabel("preferences.tex.save_datum.type"));

      saveDatumIntegerBox =
         createCheckBox("preferences.tex", "save_datum.type.int", null);
      datumTypeComp.add(saveDatumIntegerBox);

      saveDatumDecimalBox =
         createCheckBox("preferences.tex", "save_datum.type.decimal", null);
      datumTypeComp.add(saveDatumDecimalBox);

      saveDatumCurrencyBox =
         createCheckBox("preferences.tex", "save_datum.type.currency", null);
      datumTypeComp.add(saveDatumCurrencyBox);

      saveDatumStringBox =
         createCheckBox("preferences.tex", "save_datum.type.string");
      datumTypeComp.add(saveDatumStringBox);

      JComponent importComp = Box.createVerticalBox();
      texTab.add(importComp);

      importComp.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), 
        messageHandler.getLabel("preferences.tex.import")));

      stripSolnEnvBox = createCheckBox("preferences.tex", "stripsolnenv", null);
      importComp.add(stripSolnEnvBox);

      preambleOnlyBox = createCheckBox("preferences.tex", "preambleonly", null);
      importComp.add(preambleOnlyBox);

      JTextArea msgArea = resources.createMessageArea(0, 0);
      msgArea.setText(messageHandler.getLabelWithValues(
         "preferences.tex.other_import",
         messageHandler.getLabel("preferences.csv")));
      msgArea.setAlignmentX(0f);

      importComp.add(msgArea);

      return texTab;
   }

   private JComponent createCurrenciesTab()
   {
      DatatoolGuiResources resources = gui.getResources();
      MessageHandler messageHandler = gui.getMessageHandler();

      JComponent currencyTab = 
         addTab(new JPanel(new BorderLayout()), "currencies");

      currencyList = new JList<String>();
      currencyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      currencyList.setVisibleRowCount(10);

      currencyTab.add(new JScrollPane(currencyList), BorderLayout.CENTER);

      JComponent buttonPanel = Box.createVerticalBox();
      currencyTab.add(buttonPanel, BorderLayout.EAST);

      currencyTab.add(createTextArea("preferences.currencies.reminder"),
        BorderLayout.NORTH);

      buttonPanel.add(resources.createActionButton(
         "preferences.currencies", "add_currency", "increase", this, null));

      editCurrencyButton = resources.createActionButton(
         "preferences.currencies", "edit_currency", "edit", this, null);
      buttonPanel.add(editCurrencyButton);

      removeCurrencyButton = resources.createActionButton(
         "preferences.currencies", "remove_currency", "decrease", this, null);
      buttonPanel.add(removeCurrencyButton);

      currencyList.addListSelectionListener(this);
      currencyList.addMouseListener(this);

      return currencyTab;
   }

   private JComponent createEditorTab()
   {
      JComponent tab =
         addTab(new JPanel(new FlowLayout(FlowLayout.LEFT)), "editor");

      editorPropComp = new EditorPropertiesComponent(gui);
      editorPropComp.setAlignmentY(0);
      tab.add(editorPropComp);

      return tab;
   }

   private JComponent createDisplayTab()
   {
      DatatoolGuiResources resources = gui.getResources();
      MessageHandler messageHandler = gui.getMessageHandler();

      JComponent displayTab =
         addTab(new JPanel(new FlowLayout(FlowLayout.LEFT)), "display");

      JComponent leftPanel = Box.createVerticalBox();
      leftPanel.setAlignmentY(0);
      displayTab.add(leftPanel);

      JComponent lookAndFeelPanel = Box.createVerticalBox();
      lookAndFeelPanel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), 
        messageHandler.getLabel("preferences.display.lookandfeel.title")));
      leftPanel.add(lookAndFeelPanel);

      JComponent box = createNewRow(lookAndFeelPanel);

      availableLookAndFeels = UIManager.getInstalledLookAndFeels();

      String[] names = new String[availableLookAndFeels.length];

      for (int i = 0; i < names.length; i++)
      {
         names[i] = availableLookAndFeels[i].getName();
      }

      lookAndFeelBox = new JComboBox<String>(names);

      box.add(createLabel("preferences.display.lookandfeel", lookAndFeelBox));
      box.add(lookAndFeelBox);

      lookAndFeelPanel.add(createLabel("preferences.display.buttonsize"));

      box = createNewRow(lookAndFeelPanel);

      ButtonGroup grp = new ButtonGroup();

      IconSet icSet = getHelpLib().getHelpIconSet("preferences", "-24");
      toolBarIconSizeButton24 = icSet.createIconRadioButton();
      box.add(toolBarIconSizeButton24);
      grp.add(toolBarIconSizeButton24);

      icSet = getHelpLib().getHelpIconSet("preferences", "-32");
      toolBarIconSizeButton32 = icSet.createIconRadioButton();
      box.add(toolBarIconSizeButton32);
      grp.add(toolBarIconSizeButton32);

      icSet = getHelpLib().getHelpIconSet("preferences", "-64");
      toolBarIconSizeButton64 = icSet.createIconRadioButton();
      box.add(toolBarIconSizeButton64);
      grp.add(toolBarIconSizeButton64);

      lookAndFeelPanel.add(createLabel("preferences.display.smallbuttonsize"));

      box = createNewRow(lookAndFeelPanel);

      grp = new ButtonGroup();

      icSet = getHelpLib().getHelpIconSet("preferences", "-16");
      smallIconSizeButton16 = icSet.createIconRadioButton();
      box.add(smallIconSizeButton16);
      grp.add(smallIconSizeButton16);

      icSet = getHelpLib().getHelpIconSet("preferences", "-20");
      smallIconSizeButton20 = icSet.createIconRadioButton();
      box.add(smallIconSizeButton20);
      grp.add(smallIconSizeButton20);

      icSet = getHelpLib().getHelpIconSet("preferences", "-24");
      smallIconSizeButton24 = icSet.createIconRadioButton();
      box.add(smallIconSizeButton24);
      grp.add(smallIconSizeButton24);

      box = createNewRow(lookAndFeelPanel);
      box.add(createTextArea(4, 16, "preferences.display.lookandfeel.restart"));

      JComponent rightPanel = Box.createVerticalBox();
      rightPanel.setAlignmentY(0);
      displayTab.add(rightPanel);

      JComponent cellDimsPanel = Box.createVerticalBox();
      cellDimsPanel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), 
        messageHandler.getLabel("preferences.display.celldims")));
      cellDimsPanel.setAlignmentY(0);
      rightPanel.add(cellDimsPanel);

      box = createNewRow(cellDimsPanel);
      cellHeightModel = new SpinnerNumberModel(4, 1, 1000, 1);
      JSpinner cellHeightField = new JSpinner(cellHeightModel);

      JLabelGroup labelGrp = new JLabelGroup();

      box.add(createLabel(labelGrp,
         "preferences.display.cellheight", cellHeightField));
      box.add(cellHeightField);

      cellDimsPanel.add(resources.createJLabel("preferences.display.cellwidths"));

      String[] typeLabels = resources.getTypeLabels();
      int[] typeMnemonics = resources.getTypeMnemonics();

      cellWidthModels = new SpinnerNumberModel[typeLabels.length];

      for (int i = 0; i < cellWidthModels.length; i++)
      {
         box = createNewRow(cellDimsPanel);
         cellWidthModels[i] = new SpinnerNumberModel(0, 0, 1000, 1);
         JSpinner spinner = new JSpinner(cellWidthModels[i]);
         JLabel label = labelGrp.createJLabel(typeLabels[i]);

         if (typeMnemonics[i] != -1)
         {
            label.setDisplayedMnemonic(typeMnemonics[i]);
            label.setLabelFor(spinner);
         }

         box.add(label);
         box.add(spinner);
      }

      cellDatumVisibleBox = resources.createJCheckBox(
       "preferences.display", "cell_show_datum", null, 0);

      rightPanel.add(cellDatumVisibleBox);

      return displayTab;
   }

   private JComponent createLanguageTab()
   {
      DatatoolGuiResources resources = gui.getResources();
      MessageHandler messageHandler = gui.getMessageHandler();

      JComponent localisationTab =
         addTab(new JPanel(new BorderLayout()), "language");

      JComponent numericComp = Box.createVerticalBox();
      numericComp.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(),
         messageHandler.getLabel("preferences.language.numeric")));

      localisationTab.add(numericComp, BorderLayout.CENTER);

      JComponent languageComp = new JPanel(new BorderLayout());
      languageComp.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(),
         messageHandler.getLabel("preferences.language.interface")));

      localisationTab.add(languageComp, BorderLayout.SOUTH);

      languageComp.add(createTextArea("preferences.language.restart"),
         BorderLayout.NORTH);

      JComponent box = new JPanel(new FlowLayout());
      languageComp.add(box);

      helpsetLangBox = new JComboBox<HelpSetLocale>(gui.getAvailableHelpSets());

      JLabel helpsetLangLabel 
         = createLabel("preferences.language.helpset", helpsetLangBox);
      box.add(helpsetLangLabel);
      box.add(helpsetLangBox);

      dictLangBox = new JComboBox<HelpSetLocale>(gui.getAvailableDictionaries());

      JLabel dictLangLabel 
         = createLabel("preferences.language.dictionary", dictLangBox);
      box.add(dictLangLabel);
      box.add(dictLangBox);

      // Numeric Parsing

      box = createNewRow(numericComp);

      Locale[] allLocales = NumberFormat.getAvailableLocales();
      Arrays.parallelSort(allLocales,
        new Comparator<Locale>()
        {
           @Override
           public int compare(Locale o1, Locale o2)
           {
              return o1.getDisplayName().compareTo(o2.getDisplayName());
           }
        });

      numericLocaleBox = new JComboBox<Locale>(allLocales);
      numericLocaleBox.setRenderer(new LocaleListRenderer());
      numericLocaleBox.addItemListener(new ItemListener()
       {
          @Override
          public void itemStateChanged(ItemEvent evt)
          {
             if (evt.getStateChange() == ItemEvent.SELECTED)
             {
                decimalSymbolsUpdated((Locale)numericLocaleBox.getSelectedItem());
             }
          }
       });

      box.add(createLabel("preferences.language.numeric_locale",
        numericLocaleBox));
      box.add(numericLocaleBox);

      box = createNewRow(numericComp);

      decimalSymbolsField = new JEditorPane("text/html", "");
      decimalSymbolsField.setEditable(false);
      decimalSymbolsField.setOpaque(false);
      decimalSymbolsField.setBorder(BorderFactory.createEmptyBorder());
      decimalSymbolsUpdated(DecimalFormatSymbols.getInstance());

      box.add(decimalSymbolsField);

      box = createNewRow(numericComp);

      numericParsingLabel = createLabel("preferences.language.numeric_parsing");
      box.add(numericParsingLabel);

      ButtonGroup radioGrp = new ButtonGroup();

      numParseMatchLocaleButton = createRadioButton(
       "preferences.language", "numeric_parsing.match_locale", radioGrp);
      box.add(numParseMatchLocaleButton);

      numParsePatternButton = createRadioButton(
       "preferences.language", "numeric_parsing.pattern", radioGrp);
      box.add(numParsePatternButton);

      numericParserField = new JTextField(10);
      numericParserField.setEnabled(false);
      box.add(numericParserField);

      // Numeric Formatting

      box = createNewRow(numericComp);

      box.add(createLabel("preferences.language.numeric_formatting"));

      JLabelGroup labelGrp = new JLabelGroup();

      // Integer Formatting

      box = createNewRow(numericComp);

      intFmtLabel = createLabel(labelGrp,
         "preferences.language.numeric_formatting.int");
      box.add(intFmtLabel);

      radioGrp = new ButtonGroup();
      intFmtMatchLocaleButton = createRadioButton(
       "preferences.language.numeric_formatting", "int.match_locale", radioGrp);
      box.add(intFmtMatchLocaleButton);

      intFmtPatternButton = createRadioButton(
       "preferences.language.numeric_formatting", "int.pattern", radioGrp);
      box.add(intFmtPatternButton);

      intFormatterField = new JTextField(8);
      box.add(intFormatterField);

      // Currency Formatting

      box = createNewRow(numericComp);

      currencyFmtLabel = createLabel(labelGrp,
          "preferences.language.numeric_formatting.currency");
      box.add(currencyFmtLabel);

      radioGrp = new ButtonGroup();
      currencyFmtMatchLocaleButton = createRadioButton(
       "preferences.language.numeric_formatting", "currency.match_locale", radioGrp);
      box.add(currencyFmtMatchLocaleButton);

      currencyFmtPatternButton = createRadioButton(
       "preferences.language.numeric_formatting", "currency.pattern", radioGrp);
      box.add(currencyFmtPatternButton);

      currencyFormatterField = new JTextField(8);
      box.add(currencyFormatterField);

      // Decimal Formatting

      box = createNewRow(numericComp);

      decimalFmtLabel = createLabel(labelGrp,
        "preferences.language.numeric_formatting.decimal");
      box.add(decimalFmtLabel);

      radioGrp = new ButtonGroup();
      decimalFmtMatchLocaleButton = createRadioButton(
       "preferences.language.numeric_formatting", "decimal.match_locale", radioGrp);
      box.add(decimalFmtMatchLocaleButton);

      decimalFmtPatternButton = createRadioButton(
       "preferences.language.numeric_formatting", "decimal.pattern", radioGrp);
      box.add(decimalFmtPatternButton);

      decimalFormatterField = new JTextField(8);
      box.add(decimalFormatterField);

      decimalFmtSIButton = createRadioButton(
       "preferences.language.numeric_formatting", "decimal.siunitx", radioGrp);
      box.add(decimalFmtSIButton);

      return localisationTab;
   }

   private JComponent createPluginsTab()
   {
      DatatoolGuiResources resources = gui.getResources();

      JComponent pluginsTab = addTab("plugins");

      JComponent box = createNewRow(pluginsTab);
      box.add(resources.createMessageArea("preferences.plugins.note"));

      box = createNewRow(pluginsTab);
      perlFileField = new FileField(resources, this, "perl", fileChooser);
      box.add(createLabel("preferences.plugins.perl", perlFileField));
      box.add(perlFileField);

      return pluginsTab;
   }

   private JComponent addTab(String label)
   {
      return addTab(Box.createVerticalBox(), label);
   }

   private JComponent addTab(JComponent tab, String label)
   {
      DatatoolGuiResources resources = gui.getResources();

      int index = tabbedPane.getTabCount();

      JPanel panel = new JPanel();
      panel.setOpaque(true);
      panel.setBorder(BorderFactory.createEtchedBorder());
      panel.add(tab);

      tabbedPane.addTab(resources.getLabel("preferences", label), 
         new JScrollPane(panel));

      String tooltip = resources.getToolTip("preferences", label);

      if (tooltip != null)
      {
         tabbedPane.setToolTipTextAt(index, tooltip);
      }

      tabbedPane.setMnemonicAt(index,
         resources.getMnemonicInt("preferences", label));

      return tab;
   }

   private JComponent createNewRow(JComponent tab)
   {
      return createNewRow(tab, new FlowLayout(FlowLayout.LEADING, 4, 1));
   }

   private JComponent createNewRow(JComponent tab, LayoutManager layout)
   {
      JComponent comp = new JPanel(layout);
      comp.setAlignmentX(0);
      tab.add(comp);

      return comp;
   }

   private JComponent createNewRow(JComponent tab, String constraint)
   {
      return createNewRow(tab, new FlowLayout(FlowLayout.LEADING, 4, 1),
        constraint);
   }

   private JComponent createNewRow(JComponent tab, LayoutManager layout,
     Object constraints)
   {
      JComponent comp = new JPanel(layout);
      comp.setAlignmentX(0);
      tab.add(comp, constraints);

      return comp;
   }

   private JRadioButton createRadioButton(String parentLabel,
      String label, ButtonGroup bg)
   {
      JRadioButton button = gui.getResources().createJRadioButton(parentLabel,
         label, bg, this);


      button.setAlignmentX(0);
      button.setOpaque(false);

      return button;
   }

   private JLabel createLabel(String label)
   {
      return getResources().createJLabel(label);
   }

   private JLabel createLabel(JLabelGroup grp, String label)
   {
      return getResources().createJLabel(grp, label, null);
   }

   private JLabel createLabel(String label, JComponent comp)
   {
      return getResources().createJLabel(label, comp);
   }

   private JLabel createLabel(JLabelGroup grp, String label, JComponent comp)
   {
      return getResources().createJLabel(grp, label, comp);
   }

   private JTextArea createTextArea(String label)
   {
      return createTextArea(2, 40, label);
   }

   private JTextArea createTextArea(int rows, int columns, String label)
   {
      return getResources().createMessageArea(rows, columns, label);
   }

   private JCheckBox createCheckBox(String parentLabel, String label)
   {
      return createCheckBox(parentLabel, label, this);
   }

   private JCheckBox createCheckBox(String parentLabel, String label,
     ActionListener listener)
   {
      JCheckBox checkBox =
         getResources().createJCheckBox(parentLabel, label, listener);

      checkBox.setAlignmentX(0);

      return checkBox;
   }

   public void display(DatatoolProperties settings)
   {
      this.settings = settings;

      switch (settings.getStartUp())
      {
         case DatatoolProperties.STARTUP_HOME:
           homeButton.setSelected(true);
           customFileField.setEnabled(false);
         break;
         case DatatoolProperties.STARTUP_CWD:
           cwdButton.setSelected(true);
           customFileField.setEnabled(false);
         break;
         case DatatoolProperties.STARTUP_LAST:
           lastButton.setSelected(true);
           customFileField.setEnabled(false);
         break;
         case DatatoolProperties.STARTUP_CUSTOM:
           customButton.setSelected(true);
           customFileField.setEnabled(true);
           customFileField.setFile(settings.getStartUpDirectory());
         break;
      }

      initialRowCapacitySpinner.setValue(Integer.valueOf(
        settings.getInitialRowCapacity()));

      initialColumnCapacitySpinner.setValue(Integer.valueOf(
        settings.getInitialColumnCapacity()));

      csvSettingsPanel.setCsvSettingsFrom(settings);

      if (sqlSettingsPanel != null)
      {
         sqlSettingsPanel.resetFrom(settings);
      }

      stripSolnEnvBox.setSelected(settings.isSolutionEnvStrippedDefault());
      preambleOnlyBox.setSelected(settings.isPreambleOnlyDefault());

      String outFmt = settings.getDefaultOutputFormat();

      if (outFmt != null)
      {
         outputFormatBox.setSelectedItem(outFmt);
      }

      overrideInputFormatBox.setSelected(settings.getOverrideInputFormat());

      switch (settings.getDbTeX3DatumValue())
      {
         case ALL:
            saveDatumAllBox.setSelected(true);
            datumTypeComp.setVisible(false);
         break;
         case NONE:
            saveDatumNoneBox.setSelected(true);
            datumTypeComp.setVisible(false);
         break;
         case HEADER:
            saveDatumHeaderBox.setSelected(true);
            datumTypeComp.setVisible(true);
         break;
         case CELL:
            saveDatumEntryBox.setSelected(true);
            datumTypeComp.setVisible(true);
         break;
      }

      saveDatumStringBox.setSelected(settings.isStringDbTeX3DatumValue());
      saveDatumIntegerBox.setSelected(settings.isIntegerDbTeX3DatumValue());
      saveDatumDecimalBox.setSelected(settings.isDecimalDbTeX3DatumValue());
      saveDatumCurrencyBox.setSelected(settings.isCurrencyDbTeX3DatumValue());

      Charset charset = settings.getTeXEncodingDefault();

      if (charset == null)
      {
         texEncodingBox.setSelectedItem(Charset.defaultCharset());
      }
      else
      {
         texEncodingBox.setSelectedItem(charset);
      }

      cellHeightModel.setValue(Integer.valueOf(settings.getCellHeight()));

      for (int i = 0; i < cellWidthModels.length; i++)
      {
         cellWidthModels[i].setValue(
           Integer.valueOf(settings.getCellWidth(DatumType.toDatumType(i-1))));
      }

      cellDatumVisibleBox.setSelected(settings.isCellDatumVisible());

      String lookAndFeelClassName = settings.getLookAndFeel();

      if (lookAndFeelClassName != null)
      {
         for (int i = 0; i < availableLookAndFeels.length; i++)
         {
            if (lookAndFeelClassName.equals(
                   availableLookAndFeels[i].getClassName()))
            {
               lookAndFeelBox.setSelectedIndex(i);
               break;
            }
         }
      }

      String iconSuffix = settings.getLargeIconSuffix();

      if (iconSuffix.equals("-64"))
      {
         toolBarIconSizeButton64.setSelected(true);
      }
      else if (iconSuffix.equals("-32"))
      {
         toolBarIconSizeButton32.setSelected(true);
      }
      else
      {
         toolBarIconSizeButton24.setSelected(true);
      }

      iconSuffix = settings.getSmallIconSuffix();

      if (iconSuffix.equals("-24"))
      {
         smallIconSizeButton24.setSelected(true);
      }
      else if (iconSuffix.equals("-20"))
      {
         smallIconSizeButton20.setSelected(true);
      }
      else
      {
         smallIconSizeButton16.setSelected(true);
      }

      currencyListModel = new CurrencyListModel(currencyList, settings);

      Long seed = settings.getRandomSeed();

      if (seed == null)
      {
         hasSeedBox.setSelected(false);
         seedField.setEnabled(false);
      }
      else
      {
         hasSeedBox.setSelected(true);
         seedField.setEnabled(true);
         seedField.setValue(seed.intValue());
      }

      autoTrimBox.setSelected(settings.isAutoTrimLabelsOnDefault());

      helpsetLangBox.setSelectedItem(settings.getHelpSetLocale());
      dictLangBox.setSelectedItem(settings.getDictionaryLocale());

      Locale numLocale = settings.getNumericLocale();
      numericLocaleBox.setSelectedItem(numLocale);

      if (settings.isNumericParserSet())
      {
         numParsePatternButton.setSelected(true);
         numericParserField.setText(
            getDecimalPattern(settings.getNumericParser()));
      }
      else
      {
         numParseMatchLocaleButton.setSelected(true);
         numericParserField.setText("");
      }

      if (settings.isIntegerFormatterSet())
      {
         intFmtPatternButton.setSelected(true);
         
         NumberFormat fmt = settings.getNumericFormatter(DatumType.INTEGER);

         intFormatterField.setText(getDecimalPattern(fmt));
         intFormatterField.setEnabled(true);
      }
      else
      {
         intFmtMatchLocaleButton.setSelected(true);
         intFormatterField.setText("");
         intFormatterField.setEnabled(false);
      }

      if (settings.isCurrencyFormatterSet())
      {
         currencyFmtPatternButton.setSelected(true);
         NumberFormat fmt = settings.getNumericFormatter(DatumType.CURRENCY);
         currencyFormatterField.setText(getDecimalPattern(fmt));
         currencyFormatterField.setEnabled(true);
      }
      else
      {
         currencyFmtMatchLocaleButton.setSelected(true);
         currencyFormatterField.setText("");
         currencyFormatterField.setEnabled(false);
      }

      if (settings.useSIforDecimals())
      {
         decimalFmtSIButton.setSelected(true);
         decimalFormatterField.setText("");
         decimalFormatterField.setEnabled(false);
      }
      else if (settings.isDecimalFormatterSet())
      {
         decimalFmtPatternButton.setSelected(true);
         NumberFormat fmt = settings.getNumericFormatter(DatumType.DECIMAL);
         decimalFormatterField.setText(getDecimalPattern(fmt));
         decimalFormatterField.setEnabled(true);
      }
      else
      {
         decimalFmtMatchLocaleButton.setSelected(true);
         decimalFormatterField.setText("");
         decimalFormatterField.setEnabled(false);
      }

      perlFileField.setFileName(settings.getPerl());

      updateButtons();
      
      editorPropComp.resetFrom(settings);

      setVisible(true);
   }

   private void decimalSymbolsUpdated(Locale locale)
   {
      if (locale == null)
      {
         decimalSymbolsField.setText("");
      }
      else
      {
         decimalSymbolsUpdated(DecimalFormatSymbols.getInstance(locale));
      }
   }

   private void decimalSymbolsUpdated(DecimalFormatSymbols syms)
   {
      char grpSep = syms.getGroupingSeparator();
      char decSep = syms.getDecimalSeparator();

      decimalSymbolsField.setText(String.format("<html>%s</html>", 
        getMessageHandler().getLabelWithValues(
       "preferences.language.decimal_symbols",
        String.format("<b><code>%c</code></b>", grpSep),
        String.format("%04X", (int)grpSep),
        String.format("<b><code>%c</code></b>", decSep), 
        String.format("%04X", (int)decSep)))
       );
   }

   @Override
   public void valueChanged(ListSelectionEvent evt)
   {
      updateButtons();
   }

   @Override
   public void mouseClicked(MouseEvent evt)
   {
      Object source = evt.getSource();

      if (evt.getClickCount() == 2 && evt.getModifiersEx() == 0)
      {
         if (source == currencyList)
         {
            currencyListModel.editCurrency(currencyList.getSelectedIndex());
         }
      }
   }

   @Override
   public void mouseEntered(MouseEvent evt)
   {
   }

   @Override
   public void mouseExited(MouseEvent evt)
   {
   }

   @Override
   public void mousePressed(MouseEvent evt)
   {
   }

   @Override
   public void mouseReleased(MouseEvent evt)
   {
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("okay"))
      {
         try
         {
            okay();
         }
         catch (IllegalArgumentException e)
         {
            getMessageHandler().error(this, e.getMessage());
         }
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
      else if (action.equals("add_currency"))
      {
         currencyListModel.addCurrency();
      }
      else if (action.equals("remove_currency"))
      {
         int index = currencyList.getSelectedIndex();

         if (index > -1)
         {
            currencyListModel.removeCurrency(index);
         }
      }
      else if (action.equals("edit_currency"))
      {
         int index = currencyList.getSelectedIndex();

         if (index > -1)
         {
            currencyListModel.editCurrency(index);
         }
      }
      else if (action.equals("seed"))
      {
         seedField.setEnabled(hasSeedBox.isSelected());

         if (seedField.isEnabled())
         {
            seedField.requestFocusInWindow();
         }
      }
      else if (action.equals("int.pattern"))
      {
         if (intFormatterField.getText().isEmpty())
         {
            Locale locale = (Locale)numericLocaleBox.getSelectedItem();

            if (locale != null)
            {
               NumberFormat numFmt = NumberFormat.getIntegerInstance(locale);

               if (numFmt instanceof DecimalFormat)
               {
                  intFormatterField.setText(((DecimalFormat)numFmt).toPattern());
               }
            }
         }

         intFormatterField.setEnabled(true);
         intFormatterField.requestFocusInWindow();
      }
      else if (action.equals("int.match_locale"))
      {
         intFormatterField.setEnabled(false);
      }
      else if (action.equals("currency.pattern"))
      {
         if (currencyFormatterField.getText().isEmpty())
         {
            Locale locale = (Locale)numericLocaleBox.getSelectedItem();

            if (locale != null)
            {
               NumberFormat numFmt = NumberFormat.getCurrencyInstance(locale);

               if (numFmt instanceof DecimalFormat)
               {
                  currencyFormatterField.setText(((DecimalFormat)numFmt).toPattern());
               }
            }
         }

         currencyFormatterField.setEnabled(true);
         currencyFormatterField.requestFocusInWindow();
      }
      else if (action.equals("currency.match_locale"))
      {
         currencyFormatterField.setEnabled(false);
      }
      else if (action.equals("decimal.pattern"))
      {
         if (decimalFormatterField.getText().isEmpty())
         {
            Locale locale = (Locale)numericLocaleBox.getSelectedItem();

            if (locale != null)
            {
               NumberFormat numFmt = NumberFormat.getInstance(locale);

               if (numFmt instanceof DecimalFormat)
               {
                  decimalFormatterField.setText(((DecimalFormat)numFmt).toPattern());
               }
            }
         }

         decimalFormatterField.setEnabled(true);
         decimalFormatterField.requestFocusInWindow();
      }
      else if (action.equals("decimal.match_locale"))
      {
         decimalFormatterField.setEnabled(false);
      }
      else if (action.equals("decimal.siunitx"))
      {
         decimalFormatterField.setEnabled(false);
      }
      else if (action.equals("numeric_parsing.match_locale"))
      {
         numericParserField.setEnabled(false);
      }
      else if (action.equals("numeric_parsing.pattern"))
      {
         if (numericParserField.getText().isEmpty())
         {
            Locale locale = (Locale)numericLocaleBox.getSelectedItem();

            if (locale != null)
            {
               NumberFormat numFmt = NumberFormat.getInstance(locale);

               if (numFmt instanceof DecimalFormat)
               {
                  numericParserField.setText(((DecimalFormat)numFmt).toPattern());
               }
            }
         }

         numericParserField.setEnabled(true);
         numericParserField.requestFocusInWindow();
      }
      else if (action.equals("save_datum.none") || action.equals("save_datum.all"))
      {
         datumTypeComp.setVisible(false);
      }
      else if (action.equals("save_datum.header_type")
            || action.equals("save_datum.entry_type"))
      {
         datumTypeComp.setVisible(true);
      }
      else
      {
         getMessageHandler().debug("Unknown action '"+action+"'");
      }
   }

   private void updateButtons()
   {
      boolean enabled = currencyList.getSelectedIndex() != -1;
      editCurrencyButton.setEnabled(enabled);
      removeCurrencyButton.setEnabled(enabled);
   }

   private void okay() throws IllegalArgumentException
   {
      if (homeButton.isSelected())
      {
         settings.setStartUp(DatatoolProperties.STARTUP_HOME);
      }
      else if (cwdButton.isSelected())
      {
         settings.setStartUp(DatatoolProperties.STARTUP_CWD);
      }
      else if (lastButton.isSelected())
      {
         settings.setStartUp(DatatoolProperties.STARTUP_LAST);
      }
      else if (customButton.isSelected())
      {
         File file = customFileField.getFile();

         if (file == null)
         {
            throw new IllegalArgumentException( 
               getMessageHandler().getLabel("error.missing_custom_file"));
         }

         settings.setCustomStartUp(file);
      }

      settings.setInitialRowCapacityProperty(
        ((Number)initialRowCapacitySpinner.getValue()).intValue());
      settings.setInitialColumnCapacityProperty(
        ((Number)initialColumnCapacitySpinner.getValue()).intValue());

      csvSettingsPanel.applyCsvSettingsTo(settings, true);

      if (sqlSettingsPanel != null)
      {
         sqlSettingsPanel.applyTo(settings, true);
      }

      settings.setDefaultOutputFormatProperty(
         outputFormatBox.getSelectedItem().toString());
      settings.setOverrideInputFormatProperty(
         overrideInputFormatBox.isSelected());

      if (saveDatumAllBox.isSelected())
      {
         settings.setDbTeX3DatumValueProperty(DbTeX3DatumValue.ALL);
      }
      else if (saveDatumNoneBox.isSelected())
      {
         settings.setDbTeX3DatumValueProperty(DbTeX3DatumValue.NONE);
      }
      else if (saveDatumHeaderBox.isSelected())
      {
         settings.setDbTeX3DatumValueProperty(DbTeX3DatumValue.HEADER);
      }
      else if (saveDatumEntryBox.isSelected())
      {
         settings.setDbTeX3DatumValueProperty(DbTeX3DatumValue.CELL);
      }

      settings.setStringDbTeX3DatumValueProperty(saveDatumStringBox.isSelected());
      settings.setIntegerDbTeX3DatumValueProperty(saveDatumIntegerBox.isSelected());
      settings.setDecimalDbTeX3DatumValueProperty(saveDatumDecimalBox.isSelected());
      settings.setCurrencyDbTeX3DatumValueProperty(saveDatumCurrencyBox.isSelected());

      settings.setSolutionEnvStrippedProperty(stripSolnEnvBox.isSelected());
      settings.setPreambleOnlyProperty(preambleOnlyBox.isSelected());

      settings.setTeXEncodingProperty(
        (Charset)texEncodingBox.getSelectedItem());

      currencyListModel.updateSettings();

      editorPropComp.applySelected(settings);

      settings.setCellHeight(cellHeightModel.getNumber().intValue());

      for (int i = 0; i < cellWidthModels.length; i++)
      {
         settings.setCellWidth(cellWidthModels[i].getNumber().intValue(), 
          DatumType.toDatumType(i-1));
      }

      settings.setCellDatumVisible(cellDatumVisibleBox.isSelected());

      int lookAndFeelIdx = lookAndFeelBox.getSelectedIndex();

      if (lookAndFeelIdx > -1)
      {
         settings.setLookAndFeel(
            availableLookAndFeels[lookAndFeelIdx].getClassName()); 
      }

      if (toolBarIconSizeButton64.isSelected())
      {
         settings.setLargeIconSuffix("-64");
      }
      else if (toolBarIconSizeButton32.isSelected())
      {
         settings.setLargeIconSuffix("-32");
      }
      else
      {
         settings.setLargeIconSuffix("-24");
      }

      if (smallIconSizeButton24.isSelected())
      {
         settings.setSmallIconSuffix("-24");
      }
      else if (smallIconSizeButton20.isSelected())
      {
         settings.setSmallIconSuffix("-20");
      }
      else
      {
         settings.setSmallIconSuffix("-16");
      }

      gui.updateTableSettings();

      if (hasSeedBox.isSelected())
      {
         settings.setRandomSeedProperty(Long.valueOf(seedField.getValue()));
      }
      else
      {
         settings.setRandomSeedProperty(null);
      }

      settings.setAutoTrimLabelsProperty(autoTrimBox.isSelected());

      settings.setHelpSet((HelpSetLocale)helpsetLangBox.getSelectedItem());
      settings.setDictionary((HelpSetLocale)dictLangBox.getSelectedItem());

      settings.setNumericLocaleProperty((Locale)numericLocaleBox.getSelectedItem());

      if (numParseMatchLocaleButton.isSelected())
      {
         settings.setNumericParserProperty(null);
      }
      else
      {
         settings.setNumericParserProperty(
           getDecimalFormat(numericParserField.getText(),
             numericParsingLabel.getText()));
      }

      if (intFmtMatchLocaleButton.isSelected())
      {
         settings.setIntegerFormatterProperty(null);
      }
      else
      {
         settings.setIntegerFormatterProperty(
           getDecimalFormat(intFormatterField.getText(),
           intFmtLabel.getText()));
      }

      if (currencyFmtMatchLocaleButton.isSelected())
      {
         settings.setCurrencyFormatterProperty(null);
      }
      else
      {
         settings.setCurrencyFormatterProperty(
           getDecimalFormat(currencyFormatterField.getText(),
           currencyFmtLabel.getText()));
      }

      if (decimalFmtSIButton.isSelected())
      {
         settings.setSIforDecimalsProperty(true);
         settings.setDecimalFormatterProperty(null);
      }
      else if (decimalFmtMatchLocaleButton.isSelected())
      {
         settings.setSIforDecimalsProperty(false);
         settings.setDecimalFormatterProperty(null);
      }
      else
      {
         settings.setSIforDecimalsProperty(false);
         settings.setDecimalFormatterProperty(
            getDecimalFormat(decimalFormatterField.getText(),
              decimalFmtLabel.getText()));
      }

      settings.setPerl(perlFileField.getFileName());

      settings.getImportSettings().setFrom(settings);

      setVisible(false);
   }

   private String getDecimalPattern(NumberFormat fmt)
   {
      if (fmt instanceof DecimalFormat && fmt != null)
      {
         return ((DecimalFormat)fmt).toPattern();
      }
      else
      {
         return "";
      }
   }

   private DecimalFormat getDecimalFormat(String text, String field)
   throws IllegalArgumentException
   {
      if (text.isEmpty())
      {
         throw new IllegalArgumentException(
           getMessageHandler().getLabelWithValues(
             "error.missing.numeric_pattern", field));
      }

      try
      {
         return new DecimalFormat(text);
      }
      catch (IllegalArgumentException e)
      {
         throw new IllegalArgumentException(
           getMessageHandler().getLabelWithValues(
             "error.invalid.numeric_pattern", field, text), e);
      }
   }

   public CombinedMessageHandler getMessageHandler()
   {
      return (CombinedMessageHandler)settings.getMessageHandler();
   }

   public DatatoolGuiResources getResources()
   {
      return settings.getDatatoolGuiResources();
   }

   public TeXJavaHelpLib getHelpLib()
   {
      return settings.getHelpLib();
   }

   private DatatoolProperties settings;

   private JRadioButton homeButton, cwdButton, lastButton, customButton;

   private IOSettingsPanel csvSettingsPanel;

   private JCheckBox hasSeedBox, stripSolnEnvBox, autoTrimBox, preambleOnlyBox;

   private FileField customFileField, perlFileField;

   private JFileChooser fileChooser;

   private NonNegativeIntField seedField;

   private EditorPropertiesComponent editorPropComp;

   private SpinnerNumberModel cellHeightModel;

   private SpinnerNumberModel[] cellWidthModels;

   private JCheckBox cellDatumVisibleBox;

   private SqlPanel sqlSettingsPanel;

   private JButton removeCurrencyButton, editCurrencyButton;

   private JList<String> currencyList;

   private JComboBox<Charset> texEncodingBox;

   private JComboBox<String> outputFormatBox;
   private JCheckBox overrideInputFormatBox,
     saveDatumStringBox, saveDatumIntegerBox, saveDatumDecimalBox,
     saveDatumCurrencyBox;
   private JRadioButton saveDatumAllBox, saveDatumNoneBox,
     saveDatumHeaderBox, saveDatumEntryBox;
   private JComponent datumTypeComp;

   private CurrencyListModel currencyListModel;

   private JComboBox<HelpSetLocale> helpsetLangBox, dictLangBox;
   private JComboBox<Locale> numericLocaleBox;

   private JLabel numericParsingLabel, intFmtLabel,
    currencyFmtLabel, decimalFmtLabel;

   private JTextField numericParserField, intFormatterField,
    currencyFormatterField, decimalFormatterField;
   private JEditorPane decimalSymbolsField;

   private JRadioButton numParseMatchLocaleButton, numParsePatternButton,
    intFmtMatchLocaleButton, intFmtPatternButton,
    currencyFmtMatchLocaleButton, currencyFmtPatternButton,
    decimalFmtMatchLocaleButton, decimalFmtPatternButton,
    decimalFmtSIButton;

   private JTabbedPane tabbedPane;

   private DatatoolGUI gui;

   private JSpinner initialRowCapacitySpinner;
   private JSpinner initialColumnCapacitySpinner;

   private JComboBox<String> lookAndFeelBox;
   private JRadioButton toolBarIconSizeButton24,
     toolBarIconSizeButton32, toolBarIconSizeButton64,
     smallIconSizeButton16, smallIconSizeButton20,
     smallIconSizeButton24;

   private UIManager.LookAndFeelInfo[] availableLookAndFeels;

   // Integer.MAX_VALUE is excessive and will make the
   // spinner box unnecessarily wide. If the user needs a larger
   // value it's easier to type it than use the widgets.
   private static final int MAX_INT_SPINNER = 100;
}

class CurrencyListModel extends AbstractListModel<String>
{
   public CurrencyListModel(JList<String> list, DatatoolProperties settings)
   {
      this.settings = settings;
      this.list = list;

      if (LABEL_ADD == null)
      {
         MessageHandler messageHandler = settings.getMessageHandler();

         LABEL_ADD 
            = messageHandler.getLabel("preferences.currencies.add_currency");
         LABEL_EDIT 
            = messageHandler.getLabel("preferences.currencies.edit_currency");
      }

      list.setModel(this);

      int n = settings.getCurrencyCount();
      currencies = new Vector<String>();

      for (int i = 0; i < n; i++)
      {
         currencies.add(settings.getCurrency(i));
      }
   }

   @Override
   public int getSize()
   {
      return currencies.size();
   }

   @Override
   public String getElementAt(int index)
   {
      return currencies.get(index);
   }

   public void updateSettings()
   {
      settings.clearCurrencies();

      for (int i = 0, n = currencies.size(); i < n; i++)
      {
         settings.addCurrency(currencies.get(i));
      }
   }

   public void addCurrency()
   {
      String response = JOptionPane.showInputDialog(list, LABEL_ADD);

      if (response != null)
      {
         int n = currencies.size();
         currencies.add(response);

         fireIntervalAdded(this, n, n);
      }
   }

   public void removeCurrency(int index)
   {
      currencies.remove(index);

      fireIntervalRemoved(this, index, index);
   }

   public void editCurrency(int index)
   {
      String response = JOptionPane.showInputDialog(list, LABEL_EDIT,
         currencies.get(index));

      if (response != null)
      {
         currencies.set(index, response);
         fireContentsChanged(this, index, index);
      }
   }

   private DatatoolProperties settings;

   private Vector<String> currencies;

   private JList<String> list;

   private static String LABEL_ADD=null;

   private static String LABEL_EDIT=null;
}

