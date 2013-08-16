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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.*;

/**
 * Dialog for specifying sort criteria.
 */
public class SortDialog extends JDialog
  implements ActionListener
{
   public SortDialog(DatatoolGUI gui)
   {
      super(gui, DatatoolTk.getLabel("sort.title"), true);

      JComponent mainPanel = Box.createVerticalBox();
      mainPanel.setAlignmentX(0);
      getContentPane().add(mainPanel, BorderLayout.CENTER);

      JPanel row = new JPanel();
      row.setAlignmentX(0);
      mainPanel.add(row);

      headerBox = new JComboBox<DatatoolHeader>();
      row.add(DatatoolGuiResources.createJLabel(
         "sort.column", headerBox));
      row.add(headerBox);

      row = new JPanel();
      row.setAlignmentX(0);
      mainPanel.add(row);

      ButtonGroup bg = new ButtonGroup();

      ascendingButton = DatatoolGuiResources.createJRadioButton(
         "sort", "ascending", bg, this);
      row.add(ascendingButton);

      descendingButton = DatatoolGuiResources.createJRadioButton(
         "sort", "descending", bg, this);
      row.add(descendingButton);

      row = new JPanel();
      row.setAlignmentX(0);
      mainPanel.add(row);

      isCaseSensitiveBox = DatatoolGuiResources.createJCheckBox("sort", "case_sensitive", this);
      row.add(isCaseSensitiveBox);

      getContentPane().add(
        DatatoolGuiResources.createOkayCancelHelpPanel(this, gui, "sort"),
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

      isCaseSensitiveBox.setSelected(db.isSortCaseSensitive());

      pack();
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
      db.setSortCaseSensitive(isCaseSensitiveBox.isSelected());

      success = true;
      setVisible(false);
   }

   private JComboBox<DatatoolHeader> headerBox;
   private JRadioButton ascendingButton, descendingButton;

   private JCheckBox isCaseSensitiveBox;

   private DatatoolDb db;

   private boolean success=false;
}
