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

      JComponent mainPanel = Box.createVerticalBox();
      getContentPane().add(mainPanel, BorderLayout.CENTER);

      JPanel row = new JPanel();
      mainPanel.add(row);

      headerBox = new JComboBox<DatatoolHeader>();
      row.add(DatatoolGuiResources.createJLabel(
         "sort.column", headerBox));
      row.add(headerBox);

      row = new JPanel();
      mainPanel.add(row);

      ButtonGroup bg = new ButtonGroup();

      ascendingButton = DatatoolGuiResources.createJRadioButton(
         "sort", "ascending", bg, this);
      row.add(ascendingButton);

      descendingButton = DatatoolGuiResources.createJRadioButton(
         "sort", "descending", bg, this);
      row.add(descendingButton);

      getContentPane().add(
        DatatoolGuiResources.createOkayCancelPanel(this),
        BorderLayout.SOUTH);

      pack();
      setLocationRelativeTo(null);
   }

   public boolean requestInput(DatatoolDb db)
   {
      this.db = db;
      success = false;

      headerBox.setModel(
         new DefaultComboBoxModel<DatatoolHeader>(db.getHeaders()));

      int colIdx = db.getSortColumn();

      if (colIdx > -1 || colIdx < db.getColumnCount())
      {
         headerBox.setSelectedIndex(colIdx);
      }

      if (db.isSortAscending())
      {
         ascendingButton.setSelected(true);
      }
      else
      {
         descendingButton.setSelected(true);
      }

      setVisible(true);

      return success;
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
         success = false;
         setVisible(false);
      }
   }

   private void okay()
   {
      int colIdx = headerBox.getSelectedIndex();

      if (colIdx < 0)
      {
         // This shouldn't happen

         DatatoolGuiResources.error(this, 
            DatatoolTk.getLabel("error.no_sort_column_selected"));
         return;
      }

      db.setSortColumn(colIdx);
      db.setSortAscending(ascendingButton.isSelected());

      success = true;
      setVisible(false);
   }

   private JComboBox<DatatoolHeader> headerBox;
   private JRadioButton ascendingButton, descendingButton;

   private DatatoolDb db;

   private boolean success=false;
}
