package com.dickimawbooks.datatooltk.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.*;

public class PropertiesDialog extends JDialog
  implements ActionListener
{
   public PropertiesDialog(DatatoolGUI gui)
   {
      super(gui, DatatoolTk.getLabel("preferences.title"), true);
      this.gui = gui;

      JPanel buttonPanel = new JPanel();

      buttonPanel.add(DatatoolGuiResources.createOkayButton(this));
      buttonPanel.add(DatatoolGuiResources.createCancelButton(this));
      buttonPanel.add(gui.createHelpButton("properties"));

      getContentPane().add(buttonPanel, BorderLayout.SOUTH);
      pack();

      setLocationRelativeTo(null);
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("okay"))
      {
      }
      else if (action.equals("cancel"))
      {
         setVisible(false);
      }
   }

   private DatatoolGUI gui;
}
