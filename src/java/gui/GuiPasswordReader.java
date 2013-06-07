package com.dickimawbooks.datatooltk.gui;

import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JButton;

import com.dickimawbooks.datatooltk.*;
import com.dickimawbooks.datatooltk.io.DatatoolPasswordReader;

public class GuiPasswordReader extends JDialog 
  implements DatatoolPasswordReader,ActionListener
{
   public GuiPasswordReader(Frame parent)
   {
      super(parent, DatatoolTk.getLabel("password.title"), true);

      JPanel panel = new JPanel();

      getContentPane().add(panel, "Center");

      JLabel label = DatatoolGuiResources.createJLabel("password.prompt");

      panel.add(label);

      passwordField = new JPasswordField(10);
      label.setLabelFor(passwordField);

      panel.add(passwordField);

      JPanel buttonPanel = new JPanel();

      getContentPane().add(buttonPanel, "South");

      JButton okayButton = DatatoolGuiResources.createOkayButton(this);
      buttonPanel.add(okayButton);

      JButton cancelButton = DatatoolGuiResources.createCancelButton(this);
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

      throw new UserCancelledException();
   }

   private boolean success=false;

   private JPasswordField passwordField;
}
