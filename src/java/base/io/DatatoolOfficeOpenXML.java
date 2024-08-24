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
package com.dickimawbooks.datatooltk.base.io;

import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.*;
import java.nio.file.Files;
import java.nio.charset.*;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import com.dickimawbooks.datatooltk.base.*;

/**
 * Class handling importing Office Open XML Spreadsheet (.xlsx).
 */
public class DatatoolOfficeOpenXML implements DatatoolImport
{
   public DatatoolOfficeOpenXML(DatatoolSettings settings)
   {
      this.settings = settings;
   }

   public DatatoolDb importData(String source)
      throws DatatoolImportException
   {
      return importData(source, true);
   }

   public DatatoolDb importData(File file)
      throws DatatoolImportException
   {
      return importData(file, true);
   }

   public DatatoolDb importData(String source, boolean checkForVerbatim)
      throws DatatoolImportException
   {
      return importData(new File(source), checkForVerbatim);
   }

   public DatatoolDb importData(File file, boolean checkForVerbatim)
      throws DatatoolImportException
   {
      ImportSettings importSettings = settings.getImportSettings();
      importSettings.setCheckForVerbatim(checkForVerbatim);

      return importData(importSettings, file);
   }

   @Override
   public DatatoolDb importData(ImportSettings importSettings, String source)
      throws DatatoolImportException
   {
      return importData(importSettings, new File(source));
   }

   public DatatoolDb importData(ImportSettings importSettings, File file)
      throws DatatoolImportException
   {
      try
      {
         return importZipData(importSettings, file);
      }
      catch (DatatoolImportException e)
      {
         throw e;
      }
      catch (Throwable e)
      {
         throw new DatatoolImportException(
           getMessageHandler().getLabelWithValues(
             "error.import.failed", file,
             getMessageHandler().getMessage(e)), e);
      }
   }

   protected DatatoolDb importZipData(ImportSettings importSettings, File file)
      throws DatatoolImportException,SAXException,IOException,UserCancelledException
   {
      ZipFile zipFile = null;
      InputStream in = null;
      DatatoolDb db;
      MessageHandler messageHandler = getMessageHandler();

      try
      {
         zipFile = new ZipFile(file, StandardCharsets.UTF_8);

         String entryName = "xl/workbook.xml";
         ZipEntry zipEntry = zipFile.getEntry(entryName);

         if (zipEntry == null)
         {
            throw new DatatoolImportException(
             messageHandler.getLabelWithValues("error.zip_entry_not_found",
               entryName, file));
         }

         in = zipFile.getInputStream(zipEntry);

         Charset charset = DatatoolFileFormat.getXmlEncoding(messageHandler, in);

         if (!in.markSupported()
             || (charset != null && !charset.equals(StandardCharsets.UTF_8)))
         {
            in.close();
            in = null;
            zipFile.close();
            zipFile = null;

            if (charset == null)
            {
               charset = StandardCharsets.UTF_8;
            }

            zipFile = new ZipFile(file, charset);

            zipEntry = zipFile.getEntry(entryName);
            in = zipFile.getInputStream(zipEntry);
         }

         OfficeOpenReader reader = new OfficeOpenReader(importSettings);

         reader.parseWorkBook(new InputSource(in));

         in.close();
         in = null;

         entryName = "xl/_rels/workbook.xml.rels";
         zipEntry = zipFile.getEntry(entryName);

         if (zipEntry == null)
         {
            throw new DatatoolImportException(
             messageHandler.getLabelWithValues("error.zip_entry_not_found",
               entryName, file));
         }

         in = zipFile.getInputStream(zipEntry);
         reader.parseRelationships(new InputSource(in));

         in.close();
         in = null;

         entryName = "xl/sharedStrings.xml";
         zipEntry = zipFile.getEntry(entryName);

         if (zipEntry == null)
         {
            throw new DatatoolImportException(
             messageHandler.getLabelWithValues("error.zip_entry_not_found",
               entryName, file));
         }

         in = zipFile.getInputStream(zipEntry);
         reader.parseSharedStrings(new InputSource(in));

         in.close();
         in = null;

         entryName = reader.getSelectedWorkBookPath();

         if (entryName == null)
         {
            throw new DatatoolImportException(
               messageHandler.getLabelWithValues(
                "error.dbload.missing_data", file));
         }

         zipEntry = zipFile.getEntry(entryName);

         if (zipEntry == null)
         {
            throw new DatatoolImportException(
             messageHandler.getLabelWithValues("error.zip_entry_not_found",
               entryName, file));
         }

         in = zipFile.getInputStream(zipEntry);
         reader.parseData(new InputSource(in));

         db = reader.getDataBase();

         if (db == null)
         {
            throw new DatatoolImportException(
               messageHandler.getLabelWithValues(
                "error.dbload.missing_data", file));
         }

         if (reader.verbatimFound())
         {
            messageHandler.warning(
              messageHandler.getLabel("warning.verb_detected"));
         }
      }
      finally
      {
         if (in != null)
         {
            in.close();
         }

         if (zipFile != null)
         {
            zipFile.close();
         }
      }

      return db;
   }

   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
   }

   public DatatoolSettings getSettings()
   {
      return settings;
   }

   protected DatatoolSettings settings;
}
