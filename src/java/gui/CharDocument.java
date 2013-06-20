package com.dickimawbooks.datatooltk.gui;

import javax.swing.text.*;

/**
 * Document that only allows a single character.
 * @author Nicola L C Talbot
 */

public class CharDocument extends PlainDocument
{
   public void insertString(int offs, String str, AttributeSet a)
      throws BadLocationException
   {
      if (str == null) return;

      String oldString = getText(0, getLength());
      String newString = oldString.substring(0,offs)
                       +str+oldString.substring(offs);

      if (newString.length() <= 1)
      {
         super.insertString(offs, str, a);
      }
   }
}
