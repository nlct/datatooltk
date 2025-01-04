/*
    Copyright (C) 2024-2025 Nicola L.C. Talbot
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

      temporalComp = new TemporalPanel(gui, "celledit");
      midComp.add(temporalComp);

      currencyRow = createRow();
      midComp.add(currencyRow);

      currencyField = new JTextField(4);

      currencyRow.add(resources.createJLabel("celledit.currency", currencyField));
      currencyRow.add(currencyField);

      valueCardLayout = new CardLayout();
      valueRow = new JPanel(valueCardLayout);
      midComp.add(valueRow);

      intComp = createRow();
      valueRow.add(intComp, "int");

      intSpinnerModel = new SpinnerNumberModel(
        0, - Datum.TEX_MAX_INT, Datum.TEX_MAX_INT, 1);
      intSpinner = new JSpinner(intSpinnerModel);
      JComponent editor = intSpinner.getEditor();
      JFormattedTextField tf = ((JSpinner.DefaultEditor)editor).getTextField();
      tf.setColumns(5);

      intComp.add(resources.createJLabel("celledit.numeric", intSpinner));
      intComp.add(intSpinner);

      decComp = createRow();
      valueRow.add(decComp, "dec");

      decimalField = new JTextField("0.00");
      decimalField.setColumns(6);
      decComp.add(resources.createJLabel("celledit.numeric", decimalField));
      decComp.add(decimalField);

      rowComp = createRow();
      midComp.add(rowComp);

      autoReformatBox = resources.createJCheckBox("celledit", "reformat", this);
      autoReformatBox.setSelected(true);
      rowComp.add(autoReformatBox);

      midComp.add(Box.createVerticalGlue());

      autoReparseBox = resources.createJCheckBox("celledit", "reparse", this);
      autoReparseBox.setSelected(true);
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

         DatumType type = typeBox.getSelectedType();
         wasTemporal = type.isTemporal();
         wasInt = (type == DatumType.INTEGER);
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

         DatumType type = typeBox.getSelectedType();
         wasTemporal = type.isTemporal();
         wasInt = (type == DatumType.INTEGER);
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

            if (type.isTemporal())
            {
               Julian julian;

               if (wasTemporal)
               {
                  julian = temporalComp.getJulian();
               }
               else if (wasInt)
               {
                  julian = Julian.createDay(intSpinnerModel.getNumber().intValue());
               }
               else
               {
                  double num = 0.0;

                  try
                  {
                     num = Double.parseDouble(decimalField.getText());
                  }
                  catch (NumberFormatException e)
                  {
                  }

                  if (num >= -0.5 && num <= 0.5)
                  {
                     julian = Julian.createTime(num);
                  }
                  else
                  {
                     julian = Julian.createDate(num);
                  }
               }

               temporalComp.update(type, julian);
            }

            temporalComp.setVisible(type.isTemporal());
            valueRow.setVisible(!temporalComp.isVisible());

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
         case TIME:
         case DATETIME:
            return temporalComp.getNumeric(type); 
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
         case TIME:
         case DATETIME:
           julian = temporalComp.getJulian(type);
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
      Julian julian = datum.getJulian();

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

      if (julian != null)
      {
         temporalComp.update(type, julian);
      }

      JTextField textField = 
        (JTextField)super.getTableCellEditorComponent(table,
           text, isSelected, row, column);

      textField.setHorizontalAlignment(JTextField.TRAILING);

      wasTemporal = type.isTemporal();
      wasInt = (type == DatumType.INTEGER);

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
   private boolean wasTemporal=false, wasInt = false;

   private JSpinner intSpinner;
   private SpinnerNumberModel intSpinnerModel;
   private JTextField decimalField;
   private CardLayout valueCardLayout;
   private JComponent currencyRow, valueRow, intComp, decComp;
   private TemporalPanel temporalComp;

   private JCheckBox autoReparseBox, autoReformatBox;
}
