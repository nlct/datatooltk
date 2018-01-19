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

import java.util.Properties;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Vector;
import java.util.Random;
import java.util.InvalidPropertiesFormatException;
import java.awt.Font;
import java.awt.Color;
import java.awt.Dimension;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Locale;
import java.net.URL;

import com.dickimawbooks.datatooltk.io.DatatoolPasswordReader;
import com.dickimawbooks.datatooltk.gui.DatatoolPlugin;

/**
 * Application settings for datatooltk.
 */
public class DatatoolSettings extends Properties
{
   public DatatoolSettings(DatatoolTk datatooltk)
   {
      super();
      messageHandler = new MessageHandler(datatooltk);

      recentFiles = new Vector<String>();
      currencies = new Vector<String>();

      setDefaults();

      setPropertiesPath();
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

   public int[] getTypeMnemonics()
   {
      if (TYPE_MNEMONICS == null)
      {
         initLabels();
      }

      return TYPE_MNEMONICS;
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

   public void loadProperties()
      throws IOException,InvalidPropertiesFormatException
   {
      if (propertiesPath == null) return;

      File file = new File(propertiesPath, propertiesName);

      BufferedReader reader = null;
      InputStream in = null;

      try
      {
         if (file.exists())
         {
            in = new FileInputStream(file);

            loadFromXML(in);

            in.close();
            in = null;

            if (getProperty("version") == null)
            {
               upgrade=true;

               if (getProperty("tex.&") == null)
               {
                  setTeXMap((int)'&', "\\&");// missing from pre v1.7
               }
            }
         }
         else
         {
            setDefaultTeXMaps();
         }

         setProperty("version", DatatoolTk.APP_VERSION);

         file = new File(propertiesPath, recentName);

         if (file.exists())
         {
            reader = new BufferedReader(new FileReader(file));

            recentFiles.clear();

            String line;

            while ((line=reader.readLine()) != null)
            {
               recentFiles.add(line);
            }

            reader.close();
            reader = null;
         }

         file = new File(propertiesPath, currencyFileName);

         if (file.exists())
         {
            reader = new BufferedReader(new FileReader(file));

            currencies.clear();

            String line;

            while ((line=reader.readLine()) != null)
            {
               currencies.add(line);
            }

            reader.close();
            reader = null;
         }
         else
         {
            setDefaultCurrency();
         }
      }
      finally
      {
         if (in != null)
         {
            in.close();
            in = null;
         }

         if (reader != null)
         {
            reader.close();
            reader = null;
         }
      }
   }

   public void saveProperties()
      throws IOException
   {
      if (propertiesPath == null) return;

      File file = new File(propertiesPath, propertiesName);

      FileOutputStream out = null;

      PrintWriter writer = null;

      try
      {
         out = new FileOutputStream(file);

         storeToXML(out, null);

         out.close();
         out = null;

         file = new File(propertiesPath, recentName);

         writer = new PrintWriter(new FileWriter(file));

         for (String name : recentFiles)
         {
            writer.println(name);
         }

         writer.close();
         writer = null;

         file = new File(propertiesPath, currencyFileName);

         writer = new PrintWriter(new FileWriter(file));

         for (String currency : currencies)
         {
            writer.println(currency);
         }

         writer.close();
         writer = null;
      }
      finally
      {
         if (out != null)
         {
            out.close();
            out = null;
         }

         if (writer != null)
         {
            writer.close();
            writer = null;
         }
      }
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

   public String getTeXEncoding()
   {
      return getProperty("tex-encoding");
   }

   public void setTeXEncoding(Charset encoding)
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

   public void setTeXEncoding(String encoding)
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

   public String getCsvEncoding()
   {
      return getProperty("csv-encoding");
   }

   public void setCsvEncoding(Charset encoding)
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

   public void setCsvEncoding(String encoding)
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

   public String getSortLocale()
   {
      return getProperty("sort-locale");
   }

   public void setSortLocale(Locale locale)
   {
      if (locale == null)
      {
         remove("sort-locale");
      }
      else
      {
         setProperty("sort-locale", locale.toLanguageTag());
      }
   }

   public void setSortLocale(String locale)
   {
      if (locale == null || "".equals(locale))
      {
         remove("sort-locale");
      }
      else
      {
         setProperty("sort-locale", locale);
      }
   }

   public void setSeparator(int separator)
   {
      setProperty("sep", MessageHandler.codePointToString(separator));
   }

   public int getSeparator()
   {
      return getProperty("sep").codePointAt(0);
   }

   public void setDelimiter(int delimiter)
   {
      setProperty("delim", MessageHandler.codePointToString(delimiter));
   }

   public int getDelimiter()
   {
      return getProperty("delim").codePointAt(0);
   }

   public String getSheetRef()
   {
      return sheetref;
   }

   public void setSheetRef(String ref)
   {
      if (ref == null || ref.isEmpty())
      {
         sheetref = "0";
      }
      else
      {
         sheetref = ref;
      }
   }

   public String getSqlUrl()
   {
      return getSqlPrefix() + getSqlHost() + ":" + getSqlPort() + "/";
   }

   public String getSqlUrl(String sqlDb)
   {
      return getSqlUrl()+sqlDb;
   }

   public String getSqlHost()
   {
      return getProperty("sqlHost");
   }

   public String getSqlPrefix()
   {
      return getProperty("sqlPrefix");
   }

   public int getSqlPort()
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
      setProperty("sqlHost", host);
   }

   public void setSqlPrefix(String prefix)
   {
      setProperty("sqlPrefix", prefix);
   }

   public void setSqlPort(int port)
   {
      setProperty("sqlPort", ""+port);
   }

   public boolean hasCSVHeader()
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
      setProperty("csvHasHeader", ""+hasHeader);
   }

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

   public String getSqlDbName()
   {
      return getProperty("sqlDbName");
   }

   public void setSqlDbName(String name)
   {
      setProperty("sqlDbName", name);
   }

   public void wipePasswordIfRequired()
   {
      if (sqlPassword != null && isWipePasswordEnabled())
      {
         java.util.Arrays.fill(sqlPassword, ' ');
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

   public void setWipePassword(boolean wipePassword)
   {
      setProperty("wipePassword", ""+wipePassword);
   }

   public boolean isWipePasswordEnabled()
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
      setProperty("sqlUser", username);
   }

   public String getSqlUser()
   {
      return getProperty("sqlUser");
   }

   public void setPasswordReader(DatatoolPasswordReader reader)
   {
      passwordReader = reader;
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
      setProperty("subtexspecials", ""+enable);
   }

   public boolean isSolutionEnvStripped()
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
      setProperty("probsolnstripenv", ""+stripEnv);
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

   public boolean isTeXMappingOn()
   {
      String prop = getProperty("subtexspecials");

      if (prop == null || prop.isEmpty())
      {
         setTeXMapping(false);
         return false;
      }

      return Boolean.parseBoolean(prop);
   }

   public String getTeXMap(int codePoint)
   {
      if (upgrade)
      {
         // backward compatibility check first

         String key = String.format("tex.%c", codePoint);

         String prop = getProperty(key);

         if (prop != null)
         {
            remove(key);
            setProperty(getTeXMapKey(codePoint), prop);
            return prop;
         }
      }

      return getProperty(getTeXMapKey(codePoint));
   }

   public String removeTeXMap(int codePoint)
   {
      return (String)remove(getTeXMapKey(codePoint));
   }

   public void setTeXMap(int codePoint, String value)
   {
      setProperty(getTeXMapKey(codePoint), value);
   }

   private String getTeXMapKey(int codePoint)
   {
      return String.format("tex.%04d", codePoint);
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

   public int getCellEditorHeight()
   {
      try
      {
         return Integer.parseInt(getProperty("celleditorheight"));
      }
      catch (NumberFormatException e)
      {
         setCellHeight(10);
         return 10;
      }
   }

   public void setCellEditorHeight(int numLines)
   {
      setProperty("celleditorheight", ""+numLines);
   }

   public int getCellEditorWidth()
   {
      try
      {
         return Integer.parseInt(getProperty("celleditorwidth"));
      }
      catch (NumberFormatException e)
      {
         setCellHeight(8);
         return 8;
      }
   }

   public void setCellEditorWidth(int maxCharsPerLine)
   {
      setProperty("celleditorwidth", ""+maxCharsPerLine);
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
         return new Long(prop);
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
      setProperty("redefnewprob", ""+value);
   }

   public String getDictionary()
   {
      String prop = getProperty("dictionary");

      if (prop == null)
      {
         Locale locale = Locale.getDefault();

         String language = locale.getLanguage();
         String country = locale.getCountry();

         URL url = getClass().getResource(DICT_DIR + RESOURCE
          + "-" + language + "-" + country  + ".prop");

         if (url == null)
         {
            url = getClass().getResource(DICT_DIR + RESOURCE
              + "-" + language + ".prop");

            if (url == null)
            {
               prop = "en-US";
            }
            else
            {
               prop = language;
            }
         }
         else
         {
            prop = language+"-"+country;
         }

         setDictionary(prop);
      }

      return prop;
   }

   public void setDictionary(String dictionary)
   {
      setProperty("dictionary", dictionary);
   }

   public String getHelpSet()
   {
      String prop = getProperty("helpset");

      if (prop == null)
      {
         Locale locale = Locale.getDefault();

         String language = locale.getLanguage();
         String country = locale.getCountry();

         String helpsetLocation = HELPSET_DIR+RESOURCE;

         URL hsURL = getClass().getResource(String.format("%s-%s-%s/%s.hs", 
          helpsetLocation, language, country, RESOURCE));

         if (hsURL == null)
         {
            hsURL = getClass().getResource(String.format("%s-%s/%s.hs",
              helpsetLocation, language, RESOURCE));

            if (hsURL == null)
            {
               messageHandler.debug(String.format(
                "Can't find language file for %s-%s", language, country));

               prop = "en-US";
            }
            else
            {
               prop = language;
            }
         }
         else
         {
            prop = language+"-"+country;
         }

         setHelpSet(prop);
      }
 
      return prop;
   }

   public void setHelpSet(String helpset)
   {
      setProperty("helpset", helpset);
   }

   public static String getHelpSetLocation()
   {
      return HELPSET_DIR + RESOURCE;
   }

   public static String getDictionaryLocation()
   {
      return DICT_DIR + RESOURCE;
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
      setSeparator(',');
      setDelimiter('"');
      setHasCSVHeader(true);
      setCSVescape("\\");
      setSqlHost("localhost");
      setSqlPort(3306);
      setSqlPrefix("jdbc:mysql://");
      setWipePassword(true);
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

   private MessageHandler messageHandler;

   protected char[] sqlPassword = null;

   protected DatatoolPasswordReader passwordReader;

   private String sheetref = "0";

   protected Vector<String> currencies;

   private Vector<String> recentFiles;

   private File propertiesPath = null;

   private final String propertiesName="datatooltk.prop";

   private final String recentName = "recentfiles";

   private final String currencyFileName = "currencies";

   private boolean upgrade=false;

   private int compatLevel = COMPAT_LATEST;

   public static final int COMPAT_LATEST=0;
   public static final int COMPAT_1_6=1;

   public static final int STARTUP_HOME   = 0;
   public static final int STARTUP_CWD    = 1;
   public static final int STARTUP_LAST   = 2;
   public static final int STARTUP_CUSTOM = 3;

   public static final Pattern PATTERN_CURRENCY
      = Pattern.compile("(.+?) *(\\d*\\.?\\d+)");

   public static final String HELPSET_DIR = "/resources/helpsets/";
   public static final String DICT_DIR = "/resources/dictionaries/";

   public static final String TEMPLATE_DIR = "/resources/templates/";

   public static final String PLUGIN_DIR = "/resources/plugins/";

   public static final String RESOURCE = "datatooltk";

   public static final Pattern PATTERN_HELPSET 
     = Pattern.compile("datatooltk-([a-z]{2})(-[A-Z]{2})?");

   public static final Pattern PATTERN_DICT 
     = Pattern.compile("datatooltk-([a-z]{2})(-[A-Z]{2})?\\.prop");

   public static final int TYPE_UNKNOWN=-1, TYPE_STRING = 0, TYPE_INTEGER=1,
     TYPE_REAL=2, TYPE_CURRENCY=3;

   private static String[] TYPE_LABELS = null;
   private static int[] TYPE_MNEMONICS = null;
}
