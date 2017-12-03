/*
    Copyright (C) 2013 Nicola L.C. Talbot
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

import java.awt.Frame;
import java.awt.Dialog;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JButton;

import com.dickimawbooks.datatooltk.*;
import com.dickimawbooks.datatooltk.io.DatatoolPasswordReader;

/**
 * Dialog to prompt user for a password.
 */
public class GuiPasswordReader extends JDialog 
  implements DatatoolPasswordReader,ActionListener
{
   public GuiPasswordReader(MessageHandler messageHandler, Frame parent)
   {
      super(parent, messageHandler.getLabel("password.title"), true);

      init(messageHandler, parent);
   }

   public GuiPasswordReader(MessageHandler messageHandler, Dialog parent)
   {
      super(parent, messageHandler.getLabel("password.title"), true);

      init(messageHandler, parent);
   }

   private void init(MessageHandler messageHandler, Component parent)
   {
      this.messageHandler = messageHandler;
      DatatoolGuiResources resources = messageHandler.getDatatoolGuiResources();

      JPanel panel = new JPanel();

      getContentPane().add(panel, "Center");

      JLabel label = resources.createJLabel("password.prompt");

      panel.add(label);

      passwordField = new JPasswordField(10);
      label.setLabelFor(passwordField);

      panel.add(passwordField);

      JPanel buttonPanel = new JPanel();

      getContentPane().add(buttonPanel, "South");

      JButton okayButton = resources.createOkayButton(this);
      buttonPanel.add(okayButton);

      JButton cancelButton = resources.createCancelButton(this);
      buttonPanel.add(cancelButton);

      pack();

      setLocationRelativeTo(parent);
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null)
      {
         return;
      }

      if (action.equals("okay"))
      {
         success = true;
         setVisible(false);
      }
      else if (action.equals("cancel"))
      {
         success = false;
         setVisible(false);
      }
   }

   public char[] requestPassword()
      throws UserCancelledException
   {
      success = false;

      setVisible(true);

      if (success)
      {
         return passwordField.getPassword();
      }

      throw new UserCancelledException(messageHandler);
   }

   private boolean success=false;

   private JPasswordField passwordField;

   private MessageHandler messageHandler;
}
