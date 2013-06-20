package com.dickimawbooks.datatooltk.gui;

import java.awt.Dimension;
import javax.swing.JTextField;
import javax.swing.text.Document;

/**
 * Text field that only allows a single character.
 * @author Nicola L C Talbot
 */

public class CharField extends JTextField
{
   /*
    * Initialise with given value
    */

   public CharField(char defVal)
   {
      super(""+defVal, 1);
   }

   protected Document createDefaultModel()
   {
      return new CharDocument();
   }

   public void setValue(char val)
   {
      super.setText(""+val);
   }

   public char getValue()
   {
      String text = super.getText();

      if (text.length() == 0)
      {
         return (char)0;
      }

      return text.charAt(0);
   }

   public Dimension getMaximumSize()
   {
      return maxDim;
   }

   private Dimension maxDim = new Dimension(20,20);
}
