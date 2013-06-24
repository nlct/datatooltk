package com.dickimawbooks.datatooltk;

import java.util.*;
import java.util.regex.*;
import java.text.DecimalFormat;

public class Currency extends Number
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

   private float value;

   private String currency;

   private static final DecimalFormat format = new DecimalFormat("0.00");

}
