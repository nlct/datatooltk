/*
    Copyright (C) 2024 Nicola L.C. Talbot
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

import java.nio.charset.Charset;
import java.util.Vector;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.Border;

import com.dickimawbooks.texparserlib.TeXSyntaxException;
import com.dickimawbooks.texparserlib.latex.datatool.*;

import com.dickimawbooks.texjavahelplib.TeXJavaHelpLib;
import com.dickimawbooks.texjavahelplib.JLabelGroup;

import com.dickimawbooks.datatooltk.*;

import com.dickimawbooks.datatooltk.io.DatatoolFileFormat;
import com.dickimawbooks.datatooltk.io.UnsupportedFileFormatException;

public class IOSettingsPanel extends JPanel
  implements ActionListener
{
   public IOSettingsPanel(Window owner, DatatoolGuiResources resources,
      int formatModifiers, boolean formatSingleSelection)
   {
      this(owner, resources, null, IO_IN | IO_OUT,
       formatModifiers, formatSingleSelection, true, true);
   }

   public IOSettingsPanel(Window owner, DatatoolGuiResources resources,
      String tagParentLabel, int ioModifiers,
      int formatModifiers, boolean formatSingleSelection,
      boolean addTrim, boolean addEmptyToNull)
   {
      super(null);

      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      setAlignmentX(defaultAlignmentX);

      this.owner = owner;
      this.resources = resources;
      this.ioModifiers = ioModifiers;
      this.formatModifiers = formatModifiers;
      this.formatSingleSelection = formatSingleSelection;
      this.tagParentLabel = tagParentLabel;

      this.selectedFormatModifiers = 0;

      init(addTrim, addEmptyToNull);
   }

   protected void init(boolean addTrim, boolean addEmptyToNull)
   {
      initFormatButtons();

      if (addTrim || addEmptyToNull)
      {
         JComponent row = createRow();
         add(row);

         if (addTrim)
         {
            trimElementBox = createJCheckBox("element", "trim");

            row.add(trimElementBox);
         }

         if (addEmptyToNull)
         {
            emptyToNullBox = createJCheckBox("element", "empty_to_null");

            row.add(emptyToNullBox);
         }
      }

      if ( DatatoolFileFormat.isAnyTeX(formatModifiers) )
      {
         initTeXCardComp();
      }

      if ( DatatoolFileFormat.isAnyNonTeX(formatModifiers) )
      {
         initNonTeXComp();
      }

      if (texCardComp != null && nonTeXCardComp != null)
      {
         cardLayout = new CardLayout();
         cardComp = new JPanel(cardLayout);
         cardComp.setAlignmentX(defaultAlignmentX);

         add(cardComp);

         cardComp.add(texCardComp, "tex");
         cardComp.add(nonTeXCardComp, "nontex");
      }
      else if (texCardComp != null)
      {
         add(texCardComp);
      }
      else if (nonTeXCardComp != null)
      {
         add(nonTeXCardComp);
      }
      else
      {
         getMessageHandler().debug("No main components!");
      }

      formatChanged();
   }

   protected void initFormatButtons()
   {
      ButtonGroup formatBtnGrp = null;

      if (formatSingleSelection)
      {
         formatBtnGrp = new ButtonGroup();
      }

      fileFormatComp = createRow();
      add(fileFormatComp);

      int numFormats = 0;
      JToggleButton lastButton = null;

      fileFormatComp.add(createJLabel("format"));

      if (DatatoolFileFormat.isTeX(formatModifiers))
      {
         formatTeXToggleButton = createFormatButton(
           fileFormatComp, "tex", formatBtnGrp);
         lastButton = formatTeXToggleButton;
         numFormats++;
      }

      if (DatatoolFileFormat.isDTLTEX(formatModifiers))
      {
         formatDTLTEXToggleButton = createFormatButton(
           fileFormatComp, "dtltex", formatBtnGrp);
         lastButton = formatDTLTEXToggleButton;
         numFormats++;
      }

      if (DatatoolFileFormat.isDTLTEX2(formatModifiers))
      {
         formatDTLTEX2ToggleButton = createFormatButton(
           fileFormatComp, "dtltex-2", formatBtnGrp);
         lastButton = formatDTLTEX2ToggleButton;
         numFormats++;
      }

      if (DatatoolFileFormat.isDTLTEX3(formatModifiers))
      {
         formatDTLTEX3ToggleButton = createFormatButton(
           fileFormatComp, "dtltex-3", formatBtnGrp);
         lastButton = formatDTLTEX3ToggleButton;
         numFormats++;
      }

      if (DatatoolFileFormat.isDBTEX(formatModifiers))
      {
         formatDBTEXToggleButton = createFormatButton(
           fileFormatComp, "dbtex", formatBtnGrp);
         lastButton = formatDBTEXToggleButton;
         numFormats++;
      }

      if (DatatoolFileFormat.isDBTEX2(formatModifiers))
      {
         formatDBTEX2ToggleButton = createFormatButton(
           fileFormatComp, "dbtex-2", formatBtnGrp);
         lastButton = formatDBTEX2ToggleButton;
         numFormats++;
      }

      if (DatatoolFileFormat.isDBTEX3(formatModifiers))
      {
         formatDBTEX3ToggleButton = createFormatButton(
           fileFormatComp, "dbtex-3", formatBtnGrp);
         lastButton = formatDBTEX3ToggleButton;
         numFormats++;
      }

      if (DatatoolFileFormat.isCSV(formatModifiers))
      {
         formatCSVToggleButton = createFormatButton(
           fileFormatComp, "csv", formatBtnGrp);
         lastButton = formatCSVToggleButton;
         numFormats++;
      }

      if (DatatoolFileFormat.isTSV(formatModifiers))
      {
         formatTSVToggleButton = createFormatButton(
           fileFormatComp, "tsv", formatBtnGrp);
         lastButton = formatTSVToggleButton;
         numFormats++;
      }

      if (DatatoolFileFormat.isODS(formatModifiers))
      {
         formatODSToggleButton = createFormatButton(
           fileFormatComp, "ods", formatBtnGrp);
         lastButton = formatODSToggleButton;
         numFormats++;
      }

      if (DatatoolFileFormat.isFODS(formatModifiers))
      {
         formatFODSToggleButton = createFormatButton(
           fileFormatComp, "fods", formatBtnGrp);
         lastButton = formatFODSToggleButton;
         numFormats++;
      }

      if (DatatoolFileFormat.isXLSX(formatModifiers))
      {
         formatXLSXToggleButton = createFormatButton(
           fileFormatComp, "xlsx", formatBtnGrp);
         lastButton = formatXLSXToggleButton;
         numFormats++;
      }

      if (DatatoolFileFormat.isXLS(formatModifiers))
      {
         formatXLSToggleButton = createFormatButton(
           fileFormatComp, "xls", formatBtnGrp);
         lastButton = formatXLSToggleButton;
         numFormats++;
      }

      if (DatatoolFileFormat.isSQL(formatModifiers))
      {
         formatSQLToggleButton = createFormatButton(
           fileFormatComp, "sql", formatBtnGrp);
         lastButton = formatSQLToggleButton;
         numFormats++;
      }

      if (numFormats == 1)
      {
         lastButton.setSelected(true);
         lastButton.setVisible(false);
         fileFormatComp.add(new JLabel(lastButton.getText()));
      }
   }

   protected JToggleButton createFormatButton(JComponent comp,
     String label, ButtonGroup grp)
   {
      JToggleButton btn;

      if (grp == null)
      {
         btn = createJCheckBox("format", label, "format", this);
      }
      else
      {
         btn = createJRadioButton("format", label, "format", grp, this);
      }

      comp.add(btn);
      return btn;
   }

   protected void initTeXCardComp()
   {
      texCardComp = createVerticalBox();

      JComponent row = createRow();
      texCardComp.add(row);

      texEncodingBox = new JComboBox<Charset>(
         Charset.availableCharsets().values().toArray(new Charset[0]));
      row.add(createJLabel("tex.encoding", texEncodingBox));
      row.add(texEncodingBox);

      row.setMaximumSize(row.getPreferredSize());

      if (isRead())
      {
         stripSolnEnvBox = createJCheckBox("tex", "stripsolnenv");
         texCardComp.add(stripSolnEnvBox);
         stripSolnEnvBox.setMaximumSize(stripSolnEnvBox.getPreferredSize());
      }

      if (isWrite())
      {
         if (isRead())
         {
            // Include note that this setting only applicable for output files
            texIncHeaderBox = createJCheckBox("writeonly", "hasheader");
         }
         else
         {
            texIncHeaderBox = createJCheckBox("write", "hasheader");
         }

         texIncHeaderBox.setMaximumSize(texIncHeaderBox.getPreferredSize());
         texCardComp.add(texIncHeaderBox);
      }
   }

   protected void initNonTeXComp()
   {
      nonTeXCardComp = createVerticalBox();

      boolean isCsvTsv
         = DatatoolFileFormat.isCsvOrTsv(formatModifiers);

      boolean isSpread
         = DatatoolFileFormat.isSpreadSheet(formatModifiers);

      boolean isSql = DatatoolFileFormat.isSQL(formatModifiers);

      if (isSql)
      {
         sqlPanel = new SqlPanel(resources.getGUI(), getMessageParentTag("sql"));
         nonTeXCardComp.add(sqlPanel);

         String title = getMessageIfExists("sql");

         if (title != null)
         {
            sqlPanel.setBorder(BorderFactory.createTitledBorder(
             BorderFactory.createEtchedBorder(), title));
         }
      }

      if (isCsvTsv)
      {
         initCsvTsvOnlyComp();
      }

      if (isCsvTsv || isSpread)
      {
         initCsvSpreadsheetComp();
      }

      initAllNonTeXComp();
   }

   protected void initCsvSpreadsheetComp()
   {
      csvSpreadComp = createVerticalBox();

      Border border = BorderFactory.createEtchedBorder();

      String title = getMessageIfExists("csvtsvspread");

      if (title != null)
      {
         border = BorderFactory.createTitledBorder(border, title);
      }

      csvSpreadComp.setBorder(border);

      nonTeXCardComp.add(csvSpreadComp);

      csvSpreadComp.add(createMessageArea("csvspreadsheets.note"));
 
      csvIncHeaderBox = createJCheckBox("csv", "hasheader");
      csvSpreadComp.add(csvIncHeaderBox);

      if (isRead())
      {
         JComponent row = createRow();
         csvSpreadComp.add(row);

         skipLinesModel = new SpinnerNumberModel(0, 0, 1000, 1);
         JSpinner skipLinesSpinner = new JSpinner(skipLinesModel);

         JLabel skipLinesLabel =
           createJLabel("csv.skiplines", skipLinesSpinner);

         row.add(skipLinesLabel);
         row.add(skipLinesSpinner);

         row = createRow();
         csvSpreadComp.add(row);
         ButtonGroup btnGrp = new ButtonGroup();

         row.add(createJLabel("csv.blank"));

         csvBlankIgnoreButton = createJRadioButton(
           "csv.blank", "ignore", btnGrp);
         row.add(csvBlankIgnoreButton);

         csvBlankEmptyButton = createJRadioButton(
           "csv.blank", "empty-row", btnGrp);
         row.add(csvBlankEmptyButton);

         csvBlankEndButton = createJRadioButton(
           "csv.blank", "end", btnGrp);
         row.add(csvBlankEndButton);

      }
   }

   protected void initAllNonTeXComp()
   {
      if (isRead())
      {
         allNonTeXComp = createVerticalBox();

         Border border = BorderFactory.createEtchedBorder();

         String title = getMessageIfExists("nontex");

         if (title != null)
         {
            border = BorderFactory.createTitledBorder(border, title);
         }

         allNonTeXComp.setBorder(border);

         nonTeXCardComp.add(allNonTeXComp);

         allNonTeXComp.add(createMessageArea("imports_all.note"));
 
         JComponent row = createRow();
         allNonTeXComp.add(row);
         ButtonGroup btnGrp = new ButtonGroup();

         row.add(createJLabel("csv.content"));

         csvLiteralButton = createJRadioButton(
           "csv", "content.literal", "content", btnGrp);
         row.add(csvLiteralButton);

         csvTeXButton = createJRadioButton(
           "csv", "content.tex", "content", btnGrp);
         row.add(csvTeXButton);

         literalSourceComp = createVerticalBox();
         allNonTeXComp.add(literalSourceComp);

         mapTeXBox = createJCheckBox("tex", "map", "texmap");
         literalSourceComp.add(mapTeXBox);

         createTeXMapTable();
         literalSourceComp.add(texMappingsComp);
         setTeXMapping(false);
      }
   }

   protected void initCsvTsvOnlyComp()
   {
      csvTsvOnlyComp = createVerticalBox();
      nonTeXCardComp.add(csvTsvOnlyComp);

      Border border = BorderFactory.createEtchedBorder();

      String title = getMessageIfExists("csvtsv");

      if (title != null)
      {
         border = BorderFactory.createTitledBorder(border, title);
      }

      csvTsvOnlyComp.setBorder(border);

      csvTsvOnlyComp.add(createMessageArea("csvtsv.optionsonly.note"));

      JLabelGroup labelGrp = new JLabelGroup();
      ButtonGroup btnGrp = new ButtonGroup();
      JComponent row = createRow();
      csvTsvOnlyComp.add(row);

      row.add(createJLabel(labelGrp, "csv.sep"));

      csvSeparatorButton = createJRadioButton(
        "csv", "sepchar", btnGrp, this);
      row.add(csvSeparatorButton);

      csvSepField = new CharField(',');
      row.add(csvSepField);

      row.add(createJLabel("csv.or"));

      tabSeparatorButton = createJRadioButton(
        "csv", "tabsep", btnGrp, this);
      row.add(tabSeparatorButton);

      row = createRow();
      csvTsvOnlyComp.add(row);

      csvDelimField = new CharField('"');

      row.add(createJLabel(labelGrp, "csv.delim", csvDelimField));

      row.add(csvDelimField);

      strictQuotesBox = createJCheckBox("csv", "strictquotes");
      row.add(strictQuotesBox);

      row = createRow();
      csvTsvOnlyComp.add(row);
      btnGrp = new ButtonGroup();

      row.add(createJLabel("csv.escchars"));

      row = createRow();
      csvTsvOnlyComp.add(row);
      btnGrp = new ButtonGroup();

      escapeCharsNoneButton = createJRadioButton(
        "csv.escchars", "none", btnGrp);
      row.add(escapeCharsNoneButton);

      escapeCharsDoubleDelimButton = createJRadioButton(
        "csv.escchars", "double-delim", btnGrp);
      row.add(escapeCharsDoubleDelimButton);

      escapeCharsEscDelimButton = createJRadioButton(
        "csv.escchars", "delim", btnGrp);
      row.add(escapeCharsEscDelimButton);

      escapeCharsEscDelimBkslButton = createJRadioButton(
        "csv.escchars", "delim+bksl", btnGrp);
      row.add(escapeCharsEscDelimBkslButton);

      row = createRow();
      csvTsvOnlyComp.add(row);

      csvEncodingBox = new JComboBox<Charset>(
        Charset.availableCharsets().values().toArray(new Charset[0]));
      row.add(createJLabel("csv.encoding", csvEncodingBox));
      row.add(csvEncodingBox);

      if (isWrite())
      {
         row = createRow();
         csvTsvOnlyComp.add(row);
         btnGrp = new ButtonGroup();

         row.add(createJLabel("csv.add_delim"));

         addDelimiterAlwaysButton = createJRadioButton(
           "csv", "add_delim.always", btnGrp);
         row.add(addDelimiterAlwaysButton);

         addDelimiterDetectButton = createJRadioButton(
           "csv", "add_delim.detect", btnGrp);
         row.add(addDelimiterDetectButton);

         addDelimiterNeverButton = createJRadioButton(
           "csv", "add_delim.never", btnGrp);
         row.add(addDelimiterNeverButton);
      }
   }

   protected void createTeXMapTable()
   {
      texMappingsComp = createRow(new BorderLayout());
      texMapTable = new JTable();
      texMapTable.addMouseListener(new MouseAdapter()
       {
          @Override
          public void mouseClicked(MouseEvent evt)
          {
             if (evt.getClickCount() == 2 && evt.getModifiersEx() == 0)
             {
                texMapModel.editRow(texMapTable.getSelectedRow());
             }
          }
       });

      texMapModel = new TeXMapModel(this, texMapTable,
        resources.getGUI().getSettings());
      texMapTable.setModel(texMapModel);

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
      
      buttonPanel.add(createActionButton(
         "tex", "add_map", "increase"));
      
      editMapButton = createActionButton(
         "tex", "edit_map", "edit");
      buttonPanel.add(editMapButton);

      removeMapButton = createActionButton(
         "tex", "remove_map", "decrease");
      buttonPanel.add(removeMapButton);

      texMapTable.getSelectionModel().addListSelectionListener(
         new ListSelectionListener()
         {
            @Override
            public void valueChanged(ListSelectionEvent evt)
            {
               updateTeXMapButtons();
            }
         }
      );
   }

   protected JComponent createRow()
   {
      return createRow(new FlowLayout(FlowLayout.LEADING));
   }

   protected JComponent createRow(LayoutManager layout)
   {
      JComponent comp = new JPanel(layout);

      comp.setAlignmentX(defaultAlignmentX);

      return comp;
   }

   protected Box createVerticalBox()
   {
      Box box = Box.createVerticalBox();
      box.setAlignmentX(defaultAlignmentX);
      return box;
   }

   protected JRadioButton createJRadioButton(String parent, String label,
     ButtonGroup btnGrp)
   {
      return createJRadioButton(parent, label, label, btnGrp, this);
   }

   protected JRadioButton createJRadioButton(String parent, String label,
     String action, ButtonGroup btnGrp)
   {
      return createJRadioButton(parent, label, action, btnGrp, this);
   }

   protected JRadioButton createJRadioButton(String parent, String label,
     ButtonGroup btnGrp, ActionListener listener)
   {
      return createJRadioButton(parent, label, label, btnGrp, listener);
   }

   protected JRadioButton createJRadioButton(String parent, String label,
     String action, ButtonGroup btnGrp, ActionListener listener)
   {
      JRadioButton button = resources.createJRadioButton(
        getMessageParentTag(parent, label), label, action, btnGrp, listener);

      button.setAlignmentX(defaultAlignmentX);

      return button;
   }

   protected JCheckBox createJCheckBox(String parent, String label)
   {
      return createJCheckBox(parent, label, label, this);
   }

   protected JCheckBox createJCheckBox(String parent, String label,
     String action)
   {
      return createJCheckBox(parent, label, action, this);
   }

   protected JCheckBox createJCheckBox(String parent, String label,
     ActionListener listener)
   {
      return createJCheckBox(parent, label, label, listener);
   }

   protected JCheckBox createJCheckBox(String parent, String label,
     String action, ActionListener listener)
   {
      JCheckBox checkBox = resources.createJCheckBox(
        getMessageParentTag(parent, label), label, action, listener);

      checkBox.setAlignmentX(defaultAlignmentX);

      return checkBox;
   }

   protected JButton createActionButton(String parent, String label)
   {
      return createActionButton(parent, label, label);
   }

   protected JButton createActionButton(String parent, String label,
      String iconPrefix)
   {
      return createActionButton(parent, label, iconPrefix, this, null);
   }

   protected JButton createActionButton(String parent, String label,
      String iconPrefix, ActionListener listener, KeyStroke keyStroke)
   {
      JButton button = resources.createActionButton(
         getMessageParentTag(parent, label), label, iconPrefix,
         listener, keyStroke);

      return button;
   }

   protected JLabel createJLabel(String tag)
   {
      return resources.createJLabel(getMessageTag(tag));
   }

   protected JLabel createJLabel(String tag, JComponent comp)
   {
      return resources.createJLabel(getMessageTag(tag), comp);
   }

   protected JLabel createJLabel(JLabelGroup labelGrp, String tag)
   {
      return labelGrp.createJLabel(resources.getHelpLib(), getMessageTag(tag));
   }

   protected JLabel createJLabel(JLabelGroup labelGrp, String tag,
     JComponent comp)
   {
      return labelGrp.createJLabel(resources.getHelpLib(), getMessageTag(tag), comp);
   }

   protected JTextArea createMessageArea(int rows, int cols, String label)
   {
      JTextArea comp = resources.createMessageArea(rows, cols, getMessageTag(label));

      comp.setAlignmentX(defaultAlignmentX);

      return comp;
   }

   protected JTextArea createMessageArea(String label)
   {
      JTextArea comp = resources.createMessageArea(0, 0, getMessageTag(label));

      comp.setAlignmentX(defaultAlignmentX);

      return comp;
   }

   protected String getMessage(String subTag, Object... params)
   {
      TeXJavaHelpLib helpLib = resources.getHelpLib();

      if (tagParentLabel != null)
      {
         String val = helpLib.getMessageIfExists(tagParentLabel +"."+subTag,
            params);

         if (val != null)
         {
            return val;
         }
      }

      return helpLib.getMessage("iosettings."+subTag, params);
   }

   protected String getMessageIfExists(String subTag, Object... params)
   {
      TeXJavaHelpLib helpLib = resources.getHelpLib();

      if (tagParentLabel != null)
      {
         String val = helpLib.getMessageIfExists(tagParentLabel +"."+subTag,
            params);

         if (val != null)
         {
            return val;
         }
      }

      return helpLib.getMessageIfExists("iosettings."+subTag, params);
   }

   protected String getMessageTag(String label)
   {
      return getMessageTag(null, label);
   }

   protected String getMessageTag(String parent, String label)
   {
      TeXJavaHelpLib helpLib = resources.getHelpLib();
      String subTag = (parent == null ? label : parent+"."+label);

      if (tagParentLabel != null)
      {
         String tag = tagParentLabel +"."+subTag;

         if (helpLib.isMessageLabelValid(tag))
         {
            return tag;
         }
      }

      return "iosettings."+subTag;
   }

   protected String getMessageParentTag(String label)
   {
      return getMessageParentTag(null, label);
   }

   protected String getMessageParentTag(String parent, String label)
   {
      TeXJavaHelpLib helpLib = resources.getHelpLib();

      if (tagParentLabel != null)
      {
         String subTag = tagParentLabel;

         if (parent != null)
         {
            subTag += "."+parent;
         }

         if (helpLib.isMessageLabelValid(subTag+"."+label))
         {
            return subTag;
         }
      }

      String subTag = "iosettings";

      if (parent != null)
      {
         subTag += "."+parent;
      }

      return subTag;
   }

   protected void formatChanged()
   {
      if (isFileFormatSelected(DatatoolFileFormat.FILE_FORMAT_ANY_TEX))
      {
         if (cardLayout != null)
         {
            cardLayout.show(cardComp, "tex");
         }
      }
      else
      {
         if (cardLayout != null)
         {
            cardLayout.show(cardComp, "nontex");
         }

         if (isFileFormatSelected(DatatoolFileFormat.FILE_FORMAT_CSV_OR_TSV))
         {
            csvTsvOnlyComp.setVisible(true);

            if (formatCSVToggleButton instanceof JRadioButton)
            {
               if (formatCSVToggleButton.isSelected())
               {
                  if (!csvSeparatorButton.isSelected())
                  {
                     csvSeparatorButton.setSelected(true);
                     csvSepField.setEnabled(true);
                  }
               }
               else if (formatTSVToggleButton.isSelected()
                       && !tabSeparatorButton.isSelected())
               {
                  tabSeparatorButton.setSelected(true);
                  csvSepField.setEnabled(false);
               }
            }
         }
         else if (csvTsvOnlyComp != null)
         {
            csvTsvOnlyComp.setVisible(false);
         }

         if (isFileFormatSelected(
                DatatoolFileFormat.FILE_FORMAT_CSV_OR_TSV
              | DatatoolFileFormat.FILE_FORMAT_ANY_SPREADSHEET))
         {
            csvSpreadComp.setVisible(true);
         }
         else if (csvSpreadComp != null)
         {
            csvSpreadComp.setVisible(false);
         }

         if (sqlPanel != null)
         {
            sqlPanel.setVisible(isFileFormatSelected(
               DatatoolFileFormat.FILE_FORMAT_FLAG_SQL));
         }
      }
   }

   protected void updateFormatModifiers()
   {
      selectedFormatModifiers = 0;

      if (formatSingleSelection)
      {
         if (formatTeXToggleButton != null
               && formatTeXToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_ANY_TEX;
         }
         else if (formatDTLTEXToggleButton != null
               && formatDTLTEXToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_ANY_DTLTEX;
         }
         else if (formatDTLTEX2ToggleButton != null
               && formatDTLTEX2ToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_FLAG_DTLTEX2;
         }
         else if (formatDTLTEX3ToggleButton != null
               && formatDTLTEX3ToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_FLAG_DTLTEX3;
         }
         else if (formatDBTEXToggleButton != null
               && formatDBTEXToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_ANY_DBTEX;
         }
         else if (formatDBTEX2ToggleButton != null
               && formatDBTEX2ToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_FLAG_DBTEX2;
         }
         else if (formatDBTEX3ToggleButton != null
               && formatDBTEX3ToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_FLAG_DBTEX3;
         }
         else if (formatCSVToggleButton != null
               && formatCSVToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_FLAG_CSV;
         }
         else if (formatTSVToggleButton != null
               && formatTSVToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_FLAG_TSV;
         }
         else if (formatODSToggleButton != null
               && formatODSToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_FLAG_ODS;
         }
         else if (formatFODSToggleButton != null
               && formatFODSToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_FLAG_FODS;
         }
         else if (formatXLSToggleButton != null
               && formatXLSToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_FLAG_XLS;
         }
         else if (formatXLSXToggleButton != null
               && formatXLSXToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_FLAG_XLSX;
         }
         else if (formatSQLToggleButton != null
               && formatSQLToggleButton.isSelected())
         {
            selectedFormatModifiers = DatatoolFileFormat.FILE_FORMAT_FLAG_SQL;
         }
      }
      else
      {
         if (formatTeXToggleButton != null
               && formatTeXToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_ANY_TEX;
         }

         if (formatDTLTEXToggleButton != null
               && formatDTLTEXToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_ANY_DTLTEX;
         }

         if (formatDTLTEX2ToggleButton != null
               && formatDTLTEX2ToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_FLAG_DTLTEX2;
         }

         if (formatDTLTEX3ToggleButton != null
               && formatDTLTEX3ToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_FLAG_DTLTEX3;
         }

         if (formatDBTEXToggleButton != null
               && formatDBTEXToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_ANY_DBTEX;
         }

         if (formatDBTEX2ToggleButton != null
               && formatDBTEX2ToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_FLAG_DBTEX2;
         }

         if (formatDBTEX3ToggleButton != null
               && formatDBTEX3ToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_FLAG_DBTEX3;
         }

         if (formatCSVToggleButton != null
               && formatCSVToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_FLAG_CSV;
         }

         if (formatTSVToggleButton != null
               && formatTSVToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_FLAG_TSV;
         }

         if (formatODSToggleButton != null
               && formatODSToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_FLAG_ODS;
         }

         if (formatFODSToggleButton != null
               && formatFODSToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_FLAG_FODS;
         }

         if (formatXLSToggleButton != null
               && formatXLSToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_FLAG_XLS;
         }

         if (formatXLSXToggleButton != null
               && formatXLSXToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_FLAG_XLSX;
         }

         if (formatSQLToggleButton != null
               && formatSQLToggleButton.isSelected())
         {
            selectedFormatModifiers |= DatatoolFileFormat.FILE_FORMAT_FLAG_SQL;
         }
      }
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if ("format".equals(action))
      {
         int oldModifiers = selectedFormatModifiers;
         updateFormatModifiers();

         formatChanged();

         if (oldModifiers != selectedFormatModifiers)
         {
            fireFileFormatChange(
              new FileFormatSelectionChangeEvent(this,
                     oldModifiers, selectedFormatModifiers));
         }
      }
      else if ("sepchar".equals(action))
      {
         csvSepField.setEnabled(true);

         if (isCsvTsvOnly())
         {
            formatCSVToggleButton.setSelected(true);
         }
      }
      else if ("tabsep".equals(action))
      {
         csvSepField.setEnabled(false);

         if (isCsvTsvOnly())
         {
            formatTSVToggleButton.setSelected(true);
         }
      }
      else if ("content".equals(action))
      {
         if (isLiteralContent())
         {
            literalSourceComp.setVisible(true);
            mapTeXBox.setSelected(true);
            texMappingsComp.setVisible(true);
         }
         else
         {
            literalSourceComp.setVisible(false);
         }
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
   }

   protected void updateTeXMapButtons()
   {
      if (texMapTable != null)
      {
         boolean enabled = texMapTable.getSelectedRow() != -1;
         editMapButton.setEnabled(enabled);
         removeMapButton.setEnabled(enabled);
      }
   }

   public boolean isLiteralContent()
   {
      return (csvLiteralButton != null && csvLiteralButton.isSelected());
   }

   public void setLiteralContent(boolean isLiteral)
   {
      if (csvLiteralButton != null)
      {
         if (isLiteral)
         {
            csvLiteralButton.setSelected(true);
         }
         else
         {
            csvTeXButton.setSelected(true);
         }

         if (isLiteral)
         {
            literalSourceComp.setVisible(true);
            mapTeXBox.setSelected(true);
            texMappingsComp.setVisible(true);
         }
         else
         {
            literalSourceComp.setVisible(false);
         }
      }
   }

   public boolean isTeXMappingOn()
   {
      return mapTeXBox != null && mapTeXBox.isSelected();
   }

   public void setTeXMapping(boolean on)
   {
      if (mapTeXBox != null)
      {
         mapTeXBox.setSelected(on);

         texMappingsComp.setVisible(on);
      }
   }

   public int getSeparator()
   {
      if (tabSeparatorButton != null && tabSeparatorButton.isSelected())
      {
         return '\t';
      }
      else if (csvSepField != null && csvSepField.isEnabled())
      {
         return csvSepField.getValue();
      }

      return -1;
   }

   public void setSeparator(int sep)
   {
      if (csvTsvOnlyComp != null && sep > 0)
      {
         if (sep == '\t')
         {
            tabSeparatorButton.setSelected(true);

            if (formatCSVToggleButton.isSelected())
            {
               formatTSVToggleButton.setSelected(true);
            }
         }
         else
         {
            csvSeparatorButton.setSelected(true);
            csvSepField.setValue(sep);

            if (formatTSVToggleButton.isSelected())
            {
               formatCSVToggleButton.setSelected(true);
            }
         }
      }
   }

   public void setDelimiter(int delim)
   {
      if (csvDelimField != null && delim > 0)
      {
         csvDelimField.setValue(delim);

         if (isCsvTsvOnly())
         {
            if (delim == '\t')
            {
               formatTSVToggleButton.setSelected(true);
            }
            else
            {
               formatCSVToggleButton.setSelected(true);
            }
         }
      }
   }

   public int getDelimiter()
   {
      if (csvDelimField != null)
      {
         return csvDelimField.getValue();
      }

      return -1;
   }

   public int getSkipLines()
   {
      return skipLinesModel == null ? 0 : skipLinesModel.getNumber().intValue();
   }

   public void setSkipLines(int skipLines)
   {
      if (skipLinesModel != null)
      {
         skipLinesModel.setValue(Integer.valueOf(skipLines));
      }
   }

   public CsvBlankOption getCsvBlankOption()
   {
      if (csvBlankIgnoreButton != null && csvBlankIgnoreButton.isSelected())
      {
         return CsvBlankOption.IGNORE;
      }

      if (csvBlankEmptyButton != null && csvBlankEmptyButton.isSelected())
      {
         return CsvBlankOption.EMPTY_ROW;
      }

      if (csvBlankEndButton != null && csvBlankEndButton.isSelected())
      {
         return CsvBlankOption.END;
      }

      return null;
   }

   public void setCsvBlankOption(CsvBlankOption option)
   {
      switch (option)
      {
         case IGNORE:
            if (csvBlankIgnoreButton != null)
            {
               csvBlankIgnoreButton.setSelected(true);
            }
         break;
         case EMPTY_ROW:
            if (csvBlankEmptyButton != null)
            {
               csvBlankEmptyButton.setSelected(true);
            }
         break;
         case END:
            if (csvBlankEndButton != null)
            {
               csvBlankEndButton.setSelected(true);
            }
         break;
      }
   }

   public boolean isStrictQuotes()
   {
      return strictQuotesBox != null && strictQuotesBox.isSelected();
   }

   public void setStrictQuotes(boolean on)
   {
      if (strictQuotesBox != null)
      {
         strictQuotesBox.setSelected(on);
      }
   }

   public EscapeCharsOption getEscapeCharsOption()
   {
      if (escapeCharsNoneButton != null
       && escapeCharsNoneButton.isSelected())
      {
         return EscapeCharsOption.NONE;
      }

      if (escapeCharsDoubleDelimButton != null
       && escapeCharsDoubleDelimButton.isSelected())
      {
         return EscapeCharsOption.DOUBLE_DELIM;
      }

      if (escapeCharsEscDelimButton != null
       && escapeCharsEscDelimButton.isSelected())
      {
         return EscapeCharsOption.ESC_DELIM;
      }

      return null;
   }

   public void setEscapeCharsOption(EscapeCharsOption option)
   {
      switch (option)
      {
         case NONE:
            if (escapeCharsNoneButton != null)
            {
               escapeCharsNoneButton.setSelected(true);
            }
         break;
         case DOUBLE_DELIM:
            if (escapeCharsDoubleDelimButton != null)
            {
               escapeCharsDoubleDelimButton.setSelected(true);
            }
         break;
         case ESC_DELIM:
            if (escapeCharsEscDelimButton != null)
            {
               escapeCharsEscDelimButton.setSelected(true);
            }
         break;
         case ESC_DELIM_BKSL:
            if (escapeCharsEscDelimBkslButton != null)
            {
               escapeCharsEscDelimBkslButton.setSelected(true);
            }
         break;
      }
   }

   public AddDelimiterOption getAddDelimiterOption()
   {
      if (addDelimiterAlwaysButton != null
       && addDelimiterAlwaysButton.isSelected())
      {
         return AddDelimiterOption.ALWAYS;
      }

      if (addDelimiterDetectButton != null
       && addDelimiterDetectButton.isSelected())
      {
         return AddDelimiterOption.DETECT;
      }

      if (addDelimiterNeverButton != null
       && addDelimiterNeverButton.isSelected())
      {
         return AddDelimiterOption.NEVER;
      }

      return null;
   }

   public void setAddDelimiterOption(AddDelimiterOption option)
   {
      switch (option)
      {
         case ALWAYS:
            if (addDelimiterAlwaysButton != null)
            {
               addDelimiterAlwaysButton.setSelected(true);
            }
         break;
         case DETECT:
            if (addDelimiterDetectButton != null)
            {
               addDelimiterDetectButton.setSelected(true);
            }
         break;
         case NEVER:
            if (addDelimiterNeverButton != null)
            {
               addDelimiterNeverButton.setSelected(true);
            }
         break;
      }
   }

   public boolean isStripSolnEnvOn()
   {
      return stripSolnEnvBox != null && stripSolnEnvBox.isSelected();
   }

   public void setStripSolnEnvOn(boolean on)
   {
      if (stripSolnEnvBox != null)
      {
         stripSolnEnvBox.setSelected(on);
      }
   }

   public boolean isEmptyToNullOn()
   {
      return emptyToNullBox != null && emptyToNullBox.isSelected();
   }

   public void setEmptyToNullOn(boolean on)
   {
      if (emptyToNullBox != null)
      {
         emptyToNullBox.setSelected(on);
      }
   }

   public boolean isTrimElementOn()
   {
      return trimElementBox != null && trimElementBox.isSelected();
   }

   public void setTrimElementOn(boolean on)
   {
      if (trimElementBox != null)
      {
         trimElementBox.setSelected(on);
      }
   }

   public boolean isHeaderIncluded()
   {
      if (isFileFormatSelected(DatatoolFileFormat.FILE_FORMAT_ANY_TEX))
      {
         return texIncHeaderBox != null && texIncHeaderBox.isSelected();
      }
      else
      {
         return csvIncHeaderBox != null && csvIncHeaderBox.isSelected();
      }
   }

   public void setHeaderIncluded(boolean headerIncluded)
   {
      if (csvIncHeaderBox != null)
      {
         csvIncHeaderBox.setSelected(headerIncluded);
      }

      if (texIncHeaderBox != null)
      {
         texIncHeaderBox.setSelected(headerIncluded);
      }
   }

   public boolean isFileFormatSelected(int modifiers)
   {
      if (DatatoolFileFormat.isTeX(modifiers))
      {
         if (formatTeXToggleButton != null
          && formatTeXToggleButton.isSelected())
         {
            return true;
         }
      }

      if (DatatoolFileFormat.isDTLTEX(modifiers))
      {
         if (formatDTLTEXToggleButton != null
          && formatDTLTEXToggleButton.isSelected())
         {
            return true;
         }
      }

      if (DatatoolFileFormat.isDTLTEX2(modifiers))
      {
         if (formatDTLTEX2ToggleButton != null
          && formatDTLTEX2ToggleButton.isSelected())
         {
            return true;
         }
      }

      if (DatatoolFileFormat.isDTLTEX3(modifiers))
      {
         if (formatDTLTEX3ToggleButton != null
          && formatDTLTEX3ToggleButton.isSelected())
         {
            return true;
         }
      }

      if (DatatoolFileFormat.isDBTEX(modifiers))
      {
         if (formatDBTEXToggleButton != null
          && formatDBTEXToggleButton.isSelected())
         {
            return true;
         }
      }

      if (DatatoolFileFormat.isDBTEX2(modifiers))
      {
         if (formatDBTEX2ToggleButton != null
          && formatDBTEX2ToggleButton.isSelected())
         {
            return true;
         }
      }

      if (DatatoolFileFormat.isDBTEX3(modifiers))
      {
         if (formatDBTEX3ToggleButton != null
          && formatDBTEX3ToggleButton.isSelected())
         {
            return true;
         }
      }

      if (DatatoolFileFormat.isCSV(modifiers))
      {
         if (formatCSVToggleButton != null
          && formatCSVToggleButton.isSelected())
         {
            return true;
         }
      }

      if (DatatoolFileFormat.isTSV(modifiers))
      {
         if (formatTSVToggleButton != null
          && formatTSVToggleButton.isSelected())
         {
            return true;
         }
      }

      if (DatatoolFileFormat.isODS(modifiers))
      {
         if (formatODSToggleButton != null
          && formatODSToggleButton.isSelected())
         {
            return true;
         }
      }

      if (DatatoolFileFormat.isFODS(modifiers))
      {
         if (formatFODSToggleButton != null
          && formatFODSToggleButton.isSelected())
         {
            return true;
         }
      }

      if (DatatoolFileFormat.isXLS(modifiers))
      {
         if (formatXLSToggleButton != null
          && formatXLSToggleButton.isSelected())
         {
            return true;
         }
      }

      if (DatatoolFileFormat.isXLSX(modifiers))
      {
         if (formatXLSXToggleButton != null
          && formatXLSXToggleButton.isSelected())
         {
            return true;
         }
      }

      if (DatatoolFileFormat.isSQL(modifiers))
      {
         if (formatSQLToggleButton != null
          && formatSQLToggleButton.isSelected())
         {
            return true;
         }
      }

      return false;
   }

   public void setSelectedFileFormat(int modifiers)
   {
      int oldModifiers = selectedFormatModifiers;
      selectedFormatModifiers = modifiers;

      if (formatTeXToggleButton != null)
      {
         formatTeXToggleButton.setSelected(
            DatatoolFileFormat.isTeX(modifiers));
      }

      if (formatDBTEXToggleButton != null)
      {
         formatDBTEXToggleButton.setSelected(
            DatatoolFileFormat.isDBTEX(modifiers));
      }

      if (formatDBTEX2ToggleButton != null)
      {
         formatDBTEX2ToggleButton.setSelected(
            DatatoolFileFormat.isDBTEX2(modifiers));
      }

      if (formatDBTEX3ToggleButton != null)
      {
         formatDBTEX3ToggleButton.setSelected(
            DatatoolFileFormat.isDBTEX3(modifiers));
      }

      if (formatDTLTEXToggleButton != null)
      {
         formatDTLTEXToggleButton.setSelected(
            DatatoolFileFormat.isDTLTEX(modifiers));
      }

      if (formatDTLTEX2ToggleButton != null)
      {
         formatDTLTEX2ToggleButton.setSelected(
            DatatoolFileFormat.isDTLTEX2(modifiers));
      }

      if (formatDTLTEX3ToggleButton != null)
      {
         formatDTLTEX3ToggleButton.setSelected(
            DatatoolFileFormat.isDTLTEX3(modifiers));
      }

      if (formatCSVToggleButton != null)
      {
         formatCSVToggleButton.setSelected(
            DatatoolFileFormat.isCSV(modifiers));
      }

      if (formatTSVToggleButton != null)
      {
         formatTSVToggleButton.setSelected(
            DatatoolFileFormat.isTSV(modifiers));
      }

      if (formatODSToggleButton != null)
      {
         formatODSToggleButton.setSelected(
            DatatoolFileFormat.isODS(modifiers));
      }

      if (formatFODSToggleButton != null)
      {
         formatFODSToggleButton.setSelected(
            DatatoolFileFormat.isFODS(modifiers));
      }

      if (formatXLSToggleButton != null)
      {
         formatXLSToggleButton.setSelected(
            DatatoolFileFormat.isXLS(modifiers));
      }

      if (formatXLSXToggleButton != null)
      {
         formatXLSXToggleButton.setSelected(
            DatatoolFileFormat.isXLSX(modifiers));
      }

      if (formatSQLToggleButton != null)
      {
         formatSQLToggleButton.setSelected(
            DatatoolFileFormat.isSQL(modifiers));
      }

      formatChanged();

      if (oldModifiers != modifiers)
      {
         fireFileFormatChange(
          new FileFormatSelectionChangeEvent(this, oldModifiers, modifiers));
      }
   }

   protected void fireFileFormatChange(FileFormatSelectionChangeEvent evt)
   {
      if (fileFormatChangeListeners != null)
      {
         for (FileFormatSelectionChangeListener listener : fileFormatChangeListeners)
         {
            listener.fileFormatSelectionChanged(evt);

            if (evt.isConsumed())
            {
               break;
            }
         } 
      }
   }

   public void addFileFormatSelectionChangeListener(
      FileFormatSelectionChangeListener listener)
   {
      if (fileFormatChangeListeners == null)
      {
         fileFormatChangeListeners
           = new Vector<FileFormatSelectionChangeListener>();
      }

      fileFormatChangeListeners.add(listener);
   }

   public void setFileFormatComponentVisible(boolean visible)
   {
      fileFormatComp.setVisible(visible);
   }

   public boolean isFileFormatComponentVisible()
   {
      return fileFormatComp.isVisible();
   }

   public void setCsvEncoding(Charset charset)
   {
      if (csvEncodingBox != null)
      {
         csvEncodingBox.setSelectedItem(charset);
      }
   }

   public void setTeXEncoding(Charset charset)
   {
      if (texEncodingBox != null)
      {
         texEncodingBox.setSelectedItem(charset);
      }
   }

   public boolean isRead()
   {
      return (ioModifiers & IO_IN) == IO_IN;
   }

   public boolean isWrite()
   {
      return (ioModifiers & IO_OUT) == IO_OUT;
   }

   /**
    * Determines whether or not this panel only supports CSV/TSV.
    */
   public boolean isCsvTsvOnly()
   {
      return DatatoolFileFormat.isCsvOrTsvOnly(formatModifiers);
   }

   /**
    * Determines whether or not this panel only supports CSV/TSV.
    */
   public boolean isTeXOnly()
   {
      return DatatoolFileFormat.isTeXOnly(formatModifiers);
   }

   public void applyTo(ImportSettings settings)
     throws IllegalArgumentException
   {
      boolean isCsvTsv = DatatoolFileFormat.isCsvOrTsv(selectedFormatModifiers);

      boolean isSpread = DatatoolFileFormat.isSpreadSheet(selectedFormatModifiers);

      boolean isSql = DatatoolFileFormat.isSQL(selectedFormatModifiers);

      if (isCsvTsv)
      {
         int sep = getSeparator();

         if (sep >= 0xFFFF)
         {
            throw new IllegalArgumentException( 
               getMessageHandler().getLabelWithValues("error.char_sep_required", 
               MessageHandler.codePointToString(sep), 
               "0xFFFF"));
         }
         else if (sep > 0)
         {
            settings.setSeparator(sep);
         }
         else
         {
            throw new IllegalArgumentException( 
               getMessageHandler().getLabel("error.missing_sep"));
         }

         int delim = getDelimiter();

         if (delim >= 0xFFFF)
         {
            throw new IllegalArgumentException( 
               getMessageHandler().getLabelWithValues("error.char_delim_required", 
               MessageHandler.codePointToString(delim), 
               "0xFFFF"));
         }
         else if (delim > 0)
         {
            settings.setDelimiter(delim);
         }
         else
         {
            throw new IllegalArgumentException( 
               getMessageHandler().getLabel("error.missing_delim"));
         }

         settings.setStrictQuotes(isStrictQuotes());

         settings.setEscapeCharsOption(getEscapeCharsOption());

         if (csvEncodingBox != null)
         {
            settings.setCsvEncoding((Charset)csvEncodingBox.getSelectedItem());
         }
      }

      if (isCsvTsv || isSpread)
      {
         if (csvIncHeaderBox != null)
         {
            settings.setHasHeaderRow(csvIncHeaderBox.isSelected());
         }

         settings.setSkipLines(getSkipLines());
         settings.setBlankRowAction(getCsvBlankOption());
      }

      if (isCsvTsv || isSpread || isSql)
      {
         settings.setLiteralContent(isLiteralContent());
         settings.setMapChars(isTeXMappingOn());

         texMapModel.updateSettings();
      }
      else
      {
         if (texEncodingBox != null)
         {
            settings.setTeXEncoding((Charset)texEncodingBox.getSelectedItem());
         }
      }

      if (trimElementBox != null)
      {
         settings.setTrimElement(trimElementBox.isSelected());
      }

      if (emptyToNullBox != null)
      {
         settings.setImportEmptyToNull(emptyToNullBox.isSelected());
      }

      if (stripSolnEnvBox != null)
      {
         settings.setStripSolutionEnv(stripSolnEnvBox.isSelected());
      }

      if (isSql && sqlPanel != null)
      {
         sqlPanel.applyTo(settings);
      }
   }

   public void resetFrom(ImportSettings settings)
   {
      setSeparator(settings.getSeparator());
      setDelimiter(settings.getDelimiter());

      setStrictQuotes(settings.isStrictQuotesOn());
      setEscapeCharsOption(settings.getEscapeCharsOption());

      setCsvEncoding(settings.getCsvEncoding());

      if (csvIncHeaderBox != null)
      {
         csvIncHeaderBox.setSelected(settings.hasHeaderRow());
      }

      setSkipLines(settings.getSkipLines());
      setCsvBlankOption(settings.getBlankRowAction());

      setLiteralContent(settings.isLiteralContent());
      setTeXMapping(settings.isMapCharsOn());

      texMapModel = new TeXMapModel(this, texMapTable, settings);
      texMapTable.setModel(texMapModel);
      updateTeXMapButtons();

      setTeXEncoding(settings.getTeXEncoding());

      if (trimElementBox != null)
      {
         trimElementBox.setSelected(settings.isTrimElementOn());
      }

      if (emptyToNullBox != null)
      {
         emptyToNullBox.setSelected(settings.isImportEmptyToNullOn());
      }

      if (stripSolnEnvBox != null)
      {
         stripSolnEnvBox.setSelected(settings.isStripSolutionEnvOn());
      }

      if (sqlPanel != null)
      {
         sqlPanel.resetFrom(settings);
      }
   }

   public void applyCsvSettingsTo(DatatoolSettings settings)
     throws IllegalArgumentException
   {
      int sep = getSeparator();

      if (sep >= 0xFFFF)
      {
         throw new IllegalArgumentException( 
            getMessageHandler().getLabelWithValues("error.char_sep_required", 
            MessageHandler.codePointToString(sep), 
            "0xFFFF"));
      }
      else if (sep > 0)
      {
         settings.setSeparator(sep);
      }
      else
      {
         throw new IllegalArgumentException( 
            getMessageHandler().getLabel("error.missing_sep"));
      }

      int delim = getDelimiter();

      if (delim >= 0xFFFF)
      {
         throw new IllegalArgumentException( 
            getMessageHandler().getLabelWithValues("error.char_delim_required", 
            MessageHandler.codePointToString(delim), 
            "0xFFFF"));
      }
      else if (delim > 0)
      {
         settings.setDelimiter(delim);
      }
      else
      {
         throw new IllegalArgumentException( 
            getMessageHandler().getLabel("error.missing_delim"));
      }

      settings.setCSVstrictquotes(isStrictQuotes());
      settings.setHasCSVHeader(isHeaderIncluded());
      settings.setCSVskiplines(getSkipLines());
      settings.setEscapeCharsOption(getEscapeCharsOption());

      settings.setLiteralContent(isLiteralContent());
      settings.setTeXMapping(isTeXMappingOn());

      texMapModel.updateSettings();

      if (isRead())
      {
         settings.setCsvBlankOption(getCsvBlankOption());
         settings.setCSVskiplines(getSkipLines());
      }

      if (isWrite())
      {
         settings.setAddDelimiterOption(getAddDelimiterOption());
      }

      if (texEncodingBox != null)
      {
         settings.setTeXEncoding((Charset)texEncodingBox.getSelectedItem());
      }

      if (csvEncodingBox != null)
      {
         settings.setCsvEncoding((Charset)csvEncodingBox.getSelectedItem());
      }
   }

   public void setCsvSettingsFrom(DatatoolSettings settings)
   {
      setSeparator(settings.getSeparator());
      setDelimiter(settings.getDelimiter());

      setStrictQuotes(settings.hasCSVstrictquotes());
      setHeaderIncluded(settings.hasCSVHeader());
      setEscapeCharsOption(settings.getEscapeCharsOption());

      setLiteralContent(settings.isLiteralContent());
      setTeXMapping(settings.isTeXMappingOn());

      texMapModel = new TeXMapModel(this, texMapTable, settings);
      texMapTable.setModel(texMapModel);
      updateTeXMapButtons();

      if (isRead())
      {
         setCsvBlankOption(settings.getCsvBlankOption());
         setSkipLines(settings.getCSVskiplines());
      }

      if (isWrite())
      {
         setAddDelimiterOption(settings.getAddDelimiterOption());
      }

      if (csvEncodingBox != null)
      {
         Charset charset = settings.getCsvEncoding();
      
         if (charset == null)
         {
            csvEncodingBox.setSelectedItem(Charset.defaultCharset());
         }
         else
         {
            csvEncodingBox.setSelectedItem(charset);
         }
      }

      if (texEncodingBox != null)
      {
         Charset charset = settings.getTeXEncoding();

         if (charset == null)
         {
            texEncodingBox.setSelectedItem(Charset.defaultCharset());
         }
         else
         {
            texEncodingBox.setSelectedItem(charset);
         }
      }
   }

   public void setFrom(IOSettings ioSettings)
   {
      boolean isV3 = "3.0".equals(ioSettings.getFileVersion());

      switch (ioSettings.getFormat())
      {
         case DBTEX:
           if (isV3)
           {
              setSelectedFileFormat(
                  DatatoolFileFormat.FILE_FORMAT_FLAG_TEX
                | DatatoolFileFormat.FILE_FORMAT_FLAG_DBTEX
                | DatatoolFileFormat.FILE_FORMAT_FLAG_DBTEX3);
           }
           else
           {
              setSelectedFileFormat(
                  DatatoolFileFormat.FILE_FORMAT_FLAG_TEX
                | DatatoolFileFormat.FILE_FORMAT_FLAG_DBTEX
                | DatatoolFileFormat.FILE_FORMAT_FLAG_DBTEX2);
           }
         break;
         case DTLTEX:
           if (isV3)
           {
              setSelectedFileFormat(
                  DatatoolFileFormat.FILE_FORMAT_FLAG_TEX
                | DatatoolFileFormat.FILE_FORMAT_FLAG_DTLTEX
                | DatatoolFileFormat.FILE_FORMAT_FLAG_DTLTEX3);
           }
           else
           {
              setSelectedFileFormat(
                  DatatoolFileFormat.FILE_FORMAT_FLAG_TEX
                | DatatoolFileFormat.FILE_FORMAT_FLAG_DTLTEX
                | DatatoolFileFormat.FILE_FORMAT_FLAG_DTLTEX2);
           }
         break;
         case CSV:
            setSelectedFileFormat(DatatoolFileFormat.FILE_FORMAT_FLAG_CSV);
         break;
         case TSV:
            setSelectedFileFormat(DatatoolFileFormat.FILE_FORMAT_FLAG_TSV);
         break;
      }

      setHeaderIncluded(ioSettings.isHeaderIncluded());

      setTrimElementOn(ioSettings.isTrimElementOn());
      setSeparator(ioSettings.getSeparator());
      setDelimiter(ioSettings.getDelimiter());
      setLiteralContent(ioSettings.isCsvLiteral());
      setSkipLines(ioSettings.getSkipLines());
      setCsvBlankOption(ioSettings.getCsvBlankOption());
      setEscapeCharsOption(ioSettings.getEscapeCharsOption());
      setAddDelimiterOption(ioSettings.getAddDelimiterOption());
   }

   public void applySelectedTo(IOSettings ioSettings)
    throws TeXSyntaxException
   {
      boolean isCsvTsv = false;

      if (formatDBTEX2ToggleButton != null
           && formatDBTEX2ToggleButton.isSelected())
      {
         ioSettings.setFileFormat(FileFormatType.DBTEX, "2.0");
      }
      else if (
          ( formatDBTEX3ToggleButton != null
              && formatDBTEX3ToggleButton.isSelected() )
          ||
          ( formatDBTEXToggleButton != null
              && formatDBTEXToggleButton.isSelected() )
              )
      {
         ioSettings.setFileFormat(FileFormatType.DBTEX, "3.0");
      }
      else if (formatDTLTEX2ToggleButton != null
           && formatDTLTEX2ToggleButton.isSelected())
      {
         ioSettings.setFileFormat(FileFormatType.DTLTEX, "2.0");
      }
      else if (
          ( formatDTLTEX3ToggleButton != null
              && formatDTLTEX3ToggleButton.isSelected() )
          ||
          ( formatDTLTEXToggleButton != null
              && formatDTLTEXToggleButton.isSelected() )
              )
      {
         ioSettings.setFileFormat(FileFormatType.DTLTEX, "3.0");
      }
      else if (formatTeXToggleButton != null && formatTeXToggleButton.isSelected())
      {
         ioSettings.setFileFormat(FileFormatType.DTLTEX, "2.0");
      }
      else if (formatCSVToggleButton != null && formatCSVToggleButton.isSelected())
      {
         ioSettings.setFileFormat(FileFormatType.CSV);
         isCsvTsv = true;
      }
      else if (formatTSVToggleButton != null && formatTSVToggleButton.isSelected())
      {
         ioSettings.setFileFormat(FileFormatType.TSV);
         isCsvTsv = true;
      }

      ioSettings.setHeaderIncluded(isHeaderIncluded());

      if (trimElementBox != null)
      {
         ioSettings.setTrimElement(trimElementBox.isSelected());
      }

      if (isCsvTsv)
      {
         int sep = getSeparator();

         if (sep > 0)
         {
            ioSettings.setSeparator(sep);
         }

         int delim = getDelimiter();

         if (delim != -1)
         {
            ioSettings.setDelimiter(delim);
         }

         ioSettings.setCsvLiteral(isLiteralContent());

         ioSettings.setSkipLines(getSkipLines());

         CsvBlankOption csvBlankOption = getCsvBlankOption();

         if (csvBlankOption != null)
         {
            ioSettings.setCsvBlankOption(csvBlankOption);
         }

         EscapeCharsOption escapeCharsOption = getEscapeCharsOption();

         if (escapeCharsOption != null)
         {
            ioSettings.setEscapeCharsOption(escapeCharsOption);
         }

         AddDelimiterOption addDelimiterOption = getAddDelimiterOption();

         if (addDelimiterOption != null)
         {
            ioSettings.setAddDelimiterOption(addDelimiterOption);
         }
      }
   }

   public String getTeXOptionCode()
   {
      StringBuilder builder = new StringBuilder();

      builder.append(String.format("DTLsetup{%n"));

      if (trimElementBox != null && isRead() && isFileFormatSelected(
             DatatoolFileFormat.FILE_FORMAT_FLAG_TEX 
           | DatatoolFileFormat.FILE_FORMAT_ANY_DTLTEX 
           | DatatoolFileFormat.FILE_FORMAT_CSV_OR_TSV
          ))
      {
         builder.append(String.format("  new-value-trim={%s},%n",
           isTrimElementOn()));
      }

      builder.append(String.format("  io={%n"));

      getTeXOptionCode(builder);

      builder.append(String.format("%n  }%n}"));

      return builder.toString();
   }

   public void getTeXOptionCode(StringBuilder builder)
   {
      boolean isCsvTsv = false;

      if (isRead()
           && formatTeXToggleButton != null && formatTeXToggleButton.isSelected())
      {
         builder.append(String.format("  format=tex,%n"));
      }
      else if (formatDBTEX3ToggleButton != null
         && formatDBTEX3ToggleButton.isSelected())
      {
         builder.append(String.format("  format=dbtex-3,%n"));
      }
      else if (formatDBTEX2ToggleButton != null
         && formatDBTEX2ToggleButton.isSelected())
      {
         builder.append(String.format("  format=dbtex-2,%n"));
      }
      else if (formatDBTEXToggleButton != null
         && formatDBTEXToggleButton.isSelected())
      {
         builder.append(String.format("  format=dbtex,%n"));
      }
      else if (formatDTLTEX3ToggleButton != null
         && formatDTLTEX3ToggleButton.isSelected())
      {
         builder.append(String.format("  format=dtltex-3,%n"));
      }
      else if (formatDTLTEX2ToggleButton != null
         && formatDTLTEX2ToggleButton.isSelected())
      {
         builder.append(String.format("  format=dtltex-2,%n"));
      }
      else if (
        ( formatDTLTEXToggleButton != null
            && formatDTLTEXToggleButton.isSelected() )
      || 
         ( formatTeXToggleButton != null && formatTeXToggleButton.isSelected() )
        )
      {
         builder.append(String.format("  format=dtltex,%n"));
      }
      else if
        (
          (formatTSVToggleButton != null && formatTSVToggleButton.isSelected())
        ||
          (tabSeparatorButton != null && tabSeparatorButton.isSelected() )
        )
      {
         builder.append(String.format("  format=tsv,%n"));
         isCsvTsv = true;
      }
      else if (formatCSVToggleButton != null
         && formatCSVToggleButton.isSelected())
      {
         builder.append(String.format("  format=csv,%n"));

         if (csvSepField != null && csvSepField.isEnabled())
         {
            int csvSep = csvSepField.getValue();

            if (csvSep > 0)
            {
               builder.append("  separator=");
               builder.appendCodePoint(csvSep);
               builder.append(String.format(",%n"));
            }
         }

         isCsvTsv = true;
      }

      if (isCsvTsv)
      {
         int delim = getDelimiter();

         if (delim > 0)
         {
            builder.append("  delimiter=");
            builder.appendCodePoint(delim);
            builder.append(String.format(",%n"));
         }

         EscapeCharsOption escCharsOpt = getEscapeCharsOption();

         if (escCharsOpt != null)
         {
            builder.append(String.format(
               "   csv-escape-chars=%s,%n", escCharsOpt.getName()));
         }

         if (isRead())
         {
            builder.append(String.format("   csv-content=%s,%n",
               isLiteralContent() ? "literal" : "tex"));

            int skipLines = getSkipLines();

            if (skipLines > 0)
            {
               builder.append("   csv-skip-lines=");
               builder.append(skipLines);
               builder.append(String.format(",%n"));
            }

            CsvBlankOption csvBlankOption = getCsvBlankOption();

            if (csvBlankOption != null)
            {
               builder.append(String.format(
                 "   csv-blank=%s,%n", csvBlankOption.getName()));
            }
         }

         if (isWrite())
         {
            AddDelimiterOption addDelimOpt = getAddDelimiterOption();

            if (addDelimOpt != null)
            {
               builder.append(String.format(
                 "   add-delimiter=%s,%n", addDelimOpt.getName()));
            }
         }
      }

      if (!isHeaderIncluded())
      {
         builder.append(String.format("   no-header,%n"));
      }
   }

   public MessageHandler getMessageHandler()
   {
      return resources.getMessageHandler();
   }

   public DatatoolGuiResources getDatatoolGuiResources()
   {
      return resources;
   }

   public Window getOwner()
   {
      return owner;
   }

   Window owner;
   DatatoolGuiResources resources;
   String tagParentLabel;

   JToggleButton formatTeXToggleButton;
   JToggleButton formatDTLTEXToggleButton;
   JToggleButton formatDTLTEX2ToggleButton;
   JToggleButton formatDTLTEX3ToggleButton;
   JToggleButton formatDBTEXToggleButton;
   JToggleButton formatDBTEX2ToggleButton;
   JToggleButton formatDBTEX3ToggleButton;
   JToggleButton formatCSVToggleButton;
   JToggleButton formatTSVToggleButton;
   JToggleButton formatODSToggleButton;
   JToggleButton formatFODSToggleButton;
   JToggleButton formatXLSXToggleButton;
   JToggleButton formatXLSToggleButton;
   JToggleButton formatSQLToggleButton;

   int formatModifiers, ioModifiers, selectedFormatModifiers;
   boolean formatSingleSelection;

   JCheckBox csvIncHeaderBox, texIncHeaderBox, trimElementBox,
    stripSolnEnvBox, emptyToNullBox;

   JComponent fileFormatComp, csvSpreadComp, csvTsvOnlyComp, allNonTeXComp,
     texCardComp, nonTeXCardComp, cardComp;

   CardLayout cardLayout;

   JRadioButton csvSeparatorButton, tabSeparatorButton;
   CharField csvSepField;

   CharField csvDelimField;

   JRadioButton csvLiteralButton, csvTeXButton;

   SpinnerNumberModel skipLinesModel;

   JRadioButton csvBlankIgnoreButton, csvBlankEmptyButton, csvBlankEndButton;

   JRadioButton escapeCharsNoneButton, escapeCharsDoubleDelimButton,
    escapeCharsEscDelimButton, escapeCharsEscDelimBkslButton;

   JRadioButton addDelimiterAlwaysButton, addDelimiterDetectButton,
     addDelimiterNeverButton;

   JCheckBox strictQuotesBox;

   JComboBox<Charset> texEncodingBox, csvEncodingBox;

   JCheckBox mapTeXBox;
   JComponent texMappingsComp, literalSourceComp;
   TeXMapModel texMapModel;
   JButton removeMapButton, editMapButton;
   JTable texMapTable;

   SqlPanel sqlPanel;

   Vector<FileFormatSelectionChangeListener> fileFormatChangeListeners;

   /**
    * Read settings.
    */

   public static final int IO_IN = 1;

   /**
    * Write settings.
    */
   public static final int IO_OUT = 1 << 1;

   protected float defaultAlignmentX = 0f;
}
