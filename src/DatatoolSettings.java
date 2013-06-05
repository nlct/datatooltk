package com.dickimawbooks.datatooltk;

public class DatatoolSettings
{
   public void setSeparator(char separator)
   {
      sep = separator;
   }

   public char getSeparator()
   {
      return sep;
   }

   public void setDelimiter(char delimiter)
   {
      delim = delimiter;
   }

   public char getDelimiter()
   {
      return delim;
   }

   public String getSqlUrl()
   {
      return sqlPrefix + sqlHost + ":" + sqlPort + "/";
   }

   public String getSqlUrl(String sqlDb)
   {
      return getSqlUrl()+sqlDb;
   }

   public String getSqlHost()
   {
      return sqlHost;
   }

   public String getSqlPrefix()
   {
      return sqlPrefix;
   }

   public int getSqlPort()
   {
      return sqlPort;
   }

   public void setSqlHost(String host)
   {
      sqlHost = host;
   }

   public void setSqlPrefix(String prefix)
   {
      sqlPrefix = prefix;
   }

   public void setSqlPort(int port)
   {
      sqlPort = port;
   }

   public boolean hasCSVHeader()
   {
      return csvHasHeader;
   }

   public void setHasCSVHeader(boolean hasHeader)
   {
      csvHasHeader = hasHeader;
   }

   public String getSqlDbName()
   {
      return sqlDbName;
   }

   public void setSqlDbName(String name)
   {
      sqlDbName = name;
   }

   public void wipePasswordIfRequired()
   {
      if (sqlPassword != null && wipePassword)
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
      this.wipePassword = wipePassword;
   }

   public boolean isWipePasswordEnabled()
   {
      return wipePassword;
   }

   public void setSqlUser(String username)
   {
      sqlUser = username;
   }

   public String getSqlUser()
   {
      return sqlUser;
   }

   public void setPasswordReader(DatatoolPasswordReader reader)
   {
      passwordReader = reader;
   }

   protected char sep = ',';
   protected char delim = '\"';
   protected boolean csvHasHeader = true;

   protected String sqlHost = "localhost";
   protected int sqlPort = 3306;
   protected String sqlPrefix = "jdbc:mysql://";
   protected String sqlDbName = null;
   protected String sqlUser = null;
   protected char[] sqlPassword = null;
   protected boolean wipePassword = false;

   protected DatatoolPasswordReader passwordReader;
}
