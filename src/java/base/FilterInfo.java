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
package com.dickimawbooks.datatooltk.base;

import com.dickimawbooks.texjavahelplib.InvalidSyntaxException;

public class FilterInfo
{
   public FilterInfo(MessageHandler messageHandler, String label, 
      String op, String value)
    throws InvalidSyntaxException
   {
      operator = -1;

      for (int i = 0; i < DataFilter.OPERATORS.length; i++)
      {
         if (op.equals(DataFilter.OPERATORS[i]))
         {
            operator = i;
            break;
         }
      }

      if (operator == -1)
      {
         throw new InvalidSyntaxException(messageHandler.getLabelWithValues(
            "error.syntax.invalid_filter_operator", op));
      }

      this.label = label;
      this.value = value;
   }

   public String getLabel()
   {
      return label;
   }

   public String getValue()
   {
      return value;
   }

   public int getOperator()
   {
      return operator;
   }

   public String toString()
   {
      return String.format("'%s' %s '%s'", label, 
        DataFilter.OPERATORS[operator], value);
   }

   private String label, value;
   private int operator;
}
