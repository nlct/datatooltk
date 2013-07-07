package com.dickimawbooks.datatooltk.io;

import java.sql.*;

import com.dickimawbooks.datatooltk.*;

public class DatatoolSql implements DatatoolImport
{
   public DatatoolSql(DatatoolSettings settings)
   {
      this.settings = settings;
   }

   public DatatoolDb importData(String selectQuery)
     throws DatatoolImportException
   {
      try
      {
         establishConnection();
      }
      catch (UserCancelledException e)
      {
         throw new DatatoolImportException(e);
      }
      catch (SQLException e)
      {
         throw new DatatoolImportException(
            DatatoolTk.getLabel("error.sql.connection_failed"), e);
      }

      hasVerbatim = false;
      DatatoolDb db;
      String name = null;

      try
      {
         Statement statement = connection.createStatement();

         ResultSet rs = statement.executeQuery(selectQuery);

         ResultSetMetaData data = rs.getMetaData();

         int colCount = data.getColumnCount();

         db = new DatatoolDb(settings, colCount);

         for (int i = 1; i <= colCount; i++)
         {
            // The header shouldn't contain any TeX special
            // characters, but map just in case

            DatatoolHeader header 
               = new DatatoolHeader(db, mapFieldIfRequired(data.getColumnName(i)));

            if (name == null || name.isEmpty())
            {
               name = data.getTableName(i);
            }

            switch (data.getColumnType(i))
            {
               case Types.DECIMAL:
               case Types.DOUBLE:
               case Types.FLOAT:
               case Types.REAL:
                  header.setType(DatatoolDb.TYPE_REAL);
               break;
               case Types.INTEGER:
               case Types.BINARY:
               case Types.VARBINARY:
               case Types.BIT:
               case Types.BIGINT:
               case Types.SMALLINT:
               case Types.TINYINT:
                  header.setType(DatatoolDb.TYPE_INTEGER);
               break;
               default:
                  header.setType(DatatoolDb.TYPE_STRING);
            }

            if (data.isCurrency(i))
            {
               header.setType(DatatoolDb.TYPE_CURRENCY);
            }

            db.addColumn(header);
         }

         if (name != null && !name.isEmpty())
         {
            db.setName(name);
         }

         int rowIdx = 0;

         while (rs.next())
         {
            DatatoolRow row = new DatatoolRow(db, colCount);

            for (int i = 1; i <= colCount; i++)
            {
               row.addCell(i-1, mapFieldIfRequired(rs.getObject(i).toString()).replaceAll("\n\n+", "\\\\DTLpar "));
            }

            db.insertRow(rowIdx, row);

            rowIdx++;
         }
      }
      catch (SQLException e)
      {
         throw new DatatoolImportException(
           DatatoolTk.getLabel("error.sql.query_failed"), e);
      }

      if (hasVerbatim)
      {
         DatatoolTk.warning(DatatoolTk.getLabel("warning.verb_detected"));
      }

      return db;
   }

   public String mapFieldIfRequired(String value)
   {
      if (!settings.isTeXMappingOn())
      {
         if (!hasVerbatim)
         {
            hasVerbatim = DatatoolDb.checkForVerbatim(value);
         }

         return value;
      }

      if (value.isEmpty())
      {
         return value;
      }

      value = value.replaceAll("\\\\DTLpar ", "\n\n");

      int n = value.length();

      StringBuilder builder = new StringBuilder(n);

      for (int j = 0; j < n; j++)
      {
         char c = value.charAt(j);

         String map = settings.getTeXMap(c);

         if (map == null)
         {
            builder.append(c);
         }
         else
         {
            builder.append(map);
         }
      }

      return builder.toString();
   }

   public synchronized void establishConnection()
     throws SQLException,UserCancelledException
   {
      if (connection != null)
      {
         return;
      }

      connection = DriverManager.getConnection(
       settings.getSqlPrefix()+settings.getSqlHost()+":"
       + settings.getSqlPort()+"/"+settings.getSqlDbName(),
       settings.getSqlUser(), new String(settings.getSqlPassword()));

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

   private boolean hasVerbatim = false;
}
