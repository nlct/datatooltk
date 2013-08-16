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
