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
package com.dickimawbooks.datatooltk.io;

import java.sql.*;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.datatooltk.*;

/**
 * Class for importing data from an SQL table.
 */
public class DatatoolSql implements DatatoolImport
{
   public DatatoolSql(DatatoolSettings settings)
   {
      this.settings = settings;
   }

   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
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
            getMessageHandler().getLabel("error.sql.connection_failed"), e);
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
               = new DatatoolHeader(db, mapFieldIfRequired(data.getColumnLabel(i)));

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
                  header.setType(DatumType.DECIMAL);
               break;
               case Types.INTEGER:
               case Types.BINARY:
               case Types.VARBINARY:
               case Types.BIT:
               case Types.BIGINT:
               case Types.SMALLINT:
               case Types.TINYINT:
                  header.setType(DatumType.INTEGER);
               break;
               default:
                  header.setType(DatumType.STRING);
            }

            if (data.isCurrency(i))
            {
               header.setType(DatumType.CURRENCY);
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
               Object obj = rs.getObject(i);
               Datum value;

               if (obj == null)
               {
                  value = Datum.createNull(settings);
               }
               else
               {
                  String strValue = mapFieldIfRequired(obj.toString());
                  strValue = strValue.replaceAll("\n\n+", "\\\\DTLpar ");
                  value = Datum.valueOf(strValue, settings);
               }

               row.addCell(i-1, value);
            }

            db.insertRow(rowIdx, row);

            rowIdx++;
         }
      }
      catch (SQLException e)
      {
         throw new DatatoolImportException(
           getMessageHandler().getLabel("error.sql.query_failed"), e);
      }
      catch (Exception e)
      {
         throw new DatatoolImportException(
           e.getMessage(), e);
      }

      if (hasVerbatim)
      {
         getMessageHandler().warning(
           getMessageHandler().getLabel("warning.verb_detected"));
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

      for (int j = 0; j < n; )
      {
         int c = value.codePointAt(j);
         j += Character.charCount(c);

         String map = settings.getTeXMap(c);

         if (map == null)
         {
            builder.appendCodePoint(c);
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
