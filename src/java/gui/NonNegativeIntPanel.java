package com.dickimawbooks.datatooltk.gui;

import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

/**
 * Panel for specifying a non-negative integers.
 * @author Nicola L C Talbot
 */

public class NonNegativeIntPanel extends JPanel
{
   public NonNegativeIntPanel(String applabel, int defValue)
   {
      super();
      setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

      label = new JLabel(DatatoolTk.getLabel(applabel));
      label.setDisplayedMnemonic(
         DatatoolTk.getMnemonic(applabel));

      text = new NonNegativeIntField(defValue);

      add(label);
      add(text);

      label.setLabelFor(text);
   }

   public NonNegativeIntPanel(String applabel)
   {
      this(applabel, 0);
   }

   public Document getDocument()
   {
      return text.getDocument();
   }

   public NonNegativeIntField getTextField()
   {
      return text;
   }

   public int getValue()
   {
      return text.getValue();
   }

   public void setValue(int val)
   {
      text.setValue(val);
      text.setCaretPosition(0);
   }

   public String getLabelText()
   {
      return label.getText();
   }

   public void setEnabled(boolean flag)
   {
      if (label != null) label.setEnabled(flag);
      text.setEnabled(flag);
   }

   private JLabel label;
   private NonNegativeIntField text;
}
