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
package com.dickimawbooks.datatooltk.io;

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

import com.dickimawbooks.texjavahelplib.TeXJavaHelpLib;
import com.dickimawbooks.datatooltk.*;

/**
 * Dialog to prompt user for a password.
 * Note that this may be used in batch mode with console action "gui".
 */
public class GuiPasswordReader extends JDialog 
  implements DatatoolPasswordReader,ActionListener
{
   public GuiPasswordReader(TeXJavaHelpLib helpLib, Frame parent)
   {
      super(parent, helpLib.getMessage("password.title"), true);

      init(helpLib, parent);
   }

   public GuiPasswordReader(TeXJavaHelpLib helpLib, Dialog parent)
   {
      super(parent, helpLib.getMessage("password.title"), true);

      init(helpLib, parent);
   }

   private void init(TeXJavaHelpLib helpLib, Component parent)
   {
      this.helpLib = helpLib;

      JPanel panel = new JPanel();

      getContentPane().add(panel, "Center");

      passwordField = new JPasswordField(10);

      JLabel label = helpLib.createJLabel("password.prompt", passwordField);

      panel.add(label);
      panel.add(passwordField);

      JPanel buttonPanel = new JPanel();

      getContentPane().add(buttonPanel, "South");

      JButton okayButton = helpLib.createOkayButton(this);
      buttonPanel.add(okayButton);
      getRootPane().setDefaultButton(okayButton);

      buttonPanel.add(helpLib.createCancelButton(this));

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

      throw new UserCancelledException(helpLib.getMessageSystem());
   }

   private boolean success=false;

   private JPasswordField passwordField;

   private TeXJavaHelpLib helpLib;
}
