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

import java.math.BigDecimal;

import com.dickimawbooks.texparserlib.TeXParser;
import com.dickimawbooks.texparserlib.Comment;
import com.dickimawbooks.texparserlib.TeXObject;
import com.dickimawbooks.texparserlib.TeXObjectList;

import com.dickimawbooks.texparserlib.latex.datatool.DataElement;
import com.dickimawbooks.texparserlib.latex.datatool.DataNumericElement;
import com.dickimawbooks.texparserlib.latex.datatool.DataToolBaseSty;
import com.dickimawbooks.texparserlib.latex.datatool.DatumElement;
import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.texparserlib.latex.datatool.Julian;

public class Datum implements Comparable<Datum>
{
   public Datum(DatatoolSettings settings)
   {
      this(DatumType.UNKNOWN, "", null, null, null, settings);
   }

   public Datum(String stringValue, DatatoolSettings settings)
   {
      this(DatumType.STRING, stringValue, null, null, null, settings);
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
      this(stringValue, currencySymbol, num, null, settings);
   }

   public Datum(String stringValue, String currencySymbol, Number num,
      Julian julian, DatatoolSettings settings)
   {
      this.settings = settings;
      this.stringValue = stringValue;
      this.currencySymbol = currencySymbol;
      this.numValue = num;
      this.julian = julian;

      if (julian != null)
      {
         if (julian.hasDate() && julian.hasTime())
         {
            type = DatumType.DATETIME;

            if (num == null)
            {
               num = Double.valueOf(julian.getJulianDate());
            }
         }
         else if (julian.hasTime())
         {
            type = DatumType.TIME;

            if (num == null)
            {
               num = Double.valueOf(julian.getJulianTime());
            }
         }
         else
         {
            type = DatumType.DATE;

            if (num == null)
            {
               num = Integer.valueOf(julian.getJulianDay());
            }
         }
      }
      else if (num == null)
      {
         type = DatumType.STRING;
      }
      else if (currencySymbol != null)
      {
         type = DatumType.CURRENCY;
      }
      else if (num instanceof Integer)
      {
         type = DatumType.INTEGER;
      }
      else
      {
         type = DatumType.DECIMAL;
      }
   }

   public Datum(DatumType type, String stringValue, String currencySymbol,
     Number numValue, DatatoolSettings settings)
   {
      this(type, stringValue, currencySymbol, numValue, null, settings);
   }

   public Datum(DatumType type, String stringValue, 
     Julian julian, DatatoolSettings settings)
   {
      this(type, stringValue, null, null, julian, settings);
   }

   public Datum(DatumType type, String stringValue, String currencySymbol,
     Number num, Julian julian, DatatoolSettings settings)
   {
      this.settings = settings;
      this.type = type;
      this.stringValue = stringValue;
      this.currencySymbol = currencySymbol;

      if (type.isTemporal())
      {
         if (julian == null && num == null)
         {
            throw new NullPointerException(
              "Temporal type "+type+" requires non-null Number or Julian");
         }
         else if (julian == null)
         {
            switch (type)
            {
               case DATETIME:
                  julian = Julian.createDate(num.doubleValue());
               break;
               case TIME:
                  julian = Julian.createTime(num.doubleValue());
               break;
               case DATE:
                  julian = Julian.createDay(num.intValue());
               break;
               default:
                 assert false : "Invalid temporal data type "+type;
            }
         }
         else if (num == null)
         {
            switch (type)
            {
               case DATETIME:
                  num = Double.valueOf(julian.getJulianDate());
               break;
               case TIME:
                  num = Double.valueOf(julian.getJulianTime());
               break;
               case DATE:
                  num = Integer.valueOf(julian.getJulianDay());
               break;
               default:
                 assert false : "Invalid temporal data type "+type;
            }
         }
      }
      else if (type.isNumeric() && num == null)
      {
         throw new NullPointerException(
           "Numeric type "+type+" requires non-null value");
      }
      else if (type == DatumType.CURRENCY && currencySymbol == null)
      {
         throw new NullPointerException(
           "Type "+type+" requires non-null currency symbol");
      }

      this.julian = julian;
      this.numValue = num;
   }

   public static Datum createNull(DatatoolSettings settings)
   {
      return new Datum(DatumType.UNKNOWN, DatatoolDb.NULL_VALUE, null, null,
        settings);
   }

   public void setToNull()
   {
      type = DatumType.UNKNOWN;
      stringValue = DatatoolDb.NULL_VALUE;
      numValue = null;
      currencySymbol = null;
      julian = null;
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
      Julian julian = null;
      TeXObject content = entryContents;

      if (entryContents instanceof DatumElement)
      {
         DatumElement elem = (DatumElement)entryContents;
         content = elem.getOriginal();
         elemType = elem.getDatumType();
         julian = elem.getJulian();

         if (julian == null && elemType.isTemporal())
         {
            elemType = DatumType.STRING;
         }

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
         julian = elem.getJulian();

         if (julian == null && elemType.isTemporal())
         {
            elemType = DatumType.STRING;
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
         return new Datum(elemType, text, sym, elemValue, julian, settings);
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

      // First try scientific notation

      if (DataToolBaseSty.SCIENTIFIC_PATTERN.matcher(text).matches())
      {
         try
         {
            return new Datum(DatumType.DECIMAL, text, null, new BigDecimal(text), settings);
         }
         catch (NumberFormatException e)
         {// shouldn't happen
            settings.getMessageHandler().debug(e);
         }
      }

      // Try date/time

      Julian julian = null;

      try
      {
         julian = Julian.create(text);
         return new Datum(julian.getDatumType(), text, julian, settings);
      }
      catch (IllegalArgumentException e)
      {// not date/time
      }

      // Try if the text is formatted according to the
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
    * Update the numeric value without changing the type, string value, 
    * currency symbol, or Julian object.
    */
   public void setNumeric(Number num)
   {
      this.numValue = num;
   }

   /**
    * Update the currency symbol without changing the type, string value,
    * numeric value, or Julian object.
    */
   public void setCurrencySymbol(String sym)
   {
      this.currencySymbol = sym;
   }

   /**
    * Sets the Julian value. This will also update the associated
    * numeric value according to the data type. The provided value
    * may be null, which will downgrade the data type if it's
    * currently set to a temporal type.
    */
   public void setJulian(Julian julian)
   {
      this.julian = julian;

      if (julian == null)
      {
         if (type.isTemporal())
         {
            if (numValue == null)
            {
               type = DatumType.STRING;
            }
            else if (type == DatumType.DATETIME || type == DatumType.TIME)
            {
               type = DatumType.DECIMAL;
            }
            else if (type == DatumType.DATE)
            {
               type = DatumType.INTEGER;
            }
         }
      }
      else
      {
         if (type == DatumType.DATETIME)
         {
            numValue = Double.valueOf(julian.getJulianDate());
         }
         else if (type == DatumType.TIME)
         {
            numValue = Double.valueOf(julian.getJulianTime());
         }
         else if (type == DatumType.DATE)
         {
            numValue = Integer.valueOf(julian.getJulianDay());
         }
      }
   }

   public Julian getJulian()
   {
      return julian;
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
         case DATETIME:
            numStr = String.format((Locale)null,
              "\\DTLtemporalvalue{%g}{%s}",
              julian.getJulianDate(), julian.getTimeStamp());
         break;
         case TIME:
            numStr = String.format((Locale)null,
              "\\DTLtemporalvalue{%g}{%s}",
              julian.getJulianTime(), julian.getTimeStamp());
         break;
         case DATE:
            numStr = String.format((Locale)null,
              "\\DTLtemporalvalue{%d}{%s}",
              julian.getJulianDay(), julian.getTimeStamp());
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

   @Deprecated
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
         case DATE:
         case INTEGER:
           result = Integer.compare(intValue(), other.intValue());
         break;
         case TIME:
         case DATETIME:
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

      if (julian != null && other.julian != null)
      {
         if (type == DatumType.TIME || other.type == DatumType.TIME
            || (julian.hasTimeZone() && other.julian.hasTimeZone()))
         {
            return Double.compare(doubleValue(), other.doubleValue());
         }

         /* For date and datetime mix this will ensure dates with no
            time are first. */

         return julian.getTimeStamp().compareTo(other.julian.getTimeStamp());
      }

      if (isNumeric() && other.isNumeric())
      {
         switch (type)
         {
            case DATE:
            case INTEGER:
              if (other.type == type)
              {
                 result = Integer.compare(intValue(), other.intValue());
                 break;
              }
            case TIME:
            case DATETIME:
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

      if (julian != null && other.julian != null)
      {
         return julian.getTimeStamp().equals(other.julian.getTimeStamp());
      }

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
    * Reformats the string representation, if this Datum is numeric.
    */
   public void reformat()
   {
      reformat(type);
   }

   /**
    * Reformats the string representation according to the given
    * type, if this Datum and the given type are numeric.
    */
   public void reformat(DatumType dataType)
   {
      if (numValue == null)
      {
         // do nothing
         return;
      }

      if (dataType == DatumType.DECIMAL && settings.useSIforDecimals())
      {
         stringValue = String.format("\\num{%g}", doubleValue());
      }
      else if (julian != null)
      {
         if (settings.useFmtForTemporal())
         {
            stringValue = julian.getTeXFormatCode();
         }
         else
         {
            stringValue = settings.formatTemporal(julian);
         }
      }
      else
      {
         NumberFormat numfmt = settings.getNumericFormatter(type);

         switch (dataType)
         {
            case INTEGER:
              stringValue = numfmt.format(intValue());
            break;
            case DECIMAL:
              stringValue = numfmt.format(doubleValue());
            break;
            case CURRENCY:
              stringValue = numfmt.format(doubleValue());

              if (currencySymbol == null)
              {
                 currencySymbol = numfmt.getCurrency().getSymbol();
                 type = dataType;
              }
              else
              {
                 stringValue = stringValue.replace(
                    numfmt.getCurrency().getSymbol(), currencySymbol);
              }
            break;
         }
      }
   }

   /**
    * Create a new instance of a numeric type.
    */ 
   public static Datum format(DatumType type, String currencySym, Number num,
      DatatoolSettings settings)
   {
      return format(type, currencySym, num, null, settings);
   }

   public static Datum format(DatumType type, String currencySym, Number num,
      Julian julian, DatatoolSettings settings)
   {
      String text = "";

      if (type == DatumType.DECIMAL && settings.useSIforDecimals())
      {
         text = String.format("\\num{%g}", num.doubleValue());
      }
      else if (type.isTemporal())
      {
         if (julian == null && num == null)
         {
            throw new NullPointerException(
             "Temporal type "+type+" can't have both Number and Julian null");
         }

         if (julian == null)
         {
            switch (type)
            {
               case DATE:
                  julian = Julian.createDay(num.intValue());
               break;
               case DATETIME:
                  julian = Julian.createDate(num.doubleValue());
               break;
               case TIME:
                  julian = Julian.createTime(num.doubleValue());
               break;
            }
         }
         else
         {
            if (type == DatumType.DATETIME)
            {
               num = Double.valueOf(julian.getJulianDate());
            }
            else if (type == DatumType.TIME)
            {
               num = Double.valueOf(julian.getJulianTime());
            }
            else
            {
               num = Integer.valueOf(julian.getJulianDay());
            }
         }

         if (settings.useFmtForTemporal())
         {
            text = julian.getTeXFormatCode();
         }
         else
         {
            text = settings.formatTemporal(julian);
         }
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

      return new Datum(text, currencySym, num, julian, settings);
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
   private Julian julian;
   DatatoolSettings settings;

   CollationKey collationKey;
   String stringSort;
   Number numericSort;

   public static int TEX_MAX_INT = 2147483647;
   public static final Pattern TRAILING_EMPTY_COMMENT_PATTERN = 
     Pattern.compile("(^|.*[^\\\\])%\\s*");
}
