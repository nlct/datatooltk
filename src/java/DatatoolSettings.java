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
package com.dickimawbooks.datatooltk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.charset.Charset;

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

import org.xml.sax.SAXException;

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
 * Application settings for datatooltk.
 * It's necessary to determine the preferred dictionary
 * language before initiating the message system, so the
 * localisation information is now stored in a separate simple file 
 * which reduces the amount of non-localised error messages.
 */
public class DatatoolSettings extends Properties
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
      super();

      messageHandler = new MessageHandler(datatooltk);
      importSettings = new ImportSettings(this);

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
       DatatoolTk.APP_NAME, RESOURCES_PATH,
       DICT_DIR, dictLocale, helpSetLocale,
       "texparserlib", RESOURCE_PREFIX);

      helpLib.setIconPath(ICON_DIR);
      helpLib.setSmallIconSuffix(DEFAULT_SMALL_ICON_SUFFIX);
      helpLib.setLargeIconSuffix(DEFAULT_LARGE_ICON_SUFFIX);

      recentFiles = new Vector<String>();
      currencies = new Vector<String>();

      setDefaults();
   }

   private void initLangTags() throws IOException
   {
      File file = new File(propertiesPath, LANGTAG_FILE_NAME);

      if (file.exists())
      {
         BufferedReader in = null;

         try
         {
            in = Files.newBufferedReader(file.toPath());

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

   private void initLabels()
   {
      TYPE_LABELS = new String[] 
      {
         messageHandler.getLabel("header.type.unset"),
         messageHandler.getLabel("header.type.string"),
         messageHandler.getLabel("header.type.int"),
         messageHandler.getLabel("header.type.real"),
         messageHandler.getLabel("header.type.currency")
      };

      TYPE_MNEMONICS = new int[] 
      {
         messageHandler.getMnemonicInt("header.type.unset"),
         messageHandler.getMnemonicInt("header.type.string"),
         messageHandler.getMnemonicInt("header.type.int"),
         messageHandler.getMnemonicInt("header.type.real"),
         messageHandler.getMnemonicInt("header.type.currency")
      };
   }

   public String getTypeLabel(DatumType type)
   {
      if (TYPE_LABELS == null)
      {
         initLabels();
      }

      return TYPE_LABELS[type.getValue()+1];
   }

   @Deprecated
   public String getTypeLabel(int type)
   {
      if (TYPE_LABELS == null)
      {
         initLabels();
      }

      return TYPE_LABELS[type];
   }

   public String[] getTypeLabels()
   {
      if (TYPE_LABELS == null)
      {
         initLabels();
      }

      return TYPE_LABELS;
   }

   public int getTypeMnemonic(DatumType type)
   {
      if (TYPE_MNEMONICS == null)
      {
         initLabels();
      }

      return TYPE_MNEMONICS[type.getValue()+1];
   }

   public int[] getTypeMnemonics()
   {
      if (TYPE_MNEMONICS == null)
      {
         initLabels();
      }

      return TYPE_MNEMONICS;
   }

   public static String getApplicationIconPath()
   {
      return RESOURCES_PATH + "/icons/datatooltk-logosmall.png";
   }

   private void setPropertiesPath()
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

            loadFromXML(in);
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
            in = Files.newBufferedReader(file.toPath());

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
         for (String key : stringPropertyNames())
         {
            Matcher m = PATTERN_OLDER_TEX_MAP.matcher(key);

            if (m.matches())
            {
               setTeXMap(m.group(1).codePointAt(0), getProperty(key));
               remove(key);
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

                  remove(key);
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
            in = Files.newBufferedReader(file.toPath());

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
            in = Files.newBufferedReader(file.toPath());

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
      if (propertiesPath == null) return;

      loadMainProperties();

      String lastVersion = getProperty("version");

      if (lastVersion == null
            || lastVersion.compareTo(DatatoolTk.APP_VERSION) < 0)
      {
         upgrade=true;

         if (lastVersion == null
            || lastVersion.compareTo("2.0") < 0)
         {
            for (String key : stringPropertyNames())
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

                  remove(key);
               }
               else if (key.equals("helpset") || key.equals("dictionary"))
               {
                  remove(key);
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

         storeToXML(out, null);
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
         out = new PrintWriter(Files.newBufferedWriter(file.toPath()));

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
         out = new PrintWriter(Files.newBufferedWriter(file.toPath()));

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
         out = new PrintWriter(Files.newBufferedWriter(file.toPath()));

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
         out = new PrintWriter(Files.newBufferedWriter(file.toPath()));

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

   public int getInitialRowCapacity()
   {
      String prop = getProperty("initial-row-capacity");

      int capacity = 100;

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

   public void setInitialRowCapacity(int capacity)
   {
      setProperty("initial-row-capacity", ""+capacity);
   }

   public int getInitialColumnCapacity()
   {
      String prop = getProperty("initial-column-capacity");

      int capacity = 10;

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

   public void setInitialColumnCapacity(int capacity)
   {
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
         remove("lookandfeel");
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
         remove("manual.body_font");
         remove("manual.body_font_size");
         remove("manual.icon_font");
         remove("manual.keystroke_font");
         remove("manual.mono_font");
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

   public String getDefaultOutputFormat()
   {
      return getProperty("fileformat");
   }

   public void applyDefaultOutputFormat(IOSettings ioSettings)
   {
      String val = getProperty("fileformat");

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
      if (fmt == null)
      {
         remove("fileformat");
      }
      else
      {
         setProperty("fileformat", fmt);
      }
   }

   public void setOverrideInputFormat(boolean on)
   {
      setProperty("fileformatoverride", ""+on);
   }

   public boolean getOverrideInputFormat()
   {
      String val = getProperty("fileformatoverride");

      if (val == null)
      {
         return false;
      }

      return Boolean.parseBoolean(val);
   }

   public Charset getTeXEncoding()
   {
      return importSettings.getTeXEncoding();
   }

   public String getTeXEncodingProperty()
   {
      return getProperty("tex-encoding");
   }

   public void setTeXEncoding(Charset encoding)
   {
      importSettings.setTeXEncoding(encoding);
   }

   public void setTeXEncodingProperty(Charset encoding)
   {
      if (encoding == null)
      {
         remove("tex-encoding");
      }
      else
      {
         setProperty("tex-encoding", encoding.name());
      }
   }

   public void setTeXEncodingProperty(String encoding)
   {
      if (encoding == null)
      {
         remove("tex-encoding");
      }
      else
      {
         setProperty("tex-encoding", encoding);
      }
   }

   public Charset getCsvEncoding()
   {
      return importSettings.getCsvEncoding();
   }

   public String getCsvEncodingProperty()
   {
      return getProperty("csv-encoding");
   }

   public void setCsvEncoding(Charset encoding)
   {
      importSettings.setCsvEncoding(encoding);
   }

   public void setCsvEncodingProperty(Charset encoding)
   {
      if (encoding == null)
      {
         remove("csv-encoding");
      }
      else
      {
         setProperty("csv-encoding", encoding.name());
      }
   }

   public void setCsvEncodingProperty(String encoding)
   {
      if (encoding == null)
      {
         remove("csv-encoding");
      }
      else
      {
         setProperty("csv-encoding", encoding);
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
      String prop = getProperty("dbtex3-string");

      if (prop == null || prop.isEmpty()) return false;

      return Boolean.parseBoolean(prop);
   }

   public void setStringDbTeX3DatumValue(boolean enable)
   {
      setProperty("dbtex3-string", ""+enable);
   }

   public boolean isIntegerDbTeX3DatumValue()
   {
      String prop = getProperty("dbtex3-integer");

      if (prop == null || prop.isEmpty()) return false;

      return Boolean.parseBoolean(prop);
   }

   public void setIntegerDbTeX3DatumValue(boolean enable)
   {
      setProperty("dbtex3-integer", ""+enable);
   }

   public boolean isDecimalDbTeX3DatumValue()
   {
      String prop = getProperty("dbtex3-decimal");

      if (prop == null || prop.isEmpty()) return true;

      return Boolean.parseBoolean(prop);
   }

   public void setDecimalDbTeX3DatumValue(boolean enable)
   {
      setProperty("dbtex3-decimal", ""+enable);
   }

   public boolean isCurrencyDbTeX3DatumValue()
   {
      String prop = getProperty("dbtex3-currency");

      if (prop == null || prop.isEmpty()) return true;

      return Boolean.parseBoolean(prop);
   }

   public void setCurrencyDbTeX3DatumValue(boolean enable)
   {
      setProperty("dbtex3-currency", ""+enable);
   }

   public DbTeX3DatumValue getDbTeX3DatumValue()
   {
      if (dbtex3DatumValue == null)
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
      }

      return dbtex3DatumValue;
   }

   public void setDbTeX3DatumValue(DbTeX3DatumValue value)
   {
      dbtex3DatumValue = value;
      setProperty("dbtex-3-datum", value.toString());
   }

   public boolean useSIforDecimals()
   {
      String prop = getProperty("si-decimals");

      if (prop == null)
      {
         return false;
      }

      return Boolean.valueOf(prop);
   }

   public void setSIforDecimals(boolean enable)
   {
      setProperty("si-decimals", ""+enable);
   }

   public boolean isIntegerFormatterSet()
   {
      String prop = getProperty("integer-formatter");
      return prop != null && !prop.isEmpty();
   }

   public void setIntegerFormatter(DecimalFormat format)
   {
      if (format == null)
      {
         remove("integer-formatter");
      }
      else
      {
         setProperty("integer-formatter", format.toPattern());
      }
   }

   public boolean isCurrencyFormatterSet()
   {
      String prop = getProperty("currency-formatter");
      return prop != null && !prop.isEmpty();
   }

   public void setCurrencyFormatter(DecimalFormat format)
   {
      if (format == null)
      {
         remove("currency-formatter");
      }
      else
      {
         setProperty("currency-formatter", format.toPattern());
      }
   }

   public boolean isDecimalFormatterSet()
   {
      String prop = getProperty("decimal-formatter");
      return prop != null && !prop.isEmpty();
   }

   public void setDecimalFormatter(DecimalFormat format)
   {
      if (format == null)
      {
         remove("decimal-formatter");
      }
      else
      {
         setProperty("decimal-formatter", format.toPattern());
      }
   }

   public NumberFormat getNumericFormatter(DatumType type)
   {
      String prop = null;

      switch (type)
      {
         case INTEGER:
            prop = getProperty("integer-formatter");
         break;
         case CURRENCY:
            prop = getProperty("currency-formatter");
            break;
         default:
            prop = getProperty("decimal-formatter");
      }

      Locale locale = getNumericLocale();

      if (prop != null && !prop.isEmpty())
      {
         NumberFormat numfmt
            = new DecimalFormat(prop, DecimalFormatSymbols.getInstance(locale));

         numfmt.setParseIntegerOnly(type==DatumType.INTEGER);

         return numfmt;
      }

      switch (type)
      {
         case INTEGER:
           return NumberFormat.getIntegerInstance(locale);
         case CURRENCY:
           return NumberFormat.getCurrencyInstance(locale);
      }

      return NumberFormat.getInstance(locale);
   }

   public void setNumericParser(DecimalFormat format)
   {
      if (format == null)
      {
         remove("numeric-parser");
      }
      else
      {
         setProperty("numeric-parser", format.toPattern());
      }
   }

   public boolean isNumericParserSet()
   {
      String prop = getProperty("numeric-parser");
      return prop != null && !prop.isEmpty();
   }

   public NumberFormat getNumericParser()
   {
      Locale locale = getNumericLocale();
      String prop = getProperty("numeric-parser");

      if (prop != null && !prop.isEmpty())
      {
         return new DecimalFormat(prop, DecimalFormatSymbols.getInstance(locale));
      }

      return NumberFormat.getInstance(locale);
   }

   public Locale getNumericLocale()
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

   public void setNumericLocale(Locale locale)
   {
      if (locale == null)
      {
         remove("numeric-locale");
      }
      else
      {
         setProperty("numeric-locale", locale.toString());
      }
   }

   public Collator getSortCollator()
   {
      return sortCollator;
   }

   public String getSortLocale()
   {
      return getProperty("sort-locale");
   }

   public void setSortLocale(Locale locale)
   {
      if (locale == null)
      {
         remove("sort-locale");
         sortCollator = null;
      }
      else
      {
         setProperty("sort-locale", locale.toLanguageTag());
         sortCollator = Collator.getInstance(locale);
      }
   }

   public void setSortLocale(String locale)
   {
      if (locale == null || "".equals(locale))
      {
         remove("sort-locale");
         sortCollator = null;
      }
      else
      {
         setProperty("sort-locale", locale);
         sortCollator = Collator.getInstance(Locale.forLanguageTag(locale));
      }
   }

   public void setSeparator(int separator)
   {
      importSettings.setSeparator(separator);
   }

   public void setSeparatorProperty(int separator)
   {
      setProperty("sep", MessageHandler.codePointToString(separator));
   }

   public int getSeparator()
   {
      return importSettings.getSeparator();
   }

   public int getSeparatorProperty()
   {
      return getProperty("sep").codePointAt(0);
   }

   public void setDelimiter(int delimiter)
   {
      importSettings.setDelimiter(delimiter);
   }

   public void setDelimiterProperty(int delimiter)
   {
      setProperty("delim", MessageHandler.codePointToString(delimiter));
   }

   public int getDelimiter()
   {
      return importSettings.getDelimiter();
   }

   public int getDelimiterProperty()
   {
      return getProperty("delim").codePointAt(0);
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

   public String getSqlHostProperty()
   {
      return getProperty("sqlHost");
   }

   public String getSqlPrefix()
   {
      return importSettings.getSqlPrefix();
   }

   public String getSqlPrefixProperty()
   {
      return getProperty("sqlPrefix");
   }

   public int getSqlPort()
   {
      return importSettings.getSqlPort();
   }

   public int getSqlPortProperty()
   {
      String prop = getProperty("sqlPort");

      if (prop == null)
      {
         setSqlPort(3306);
         return 3306;
      }

      try
      {
         return Integer.parseInt(prop);
      }
      catch (NumberFormatException e)
      {
         // This shouldn't happen unless someone messes around with
         // the properties file

         setSqlPort(3306);

         throw new IllegalArgumentException(
            "Invalid port number "+prop, e);
      }
   }

   public void setSqlHost(String host)
   {
      importSettings.setSqlHost(host);
   }

   public void setSqlHostProperty(String host)
   {
      setProperty("sqlHost", host);
   }

   public void setSqlPrefix(String prefix)
   {
      importSettings.setSqlPrefix(prefix);
   }

   public void setSqlPrefixProperty(String prefix)
   {
      setProperty("sqlPrefix", prefix);
   }

   public void setSqlPort(int port)
   {
      importSettings.setSqlPort(port);
   }

   public void setSqlPortProperty(int port)
   {
      setProperty("sqlPort", ""+port);
   }

   public boolean hasCSVHeader()
   {
      return importSettings.hasHeaderRow();
   }

   public boolean hasCSVHeaderProperty()
   {
      String prop = getProperty("csvHasHeader");

      if (prop == null)
      {
         setHasCSVHeader(true);
         return true;
      }

      return Boolean.parseBoolean(prop);
   }

   public void setHasCSVHeader(boolean hasHeader)
   {
      importSettings.setHasHeaderRow(hasHeader);
   }

   public void setHasCSVHeaderProperty(boolean hasHeader)
   {
      setProperty("csvHasHeader", ""+hasHeader);
   }

   public boolean hasCSVstrictquotes()
   {
      return importSettings.isStrictQuotesOn();
   }

   public boolean hasCSVstrictquotesProperty()
   {
      String prop = getProperty("csvstrictquotes");

      if (prop == null)
      {
         setCSVstrictquotes(false);
         return true;
      }

      return Boolean.parseBoolean(prop);
   }

   public void setCSVstrictquotes(boolean strictquotes)
   {
      importSettings.setStrictQuotes(strictquotes);
   }

   public void setCSVstrictquotesProperty(boolean strictquotes)
   {
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
      setProperty("csvescape", esc == 0 ? "" : MessageHandler.codePointToString(esc));
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

   public void setCsvBlankOption(CsvBlankOption opt)
   {
      importSettings.setBlankRowAction(opt);
   }

   public void setCsvBlankOptionProperty(CsvBlankOption opt)
   {
      setProperty("csv-blank", opt.toString());
   }

   public CsvBlankOption getCsvBlankOption()
   {
      return importSettings.getBlankRowAction();
   }

   public CsvBlankOption getCsvBlankOptionProperty()
   {
      String val = getProperty("csv-blank");

      if (val != null)
      {
         return CsvBlankOption.valueOf(val);
      }

      return CsvBlankOption.IGNORE;
   }

   public void setEscapeCharsOption(EscapeCharsOption opt)
   {
      importSettings.setEscapeCharsOption(opt);
   }

   public void setEscapeCharsOptionProperty(EscapeCharsOption opt)
   {
      setProperty("csv-esc-chars", opt.toString());
   }

   public EscapeCharsOption getEscapeCharsOption()
   {
      return importSettings.getEscapeCharsOption();
   }

   public EscapeCharsOption getEscapeCharsOptionProperty()
   {
      String val = getProperty("csv-esc-chars");

      if (val != null)
      {
         return EscapeCharsOption.valueOf(val);
      }

      return EscapeCharsOption.DOUBLE_DELIM;
   }

   public void setAddDelimiterOption(AddDelimiterOption opt)
   {
      setProperty("csv-add-delim", opt.toString());
   }

   public AddDelimiterOption getAddDelimiterOption()
   {
      String val = getProperty("csv-add-delim");

      if (val != null)
      {
         return AddDelimiterOption.valueOf(val);
      }

      return AddDelimiterOption.DETECT;
   }

   public int getCSVskiplines()
   {
      return importSettings.getSkipLines();
   }

   public int getCSVskiplinesProperty()
   {
      String prop = getProperty("csvskiplines");

      if (prop == null)
      {
         setCSVskiplines(0);
         return 0;
      }

      try
      {
         return Integer.parseInt(prop);
      }
      catch (NumberFormatException e)
      {
         getMessageHandler().debug(e);
         setCSVskiplines(0);
         return 0;
      }
   }

   public void setCSVskiplines(int lines)
   throws IllegalArgumentException
   {
      importSettings.setSkipLines(lines);
   }

   public void setCSVskiplinesProperty(int lines)
   throws IllegalArgumentException
   {
      if (lines < 0)
      {
         throw new IllegalArgumentException("Invalid skip lines value: "+lines);
      }

      setProperty("csvskiplines", ""+lines);
   }

   public String getSqlDbName()
   {
      return importSettings.getSqlDbName();
   }

   public String getSqlDbNameProperty()
   {
      return getProperty("sqlDbName");
   }

   public void setSqlDbName(String name)
   {
      importSettings.setSqlDbName(name);
   }

   public void setSqlDbNameProperty(String name)
   {
      setProperty("sqlDbName", name);
   }

   public void setWipePassword(boolean wipePassword)
   {
      importSettings.setWipePassword(wipePassword);
   }

   public void setWipePasswordProperty(boolean wipePassword)
   {
      setProperty("wipePassword", ""+wipePassword);
   }

   public boolean isWipePasswordEnabled()
   {
      return importSettings.isWipePasswordOn();
   }

   public boolean isWipePasswordEnabledProperty()
   {
      String prop = getProperty("wipePassword");

      if (prop == null)
      {
         setWipePassword(false);
         return false;
      }

      return Boolean.parseBoolean(prop);
   }

   public void setSqlUser(String username)
   {
      importSettings.setSqlUser(username);
   }

   public void setSqlUserProperty(String username)
   {
      setProperty("sqlUser", username);
   }

   public String getSqlUser()
   {
      return importSettings.getSqlUser();
   }

   public String getSqlUserProperty()
   {
      return getProperty("sqlUser");
   }

   public void setPasswordReader(DatatoolPasswordReader reader)
   {
      importSettings.setPasswordReader(reader);
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

   public void setTeXMapping(boolean enable)
   {
      importSettings.setMapChars(enable);
   }

   public void setTeXMappingProperty(boolean enable)
   {
      setProperty("subtexspecials", ""+enable);
   }

   public boolean isImportEmptyToNullOn()
   {
      return importSettings.isImportEmptyToNullOn();
   }

   public boolean isImportEmptyToNullOnProperty()
   {
      String prop = getProperty("impemptytonull");

      if (prop == null || prop.isEmpty())
      {
         setImportEmptyToNull(true);
         return true;
      }

      return Boolean.parseBoolean(prop);
   }

   public void setImportEmptyToNull(boolean on)
   {
      importSettings.setImportEmptyToNull(on);
   }

   public void setImportEmptyToNullProperty(boolean on)
   {
      setProperty("impemptytonull", ""+on);
   }

   public boolean isSolutionEnvStripped()
   {
      return importSettings.isStripSolutionEnvOn();
   }

   public boolean isSolutionEnvStrippedProperty()
   {
      String prop = getProperty("probsolnstripenv");

      if (prop == null || prop.isEmpty())
      {
         setSolutionEnvStripped(true);
         return true;
      }

      return Boolean.parseBoolean(prop);
   }

   public void setSolutionEnvStripped(boolean stripEnv)
   {
      importSettings.setStripSolutionEnv(stripEnv);
   }

   public void setSolutionEnvStrippedProperty(boolean stripEnv)
   {
      setProperty("probsolnstripenv", ""+stripEnv);
   }

   public boolean isPreambleOnly()
   {
      return importSettings.isPreambleOnly();
   }

   public boolean isPreambleOnlyProperty()
   {
      String prop = getProperty("preambleonly");

      if (prop == null || prop.isEmpty())
      {
         setPreambleOnly(true);
         return true;
      }

      return Boolean.parseBoolean(prop);
   }

   public void setPreambleOnly(boolean on)
   {
      importSettings.setPreambleOnly(on);
   }

   public void setPreambleOnlyProperty(boolean on)
   {
      setProperty("preambleonly", ""+on);
   }

   public void setOwnerOnly(boolean enable)
   {
      setProperty("owneronly", ""+enable);
   }

   public boolean isOwnerOnly()
   {
      String prop = getProperty("owneronly");

      if (prop == null || prop.isEmpty())
      {
         setOwnerOnly(false);
         return false;
      }

      return Boolean.parseBoolean(prop);
   }

   public void setAutoTrimLabels(boolean enable)
   {
      importSettings.setTrimLabels(enable);
   }

   public void setAutoTrimLabelsProperty(boolean enable)
   {
      setProperty("trimlabels", ""+enable);
   }

   public boolean isAutoTrimLabelsOn()
   {
      return importSettings.isTrimLabelsOn();
   }

   public boolean isAutoTrimLabelsOnProperty()
   {
      String prop = getProperty("trimlabels");

      if (prop == null || prop.isEmpty())
      {
         setAutoTrimLabels(true);
         return true;
      }

      return Boolean.parseBoolean(prop);
   }

   public void setTrimElement(boolean enable)
   {
      importSettings.setTrimElement(enable);
   }

   public void setTrimElementProperty(boolean enable)
   {
      setProperty("trimelement", ""+enable);
   }

   public boolean isTrimElementOn()
   {
      return importSettings.isTrimElementOn();
   }

   public boolean isTrimElementOnProperty()
   {
      String prop = getProperty("trimelement");

      if (prop == null || prop.isEmpty())
      {
         setTrimElement(true);
         return true;
      }

      return Boolean.parseBoolean(prop);
   }

   public boolean isLiteralContent()
   {
      return importSettings.isLiteralContent();
   }

   public boolean isLiteralContentProperty()
   {
      String prop = getProperty("literalcontent");

      if (prop == null || prop.isEmpty())
      {
         return isTeXMappingOn();
      }

      return Boolean.parseBoolean(prop);
   }

   public void setLiteralContent(boolean on)
   {
      importSettings.setLiteralContent(on);
   }

   public void setLiteralContentProperty(boolean on)
   {
      setProperty("literalcontent", ""+on);
   }

   public boolean isTeXMappingOn()
   {
      return importSettings.isMapCharsOn();
   }

   public boolean isTeXMappingOnProperty()
   {
      String prop = getProperty("subtexspecials");

      if (prop == null || prop.isEmpty())
      {
         return true;
      }

      return Boolean.parseBoolean(prop);
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
               remove("celleditorheight");
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
               remove("celleditorwidth");
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

   public void setRandomSeed(Long seed)
   {
      setProperty("seed", seed == null ? "" : seed.toString());
   }

   public Long getRandomSeed()
   {
      String prop = getProperty("seed");

      if (prop == null || prop.isEmpty()) return null;

      try
      {
         return Long.valueOf(prop);
      }
      catch (NumberFormatException e)
      {
         return null;
      }
   }

   public Random getRandom()
   {
      Long seed = getRandomSeed();

      return seed == null ? new Random() : new Random(seed.longValue());
   }

   public int getShuffleIterations()
   {
      try
      {
         return Integer.parseInt(getProperty("shuffle.iter"));
      }
      catch (Exception e)
      {
         setShuffleIterations(100);
         return 100;
      }
   }

   public void setShuffleIterations(int number)
   {
      setProperty("shuffle.iter", ""+number);
   }

   public boolean isRedefNewProblemEnabled()
   {
      return importSettings.isRedefNewProblemOn();
   }

   public boolean isRedefNewProblemEnabledProperty()
   {
      String prop = getProperty("redefnewprob");

      if (prop == null)
      {
         setRedefNewProblem(true);
         return true;
      }

      return Boolean.parseBoolean(prop);
   }

   public void setRedefNewProblem(boolean value)
   {
      importSettings.setRedefNewProblem(value);
   }

   public void setRedefNewProblemProperty(boolean value)
   {
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

   public void setPerl(String perlExe)
   {
      setProperty("perl", perlExe);
   }

   public String getPerl()
   {
      return getProperty("perl");
   }

   public void setDefaults()
   {
      importSettings.setDefaults();
      setStartUp(STARTUP_HOME);
      setTeXMapping(false);
      setPerl("perl");

      setFontName("Monospaced");
      setFontSize(12);
      setCellHeight(4);

   }

   private void setDefaultTeXMaps()
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

   private void setDefaultCurrency()
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

   public DatatoolTk getDatatoolTk()
   {
      return messageHandler.getDatatoolTk();
   }

   public DatatoolGuiResources getDatatoolGuiResources()
   {
      return messageHandler.getDatatoolGuiResources();
   }

   public boolean isBatchMode()
   {
      return messageHandler == null ? true : messageHandler.isBatchMode();
   }

   public void setBatchMode(boolean enabled)
   {
      messageHandler.setBatchMode(enabled);
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

   private MessageHandler messageHandler;

   private TeXJavaHelpLib helpLib;

   private ImportSettings importSettings;

   protected Vector<String> currencies;

   private Vector<String> recentFiles;

   private HashMap<Integer,String> texMap;

   private File propertiesPath = null;

   private static final String PROPERTIES_NAME = "datatooltk.prop";

   private static final String TEX_MAP_NAME = "texmap.prop";

   private static final String RECENT_NAME = "recentfiles";

   private final String CURRENCY_FILE_NAME = "currencies";

   private final String LANGTAG_FILE_NAME = "languages";

   private boolean upgrade=false;

   private int compatLevel = COMPAT_LATEST;

   private LoadSettings loadSettings;

   private Collator sortCollator;

   private DbTeX3DatumValue dbtex3DatumValue;

   private HelpSetLocale helpSetLocale, dictLocale;

   private DataToolTeXParserListener parserListener;

   public static final int COMPAT_LATEST=0;
   public static final int COMPAT_1_6=1;

   public static final int STARTUP_HOME   = 0;
   public static final int STARTUP_CWD    = 1;
   public static final int STARTUP_LAST   = 2;
   public static final int STARTUP_CUSTOM = 3;

   public static final Pattern PATTERN_CURRENCY
      = Pattern.compile("(.+?) *(\\d*\\.?\\d+)");

   public static final String RESOURCES_PATH = "/resources";
   public static final String ICON_DIR = "/com/dickimawbooks/datatooltk/gui/icons/";
   public static final String HELPSETS = "helpsets";
   public static final String HELPSET_DIR = RESOURCES_PATH+"/"+HELPSETS;
   public static final String DICT_DIR = RESOURCES_PATH+"/dictionaries/";

   public static final String TEMPLATE_DIR = RESOURCES_PATH+"/templates/";

   public static final String PLUGIN_DIR = RESOURCES_PATH+"/plugins/";

   public static final String RESOURCE_PREFIX = "datatooltk-";

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

   private static String[] TYPE_LABELS = null;
   private static int[] TYPE_MNEMONICS = null;

   private static final String DEFAULT_LARGE_ICON_SUFFIX = "-24";
   private static final String DEFAULT_SMALL_ICON_SUFFIX = "-16";
}
