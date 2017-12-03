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

import java.util.Locale;
import java.util.Arrays;
import java.util.Comparator;
import java.text.Collator;

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
      super(gui, gui.getMessageHandler().getLabel("sort.title"), true);

      messageHandler = gui.getMessageHandler();
      DatatoolGuiResources resources = messageHandler.getDatatoolGuiResources();

      JComponent mainPanel = Box.createVerticalBox();
      mainPanel.setAlignmentX(0);
      getContentPane().add(mainPanel, BorderLayout.CENTER);

      JPanel row = new JPanel();
      row.setAlignmentX(0);
      mainPanel.add(row);

      headerBox = new JComboBox<DatatoolHeader>();
      headerBox.setAlignmentX(0);
      row.add(resources.createJLabel(
         "sort.column", headerBox, 0));
      row.add(headerBox);

      row = new JPanel();
      row.setAlignmentX(0);
      mainPanel.add(row);

      ButtonGroup bg = new ButtonGroup();

      ascendingButton = resources.createJRadioButton(
         "sort", "ascending", bg, this, 0);
      row.add(ascendingButton);

      descendingButton = resources.createJRadioButton(
         "sort", "descending", bg, this, 0);
      row.add(descendingButton);

      row = new JPanel();
      row.setAlignmentX(0);
      mainPanel.add(row);

      row.add(resources.createJLabel("sort.string.message", 0));

      row = new JPanel();
      row.setAlignmentX(0);
      mainPanel.add(row);

      bg = new ButtonGroup();

      letterSortButton = resources.createJRadioButton(
         "sort", "letter", bg, this, 0);
      row.add(letterSortButton);

      isCaseSensitiveBox = resources.createJCheckBox(
        "sort", "case_sensitive", this, 0);
      row.add(isCaseSensitiveBox);

      row = new JPanel();
      row.setAlignmentX(0);
      mainPanel.add(row);

      localeSortButton = resources.createJRadioButton(
         "sort", "locale", bg, this, 0);
      row.add(localeSortButton);

      Locale[] locales = Locale.getAvailableLocales();

      Arrays.sort(locales, new LocaleComparator());

      localeBox = new JComboBox<Locale>(locales);
      localeBox.setRenderer(new LocaleItemRenderer());
      localeBox.setAlignmentX(0);
      row.add(localeBox);

      localeBox.setSelectedItem(Locale.getDefault());

      getContentPane().add(
        resources.createOkayCancelHelpPanel(this, gui, "sort"),
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

      Locale locale = db.getSortLocale();

      if (locale == null)
      {
         letterSortButton.setSelected(true);
         localeBox.setEnabled(false);
         isCaseSensitiveBox.setEnabled(true);
      }
      else
      {
         localeSortButton.setSelected(true);
         localeBox.setEnabled(true);
         localeBox.setSelectedItem(locale);
         isCaseSensitiveBox.setEnabled(false);
      }

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
      else if (action.equals("letter"))
      {
         localeBox.setEnabled(false);
         isCaseSensitiveBox.setEnabled(true);
      }
      else if (action.equals("locale"))
      {
         localeBox.setEnabled(true);
         isCaseSensitiveBox.setEnabled(false);
      }
   }

   private void okay()
   {
      int colIdx = headerBox.getSelectedIndex();

      if (colIdx < 0)
      {
         // This shouldn't happen

         getMessageHandler().error(this, 
            getMessageHandler().getLabel("error.no_sort_column_selected"));
         return;
      }

      db.setSortColumn(colIdx);
      db.setSortAscending(ascendingButton.isSelected());
      db.setSortCaseSensitive(isCaseSensitiveBox.isSelected());

      if (letterSortButton.isSelected())
      {
         db.setSortLocale(null);
      }
      else
      {
         db.setSortLocale((Locale)localeBox.getSelectedItem());
      }

      success = true;
      setVisible(false);
   }

   public MessageHandler getMessageHandler()
   {
      return messageHandler;
   }

   private JComboBox<DatatoolHeader> headerBox;
   private JComboBox<Locale> localeBox;
   private JRadioButton ascendingButton, descendingButton;
   private JRadioButton letterSortButton, localeSortButton;

   private JCheckBox isCaseSensitiveBox;

   private DatatoolDb db;

   private MessageHandler messageHandler;

   private boolean success=false;
}

class LocaleItemRenderer extends JLabel implements ListCellRenderer<Locale>
{
   public LocaleItemRenderer()
   {
      setOpaque(true);
      setHorizontalAlignment(LEFT);
      setVerticalAlignment(CENTER);
   }

   public Component getListCellRendererComponent(JList<? extends Locale> list,
    Locale locale, int index, boolean isSelected, boolean cellHasFocus) 
   {
      if (isSelected)
      {
         setBackground(list.getSelectionBackground());
         setForeground(list.getSelectionForeground());
      }
      else
      {
         setBackground(list.getBackground());
         setForeground(list.getForeground());
      }

      setText(locale.getDisplayName());

      return this;
   }
}

class LocaleComparator implements Comparator<Locale>
{
   public LocaleComparator()
   {
      collator = Collator.getInstance();
   }

   public int compare(Locale locale1, Locale locale2)
   {
      return collator.compare(locale1.getDisplayName(), 
        locale2.getDisplayName());
   }

   private Collator collator;
}
