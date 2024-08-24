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

import java.io.*;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.text.Collator;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import com.dickimawbooks.texparserlib.TeXApp;
import com.dickimawbooks.texparserlib.TeXSyntaxException;
import com.dickimawbooks.texparserlib.latex.datatool.AddDelimiterOption;
import com.dickimawbooks.texparserlib.latex.datatool.CsvBlankOption;
import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.texparserlib.latex.datatool.EscapeCharsOption;
import com.dickimawbooks.texparserlib.latex.datatool.FileFormatType;
import com.dickimawbooks.texparserlib.latex.datatool.IOSettings;
import com.dickimawbooks.texjavahelplib.*;

import com.dickimawbooks.datatooltk.io.DatatoolPasswordReader;
import com.dickimawbooks.datatooltk.gui.DatatoolPlugin;
import com.dickimawbooks.datatooltk.gui.DatatoolGuiResources;

/**
 * Application settings for datatooltk GUI/batch with a properties
 * directory.
 * It's necessary to determine the preferred dictionary
 * language before initiating the message system, so the
 * localisation information is now stored in a separate simple file 
 * which reduces the amount of non-localised error messages.
 */
public class DatatoolProperties extends DatatoolSettings
{
   public DatatoolProperties(DatatoolTk datatooltk)
    throws IOException
   {
      this(datatooltk, null, null);
   }

   public DatatoolProperties(DatatoolTk datatooltk,
      String dictionaryTag, String helpsetTag)
    throws IOException
   {
      this.datatooltk = datatooltk;
      messageHandler = new MessageHandler(datatooltk);
      importSettings = new ImportSettings(this);

      initLocalisation(dictionaryTag, helpsetTag);

      recentFiles = new Vector<String>();
      setDefaults();
   }

   public File getPropertiesPath()
   {
      return propertiesPath;
   }

   @Override
   protected void initLocalisation(String dictionaryTag, String helpsetTag)
     throws IOException
   {
      properties = new Properties();

      setPropertiesPath();

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
         initLangTags();
      }

      helpLib = new TeXJavaHelpLib(messageHandler,
       datatooltk.getApplicationName(), RESOURCES_PATH,
       DICT_DIR, dictLocale, helpSetLocale,
       "texparserlib", RESOURCE_PREFIX);

      helpLib.setIconPath(ICON_DIR);
      helpLib.setSmallIconSuffix(DEFAULT_SMALL_ICON_SUFFIX);
      helpLib.setLargeIconSuffix(DEFAULT_LARGE_ICON_SUFFIX);
   }

   protected void initLangTags() throws IOException
   {
      File file = new File(propertiesPath, LANGTAG_FILE_NAME);

      if (file.exists())
      {
         BufferedReader in = null;

         try
         {
            in = createBufferedReader(file);

            String line = in.readLine();

            if (line != null && dictLocale == null)
            {
               dictLocale = new HelpSetLocale(line.trim());
            }

            if (line != null && helpSetLocale == null)
            {
               line = in.readLine();

               if (line != null)
               {
                  helpSetLocale = new HelpSetLocale(line.trim());
               }
            }
         }
         finally
         {
            if (in != null)
            {
               in.close();
            }
         }
      }

      if (dictLocale == null)
      {
         dictLocale = new HelpSetLocale(Locale.getDefault());
      }

      if (helpSetLocale == null)
      {
         helpSetLocale = dictLocale;
      }
   }

   public static String getApplicationIconPath()
   {
      return RESOURCES_PATH + "/icons/datatooltk-logosmall.png";
   }

   public void setProperty(String key, String value)
   {
      properties.setProperty(key, value);
   }

   public Object removeProperty(String key)
   {
      return properties.remove(key);
   }

   public String getProperty(String key)
   {
      return properties.getProperty(key);
   }

   public String getProperty(String key, String defValue)
   {
      return properties.getProperty(key, defValue);
   }

   protected void setPropertiesPath()
   {
      String dirname = System.getenv("DATATOOLTK");

      if (dirname != null && !dirname.isEmpty())
      {
         propertiesPath = new File(dirname);

         if (!propertiesPath.exists())
         {
            if (!propertiesPath.mkdir())
            {
               messageHandler.debug(String.format("Unable to mkdir '%s'", propertiesPath));
               propertiesPath = null;
            }
         }
         else if (!propertiesPath.isDirectory())
         {
            messageHandler.debug(String.format(
             "DATATOOLTK environment variable '%s' is not a directory.", propertiesPath));
            propertiesPath = null;
         }
      }

      if (propertiesPath == null)
      {
         String base;

         if (System.getProperty("os.name").toLowerCase().startsWith("win"))
         {
            base = "datatooltk-settings";
         }
         else
         {
            base = ".datatooltk";
         }

         String home = System.getProperty("user.home");

         if (home == null)
         {
            messageHandler.debug("No 'user.home' property! (Set DATATOOLTK environment variable to appropriate directory.)");
            return;
         }

         File dir = new File(home);

         if (!dir.exists())
         {
            messageHandler.debug("Home directory '"+home+"' doesn't exist!");
            return;
         }

         propertiesPath = new File(dir, base);
      }

      if (propertiesPath.exists())
      {
         if (!propertiesPath.isDirectory())
         {
            messageHandler.debug("'"+propertiesPath+"' isn't a directory");
            propertiesPath = null;
            return;
         }
      }
      else
      {
         if (!propertiesPath.mkdir())
         {
            messageHandler.debug("Unable to mkdir '"+propertiesPath+"'");
            propertiesPath = null;

            return;
         }
      }
   }

   protected void loadMainProperties()
     throws IOException
   {
      File file = new File(propertiesPath, PROPERTIES_NAME);

      if (file.exists())
      {
         InputStream in = null;

         try
         {
            in = new FileInputStream(file);

            properties.loadFromXML(in);
         }
         finally
         {
            setDefaultTeXMaps();

            if (in != null)
            {
               in.close();
            }
         }

         helpLib.setLargeIconSuffix(getLargeIconSuffix());
         helpLib.setSmallIconSuffix(getSmallIconSuffix());
      }

      initFromProperties();
   }

   protected void initFromProperties()
   {
      setInitialRowCapacity(getInitialRowCapacityProperty());
      setInitialColumnCapacity(getInitialColumnCapacityProperty());

      setDefaultOutputFormat(getDefaultOutputFormatProperty());
      setOverrideInputFormat(getOverrideInputFormatProperty());
      setTeXEncodingProperty(getProperty("tex-encoding"));
      setCsvEncodingProperty(getProperty("csv-encoding"));

      setSeparatorProperty(getSeparatorProperty());
      setDelimiterProperty(getDelimiterProperty());

      setSqlHostProperty(getSqlHostProperty());
      setSqlPrefixProperty(getSqlPrefixProperty());
      setSqlPortProperty(getSqlPortProperty());
      setSqlDbNameProperty(getSqlDbNameProperty());
      setSqlUserProperty(getSqlUserProperty());

      setHasCSVHeaderProperty(hasCSVHeaderProperty());
      setCSVstrictquotesProperty(hasCSVstrictquotesProperty());
      setCsvBlankOptionProperty(getCsvBlankOptionProperty());
      setEscapeCharsOptionProperty(getEscapeCharsOptionProperty());
      setAddDelimiterOptionProperty(getAddDelimiterOptionProperty());
      setCSVskiplinesProperty(getCSVskiplinesProperty());

      setTeXMappingProperty(isTeXMappingOnProperty());
      setImportEmptyToNullProperty(isImportEmptyToNullOnProperty());
      setSolutionEnvStrippedProperty(isSolutionEnvStrippedProperty());
      setPreambleOnlyProperty(isPreambleOnlyProperty());

      setOwnerOnlyProperty(isOwnerOnlyProperty());
      setAutoTrimLabelsProperty(isAutoTrimLabelsOnProperty());
      setTrimElementProperty(isTrimElementOnProperty());
      setLiteralContentProperty(isLiteralContentProperty());

      setStringDbTeX3DatumValue(isStringDbTeX3DatumValueProperty());
      setIntegerDbTeX3DatumValue(isIntegerDbTeX3DatumValueProperty());
      setDecimalDbTeX3DatumValue(isDecimalDbTeX3DatumValueProperty());
      setCurrencyDbTeX3DatumValue(isCurrencyDbTeX3DatumValueProperty());
      setDbTeX3DatumValue(getDbTeX3DatumValueProperty());
      setSIforDecimals(useSIforDecimalsProperty());

      setNumericLocaleProperty(getNumericLocaleProperty());
      setNumericParserProperty(getNumericParserProperty());
      setIntegerFormatterProperty(getIntegerFormatterProperty());
      setCurrencyFormatterProperty(getCurrencyFormatterProperty());
      setDecimalFormatterProperty(getDecimalFormatterProperty());

      setSortLocaleProperty(getSortLocaleProperty());

      setRandomSeedProperty(getRandomSeedProperty());
      setShuffleIterationsProperty(getShuffleIterationsProperty());
      setRedefNewProblemProperty(isRedefNewProblemEnabledProperty());
      setNullFirstProperty(isNullFirstProperty());
   }

   protected void loadTeXMappings()
     throws IOException
   {
      File file = new File(propertiesPath, TEX_MAP_NAME);

      if (file.exists())
      {
         BufferedReader in = null;

         try
         {
            in = createBufferedReader(file);

            String line;

            while ((line = in.readLine()) != null)
            {
               if (line.startsWith("#") || line.isEmpty()) continue;

               Matcher m = PATTERN_TEX_MAP.matcher(line);

               if (m.matches())
               {
                  try
                  {
                     setTeXMap(Integer.parseInt(m.group(1), 16), m.group(2));
                  }
                  catch (NumberFormatException e)
                  {
                     messageHandler.debug(e);
                  }
               }
            }
         }
         finally
         {
            if (in != null)
            {
               in.close();
            }
         }
      }
      else
      {
         for (String key : properties.stringPropertyNames())
         {
            Matcher m = PATTERN_OLDER_TEX_MAP.matcher(key);

            if (m.matches())
            {
               setTeXMap(m.group(1).codePointAt(0), getProperty(key));
               removeProperty(key);
            }
            else
            {
               m = PATTERN_OLD_TEX_MAP.matcher(key);

               if (m.matches())
               {
                  try
                  {
                     setTeXMap(Integer.parseInt(m.group(1)), getProperty(key));
                  }
                  catch (NumberFormatException e)
                  {
                     // shouldn't happen
                     messageHandler.debug(e);
                  }

                  removeProperty(key);
               }
            }
         }
      }
   }

   protected void loadRecentFiles()
     throws IOException
   {
      File file = new File(propertiesPath, RECENT_NAME);

      if (file.exists())
      {
         BufferedReader in = null;

         try
         {
            in = createBufferedReader(file);

            recentFiles.clear();

            String line;

            while ((line = in.readLine()) != null)
            {
               if (!line.isEmpty())
               {
                  File f = new File(line);

                  if (f.exists())
                  {
                     recentFiles.add(line);
                  }
               }
            }
         }
         finally
         {
            if (in != null)
            {
               in.close();
            }
         }
      }
   }

   protected void loadCurrencies()
     throws IOException
   {
      File file = new File(propertiesPath, CURRENCY_FILE_NAME);

      if (file.exists())
      {
         BufferedReader in = null;

         try
         {
            in = createBufferedReader(file);

            currencies.clear();

            String line;

            while ((line = in.readLine()) != null)
            {
               if (!line.isEmpty())
               {
                  currencies.add(line);
               }
            }
         }
         finally
         {
            if (in != null)
            {
               in.close();
            }
         }
      }
      else
      {
         setDefaultCurrency();
      }
   }

   public void loadProperties()
      throws IOException,InvalidPropertiesFormatException
   {
      if (propertiesPath == null)
      {
         throw new NullPointerException();
      }

      loadMainProperties();

      String lastVersion = getProperty("version");

      if (lastVersion == null
            || lastVersion.compareTo(DatatoolTk.APP_VERSION) < 0)
      {
         upgrade=true;

         if (lastVersion == null
            || lastVersion.compareTo("2.0") < 0)
         {
            for (String key : properties.stringPropertyNames())
            {
               if (key.equals("skip-empty-rows"))
               {
                  if (Boolean.parseBoolean(getProperty(key)))
                  {
                     setCsvBlankOption(CsvBlankOption.IGNORE);
                  }
                  else
                  {
                     setCsvBlankOption(CsvBlankOption.EMPTY_ROW);
                  }

                  removeProperty(key);
               }
               else if (key.equals("helpset") || key.equals("dictionary"))
               {
                  removeProperty(key);
               }
            }
         }
      }

      loadTeXMappings();

      if (texMap == null)
      {
         setDefaultTeXMaps();
      }

      loadRecentFiles();
      loadCurrencies();

      importSettings.setFrom(this);
   }

   protected void saveMainProperties()
    throws IOException
   {
      File file = new File(propertiesPath, PROPERTIES_NAME);

      FileOutputStream out = null;

      try
      {
         out = new FileOutputStream(file);

         properties.storeToXML(out, null);
      }
      finally
      {
         if (out != null)
         {
            out.close();
         }
      }
   }

   protected void saveTeXMappings()
    throws IOException
   {
      File file = new File(propertiesPath, TEX_MAP_NAME);

      PrintWriter out = null;

      try
      {
         out = new PrintWriter(createBufferedWriter(file));

         for (Integer key : texMap.keySet())
         {
            out.format("%04x=%s%n", key.intValue(), texMap.get(key));
         }
      }
      finally
      {
         if (out != null)
         {
            out.close();
         }
      }
   }

   protected void saveRecentFiles()
    throws IOException
   {
      File file = new File(propertiesPath, RECENT_NAME);

      PrintWriter out = null;

      try
      {
         out = new PrintWriter(createBufferedWriter(file.toPath()));

         for (String filename : recentFiles)
         {
            out.println(filename);
         }
      }
      finally
      {
         if (out != null)
         {
            out.close();
         }
      }
   }

   protected void saveCurrencies()
    throws IOException
   {
      File file = new File(propertiesPath, CURRENCY_FILE_NAME);

      PrintWriter out = null;

      try
      {
         out = new PrintWriter(createBufferedWriter(file));

         for (String currency : currencies)
         {
            out.println(currency);
         }
      }
      finally
      {
         if (out != null)
         {
            out.close();
         }
      }
   }

   protected void saveLanguages()
      throws IOException
   {
      File file = new File(propertiesPath, LANGTAG_FILE_NAME);

      PrintWriter out = null;

      try
      {
         out = new PrintWriter(createBufferedWriter(file));

         out.println(dictLocale.getTag());
         out.println(helpSetLocale.getTag());
      }
      finally
      {
         if (out != null)
         {
            out.close();
         }
      }
   }

   public void saveProperties()
      throws IOException
   {
      saveProperties(true);
   }

   public void saveProperties(boolean updateImportSettings)
      throws IOException
   {
      if (propertiesPath == null) return;

      if (updateImportSettings)
      {
         importSettings.applyTo(this);
      }

      saveMainProperties();
      saveTeXMappings();
      saveRecentFiles();
      saveCurrencies();
      saveLanguages();
   }

   public void clearRecentFiles()
   {
      recentFiles.clear();
   }

   public String getRecentFileName(int i)
   {
      return recentFiles.get(i);
   }

   public int getRecentFileCount()
   {
      return recentFiles.size();
   }

   public void addRecentFile(File file)
   {
      String name = file.getAbsolutePath();

      // remove if already in the list

      recentFiles.remove(name);

      // Insert at the start of the list

      recentFiles.add(0, name);
   }

   public int getInitialRowCapacityProperty()
   {
      String prop = getProperty("initial-row-capacity");

      int capacity = getInitialRowCapacity();

      if (prop == null)
      {
         setInitialRowCapacity(capacity);
      }
      else
      {
         try
         {
            capacity = Integer.parseInt(prop);
         }
         catch (NumberFormatException e)
         {
            messageHandler.debug("Invalid initial row capacity setting '"
               +prop+"'");
            setInitialRowCapacity(capacity);
         }
      }

      return capacity;
   }

   @Override
   public void setInitialRowCapacity(int capacity)
   {
      super.setInitialRowCapacity(capacity);
      setProperty("initial-row-capacity", ""+capacity);
   }

   public int getInitialColumnCapacityProperty()
   {
      String prop = getProperty("initial-column-capacity");

      int capacity = getInitialColumnCapacity();

      if (prop == null)
      {
         setInitialColumnCapacity(capacity);
      }
      else
      {
         try
         {
            capacity = Integer.parseInt(prop);
         }
         catch (NumberFormatException e)
         {
            messageHandler.debug("Invalid initial column capacity setting '"
               +prop+"'");
            setInitialColumnCapacity(capacity);
         }
      }

      return capacity;
   }

   @Override
   public void setInitialColumnCapacity(int capacity)
   {
      super.setInitialColumnCapacity(capacity);
      setProperty("initial-column-capacity", ""+capacity);
   }

   public int getWindowWidth()
   {
      String prop = getProperty("window-width");

      if (prop == null) return 0;

      int width = 0;

      try
      {
         width = Integer.parseInt(prop);

         if (width < 0)
         {
            return 0;
         }
      }
      catch (NumberFormatException e)
      {
      }

      return width;
   }

   public void setWindowWidth(int width)
   {
      setProperty("window-width", ""+width);
   }

   public int getWindowHeight()
   {
      String prop = getProperty("window-height");

      if (prop == null) return 0;

      int height = 0;

      try
      {
         height = Integer.parseInt(prop);

         if (height < 0)
         {
            return 0;
         }
      }
      catch (NumberFormatException e)
      {
      }

      return height;
   }

   public void setWindowHeight(int height)
   {
      setProperty("window-height", ""+height);
   }

   public void setWindowSize(Dimension dim)
   {
      setWindowWidth(dim.width);
      setWindowHeight(dim.height);
   }

   public String getLargeIconSuffix()
   {
      String val = getProperty("largeiconsuffix");

      if (val == null)
      {
         return DEFAULT_LARGE_ICON_SUFFIX;
      }

      return val;
   }

   public void setLargeIconSuffix(String suffix)
   {
      setProperty("largeiconsuffix", suffix);
   }

   public String getSmallIconSuffix()
   {
      String val = getProperty("smalliconsuffix");

      if (val == null)
      {
         return DEFAULT_SMALL_ICON_SUFFIX;
      }

      return val;
   }

   public void setSmallIconSuffix(String suffix)
   {
      setProperty("smalliconsuffix", suffix);
   }

   public void setLookAndFeel(String lookAndFeel)
   {
      if (lookAndFeel == null)
      {
         removeProperty("lookandfeel");
      }
      else
      {
         setProperty("lookandfeel", lookAndFeel);
      }
   }

   public String getLookAndFeel()
   {
      return getProperty("lookandfeel");
   }

   public int getHelpSetLowerNavLabelLimit()
   {
      String val = getProperty("manual.lower_nav.limit");

      if (val == null || val.isEmpty())
      {
         return 20;
      }

      try
      {
         return Integer.parseInt(val);
      }
      catch (NumberFormatException e)
      {
         return 20;
      }
   }

   public boolean isHelpSetLowerNavLabelTextOn()
   {
      String val = getProperty("manual.lower_nav.text");

      if (val == null || val.isEmpty())
      {
         return true;
      }

      return Boolean.parseBoolean(val);
   }

   public void setHelpSetLowerNavLabelLimit(int limit)
   {
      setProperty("manual.lower_nav.limit", ""+limit);
   }

   public void setHelpSetLowerNavLabelText(boolean on)
   {
      setProperty("manual.lower_nav.text", ""+on);
   }

   public void initHelpSetSettings()
   {
      helpLib.setDefaultLowerNavSettings(
        isHelpSetLowerNavLabelTextOn(),
        getHelpSetLowerNavLabelLimit());

      helpLib.getHelpFontSettings().copyFrom(getManualFontSettings());
   }

   public void updateHelpSetSettings()
   {
      setManualFont(helpLib.getHelpFontSettings());
      setHelpSetLowerNavLabelLimit(helpLib.getDefaultLowerNavLabelLimit());
      setHelpSetLowerNavLabelText(helpLib.isDefaultLowerNavLabelTextOn());
   }

   public HelpFontSettings getManualFontSettings()
   {
      HelpFontSettings fontSettings = new HelpFontSettings();
      String val = getProperty("manual.body_font");

      if (val != null && !val.isEmpty())
      {
         fontSettings.setBodyFontCssName(val);
      }

      val = getProperty("manual.body_font_size");

      if (val != null && !val.isEmpty())
      {
         try
         {
            fontSettings.setBodyFontSize(Integer.parseInt(val));
         }
         catch (NumberFormatException e)
         {// ignore if invalid
         }
      }

      val = getProperty("manual.icon_font");

      if (val != null && !val.isEmpty())
      {
         fontSettings.setIconFontCssName(val);
      }

      val = getProperty("manual.keystroke_font");

      if (val != null && !val.isEmpty())
      {
         fontSettings.setKeyStrokeFontCssName(val);
      }

      val = getProperty("manual.mono_font");

      if (val != null && !val.isEmpty())
      {
         fontSettings.setMonoFontCssName(val);
      }

      return fontSettings;
   }

   public void setManualFont(HelpFontSettings fontSettings)
   {
      if (fontSettings == null)
      {
         removeProperty("manual.body_font");
         removeProperty("manual.body_font_size");
         removeProperty("manual.icon_font");
         removeProperty("manual.keystroke_font");
         removeProperty("manual.mono_font");
      }
      else
      {
         setProperty("manual.body_font", fontSettings.getBodyFontCssName());
         setProperty("manual.body_font_size",
           ""+fontSettings.getBodyFontSize());
         setProperty("manual.icon_font", fontSettings.getIconFontCssName());
         setProperty("manual.keystroke_font", fontSettings.getKeyStrokeFontCssName());
         setProperty("manual.mono_font", fontSettings.getMonoFontCssName());
      }
   }

   public String getDefaultOutputFormatProperty()
   {
      return getProperty("fileformat");
   }

   @Override
   public void setDefaultOutputFormat(String fmt)
   {
      super.setDefaultOutputFormat(fmt);

      if (fmt == null)
      {
         removeProperty("fileformat");
      }
      else
      {
         setProperty("fileformat", fmt);
      }
   }

   @Override
   public void setOverrideInputFormat(boolean on)
   {
      super.setOverrideInputFormat(on);
      setProperty("fileformatoverride", ""+on);
   }

   public boolean getOverrideInputFormatProperty()
   {
      String val = getProperty("fileformatoverride");

      if (val == null)
      {
         return getOverrideInputFormat();
      }

      return Boolean.parseBoolean(val);
   }

   @Override
   public void setTeXEncodingProperty(Charset encoding)
   {
      super.setTeXEncodingProperty(encoding);

      if (encoding == null)
      {
         removeProperty("tex-encoding");
      }
      else
      {
         setProperty("tex-encoding", texEncodingName);
      }
   }

   @Override
   public void setTeXEncodingProperty(String encoding)
   {
      super.setTeXEncodingProperty(encoding);

      if (encoding == null)
      {
         removeProperty("tex-encoding");
      }
      else
      {
         setProperty("tex-encoding", encoding);
      }
   }

   @Override
   public void setCsvEncodingProperty(Charset encoding)
   {
      super.setCsvEncodingProperty(encoding);

      if (encoding == null)
      {
         removeProperty("csv-encoding");
      }
      else
      {
         setProperty("csv-encoding", csvEncodingName);
      }
   }

   @Override
   public void setCsvEncodingProperty(String encoding)
   {
      super.setCsvEncodingProperty(encoding);

      if (encoding == null)
      {
         removeProperty("csv-encoding");
      }
      else
      {
         setProperty("csv-encoding", encoding);
      }
   }

   public boolean isStringDbTeX3DatumValueProperty()
   {
      String prop = getProperty("dbtex3-string");

      if (prop == null || prop.isEmpty())
      {
         return isStringDbTeX3DatumValue();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setStringDbTeX3DatumValue(boolean enable)
   {
      super.setStringDbTeX3DatumValue(enable);
      setProperty("dbtex3-string", ""+enable);
   }

   public boolean isIntegerDbTeX3DatumValueProperty()
   {
      String prop = getProperty("dbtex3-integer");

      if (prop == null || prop.isEmpty())
      {
         return isIntegerDbTeX3DatumValue();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setIntegerDbTeX3DatumValue(boolean enable)
   {
      super.setIntegerDbTeX3DatumValue(enable);
      setProperty("dbtex3-integer", ""+enable);
   }

   public boolean isDecimalDbTeX3DatumValueProperty()
   {
      String prop = getProperty("dbtex3-decimal");

      if (prop == null || prop.isEmpty())
      {
         return isDecimalDbTeX3DatumValue();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setDecimalDbTeX3DatumValue(boolean enable)
   {
      super.setDecimalDbTeX3DatumValue(enable);
      setProperty("dbtex3-decimal", ""+enable);
   }

   public boolean isCurrencyDbTeX3DatumValueProperty()
   {
      String prop = getProperty("dbtex3-currency");

      if (prop == null || prop.isEmpty()) return true;

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setCurrencyDbTeX3DatumValue(boolean enable)
   {
      super.setCurrencyDbTeX3DatumValue(enable);
      setProperty("dbtex3-currency", ""+enable);
   }

   public DbTeX3DatumValue getDbTeX3DatumValueProperty()
   {
      String prop = getProperty("dbtex-3-datum");

      if (prop == null || prop.isEmpty())
      {
         dbtex3DatumValue = DbTeX3DatumValue.HEADER;
      }
      else
      {
         try
         {
            dbtex3DatumValue = DbTeX3DatumValue.valueOf(prop);
         }
         catch (IllegalArgumentException e)
         {
            dbtex3DatumValue = DbTeX3DatumValue.HEADER;
            messageHandler.debug(e);
         }
      }

      return dbtex3DatumValue;
   }

   @Override
   public void setDbTeX3DatumValue(DbTeX3DatumValue value)
   {
      super.setDbTeX3DatumValue(value);
      setProperty("dbtex-3-datum", value.toString());
   }

   public boolean useSIforDecimalsProperty()
   {
      String prop = getProperty("si-decimals");

      if (prop == null)
      {
         return useSIforDecimals();
      }

      return Boolean.valueOf(prop);
   }

   @Override
   public void setSIforDecimals(boolean enable)
   {
      super.setSIforDecimals(enable);
      setProperty("si-decimals", ""+enable);
   }

   public void setIntegerFormatterProperty(DecimalFormat format)
   {
      super.setIntegerFormatter(format);

      if (format == null)
      {
         removeProperty("integer-formatter");
      }
      else
      {
         setProperty("integer-formatter", format.toPattern());
      }
   }

   public DecimalFormat getIntegerFormatterProperty()
   {
      String prop = getProperty("integer-formatter");

      if (!(prop == null || prop.isEmpty()))
      {
         try
         {
            Locale locale = getNumericLocale();

            integerFormat 
               = new DecimalFormat(prop, DecimalFormatSymbols.getInstance(locale));
            integerFormat.setParseIntegerOnly(true);
         }
         catch (Throwable e)
         {
            messageHandler.debug(e);
         }
      }

      return integerFormat;
   }

   public void setCurrencyFormatterProperty(DecimalFormat format)
   {
      super.setCurrencyFormatter(format);

      if (format == null)
      {
         removeProperty("currency-formatter");
      }
      else
      {
         setProperty("currency-formatter", format.toPattern());
      }
   }

   public DecimalFormat getCurrencyFormatterProperty()
   {
      String prop = getProperty("currency-formatter");

      if (!(prop == null || prop.isEmpty()))
      {
         try
         {
            Locale locale = getNumericLocale();

            currencyFormat 
               = new DecimalFormat(prop, DecimalFormatSymbols.getInstance(locale));
            currencyFormat.setParseIntegerOnly(false);
         }
         catch (Throwable e)
         {
            messageHandler.debug(e);
         }
      }

      return currencyFormat;
   }

   public void setDecimalFormatterProperty(DecimalFormat format)
   {
      super.setDecimalFormatter(format);

      if (format == null)
      {
         removeProperty("decimal-formatter");
      }
      else
      {
         setProperty("decimal-formatter", format.toPattern());
      }
   }

   public DecimalFormat getDecimalFormatterProperty()
   {
      String prop = getProperty("decimal-formatter");

      if (!(prop == null || prop.isEmpty()))
      {
         try
         {
            Locale locale = getNumericLocale();

            decimalFormat 
               = new DecimalFormat(prop, DecimalFormatSymbols.getInstance(locale));
            decimalFormat.setParseIntegerOnly(false);
         }
         catch (Throwable e)
         {
            messageHandler.debug(e);
         }
      }

      return currencyFormat;
   }

   public void setNumericParserProperty(DecimalFormat format)
   {
      super.setNumericParser(format);

      if (format == null)
      {
         removeProperty("numeric-parser");
      }
      else
      {
         setProperty("numeric-parser", format.toPattern());
      }
   }

   public DecimalFormat getNumericParserProperty()
   {
      Locale locale = getNumericLocale();
      String prop = getProperty("numeric-parser");

      if (prop != null && !prop.isEmpty())
      {
         return new DecimalFormat(prop, DecimalFormatSymbols.getInstance(locale));
      }

      return numericParser;
   }

   public Locale getNumericLocaleProperty()
   {
      String prop = getProperty("numeric-locale");

      Locale locale;

      if (prop != null && !prop.isEmpty())
      {
         locale = Locale.forLanguageTag(prop.replaceAll("_", "-"));

         if (locale.toLanguageTag().equals("und"))
         {
            System.err.println("Unknown language tag '"+prop+"'");
         }
         else
         {
            return locale;
         }
      }

      locale = getLocaleProperty("sort-locale",
         Locale.getDefault(Locale.Category.FORMAT));

      setProperty("numeric-locale", locale.toLanguageTag());

      return locale;
   }

   public void setNumericLocaleProperty(Locale locale)
   {
      setNumericLocale(locale);

      if (locale == null)
      {
         removeProperty("numeric-locale");
      }
      else
      {
         setProperty("numeric-locale", locale.toString());
      }
   }

   public String getSortLocaleProperty()
   {
      return getProperty("sort-locale");
   }

   public void setSortLocaleProperty(Locale locale)
   {
      setSortLocale(locale);

      if (locale == null)
      {
         removeProperty("sort-locale");
      }
      else
      {
         setProperty("sort-locale", locale.toLanguageTag());
      }
   }

   public void setSortLocaleProperty(String locale)
   {
      setSortLocale(locale);

      if (locale == null || locale.isEmpty())
      {
         removeProperty("sort-locale");
      }
      else
      {
         setProperty("sort-locale", locale);
      }
   }

   @Override
   public void setSeparatorProperty(int separator)
   {
      super.setSeparatorProperty(separator);
      setProperty("sep", new String(Character.toChars(separator)));
   }

   @Override
   public int getSeparatorProperty()
   {
      String prop = getProperty("sep");

      if (prop != null && !prop.isEmpty())
      {
         return prop.codePointAt(0);
      }
      else
      {
         return super.getSeparatorProperty();
      }
   }

   @Override
   public void setDelimiterProperty(int delimiter)
   {
      super.setDelimiterProperty(delimiter);
      setProperty("delim", new String(Character.toChars(delimiter)));
   }

   @Override
   public int getDelimiterProperty()
   {
      String prop = getProperty("delim");

      if (prop != null && !prop.isEmpty())
      {
         return prop.codePointAt(0);
      }
      else
      {
         return super.getDelimiterProperty();
      }
   }

   @Override
   public String getSqlHostProperty()
   {
      String prop = getProperty("sqlHost");

      if (prop != null && !prop.isEmpty())
      {
         return prop;
      }
      else
      {
         return super.getSqlHostProperty();
      }
   }

   @Override
   public void setSqlHostProperty(String host)
   {
      super.setSqlHostProperty(host);
      setProperty("sqlHost", host);
   }

   @Override
   public String getSqlPrefixProperty()
   {
      String prop = getProperty("sqlPrefix");

      if (prop != null && !prop.isEmpty())
      {
         return prop;
      }
      else
      {
         return super.getSqlPrefixProperty();
      }
   }

   @Override
   public void setSqlPrefixProperty(String prefix)
   {
      super.setSqlPrefixProperty(prefix);
      setProperty("sqlPrefix", prefix);
   }

   @Override
   public int getSqlPortProperty()
   {
      String prop = getProperty("sqlPort");

      if (prop == null || prop.isEmpty())
      {
         return super.getSqlPortProperty();
      }

      try
      {
         return Integer.parseInt(prop);
      }
      catch (NumberFormatException e)
      {
         // This shouldn't happen unless someone messes around with
         // the properties file

         setSqlPortProperty(sqlPort);

         throw new IllegalArgumentException(
            "Invalid port number "+prop, e);
      }
   }

   @Override
   public void setSqlPortProperty(int port)
   {
      super.setSqlPortProperty(port);
      setProperty("sqlPort", ""+port);
   }

   @Override
   public String getSqlDbNameProperty()
   {
      String prop = getProperty("sqlDbName");

      if (prop != null)
      {
         return prop;
      }

      return super.getSqlDbNameProperty();
   }

   @Override
   public void setSqlDbNameProperty(String name)
   {
      super.setSqlDbNameProperty(name);

      if (name == null)
      {
         removeProperty("sqlDbName");
      }
      else
      {
         setProperty("sqlDbName", name);
      }
   }

   @Override
   public void setWipePasswordProperty(boolean wipePassword)
   {
      super.setWipePasswordProperty(wipePassword);
      setProperty("wipePassword", ""+wipePassword);
   }

   @Override
   public boolean isWipePasswordEnabledProperty()
   {
      String prop = getProperty("wipePassword");

      if (prop == null)
      {
         return super.isWipePasswordEnabledProperty();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setSqlUserProperty(String username)
   {
      super.setSqlUserProperty(username);

      if (username == null)
      {
         removeProperty("sqlUser");
      }
      else
      {
         setProperty("sqlUser", username);
      }
   }

   @Override
   public String getSqlUserProperty()
   {
      String prop = getProperty("sqlUser");

      if (prop != null)
      {
         return prop;
      }
      else
      {
         return super.getSqlUserProperty();
      }
   }

   @Override
   public boolean hasCSVHeaderProperty()
   {
      String prop = getProperty("csvHasHeader");

      if (prop == null)
      {
         return super.hasCSVHeaderProperty();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setHasCSVHeaderProperty(boolean hasHeader)
   {
      super.setHasCSVHeaderProperty(hasHeader);
      setProperty("csvHasHeader", ""+hasHeader);
   }

   @Override
   public boolean hasCSVstrictquotesProperty()
   {
      String prop = getProperty("csvstrictquotes");

      if (prop == null)
      {
         return super.hasCSVstrictquotesProperty();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setCSVstrictquotesProperty(boolean strictquotes)
   {
      super.setCSVstrictquotesProperty(strictquotes);
      setProperty("csvstrictquotes", ""+strictquotes);
   }

// TODO deprecate?
// make equivalent to EscapeCharsOption.ESC_DELIM_BKSL/DOUBLE_DELIM
   public int getCSVescape()
   {
      String prop = getProperty("csvescape");

      if (prop == null)
      {
         setCSVescape("\\");
         return '\\';
      }

      return prop.isEmpty() || prop.equals("\\0") ? 0 : prop.codePointAt(0);
   }

   public void setCSVescape(String esc)
   {
      setProperty("csvescape", esc);
   }

   public void setCSVescape(int esc)
   {
      setProperty("csvescape", esc == 0 ? "" : new String(Character.toChars(esc)));
   }

   // deprecate? use csv-blank
   public void setSkipEmptyRows(boolean enable)
   {
      setProperty("skip-empty-rows", ""+enable);
   }

   @Deprecated
   public boolean isSkipEmptyRowsOn()
   {
      return getCsvBlankOption() == CsvBlankOption.IGNORE;
   }

   @Override
   public void setCsvBlankOptionProperty(CsvBlankOption opt)
   {
      super.setCsvBlankOptionProperty(opt);
      setProperty("csv-blank", opt.toString());
   }

   @Override
   public CsvBlankOption getCsvBlankOptionProperty()
   {
      String val = getProperty("csv-blank");

      if (val != null)
      {
         return CsvBlankOption.valueOf(val);
      }

      return super.getCsvBlankOptionProperty();
   }

   @Override
   public void setEscapeCharsOptionProperty(EscapeCharsOption opt)
   {
      super.setEscapeCharsOptionProperty(opt);
      setProperty("csv-esc-chars", opt.toString());
   }

   @Override
   public EscapeCharsOption getEscapeCharsOptionProperty()
   {
      String val = getProperty("csv-esc-chars");

      if (val != null)
      {
         return EscapeCharsOption.valueOf(val);
      }

      return super.getEscapeCharsOptionProperty();
   }

   public void setAddDelimiterOptionProperty(AddDelimiterOption opt)
   {
      setAddDelimiterOption(opt);
      setProperty("csv-add-delim", opt.toString());
   }

   public AddDelimiterOption getAddDelimiterOptionProperty()
   {
      String val = getProperty("csv-add-delim");

      if (val != null)
      {
         return AddDelimiterOption.valueOf(val);
      }

      return getAddDelimiterOption();
   }

   @Override
   public int getCSVskiplinesProperty()
   {
      String prop = getProperty("csvskiplines");

      if (prop == null)
      {
         return super.getCSVskiplinesProperty();
      }

      try
      {
         return Integer.parseInt(prop);
      }
      catch (NumberFormatException e)
      {
         getMessageHandler().debug(e);
         return super.getCSVskiplinesProperty();
      }
   }

   @Override
   public void setCSVskiplinesProperty(int lines)
   throws IllegalArgumentException
   {
      super.setCSVskiplinesProperty(lines);
      setProperty("csvskiplines", ""+lines);
   }

   public void setStartUp(int category)
   {
      if (category < 0 || category > STARTUP_CUSTOM)
      {
         throw new IllegalArgumentException(
           "Invalid startup category "+category);
      }

      setProperty("startup", ""+category);
   }

   public int getStartUp()
   {
      String prop = getProperty("startup");

      if (prop == null)
      {
         setStartUp(STARTUP_HOME);
         return STARTUP_HOME;
      }

      try
      {
         int result = Integer.parseInt(prop);

         if (result < 0 || result > STARTUP_CUSTOM)
         {
            messageHandler.debug("Invalid startup setting '"+prop+"'");
            return STARTUP_HOME;
         }

         return result;
      }
      catch (NumberFormatException e)
      {
         messageHandler.debug("Invalid startup setting '"+prop+"'");
         return STARTUP_HOME;
      }
   }

   public void directoryOnExit(File file)
   {
      if (getStartUp() == STARTUP_LAST)
      {
         setProperty("startupdir", file.getAbsolutePath());
      }
   }

   public void setCustomStartUp(File file)
   {
      setStartUp(STARTUP_CUSTOM);
      setProperty("startupdir", file.getAbsolutePath());
   }

   public File getStartUpDirectory()
   {
      switch (getStartUp())
      {
         case STARTUP_HOME:
            return new File(System.getProperty("user.home"));
         case STARTUP_CWD:
            return new File(".");
      }

      String name = getProperty("startupdir");

      if (name == null)
      {
         return new File(System.getProperty("user.home"));
      }

      return new File(name);
   }

   @Override
   public boolean isImportEmptyToNullOnProperty()
   {
      String prop = getProperty("impemptytonull");

      if (prop == null || prop.isEmpty())
      {
         return super.isImportEmptyToNullOnProperty();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setImportEmptyToNullProperty(boolean on)
   {
      super.setImportEmptyToNullProperty(on);
      setProperty("impemptytonull", ""+on);
   }

   @Override
   public boolean isSolutionEnvStrippedProperty()
   {
      String prop = getProperty("probsolnstripenv");

      if (prop == null || prop.isEmpty())
      {
         return super.isSolutionEnvStrippedProperty();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setSolutionEnvStrippedProperty(boolean stripEnv)
   {
      super.setSolutionEnvStrippedProperty(stripEnv);
      setProperty("probsolnstripenv", ""+stripEnv);
   }

   @Override
   public boolean isPreambleOnlyProperty()
   {
      String prop = getProperty("preambleonly");

      if (prop == null || prop.isEmpty())
      {
         return super.isPreambleOnlyProperty();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setPreambleOnlyProperty(boolean on)
   {
      super.setPreambleOnlyProperty(on);
      setProperty("preambleonly", ""+on);
   }

   public void setOwnerOnlyProperty(boolean enable)
   {
      setOwnerOnly(enable);
      setProperty("owneronly", ""+enable);
   }

   public boolean isOwnerOnlyProperty()
   {
      String prop = getProperty("owneronly");

      if (prop == null || prop.isEmpty())
      {
         return isOwnerOnly();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setAutoTrimLabelsProperty(boolean enable)
   {
      super.setAutoTrimLabelsProperty(enable);
      setProperty("trimlabels", ""+enable);
   }

   @Override
   public boolean isAutoTrimLabelsOnProperty()
   {
      String prop = getProperty("trimlabels");

      if (prop == null || prop.isEmpty())
      {
         return isAutoTrimLabelsOnProperty();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setTrimElementProperty(boolean enable)
   {
      super.setTrimElementProperty(enable);
      setProperty("trimelement", ""+enable);
   }

   @Override
   public boolean isTrimElementOnProperty()
   {
      String prop = getProperty("trimelement");

      if (prop == null || prop.isEmpty())
      {
         return super.isTrimElementOnProperty();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public boolean isLiteralContentProperty()
   {
      String prop = getProperty("literalcontent");

      if (prop == null || prop.isEmpty())
      {
         return super.isLiteralContentProperty();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setLiteralContentProperty(boolean on)
   {
      super.setLiteralContentProperty(on);
      setProperty("literalcontent", ""+on);
   }

   @Override
   public void setTeXMappingProperty(boolean enable)
   {
      super.setTeXMappingProperty(enable);
      setProperty("subtexspecials", ""+enable);
   }

   @Override
   public boolean isTeXMappingOnProperty()
   {
      String prop = getProperty("subtexspecials");

      if (prop == null || prop.isEmpty())
      {
         return super.isTeXMappingOnProperty();
      }

      return Boolean.parseBoolean(prop);
   }

   public String getFontName()
   {
      return getProperty("fontname");
   }

   public void setFontName(String name)
   {
      setProperty("fontname", name);
   }

   public int getFontSize()
   {
      try
      {
         return Integer.parseInt(getProperty("fontsize"));
      }
      catch (NumberFormatException e)
      {
         setFontSize(12);
         return 12;
      }
   }

   public void setFontSize(int fontSize)
   {
      setProperty("fontsize", ""+fontSize);
   }

   public Font getFont()
   {
      return new Font(getFontName(), Font.PLAIN, getFontSize());
   }

   public void setCellWidth(int cellWidth, DatumType type)
   {
      String tag="";

      switch (type)
      {
         case STRING:
            tag = "string";
         break;
         case UNKNOWN:
            tag = "unset";
         break;
         case INTEGER:
            tag = "int";
         break;
         case DECIMAL:
            tag = "real";
         break;
         case CURRENCY:
            tag = "currency";
         break;
         default:
            assert false : "Invalid data type "+type;
      }

      setProperty("cellwidth."+tag, ""+cellWidth);
   }

   @Deprecated
   public void setCellWidth(int cellWidth, int type)
   {
      String tag;

      switch (type)
      {
         case TYPE_STRING:
            tag = "string";
         break;
         case TYPE_UNKNOWN:
            tag = "unset";
         break;
         case TYPE_INTEGER:
            tag = "int";
         break;
         case TYPE_REAL:
            tag = "real";
         break;
         case TYPE_CURRENCY:
            tag = "currency";
         break;
         default:
            throw new IllegalArgumentException(
              "setCellWidth(int,int): Invalid data type "+type);
      }

      setProperty("cellwidth."+tag, ""+cellWidth);
   }

   public int getCellWidth(DatumType type)
   {
      String tag;
      int defValue;

      switch (type)
      {
         case STRING:
            tag = "string";
            defValue = 300;
         break;
         case UNKNOWN:
            tag = "unset";
            defValue = 100;
         break;
         case INTEGER:
            tag = "int";
            defValue = 40;
         break;
         case DECIMAL:
            tag = "real";
            defValue = 60;
         break;
         case CURRENCY:
            tag = "currency";
            defValue = 150;
         break;
         default:
            throw new IllegalArgumentException(
              "getCellWidth(DatumType): Invalid data type "+type);
      }

      String prop = getProperty("cellwidth."+tag);

      if (prop == null)
      {
         setCellWidth(defValue, type);
         return defValue;
      }

      try
      {
         return Integer.parseInt(prop);
      }
      catch (NumberFormatException e)
      {
         setCellWidth(defValue, type);
         messageHandler.debug("Property 'cellwidth."+tag
           +"' should be an integer. Found: '"+prop+"'");
      }

      return defValue;
   }

   @Deprecated
   public int getCellWidth(int type)
   {
      String tag;
      int defValue;

      switch (type)
      {
         case TYPE_STRING:
            tag = "string";
            defValue = 300;
         break;
         case TYPE_UNKNOWN:
            tag = "unset";
            defValue = 100;
         break;
         case TYPE_INTEGER:
            tag = "int";
            defValue = 40;
         break;
         case TYPE_REAL:
            tag = "real";
            defValue = 60;
         break;
         case TYPE_CURRENCY:
            tag = "currency";
            defValue = 150;
         break;
         default:
            throw new IllegalArgumentException(
              "getCellWidth(int): Invalid data type "+type);
      }

      String prop = getProperty("cellwidth."+tag);

      if (prop == null)
      {
         setCellWidth(defValue, type);
         return defValue;
      }

      try
      {
         return Integer.parseInt(prop);
      }
      catch (NumberFormatException e)
      {
         setCellWidth(defValue, type);
         messageHandler.debug("Property 'cellwidth."+tag
           +"' should be an integer. Found: '"+prop+"'");
      }

      return defValue;
   }

   public boolean isCellDatumVisible()
   {
      String prop = getProperty("celldatumvisible");

      if (prop == null || prop.isEmpty())
      {
         setCellDatumVisible(true);
         return true;
      }

      return Boolean.parseBoolean(prop);
   }

   public void setCellDatumVisible(boolean visible)
   {
      setProperty("celldatumvisible", ""+visible);
   }

   public int getCellEditorPreferredHeight()
   {
      try
      {
         String val = getProperty("celleditorprefheight");

         if (val == null)
         {
            val = getProperty("celleditorheight");

            if (val != null)
            {
               int h = Integer.parseInt(val)*10;
               setProperty("celleditorprefheight", ""+h);
               removeProperty("celleditorheight");
               return h;
            }
         }
         else
         {
            return Integer.parseInt(val);
         }
      }
      catch (NumberFormatException e)
      {
      }

      setProperty("celleditorprefheight", "170");
      return 170;
   }

   public void setCellEditorPreferredHeight(int height)
   {
      setProperty("celleditorprefheight", ""+height);
   }

   public int getCellEditorPreferredWidth()
   {
      try
      {
         String val = getProperty("celleditorprefwidth");

         if (val == null)
         {
            val = getProperty("celleditorwidth");

            if (val != null)
            {
               int w = Integer.parseInt(val)*22;
               setProperty("celleditorprefwidth", ""+w);
               removeProperty("celleditorwidth");
               return w;
            }
         }
         else
         {
            return Integer.parseInt(val);
         }
      }
      catch (NumberFormatException e)
      {
      }

      setProperty("celleditorprefwidth", "220");
      return 220;
   }

   public void setCellEditorPreferredWidth(int width)
   {
      setProperty("celleditorprefwidth", ""+width);
   }

   @Deprecated
   public int getCellEditorHeight()
   {
      try
      {
         return Integer.parseInt(getProperty("celleditorheight"));
      }
      catch (NumberFormatException e)
      {
         return 10;
      }
   }

   @Deprecated
   public void setCellEditorHeight(int numLines)
   {
      setProperty("celleditorheight", ""+numLines);
   }

   @Deprecated
   public int getCellEditorWidth()
   {
      try
      {
         return Integer.parseInt(getProperty("celleditorwidth"));
      }
      catch (NumberFormatException e)
      {
         return 8;
      }
   }

   @Deprecated
   public void setCellEditorWidth(int maxCharsPerLine)
   {
      setProperty("celleditorwidth", ""+maxCharsPerLine);
   }

   public void setStringCellEditable(boolean enable)
   {
      setProperty("stringcelleditable", ""+enable);
   }

   public boolean isStringCellEditable()
   {
      String prop = getProperty("stringcelleditable");

      if (prop == null || prop.isEmpty())
      {
         setStringCellEditable(false);
         return false;
      }

      return Boolean.parseBoolean(prop);
   }

   public void setSyntaxHighlighting(boolean enable)
   {
      setProperty("syntaxhighlighting", ""+enable);
   }

   public boolean isSyntaxHighlightingOn()
   {
      String prop = getProperty("syntaxhighlighting");

      if (prop == null || prop.isEmpty())
      {
         setSyntaxHighlighting(true);
         return true;
      }

      return Boolean.parseBoolean(prop);
   }

   public Color getCellBackground()
   {
      String prop = getProperty("cellbackground");

      if (prop == null)
      {
         return Color.WHITE;
      }

      try
      {
         return new Color(Integer.parseInt(prop));
      }
      catch (NumberFormatException e)
      {
         return Color.WHITE;
      }
   }

   public void setCellBackground(Color background)
   {
      setProperty("cellbackground", ""+background.getRGB());
   }

   public Color getCellForeground()
   {
      String prop = getProperty("cellforeground");

      if (prop == null)
      {
         return Color.BLACK;
      }

      try
      {
         return new Color(Integer.parseInt(prop));
      }
      catch (NumberFormatException e)
      {
         return Color.BLACK;
      }
   }

   public void setCellForeground(Color foreground)
   {
      setProperty("cellforeground", ""+foreground.getRGB());
   }

   public Color getControlSequenceHighlight()
   {
      String prop = getProperty("highlightcs");

      if (prop == null)
      {
         setControlSequenceHighlight(Color.BLUE);
         return Color.BLUE;
      }

      try
      {
         return new Color(Integer.parseInt(prop));
      }
      catch (NumberFormatException e)
      {
         setControlSequenceHighlight(Color.BLUE);
         return Color.BLUE;
      }
   }

   public void setControlSequenceHighlight(Color highlight)
   {
      setProperty("highlightcs", ""+highlight.getRGB());
   }

   public Color getCommentHighlight()
   {
      String prop = getProperty("highlightcomment");

      if (prop == null)
      {
         setCommentHighlight(Color.GRAY);
         return Color.GRAY;
      }

      try
      {
         return new Color(Integer.parseInt(prop));
      }
      catch (NumberFormatException e)
      {
         setCommentHighlight(Color.GRAY);
         return Color.GRAY;
      }
   }

   public void setCommentHighlight(Color highlight)
   {
      setProperty("highlightcomment", ""+highlight.getRGB());
   }

   public int getCellHeight()
   {
      try
      {
         return Integer.parseInt(getProperty("cellheight"));
      }
      catch (NumberFormatException e)
      {
         setCellHeight(4);
         return 4;
      }
   }

   public void setCellHeight(int height)
   {
      setProperty("cellheight", ""+height);
   }

   public void setRandomSeedProperty(Long seed)
   {
      super.setRandomSeed(seed);
      setProperty("seed", seed == null ? "" : seed.toString());
   }

   public Long getRandomSeedProperty()
   {
      String prop = getProperty("seed");

      if (prop != null && !prop.isEmpty())
      {
         try
         {
            return Long.valueOf(prop);
         }
         catch (NumberFormatException e)
         {
         }
      }

      return getRandomSeed();
   }

   public int getShuffleIterationsProperty()
   {
      String prop = getProperty("shuffle.iter");

      if (prop != null && !prop.isEmpty())
      {
         try
         {
            return Integer.parseInt(prop);
         }
         catch (Exception e)
         {
         }
      }

      return getShuffleIterations();
   }

   public void setShuffleIterationsProperty(int number)
   {
      setShuffleIterations(number);
      setProperty("shuffle.iter", ""+number);
   }

   @Override
   public boolean isRedefNewProblemEnabledProperty()
   {
      String prop = getProperty("redefnewprob");

      if (prop == null)
      {
         return super.isRedefNewProblemEnabledProperty();
      }

      return Boolean.parseBoolean(prop);
   }

   @Override
   public void setRedefNewProblemProperty(boolean value)
   {
      super.setRedefNewProblemProperty(value);
      setProperty("redefnewprob", ""+value);
   }

   public Locale getLocaleProperty(String propName, Locale defaultValue)
   {
      String value = getProperty(propName);

      if (value == null)
      {
         return defaultValue;
      }

      return Locale.forLanguageTag(value);
   }

   public void setPerl(String perlExe)
   {
      setProperty("perl", perlExe);
   }

   public String getPerl()
   {
      return getProperty("perl");
   }

   @Override
   public void setDefaults()
   {
      super.setDefaults();
      setPerl("perl");

      setStartUp(STARTUP_HOME);
      setFontName("Monospaced");
      setFontSize(12);
      setCellHeight(4);
   }

   public DatatoolGuiResources getDatatoolGuiResources()
   {
      return messageHandler.getDatatoolGuiResources();
   }

   public Template[] getTemplates()
     throws java.net.URISyntaxException
   {
      File dir = new File(DatatoolTk.class.getResource(TEMPLATE_DIR).toURI());

      File[] mainList = dir.listFiles(new FilenameFilter()
      {
         public boolean accept(File directory, String name)
         {
            return name.toLowerCase().endsWith(".xml");
         }
      });

      File[] userList = null;

      int num = mainList.length;

      if (propertiesPath != null)
      {
         File templatesDir = new File(propertiesPath, "templates");

         if (templatesDir.exists() && templatesDir.isDirectory())
         {
            userList = templatesDir.listFiles(new FilenameFilter()
            {
               public boolean accept(File directory, String name)
               {
                  return name.toLowerCase().endsWith(".xml");
               }
            });

            num += userList.length;
         }
      }

      Template[] templates = new Template[num];

      for (int i = 0; i < num; i++)
      {
         templates[i] = new Template(i < mainList.length ?
            mainList[i] : userList[i-mainList.length]);
      }

      return templates;
   }

   public DatatoolPlugin[] getPlugins()
     throws java.net.URISyntaxException
   {
      File dir = new File(DatatoolTk.class.getResource(PLUGIN_DIR).toURI());

      File[] mainList = dir.listFiles(new FilenameFilter()
      {
         public boolean accept(File directory, String name)
         {
            return name.toLowerCase().endsWith(".pl");
         }
      });

      File[] userList = null;

      int num = mainList.length;

      if (propertiesPath != null)
      {
         File pluginsDir = new File(propertiesPath, "plugins");

         if (pluginsDir.exists() && pluginsDir.isDirectory())
         {
            userList = pluginsDir.listFiles(new FilenameFilter()
            {
               public boolean accept(File directory, String name)
               {
                  return name.toLowerCase().endsWith(".pl");
               }
            });

            num += userList.length;
         }
      }

      DatatoolPlugin[] plugins = new DatatoolPlugin[num];

      for (int i = 0; i < num; i++)
      {
         plugins[i] = new DatatoolPlugin(i < mainList.length ?
            mainList[i] : userList[i-mainList.length], messageHandler);
      }

      return plugins;
   }

   public boolean isNullFirstProperty()
   {
      String prop = getProperty("nullfirst");

      if (prop == null || prop.isEmpty())
      {
         return isNullFirst();
      }

      return Boolean.parseBoolean(prop);
   }

   public void setNullFirstProperty(boolean isFirst)
   {
      setNullFirst(isFirst);
      setProperty("nullfirst", ""+isFirst);
   }

   protected Properties properties;

   protected Vector<String> recentFiles;

   protected File propertiesPath = null;

   protected static final String PROPERTIES_NAME = "datatooltk.prop";

   protected static final String TEX_MAP_NAME = "texmap.prop";

   protected static final String RECENT_NAME = "recentfiles";

   protected final String CURRENCY_FILE_NAME = "currencies";

   protected final String LANGTAG_FILE_NAME = "languages";

   protected boolean upgrade=false;

   public static final int STARTUP_HOME   = 0;
   public static final int STARTUP_CWD    = 1;
   public static final int STARTUP_LAST   = 2;
   public static final int STARTUP_CUSTOM = 3;

   public static final String ICON_DIR = "/com/dickimawbooks/datatooltk/gui/icons/";
   public static final String HELPSETS = "helpsets";
   public static final String HELPSET_DIR = RESOURCES_PATH+"/"+HELPSETS;

   public static final String TEMPLATE_DIR = RESOURCES_PATH+"/templates/";

   public static final String PLUGIN_DIR = RESOURCES_PATH+"/plugins/";

   // old TeX map property setting with key "tex.<c>" where <c> is
   // the character to map
   public static final Pattern PATTERN_OLDER_TEX_MAP
    = Pattern.compile("tex\\.(.)");

   // TeX map property setting with key "tex.<hex>" where <hex> is
   // the character code point to map
   public static final Pattern PATTERN_OLD_TEX_MAP
    = Pattern.compile("tex\\.([0-9]{4})");

   public static final Pattern PATTERN_TEX_MAP
    = Pattern.compile("([0-9a-fA-F].+)=(.+)");

   public static final int TYPE_UNKNOWN=-1, TYPE_STRING = 0, TYPE_INTEGER=1,
     TYPE_REAL=2, TYPE_CURRENCY=3;

   public static final String DEFAULT_LARGE_ICON_SUFFIX = "-24";
   public static final String DEFAULT_SMALL_ICON_SUFFIX = "-16";
}
