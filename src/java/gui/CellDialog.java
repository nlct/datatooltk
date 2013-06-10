package com.dickimawbooks.datatooltk.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.*;

public class CellDialog extends JDialog
  implements ActionListener
{
   public CellDialog(DatatoolGUI gui, DatatoolDb db, DatatoolDbPanel panel)
   {
      super(gui, "", true);

      this.panel = panel;

      this.db = db;

      JPanel p = new JPanel();

      p.add(DatatoolGuiResources.createOkayButton(this));
      p.add(DatatoolGuiResources.createCancelButton(this));

      getContentPane().add(p, BorderLayout.SOUTH);

      pack();

      setLocationRelativeTo(null);
   }

   public void display(int rowIdx, int colIdx)
   {

      setVisible(true);
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("okay"))
      {
         panel.setModified(true);

         setVisible(false);
      }
      else if (action.equals("cancel"))
      {
         setVisible(false);
      }
   }

   private DatatoolDb db;

   private DatatoolDbPanel panel;
}
