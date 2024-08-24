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

import java.util.Vector;
import java.util.Locale;
import java.util.regex.*;
import java.text.Collator;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;

import com.dickimawbooks.datatooltk.io.*;

public class DataFilter
{
   public DataFilter(DatatoolDb db)
   {
      this(db, true);
   }

   public DataFilter(DatatoolDb db, boolean useOr)
   {
      this.db = db;
      this.useOr = useOr;
      this.fieldFilters = new Vector<FieldFilter>();
   }

   public void addFilters(Vector<FilterInfo> filters)
   {
      for (FilterInfo filterInfo: filters)
      {
         try
         {
            addFilter(filterInfo);
         }
         catch (UnknownLabelException e)
         {
            System.err.println(getMessageHandler().getLabelWithValues(
               "warning.ignoring_filter", filterInfo.toString()));
         }
      }
   }

   public void addFilter(FilterInfo filterInfo)
      throws UnknownLabelException
   {
      addFilter(filterInfo.getLabel(), filterInfo.getOperator(), 
         filterInfo.getValue());
   }

   public void addFilter(String label, int operator, String value)
      throws UnknownLabelException
   {
      fieldFilters.add(new FieldFilter(db, label, operator, value));
   }

   public void setUseOr(boolean useOr)
   {
      this.useOr = useOr;
   }

   public boolean matches(DatatoolRow row)
   {
      boolean result = true;
      boolean isFirst = true;

      for (FieldFilter filter : fieldFilters)
      {
         boolean match = filter.matches(row);

         if (useOr)
         {
            if (isFirst)
            {
               result = match;
            }
            else
            {
               result = (result || match);
            }
         }
         else
         {
            if (!match)
            {
               return false;
            }

            if (isFirst)
            {
               result = match;
            }
            else
            {
               result = (result && match);
            }
         }

         isFirst = false;
      }

      return result;
   }

   public MessageHandler getMessageHandler()
   {
      return db.getMessageHandler();
   }

   private DatatoolDb db;
   private boolean useOr = true;

   private Vector<FieldFilter> fieldFilters;

   public static final int OPERATOR_EQ=0;
   public static final int OPERATOR_LE=1;
   public static final int OPERATOR_LT=2;
   public static final int OPERATOR_GE=3;
   public static final int OPERATOR_GT=4;
   public static final int OPERATOR_NE=5;
   public static final int OPERATOR_REGEX=6;

   public static final String[] OPERATORS = new String[]
   {
      "eq", "le", "lt", "ge", "gt", "ne", "regex"
   };
}

class FieldFilter
{
   public FieldFilter(DatatoolDb db, String label, int operator,
     String value)
   throws UnknownLabelException
   {
      this.colIdx = 0;
      boolean found = false;
      MessageHandler messageHandler = db.getMessageHandler();
      DatatoolSettings settings = db.getSettings();

      Locale sortLocale = settings.getSortLocale();

      if (sortLocale == null)
      {
         isCaseSensitive = settings.getLoadSettings().isCaseSensitive();
      }
      else
      {
         collator = Collator.getInstance(sortLocale);
      }

      Vector<DatatoolHeader> headers = db.getHeaders();

      for (DatatoolHeader header : headers)
      {
         if (header.getKey().equals(label))
         {
            found = true;
            this.type = header.getDatumType();
            break;
         }

         this.colIdx++;
      }

      if (!found)
      {
         throw new UnknownLabelException(messageHandler, label);
      }

      this.operator = operator;

      if (this.operator == DataFilter.OPERATOR_REGEX)
      {
         pattern = Pattern.compile(value);
      }
      else if (type == DatumType.DECIMAL)
      {
         try
         {
            match = Double.valueOf(value);
         }
         catch (NumberFormatException e)
         {
            match = value;
         }
      }
      else if (type == DatumType.INTEGER)
      {
         try
         {
            match = Integer.valueOf(value);
         }
         catch (NumberFormatException e)
         {
            match = value;
         }
      }
      else
      {
         match = value;
      }
   }

   public boolean matches(DatatoolRow row)
   {
      Datum datum = row.get(colIdx);

      String strVal = datum.toString();

      if (pattern != null)
      {
         Matcher m = pattern.matcher(strVal);
         return m.matches();
      }

      int result = 0;

      try
      {
         if (match instanceof String)
         {
            if (collator == null)
            {
               if (isCaseSensitive)
               {
                  result = strVal.compareTo((String)match);
               }
               else
               {
                  result = strVal.toLowerCase().compareTo(
                     ((String)match).toLowerCase());
               }
            }
            else
            {
               result = collator.compare(strVal, match);
            }
         }
         else if (type == DatumType.DECIMAL)
         {
            result = Double.compare(datum.doubleValue(), ((Number)match).doubleValue());
         }
         else if (type == DatumType.INTEGER)
         {
            result = Integer.compare(datum.intValue(), ((Number)match).intValue());
         }
         else
         {
            result = strVal.compareTo(match.toString());
         }
      }
      catch (NumberFormatException e)
      {
         result = strVal.compareTo(match.toString());
      }

      switch (operator)
      {
         case DataFilter.OPERATOR_EQ: return result == 0;
         case DataFilter.OPERATOR_LE: return result <= 0;
         case DataFilter.OPERATOR_LT: return result < 0;
         case DataFilter.OPERATOR_GE: return result >= 0;
         case DataFilter.OPERATOR_GT: return result > 0;
         case DataFilter.OPERATOR_NE: return result != 0;
      }

      return false;
   }

   private int colIdx;
   private DatumType type;
   private int operator;
   private Object match = null;
   private Pattern pattern = null;
   private Collator collator=null;
   private boolean isCaseSensitive;
}
