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

import java.nio.charset.StandardCharsets;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import com.dickimawbooks.texparserlib.latex.datatool.IOSettings;

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
   public ImportDialog(DatatoolGUI gui, JFileChooser fileChooser)
   {
      this(gui, "import",
        IOSettingsPanel.FILE_FORMAT_FLAG_TEX
      | IOSettingsPanel.FILE_FORMAT_ANY_NON_TEX,
        true,
        fileChooser, IOSettingsPanel.FILE_FORMAT_FLAG_CSV);
   }

   public ImportDialog(DatatoolGUI gui, String tagPrefix, int formatModifiers,
     boolean addTrim, JFileChooser fileChooser, int initialFormat)
   {
      super(gui, gui.getMessageHandler().getLabel(tagPrefix+".title"), true);
      this.gui = gui;

      DatatoolGuiResources resources = gui.getResources();

      settingsPanel = new IOSettingsPanel(this, resources, tagPrefix,
        IOSettingsPanel.IO_IN, formatModifiers, true, addTrim);

      getContentPane().add(new JScrollPane(settingsPanel), BorderLayout.CENTER);

      fileField = new FileField(gui.getMessageHandler(), this, null, fileChooser,
        JFileChooser.FILES_ONLY, tagPrefix+".file");

      fileField.addFileSelectionChangeListener(this);

      if (
           ( formatModifiers & IOSettingsPanel.FILE_FORMAT_FLAG_SQL )
             == IOSettingsPanel.FILE_FORMAT_FLAG_SQL
         )
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
      if (
            (evt.getNewModifiers() & IOSettingsPanel.FILE_FORMAT_FLAG_SQL)
              == IOSettingsPanel.FILE_FORMAT_FLAG_SQL
         )
      {
         fileSelectCardLayout.show(fileSelectCardComp, "select");
      }
      else if (
            (evt.getOldModifiers() & IOSettingsPanel.FILE_FORMAT_FLAG_SQL)
              == IOSettingsPanel.FILE_FORMAT_FLAG_SQL
         )
      {
         fileSelectCardLayout.show(fileSelectCardComp, "file");
      }
   }

   @Override
   public void fileSelectionChanged(FileSelectionChangeEvent evt)
   {
      String newFile = evt.getNewFilename();

      if (!newFile.isEmpty())
      {
         String oldFile = evt.getOldFilename();
         String oldExt = "";
         String newExt = "";

         int idx = newFile.lastIndexOf(".");

         if (idx > 0)
         {
            newExt = newFile.substring(idx+1).toLowerCase();
         }

         if (!oldFile.isEmpty())
         {
            idx = oldFile.lastIndexOf(".");

            if (idx > 0)
            {
               oldExt = oldFile.substring(idx+1).toLowerCase();
            }
         }

         if (!newExt.equals(oldExt))
         {
            int modifiers = 0;

            if (newExt.endsWith("tex") || newExt.equals("ltx"))
            {
               modifiers = IOSettingsPanel.FILE_FORMAT_ANY_TEX;
            }
            else if (newExt.equals("tsv"))
            {
               modifiers = IOSettingsPanel.FILE_FORMAT_FLAG_TSV;
            }
            else
            {
               File file = new File(newFile);

               if (file.exists())
               {
                  try
                  {
                     modifiers = probeFile(file);
                  }
                  catch (IOException e)
                  {
                     gui.getMessageHandler().error(this, e);
                  }
               }
            }

            if (modifiers != 0)
            {
               settingsPanel.setSelectedFileFormat(modifiers);
            }
         }
      }
   }

   protected int probeFile(File file)
    throws IOException
   {
      MessageHandler messageHandler = gui.getMessageHandler();

      int modifiers = 0;
      byte[] buffer = new byte[256];

      FileInputStream in = null;

      try
      {
         in = new FileInputStream(file);

         int n = in.read(buffer);

         if (n > 0)
         {
            if (startsWith(buffer, BOM, 0, n))
            {
               if (buffer[BOM.length] == '%')
               {
                  modifiers = IOSettingsPanel.FILE_FORMAT_ANY_TEX;
                  settingsPanel.setTeXEncoding(StandardCharsets.UTF_8);
               }
               else if (contains(buffer, (byte)'\t', BOM.length, n))
               {
                  modifiers = IOSettingsPanel.FILE_FORMAT_FLAG_TSV;
                  settingsPanel.setCsvEncoding(StandardCharsets.UTF_8);
               }
               else
               {
                  modifiers = IOSettingsPanel.FILE_FORMAT_FLAG_CSV;
                  settingsPanel.setCsvEncoding(StandardCharsets.UTF_8);
               }
            }
            else if (startsWith(buffer, ZIP_MARKER, 0, n))
            {
               if (startsWith(buffer, MIMETYPE_MARKER, 30, n))
               {
                  if (startsWith(buffer, ODS_MIMETYPE, 38, n))
                  {
                     modifiers = IOSettingsPanel.FILE_FORMAT_FLAG_ODS;
                  }
                  else if (startsWith(buffer, XLSX_MIMETYPE, 38, n))
                  {
                     // xlsx doesn't seem to include mimetype but
                     // include check anyway
                     modifiers = IOSettingsPanel.FILE_FORMAT_FLAG_XLSX;
                  }
                  else
                  {
                     messageHandler.error(this, 
                      messageHandler.getLabel("error.unsupported_mimetype"));
                  }
               }
               else
               {
                  // assume xlsx
                  modifiers = IOSettingsPanel.FILE_FORMAT_FLAG_XLSX;
               }
            }
            else if (startsWith(buffer, XML_MARKER, 0, n))
            {
               if (contains(buffer, OFFICE_DOCUMENT_MARKER, 6, n))
               {
                  modifiers = IOSettingsPanel.FILE_FORMAT_FLAG_FODS;
               }
               else
               {
                  messageHandler.error(this, 
                   messageHandler.getLabel("error.unsupported_xml"));
               }
            }
            else if (startsWith(buffer, XLS_MARKER, 0, n))
            {
               modifiers = IOSettingsPanel.FILE_FORMAT_FLAG_XLS;
            }
            else if (buffer[0] == '%')
            {
               modifiers = IOSettingsPanel.FILE_FORMAT_ANY_TEX;
            }
            else if (contains(buffer, (byte)'\t', 0, n))
            {
               modifiers = IOSettingsPanel.FILE_FORMAT_FLAG_TSV;
            }
            else if (contains(buffer, (byte)settingsPanel.getSeparator(), 0, n)
                   ||contains(buffer, (byte)settingsPanel.getDelimiter(), 0, n))
            {
               modifiers = IOSettingsPanel.FILE_FORMAT_FLAG_CSV;
            }
            else
            {
               modifiers = IOSettingsPanel.FILE_FORMAT_ANY_TEX;
            }
         }
         else
         {
            messageHandler.error(this,
              messageHandler.getLabel("error.empty_file"));
         }
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }
      }

      return modifiers;
   }

   protected boolean startsWith(byte[] buffer, byte[] marker,
      int startIdx, int buffLen)
   {
      if (buffLen - startIdx < marker.length)
      {
         return false;
      }

      for (int i = startIdx, j = 0; i < buffLen && j < marker.length; i++, j++)
      {
         if (buffer[i] != marker[j]) return false;
      }

      return true;
   }

   protected boolean contains(byte[] buffer, byte[] marker,
      int startIdx, int buffLen)
   {
      for (int i = startIdx; i < buffLen; i++)
      {
         if (startsWith(buffer, marker, i, buffLen))
         {
            return true;
         }
      }

      return false;
   }

   protected boolean contains(byte[] buffer, byte marker,
      int startIdx, int buffLen)
   {
      for (int i = startIdx; i < buffLen; i++)
      {
         if (buffer[i] == marker) return true;
      }

      return false;
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
         catch (Exception e)
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
          IOSettingsPanel.FILE_FORMAT_FLAG_SQL));

      String filename = "";
      String select = "";
      String source = "";

      if (isSql)
      {
         select = selectField.getText().trim();

         if (select.isEmpty())
         {
            throw new IOException(messageHandler.getLabel(
              "error.missing_select"));
         }

         source = "SELECT "+select;
      }
      else
      {
         filename = fileField.getFileName();

         if (filename.isEmpty())
         {
            throw new IOException(messageHandler.getLabel(
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
           IOSettingsPanel.FILE_FORMAT_CSV_OR_TSV))
      {
         imp = new DatatoolCsv(settings);
      }
      else if (isSql)
      {
         imp = new DatatoolSql(settings);
      }
      else if (settingsPanel.isFileFormatSelected(
           IOSettingsPanel.FILE_FORMAT_FLAG_ODS))
      {
         imp = new DatatoolOpenDoc(settings, false);
      }
      else if (settingsPanel.isFileFormatSelected(
           IOSettingsPanel.FILE_FORMAT_FLAG_FODS))
      {
         imp = new DatatoolOpenDoc(settings, true);
      }
      else if (settingsPanel.isFileFormatSelected(
           IOSettingsPanel.FILE_FORMAT_FLAG_XLS))
      {
         imp = new DatatoolExcel(settings);
      }
      else if (settingsPanel.isFileFormatSelected(
           IOSettingsPanel.FILE_FORMAT_FLAG_XLSX))
      {
         imp = new DatatoolExcel(settings);
      }
      else
      {
         imp = new DatatoolTeX(settings);
      }

      loadSettings.setDataImport(imp, source);

      if (imp instanceof DatatoolSpreadSheetImport)
      {
         File file = new File(source);

         Object ref = JOptionPane.showInputDialog(this,
            messageHandler.getLabel("importspread.sheet"),
            messageHandler.getLabel("importspread.title"),
            JOptionPane.PLAIN_MESSAGE,
            null, ((DatatoolSpreadSheetImport)imp).getSheetNames(file),
            null);

         if (ref == null)
         {
            return;
         }

         importSettings.setSheetName(ref.toString());
      }

      setVisible(false);

      DatatoolFileLoader loader = new DatatoolFileLoader(gui, loadSettings);
      loader.execute();
   }

   public void display()
   {
      DatatoolSettings settings = gui.getSettings();

      try
      {
         DataToolTeXParserListener listener = settings.getTeXParserListener();
         listener.applyCurrentSettings();
         IOSettings ioSettings = listener.getIOSettings();

         settingsPanel.setFrom(ioSettings);
      }
      catch (IOException e)
      {
         gui.getMessageHandler().error(this, e);
      }

      settingsPanel.setCsvSettingsFrom(settings);

      if (settings.getSeparator() == '\t')
      {
         settingsPanel.setSelectedFileFormat(IOSettingsPanel.FILE_FORMAT_FLAG_TSV);
      }
      else
      {
         settingsPanel.setSelectedFileFormat(IOSettingsPanel.FILE_FORMAT_FLAG_CSV);
      }

      fileField.setFileName("");

      setVisible(true);
   }

   DatatoolGUI gui;
   IOSettingsPanel settingsPanel;
   FileField fileField;
   JTextField selectField;

   CardLayout fileSelectCardLayout;
   JComponent fileSelectCardComp, selectComp;

   public static final byte[] ZIP_MARKER = new byte[]
     { (byte)0x50, (byte)0x4B, (byte)0x03, (byte)0x04 };

   public static final byte[] MIMETYPE_MARKER = 
     "mimetype".getBytes();

   public static final byte[] ODS_MIMETYPE = 
     "application/vnd.oasis.opendocument.spreadsheet".getBytes();

   public static final byte[] XML_MARKER = "<?xml".getBytes();

   public static final byte[] OFFICE_DOCUMENT_MARKER
    = "<office:document".getBytes();

   public static final byte[] XLSX_MIMETYPE =
     "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".getBytes();

   public static final byte[] XLS_MARKER = new byte[]
     { (byte)0xD0, (byte)0xCF, (byte)0x11, (byte)0xE0,
       (byte)0xA1, (byte)0xB1, (byte)0x1A, (byte)0xE1};

   public static final byte[] BOM = new byte[]
     { (byte)0xEF, (byte)0xBB, (byte)0xBF };
}
