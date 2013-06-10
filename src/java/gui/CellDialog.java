package com.dickimawbooks.datatooltk.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.*;

public class CellDialog extends JDialog
  implements ActionListener
{
   public CellDialog(DatatoolGUI gui)
   {
      super(gui, DatatoolTk.getLabel("celledit.title"), true);

      this.gui = gui;

      textArea = new JTextArea(20,40);

      textArea.setEditable(true);
      textArea.setLineWrap(true);
      textArea.setWrapStyleWord(true);

      getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);

      JPanel p = new JPanel();

      p.add(DatatoolGuiResources.createOkayButton(this));
      p.add(DatatoolGuiResources.createCancelButton(this));

      getContentPane().add(p, BorderLayout.SOUTH);

      pack();

      setLocationRelativeTo(null);
   }

   public boolean requestEdit(int rowIdx, int colIdx, DatatoolDb db)
   {
      this.db = db;
      this.cell = db.getRow(rowIdx+1).getCell(colIdx+1);

      textArea.setText(cell.getValue().replaceAll("\\\\DTLpar *", "\n\n"));
      revalidate();

      modified = false;

      setVisible(true);

      return modified;
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("okay"))
      {
         cell.setValue(textArea.getText().replaceAll("\n *\n+", "\\\\DTLpar "));
         modified = true;

         setVisible(false);
      }
      else if (action.equals("cancel"))
      {
         setVisible(false);
      }
   }

   private JTextArea textArea;

   private DatatoolDb db;

   private DatatoolCell cell;

   private DatatoolGUI gui;

   private boolean modified;
}
