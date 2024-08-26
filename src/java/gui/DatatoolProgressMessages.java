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

import java.awt.IllegalComponentStateException;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import com.dickimawbooks.datatooltk.DatatoolTk;

/**
 * Progress messages window.
 */
public class DatatoolProgressMessages extends JDialog
{
   public DatatoolProgressMessages(DatatoolGUI gui)
   {
      super(gui, gui.getMessageHandler().getLabelWithValues(
        "progress.title", gui.getMessageHandler().getApplicationName()));
      this.gui = gui;

      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      addWindowListener(new WindowAdapter()
      {
         public void windowClosing(WindowEvent evt)
         {
            cancelProgress();
         }
      });

      messageArea = new JTextArea(4, 50);
      messageArea.setEditable(false);
      messageArea.setLineWrap(true);

      getContentPane().add(new JScrollPane(messageArea), "Center");

      progressBar = new JProgressBar(0, 100);
      getContentPane().add(progressBar, "South");

      pack();
      setLocationRelativeTo(gui);
   }

   private void cancelProgress()
   {
      if (gui.cancelProgress())
      {
         setVisible(false);
      }
   }

   public void reset()
   {
      messageArea.setText("");
      progressBar.setValue(0);
   }

   public void addMessage(String msg)
   {
      messageArea.setText(String.format("%s%s%n",
       messageArea.getText(), msg));
   }

   public void setProgress(int value)
   {
      progressBar.setValue(value);
   }

   private DatatoolGUI gui;
   private JTextArea messageArea;
   private JProgressBar progressBar;
}
