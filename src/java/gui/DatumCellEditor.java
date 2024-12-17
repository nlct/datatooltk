/*
    Copyright (C) 2024 Nicola L.C. Talbot
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

import java.util.EventObject;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.text.*;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.texparserlib.latex.datatool.Julian;
import com.dickimawbooks.datatooltk.*;

/**
 * Cell editor for datum value.
 */

public class DatumCellEditor extends DefaultCellEditor
 implements ActionListener,ItemListener
{
   public DatumCellEditor(DatatoolGUI gui)
   {
      super(new JTextField());
      this.gui = gui;
      DatatoolGuiResources resources = gui.getResources();

      panel = new JPanel(new BorderLayout());

      Box midComp = Box.createVerticalBox();
      midCompSp = new JScrollPane(midComp);

      JComponent rowComp;

      rowComp = createRow();
      midComp.add(rowComp);

      typeBox = new DatumTypeComboBox(resources);
      typeBox.addItemListener(this);

      rowComp.add(resources.createJLabel("celledit.type", typeBox));
      rowComp.add(typeBox);

      midComp.add(createTemporalComponents());

      currencyRow = createRow();
      midComp.add(currencyRow);

      currencyField = new JTextField();

      currencyRow.add(resources.createJLabel("celledit.currency", currencyField));
      currencyRow.add(currencyField);

      valueCardLayout = new CardLayout();
      valueRow = new JPanel(valueCardLayout);
      midComp.add(valueRow);

      rowComp = createRow();
      valueRow.add(rowComp, "int");

      intSpinnerModel = new SpinnerNumberModel(
        0, - Datum.TEX_MAX_INT, Datum.TEX_MAX_INT, 1);
      intSpinner = new JSpinner(intSpinnerModel);
      JComponent editor = intSpinner.getEditor();
      JFormattedTextField tf = ((JSpinner.DefaultEditor)editor).getTextField();
      tf.setColumns(5);

      rowComp.add(resources.createJLabel("celledit.numeric", intSpinner));
      rowComp.add(intSpinner);

      rowComp = createRow();
      valueRow.add(rowComp, "dec");

      decimalField = new JTextField("0.00");
      decimalField.setColumns(6);
      rowComp.add(resources.createJLabel("celledit.numeric", decimalField));
      rowComp.add(decimalField);

      rowComp = createRow();
      midComp.add(rowComp);

      autoReformatBox = resources.createJCheckBox("celledit", "reformat", this);
      autoReformatBox.setSelected(true);
      rowComp.add(autoReformatBox);

      midComp.add(Box.createVerticalGlue());

      autoReparseBox = resources.createJCheckBox("celledit", "reparse", this);
      autoReparseBox.setSelected(true);
   }

   protected JComponent createTemporalComponents()
   {
      DatatoolGuiResources resources = gui.getResources();

      temporalCardLayout = new CardLayout();
      temporalComp = new JPanel(temporalCardLayout);

      Locale locale = resources.getSettings().getDateTimeLocale();
      String pattern;
      DateFormat df;

      JComponent datetimeRow = createRow();
      temporalComp.add(datetimeRow, "datetime");

      df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);

      if (df instanceof SimpleDateFormat)
      {
         pattern = ((SimpleDateFormat)df).toPattern();
      }
      else
      {
         pattern = "yyyy-mm-dd hh:mm:ss";
      }

      datetimeSpinnerModel = new SpinnerDateModel();
      datetimeSpinner = new JSpinner(datetimeSpinnerModel);
      datetimeSpinner.setEditor(new JSpinner.DateEditor(datetimeSpinner, pattern));

      datetimeRow.add(resources.createJLabel("celledit.datetime", datetimeSpinner));
      datetimeRow.add(datetimeSpinner);

      JComponent dateRow = createRow();
      temporalComp.add(dateRow, "date");

      df = DateFormat.getDateInstance(DateFormat.SHORT, locale);

      if (df instanceof SimpleDateFormat)
      {
         pattern = ((SimpleDateFormat)df).toPattern();
      }
      else
      {
         pattern = "yyyy-mm-dd";
      }

      dateSpinnerModel = new SpinnerDateModel();
      dateSpinner = new JSpinner(dateSpinnerModel);
      dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, pattern));

      dateRow.add(resources.createJLabel("celledit.date", dateSpinner));
      dateRow.add(dateSpinner);

      JComponent timeRow = createRow();
      temporalComp.add(timeRow, "time");

      df = DateFormat.getTimeInstance(DateFormat.SHORT, locale);

      if (df instanceof SimpleDateFormat)
      {
         pattern = ((SimpleDateFormat)df).toPattern();
      }
      else
      {
         pattern = "hh:mm:ss";
      }

      timeSpinnerModel = new SpinnerDateModel();
      timeSpinner = new JSpinner(timeSpinnerModel);
      timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, pattern));

      timeRow.add(resources.createJLabel("celledit.time", timeSpinner));
      timeRow.add(timeSpinner);

      return temporalComp;
   }

   protected JComponent createRow()
   {
      JComponent comp = new JPanel(new FlowLayout(FlowLayout.LEADING));

      return comp;
   }

   public JTextField getTextField()
   {
      return (JTextField)getComponent();
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if ("reparse".equals(action))
      {
         updateMidComps();
      }
   }

   @Override
   public void itemStateChanged(ItemEvent evt)
   {
      if (evt.getStateChange() == ItemEvent.SELECTED)
      {
         intSpinnerModel.setValue(Integer.valueOf(orgValue.intValue()));
         decimalField.setText(orgValue.toString());
         updateMidComps();
      }
   }

   protected void updateMidComps()
   {
      boolean autoOn = autoReparseBox.isSelected();

      midCompSp.setVisible(!autoOn);
      currencyRow.setVisible(false);
      valueRow.setVisible(false);
      temporalComp.setVisible(false);
      autoReformatBox.setVisible(false);

      if (!autoOn)
      {
         DatumType type = typeBox.getSelectedType();

         if (type.isNumeric())
         {
            autoReformatBox.setVisible(true);
            valueRow.setVisible(!type.isTemporal());
            temporalComp.setVisible(!valueRow.isVisible());

            switch (type)
            {
               case INTEGER:
                 valueCardLayout.show(valueRow, "int");
               break;
               case CURRENCY:
                 currencyRow.setVisible(true);
                 // fall through
               case DECIMAL:
                 valueCardLayout.show(valueRow, "dec");
               break;
               case DATE:
                 temporalCardLayout.show(temporalComp, "date");
               break;
               case TIME:
                 temporalCardLayout.show(temporalComp, "time");
               break;
               case DATETIME:
                 temporalCardLayout.show(temporalComp, "datetime");
               break;
            }
         }
      }

      panel.revalidate();
   }

   protected Number getValue()
   {
      DatumType type = typeBox.getSelectedType();

      switch (type)
      {
         case INTEGER:
           return intSpinnerModel.getNumber();
         case CURRENCY:
         case DECIMAL:
           try
           {
              return Double.valueOf(decimalField.getText());
           }
           catch (NumberFormatException e)
           {
           }
         break;
         case DATE:
            return Long.valueOf(dateSpinnerModel.getDate().getTime()); 
         case TIME:
            return Long.valueOf(timeSpinnerModel.getDate().getTime()); 
         case DATETIME:
            return Long.valueOf(datetimeSpinnerModel.getDate().getTime()); 
      }

      return Integer.valueOf(0);
   }

   @Override
   public Object getCellEditorValue()
   {
      if (autoReparseBox.isSelected())
      {
         return Datum.valueOf(getTextField().getText(), gui.getSettings());
      }

      DatumType type = typeBox.getSelectedType();
      String currencySym = null;
      Number num = null;
      Julian julian = null;

      switch (type)
      {
         case INTEGER:
           num = intSpinnerModel.getNumber();
         break;
         case CURRENCY:
           currencySym = currencyField.getText();
         // fall through
         case DECIMAL:
           try
           {
              num = Double.valueOf(decimalField.getText());
           }
           catch (NumberFormatException e)
           {
              num = Double.valueOf(0);
           }
         break;
         case DATE:
           julian = Julian.createDay(dateSpinnerModel.getDate(),
             gui.getSettings().getDateTimeLocale());
         break;
         case TIME:
           julian = Julian.createTime(timeSpinnerModel.getDate(),
             gui.getSettings().getDateTimeLocale());
         break;
         case DATETIME:
           julian = Julian.createDate(datetimeSpinnerModel.getDate(),
             gui.getSettings().getDateTimeLocale());
         break;
      }

      String text = getTextField().getText();

      if (autoReformatBox.isSelected() && type.isNumeric())
      {
         return Datum.format(type, currencySym, num, julian, gui.getSettings());
      }

      return new Datum(type, text, currencySym, num, julian, gui.getSettings());
   }

   @Override
   public Component getTableCellEditorComponent(JTable table,
     Object value, boolean isSelected, int row, int column)
   {
      panel.removeAll();

      Datum datum;

      if (value instanceof Datum)
      {
         datum = (Datum)value;
      }
      else
      {
         datum = Datum.valueOf(value.toString(), gui.getSettings());
      }

      DatumType type = datum.getDatumType();
      String text = datum.getText();
      String currencySym = datum.getCurrencySymbol();
      Number num = datum.getNumber();

      typeBox.setSelectedType(type);

      if (currencySym == null)
      {
         currencyField.setText("");
      }
      else
      {
         currencyField.setText(currencySym);
      }

      if (num == null)
      {
         orgValue = Integer.valueOf(0);
         intSpinnerModel.setValue(orgValue);
         decimalField.setText("0.0");
      }
      else
      {
         orgValue = num;
         intSpinnerModel.setValue(Integer.valueOf(num.intValue()));
         decimalField.setText(num.toString());
      }

      JTextField textField = 
        (JTextField)super.getTableCellEditorComponent(table,
           text, isSelected, row, column);

      textField.setHorizontalAlignment(JTextField.TRAILING);

      updateMidComps();

      panel.add(textField, BorderLayout.NORTH);
      panel.add(midCompSp, BorderLayout.CENTER);
      panel.add(autoReparseBox, BorderLayout.SOUTH);

      textField.requestFocusInWindow();

      return panel;
   }

   private JComponent panel;
   private JScrollPane midCompSp;
   private DatumTypeComboBox typeBox;
   private DatatoolGUI gui;
   private JTextField currencyField;
   private Number orgValue = Integer.valueOf(0);

   private JSpinner intSpinner, dateSpinner, datetimeSpinner, timeSpinner;
   private SpinnerNumberModel intSpinnerModel;
   private JTextField decimalField;
   private CardLayout valueCardLayout, temporalCardLayout;
   private JComponent currencyRow, valueRow, temporalComp;
   private SpinnerDateModel dateSpinnerModel, timeSpinnerModel, datetimeSpinnerModel;

   private JCheckBox autoReparseBox, autoReformatBox;
}
