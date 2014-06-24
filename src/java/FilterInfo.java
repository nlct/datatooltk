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
package com.dickimawbooks.datatooltk;

import com.dickimawbooks.datatooltk.io.InvalidSyntaxException;

public class FilterInfo
{
   public FilterInfo(String label, String op, String value)
    throws InvalidSyntaxException
   {
      if (op.equals("eq"))
      {
         operator = DataFilter.OPERATOR_EQ;
      }
      else if (op.equals("le"))
      {
         operator = DataFilter.OPERATOR_LE;
      }
      else if (op.equals("lt"))
      {
         operator = DataFilter.OPERATOR_LT;
      }
      else if (op.equals("ge"))
      {
         operator = DataFilter.OPERATOR_GE;
      }
      else if (op.equals("gt"))
      {
         operator = DataFilter.OPERATOR_GT;
      }
      else if (op.equals("ne"))
      {
         operator = DataFilter.OPERATOR_NE;
      }
      else if (op.equals("regex"))
      {
         operator = DataFilter.OPERATOR_REGEX;
      }
      else
      {
         throw new InvalidSyntaxException(DatatoolTk.getLabelWithValue(
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

   private String label, value;
   private int operator;
}
