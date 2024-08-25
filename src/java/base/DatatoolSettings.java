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

import java.io.*;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.text.Collator;
import java.text.NumberFormat;
import java.text.DecimalFormat;

import com.dickimawbooks.texparserlib.TeXApp;
import com.dickimawbooks.texparserlib.TeXSyntaxException;
import com.dickimawbooks.texparserlib.latex.datatool.AddDelimiterOption;
import com.dickimawbooks.texparserlib.latex.datatool.CsvBlankOption;
import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.texparserlib.latex.datatool.EscapeCharsOption;
import com.dickimawbooks.texparserlib.latex.datatool.FileFormatType;
import com.dickimawbooks.texparserlib.latex.datatool.IOSettings;
import com.dickimawbooks.texjavahelplib.*;

import com.dickimawbooks.datatooltk.base.io.DatatoolPasswordReader;

/**
 * Application settings for datatooltk.
 */
public class DatatoolSettings
{
   public DatatoolSettings(DatatoolTk datatooltk)
    throws IOException
   {
      this(datatooltk, null, null);
   }

   public DatatoolSettings(DatatoolTk datatooltk,
      String dictionaryTag, String helpsetTag)
    throws IOException
   {
      this();
      this.datatooltk = datatooltk;
      messageHandler = new MessageHandler(datatooltk);
      importSettings = new ImportSettings(this);

      initLocalisation(dictionaryTag, helpsetTag);

      setDefaults();
   }

   protected DatatoolSettings()
   {
      currencies = new Vector<String>();
   }

   protected void initLocalisation(String dictionaryTag, String helpsetTag)
     throws IOException
   {
      if (dictionaryTag != null)
      {
         dictLocale = new HelpSetLocale(dictionaryTag);
      }

      if (helpsetTag != null)
      {
         helpSetLocale = new HelpSetLocale(helpsetTag);
      }

      if (dictLocale == null || helpSetLocale == null)
      {
         dictLocale = new HelpSetLocale("en");
         helpSetLocale = new HelpSetLocale("en");
      }

      helpLib = new TeXJavaHelpLib(messageHandler,
       datatooltk.getApplicationName(), RESOURCES_PATH,
       DICT_DIR, dictLocale, helpSetLocale,
       "texparserlib", RESOURCE_PREFIX);
   }

   public int getInitialRowCapacity()
   {
      return initialRowCapacity;
   }

   public void setInitialRowCapacity(int capacity)
   {
      initialRowCapacity = capacity;
   }

   public int getInitialColumnCapacity()
   {
      return initialColumnCapacity;
   }

   public void setInitialColumnCapacity(int capacity)
   {
      initialColumnCapacity = capacity;
   }

   public String getDefaultOutputFormat()
   {
      return defaultOutputFormat;
   }

   public void applyDefaultOutputFormat(IOSettings ioSettings)
   {
      String val = getDefaultOutputFormat();

      if (val != null && !val.isEmpty())
      {
         String[] split = val.split(" ", 2);
         String version = "3.0";

         if (split.length == 2 && "2.0".equals(split[1]) || "3.0".equals(split[1]))
         {
            version = split[1];
         }

         try
         {
            if ("dtltex".equals(split[0]))
            {
               ioSettings.setFileFormat(FileFormatType.DTLTEX, version);
            }
            else
            {
               ioSettings.setFileFormat(FileFormatType.DBTEX, version);
            }
         }
         catch (TeXSyntaxException e)
         {// shouldn't happen
         }
      }
   }

   public void setDefaultOutputFormat(FileFormatType ft, String version)
   {
      setDefaultOutputFormat(String.format("%s %s", ft.toString(), version));
   }

   public void setDefaultOutputFormat(String fmt)
   {
      defaultOutputFormat = fmt;
   }

   public void setOverrideInputFormat(boolean on)
   {
      overrideInputFormat = on;
   }

   public boolean getOverrideInputFormat()
   {
      return overrideInputFormat;
   }

   public Charset getTeXEncoding()
   {
      return importSettings.getTeXEncoding();
   }

   public Charset getTeXEncodingDefault()
   {
      return texEncoding;
   }

   public String getTeXEncodingNameDefault()
   {
      return texEncoding == null ? null : texEncoding.name();
   }

   public void setTeXEncoding(Charset encoding)
   {
      importSettings.setTeXEncoding(encoding);
   }

   public void setTeXEncodingDefault(Charset encoding)
   {
      texEncoding = encoding;
   }

   public void setTeXEncodingDefault(String encoding)
     throws IllegalCharsetNameException,
            UnsupportedCharsetException,
            IllegalArgumentException
   {
      if (encoding == null)
      {
         texEncoding = null;
      }
      else
      {
         texEncoding = Charset.forName(encoding);
      }
   }

   public Charset getCsvEncoding()
   {
      return importSettings.getCsvEncoding();
   }

   public Charset getCsvEncodingDefault()
   {
      return csvEncoding;
   }

   public String getCsvEncodingNameDefault()
   {
      return csvEncoding == null ? null : csvEncoding.name();
   }

   public void setCsvEncoding(Charset encoding)
   {
      importSettings.setCsvEncoding(encoding);
   }

   public void setCsvEncodingDefault(Charset encoding)
   {
      csvEncoding = encoding;
   }

   public void setCsvEncodingDefault(String encoding)
     throws IllegalCharsetNameException,
            UnsupportedCharsetException,
            IllegalArgumentException
   {
      if (encoding == null)
      {
         csvEncoding = null;
      }
      else
      {
         csvEncoding = Charset.forName(encoding);
      }
   }

   /**
    * Write value int DBTEX v3.0 format according to current
    * settings. The applicable properties are:
    * dbtex-3-datum = NONE/ALL/HEADER/CELL (don't use datum format,
    * always use datum format, only use datum format according to
    * header type, only use datum format according to cell type).
    * The following only apply for the header and cell options:
    * dbtex3-string = true/false
    * dbtex3-integer = true/false
    * dbtex3-decimal = true/false
    * dbtex3-currency = true/false
    */
   public String getDbTeX3Cell(Datum datum, DatatoolHeader header) 
      throws IOException
   {
      boolean saveDatum = false;

      switch (getDbTeX3DatumValue())
      {
         case NONE:
            saveDatum = false;
         break;
         case ALL:
            saveDatum = true;
         break;
         case HEADER:
            saveDatum = isDbTeX3DatumValue(header.getDatumType());
         break;
         case CELL:
            saveDatum = isDbTeX3DatumValue(datum.getDatumType());
         break;
         default:
            assert false : "Invalid DbTeX3DatumValue case";
      }

      if (saveDatum)
      {
         return String.format("\\dtldbdatumreconstruct%s", datum.getDatumArgs());
      }
      else
      {
         return String.format("\\dtldbvaluereconstruct{%s}", datum.getText());
      }
   }

   public boolean isDbTeX3DatumValue(DatumType type)
   {
      switch (type)
      {
         case STRING: return isStringDbTeX3DatumValue();
         case INTEGER: return isIntegerDbTeX3DatumValue();
         case DECIMAL: return isDecimalDbTeX3DatumValue();
         case CURRENCY: return isCurrencyDbTeX3DatumValue();
      }

      return false;
   }

   public boolean isStringDbTeX3DatumValue()
   {
      return isStringDbTeX3DatumValueOn;
   }

   public void setStringDbTeX3DatumValue(boolean enable)
   {
      isStringDbTeX3DatumValueOn = enable;
   }

   public boolean isIntegerDbTeX3DatumValue()
   {
      return isStringDbTeX3DatumValueOn;
   }

   public void setIntegerDbTeX3DatumValue(boolean enable)
   {
      isStringDbTeX3DatumValueOn = enable;
   }

   public boolean isDecimalDbTeX3DatumValue()
   {
      return isDecimalDbTeX3DatumValueOn;
   }

   public void setDecimalDbTeX3DatumValue(boolean enable)
   {
      isDecimalDbTeX3DatumValueOn = enable;
   }

   public boolean isCurrencyDbTeX3DatumValue()
   {
      return isCurrencyDbTeX3DatumValueOn;
   }

   public void setCurrencyDbTeX3DatumValue(boolean enable)
   {
      isCurrencyDbTeX3DatumValueOn = enable;
   }

   public DbTeX3DatumValue getDbTeX3DatumValue()
   {
      return dbtex3DatumValue;
   }

   public void setDbTeX3DatumValue(DbTeX3DatumValue value)
   {
      dbtex3DatumValue = value;
   }

   public boolean useSIforDecimals()
   {
      return useSiForDecimals;
   }

   public void setSIforDecimals(boolean enable)
   {
      useSiForDecimals = enable;
   }

   public boolean isIntegerFormatterSet()
   {
      return integerFormat != null;
   }

   public void setIntegerFormatter(DecimalFormat format)
   {
      integerFormat = format;
   }

   public boolean isCurrencyFormatterSet()
   {
      return currencyFormat != null;
   }

   public void setCurrencyFormatter(DecimalFormat format)
   {
      currencyFormat = format;
   }

   public boolean isDecimalFormatterSet()
   {
      return decimalFormat != null;
   }

   public void setDecimalFormatter(DecimalFormat format)
   {
      decimalFormat = format;
   }

   public NumberFormat getNumericFormatter(DatumType type)
   {
      Locale locale = getNumericLocale();

      switch (type)
      {
         case INTEGER:

            if (integerFormat != null)
            {
               return integerFormat;
            }
            else
            {
               return NumberFormat.getIntegerInstance(locale);
            }

         case CURRENCY:

            if (currencyFormat != null)
            {
               return currencyFormat;
            }
            else
            {
               return NumberFormat.getCurrencyInstance(locale);
            }

         default:

            if (decimalFormat != null)
            {
               return decimalFormat;
            }
            else
            {
               return NumberFormat.getInstance(locale);
            }

      }
   }

   public void setNumericParser(DecimalFormat format)
   {
      numericParser = format;
   }

   public boolean isNumericParserSet()
   {
      return numericParser != null;
   }

   public NumberFormat getNumericParser()
   {
      if (numericParser == null)
      {
         return NumberFormat.getInstance(getNumericLocale());
      }

      return numericParser;
   }

   public Locale getNumericLocale()
   {
      if (numericLocale == null)
      {
         numericLocale = Locale.getDefault(Locale.Category.FORMAT);
      }

      return numericLocale;
   }

   public void setNumericLocale(Locale locale)
   {
      numericLocale = locale;
   }

   public Collator getSortCollator()
   {
      return sortCollator;
   }

   public String getSortLocaleString()
   {
      return sortLocale == null ? null : sortLocale.toLanguageTag();
   }

   public Locale getSortLocale()
   {
      return sortLocale;
   }

   public void setSortLocale(Locale locale)
   {
      sortLocale = locale;

      if (locale == null)
      {
         sortCollator = null;
      }
      else
      {
         sortCollator = Collator.getInstance(locale);
      }
   }

   public void setSortLocale(String locale)
   {
      if (locale == null || "".equals(locale))
      {
         sortLocale = null;
         sortCollator = null;
      }
      else
      {
         sortLocale = Locale.forLanguageTag(locale);
         sortCollator = Collator.getInstance(sortLocale);
      }
   }

   public void setSeparator(int separator)
   {
      importSettings.setSeparator(separator);
   }

   public void setSeparatorDefault(int separator)
   {
      csvSeparator = separator;
   }

   public int getSeparator()
   {
      return importSettings.getSeparator();
   }

   public int getSeparatorDefault()
   {
      return csvSeparator;
   }

   public void setDelimiter(int delimiter)
   {
      importSettings.setDelimiter(delimiter);
   }

   public void setDelimiterDefault(int delimiter)
   {
      csvDelimiter = delimiter;
   }

   public int getDelimiter()
   {
      return importSettings.getDelimiter();
   }

   public int getDelimiterDefault()
   {
      return csvDelimiter;
   }

   public String getSqlUrl()
   {
      return importSettings.getSqlUrl();
   }

   public String getSqlUrl(String sqlDb)
   {
      return importSettings.getSqlUrl(sqlDb);
   }

   public String getSqlHost()
   {
      return importSettings.getSqlHost();
   }

   public String getSqlHostDefault()
   {
      return sqlHost;
   }

   public void setSqlHost(String host)
   {
      importSettings.setSqlHost(host);
   }

   public void setSqlHostDefault(String host)
   {
      sqlHost = host;
   }

   public String getSqlPrefix()
   {
      return importSettings.getSqlPrefix();
   }

   public String getSqlPrefixDefault()
   {
      return sqlPrefix;
   }

   public void setSqlPrefix(String prefix)
   {
      importSettings.setSqlPrefix(prefix);
   }

   public void setSqlPrefixDefault(String prefix)
   {
      sqlPrefix = prefix;
   }

   public int getSqlPort()
   {
      return importSettings.getSqlPort();
   }

   public int getSqlPortDefault()
   {
      return sqlPort;
   }

   public void setSqlPort(int port)
   {
      importSettings.setSqlPort(port);
   }

   public void setSqlPortDefault(int port)
   {
      sqlPort = port;
   }

   public String getSqlDbName()
   {
      return importSettings.getSqlDbName();
   }

   public String getSqlDbNameDefault()
   {
      return sqlDbName;
   }

   public void setSqlDbName(String name)
   {
      importSettings.setSqlDbName(name);
   }

   public void setSqlDbNameDefault(String name)
   {
      sqlDbName = name;
   }

   public void setWipePassword(boolean wipePassword)
   {
      importSettings.setWipePassword(wipePassword);
   }

   public void setWipePasswordDefault(boolean wipePassword)
   {
      this.wipePassword = wipePassword;
   }

   public boolean isWipePasswordEnabled()
   {
      return importSettings.isWipePasswordOn();
   }

   public boolean isWipePasswordEnabledDefault()
   {
      return wipePassword;
   }

   public void setSqlUser(String username)
   {
      importSettings.setSqlUser(username);
   }

   public void setSqlUserDefault(String username)
   {
      sqlDbUser = username;
   }

   public String getSqlUser()
   {
      return importSettings.getSqlUser();
   }

   public String getSqlUserDefault()
   {
      return sqlDbUser;
   }

   public void setPasswordReader(DatatoolPasswordReader reader)
   {
      importSettings.setPasswordReader(reader);
   }

   public boolean hasCSVHeader()
   {
      return importSettings.hasHeaderRow();
   }

   public boolean hasCSVHeaderDefault()
   {
      return hasCsvHeader;
   }

   public void setHasCSVHeader(boolean hasHeader)
   {
      importSettings.setHasHeaderRow(hasHeader);
   }

   public void setHasCSVHeaderDefault(boolean hasHeader)
   {
      hasCsvHeader = hasHeader;
   }

   public void setColumnHeaders(String[] headers)
   {
      importSettings.setColumnHeaders(headers);
   }

   public String[] getColumnHeaders()
   {
      return importSettings.getColumnHeaders();
   }

   public void setColumnKeys(String[] keys)
   {
      importSettings.setColumnKeys(keys);
   }

   public String[] getColumnKeys()
   {
      return importSettings.getColumnKeys();
   }

   public void setAutoKeys(boolean on)
   {
      importSettings.setAutoKeys(on);
   }

   public boolean isAutoKeysOn()
   {
      return importSettings.isAutoKeysOn();
   }

   public boolean hasCSVStrictQuotes()
   {
      return importSettings.isStrictQuotesOn();
   }

   public boolean hasCSVStrictQuotesDefault()
   {
      return csvStrictQuotes;
   }

   public void setCSVStrictQuotes(boolean strictquotes)
   {
      importSettings.setStrictQuotes(strictquotes);
   }

   public void setCSVStrictQuotesDefault(boolean strictquotes)
   {
      csvStrictQuotes = strictquotes;
   }

   public void setCsvBlankOption(CsvBlankOption opt)
   {
      importSettings.setBlankRowAction(opt);
   }

   public void setCsvBlankOptionDefault(CsvBlankOption opt)
   {
      csvBlankOption = opt;
   }

   public CsvBlankOption getCsvBlankOption()
   {
      return importSettings.getBlankRowAction();
   }

   public CsvBlankOption getCsvBlankOptionDefault()
   {
      return csvBlankOption;
   }

   public void setEscapeCharsOption(EscapeCharsOption opt)
   {
      importSettings.setEscapeCharsOption(opt);
   }

   public void setEscapeCharsOptionDefault(EscapeCharsOption opt)
   {
      escapeCharsOption = opt;
   }

   public EscapeCharsOption getEscapeCharsOption()
   {
      return importSettings.getEscapeCharsOption();
   }

   public EscapeCharsOption getEscapeCharsOptionDefault()
   {
      return escapeCharsOption;
   }

   public void setAddDelimiterOption(AddDelimiterOption opt)
   {
      addDelimiterOption = opt;
   }

   public AddDelimiterOption getAddDelimiterOption()
   {
      return addDelimiterOption;
   }

   public int getCSVSkipLines()
   {
      return importSettings.getSkipLines();
   }

   public int getCSVSkipLinesDefault()
   {
      return skipLines;
   }

   public void setCSVSkipLines(int lines)
   throws IllegalArgumentException
   {
      importSettings.setSkipLines(lines);
   }

   public void setCSVSkipLinesDefault(int lines)
   throws IllegalArgumentException
   {
      if (lines < 0)
      {
         throw new IllegalArgumentException("Invalid skip lines value: "+lines);
      }

      skipLines = lines;
   }

   public boolean isImportEmptyToNullOn()
   {
      return importSettings.isImportEmptyToNullOn();
   }

   public boolean isImportEmptyToNullOnDefault()
   {
      return importEmptyToNull;
   }

   public void setImportEmptyToNull(boolean on)
   {
      importSettings.setImportEmptyToNull(on);
   }

   public void setImportEmptyToNullDefault(boolean on)
   {
      importEmptyToNull = on;
   }

   public boolean isSolutionEnvStripped()
   {
      return importSettings.isStripSolutionEnvOn();
   }

   public boolean isSolutionEnvStrippedDefault()
   {
      return stripSolnEnv;
   }

   public void setSolutionEnvStripped(boolean stripEnv)
   {
      importSettings.setStripSolutionEnv(stripEnv);
   }

   public void setSolutionEnvStrippedDefault(boolean stripEnv)
   {
      stripSolnEnv = stripEnv;
   }

   public boolean isPreambleOnly()
   {
      return importSettings.isPreambleOnly();
   }

   public boolean isPreambleOnlyDefault()
   {
      return isPreambleOnly;
   }

   public void setPreambleOnly(boolean on)
   {
      importSettings.setPreambleOnly(on);
   }

   public void setPreambleOnlyDefault(boolean on)
   {
      isPreambleOnly = on;
   }

   public void setOwnerOnly(boolean enable)
   {
      ownerOnly = enable;
   }

   public boolean isOwnerOnly()
   {
      return ownerOnly;
   }

   public void setAutoTrimLabels(boolean enable)
   {
      importSettings.setTrimLabels(enable);
   }

   public void setAutoTrimLabelsDefault(boolean enable)
   {
      autoTrimLabels = enable;
   }

   public boolean isAutoTrimLabelsOn()
   {
      return importSettings.isTrimLabelsOn();
   }

   public boolean isAutoTrimLabelsOnDefault()
   {
      return autoTrimLabels;
   }

   public void setTrimElement(boolean enable)
   {
      importSettings.setTrimElement(enable);
   }

   public void setTrimElementDefault(boolean enable)
   {
      trimElements = enable;
   }

   public boolean isTrimElementOn()
   {
      return importSettings.isTrimElementOn();
   }

   public boolean isTrimElementOnDefault()
   {
      return trimElements;
   }

   @Deprecated
   public void setTeXMapping(boolean enable)
   {
      setLiteralContent(enable);
   }

   @Deprecated
   public void setTeXMappingDefault(boolean enable)
   {
      setLiteralContentDefault(enable);
   }

   @Deprecated
   public boolean isTeXMappingOnDefault()
   {
      return isLiteralContentDefault();
   }

   public boolean isLiteralContent()
   {
      return importSettings.isLiteralContent();
   }

   public boolean isLiteralContentDefault()
   {
      return literalContent;
   }

   public void setLiteralContent(boolean on)
   {
      importSettings.setLiteralContent(on);
   }

   public void setLiteralContentDefault(boolean on)
   {
      literalContent = on;
   }

   @Deprecated
   public boolean isTeXMappingOn()
   {
      return isLiteralContent();
   }

   public HashMap<Integer,String> getTeXMappings()
   {
      return texMap;
   }

   public String getTeXMap(int codePoint)
   {
      return getTeXMap(Integer.valueOf(codePoint));
   }

   public String getTeXMap(Integer codePoint)
   {
      if (texMap != null)
      {
         return texMap.get(codePoint);
      }

      return null;
   }

   public String removeTeXMap(int codePoint)
   {
      return removeTeXMap(Integer.valueOf(codePoint));
   }

   public String removeTeXMap(Integer codePoint)
   {
      return texMap == null ? null : texMap.remove(codePoint);
   }

   public void setTeXMap(int codePoint, String value)
   {
      setTeXMap(Integer.valueOf(codePoint), value);
   }

   public void setTeXMap(Integer codePoint, String value)
   {
      if (texMap == null)
      {
         texMap = new HashMap<Integer,String>();
      }

      texMap.put(codePoint, value);
   }

   public int getCurrencyCount()
   {
      return currencies.size();
   }

   public int getCurrencyIndex(String currency)
   {
      return currencies.indexOf(currency);
   }

   public String getCurrency(int index)
   {
      return currencies.get(index);
   }

   public void addCurrency(String value)
   {
      currencies.add(value);
   }

   public void setCurrency(int index, String value)
   {
      currencies.set(index, value);
   }

   public boolean removeCurrency(String currency)
   {
      return currencies.remove(currency);
   }

   public void clearCurrencies()
   {
      currencies.clear();
   }

   public boolean isCurrency(String text)
   {
      Matcher m = PATTERN_CURRENCY.matcher(text);

      if (m.matches())
      {
         String currency = m.group(1);

         for (int i = 0, n = currencies.size(); i < n; i++)
         {
            if (currencies.get(i).equals(currency)) return true;
         }
      }

      return false;
   }

   @Deprecated
   public Currency parseCurrency(String text)
      throws NumberFormatException
   {
      Matcher m = PATTERN_CURRENCY.matcher(text);

      if (m.matches())
      {
         String currency = m.group(1);
         float value = Float.parseFloat(m.group(2));

         for (int i = 0, n = currencies.size(); i < n; i++)
         {
            if (currencies.get(i).equals(currency))
            {
               return new Currency(currency, value);
            }
         }
      }

      throw new NumberFormatException(messageHandler.getLabelWithValues(
         "error.not_currency", text));
   }

   public void setRandomSeed(Long seed)
   {
      randomSeed = seed;
   }

   public Long getRandomSeed()
   {
      return randomSeed;
   }

   public Random getRandom()
   {
      Long seed = getRandomSeed();

      return seed == null ? new Random() : new Random(seed.longValue());
   }

   public int getShuffleIterations()
   {
      return shuffleIterations;
   }

   public void setShuffleIterations(int number)
   {
      shuffleIterations = number;
   }

// TODO?? allow \newproblem to be redefined??
   public boolean isRedefNewProblemEnabled()
   {
      return importSettings.isRedefNewProblemOn();
   }

   public boolean isRedefNewProblemEnabledDefault()
   {
      return allowRedefNewProb;
   }

   public void setRedefNewProblem(boolean value)
   {
      importSettings.setRedefNewProblem(value);
   }

   public void setRedefNewProblemDefault(boolean value)
   {
      allowRedefNewProb = value;
   }

   public String getDictionary()
   {
      return dictLocale.getTag();
   }

   public HelpSetLocale getDictionaryLocale()
   {
      return dictLocale;
   }

   public void setDictionary(HelpSetLocale hsLocale)
   {
      dictLocale = hsLocale;
   }

   public String getHelpSet()
   {
      return helpSetLocale.getTag();
   }

   public HelpSetLocale getHelpSetLocale()
   {
      return helpSetLocale;
   }

   public void setHelpSet(HelpSetLocale hsLocale)
   {
      helpSetLocale = hsLocale;
   }

   public TeXJavaHelpLib getHelpLib()
   {
      return helpLib;
   }

   public void setDefaults()
   {
      importSettings.setDefaults();
      setTeXMapping(false);
   }

   protected void setDefaultTeXMaps()
   {
      setTeXMap((int)'\\', "\\textbackslash ");
      setTeXMap((int)'$', "\\$");
      setTeXMap((int)'#', "\\#");
      setTeXMap((int)'%', "\\%");
      setTeXMap((int)'_', "\\_");
      setTeXMap((int)'{', "\\{");
      setTeXMap((int)'}', "\\}");
      setTeXMap((int)'&', "\\&");
      setTeXMap((int)'~', "\\textasciitilde ");
      setTeXMap((int)'^', "\\textasciicircum ");
   }

   protected void setDefaultCurrency()
   {
      addCurrency("\\$");
      addCurrency("\\pounds");
      addCurrency("\\texteuro");
      addCurrency("\\textdollar");
      addCurrency("\\textstirling");
      addCurrency("\\textyen");
      addCurrency("\\textwon");
      addCurrency("\\textcurrency");
      addCurrency("\\euro");
      addCurrency("\\yen");
   }

   public void setMessageHandler(MessageHandler handler)
   {
      messageHandler = handler;
   }

   public MessageHandler getMessageHandler()
   {
      return messageHandler;
   }

   public TeXApp getTeXApp()
   {
      return messageHandler.getTeXApp();
   }

   public DataToolTeXParserListener getTeXParserListener()
     throws IOException
   {
      return getTeXParserListener(false);
   }

   public DataToolTeXParserListener getTeXParserListener(boolean reset)
     throws IOException
   {
      if (parserListener == null || reset)
      {
         parserListener = new DataToolTeXParserListener(this);
      }

      return parserListener;
   }

   public String getApplicationName()
   {
      return getDatatoolTk().getApplicationName();
   }

   public DatatoolTk getDatatoolTk()
   {
      return messageHandler.getDatatoolTk();
   }

   public boolean isBatchMode()
   {
      return messageHandler == null ? true : messageHandler.isBatchMode();
   }

   public void setBatchMode(boolean enabled)
   {
      messageHandler.setBatchMode(enabled);
   }

   public boolean isCompatibilityLevel(int level)
   {
      return compatLevel == level;
   }

   public void setCompatibilityLevel(int level)
   {
      switch (level)
      {
         case COMPAT_LATEST:
         case COMPAT_1_6:
           compatLevel = level;
           break;
         default:
            throw new IllegalArgumentException(
              "Invalid compatibility level setting "+level);
      }
   }

   public boolean isNullFirst()
   {
      return isNullFirst;
   }

   public void setNullFirst(boolean isFirst)
   {
      isNullFirst = isFirst;
   }

   public LoadSettings getLoadSettings()
   {
      return loadSettings;
   }

   public void setLoadSettings(LoadSettings settings)
   {
      this.loadSettings = settings;
   }

   public ImportSettings getImportSettings()
   {
      return importSettings;
   }

   public BufferedReader createBufferedReader(File file)
    throws IOException,SecurityException
   {
      return createBufferedReader(file.toPath());
   }

   public BufferedReader createBufferedReader(Path path)
    throws IOException,SecurityException
   {
      return getTeXApp().createBufferedReader(path, StandardCharsets.UTF_8);
   }

   public BufferedWriter createBufferedWriter(File file)
    throws IOException,SecurityException
   {
      return createBufferedWriter(file.toPath());
   }

   public BufferedWriter createBufferedWriter(Path path)
    throws IOException,SecurityException
   {
      return getTeXApp().createBufferedWriter(path, StandardCharsets.UTF_8);
   }

   protected int initialRowCapacity = 100;
   protected int initialColumnCapacity = 10;
   protected String defaultOutputFormat = null;
   protected boolean overrideInputFormat = false;
   protected Charset texEncoding = null;
   protected Charset csvEncoding = null;
   protected boolean isStringDbTeX3DatumValueOn = false;
   protected boolean isDecimalDbTeX3DatumValueOn = true;
   protected boolean isCurrencyDbTeX3DatumValueOn = true;
   protected DbTeX3DatumValue dbtex3DatumValue = DbTeX3DatumValue.HEADER;
   protected boolean useSiForDecimals = false;
   protected DecimalFormat integerFormat = null;
   protected DecimalFormat currencyFormat = null;
   protected DecimalFormat decimalFormat = null;
   protected DecimalFormat numericParser = null;
   protected Locale numericLocale = null;
   protected Locale sortLocale = null;

   protected String sqlHost = "localhost";
   protected String sqlPrefix = "jdbc:mysql://";
   protected int sqlPort = 3306;
   protected String sqlDbName = "";
   protected String sqlDbUser = "";
   protected boolean wipePassword = false;

   protected int csvSeparator = ',';
   protected int csvDelimiter = '"';
   protected boolean hasCsvHeader = true;
   protected boolean csvStrictQuotes = false;
   protected CsvBlankOption csvBlankOption = CsvBlankOption.IGNORE;
   protected EscapeCharsOption escapeCharsOption = EscapeCharsOption.DOUBLE_DELIM;
   protected AddDelimiterOption addDelimiterOption = AddDelimiterOption.DETECT;
   protected int skipLines = 0;

   protected boolean literalContent = true;

   protected boolean importEmptyToNull = false;
   protected boolean stripSolnEnv = true;
   protected boolean allowRedefNewProb = false;
   protected boolean isPreambleOnly = true;

   protected boolean autoTrimLabels = true;
   protected boolean trimElements = true;

   protected boolean ownerOnly = false;
   protected Long randomSeed = null;
   protected int shuffleIterations = 100;

   protected DatatoolTk datatooltk;
   protected MessageHandler messageHandler;

   protected TeXJavaHelpLib helpLib;

   protected ImportSettings importSettings;

   protected Vector<String> currencies;

   protected HashMap<Integer,String> texMap;

   protected int compatLevel = COMPAT_LATEST;

   protected LoadSettings loadSettings;

   protected boolean isNullFirst=true;

   protected Collator sortCollator;

   protected HelpSetLocale helpSetLocale, dictLocale;

   protected DataToolTeXParserListener parserListener;

   public static final int COMPAT_LATEST=0;
   public static final int COMPAT_1_6=1;

   public static final Pattern PATTERN_CURRENCY
      = Pattern.compile("(.+?) *(\\d*\\.?\\d+)");

   public static final String RESOURCES_PATH = "/resources";
   public static final String DICT_DIR = RESOURCES_PATH+"/dictionaries/";

   public static final String RESOURCE_PREFIX = "datatooltk-";
}
