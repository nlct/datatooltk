package com.dickimawbooks.datatooltk;

import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JButton;

public class GuiPasswordReader extends JDialog 
  implements DatatoolPasswordReader,ActionListener
{
   public GuiPasswordReader(Frame parent)
   {
      super(parent, "SQL Password", true);

      JPanel panel = new JPanel();

      getContentPane().add(panel, "Center");

      JLabel label = new JLabel("Password:");
      label.setDisplayedMnemonic('P');

      panel.add(label);

      passwordField = new JPasswordField(10);
      label.setLabelFor(passwordField);

      panel.add(passwordField);

      JPanel buttonPanel = new JPanel();

      getContentPane().add(buttonPanel, "South");

      JButton okayButton = new JButton("Okay");
      okayButton.setMnemonic('O');
      okayButton.setActionCommand("okay");
      okayButton.addActionListener(this);
      buttonPanel.add(okayButton);

      JButton cancelButton = new JButton("Cancel");
      cancelButton.setMnemonic('C');
      cancelButton.setActionCommand("cancel");
      cancelButton.addActionListener(this);
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
