package com.dickimawbooks.datatooltk.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.*;

public class SortDialog extends JDialog
  implements ActionListener
{
   public SortDialog(DatatoolGUI gui)
   {
      super(gui, DatatoolTk.getLabel("sort.title"), true);

      getContentPane().add(
        DatatoolGuiResources.createOkayCancelPanel(this),
        BorderLayout.SOUTH);

      pack();
      setLocationRelativeTo(null);
   }

   public void display(DatatoolDb db)
   {
      this.db = db;

      setVisible(true);
   }

   public void actionPerformed(ActionEvent event)
   {
      String action = event.getActionCommand();

      if (action == null) return;

      if (action.equals("okay"))
      {
         okay();
      }
      else if (action.equals("cancel"))
      {
         setVisible(false);
      }
   }

   private void okay()
   {
      setVisible(false);
   }

   private JComboBox<DatatoolHeader> headerBox;
   private JRadioButton ascendingButton, descendingButton;

   private DatatoolDb db;
}
