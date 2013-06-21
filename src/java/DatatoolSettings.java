package com.dickimawbooks.datatooltk;

import java.util.Properties;
import java.io.*;
import java.util.Vector;
import java.util.InvalidPropertiesFormatException;
import java.awt.Font;

import com.dickimawbooks.datatooltk.io.DatatoolPasswordReader;

public class DatatoolSettings extends Properties
{
   public DatatoolSettings()
   {
      super();
      setDefaults();
      recentFiles = new Vector<String>();

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

      try
      {
         out = new FileOutputStream(file);

         storeToXML(out, null);

         out.close();
         out = null;

         file = new File(propertiesPath, recentName);

         PrintWriter writer = new PrintWriter(new FileWriter(file));

         for (String name : recentFiles)
         {
            writer.println(name);
         }

         writer.close();
      }
      finally
      {
         if (out != null)
         {
            out.close();
            out = null;
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
      setWipePassword(false);
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
   }

   protected char[] sqlPassword = null;

   protected DatatoolPasswordReader passwordReader;

   private Vector<String> recentFiles;

   private File propertiesPath = null;

   private final String propertiesName="datatooltk.prop";

   private final String recentName = "recentfiles";

   public static final int STARTUP_HOME   = 0;
   public static final int STARTUP_CWD    = 1;
   public static final int STARTUP_LAST   = 2;
   public static final int STARTUP_CUSTOM = 3;
}
