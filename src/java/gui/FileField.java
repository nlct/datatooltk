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

import java.io.*;
import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.texjavahelplib.TeXJavaHelpLib;

import com.dickimawbooks.datatooltk.DatatoolTk;
import com.dickimawbooks.datatooltk.MessageHandler;

/**
 * Component for specifying a file or directory.
 */
public class FileField extends Box
  implements ActionListener
{
   public FileField(DatatoolGuiResources guiResources, Container parent, 
       JFileChooser fileChooser)
   {
      this(guiResources, parent, null, fileChooser, JFileChooser.FILES_ONLY);
   }

   public FileField(DatatoolGuiResources guiResources, Container parent,
      JFileChooser fileChooser, int mode)
   {
      this(guiResources, parent, null, fileChooser, mode);
   }

   public FileField(DatatoolGuiResources guiResources, Container parent, 
      String fileName, JFileChooser fileChooser)
   {
      this(guiResources, parent, fileName, fileChooser, JFileChooser.FILES_ONLY);
   }

   public FileField(DatatoolGuiResources guiResources, Container parent, 
      String fileName, JFileChooser fileChooser, int mode)
   {
      this(guiResources, parent, fileName, fileChooser, mode, null);
   }

   public FileField(DatatoolGuiResources guiResources, Container parent, 
      String fileName, JFileChooser fileChooser, int mode,
      String labelTag)
   {
      super(BoxLayout.Y_AXIS);

      this.fileChooser = fileChooser;
      this.parent = parent;
      this.mode = mode;
      this.guiResources = guiResources;

      TeXJavaHelpLib helpLib = guiResources.getHelpLib();

      add(Box.createVerticalGlue());

      Box box = Box.createHorizontalBox();
      add(box);

      textField = new JTextField(fileName == null ? "" : fileName, 20);

      if (labelTag != null && !labelTag.isEmpty())
      {
         box.add(helpLib.createJLabel(labelTag, textField));
      }

      Dimension dim = textField.getPreferredSize();
      dim.width = (int)textField.getMaximumSize().getWidth();

      textField.setMaximumSize(dim);

      box.add(textField);

      textField.addFocusListener(new FocusAdapter()
       {
          public void focusLost(FocusEvent evt)
          {
             String newFile = textField.getText();

             if (!oldFile.equals(newFile))
             {
                fireFileChangeUpdate(new FileSelectionChangeEvent(this,
                 oldFile, newFile));
             }

             oldFile = newFile;
          }
       });

      button = helpLib.createJButton("button", "choose_file", this,
        "open", true, true);

      box.add(button);

      add(Box.createVerticalGlue());

      setAlignmentY(Component.CENTER_ALIGNMENT);
      setAlignmentX(Component.LEFT_ALIGNMENT);
   }

   public void setAlignmentY(float align)
   {
      super.setAlignmentY(align);
      textField.setAlignmentY(align);
      button.setAlignmentY(align);
   }

   public void setAlignmentX(float align)
   {
      super.setAlignmentX(align);
      textField.setAlignmentX(align);
      button.setAlignmentX(align);
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("choose_file"))
      {
         fileChooser.setFileSelectionMode(mode);

         File file = getFile();

         if (file != null)
         {
            fileChooser.setCurrentDirectory(file.getParentFile());

            fileChooser.setSelectedFile(file);
         }

         fileChooser.setApproveButtonMnemonic(
            guiResources.getMnemonicInt("button.select"));

         if (fileChooser.showDialog(parent,
            guiResources.getMessage("button.select"))
            == JFileChooser.APPROVE_OPTION)
         {
            oldFile = textField.getText();
            String newFile = fileChooser.getSelectedFile().getAbsolutePath();

            textField.setText(newFile);

            if (!oldFile.equals(newFile))
            {
               fireFileChangeUpdate(new FileSelectionChangeEvent(this,
                oldFile, newFile));
            }

            oldFile = newFile;
         }
      }
   }

   public boolean requestFocusInWindow()
   {
      return textField.requestFocusInWindow();
   }

   public JTextField getTextField()
   {
      return textField;
   }

   public File getFile()
   {
      String fileName = getFileName();

      if (fileName == null || fileName.equals("")) return null;

      return fileName.contains(File.separator) 
         ? new File(fileName)
         : new File(fileChooser.getCurrentDirectory(), fileName);
   }

   public String getFileName()
   {
      return textField.getText();
   }

   public void setFileName(String name)
   {
      textField.setText(name);
   }

   public void setCurrentDirectory(String dirPath)
   {
      setCurrentDirectory(new File(dirPath));
   }

   public void setCurrentDirectory(File dir)
   {
      fileChooser.setCurrentDirectory(dir);
   }

   public void setFile(File file)
   {
      setCurrentDirectory(file.getParentFile());
      setFileName(file.getName());
   }

   public void setEnabled(boolean flag)
   {
      super.setEnabled(flag);

      textField.setEnabled(flag);
      button.setEnabled(flag);
   }

   protected void fireFileChangeUpdate(FileSelectionChangeEvent evt)
   {
      if (fileChangeListeners != null)
      {
         for (FileSelectionChangeListener listener : fileChangeListeners)
         {
            listener.fileSelectionChanged(evt);

            if (evt.isConsumed())
            {
               break;
            }
         }
      }
   }

   public void addFileSelectionChangeListener(
      FileSelectionChangeListener listener)
   {
      if (fileChangeListeners == null)
      {
         fileChangeListeners = new Vector<FileSelectionChangeListener>();
      }

      fileChangeListeners.add(listener);
   }

   private JTextField textField;

   private JButton button;

   private JFileChooser fileChooser;

   private Container parent;

   private int mode;

   private DatatoolGuiResources guiResources;

   private Vector<FileSelectionChangeListener> fileChangeListeners;
   private String oldFile="";
}
