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

public class IOSettingsPanel extends JPanel
  implements ActionListener
{
   public IOSettingsPanel(Window owner, DatatoolGuiResources resources,
      int formatModifiers, boolean formatSingleSelection)
   {
      this(owner, resources, null, IO_IN | IO_OUT,
       formatModifiers, formatSingleSelection, true);
   }

   public IOSettingsPanel(Window owner, DatatoolGuiResources resources,
      String tagParentLabel, int ioModifiers,
      int formatModifiers, boolean formatSingleSelection,
      boolean addTrim)
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

      init(addTrim);
   }

   protected void init(boolean addTrim)
   {
      initFormatButtons();

      if (addTrim)
      {
         trimElementBox = createJCheckBox("element", "trim");

         add(trimElementBox);
      }

      if (
           ((formatModifiers | FILE_FORMAT_ANY_TEX)
               & FILE_FORMAT_ANY_TEX) == FILE_FORMAT_ANY_TEX
         )
      {
         initTeXCardComp();
      }

      if (
           ((formatModifiers | FILE_FORMAT_ANY_NON_TEX)
               & FILE_FORMAT_ANY_NON_TEX) == FILE_FORMAT_ANY_NON_TEX
         )
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

      if ((formatModifiers & FILE_FORMAT_FLAG_TEX) == FILE_FORMAT_FLAG_TEX)
      {
         formatTeXToggleButton = createFormatButton(
           fileFormatComp, "tex", formatBtnGrp);
         lastButton = formatTeXToggleButton;
         numFormats++;
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_DTLTEX) == FILE_FORMAT_FLAG_DTLTEX)
      {
         formatDTLTEXToggleButton = createFormatButton(
           fileFormatComp, "dtltex", formatBtnGrp);
         lastButton = formatDTLTEXToggleButton;
         numFormats++;
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_DTLTEX2) == FILE_FORMAT_FLAG_DTLTEX2)
      {
         formatDTLTEX2ToggleButton = createFormatButton(
           fileFormatComp, "dtltex-2", formatBtnGrp);
         lastButton = formatDTLTEX2ToggleButton;
         numFormats++;
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_DTLTEX3) == FILE_FORMAT_FLAG_DTLTEX3)
      {
         formatDTLTEX3ToggleButton = createFormatButton(
           fileFormatComp, "dtltex-3", formatBtnGrp);
         lastButton = formatDTLTEX3ToggleButton;
         numFormats++;
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_DBTEX) == FILE_FORMAT_FLAG_DBTEX)
      {
         formatDBTEXToggleButton = createFormatButton(
           fileFormatComp, "dbtex", formatBtnGrp);
         lastButton = formatDBTEXToggleButton;
         numFormats++;
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_DBTEX2) == FILE_FORMAT_FLAG_DBTEX2)
      {
         formatDBTEX2ToggleButton = createFormatButton(
           fileFormatComp, "dbtex-2", formatBtnGrp);
         lastButton = formatDBTEX2ToggleButton;
         numFormats++;
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_DBTEX3) == FILE_FORMAT_FLAG_DBTEX3)
      {
         formatDBTEX3ToggleButton = createFormatButton(
           fileFormatComp, "dbtex-3", formatBtnGrp);
         lastButton = formatDBTEX3ToggleButton;
         numFormats++;
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_CSV) == FILE_FORMAT_FLAG_CSV)
      {
         formatCSVToggleButton = createFormatButton(
           fileFormatComp, "csv", formatBtnGrp);
         lastButton = formatCSVToggleButton;
         numFormats++;
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_TSV) == FILE_FORMAT_FLAG_TSV)
      {
         formatTSVToggleButton = createFormatButton(
           fileFormatComp, "tsv", formatBtnGrp);
         lastButton = formatTSVToggleButton;
         numFormats++;
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_ODS) == FILE_FORMAT_FLAG_ODS)
      {
         formatODSToggleButton = createFormatButton(
           fileFormatComp, "ods", formatBtnGrp);
         lastButton = formatODSToggleButton;
         numFormats++;
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_FODS) == FILE_FORMAT_FLAG_FODS)
      {
         formatFODSToggleButton = createFormatButton(
           fileFormatComp, "fods", formatBtnGrp);
         lastButton = formatFODSToggleButton;
         numFormats++;
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_XLSX) == FILE_FORMAT_FLAG_XLSX)
      {
         formatXLSXToggleButton = createFormatButton(
           fileFormatComp, "xlsx", formatBtnGrp);
         lastButton = formatXLSXToggleButton;
         numFormats++;
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_XLS) == FILE_FORMAT_FLAG_XLS)
      {
         formatXLSToggleButton = createFormatButton(
           fileFormatComp, "xls", formatBtnGrp);
         lastButton = formatXLSToggleButton;
         numFormats++;
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_SQL) == FILE_FORMAT_FLAG_SQL)
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

         texCardComp.add(texIncHeaderBox);
      }
   }

   protected void initNonTeXComp()
   {
      nonTeXCardComp = createVerticalBox();

      boolean isCsvTsv = (((formatModifiers | FILE_FORMAT_CSV_OR_TSV)
               & FILE_FORMAT_CSV_OR_TSV) == FILE_FORMAT_CSV_OR_TSV);

      boolean isSpread = (((formatModifiers | FILE_FORMAT_ANY_SPREADSHEET)
               & FILE_FORMAT_ANY_SPREADSHEET) == FILE_FORMAT_ANY_SPREADSHEET);

      boolean isSql =
         ((formatModifiers & FILE_FORMAT_FLAG_SQL) == FILE_FORMAT_FLAG_SQL);

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
      if (isFileFormatSelected(FILE_FORMAT_ANY_TEX))
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

         if (isFileFormatSelected(FILE_FORMAT_CSV_OR_TSV))
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

         if (isFileFormatSelected(FILE_FORMAT_CSV_OR_TSV
              | FILE_FORMAT_ANY_SPREADSHEET))
         {
            csvSpreadComp.setVisible(true);
         }
         else if (csvSpreadComp != null)
         {
            csvSpreadComp.setVisible(false);
         }

         if (sqlPanel != null)
         {
            sqlPanel.setVisible(isFileFormatSelected(FILE_FORMAT_FLAG_SQL));
         }
      }
   }

   protected void updateFormatModifiers()
   {
      formatModifiers = 0;

      if (formatSingleSelection)
      {
         if (formatTeXToggleButton != null
               && formatTeXToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_ANY_TEX;
         }
         else if (formatDTLTEXToggleButton != null
               && formatDTLTEXToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_FLAG_DTLTEX
                            | FILE_FORMAT_FLAG_DTLTEX2
                            | FILE_FORMAT_FLAG_DTLTEX3;
         }
         else if (formatDTLTEX2ToggleButton != null
               && formatDTLTEX2ToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_FLAG_DTLTEX2;
         }
         else if (formatDTLTEX3ToggleButton != null
               && formatDTLTEX3ToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_FLAG_DTLTEX3;
         }
         else if (formatDBTEXToggleButton != null
               && formatDBTEXToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_FLAG_DBTEX
                            | FILE_FORMAT_FLAG_DBTEX2
                            | FILE_FORMAT_FLAG_DBTEX3;
         }
         else if (formatDBTEX2ToggleButton != null
               && formatDBTEX2ToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_FLAG_DBTEX2;
         }
         else if (formatDBTEX3ToggleButton != null
               && formatDBTEX3ToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_FLAG_DBTEX3;
         }
         else if (formatCSVToggleButton != null
               && formatCSVToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_FLAG_CSV;
         }
         else if (formatTSVToggleButton != null
               && formatTSVToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_FLAG_TSV;
         }
         else if (formatODSToggleButton != null
               && formatODSToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_FLAG_ODS;
         }
         else if (formatFODSToggleButton != null
               && formatFODSToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_FLAG_FODS;
         }
         else if (formatXLSToggleButton != null
               && formatXLSToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_FLAG_XLS;
         }
         else if (formatXLSXToggleButton != null
               && formatXLSXToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_FLAG_XLSX;
         }
         else if (formatSQLToggleButton != null
               && formatSQLToggleButton.isSelected())
         {
            formatModifiers = FILE_FORMAT_FLAG_SQL;
         }
      }
      else
      {
         if (formatTeXToggleButton != null
               && formatTeXToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_ANY_TEX;
         }

         if (formatDTLTEXToggleButton != null
               && formatDTLTEXToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_FLAG_DTLTEX
                            | FILE_FORMAT_FLAG_DTLTEX2
                            | FILE_FORMAT_FLAG_DTLTEX3;
         }

         if (formatDTLTEX2ToggleButton != null
               && formatDTLTEX2ToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_FLAG_DTLTEX2;
         }

         if (formatDTLTEX3ToggleButton != null
               && formatDTLTEX3ToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_FLAG_DTLTEX3;
         }

         if (formatDBTEXToggleButton != null
               && formatDBTEXToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_FLAG_DBTEX
                            | FILE_FORMAT_FLAG_DBTEX2
                            | FILE_FORMAT_FLAG_DBTEX3;
         }

         if (formatDBTEX2ToggleButton != null
               && formatDBTEX2ToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_FLAG_DBTEX2;
         }

         if (formatDBTEX3ToggleButton != null
               && formatDBTEX3ToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_FLAG_DBTEX3;
         }

         if (formatCSVToggleButton != null
               && formatCSVToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_FLAG_CSV;
         }

         if (formatTSVToggleButton != null
               && formatTSVToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_FLAG_TSV;
         }

         if (formatODSToggleButton != null
               && formatODSToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_FLAG_ODS;
         }

         if (formatFODSToggleButton != null
               && formatFODSToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_FLAG_FODS;
         }

         if (formatXLSToggleButton != null
               && formatXLSToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_FLAG_XLS;
         }

         if (formatXLSXToggleButton != null
               && formatXLSXToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_FLAG_XLSX;
         }

         if (formatSQLToggleButton != null
               && formatSQLToggleButton.isSelected())
         {
            formatModifiers |= FILE_FORMAT_FLAG_SQL;
         }
      }
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if ("format".equals(action))
      {
         int oldModifiers = formatModifiers;
         updateFormatModifiers();

         formatChanged();

         if (oldModifiers != formatModifiers)
         {
            fireFileFormatChange(
              new FileFormatSelectionChangeEvent(this,
                     oldModifiers, formatModifiers));
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
      if (isFileFormatSelected(FILE_FORMAT_ANY_TEX))
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

   public boolean isFileFormatSelected(int formatModifiers)
   {
      if ((formatModifiers & FILE_FORMAT_FLAG_TEX) == FILE_FORMAT_FLAG_TEX)
      {
         if (formatTeXToggleButton != null
          && formatTeXToggleButton.isSelected())
         {
            return true;
         }
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_DTLTEX) == FILE_FORMAT_FLAG_DTLTEX)
      {
         if (formatDTLTEXToggleButton != null
          && formatDTLTEXToggleButton.isSelected())
         {
            return true;
         }
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_DTLTEX2) == FILE_FORMAT_FLAG_DTLTEX2)
      {
         if (formatDTLTEX2ToggleButton != null
          && formatDTLTEX2ToggleButton.isSelected())
         {
            return true;
         }
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_DTLTEX3) == FILE_FORMAT_FLAG_DTLTEX3)
      {
         if (formatDTLTEX3ToggleButton != null
          && formatDTLTEX3ToggleButton.isSelected())
         {
            return true;
         }
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_DBTEX) == FILE_FORMAT_FLAG_DBTEX)
      {
         if (formatDBTEXToggleButton != null
          && formatDBTEXToggleButton.isSelected())
         {
            return true;
         }
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_DBTEX2) == FILE_FORMAT_FLAG_DBTEX2)
      {
         if (formatDBTEX2ToggleButton != null
          && formatDBTEX2ToggleButton.isSelected())
         {
            return true;
         }
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_DBTEX3) == FILE_FORMAT_FLAG_DBTEX3)
      {
         if (formatDBTEX3ToggleButton != null
          && formatDBTEX3ToggleButton.isSelected())
         {
            return true;
         }
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_CSV) == FILE_FORMAT_FLAG_CSV)
      {
         if (formatCSVToggleButton != null
          && formatCSVToggleButton.isSelected())
         {
            return true;
         }
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_TSV) == FILE_FORMAT_FLAG_TSV)
      {
         if (formatTSVToggleButton != null
          && formatTSVToggleButton.isSelected())
         {
            return true;
         }
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_ODS) == FILE_FORMAT_FLAG_ODS)
      {
         if (formatODSToggleButton != null
          && formatODSToggleButton.isSelected())
         {
            return true;
         }
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_FODS) == FILE_FORMAT_FLAG_FODS)
      {
         if (formatFODSToggleButton != null
          && formatFODSToggleButton.isSelected())
         {
            return true;
         }
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_XLS) == FILE_FORMAT_FLAG_XLS)
      {
         if (formatXLSToggleButton != null
          && formatXLSToggleButton.isSelected())
         {
            return true;
         }
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_XLSX) == FILE_FORMAT_FLAG_XLSX)
      {
         if (formatXLSXToggleButton != null
          && formatXLSXToggleButton.isSelected())
         {
            return true;
         }
      }

      if ((formatModifiers & FILE_FORMAT_FLAG_SQL) == FILE_FORMAT_FLAG_SQL)
      {
         if (formatSQLToggleButton != null
          && formatSQLToggleButton.isSelected())
         {
            return true;
         }
      }

      return false;
   }

   public void setSelectedFileFormat(int formatModifiers)
   {
      int oldModifiers = this.formatModifiers;
      this.formatModifiers = formatModifiers;

      if (formatTeXToggleButton != null)
      {
         formatTeXToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_TEX) == FILE_FORMAT_FLAG_TEX);
      }

      if (formatDBTEXToggleButton != null)
      {
         formatDBTEXToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_DBTEX) == FILE_FORMAT_FLAG_DBTEX);
      }

      if (formatDBTEX2ToggleButton != null)
      {
         formatDBTEX2ToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_DBTEX2) == FILE_FORMAT_FLAG_DBTEX2);
      }

      if (formatDBTEX3ToggleButton != null)
      {
         formatDBTEX3ToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_DBTEX3) == FILE_FORMAT_FLAG_DBTEX3);
      }

      if (formatDTLTEXToggleButton != null)
      {
         formatDTLTEXToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_DTLTEX) == FILE_FORMAT_FLAG_DTLTEX);
      }

      if (formatDTLTEX2ToggleButton != null)
      {
         formatDTLTEX2ToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_DTLTEX2) == FILE_FORMAT_FLAG_DTLTEX2);
      }

      if (formatDTLTEX3ToggleButton != null)
      {
         formatDTLTEX3ToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_DTLTEX3) == FILE_FORMAT_FLAG_DTLTEX3);
      }

      if (formatCSVToggleButton != null)
      {
         formatCSVToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_CSV) == FILE_FORMAT_FLAG_CSV);
      }

      if (formatTSVToggleButton != null)
      {
         formatTSVToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_TSV) == FILE_FORMAT_FLAG_TSV);
      }

      if (formatODSToggleButton != null)
      {
         formatODSToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_ODS) == FILE_FORMAT_FLAG_ODS);
      }

      if (formatFODSToggleButton != null)
      {
         formatFODSToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_FODS) == FILE_FORMAT_FLAG_FODS);
      }

      if (formatXLSToggleButton != null)
      {
         formatXLSToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_XLS) == FILE_FORMAT_FLAG_XLS);
      }

      if (formatXLSXToggleButton != null)
      {
         formatXLSXToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_XLSX) == FILE_FORMAT_FLAG_XLSX);
      }

      if (formatSQLToggleButton != null)
      {
         formatSQLToggleButton.setSelected(
          (formatModifiers & FILE_FORMAT_FLAG_SQL) == FILE_FORMAT_FLAG_SQL);
      }

      formatChanged();

      if (oldModifiers != formatModifiers)
      {
         fireFileFormatChange(
          new FileFormatSelectionChangeEvent(this, oldModifiers, formatModifiers));
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

   public boolean isCsvTsvOnly()
   {
      return (formatModifiers | FILE_FORMAT_CSV_OR_TSV)
       == FILE_FORMAT_CSV_OR_TSV;
   }

   public boolean isTeXOnly()
   {
      return (formatModifiers | FILE_FORMAT_ANY_TEX)
        == FILE_FORMAT_ANY_TEX;
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
         String encoding = settings.getCsvEncoding();
      
         if (encoding == null)
         {
            csvEncodingBox.setSelectedItem(Charset.defaultCharset());
         }
         else
         {
            csvEncodingBox.setSelectedItem(Charset.forName(encoding));
         }
      }

      if (texEncodingBox != null)
      {
         String encoding = settings.getTeXEncoding();

         if (encoding == null)
         {
            texEncodingBox.setSelectedItem(Charset.defaultCharset());
         }
         else
         {
            texEncodingBox.setSelectedItem(Charset.forName(encoding));
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
              setSelectedFileFormat(FILE_FORMAT_FLAG_TEX
                | FILE_FORMAT_FLAG_DBTEX | FILE_FORMAT_FLAG_DBTEX3);
           }
           else
           {
              setSelectedFileFormat(FILE_FORMAT_FLAG_TEX
                | FILE_FORMAT_FLAG_DBTEX | FILE_FORMAT_FLAG_DBTEX2);
           }
         break;
         case DTLTEX:
           if (isV3)
           {
              setSelectedFileFormat(FILE_FORMAT_FLAG_TEX
                | FILE_FORMAT_FLAG_DTLTEX | FILE_FORMAT_FLAG_DTLTEX3);
           }
           else
           {
              setSelectedFileFormat(FILE_FORMAT_FLAG_TEX
                | FILE_FORMAT_FLAG_DTLTEX | FILE_FORMAT_FLAG_DTLTEX2);
           }
         break;
         case CSV:
            setSelectedFileFormat(FILE_FORMAT_FLAG_CSV);
         break;
         case TSV:
            setSelectedFileFormat(FILE_FORMAT_FLAG_TSV);
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
             FILE_FORMAT_FLAG_TEX 
           | FILE_FORMAT_FLAG_DTLTEX 
           | FILE_FORMAT_FLAG_DTLTEX2 
           | FILE_FORMAT_FLAG_DTLTEX3 
           | FILE_FORMAT_CSV_OR_TSV
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

   int formatModifiers, ioModifiers;
   boolean formatSingleSelection;

   JCheckBox csvIncHeaderBox, texIncHeaderBox, trimElementBox;

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
    * Input file contains LaTeX code. Not applicable for output
    * files.
    */
   public static final int FILE_FORMAT_FLAG_TEX = 1;

   /**
    * Input file contains LaTeX code in special DTLTEX format. Parse
    * header to determine version and encoding. Output file 
    * latest DTLTEX version.
    */
   public static final int FILE_FORMAT_FLAG_DTLTEX = 1 << 1;

   /**
    * Input file contains LaTeX code in special DTLTEX format. Parse
    * header to determine encoding. Output file format
    * DTLTEX version 2.0.
    */
   public static final int FILE_FORMAT_FLAG_DTLTEX2 = 1 << 2;

   /**
    * Input file contains LaTeX code in special DTLTEX format. Parse
    * header to determine encoding. Output file format
    * DTLTEX version 3.0.
    */
   public static final int FILE_FORMAT_FLAG_DTLTEX3 = 1 << 3;

   /**
    * Input file contains LaTeX code in DBTEX format. Parse
    * header to determine version and encoding. Output file 
    * latest DBTEX version.
    */
   public static final int FILE_FORMAT_FLAG_DBTEX = 1 << 4;

   /**
    * Input file contains LaTeX code in special DBTEX format. Parse
    * header to determine encoding. Output file format
    * DBTEX version 2.0.
    */
   public static final int FILE_FORMAT_FLAG_DBTEX2 = 1 << 5;

   /**
    * Input file contains LaTeX code in special DBTEX format. Parse
    * header to determine encoding. Output file format
    * DBTEX version 3.0.
    */
   public static final int FILE_FORMAT_FLAG_DBTEX3 = 1 << 6;

   /**
    * CSV file format.
    */
   public static final int FILE_FORMAT_FLAG_CSV = 1 << 7;

   /**
    * TSV file format (tab separator).
    */
   public static final int FILE_FORMAT_FLAG_TSV = 1 << 8;

   /**
    * ODS (zip) file format.
    */
   public static final int FILE_FORMAT_FLAG_ODS = 1 << 9;

   /**
    * FODS (flat xml) file format.
    */
   public static final int FILE_FORMAT_FLAG_FODS = 1 << 10;

   /**
    * XLSX (Excel xml) file format.
    */
   public static final int FILE_FORMAT_FLAG_XLSX = 1 << 11;

   /**
    * XLS (Excel) file format.
    */
   public static final int FILE_FORMAT_FLAG_XLS = 1 << 12;

   /**
    * SQL format.
    */
   public static final int FILE_FORMAT_FLAG_SQL = 1 << 13;

   /**
    * Either CSV or TSV.
    */
   public static final int FILE_FORMAT_CSV_OR_TSV =
     FILE_FORMAT_FLAG_CSV | FILE_FORMAT_FLAG_TSV;

   /**
    * Any TeX format.
    */
   public static final int FILE_FORMAT_ANY_TEX =
     FILE_FORMAT_FLAG_TEX
   | FILE_FORMAT_FLAG_DTLTEX
   | FILE_FORMAT_FLAG_DTLTEX2
   | FILE_FORMAT_FLAG_DTLTEX3
   | FILE_FORMAT_FLAG_DBTEX
   | FILE_FORMAT_FLAG_DBTEX2
   | FILE_FORMAT_FLAG_DBTEX3;

   /**
    * Any spreadsheet format.
    */
   public static final int FILE_FORMAT_ANY_SPREADSHEET =
     FILE_FORMAT_FLAG_ODS
   | FILE_FORMAT_FLAG_FODS
   | FILE_FORMAT_FLAG_XLSX
   | FILE_FORMAT_FLAG_XLS;

   /**
    * Any non-TeX format.
    */
   public static final int FILE_FORMAT_ANY_NON_TEX =
     FILE_FORMAT_CSV_OR_TSV
   | FILE_FORMAT_ANY_SPREADSHEET
   | FILE_FORMAT_FLAG_SQL;

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
