/*
    Copyright (C) 2024 Nicola L.C. Talbot
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

import java.text.Collator;
import java.text.ParsePosition;
import java.text.NumberFormat;

import com.dickimawbooks.texparserlib.TeXParser;
import com.dickimawbooks.texparserlib.Comment;
import com.dickimawbooks.texparserlib.TeXObject;
import com.dickimawbooks.texparserlib.TeXObjectList;
import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.texparserlib.latex.datatool.DataElement;
import com.dickimawbooks.texparserlib.latex.datatool.DataNumericElement;

public class Datum implements Comparable<Datum>
{
   public Datum(DatatoolSettings settings)
   {
      this(DatumType.UNKNOWN, "", null, null, settings);
   }

   public Datum(String stringValue, DatatoolSettings settings)
   {
      this(DatumType.STRING, stringValue, null, null, settings);
   }

   public Datum(DatumType type, String stringValue, String currencySymbol,
      int num, DatatoolSettings settings)
   {
      this(DatumType.STRING, stringValue, currencySymbol, Integer.valueOf(num),
       settings);
   }

   public Datum(DatumType type, String stringValue, String currencySymbol,
      double num, DatatoolSettings settings)
   {
      this(DatumType.STRING, stringValue, currencySymbol, Double.valueOf(num),
       settings);
   }

   public Datum(String stringValue, String currencySymbol, Number num,
      DatatoolSettings settings)
   {
      this.settings = settings;
      this.stringValue = stringValue;
      this.currencySymbol = currencySymbol;
      this.numValue = num;

      if (num == null)
      {
         type = DatumType.STRING;
      }
      else if (currencySymbol != null)
      {
         type = DatumType.CURRENCY;
      }
      else if (num instanceof Double || num instanceof Float)
      {
         type = DatumType.DECIMAL;
      }
      else
      {
         type = DatumType.INTEGER;
      }
   }

   public Datum(DatumType type, String stringValue, String currencySymbol,
     Number numValue, DatatoolSettings settings)
   {
      this.settings = settings;
      this.type = type;
      this.stringValue = stringValue;
      this.currencySymbol = currencySymbol;
      this.numValue = numValue;
   }

   public static Datum createNull(DatatoolSettings settings)
   {
      return new Datum(DatumType.UNKNOWN, DatatoolDb.NULL_VALUE, null, null,
        settings);
   }

   public boolean isNull()
   {
      return stringValue.equals(DatatoolDb.NULL_VALUE);
   }

   public static Datum valueOf(TeXObject entryContents, TeXParser parser,
     DatatoolSettings settings)
   {
      Datum datum;

      if (parser.isStack(entryContents) && !entryContents.isEmpty())
      {
         TeXObjectList list = (TeXObjectList)entryContents;
         TeXObject obj = list.lastElement();

         if (obj instanceof Comment
             && ((Comment)obj).isEmpty())
         {
            list.remove(list.size()-1);
         }
      }

      if (entryContents.isEmpty())
      {
         datum = new Datum(settings);
      }
      else if (entryContents instanceof DataElement)
      {
         DataElement elem = (DataElement)entryContents;
         DatumType elemType = elem.getDatumType();
         TeXObject texSym = elem.getCurrencySymbol();
         Number num = null;

         if (elem instanceof DataNumericElement)
         {
            DataNumericElement numElem = (DataNumericElement)elem;

            switch (elemType)
            {
               case INTEGER:
                 num = Integer.valueOf(numElem.intValue());
               break;
               case DECIMAL:
               case CURRENCY:
                 num = Double.valueOf(numElem.doubleValue());
               break;
            }
         }

         datum = new Datum(elemType, entryContents.toString(parser),
           texSym == null ? null : texSym.toString(parser), 
           num, settings);
      }
      else
      {
         datum = Datum.valueOf(entryContents.toString(parser), settings);
      }

      return datum;
   }

   public static Datum valueOf(String text, DatatoolSettings settings)
   {
      if (text.isEmpty()) return new Datum(settings);

      // Does text start with a known currency symbol?

      String currencySym = null;
      int idx = 0;

      for (int i = 0, n = settings.getCurrencyCount(); i < n; i++)
      {
         String sym = settings.getCurrency(i);

         if (text.startsWith(sym))
         {
            currencySym = sym;
            idx = sym.length();
            break;
         }
      }

      ParsePosition pos = new ParsePosition(idx);

      NumberFormat numfmt = NumberFormat.getInstance(settings.getNumericLocale());

      Number num = numfmt.parse(text, pos);

      if (pos.getErrorIndex() == -1 && pos.getIndex() == text.length())
      {
         return new Datum(text, currencySym, num, settings);
      }
      else
      {
         return new Datum(text, settings);
      }
   }

   public DatumType getDatumType()
   {
      return type;
   }

   public int getType()
   {
      return type.getValue();
   }

   public String getCurrencySymbol()
   {
      return currencySymbol;
   }

   public String getText()
   {
      return stringValue;
   }

   /**
    * Update text without changing the type or reparsing.
    */ 
   public void setText(String text)
   {
      this.stringValue = text;
   }

   /**
    * Update the numeric value without changing the type, string value or
    * currency symbol.
    */
   public void setNumeric(Number num)
   {
      this.numValue = num;
   }

   /**
    * Update the currency symbol without changing the type, string value or
    * numeric value.
    */
   public void setCurrencySymbol(String sym)
   {
      this.currencySymbol = sym;
   }

   @Override
   public String toString()
   {
      return stringValue;
   }

   public String getDatumArgs()
   {
      String numStr = "";

      switch (type)
      {
         case INTEGER:
            numStr = ""+intValue();
         break;
         case DECIMAL:
         case CURRENCY:
            numStr = ""+doubleValue();
         break;
      }

      return String.format("{%s}{%s}{%s}{%d}",
        stringValue, numStr, currencySymbol == null ? "" : currencySymbol,
        type.getValue());
   }

   public Number getNumber()
   {
      return numValue;
   }

   public int intValue()
   {
      return numValue == null ? 0 : numValue.intValue();
   }

   public double doubleValue()
   {
      return numValue == null ? 0.0 : numValue.doubleValue();
   }

   public int compareCurrencySymbols(Datum other)
   {
      String sym1 = currencySymbol == null ? "" : currencySymbol;
      String sym2 = other.currencySymbol == null ? "" : other.currencySymbol;

      return sym1.compareTo(sym2);
   }

   public int compareTo(Datum other, DatumType compareType, boolean caseSensitive)
   {
      int result=0;

      switch (compareType)
      {
         case INTEGER:
           result = Integer.compare(intValue(), other.intValue());
         break;
         case DECIMAL:
           result = Double.compare(doubleValue(), other.doubleValue());
         break;
         case CURRENCY:
           result = Double.compare(doubleValue(), other.doubleValue());

           if (result == 0)
           {
              result = compareCurrencySymbols(other);
           }
         break;
      }

      if (result == 0)
      {
         Collator collator = settings.getSortCollator();

         if (caseSensitive)
         {
            if (collator == null)
            {
               result = stringValue.compareTo(other.stringValue);
            }
            else
            {
               int orgStrength = collator.getStrength();
               collator.setStrength(Collator.TERTIARY);
               result = collator.compare(stringValue, other.stringValue);
               collator.setStrength(orgStrength);
            }
         }
         else if (collator == null)
         {
            result = stringValue.toLowerCase().compareTo(
               other.stringValue.toLowerCase());
         }
         else
         {
            result = collator.compare(stringValue, other.stringValue);
         }
      }

      return result;
   }

   @Override
   public int compareTo(Datum other)
   {
      int result = 0;

      if ((type == DatumType.UNKNOWN || type == DatumType.STRING)
        ||(other.type == DatumType.UNKNOWN || other.type == DatumType.STRING)
         )
      {
         Collator collator = settings.getSortCollator();

         if (collator == null)
         {
            return stringValue.compareTo(other.stringValue);
         }
         else
         {
            result = collator.compare(stringValue, other.stringValue);
         }
      }

      switch (type)
      {
         case INTEGER:
           if (other.type == DatumType.INTEGER)
           {
              result = Integer.compare(intValue(), other.intValue());
              break;
           }
         case DECIMAL:
         case CURRENCY:
           result = Double.compare(doubleValue(), other.doubleValue());
      }

      if (result == 0)
      {
         result = stringValue.compareTo(other.stringValue);
      }

      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null || !(obj instanceof Datum)) return false;

      Datum other = (Datum)obj;

      if (type == DatumType.STRING || type == DatumType.UNKNOWN
        || other.type == DatumType.STRING || other.type == DatumType.UNKNOWN)
      {
         return stringValue.equals(other.stringValue);
      }

      if ((type == DatumType.CURRENCY || other.type == DatumType.CURRENCY)
          && compareCurrencySymbols(other) != 0)
      {
         return false;
      }

      return numValue.equals(other.numValue);
   }

   private DatumType type;
   private String stringValue;
   private String currencySymbol;
   private Number numValue;
   DatatoolSettings settings;
}
