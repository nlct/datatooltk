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

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.DatatoolTk;
import com.dickimawbooks.datatooltk.MessageHandler;

/**
 * Panel for error messages.
 */
public class ErrorPanel extends JPanel
  implements ActionListener
{
   public ErrorPanel(DatatoolGuiResources resources)
   {
      super(new BorderLayout());
      this.resources = resources;
      MessageHandler messageHandler = resources.getMessageHandler();

      messageArea = resources.createMessageArea();
      stackTraceArea = resources.createMessageArea();

      onlyMessageArea = resources.createMessageArea();

      cardLayout = new CardLayout();

      cardPanel = new JPanel(cardLayout);
      add(cardPanel, "Center");

      cardPanel.add(new JScrollPane(onlyMessageArea), "onlymessage");
      
      tabbedPane = new JTabbedPane();

      tabbedPane.addTab(messageHandler.getLabelWithAlt(
          "error.message", "Error Message"),
       new JScrollPane(messageArea));

      tabbedPane.setMnemonicAt(tabbedPane.getTabCount()-1,
         resources.getMnemonicInt("error.message"));

      tabbedPane.addTab(messageHandler.getLabelWithAlt(
          "error.stacktrace", "Stack Trace"),
       new JScrollPane(stackTraceArea));

      tabbedPane.setMnemonicAt(tabbedPane.getTabCount()-1,
         resources.getMnemonicInt("error.stacktrace"));

      cardPanel.add(tabbedPane, "tabbedpane");

      JComponent buttonPanel = Box.createVerticalBox();

      buttonPanel.add(resources.createActionButton(
         "button", "exit", this, null));

      buttonPanel.add(Box.createVerticalStrut(50));

      buttonPanel.add(resources.createActionButton(
         "button", "copy", this, 
         KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.SHIFT_DOWN_MASK)));

      add(buttonPanel, "East");

   }

   public synchronized void updateMessage(String message)
   {
      updateMessage(message, null);
   }

   public synchronized void updateMessage(String message, Throwable exception)
   {
      if (exception == null)
      {
         onlyMessageArea.setText(message);

         cardLayout.show(cardPanel, "onlymessage");
      }
      else
      {
         if (message == null)
         {
            messageArea.setText(resources.getMessageHandler().getMessage(exception));
         }
         else
         {
            messageArea.setText(message);
         }

         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

         exception.printStackTrace(new PrintStream(outputStream));

         stackTraceArea.setText(outputStream.toString());

         tabbedPane.setSelectedIndex(0);

         cardLayout.show(cardPanel, "tabbedpane");
      }
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null)
      {
         return;
      }

      if (action.equals("copy"))
      {
         JTextArea area;

         // which card is being viewed?

         if (tabbedPane.isVisible())
         {
            // Which tab is being viewed?

            int idx = tabbedPane.getSelectedIndex();

            if (idx == 0)
            {
               area = messageArea;
            }
            else
            {
               area = stackTraceArea;
            }
         }
         else
         {
            area = onlyMessageArea;
         }

         area.selectAll();
         area.copy();
      }
      else if (action.equals("exit"))
      {
         resources.getDatatoolTk().exit(DatatoolTk.EXIT_USER_FORCED);
      }
   }

   private JTextArea messageArea; // area for error messages.
   private JTextArea stackTraceArea; // area for stack traces

   private JTextArea onlyMessageArea; // area for error messages (without exceptions)

   private JTabbedPane tabbedPane;

   private CardLayout cardLayout;

   private JPanel cardPanel;

   private DatatoolGuiResources resources;
}
