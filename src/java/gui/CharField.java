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

import java.awt.Dimension;
import java.awt.FontMetrics;
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

   public CharField(int defVal)
   {
      super(new String(Character.toChars(defVal)), 1);
      setHorizontalAlignment(CENTER);
   }

   public CharField()
   {
      super(1);
      setHorizontalAlignment(CENTER);
   }

   protected Document createDefaultModel()
   {
      return new CharDocument();
   }

   public void setValue(int val)
   {
      if (val > 0)
      {
         super.setText(new String(Character.toChars(val)));
      }
      else
      {
         super.setText("");
      }
   }

   public int getValue()
   {
      String text = super.getText();

      if (text.length() == 0)
      {
         return 0;
      }

      return text.codePointAt(0);
   }

   public Dimension getMaximumSize()
   {
      Dimension maxDim = super.getMaximumSize();

      FontMetrics fm = getFontMetrics(getFont());

      maxDim.width = Math.max(20, fm.getMaxAdvance());

      return maxDim;
   }

   public Dimension getPreferredSize()
   {
      Dimension maxDim = super.getPreferredSize();

      FontMetrics fm = getFontMetrics(getFont());

      maxDim.width = Math.max(20, fm.getMaxAdvance());

      return maxDim;
   }

}
