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

