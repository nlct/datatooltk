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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.text.CollationKey;
import java.text.Collator;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.NumberFormat;

import com.dickimawbooks.texparserlib.TeXParser;
import com.dickimawbooks.texparserlib.Comment;
import com.dickimawbooks.texparserlib.TeXObject;
import com.dickimawbooks.texparserlib.TeXObjectList;
import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.texparserlib.latex.datatool.DataElement;
import com.dickimawbooks.texparserlib.latex.datatool.DataNumericElement;
import com.dickimawbooks.texparserlib.latex.datatool.DatumElement;

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

   public boolean isNullOrEmpty()
   {
      return isNull() || stringValue.isEmpty();
   }

   public static Datum valueOf(TeXObject entryContents, TeXParser parser,
     DatatoolSettings settings)
   {
      DatumType elemType = DatumType.UNKNOWN;
      Number elemValue = null;
      String sym = null;
      TeXObject content = entryContents;

      if (entryContents instanceof DatumElement)
      {
         DatumElement elem = (DatumElement)entryContents;
         content = elem.getOriginal();
         elemType = elem.getDatumType();

         if (elemType.isNumeric())
         {
            elemValue = elem.getNumber();

            if (elemValue == null)
            {
               elemType = DatumType.STRING;
            }
         }

         if (elemType == DatumType.CURRENCY)
         {
            TeXObject obj = elem.getCurrencySymbol();

            if (obj == null)
            {
               elemType = DatumType.DECIMAL;
            }
            else
            {
               sym = obj.toString(parser);
            }
         }
      }
      else if (entryContents instanceof DataElement)
      {
         DataElement elem = (DataElement)entryContents;
         content = elem.getContent(parser);
         elemType = elem.getDatumType();

         if (elemType == DatumType.CURRENCY)
         {
            TeXObject obj = elem.getCurrencySymbol();

            if (obj == null)
            {
               elemType = DatumType.DECIMAL;
            }
            else
            {
               sym = obj.toString(parser);
            }
         }

         if (elem instanceof DataNumericElement)
         {
            DataNumericElement numElem = (DataNumericElement)elem;

            switch (elemType)
            {
               case INTEGER:
                 elemValue = Integer.valueOf(numElem.intValue());
               break;
               case CURRENCY:
               case DECIMAL:
                 elemValue = Double.valueOf(numElem.doubleValue());
               break;
            }
         }
      }

      if (parser.isStack(content) && !content.isEmpty())
      {
         TeXObjectList list = (TeXObjectList)content;
         TeXObject obj = list.lastElement();

         if (obj instanceof Comment
             && ((Comment)obj).getText().trim().isEmpty())
         {
            list.remove(list.size()-1);
         }
      }

      String text = content.toString(parser);

      if (elemType == DatumType.UNKNOWN)
      {
         return Datum.valueOf(text, settings);
      }
      else
      {
         return new Datum(elemType, text, sym, elemValue, settings);
      }
   }

   public static Datum valueOf(String text, DatatoolSettings settings)
   {
      // Is there a trailing empty comment?

      Matcher m = TRAILING_EMPTY_COMMENT_PATTERN.matcher(text);

      if (m.matches())
      {
         text = m.group(1);
      }

      if (text.isEmpty())
      {
         return new Datum(settings);
      }

      if (text.equals(DatatoolDb.NULL_VALUE))
      {
         return createNull(settings);
      }

      // First try if the text is formatted according to the
      // numeric locale's currency.

      Locale numLocale = settings.getNumericLocale();
      NumberFormat currFmt = NumberFormat.getCurrencyInstance(numLocale);

      try
      {
         Number num = currFmt.parse(text);
         String sym = currFmt.getCurrency().getSymbol(numLocale);

         if (sym.equals("$"))
         {
            sym = "\\$";
            text.replace("\\$", "\\\\$");
         }

         return new Datum(text, sym, num, settings);
      }
      catch (ParseException e)
      {// not in the locale's currency format
      }

      // Does text start with a known currency symbol?

      String currencySym = null;
      int idx = 0;
      int sign = 0;

      int cp = text.codePointAt(0);
      String str = text;

      if (cp == '+' || cp == '-')
      {
         str = text.substring(1);
         sign = cp;
      }

      for (int i = 0, n = settings.getCurrencyCount(); i < n; i++)
      {
         String sym = settings.getCurrency(i);

         if (str.startsWith(sym))
         {
            currencySym = sym;
            idx = sym.length();

            if (sym.matches(".*\\\\[a-zA-Z]+\\s*"))
            {
               while (idx < str.length())
               {
                  cp = str.codePointAt(idx);

                  if (Character.isWhitespace(cp))
                  {
                     idx += Character.charCount(cp);
                  }
                  else
                  {
                     break;
                  }
               }
            }

            break;
         }
      }

      if (idx > 0)
      {
         if (sign != 0) idx++;
      }
      else
      {
         sign = 0;
      }

      ParsePosition pos = new ParsePosition(idx);

      NumberFormat numfmt = settings.getNumericParser();

      Number num = numfmt.parse(text, pos);

      if (sign == '-')
      {
         num = Double.valueOf(-num.doubleValue());
      }

      if (pos.getErrorIndex() == -1 && pos.getIndex() == text.length())
      {
         return new Datum(text, currencySym, num, settings);
      }
      else
      {
         return new Datum(text, settings);
      }
   }

   /**
    * Update type without altering text, currency or number.
    */ 
   public void setDatumType(DatumType type)
   {
      this.type = type;
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
            numStr = String.format((Locale)null, "%d", intValue());
         break;
         case DECIMAL:
         case CURRENCY:
            numStr = String.format((Locale)null, "%g", doubleValue());
         break;
      }

      return String.format((Locale)null, "{%s}{%s}{%s}{%d}",
        stringValue, numStr, currencySymbol == null ? "" : currencySymbol,
        type.getValue());
   }

   public boolean isNumeric()
   {
      return numValue != null && type.isNumeric();
   }

   public boolean overrides(DatumType other)
   {
      return type.overrides(other);
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

      if (isNull())
      {
         if (other.isNull()) return 0;

         return settings.isNullFirst() ? -1 : 1;
      }
      else if (other.isNull())
      {
         return settings.isNullFirst() ? 1 : -1;
      }

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

      if (result == 0 && 
          (!compareType.isNumeric() || getDatumType() != other.getDatumType()))
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

      if (isNull())
      {
         if (other.isNull()) return 0;

         return settings.isNullFirst() ? -1 : 1;
      }
      else if (other.isNull())
      {
         return settings.isNullFirst() ? 1 : -1;
      }

      if (isNumeric() && other.isNumeric())
      {
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
              // currency symbol is ignored unless values are equal
              result = Double.compare(doubleValue(), other.doubleValue());
         }
      }
      else
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

      if (result == 0)
      {
         if (type == DatumType.CURRENCY || other.type == DatumType.CURRENCY)
         {
            result = compareCurrencySymbols(other);
         }

         if (result == 0)
         {
            result = stringValue.compareTo(other.stringValue);
         }
      }

      return result;
   }

   @Override
   public boolean equals(Object obj)
   {
      if (obj == null || !(obj instanceof Datum)) return false;

      Datum other = (Datum)obj;

      if (!isNumeric() || !other.isNumeric())
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

   /**
    * Create a new instance of a numeric type.
    */ 
   public static Datum format(DatumType type, String currencySym, Number num,
      DatatoolSettings settings)
   {
      String text = "";

      if (type == DatumType.DECIMAL && settings.useSIforDecimals())
      {
         text = String.format("\\num{%g}", num.doubleValue());
      }
      else
      {
         NumberFormat numfmt = settings.getNumericFormatter(type);

         switch (type)
         {
            case INTEGER:
              text = numfmt.format(num.intValue());
            break;
            case DECIMAL:
              text = numfmt.format(num.doubleValue());
            break;
            case CURRENCY:
              text = numfmt.format(num.doubleValue());
              if (currencySym == null)
              {
                 currencySym = numfmt.getCurrency().getSymbol();
              }
              else
              {
                 text = text.replace(numfmt.getCurrency().getSymbol(), currencySym);
              }
            break;
         }
      }

      return new Datum(text, currencySym, num, settings);
   }

   public void setCollationKey(CollationKey key)
   {
      collationKey = key;
   }

   public CollationKey getCollationKey()
   {
      return collationKey;
   }

   public void setStringSort(String sortValue)
   {
      stringSort = sortValue;
   }

   public String getStringSort()
   {
      return stringSort;
   }

   public void setNumericSort(Number num)
   {
      numericSort = num;
   }

   public Number getNumericSort()
   {
      return numericSort;
   }

   private DatumType type;
   private String stringValue;
   private String currencySymbol;
   private Number numValue;
   DatatoolSettings settings;

   CollationKey collationKey;
   String stringSort;
   Number numericSort;

   public static int TEX_MAX_INT = 2147483647;
   public static final Pattern TRAILING_EMPTY_COMMENT_PATTERN = 
     Pattern.compile("(^|.*[^\\\\])%\\s*");
}
