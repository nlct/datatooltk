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

import java.net.URL;

import java.awt.Frame;
import java.awt.Dialog;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

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

      passwordField = new JPasswordField(10);

      JLabel label = createJLabel(messageHandler, "password.prompt",
         passwordField);

      panel.add(label);
      panel.add(passwordField);

      JPanel buttonPanel = new JPanel();

      getContentPane().add(buttonPanel, "South");

      buttonPanel.add(createActionButton(messageHandler, "button", "okay",
          KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)));

      buttonPanel.add(createActionButton(messageHandler, "button", "cancel",
          KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));

      pack();

      setLocationRelativeTo(parent);
   }

   // GUI resources won't have been created if password dialog is
   // required in batch mode, so provide local methods:
   private JLabel createJLabel(MessageHandler messageHandler, String label,
     JComponent comp)
   {
       JLabel jLabel = new JLabel(messageHandler.getLabel(label));

       int mnemonic = messageHandler.getMnemonicInt(label);

       if (mnemonic != -1)
       {
          jLabel.setDisplayedMnemonic(mnemonic);
       }

       String tooltip = messageHandler.getToolTip(label);

       if (tooltip != null)
       {
          jLabel.setToolTipText(tooltip);
       }

       if (comp != null)
       {
          jLabel.setLabelFor(comp);
       }

       return jLabel;
   }

    public JButton createActionButton(MessageHandler messageHandler,
      String parent, String label, KeyStroke keyStroke)
    {
       String tooltipText = messageHandler.getToolTip(parent, label);
       
       DatatoolGuiResources resources
          = messageHandler.getDatatoolGuiResources();

       URL imageUrl = null;
       URL rollOverImageUrl = null;
       URL pressedImageUrl = null;
       URL selectedImageUrl = null;

       if (resources != null)
       {
          imageUrl = resources.getImageUrl(label);

          if (imageUrl != null)
          {
             rollOverImageUrl = resources.getImageUrl(label+"_rollover");
             pressedImageUrl = resources.getImageUrl(label+"_pressed");
             selectedImageUrl = resources.getImageUrl(label+"_selected");
          }
       }

       String buttonLabel = messageHandler.getLabel(parent, label);
       int mnemonic = messageHandler.getMnemonicInt(parent, label);
       String actionCommand = label;

       JButton button;

       if (imageUrl == null)
       {
          button = new JButton(buttonLabel);
       }
       else
       {
          button = new JButton(buttonLabel, new ImageIcon(imageUrl));

          if (rollOverImageUrl != null)
          {
             button.setRolloverIcon(new ImageIcon(rollOverImageUrl));
          }

          if (pressedImageUrl != null)
          {
             button.setPressedIcon(new ImageIcon(pressedImageUrl));
          }

          if (selectedImageUrl != null)
          {
             button.setSelectedIcon(new ImageIcon(selectedImageUrl));
          }
       }

       if (mnemonic != -1)
       {
          button.setMnemonic(mnemonic);
       }

       button.addActionListener(this);

       if (actionCommand != null)
       {
          button.setActionCommand(actionCommand);

          if (keyStroke != null)
          {
             button.registerKeyboardAction(this,
               actionCommand, keyStroke,
               JComponent.WHEN_IN_FOCUSED_WINDOW);
          }
       }

       if (tooltipText != null)
       {
          button.setToolTipText(tooltipText);
       }

       return button;
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
