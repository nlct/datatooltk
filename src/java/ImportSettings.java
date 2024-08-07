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

import java.util.Arrays;
import java.util.HashMap;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.dickimawbooks.texparserlib.TeXParser;
import com.dickimawbooks.texparserlib.latex.datatool.IOSettings;
import com.dickimawbooks.texparserlib.latex.datatool.CsvBlankOption;
import com.dickimawbooks.texparserlib.latex.datatool.EscapeCharsOption;

import com.dickimawbooks.datatooltk.io.DatatoolPasswordReader;
import com.dickimawbooks.datatooltk.gui.DatatoolGuiResources;

public class ImportSettings
{
   public ImportSettings(DatatoolSettings settings)
   {
      this.settings = settings;
      setDefaults();
   }

   public void setDefaults()
   {
      texEncoding = StandardCharsets.UTF_8;
      csvEncoding = StandardCharsets.UTF_8;

      separator = ',';
      delimiter = '"';
      incHeader = true;
      sqlHost = "localhost";
      sqlPort = 3306;
      sqlPrefix = "jdbc:mysql://";
      wipePassword = true;

      trimElement=true;
      trimLabels = true;
      mapTeXChars = true;
      stripSolutionEnv = true;
      redefNewProblem = true;

      nonTeXLiteral = true;
      skipLines=0;
      autoKeys = false;
      strictQuotes = false;

      blankRowOpt = CsvBlankOption.IGNORE;
      escCharsOpt = EscapeCharsOption.DOUBLE_DELIM;
   }

   public void update()
   {
      setFrom(settings);
   }

   public void setFrom(DatatoolSettings settings)
   {
      this.settings = settings;

      String charsetName = settings.getTeXEncodingProperty();

      if (charsetName != null)
      {
         try
         {
            texEncoding = Charset.forName(charsetName);
         }
         catch (Throwable e)
         {
            settings.getMessageHandler().debug(e);
         }
      }

      charsetName = settings.getCsvEncodingProperty();

      if (charsetName != null)
      {
         try
         {
            csvEncoding = Charset.forName(charsetName);
         }
         catch (Throwable e)
         {
            settings.getMessageHandler().debug(e);
         }
      }

      separator = settings.getSeparatorProperty();
      delimiter = settings.getDelimiterProperty();

      incHeader = settings.hasCSVHeaderProperty();
      strictQuotes = settings.hasCSVstrictquotesProperty();

      blankRowOpt = settings.getCsvBlankOptionProperty();
      escCharsOpt = settings.getEscapeCharsOptionProperty();
      skipLines = settings.getCSVskiplinesProperty();

      sqlHost = settings.getSqlHostProperty();
      sqlPrefix = settings.getSqlPrefixProperty();
      sqlPort = settings.getSqlPortProperty();

      sqlDbName = settings.getSqlDbNameProperty();
      sqlUser = settings.getSqlUserProperty();
      wipePassword = settings.isWipePasswordEnabledProperty();

      stripSolutionEnv = settings.isSolutionEnvStrippedProperty();
      redefNewProblem = settings.isRedefNewProblemEnabledProperty();

      trimLabels = settings.isAutoTrimLabelsOnProperty();
      nonTeXLiteral = settings.isLiteralContentProperty();
      mapTeXChars = settings.isTeXMappingOnProperty();
   }

   public void applyTo(DatatoolSettings settings)
   {
      settings.setTeXEncodingProperty(texEncoding);
      settings.setCsvEncodingProperty(csvEncoding);

      settings.setSeparatorProperty(separator);
      settings.setDelimiterProperty(delimiter);

      settings.setHasCSVHeaderProperty(incHeader);
      settings.setCSVstrictquotesProperty(strictQuotes);
      settings.setCsvBlankOptionProperty(blankRowOpt);
      settings.setEscapeCharsOptionProperty(escCharsOpt);
      settings.setCSVskiplinesProperty(skipLines);

      settings.setSqlHostProperty(sqlHost);
      settings.setSqlPrefixProperty(sqlPrefix);
      settings.setSqlPortProperty(sqlPort);

      settings.setSqlDbNameProperty(sqlDbName);
      settings.setSqlUserProperty(sqlUser);
      settings.setWipePasswordProperty(wipePassword);

      settings.setAutoTrimLabelsProperty(trimLabels);
      settings.setTeXMappingProperty(mapTeXChars);
      settings.setLiteralContentProperty(nonTeXLiteral);

      settings.setSolutionEnvStrippedProperty(stripSolutionEnv);
      settings.setRedefNewProblemProperty(redefNewProblem);
   }

   public void setFrom(IOSettings ioSettings)
   throws IOException
   {
      setFrom(ioSettings, getTeXParserListener().getParser());
   }

   public void setFrom(IOSettings ioSettings, TeXParser parser)
   throws IOException
   {
      incHeader = ioSettings.isHeaderIncluded();
      separator = ioSettings.getSeparator();
      delimiter = ioSettings.getDelimiter();
      escCharsOpt = ioSettings.getEscapeCharsOption();
      strictQuotes = ioSettings.isCsvStrictQuotes();
      blankRowOpt = ioSettings.getCsvBlankOption();
      nonTeXLiteral = ioSettings.isCsvLiteral();
      autoKeys = ioSettings.isAutoKeysOn();
      skipLines = ioSettings.getSkipLines();
      trimElement = ioSettings.isTrimElementOn();

      int n = ioSettings.getColumnKeyCount();

      if (n == 0)
      {
         keys = null;
      }
      else
      {
         keys = new String[n];

         for (int i = 1; i <= n; i++)
         {
            keys[i-1] = ioSettings.getColumnKey(i);
         }
      }

      n = ioSettings.getColumnHeaderCount();

      if (n == 0)
      {
         headers = null;
      }
      else
      {
         headers = new String[n];

         for (int i = 1; i <=n; i++)
         {
            headers[i-1] = ioSettings.getColumnHeader(i).toString(parser);
         }
      }
   }

   public void applyTo(IOSettings ioSettings)
   throws IOException
   {
      applyTo(ioSettings, getTeXParserListener().getParser());
   }

   public void applyTo(IOSettings ioSettings, TeXParser parser)
   throws IOException
   {
      ioSettings.setHeaderIncluded(incHeader);
      ioSettings.setSeparator(separator );
      ioSettings.setDelimiter(delimiter);
      ioSettings.setEscapeCharsOption(escCharsOpt);
      ioSettings.setCsvStrictQuotes(strictQuotes);
      ioSettings.setCsvBlankOption(blankRowOpt);
      ioSettings.setCsvLiteral(nonTeXLiteral);
      ioSettings.setAutoKeys(autoKeys);
      ioSettings.setSkipLines(skipLines);
      ioSettings.setTrimElement(trimElement);

      ioSettings.setColumnKeys(keys);
      ioSettings.setColumnHeaders(headers, parser);
   }

   /**
    * Gets the spreadsheet table reference.
    */
   public Object getSheetRef()
   {
      return sheetRef;
   }

   public void setSheetName(String sheetName)
   {
      sheetRef = sheetName;
   }

   public void setSheetIndex(Number sheetIdx)
   {
      sheetRef = sheetIdx;
   }

   public void setSheetIndex(int sheetIdx)
   {
      sheetRef = Integer.valueOf(sheetIdx);
   }

   /**
    * If true, TeX data should be check for known verbatim commands
    * or environments.
    */
   public boolean isCheckForVerbatimOn()
   {
      return checkForVerbatim;
   }

   public void setCheckForVerbatim(boolean on)
   {
      checkForVerbatim = on;
   }

   /**
    * If true, CSV/TSV and spreadsheets have a header row.
    */
   public boolean hasHeaderRow()
   {
      return incHeader;
   }

   public void setHasHeaderRow(boolean on)
   {
      incHeader = on;
   }

   /**
    * Gets the CSV separator.
    */
   public int getSeparator()
   {
      return separator;
   }

   public void setSeparator(int separator)
   {
      this.separator = separator;
   }

   public int getDelimiter()
   {
      return delimiter;
   }

   public void setDelimiter(int delimiter)
   {
      this.delimiter = delimiter;
   }

   public EscapeCharsOption getEscapeCharsOption()
   {
      return escCharsOpt;
   }

   public void setEscapeCharsOption(EscapeCharsOption opt)
   {
      escCharsOpt = opt;
   }

   public CsvBlankOption getBlankRowAction()
   {
      return blankRowOpt;
   }

   public void setBlankRowAction(CsvBlankOption opt)
   {
      blankRowOpt = opt;
   }

   public boolean isStrictQuotesOn()
   {
      return strictQuotes;
   }

   public void setStrictQuotes(boolean on)
   {
      strictQuotes = on;
   }

   public boolean isLiteralContent()
   {
      return nonTeXLiteral;
   }

   public void setLiteralContent(boolean on)
   {
      nonTeXLiteral = on;
   }

   public boolean isAutoKeysOn()
   {
      return autoKeys;
   }

   public void setAutoKeysOn(boolean on)
   {
      autoKeys = on;
   }

   public int getSkipLines()
   {
      return skipLines;
   }

   public void setSkipLines(int lines)
   throws IllegalArgumentException
   {
      if (lines < 0)
      {
         throw new IllegalArgumentException("Invalid skip lines value: "+lines);
      }

      skipLines = lines;
   }

   public boolean isTrimElementOn()
   {
      return trimElement;
   }

   public void setTrimElement(boolean on)
   {
      trimElement = on;
   }

   public boolean isTrimLabelsOn()
   {
      return trimLabels;
   }

   public void setTrimLabels(boolean on)
   {
      trimLabels = on;
   }

   public boolean isMapCharsOn()
   {
      return mapTeXChars;
   }

   public void setMapChars(boolean on)
   {
      mapTeXChars = on;
   }

   public HashMap<Integer,String> getTeXMappings()
   {
      return settings.getTeXMappings();
   }

   public String getTeXMap(int codePoint)
   {
      return settings.getTeXMap(codePoint);
   }

   public String getTeXMap(Integer codePoint)
   {
      return settings.getTeXMap(codePoint);
   }

   public String removeTeXMap(int codePoint)
   {
      return settings.removeTeXMap(codePoint);
   }

   public String removeTeXMap(Integer codePoint)
   {
      return settings.removeTeXMap(codePoint);
   }

   public void setTeXMap(int codePoint, String value)
   {
      settings.setTeXMap(codePoint, value);
   }

   public void setTeXMap(Integer codePoint, String value)
   {
      settings.setTeXMap(codePoint, value);
   }

   public int getCurrencyCount()
   {
      return settings.getCurrencyCount();
   }

   public int getCurrencyIndex(String currency)
   {
      return settings.getCurrencyIndex(currency);
   }

   public String getCurrency(int index)
   {
      return settings.getCurrency(index);
   }

   public void addCurrency(String value)
   {
      settings.addCurrency(value);
   }

   public void setCurrency(int index, String value)
   {
      settings.setCurrency(index, value);
   }

   public boolean removeCurrency(String currency)
   {
      return settings.removeCurrency(currency);
   }

   public void clearCurrencies()
   {
      settings.clearCurrencies();
   }

   public boolean isCurrency(String text)
   {
      return settings.isCurrency(text);
   }

   public boolean isStripSolutionEnvOn()
   {
      return stripSolutionEnv;
   }

   public void setStripSolutionEnv(boolean on)
   {
      stripSolutionEnv = on;
   }

   // TODO what was this setting for??
   public boolean isRedefNewProblemOn()
   {
      return redefNewProblem;
   }

   public void setRedefNewProblem(boolean on)
   {
      redefNewProblem = on;
   }

   public int getColumnKeyCount()
   {
      return keys == null ? 0 : keys.length;
   }

   public String getColumnKey(int colIdx)
   {
      return (keys == null || keys.length < colIdx)
         ? null : keys[colIdx-1];
   }

   public void setColumnKeys(String[] keys)
   {
      this.keys = keys;
   }

   public int getColumnHeaderCount()
   {
      return headers == null ? 0 : headers.length;
   }

   public String getColumnHeader(int colIdx)
   {
      return (headers == null || headers.length < colIdx)
         ? null : headers[colIdx-1];
   }

   public void setColumnHeaders(String[] headers)
   {
      this.headers = headers;
   }

   public Charset getTeXEncoding()
   {
      return texEncoding;
   }

   public void setTeXEncoding(Charset charset)
   {
      texEncoding = charset;
   }

   public Charset getCsvEncoding()
   {
      return csvEncoding;
   }

   public void setCsvEncoding(Charset charset)
   {
      csvEncoding = charset;
   }

   public String getSqlHost()
   {
      return sqlHost;
   }

   public void setSqlHost(String host)
   {
      sqlHost = host;
   }

   public String getSqlPrefix()
   {
      return sqlPrefix;
   }

   public void setSqlPrefix(String prefix)
   {
      sqlPrefix = prefix;
   }

   public String getSqlDbName()
   {
      return sqlDbName;
   }

   public void setSqlDbName(String name)
   {
      sqlDbName = name;
   }

   public String getSqlUser()
   {
      return sqlUser;
   }

   public void setSqlUser(String user)
   {
      sqlUser = user;
   }

   public int getSqlPort()
   {
      return sqlPort;
   }

   public void setSqlPort(int port)
   {
      sqlPort = port;
   }

   public void wipePasswordIfRequired()
   {
      if (sqlPassword != null && wipePassword)
      {
         Arrays.fill(sqlPassword, ' ');
         sqlPassword = null;
      }
   }

   public void setSqlPassword(char[] passwd)
   {
      sqlPassword = passwd;
   }

   public char[] getSqlPassword()
     throws UserCancelledException
   {
      if (sqlPassword == null && passwordReader != null)
      {
         sqlPassword = passwordReader.requestPassword();
      }

      return sqlPassword;
   }

   public boolean isWipePasswordOn()
   {
      return wipePassword;
   }

   public void setWipePassword(boolean on)
   {
      wipePassword = on;
   }

   public void setPasswordReader(DatatoolPasswordReader reader)
   {
      passwordReader = reader;
   }

   public String getSqlUrl()
   {
      return getSqlPrefix() + getSqlHost() + ":" + getSqlPort() + "/";
   }

   public String getSqlUrl(String sqlDb)
   {
      return getSqlUrl()+sqlDb;
   }

   public DatatoolSettings getSettings()
   {
      return settings;
   }

   public LoadSettings getLoadSettings()
   {
      return settings.getLoadSettings();
   }

   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
   }

   public DatatoolTeXApp getTeXApp()
   {
      return getMessageHandler().getDatatoolTeXApp();
   }

   public DataToolTeXParserListener getTeXParserListener()
     throws IOException
   {
      return settings.getTeXParserListener();
   }

   public DatatoolTk getDatatoolTk()
   {
      return settings.getDatatoolTk();
   }

   public DatatoolGuiResources getDatatoolGuiResources()
   {
      return settings.getDatatoolGuiResources();
   }

   public boolean isBatchMode()
   {
      return settings.isBatchMode();
   }

   public void setFrom(ImportSettings other)
   {
      sheetRef = other.sheetRef;
      checkForVerbatim = other.checkForVerbatim;
      incHeader = other.incHeader;
      separator = other.separator;
      delimiter = other.delimiter;
      escCharsOpt = other.escCharsOpt;
      blankRowOpt = other.blankRowOpt;
      strictQuotes = other.strictQuotes;
      nonTeXLiteral = other.nonTeXLiteral;
      autoKeys = other.autoKeys;
      skipLines = other.skipLines;
      trimElement = other.trimElement;
      trimLabels = other.trimLabels;
      mapTeXChars = other.mapTeXChars;
      stripSolutionEnv = other.stripSolutionEnv;
      redefNewProblem = other.redefNewProblem;
      texEncoding = other.texEncoding;
      csvEncoding = other.csvEncoding;
      sqlHost = other.sqlHost;
      sqlPrefix = other.sqlPrefix;
      sqlDbName = other.sqlDbName;
      sqlUser = other.sqlUser;
      sqlPort = other.sqlPort;
      wipePassword = other.wipePassword;
      passwordReader = other.passwordReader;

      if (other.keys == null)
      {
         keys = null;
      }
      else
      {
         keys = Arrays.copyOf(other.keys, other.keys.length);
      }

      if (other.headers == null)
      {
         headers = null;
      }
      else
      {
         headers = Arrays.copyOf(other.headers, other.headers.length);
      }

      if (other.sqlPassword == null)
      {
         sqlPassword = null;
      }
      else
      {
         sqlPassword = Arrays.copyOf(other.sqlPassword, other.sqlPassword.length);
      }
   }

   DatatoolSettings settings;

   Object sheetRef;
   boolean checkForVerbatim;
   boolean incHeader=true;
   int separator=',', delimiter='"';
   EscapeCharsOption escCharsOpt;
   CsvBlankOption blankRowOpt;
   boolean strictQuotes;
   boolean nonTeXLiteral;
   boolean autoKeys;
   int skipLines=0;
   boolean trimElement=true;
   boolean trimLabels = true;
   boolean mapTeXChars = true;

   boolean stripSolutionEnv = true;
   boolean redefNewProblem = true;

   String[] keys, headers;

   Charset texEncoding, csvEncoding;

   String sqlHost, sqlPrefix, sqlDbName, sqlUser;
   int sqlPort;

   protected char[] sqlPassword = null;

   boolean wipePassword = false;
   DatatoolPasswordReader passwordReader;
}
