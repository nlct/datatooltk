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

import java.util.*;
import java.util.regex.*;
import java.text.DecimalFormat;

/**
 * Class representing currency as per datatool currency data type.
 */
public class Currency extends Number
  implements Comparable<Number>
{
   public Currency(String currency, float value)
   {
      this.currency = currency;
      this.value = value;
   }

   public double doubleValue()
   {
      return (double)value;
   }

   public float floatValue()
   {
      return (float)value;
   }

   public int intValue()
   {
      return (int)value;
   }

   public long longValue()
   {
      return (long)value;
   }

   public String toString()
   {
      return (currency == null ? "" : currency) + format.format(value);
   }

   public int compareTo(Number object)
   {
      float num = object.floatValue();

      if (value == num)
      {
         return 0;
      }
      else if (value < num)
      {
         return -1;
      }
      else
      {
         return 1;
      }
   }

   private float value;

   private String currency;

   private static final DecimalFormat format = new DecimalFormat("0.00");

}
