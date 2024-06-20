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

import java.io.*;
import java.util.regex.*;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;

import com.dickimawbooks.datatooltk.io.*;
import com.dickimawbooks.datatooltk.gui.*;

/**
 * Class representing header information.
 */
public class DatatoolHeader
{
   public DatatoolHeader(DatatoolDb db)
   {
      this(db, null);
   }

   public DatatoolHeader(DatatoolDb db, String key)
   {
      this(db, key, key);
   }

   public DatatoolHeader(DatatoolDb db, String key, String title)
   {
      this(db, key, title, DatumType.UNKNOWN);
   }

   public DatatoolHeader(DatatoolDb db, String key, String title, DatumType type)
   {
      super();
      this.db = db;
      messageHandler = db.getMessageHandler();

      if (key == title)
      {
         if (key == null)
         {
            setKey(null);
            setTitle(null);
         }
         else
         {
            setTitle(title.trim());

            // strip any invalid markup from the key

            setKey(
               DatatoolDb.INVALID_LABEL_CONTENT.matcher(key).replaceAll(""));
         }
      }
      else
      {
         setKey(key);
         setTitle(title);
      }

      setType(type);
   }

   public DatumType getDatumType()
   {
      return type;
   }

   @Deprecated
   public int getType()
   {
      return type.getValue();
   }

   public String getKey()
   {
      return key;
   }

   public String getTitle()
   {
      return title;
   }

   public void setType(DatumType type)
   {
      this.type = type;
   }

   @Deprecated
   public void setType(int typeId)
   {
      if (typeId < DatatoolSettings.TYPE_UNKNOWN
       || typeId > DatatoolSettings.TYPE_CURRENCY)
      {
         throw new IllegalArgumentException(
            messageHandler.getLabelWithValues(
              "error.invalid_data_type", typeId));
      }

      this.type = DatumType.toDatumType(typeId);
   }

   public void setKey(String key)
   {
      if (key != null && db.getSettings().isAutoTrimLabelsOn())
      {
         this.key = key.trim();
      }
      else
      {
         this.key = key;
      }
   }

   public void setTitle(String title)
   {
      this.title = title;
   }

   public String toString()
   {
      return title;
   }

   public Object clone()
   {
      return new DatatoolHeader(db, key, title, type);
   }

   public String getHeaderValue()
   {
      return title;
   }

   public String getIdentifier()
   {
      return key;
   }

   public DatatoolDb getDb()
   {
      return db;
   }

   public void setDb(DatatoolDb db)
   {
      this.db = db;
   }

   private DatatoolDb db;
   private MessageHandler messageHandler;

   private String key;
   private String title;
   private DatumType type = DatumType.UNKNOWN;
}
