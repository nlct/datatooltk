package com.dickimawbooks.datatooltk;

import java.util.Properties;
import java.io.*;
import java.util.Vector;
import java.util.InvalidPropertiesFormatException;
import java.awt.Font;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.dickimawbooks.datatooltk.io.DatatoolPasswordReader;

public class DatatoolSettings extends Properties
{
   public DatatoolSettings()
   {
      super();
      recentFiles = new Vector<String>();
      currencies = new Vector<String>();

      setDefaults();

      setPropertiesPath();
   }

   private void setPropertiesPath()
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
         DatatoolTk.debug("No 'user.home' property!");
         return;
      }

      File homeDir = new File(home);

      if (!homeDir.exists())
      {
         DatatoolTk.debug("Home directory '"+home+"' doesn't exist!");
         return;
      }

      propertiesPath = new File(homeDir, base);

      if (propertiesPath.exists())
      {
         if (!propertiesPath.isDirectory())
         {
            DatatoolTk.debug("'"+propertiesPath+"' isn't a directory");
            propertiesPath = null;
            return;
         }
      }
      else
      {
         if (!propertiesPath.mkdir())
         {
            DatatoolTk.debug("Unable to mkdir '"+propertiesPath+"'");
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
         }

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

   public void setSeparator(char separator)
   {
      setProperty("sep", ""+separator);
   }

   public char getSeparator()
   {
      return getProperty("sep").charAt(0);
   }

   public void setDelimiter(char delimiter)
   {
      setProperty("delim", ""+delimiter);
   }

   public char getDelimiter()
   {
      return getProperty("delim").charAt(0);
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
            DatatoolTk.debug("Invalid startup setting '"+prop+"'");
            return STARTUP_HOME;
         }

         return result;
      }
      catch (NumberFormatException e)
      {
         DatatoolTk.debug("Invalid startup setting '"+prop+"'");
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

   public String getTeXMap(char c)
   {
      String prop = getProperty("tex."+c);

      return prop;
   }

   public void setTeXMap(char c, String value)
   {
      setProperty("tex."+c, value);
   }

   public String removeTeXMap(char c)
   {
      return (String)remove("tex."+c);
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

      throw new NumberFormatException(DatatoolTk.getLabelWithValue(
         "error.not_currency", text));
   }

   public void setLaTeX(String app)
   {
      setProperty("app.latex", app);
   }

   public String getLaTeX()
   {
      return getProperty("app.latex");
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
         case DatatoolDb.TYPE_STRING:
            tag = "string";
         break;
         case DatatoolDb.TYPE_UNKNOWN:
            tag = "unset";
         break;
         case DatatoolDb.TYPE_INTEGER:
            tag = "int";
         break;
         case DatatoolDb.TYPE_REAL:
            tag = "real";
         break;
         case DatatoolDb.TYPE_CURRENCY:
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
         case DatatoolDb.TYPE_STRING:
            tag = "string";
            defValue = 300;
         break;
         case DatatoolDb.TYPE_UNKNOWN:
            tag = "unset";
            defValue = 100;
         break;
         case DatatoolDb.TYPE_INTEGER:
            tag = "int";
            defValue = 40;
         break;
         case DatatoolDb.TYPE_REAL:
            tag = "real";
            defValue = 60;
         break;
         case DatatoolDb.TYPE_CURRENCY:
            tag = "currency";
            defValue = 60;
         break;
         default:
            throw new IllegalArgumentException(
              "getCellWidth(int): Invalid data type "+type);
      }

      String prop = getProperty("cellwidth."+tag);

      try
      {
         if (prop == null) throw new NullPointerException();

         return Integer.parseInt(prop);
      }
      catch (NullPointerException e)
      {
      }
      catch (NumberFormatException e)
      {
         DatatoolTk.debug("Property 'cellwidth."+tag
           +"' should be an integer. Found: '"+prop+"'");
      }

      setProperty("cellwidth."+tag, ""+defValue);
      return defValue;
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

   public void setDefaults()
   {
      setSeparator(',');
      setDelimiter('"');
      setHasCSVHeader(true);
      setSqlHost("localhost");
      setSqlPort(3306);
      setSqlPrefix("jdbc:mysql://");
      setWipePassword(true);
      setStartUp(STARTUP_HOME);
      setTeXMapping(false);

      setTeXMap('\\', "\\textbackslash ");
      setTeXMap('$', "\\$");
      setTeXMap('#', "\\#");
      setTeXMap('%', "\\%");
      setTeXMap('_', "\\_");
      setTeXMap('{', "\\{");
      setTeXMap('}', "\\}");
      setTeXMap('~', "\\textasciitilde ");
      setTeXMap('^', "\\textasciicircum ");

      setLaTeX("latex");
      setFontName("Monospaced");
      setFontSize(12);
      setCellHeight(4);

      if (currencies.size() == 0)
      {
         addCurrency("\\$");
         addCurrency("\\pounds");
      }
   }

   protected char[] sqlPassword = null;

   protected DatatoolPasswordReader passwordReader;

   protected Vector<String> currencies;

   private Vector<String> recentFiles;

   private File propertiesPath = null;

   private final String propertiesName="datatooltk.prop";

   private final String recentName = "recentfiles";

   private final String currencyFileName = "currencies";

   public static final int STARTUP_HOME   = 0;
   public static final int STARTUP_CWD    = 1;
   public static final int STARTUP_LAST   = 2;
   public static final int STARTUP_CUSTOM = 3;

   public static final Pattern PATTERN_CURRENCY
      = Pattern.compile("([^\\d\\.]+) *(\\d*\\.?\\d+)");

}
