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

import java.awt.event.*;
import java.awt.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

/**
 * Dialog box for finding a cell that matches a search string.
 */
public class FindCellDialog extends JDialog
  implements ActionListener
{
   public FindCellDialog(DatatoolGUI gui)
   {
      super(gui, gui.getMessageHandler().getLabel("find.title"));

      this.gui = gui;

      DatatoolGuiResources resources = gui.getResources();

      JComponent mainPanel = Box.createVerticalBox();
      getContentPane().add(mainPanel, BorderLayout.CENTER);

      JComponent panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      searchField = new JTextField();
      JLabel searchLabel = resources.createJLabel(
         "find.cell_containing", searchField);

      panel.add(searchLabel);
      panel.add(searchField);

      Dimension dim = searchField.getMaximumSize();
      dim.height = (int)searchField.getPreferredSize().getHeight();
      searchField.setMaximumSize(dim);

      searchField.getDocument().addDocumentListener(new DocumentListener()
      {
         public void changedUpdate(DocumentEvent e)
         {
            updateButtons();
         }

         public void insertUpdate(DocumentEvent e)
         {
            updateButtons();
         }

         public void removeUpdate(DocumentEvent e)
         {
            updateButtons();
         }
      });

      panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      caseBox = resources.createJCheckBox("find",
        "case", null);
      panel.add(caseBox);

      regexBox = resources.createJCheckBox("find",
        "regex", null);
      panel.add(regexBox);

      wrapBox = resources.createJCheckBox("find",
        "wrap", null);
      panel.add(wrapBox);
      wrapBox.setSelected(true);

      panel = Box.createHorizontalBox();
      mainPanel.add(panel);

      ButtonGroup bg = new ButtonGroup();

      rowWiseButton = resources.createJRadioButton("find",
         "rowwise", bg, null);
      panel.add(rowWiseButton);

      columnWiseButton = resources.createJRadioButton("find",
         "colwise", bg, null);
      panel.add(columnWiseButton);

      rowWiseButton.setSelected(true);

      JPanel buttonPanel = new JPanel();
      getContentPane().add(buttonPanel, BorderLayout.SOUTH);

      findButton = resources.createActionButton(
        "find", "find", "search", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));

      buttonPanel.add(findButton);

      buttonPanel.add(resources.createActionButton(
        "find", "close", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));

      getRootPane().setDefaultButton(findButton);
      updateButtons();

      pack();
      setLocationRelativeTo(gui);
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("find"))
      {
         find();
      }
      else if (action.equals("close"))
      {
         gui.updateTools();
         setVisible(false);
      }
   }

   public void setSearchText(String searchText)
   {
      searchField.setText(searchText);
   }

   public String getSearchText()
   {
      return searchField.getText();
   }

   public void display(DatatoolDbPanel table)
   {
      this.table = table;

      searchField.requestFocusInWindow();

      column = table.getModelSelectedColumn();
      row = table.getModelSelectedRow();

      found = false;

      updateButtons();

      setVisible(true);
   }

   public void findNext(DatatoolDbPanel table)
   {
      this.table = table;

      column = table.getModelSelectedColumn();
      row = table.getModelSelectedRow();

      found = false;

      find();
   }

   public void find()
   {
      found = regexBox.isSelected() ?  findRegEx() : findNoRegEx();

      if (found)
      {
         table.selectModelCell(row, column);
         table.scrollToModelCell(row, column);
      }
      else
      {
         JOptionPane.showMessageDialog(this,
            gui.getMessageHandler().getLabel("find.not_found"));
      }

      updateButtons();
   }

   private boolean advance()
   {
      if (isRowWise())
      {
         row++;

         if (row >= table.getRowCount())
         {
            row = 0;
            column++;

            if (column >= table.getColumnCount())
            {
               if (wrapBox.isSelected())
               {
                  column = 0;
               }
               else
               {
                  return false;
               }
            }
         }

         if (column < 0) column = 0;
      }
      else
      {
         column++;

         if (column >= table.getColumnCount())
         {
            column = 0;
            row++;

            if (row >= table.getRowCount())
            {
               if (wrapBox.isSelected())
               {
                  row = 0;
               }
               else
               {
                  return false;
               }
            }
         }

         if (row < 0) row = 0;
      }


      return true;
   }

   public boolean findNoRegEx()
   {
      String searchText = searchField.getText();

      int currentRow = row;
      int currentCol = column;

      if (!advance())
      {
         return false;
      }

      while (!(currentRow == row && currentCol == column))
      {
         String text = table.getValueAtModel(row, column).toString();

         if (!caseBox.isSelected())
         {
            searchText = searchText.toLowerCase();
            text = text.toLowerCase();
         }

         if (text.indexOf(searchText) == -1)
         {
            if (!advance())
            {
               return false;
            }
         }
         else
         {
            return true;
         }
      }

      // If we've reached here we've wrapped round to our starting
      // point. So now check that cell.

      String text = table.getValueAtModel(row, column).toString();

      if (!caseBox.isSelected())
      {
         searchText = searchText.toLowerCase();
         text = text.toLowerCase();
      }

      return text.indexOf(searchText) != -1;
   }

   public boolean findRegEx()
   {
      Pattern pattern;

      int currentRow = row;
      int currentCol = column;

      if (!advance())
      {
         return false;
      }

      while (!(currentRow == row && currentCol == column))
      {
         String text = table.getValueAtModel(row, column).toString();

         if (caseBox.isSelected())
         {
            pattern = Pattern.compile(searchField.getText());
         }
         else
         {
            pattern = Pattern.compile(searchField.getText(),
               Pattern.CASE_INSENSITIVE);
         }

         Matcher matcher = pattern.matcher(text);

         if (matcher.find())
         {
            return true;
         }
         else
         {
            if (!advance())
            {
               return false;
            }
         }
      }

      // If we've reached here we've wrapped round to our starting
      // point. So now check that cell.

      String text = table.getValueAtModel(row, column).toString();

      if (caseBox.isSelected())
      {
         pattern = Pattern.compile(searchField.getText());
      }
      else
      {
         pattern = Pattern.compile(searchField.getText(),
            Pattern.CASE_INSENSITIVE);
      }

      Matcher matcher = pattern.matcher(text);

      return matcher.find();
   }

   private void updateButtons()
   {
      findButton.setEnabled(!searchField.getText().isEmpty());
   }

   public boolean isRowWise()
   {
      return rowWiseButton.isSelected();
   }

   private JButton findButton;
   private JTextField searchField;
   private DatatoolDbPanel table;
   private JCheckBox caseBox, regexBox, wrapBox;

   private JRadioButton rowWiseButton, columnWiseButton;

   private boolean found = false;

   private int row, column;

   private DatatoolGUI gui;
}
