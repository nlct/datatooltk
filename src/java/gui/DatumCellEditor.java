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
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import javax.swing.text.*;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
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
      panel.setAlignmentX(JComponent.LEFT_ALIGNMENT);

      midComp = Box.createVerticalBox();
      midComp.setAlignmentX(JComponent.LEFT_ALIGNMENT);

      JComponent rowComp;

      rowComp = createRow();
      midComp.add(rowComp);

      typeBox = new DatumTypeComboBox(gui.getSettings());
      typeBox.addItemListener(this);

      rowComp.add(resources.createJLabel("celledit.type", typeBox));
      rowComp.add(typeBox);

      rowComp = createRow();
      midComp.add(rowComp);

      currencyField = new JTextField();

      rowComp.add(resources.createJLabel("celledit.currency", currencyField));
      rowComp.add(currencyField);

      rowComp = createRow();
      midComp.add(rowComp);

      spinnerModel = new SpinnerNumberModel();
      numField = new JSpinner(spinnerModel);

      rowComp.add(resources.createJLabel("celledit.numeric", numField));
      rowComp.add(numField);

      midComp.add(Box.createVerticalGlue());

      autoReparseBox = resources.createJCheckBox("celledit", "reparse", this);
      autoReparseBox.setSelected(true);
   }

   protected JComponent createRow()
   {
      JComponent comp = new JPanel();

      comp.setAlignmentX(JComponent.LEFT_ALIGNMENT);

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
         updateMidComps();
      }
   }

   protected void updateMidComps()
   {
      boolean autoOn = autoReparseBox.isSelected();

      midComp.setVisible(!autoOn);
      currencyField.setEnabled(false);
      numField.setEnabled(false);

      if (!autoOn)
      {
         switch (typeBox.getSelectedType())
         {
            case CURRENCY:
              currencyField.setEnabled(true);
            case INTEGER:
            case DECIMAL:
              numField.setEnabled(true);
         }
      }
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

      switch (type)
      {
         case CURRENCY:
           currencySym = currencyField.getText();
         // fall through
         case INTEGER:
         case DECIMAL:
           num = spinnerModel.getNumber();
      }

      return new Datum(type, getTextField().getText(), currencySym, num,
        gui.getSettings());
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
         spinnerModel.setValue(Integer.valueOf(0));
      }
      else
      {
         spinnerModel.setValue(num);
      }

      JTextField textField = 
        (JTextField)super.getTableCellEditorComponent(table,
           text, isSelected, row, column);

      textField.setHorizontalAlignment(JTextField.TRAILING);

      boolean enable = !autoReparseBox.isSelected();

      midComp.setVisible(enable);
      numField.setEnabled(enable && num != null);
      currencyField.setEnabled(enable && currencySym != null);

      panel.add(textField, BorderLayout.NORTH);
      panel.add(midComp, BorderLayout.CENTER);
      panel.add(autoReparseBox, BorderLayout.SOUTH);

      return panel;
   }

   private JComponent panel, midComp;
   private DatumTypeComboBox typeBox;
   private DatatoolGUI gui;
   private JTextField currencyField;
   private JSpinner numField;
   private SpinnerNumberModel spinnerModel;
   private JCheckBox autoReparseBox;
}
