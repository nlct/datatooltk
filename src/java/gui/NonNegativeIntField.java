package com.dickimawbooks.datatooltk.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

/**
 * Text field that only allows non-negative integers.
 * @author Nicola L C Talbot
 */
public class NonNegativeIntField extends JTextField
{
   /**
    * Initialise with given value.
    * @param defval the initial value of this text field
    */
   public NonNegativeIntField(int defval)
   {
      super(""+defval, 3);
      setHorizontalAlignment(JTextField.RIGHT);
   }

   protected Document createDefaultModel()
   {
      return new NonNegativeIntDocument();
   }

   public void setValue(int val)
   {
      super.setText(""+val);
   }

   public int getValue()
   {
      try
      {
         int i = Integer.parseInt(getText());
         if (i >= 0) return i;
         return 0;
      }
      catch (NumberFormatException e)
      {
         return 0;
      }
      catch (NullPointerException e)
      {
         return 0;
      }
   }
}

