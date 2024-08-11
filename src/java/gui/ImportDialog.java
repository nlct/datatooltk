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

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;

import java.nio.charset.Charset;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;

import com.dickimawbooks.texparserlib.latex.datatool.IOSettings;
import com.dickimawbooks.texjavahelplib.InvalidSyntaxException;

import com.dickimawbooks.datatooltk.*;
import com.dickimawbooks.datatooltk.io.*;

/**
 * Dialog box for importing files.
 */
public class ImportDialog extends JDialog
  implements ActionListener,
  FileSelectionChangeListener,
  FileFormatSelectionChangeListener
{
   public ImportDialog(DatatoolGUI gui)
   {
      this(gui, "import",
        DatatoolFileFormat.FILE_FORMAT_FLAG_TEX
      | DatatoolFileFormat.FILE_FORMAT_ANY_NON_TEX,
        true, true,
        DatatoolFileFormat.FILE_FORMAT_FLAG_CSV);
   }

   public ImportDialog(DatatoolGUI gui, String tagPrefix, int formatModifiers,
     boolean addTrim, boolean addEmptyToNull, int initialFormat)
   {
      super(gui, gui.getMessageHandler().getLabel(tagPrefix+".title"), true);
      this.gui = gui;

      MessageHandler messageHandler = gui.getMessageHandler();

      fileChooser = new JFileChooser();
      fileChooser.setCurrentDirectory(gui.getCurrentChooserDirectory());

      texFilter = new TeXFileFilter(messageHandler);
      csvtxtFilter = new CsvTxtFileFilter(messageHandler);
      spreadFilter = new SpreadSheetFilter(messageHandler);

      allFilter = fileChooser.getAcceptAllFileFilter();
      fileChooser.removeChoosableFileFilter(allFilter);

      fileChooser.addChoosableFileFilter(texFilter);
      fileChooser.addChoosableFileFilter(csvtxtFilter);
      fileChooser.addChoosableFileFilter(spreadFilter);
      fileChooser.addChoosableFileFilter(allFilter);

      DatatoolGuiResources resources = gui.getResources();

      settingsPanel = new IOSettingsPanel(this, resources, tagPrefix,
        IOSettingsPanel.IO_IN, formatModifiers, true, addTrim, addEmptyToNull);

      getContentPane().add(new JScrollPane(settingsPanel), BorderLayout.CENTER);

      fileField = new FileField(messageHandler, this, null, fileChooser,
        JFileChooser.FILES_ONLY, tagPrefix+".file");

      fileField.addFileSelectionChangeListener(this);

      if ( DatatoolFileFormat.isSQL(formatModifiers))
      {
         settingsPanel.addFileFormatSelectionChangeListener(this);

         selectComp = Box.createHorizontalBox();
         selectComp.setAlignmentX(0);

         selectField = new JTextField(gui.getMessageHandler().getLabel(
           "message.importsql.select_placeholder"));
         selectComp.add(resources.createJLabel(tagPrefix+".select", selectField));
         selectComp.add(selectField);

         fileSelectCardLayout = new CardLayout();
         fileSelectCardComp = new JPanel(fileSelectCardLayout);
         fileSelectCardComp.setAlignmentX(0);

         fileSelectCardComp.add(fileField, "file");
         fileSelectCardComp.add(selectComp, "select");

         getContentPane().add(fileSelectCardComp, BorderLayout.NORTH);
      }
      else
      {
         getContentPane().add(fileField, BorderLayout.NORTH);
      }

      getContentPane().add(
        resources.createDialogOkayCancelHelpPanel(this, this, gui, tagPrefix),
        BorderLayout.SOUTH);

      settingsPanel.setSelectedFileFormat(initialFormat);

      pack();

      setLocationRelativeTo(null);
   }

   @Override
   public void fileFormatSelectionChanged(FileFormatSelectionChangeEvent evt)
   {
      int modifiers = evt.getNewModifiers();

      if ( DatatoolFileFormat.isSQLOnly(modifiers))
      {
         fileSelectCardLayout.show(fileSelectCardComp, "select");
      }
      else
      {
         if ( DatatoolFileFormat.isSQL(evt.getOldModifiers()) )
         {
            fileSelectCardLayout.show(fileSelectCardComp, "file");
         }

         if ( DatatoolFileFormat.isCsvOrTsvOnly(modifiers) )
         {
            fileChooser.setFileFilter(csvtxtFilter);
         }
         else if ( DatatoolFileFormat.isSpreadSheetOnly(modifiers) )
         {
            fileChooser.setFileFilter(spreadFilter);
         }
         else if ( DatatoolFileFormat.isTeXOnly(modifiers) )
         {
            fileChooser.setFileFilter(texFilter);
         }
         else
         {
            fileChooser.setFileFilter(allFilter);
         }

      }
   }

   @Override
   public void fileSelectionChanged(FileSelectionChangeEvent evt)
   {
      String newFile = evt.getNewFilename();

      if (!newFile.isEmpty() && !newFile.equals(evt.getOldFilename()))
      {
         try
         {
            DatatoolFileFormat newFmt = DatatoolFileFormat.valueOf(
              gui.getMessageHandler(), new File(newFile));

            int format = newFmt.getFileFormat();
            Charset newCharset = newFmt.getEncoding();

            if (format != 0)
            {
               settingsPanel.setSelectedFileFormat(format);

               if (newCharset != null)
               {
                  if (newFmt.isAnyTeX())
                  {
                     settingsPanel.setTeXEncoding(newCharset);
                  }
                  else if (newFmt.isCsvOrTsv())
                  {
                     settingsPanel.setCsvEncoding(newCharset);
                  }
               }
            }
         }
         catch (IOException e)
         {
            gui.getMessageHandler().error(this, e);
         }
      }
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if ("okay".equals(action))
      {
         try
         {
            okay();
         }
         catch (InvalidSyntaxException e)
         {
            gui.getMessageHandler().error(this, e.getMessage());
         }
         catch (Throwable e)
         {
            gui.getMessageHandler().error(this, e);
         }
      }
      else if ("cancel".equals(action))
      {
         setVisible(false);
      }
      else
      {
         gui.getMessageHandler().debug("Unknown action '"+action+"'");
      }
   }

   protected void okay() throws IOException
   {
      DatatoolSettings settings = gui.getSettings();
      MessageHandler messageHandler = gui.getMessageHandler();

      boolean isSql = (settingsPanel.isFileFormatSelected(
          DatatoolFileFormat.FILE_FORMAT_FLAG_SQL));

      String filename = "";
      String select = "";
      String source = "";

      if (isSql)
      {
         select = selectField.getText().trim();

         if (select.isEmpty())
         {
            selectField.requestFocusInWindow();

            throw new InvalidSyntaxException(messageHandler.getLabel(
              "error.missing_select"));
         }

         source = "SELECT "+select;
      }
      else
      {
         filename = fileField.getFileName();

         if (filename.isEmpty())
         {
            fileField.requestFocusInWindow();

            throw new InvalidSyntaxException(messageHandler.getLabel(
              "error.missing_filename"));
         }

         source = filename;
      }

      ImportSettings importSettings;

      boolean saveSelected = true;// TODO??

      if (saveSelected)
      {
         importSettings = settings.getImportSettings();
      }
      else
      {
         // TODO?? (map and wipe password would need to be moved)
         importSettings = new ImportSettings(settings);
      }

      settingsPanel.applyTo(importSettings);

      LoadSettings loadSettings = new LoadSettings(settings);
      loadSettings.setImportSettings(importSettings);

      DatatoolImport imp;

      if (settingsPanel.isFileFormatSelected(
           DatatoolFileFormat.FILE_FORMAT_CSV_OR_TSV))
      {
         imp = new DatatoolCsv(settings);
      }
      else if (isSql)
      {
         imp = new DatatoolSql(settings);
      }
      else if (settingsPanel.isFileFormatSelected(
           DatatoolFileFormat.FILE_FORMAT_FLAG_ODS))
      {
         imp = new DatatoolOpenDoc(settings, false);
      }
      else if (settingsPanel.isFileFormatSelected(
           DatatoolFileFormat.FILE_FORMAT_FLAG_FODS))
      {
         imp = new DatatoolOpenDoc(settings, true);
      }
      else if (settingsPanel.isFileFormatSelected(
           DatatoolFileFormat.FILE_FORMAT_FLAG_XLS))
      {
         imp = new DatatoolExcel(settings);
      }
      else if (settingsPanel.isFileFormatSelected(
           DatatoolFileFormat.FILE_FORMAT_FLAG_XLSX))
      {
         imp = new DatatoolOfficeOpenXML(settings);
      }
      else
      {
         imp = new DatatoolTeX(settings);
      }

      loadSettings.setDataImport(imp, source);

      setVisible(false);

      DatatoolFileLoader loader = new DatatoolFileLoader(gui, loadSettings);
      loader.execute();
   }

   public void display()
   {
      DatatoolSettings settings = gui.getSettings();
      settingsPanel.resetFrom(settings.getImportSettings());

      fileField.setFileName("");

      setVisible(true);
   }

   DatatoolGUI gui;
   IOSettingsPanel settingsPanel;
   FileField fileField;
   JTextField selectField;

   JFileChooser fileChooser;

   private FileFilter texFilter, csvtxtFilter, spreadFilter, allFilter;

   CardLayout fileSelectCardLayout;
   JComponent fileSelectCardComp, selectComp;

}
