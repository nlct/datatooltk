package com.dickimawbooks.datatooltk.gui;

import javax.swing.text.*;

/**
 * Document that only allows non-negative integers.
 * @author Nicola L C Talbot
 */
public class NonNegativeIntDocument extends PlainDocument
{
   public void insertString(int offs, String str, AttributeSet a)
      throws BadLocationException
   {
      if (str == null) return;

      String oldString = getText(0, getLength());
      String newString = oldString.substring(0,offs)
                       +str+oldString.substring(offs);
      try
      {
         int i = Integer.parseInt(newString);
         if (i >= 0) super.insertString(offs, str, a);
      }
      catch (NumberFormatException e)
      {
      }
   }
}
