/*
    Copyright (C) 2013-2024 Nicola L.C. Talbot
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
import java.util.Vector;
import java.text.Collator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import com.dickimawbooks.texjavahelplib.InvalidSyntaxException;

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

      criteriaListPanel = Box.createVerticalBox();

      criteriaListPanel.add(new SortCriteriaPanel(this, 0));

      // common settings

      JComponent row;
      ButtonGroup bg;

      JComponent commonPanel = Box.createVerticalBox();
      commonPanel.setAlignmentX(0);

      row = createRow();
      commonPanel.add(row);

      row.add(resources.createJLabel("sort.null"));

      bg = new ButtonGroup();

      nullFirstButton = resources.createJRadioButton(
         "sort", "null_first", bg, null);
      row.add(nullFirstButton);

      nullLastButton = resources.createJRadioButton(
         "sort", "null_last", bg, null);
      row.add(nullLastButton);

      row = createRow();
      commonPanel.add(row);

      row.add(resources.createJLabel("sort.string.message", 0));

      row = createRow();
      commonPanel.add(row);

      bg = new ButtonGroup();

      letterSortButton = resources.createJRadioButton(
         "sort", "letter", bg, this, 0);
      row.add(letterSortButton);

      isCaseSensitiveBox = resources.createJCheckBox(
        "sort", "case_sensitive", this, 0);
      row.add(isCaseSensitiveBox);

      row = createRow();
      commonPanel.add(row);

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

      JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
        new JScrollPane(criteriaListPanel), new JScrollPane(commonPanel));

      splitPane.setResizeWeight(1.0);
      splitPane.setOneTouchExpandable(true);

      getContentPane().add(splitPane, BorderLayout.CENTER);

      getContentPane().add(
        resources.createDialogOkayCancelHelpPanel(this, this, gui, "sort"),
        BorderLayout.SOUTH);

      pack();
      setLocationRelativeTo(null);
   }

   protected JComponent createRow()
   {
      JPanel comp = new JPanel(new FlowLayout(FlowLayout.LEADING));
      comp.setAlignmentX(0);
      return comp;
   }

   public boolean requestInput(DatatoolDb db, int colIdx)
   {
      if (this.db != db && getSortCriteriaCount() > 1)
      {
         criteriaListPanel.removeAll();
      }

      this.db = db;
      success = false;

      resetSortCriteriaPanels(colIdx);

      if (db.getSettings().isNullFirst())
      {
         nullFirstButton.setSelected(true);
      }
      else
      {
         nullLastButton.setSelected(true);
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
      Vector<SortCriteria> list = new Vector<SortCriteria>();

      for (int i = 0, n = getSortCriteriaCount(); i < n; i++)
      {
         SortCriteriaPanel sortCriteriaPanel = getSortCriteriaPanel(i);

         try
         {
            SortCriteria criteria = sortCriteriaPanel.getCriteria();

            if (criteria != null)
            {
               list.add(criteria);
            }
         }
         catch (InvalidSyntaxException e)
         {
            getMessageHandler().error(this, e.getMessage());
            return;
         }
      }

      if (list.isEmpty())
      {
         getMessageHandler().error(this, 
            getMessageHandler().getLabel("error.no_sort_column_selected"));

         return;
      }

      db.setSortCriteria(list);

      db.setSortCaseSensitive(isCaseSensitiveBox.isSelected());

      if (letterSortButton.isSelected())
      {
         db.setSortLocale(null);
      }
      else
      {
         db.setSortLocale((Locale)localeBox.getSelectedItem());
      }

      db.getSettings().setNullFirst(nullFirstButton.isSelected());

      success = true;
      setVisible(false);
   }

   public MessageHandler getMessageHandler()
   {
      return messageHandler;
   }

   public DatatoolDb getDataBase()
   {
      return db;
   }

   protected void resetSortCriteriaPanels(int colIdx)
   {
      Vector<SortCriteria> criteriaList 
        = (db == null ? null : db.getSortCriteria());

      int listSize = 0;

      if (criteriaList != null)
      {
         listSize = criteriaList.size();
      }

      int compCount = getSortCriteriaCount();

      if (compCount > listSize)
      {
         for (int i = compCount-1; i >= listSize; i--)
         {
            removeSortCriteriaPanel(i);
         }

         compCount = getSortCriteriaCount();
      }

      SortCriteriaPanel criteriaPanel = null;

      if (compCount == 0)
      {
         criteriaPanel = new SortCriteriaPanel(this, 0);
         criteriaListPanel.add(criteriaPanel);
         compCount = 1;
      }

      if (listSize == 0)
      {
         if (criteriaPanel == null)
         {
            criteriaPanel = getSortCriteriaPanel(0);
         }

         criteriaPanel.reset(colIdx == -1 ? 0 : colIdx);

         for (int i = 1; i < compCount; i++)
         {
            getSortCriteriaPanel(i).reset(-1);
         }
      }
      else
      {
         for (int i = 0; i < listSize; i++)
         {
            if (i < compCount)
            {
               criteriaPanel = getSortCriteriaPanel(i);
            }
            else
            {
               criteriaPanel = new SortCriteriaPanel(this, i);
               criteriaListPanel.add(criteriaPanel);
               compCount++;
            }

            criteriaPanel.reset(criteriaList.get(i), colIdx);
            colIdx = -1;
         }
      }

      revalidate();
   }

   protected int getSortCriteriaCount()
   {
      return criteriaListPanel == null ? 0 : criteriaListPanel.getComponentCount();
   }

   protected SortCriteriaPanel getSortCriteriaPanel(int idx)
   {
      return (SortCriteriaPanel)criteriaListPanel.getComponent(idx);
   }

   protected void removeSortCriteriaPanel(int idx)
   {
      // first panel can't be removed

      if (idx > 0)
      {
         int n = getSortCriteriaCount();

         if (idx < n)
         {
            criteriaListPanel.remove(idx);
            n--;

            for (int i = idx-1; i < n; i++)
            {
               getSortCriteriaPanel(i).setComponentIndex(i);
            }
         }
      }
   }

   protected void addSortCriteriaPanel(SortCriteriaPanel comp)
   {
      criteriaListPanel.add(comp);

      int idx = getSortCriteriaCount()-1;

      comp.setComponentIndex(idx);

      idx--;

      if (idx >= 0)
      {
         getSortCriteriaPanel(idx).setComponentIndex(idx);
      }
   }

   protected void insertSortCriteriaPanel(SortCriteriaPanel comp, int index)
   {
      criteriaListPanel.add(comp, index);

      for (int i = Math.max(0, index-1), n = getSortCriteriaCount(); i < n; i++)
      {
         getSortCriteriaPanel(i).setComponentIndex(i);
      }
   }

   protected void moveSortCriteriaPanelUp(SortCriteriaPanel comp)
   {
      int index = comp.getComponentIndex();

      if (index > 0)
      {
         criteriaListPanel.remove(comp);
         criteriaListPanel.add(comp, index-1);
         comp.setComponentIndex(index-1);

         SortCriteriaPanel shiftedComp = 
            getSortCriteriaPanel(index);
         shiftedComp.setComponentIndex(index);
      }
   }

   protected void moveSortCriteriaPanelDown(SortCriteriaPanel comp)
   {
      int index = comp.getComponentIndex();

      if (index < getSortCriteriaCount())
      {
         criteriaListPanel.remove(comp);
         criteriaListPanel.add(comp, index+1);
         comp.setComponentIndex(index+1);

         SortCriteriaPanel shiftedComp = 
            getSortCriteriaPanel(index);
         shiftedComp.setComponentIndex(index);
      }
   }

   JComponent criteriaListPanel;

   private JComboBox<Locale> localeBox;
   private JRadioButton letterSortButton, localeSortButton;

   private JCheckBox isCaseSensitiveBox;

   private JRadioButton nullFirstButton, nullLastButton;

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

class SortCriteriaPanel extends JPanel
 implements ActionListener,ListSelectionListener
{
   public SortCriteriaPanel(SortDialog dialog)
   {
      this(dialog, -1);
   }

   public SortCriteriaPanel(SortDialog dialog, int compIndex)
   {
      super(new BorderLayout());
      this.dialog = dialog;

      if (compIndex == -1)
      {
         compIndex = dialog.getSortCriteriaCount();
      }

      DatatoolGuiResources resources = getMessageHandler().getDatatoolGuiResources();

      JPanel mainPanel = new JPanel();
      add(mainPanel, BorderLayout.CENTER);

      headerBox = new JComboBox<DatatoolHeader>();
      headerBox.setAlignmentX(0);
      mainPanel.add(resources.createJLabel(
         "sort.column", headerBox, 0));
      mainPanel.add(headerBox);

      ButtonGroup bg = new ButtonGroup();

      ascendingButton = resources.createJRadioButton(
         "sort", "ascending", bg, null, 0);
      mainPanel.add(ascendingButton);

      descendingButton = resources.createJRadioButton(
         "sort", "descending", bg, null, 0);
      mainPanel.add(descendingButton);

      JPanel buttonPanel = new JPanel();
      add(buttonPanel, BorderLayout.WEST);

      addButton = resources.createActionButton("sort", "add_criteria",
       "increase", true, this, null, true);
      buttonPanel.add(addButton);

      removeButton = resources.createActionButton("sort", "remove_criteria",
       "decrease", true, this, null, true);
      buttonPanel.add(removeButton);

      upButton = resources.createActionButton("sort", "moveup",
       "up", true, this, null, true);
      buttonPanel.add(upButton);

      downButton = resources.createActionButton("sort", "movedown",
       "down", true, this, null, true);
      buttonPanel.add(downButton);

      // Fallbacks

      JComponent comp = new JPanel(new FlowLayout(FlowLayout.LEADING));
      comp.setAlignmentY(0);
      add(comp, BorderLayout.SOUTH);

      fallbackButton = resources.createJCheckBox(
        "sort", "fallbacks", this);
      fallbackButton.setAlignmentY(0);
      comp.add(fallbackButton);

      fallbackComp = new JPanel(new FlowLayout(FlowLayout.LEADING));
      fallbackComp.setAlignmentY(0);
      comp.add(fallbackComp);

      fallbackListModel = new DefaultListModel<DatatoolHeader>();
      fallbackList = new JList<DatatoolHeader>(fallbackListModel);
      fallbackList.setAlignmentY(0);
      fallbackList.setVisibleRowCount(3);
      fallbackList.addListSelectionListener(this);

      JScrollPane sp = new JScrollPane(fallbackList);
      sp.setAlignmentY(0);
      fallbackComp.add(sp);

      fallbackAddButton = resources.createActionButton("sort", "add_fallback",
       "increase", true, this, null, true);
      fallbackAddButton.setAlignmentY(0);
      fallbackComp.add(fallbackAddButton);

      fallbackRemoveButton = resources.createActionButton("sort", "remove_fallback",
       "decrease", true, this, null, true);
      fallbackRemoveButton.setAlignmentY(0);
      fallbackComp.add(fallbackRemoveButton);

      fallbackUpButton = resources.createActionButton("sort", "fallbackup",
       "up", true, this, null, true);
      fallbackUpButton.setAlignmentY(0);
      fallbackComp.add(fallbackUpButton);

      fallbackDownButton = resources.createActionButton("sort", "fallbackdown",
       "down", true, this, null, true);
      fallbackDownButton.setAlignmentY(0);
      fallbackComp.add(fallbackDownButton);


      setComponentIndex(compIndex);

      reset(-1);
   }

   public void setComponentIndex(int compIndex)
   {
      this.compIndex = compIndex;

      removeButton.setEnabled(compIndex > 0);
      upButton.setEnabled(compIndex > 0);
      downButton.setEnabled(compIndex >= 0
        && compIndex < dialog.getSortCriteriaCount()-1);
   }

   public int getComponentIndex()
   {
      return compIndex;
   }

   public SortCriteria getCriteria()
     throws InvalidSyntaxException 
   {
      int colIdx = headerBox.getSelectedIndex();

      if (colIdx < 0)
      {
         return null;
      }

      SortCriteria criteria = new SortCriteria(colIdx, 
         ascendingButton.isSelected());

      if (fallbackButton.isSelected() && !fallbackListModel.isEmpty())
      {
         int[] fallbacks = new int[fallbackListModel.getSize()];

         for (int i = 0; i < fallbacks.length; i++)
         {
            DatatoolHeader header = fallbackListModel.get(i);
            int headerIdx = headers.indexOf(header);

            if (headerIdx == colIdx)
            {
               throw new InvalidSyntaxException(
                 getMessageHandler().getLabelWithValues(
                  "error.field_in_fallbacks", header));
            }

            fallbacks[i] = headerIdx;
         }

         criteria.setFallbackColumns(fallbacks);
      }

      return criteria;
   }

   public void reset(int colIdx)
   {
      reset(null, colIdx);
   }

   public void reset(SortCriteria criteria, int colIdx)
   {
      DatatoolDb db = dialog.getDataBase();

      if (db == null)
      {
         return;
      }

      headers = db.getHeaders();

      headerBox.setModel(
         new DefaultComboBoxModel<DatatoolHeader>(headers));

      boolean asc = true;

      if (criteria != null)
      {
         criteria.isAscending();

         if (colIdx == -1)
         {
            colIdx = criteria.getColumnIndex();
         }
      }

      if (colIdx > -1 || colIdx < db.getColumnCount())
      {
         headerBox.setSelectedIndex(colIdx);
      }

      if (asc)
      {
         ascendingButton.setSelected(true);
      }
      else
      {
         descendingButton.setSelected(true);
      }

      fallbackList.setFixedCellWidth(headerBox.getPreferredSize().width+10);

      int[] fallbacks = criteria == null ? null : criteria.getFallbackColumnIndexes();

      fallbackListModel.clear();

      if (fallbacks != null)
      {
         for (int idx : fallbacks)
         {
            if (idx >= 0 && idx < headers.size())
            {
               fallbackListModel.addElement(headers.get(idx));
            }
         }
      }

      fallbackButton.setSelected(fallbacks != null);

      if (fallbackButton.isSelected())
      {
         updateFallbackButtons();
      }

      fallbackComp.setVisible(fallbackButton.isSelected());
   }

   @Override
   public void valueChanged(ListSelectionEvent e)
   {
      if (fallbackComp.isVisible())
      {
         updateFallbackButtons();
      }
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if ("add_criteria".equals(action))
      {
         SortCriteriaPanel comp = new SortCriteriaPanel(dialog);
         dialog.insertSortCriteriaPanel(comp, compIndex+1);
         dialog.revalidate();
         dialog.pack();
      }
      else if ("remove_criteria".equals(action))
      {
         dialog.removeSortCriteriaPanel(compIndex);
         dialog.revalidate();
         dialog.pack();
      }
      else if ("moveup".equals(action))
      {
         dialog.moveSortCriteriaPanelUp(this);
         dialog.revalidate();
      }
      else if ("movedown".equals(action))
      {
         dialog.moveSortCriteriaPanelDown(this);
         dialog.revalidate();
      }
      else if ("fallbacks".equals(action))
      {
         fallbackComp.setVisible(fallbackButton.isSelected());

         if (fallbackComp.isVisible())
         {
            updateFallbackButtons();
         }

         dialog.revalidate();
         dialog.pack();
      }
      else if ("add_fallback".equals(action))
      {
         addFallback();
      }
      else if ("remove_fallback".equals(action))
      {
         removeSelectedFallback();
      }
      else if ("fallbackup".equals(action))
      {
         moveSelectedFallbackUp();
      }
      else if ("fallbackdown".equals(action))
      {
         moveSelectedFallbackDown();
      }
      else
      {
         getMessageHandler().debug("Unknown action "+action);
      }
   }

   protected void addFallback()
   {
      Vector<DatatoolHeader> sublist
         = new Vector<DatatoolHeader>(headers.capacity());

      for (DatatoolHeader h : headers)
      {
         if (fallbackListModel.indexOf(h) == -1
               && h != headerBox.getSelectedItem())
         {
            sublist.add(h);
         }
      }

      if (sublist.isEmpty())
      {
         getMessageHandler().error(this,
           getMessageHandler().getLabel("error.no_fallbacks_available"));
      }
      else
      {
         Object result = JOptionPane.showInputDialog(this,
           getMessageHandler().getLabel("sort.fallback_selection.field"),
           getMessageHandler().getLabel("sort.fallback_selection.title"),
           JOptionPane.PLAIN_MESSAGE, null,
           sublist.toArray(), null);

         if (result != null && result instanceof DatatoolHeader)
         {
            DatatoolHeader header = (DatatoolHeader)result;

            int idx = fallbackList.getMaxSelectionIndex();

            if (idx == -1 || idx == fallbackListModel.getSize()-1)
            {
               fallbackListModel.addElement(header);
               idx = fallbackListModel.getSize()-1;
            }
            else
            {
               idx++;
               fallbackListModel.add(idx, header);
            }

            fallbackList.ensureIndexIsVisible(idx);

            updateFallbackButtons();
         }
      }
   }

   protected void removeSelectedFallback()
   {
      int[] indexes = fallbackList.getSelectedIndices();

      for (int i = indexes.length-1; i >= 0; i--)
      {
         fallbackListModel.remove(indexes[i]);
      }

      fallbackList.clearSelection();
   }

   public void moveSelectedFallbackUp()
   {
      int idx = fallbackList.getSelectedIndex();

      if (idx > 0)
      {
         DatatoolHeader header = fallbackListModel.remove(idx);
         idx--;
         fallbackListModel.add(idx, header);
         fallbackList.setSelectedIndex(idx);
         fallbackList.ensureIndexIsVisible(idx);
      }
   }

   public void moveSelectedFallbackDown()
   {
      int idx = fallbackList.getSelectedIndex();

      if (idx > -1 && idx < fallbackListModel.getSize() - 1)
      {
         DatatoolHeader header = fallbackListModel.remove(idx);
         idx++;
         fallbackListModel.add(idx, header);
         fallbackList.setSelectedIndex(idx);
         fallbackList.ensureIndexIsVisible(idx);
      }
   }

   protected void updateFallbackButtons()
   {
      int minIdx = fallbackList.getMinSelectionIndex();
      int maxIdx = fallbackList.getMaxSelectionIndex();

      if (minIdx == -1)
      {
         fallbackRemoveButton.setEnabled(false);
         fallbackUpButton.setEnabled(false);
         fallbackDownButton.setEnabled(false);
      }
      else
      {
         fallbackRemoveButton.setEnabled(true);

         if (maxIdx == minIdx)
         {
            fallbackUpButton.setEnabled(minIdx > 0);
            fallbackDownButton.setEnabled(maxIdx < fallbackListModel.size()-1);
         }
         else
         {
            fallbackUpButton.setEnabled(false);
            fallbackDownButton.setEnabled(false);
         }
      }
   }

   public MessageHandler getMessageHandler()
   {
      return dialog.getMessageHandler();
   }

   @Override
   public String toString()
   {
      return String.format("%s[compIndex=%d,ascending=%s,header:%s]",
        getClass().getSimpleName(), compIndex,
         ascendingButton.isSelected(), headerBox.getSelectedItem());
   }

   SortDialog dialog;
   int compIndex;

   Vector<DatatoolHeader> headers;

   JComboBox<DatatoolHeader> headerBox;
   JRadioButton ascendingButton, descendingButton;

   JButton addButton, removeButton, upButton, downButton;

   JCheckBox fallbackButton;
   JComponent fallbackComp;
   JList<DatatoolHeader> fallbackList;
   DefaultListModel<DatatoolHeader> fallbackListModel;

   JButton fallbackAddButton, fallbackRemoveButton, fallbackUpButton,
     fallbackDownButton;
}
