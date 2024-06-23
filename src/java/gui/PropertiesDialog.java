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
import javax.swing.event.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.texjavahelplib.JLabelGroup;

import com.dickimawbooks.datatooltk.*;

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

      JComponent generalTab = createGeneralTab();

      // CSV tab

      JComponent csvTab = createCsvTab();

      // SQL tab

      JComponent sqlTab = createSqlTab();

      // TeX Tab

      JComponent texTab = createTeXTab();

      // Currencies Tab

      JComponent currencyTab = createCurrenciesTab();

      // Display Tab

      JComponent displayTab = createDisplayTab();

      // Language Tab

      JComponent languageTab = createLanguageTab();

      // Plugins tab

      JComponent pluginsTab = createPluginsTab();

      getContentPane().add(
        resources.createOkayCancelHelpPanel(this, gui, "preferences"),
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

      customFileField = new FileField(messageHandler, startupComp, fileChooser,
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
      MessageHandler messageHandler = gui.getMessageHandler();

      JComponent csvTab = addTab("csv");

      JComponent box = Box.createHorizontalBox();
      box.setAlignmentX(0);
      csvTab.add(box);

      box.add(new JLabel(messageHandler.getLabel("preferences.csv.sep")));

      ButtonGroup bg = new ButtonGroup();

      sepTabButton = createRadioButton("preferences.csv", "tabsep", bg);
      box.add(sepTabButton);

      box.add(Box.createHorizontalStrut(10));
      box.add(new JLabel(messageHandler.getLabel("preferences.csv.or")));
      box.add(Box.createHorizontalStrut(10));

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

      box.add(Box.createHorizontalStrut(10));

      strictQuotesBox = createCheckBox("preferences.csv", "strictquotes");
      box.add(strictQuotesBox);

      box = Box.createHorizontalBox();
      box.setAlignmentX(0);
      csvTab.add(box);

      hasHeaderBox = createCheckBox("preferences.csv", "hasheader");
      box.add(hasHeaderBox);

      box.add(Box.createHorizontalStrut(10));

      skipEmptyRowsBox = resources.createJCheckBox("preferences.csv",
        "skipemptyrows", null, 0);
      box.add(skipEmptyRowsBox);

      box = Box.createHorizontalBox();
      box.setAlignmentX(0);
      csvTab.add(box);

      skipLinesBox = new JSpinner(new SpinnerNumberModel(0, 0, MAX_INT_SPINNER, 1));
      box.add(resources.createJLabel("preferences.csv.skiplines", skipLinesBox));
      box.add(skipLinesBox);

      box = Box.createHorizontalBox();
      box.setAlignmentX(0);
      csvTab.add(box);

      box.add(new JLabel(messageHandler.getLabel("preferences.csv.esc")));

      bg = new ButtonGroup();

      noEscCharButton = createRadioButton("preferences.csv", "noesc", bg);
      box.add(noEscCharButton);

      box.add(Box.createHorizontalStrut(10));
      box.add(new JLabel(messageHandler.getLabel("preferences.csv.or")));
      box.add(Box.createHorizontalStrut(10));

      escCharButton = createRadioButton("preferences.csv", "escchar", bg);
      box.add(escCharButton);

      escCharField = new CharField('\\');
      box.add(escCharField);

      box = Box.createHorizontalBox();
      box.setAlignmentX(0);
      csvTab.add(box);

      csvEncodingBox = new JComboBox<Charset>(
        Charset.availableCharsets().values().toArray(new Charset[0]));
      box.add(createLabel("preferences.csv.encoding", csvEncodingBox));
      box.add(csvEncodingBox);

      return csvTab;
   }

   private JComponent createSqlTab()
   {
      DatatoolGuiResources resources = gui.getResources();
      MessageHandler messageHandler = gui.getMessageHandler();

      JComponent sqlTab = addTab("sql");

      JLabelGroup labelGrp = new JLabelGroup();

      JComponent box = createNewRow(sqlTab);

      hostField = new JTextField(16);

      box.add(createLabel(labelGrp, "preferences.sql.host", hostField));
      box.add(hostField);

      box = createNewRow(sqlTab);

      portField = new NonNegativeIntField(3306);

      box.add(createLabel(labelGrp, "preferences.sql.port", portField));

      box.add(portField);

      box = createNewRow(sqlTab);

      prefixField = new JTextField(16);

      box.add(createLabel(labelGrp, "preferences.sql.prefix", prefixField));
      box.add(prefixField);

      box = createNewRow(sqlTab);

      databaseField = new JTextField(16);

      box.add(createLabel(labelGrp, "preferences.sql.database", databaseField));
      box.add(databaseField);

      box = createNewRow(sqlTab);

      userField = new JTextField(16);

      box.add(createLabel(labelGrp, "preferences.sql.user", userField));
      box.add(userField);

      wipeBox = createCheckBox("preferences.sql", "wipe");
      sqlTab.add(wipeBox);

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

      stripSolnEnvBox = createCheckBox("preferences.tex", "stripsolnenv");
      texTab.add(stripSolnEnvBox);

      mapTeXBox = createCheckBox("preferences.tex", "map");
      mapTeXBox.addActionListener(this);
      mapTeXBox.setActionCommand("texmap");
      texTab.add(mapTeXBox);

      texMappingsComp = createNewRow(texTab, new BorderLayout());
      texMapTable = new JTable();
      texMapTable.addMouseListener(this);
      texMapTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      texMapTable.setBorder(BorderFactory.createEtchedBorder());
      texMapTable.setIntercellSpacing(new Dimension(6, 1));

      FontMetrics fm = texMapTable.getFontMetrics(texMapTable.getFont());

      int rowHeight = fm.getHeight()+6;
      texMapTable.setRowHeight(rowHeight);

      JScrollPane sp = new JScrollPane(texMapTable);
      sp.setBorder(BorderFactory.createEmptyBorder());

      sp.setPreferredSize(new Dimension(150, 11*rowHeight));
      texMappingsComp.add(sp, BorderLayout.CENTER);

      JComponent buttonPanel = Box.createVerticalBox();
      texMappingsComp.add(buttonPanel, BorderLayout.EAST);

      buttonPanel.add(resources.createActionButton(
         "preferences.tex", "add_map", this, null));

      editMapButton = resources.createActionButton(
         "preferences.tex", "edit_map", this, null);
      buttonPanel.add(editMapButton);

      removeMapButton = resources.createActionButton(
         "preferences.tex", "remove_map", this, null);
      buttonPanel.add(removeMapButton);

      texMapTable.getSelectionModel().addListSelectionListener(this);

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
         "preferences.currencies", "add_currency", this, null));

      editCurrencyButton = resources.createActionButton(
         "preferences.currencies", "edit_currency", this, null);
      buttonPanel.add(editCurrencyButton);

      removeCurrencyButton = resources.createActionButton(
         "preferences.currencies", "remove_currency", this, null);
      buttonPanel.add(removeCurrencyButton);

      currencyList.addListSelectionListener(this);
      currencyList.addMouseListener(this);

      return currencyTab;
   }

   private JComponent createDisplayTab()
   {
      DatatoolGuiResources resources = gui.getResources();
      MessageHandler messageHandler = gui.getMessageHandler();

      JComponent displayTab =
         addTab(new JPanel(new FlowLayout(FlowLayout.LEFT)), "display");

      displayTab.setAlignmentY(0);
      JComponent leftPanel = Box.createVerticalBox();
      leftPanel.setAlignmentY(0);
      displayTab.add(leftPanel);

      JLabelGroup labelGrp = new JLabelGroup();

      JComponent box = createNewRow(leftPanel);

      GraphicsEnvironment env =
         GraphicsEnvironment.getLocalGraphicsEnvironment();

      fontBox = new JComboBox<String>(env.getAvailableFontFamilyNames());

      box.add(createLabel(labelGrp, "preferences.display.font", fontBox));
      box.add(fontBox);

      box = createNewRow(leftPanel);
      sizeField = new NonNegativeIntField(10);

      box.add(createLabel(labelGrp, "preferences.display.fontsize", sizeField));
      box.add(sizeField);

      box = createNewRow(leftPanel);
      cellHeightField = new NonNegativeIntField(4);

      box.add(createLabel(labelGrp,
         "preferences.display.cellheight", cellHeightField));
      box.add(cellHeightField);

      box = createNewRow(leftPanel);
      box.add(Box.createVerticalStrut(20));

      JComponent editorBox = createNewRow(leftPanel);
      editorBox.setLayout(new BoxLayout(editorBox, BoxLayout.Y_AXIS));
      editorBox.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), 
        messageHandler.getLabel("preferences.display.editor")));

      box = createNewRow(editorBox);
      box.add(resources.createMessageArea(2, 22,
         "preferences.display.editor.info"));

      box = createNewRow(editorBox);

      labelGrp = new JLabelGroup();

      editorHeightField = new NonNegativeIntField(10);
      box.add(createLabel(labelGrp, "preferences.display.editorheight",
         editorHeightField));
      box.add(editorHeightField);

      box = createNewRow(editorBox);

      editorWidthField = new NonNegativeIntField(8);
      box.add(createLabel(labelGrp, "preferences.display.editorwidth",
         editorWidthField));
      box.add(editorWidthField);

      box = createNewRow(editorBox);

      JButton button = new JButton("...");
      button.setActionCommand("highlightcs");
      button.addActionListener(this);
      box.add(createLabel(labelGrp, "preferences.display.highlightcs", button));

      highlightCsSwatch = new JPanel();
      highlightCsSwatch.setPreferredSize(new Dimension(38,20));
      box.add(highlightCsSwatch);
      box.add(button);

      box = createNewRow(editorBox);

      button = new JButton("...");
      button.setActionCommand("highlightcomment");
      button.addActionListener(this);
      box.add(createLabel(labelGrp, "preferences.display.highlightcomment", button));

      highlightCommentSwatch = new JPanel();
      highlightCommentSwatch.setPreferredSize(new Dimension(38,20));
      box.add(highlightCommentSwatch);
      box.add(button);

      syntaxHighlightingBox = resources.createJCheckBox
        ("preferences.display", "editorsyntax", null);
      editorBox.add(syntaxHighlightingBox);

      JComponent rightPanel = Box.createVerticalBox();
      rightPanel.setAlignmentY(0);
      displayTab.add(rightPanel);

      JComponent lookAndFeelPanel = Box.createVerticalBox();
      lookAndFeelPanel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), 
        messageHandler.getLabel("preferences.display.lookandfeel.title")));
      rightPanel.add(lookAndFeelPanel);

      box = createNewRow(lookAndFeelPanel);

      availableLookAndFeels = UIManager.getInstalledLookAndFeels();

      String[] names = new String[availableLookAndFeels.length];

      for (int i = 0; i < names.length; i++)
      {
         names[i] = availableLookAndFeels[i].getName();
      }

      lookAndFeelBox = new JComboBox<String>(names);

      box.add(createLabel("preferences.display.lookandfeel", lookAndFeelBox));
      box.add(lookAndFeelBox);

      box = createNewRow(lookAndFeelPanel);
      box.add(createTextArea(4, 16, "preferences.display.lookandfeel.restart"));

      JComponent cellWidthsPanel = Box.createVerticalBox();
      cellWidthsPanel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), 
        messageHandler.getLabel("preferences.display.cellwidths")));
      cellWidthsPanel.setAlignmentY(0);
      rightPanel.add(cellWidthsPanel);

      String[] typeLabels = settings.getTypeLabels();
      int[] typeMnemonics = settings.getTypeMnemonics();

      cellWidthFields = new NonNegativeIntField[typeLabels.length];
      labelGrp = new JLabelGroup();

      for (int i = 0; i < cellWidthFields.length; i++)
      {
         box = createNewRow(cellWidthsPanel);
         cellWidthFields[i] = new NonNegativeIntField(0);
         JLabel label = labelGrp.createJLabel(typeLabels[i]);

         if (typeMnemonics[i] != -1)
         {
            label.setDisplayedMnemonic(typeMnemonics[i]);
            label.setLabelFor(cellWidthFields[i]);
         }

         box.add(label);
         box.add(cellWidthFields[i]);
      }

      return displayTab;
   }

   private JComponent createLanguageTab()
   {
      DatatoolGuiResources resources = gui.getResources();
      MessageHandler messageHandler = gui.getMessageHandler();

      JComponent languageTab =
         addTab(new JPanel(new BorderLayout()), "language");

      languageTab.add(createTextArea("preferences.language.restart"),
         BorderLayout.NORTH);

      JComponent comp = Box.createVerticalBox();
      languageTab.add(comp);

      JComponent box = new JPanel(new FlowLayout());
      comp.add(box);

      helpsetLangBox = new JComboBox<String>(gui.getHelpSets());

      JLabel helpsetLangLabel 
         = createLabel("preferences.language.helpset", helpsetLangBox);
      box.add(helpsetLangLabel);
      box.add(helpsetLangBox);

      dictLangBox = new JComboBox<String>(gui.getDictionaries());

      JLabel dictLangLabel 
         = createLabel("preferences.language.dictionary", dictLangBox);
      box.add(dictLangLabel);
      box.add(dictLangBox);

      // Numeric Parsing

      box = new JPanel(new FlowLayout(FlowLayout.LEADING));
      comp.add(box);

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

      decimalSymbolsField = new JEditorPane("text/html", "");
      decimalSymbolsField.setEditable(false);
      decimalSymbolsField.setOpaque(false);
      decimalSymbolsField.setBorder(BorderFactory.createEmptyBorder());
      decimalSymbolsUpdated(DecimalFormatSymbols.getInstance());
      comp.add(decimalSymbolsField);

      box = new JPanel(new FlowLayout(FlowLayout.LEADING));
      comp.add(box);

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

      box = new JPanel(new FlowLayout(FlowLayout.LEADING));
      comp.add(box);

      box.add(createLabel("preferences.language.numeric_formatting"));

      JLabelGroup labelGrp = new JLabelGroup();

      // Integer Formatting

      box = new JPanel(new FlowLayout(FlowLayout.LEADING));
      comp.add(box);

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

      box = new JPanel(new FlowLayout(FlowLayout.LEADING));
      comp.add(box);

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

      box = new JPanel(new FlowLayout(FlowLayout.LEADING));
      comp.add(box);

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

      return languageTab;
   }

   private JComponent createPluginsTab()
   {
      DatatoolGuiResources resources = gui.getResources();
      MessageHandler messageHandler = gui.getMessageHandler();

      JComponent pluginsTab = addTab("plugins");

      JComponent box = createNewRow(pluginsTab);
      box.add(resources.createMessageArea("preferences.plugins.note"));

      box = createNewRow(pluginsTab);
      perlFileField = new FileField(messageHandler, this, "perl", fileChooser);
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
      int index = tabbedPane.getTabCount();

      JPanel panel = new JPanel();
      panel.setOpaque(true);
      panel.setBorder(BorderFactory.createEtchedBorder());
      panel.add(tab);

      tabbedPane.addTab(getMessageHandler().getLabel("preferences", label), 
         panel);

      String tooltip = getMessageHandler().getToolTip("preferences", label);

      if (tooltip != null)
      {
         tabbedPane.setToolTipTextAt(index, tooltip);
      }

      tabbedPane.setMnemonicAt(index,
         getMessageHandler().getMnemonicInt("preferences", label));

      return tab;
   }

   private JComponent createNewRow(JComponent tab)
   {
      return createNewRow(tab, new FlowLayout(FlowLayout.LEFT, 4, 1));
   }

   private JComponent createNewRow(JComponent tab, LayoutManager layout)
   {
      JComponent comp = new JPanel(layout);
      comp.setAlignmentX(0);
      tab.add(comp);

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
      JCheckBox checkBox = getResources().createJCheckBox(parentLabel, label, this);

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

      initialRowCapacitySpinner.setValue(Integer.valueOf(
        settings.getInitialRowCapacity()));

      initialColumnCapacitySpinner.setValue(Integer.valueOf(
        settings.getInitialColumnCapacity()));

      int sep = settings.getSeparator();

      if (sep == '\t')
      {
         sepTabButton.setSelected(true);
         sepCharField.setEnabled(false);
      }
      else
      {
         sepCharButton.setSelected(true);
         sepCharField.setEnabled(true);
         sepCharField.setValue(sep);
      }

      delimCharField.setValue(settings.getDelimiter());

      hasHeaderBox.setSelected(settings.hasCSVHeader());
      strictQuotesBox.setSelected(settings.hasCSVstrictquotes());

      skipLinesBox.setValue(Integer.valueOf(settings.getCSVskiplines()));

      int esc = settings.getCSVescape();

      if (esc == 0)
      {
         noEscCharButton.setSelected(true);
         escCharField.setEnabled(false);
      }
      else
      {
         escCharButton.setSelected(true);
         escCharField.setEnabled(true);
         escCharField.setValue(esc);
      }

      hostField.setText(settings.getSqlHost());
      prefixField.setText(settings.getSqlPrefix());
      portField.setValue(settings.getSqlPort());
      wipeBox.setSelected(settings.isWipePasswordEnabled());

      String user = settings.getSqlUser();

      userField.setText(user == null ? "" : user);

      String db = settings.getSqlDbName();

      databaseField.setText(db == null ? "" : db);

      mapTeXBox.setSelected(settings.isTeXMappingOn());
      texMappingsComp.setVisible(mapTeXBox.isSelected());
      stripSolnEnvBox.setSelected(settings.isSolutionEnvStripped());

      String encoding = settings.getTeXEncoding();

      if (encoding == null)
      {
         texEncodingBox.setSelectedItem(Charset.defaultCharset());
      }
      else
      {
         texEncodingBox.setSelectedItem(Charset.forName(encoding));
      }

      encoding = settings.getCsvEncoding();

      if (encoding == null)
      {
         csvEncodingBox.setSelectedItem(Charset.defaultCharset());
      }
      else
      {
         csvEncodingBox.setSelectedItem(Charset.forName(encoding));
      }

      sizeField.setValue(settings.getFontSize());
      fontBox.setSelectedItem(settings.getFontName());
      cellHeightField.setValue(settings.getCellHeight());

      for (int i = 0; i < cellWidthFields.length; i++)
      {
         cellWidthFields[i].setValue(
           settings.getCellWidth(DatumType.toDatumType(i-1)));
      }

      editorHeightField.setValue(settings.getCellEditorHeight());
      editorWidthField.setValue(settings.getCellEditorWidth());
      syntaxHighlightingBox.setSelected(settings.isSyntaxHighlightingOn());
      highlightCsSwatch.setBackground(settings.getControlSequenceHighlight());
      highlightCommentSwatch.setBackground(settings.getCommentHighlight());

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

      texMapModel = new TeXMapModel(this, texMapTable, settings);
      texMapTable.setModel(texMapModel);

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

      autoTrimBox.setSelected(settings.isAutoTrimLabelsOn());
      skipEmptyRowsBox.setSelected(settings.isSkipEmptyRowsOn());

      helpsetLangBox.setSelectedItem(settings.getHelpSet());
      dictLangBox.setSelectedItem(settings.getDictionary());

      Locale numLocale = settings.getNumericLocale();
      numericLocaleBox.setSelectedItem(numLocale);

      if (settings.isNumericParserSet())
      {
         numParsePatternButton.setSelected(true);
         numericParserField.setText(getDecimalPattern(settings.getNumericParser()));
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
      decimalSymbolsField.setText(String.format("<html>%s</html>", 
        getMessageHandler().getLabelWithValues(
       "preferences.language.decimal_symbols",
        String.format("<b><code>%c</code></b>", syms.getDecimalSeparator()), 
        String.format("<b><code>%c</code></b>", syms.getGroupingSeparator()))));
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
         else if (source == texMapTable)
         {
            texMapModel.editRow(texMapTable.getSelectedRow());
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
      else if (action.equals("tabsep"))
      {
         sepCharField.setEnabled(false);
      }
      else if (action.equals("sepchar"))
      {
         sepCharField.setEnabled(true);
         sepCharField.requestFocusInWindow();
      }
      else if (action.equals("noesc"))
      {
         escCharField.setEnabled(false);
         escCharField.requestFocusInWindow();
      }
      else if (action.equals("escchar"))
      {
         escCharField.setEnabled(true);
         escCharField.requestFocusInWindow();
      }
      else if (action.equals("texmap"))
      {
         texMappingsComp.setVisible(mapTeXBox.isSelected());
      }
      else if (action.equals("add_map"))
      {
         texMapModel.addRow();
      }
      else if (action.equals("edit_map"))
      {
         int index = texMapTable.getSelectedRow();

         if (index > -1)
         {
            texMapModel.editRow(index);
         }
      }
      else if (action.equals("remove_map"))
      {
         int index = texMapTable.getSelectedRow();

         if (index > -1)
         {
            texMapModel.removeRow(index);
         }
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
      else if (action.equals("highlightcs"))
      {
         Color col = JColorChooser.showDialog(this, 
            getMessageHandler().getLabel("preferences.display.highlightcs"),
            highlightCsSwatch.getBackground());

         if (col != null)
         {
            highlightCsSwatch.setBackground(col);
         }
      }
      else if (action.equals("highlightcomment"))
      {
         Color col = JColorChooser.showDialog(this, 
            getMessageHandler().getLabel("preferences.display.highlightcomment"),
            highlightCommentSwatch.getBackground());

         if (col != null)
         {
            highlightCommentSwatch.setBackground(col);
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
      else
      {
         System.err.println("Unknown action '"+action+"'");
      }
   }

   private void updateButtons()
   {
      boolean enabled = texMapTable.getSelectedRow() != -1;
      editMapButton.setEnabled(enabled);
      removeMapButton.setEnabled(enabled);

      enabled = currencyList.getSelectedIndex() != -1;
      editCurrencyButton.setEnabled(enabled);
      removeCurrencyButton.setEnabled(enabled);
   }

   private void okay() throws IllegalArgumentException
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
            throw new IllegalArgumentException( 
               getMessageHandler().getLabel("error.missing_custom_file"));
         }

         settings.setCustomStartUp(file);
      }

      settings.setInitialRowCapacity(
        ((Number)initialRowCapacitySpinner.getValue()).intValue());
      settings.setInitialColumnCapacity(
        ((Number)initialColumnCapacitySpinner.getValue()).intValue());

      if (sepTabButton.isSelected())
      {
         settings.setSeparator('\t');
      }
      else
      {
         int sep = sepCharField.getValue();

         if (sep == 0)
         {
            throw new IllegalArgumentException( 
               getMessageHandler().getLabel("error.missing_sep"));
         }
         else if (sep >= 0xFFFF)
         {
            throw new IllegalArgumentException( 
               getMessageHandler().getLabelWithValues("error.char_sep_required", 
               MessageHandler.codePointToString(sep), 
               "0xFFFF"));
         }

         settings.setSeparator(sep);
      }

      int delim = delimCharField.getValue();

      if (delim == 0)
      {
         throw new IllegalArgumentException( 
            getMessageHandler().getLabel("error.missing_delim"));
      }
      else if (delim >= 0xFFFF)
      {
         throw new IllegalArgumentException( 
            getMessageHandler().getLabelWithValues("error.char_delim_required", 
            MessageHandler.codePointToString(delim), 
            "0xFFFF"));
      }

      settings.setDelimiter(delim);

      settings.setHasCSVHeader(hasHeaderBox.isSelected());
      settings.setCSVstrictquotes(strictQuotesBox.isSelected());

      settings.setCSVskiplines((Integer)skipLinesBox.getValue());

      if (noEscCharButton.isSelected())
      {
         settings.setCSVescape("");
      }
      else
      {
         int escChar = escCharField.getValue();

         if (escChar >= 0xFFFF)
         {
            throw new IllegalArgumentException( 
               getMessageHandler().getLabelWithValues("error.char_esc_required", 
               MessageHandler.codePointToString(escChar), 
               "0xFFFF"));
         }

         settings.setCSVescape(escChar);
      }

      String host = hostField.getText();

      if (host.isEmpty())
      {
         throw new IllegalArgumentException( 
            getMessageHandler().getLabel("error.missing_host"));
      }

      settings.setSqlHost(host);

      String prefix = prefixField.getText();

      if (prefix.isEmpty())
      {
         throw new IllegalArgumentException(
            getMessageHandler().getLabel("error.missing_prefix"));
      }

      settings.setSqlPrefix(prefix);

      if (portField.getText().isEmpty())
      {
         throw new IllegalArgumentException(
            getMessageHandler().getLabel("error.missing_port"));
      }

      settings.setSqlPort(portField.getValue());

      settings.setSqlUser(userField.getText());
      settings.setSqlDbName(databaseField.getText());

      settings.setTeXMapping(mapTeXBox.isSelected());
      settings.setSolutionEnvStripped(stripSolnEnvBox.isSelected());

      settings.setTeXEncoding((Charset)texEncodingBox.getSelectedItem());
      settings.setCsvEncoding((Charset)csvEncodingBox.getSelectedItem());

      texMapModel.updateSettings();

      currencyListModel.updateSettings();

      settings.setFontName(fontBox.getSelectedItem().toString());
      settings.setFontSize(sizeField.getValue());
      settings.setCellHeight(cellHeightField.getValue());

      for (int i = 0; i < cellWidthFields.length; i++)
      {
         settings.setCellWidth(cellWidthFields[i].getValue(), 
          DatumType.toDatumType(i-1));
      }

      settings.setCellEditorHeight(editorHeightField.getValue());
      settings.setCellEditorWidth(editorWidthField.getValue());
      settings.setSyntaxHighlighting(syntaxHighlightingBox.isSelected());

      settings.setControlSequenceHighlight(highlightCsSwatch.getBackground());
      settings.setCommentHighlight(highlightCommentSwatch.getBackground());

      int lookAndFeelIdx = lookAndFeelBox.getSelectedIndex();

      if (lookAndFeelIdx > -1)
      {
         settings.setLookAndFeel(
            availableLookAndFeels[lookAndFeelIdx].getClassName()); 
      }

      gui.updateTableSettings();

      if (hasSeedBox.isSelected())
      {
         settings.setRandomSeed(new Long(seedField.getValue()));
      }
      else
      {
         settings.setRandomSeed(null);
      }

      settings.setAutoTrimLabels(autoTrimBox.isSelected());
      settings.setSkipEmptyRows(skipEmptyRowsBox.isSelected());

      settings.setHelpSet(helpsetLangBox.getSelectedItem().toString());
      settings.setDictionary(dictLangBox.getSelectedItem().toString());

      settings.setNumericLocale((Locale)numericLocaleBox.getSelectedItem());

      if (numParseMatchLocaleButton.isSelected())
      {
         settings.setNumericParser(null);
      }
      else
      {
         settings.setNumericParser(getDecimalFormat(numericParserField.getText(),
           numericParsingLabel.getText()));
      }

      if (intFmtMatchLocaleButton.isSelected())
      {
         settings.setIntegerFormatter(null);
      }
      else
      {
         settings.setIntegerFormatter(
           getDecimalFormat(intFormatterField.getText(),
           intFmtLabel.getText()));
      }

      if (currencyFmtMatchLocaleButton.isSelected())
      {
         settings.setCurrencyFormatter(null);
      }
      else
      {
         settings.setCurrencyFormatter(
           getDecimalFormat(currencyFormatterField.getText(),
           currencyFmtLabel.getText()));
      }

      if (decimalFmtSIButton.isSelected())
      {
         settings.setSIforDecimals(true);
         settings.setDecimalFormatter(null);
      }
      else if (decimalFmtMatchLocaleButton.isSelected())
      {
         settings.setSIforDecimals(false);
         settings.setDecimalFormatter(null);
      }
      else
      {
         settings.setSIforDecimals(false);
         settings.setDecimalFormatter(
            getDecimalFormat(decimalFormatterField.getText(),
              decimalFmtLabel.getText()));
      }

      settings.setPerl(perlFileField.getFileName());

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

   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
   }

   public DatatoolGuiResources getResources()
   {
      return settings.getMessageHandler().getDatatoolGuiResources();
   }

   private DatatoolSettings settings;

   private JRadioButton homeButton, cwdButton, lastButton, customButton;

   private JRadioButton sepTabButton, sepCharButton, 
     noEscCharButton, escCharButton;

   private CharField sepCharField, delimCharField, escCharField;

   private JCheckBox hasHeaderBox, strictQuotesBox, wipeBox, mapTeXBox,
      hasSeedBox, syntaxHighlightingBox, stripSolnEnvBox, autoTrimBox,
      skipEmptyRowsBox;

   private JSpinner skipLinesBox;

   private JComponent texMappingsComp;

   private FileField customFileField, perlFileField;

   private JFileChooser fileChooser;

   private NonNegativeIntField portField, sizeField, cellHeightField,
      seedField, editorHeightField, editorWidthField;

   private NonNegativeIntField[] cellWidthFields;

   private JTextField hostField, prefixField, databaseField, userField;

   private JButton removeMapButton, editMapButton,
      removeCurrencyButton, editCurrencyButton;

   private JComboBox<String> fontBox;

   private TeXMapModel texMapModel;

   private JTable texMapTable;

   private JList<String> currencyList;

   private JComboBox<Charset> texEncodingBox, csvEncodingBox;

   private CurrencyListModel currencyListModel;

   private JComboBox<String> helpsetLangBox, dictLangBox;
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

   private JComponent highlightCsSwatch, highlightCommentSwatch;

   private JTabbedPane tabbedPane;

   private DatatoolGUI gui;

   private JSpinner initialRowCapacitySpinner;
   private JSpinner initialColumnCapacitySpinner;

   private JComboBox<String> lookAndFeelBox;

   private UIManager.LookAndFeelInfo[] availableLookAndFeels;

   // Integer.MAX_VALUE is excessive and will make the
   // spinner box unnecessarily wide. If the user needs a larger
   // value it's easier to type it than use the widgets.
   private static final int MAX_INT_SPINNER = 100;
}

class CurrencyListModel extends AbstractListModel<String>
{
   public CurrencyListModel(JList<String> list, DatatoolSettings settings)
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
      currencies = new Vector<String>(n);

      for (int i = 0; i < n; i++)
      {
         currencies.add(settings.getCurrency(i));
      }
   }

   public int getSize()
   {
      return currencies.size();
   }

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
         currencies.add(response);

         list.revalidate();
      }
   }

   public void removeCurrency(int index)
   {
      currencies.remove(index);

      list.revalidate();
   }

   public void editCurrency(int index)
   {
      String response = JOptionPane.showInputDialog(list, LABEL_EDIT,
         currencies.get(index));

      if (response != null)
      {
         currencies.set(index, response);
      }

      list.revalidate();
   }

   private DatatoolSettings settings;

   private Vector<String> currencies;

   private JList<String> list;

   private static String LABEL_ADD=null;

   private static String LABEL_EDIT=null;
}
