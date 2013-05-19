package com.dickimawbooks.datatooltk;

import java.sql.*;

public class DatatoolSql implements DatatoolImport
{
   public DatatoolSql(DatatoolSettings settings)
   {
      this.settings = settings;
   }

   public DatatoolDb importData(String source)
     throws DatatoolImportException
   {
      establishConnection();

      DatatoolDb = new DatatoolDb();

      return db;
   }

   public synchronized void establishConnection()
     throws SQLException
   {
      if (connection != null)
      {
         return;
      }

      connection = DriverManager.getConnection(
       settings.getSqlPrefix()+settings.getSqlHost()+":"
       + settings.getSqlPort()+"/"+settings.getSqlDbName(),
       settings.getSqlUser(), settings.getSqlPassword());

      settings.wipePasswordIfRequired();
   }

   public synchronized void close()
      throws SQLException
   {
      if (connection != null)
      {
         connection.close();
      }
   }

   private DatatoolSettings settings;

   private Connection connection = null;
}
