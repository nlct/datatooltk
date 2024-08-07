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
package com.dickimawbooks.datatooltk.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.charset.Charset;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.KeyValList;
import com.dickimawbooks.texparserlib.latex.datatool.*;

import com.dickimawbooks.datatooltk.*;

/**
 * Class handling importing and exporting via the TeX Parser Library's
 * implementation of <code>\DTLread</code> and
 * <code>\DTLwrite</code>.
 */
public class DatatoolTeX implements DatatoolImport,DatatoolExport
{
   public DatatoolTeX(DatatoolSettings settings)
   {
      this(null, settings);
   }

   public DatatoolTeX(String optionList, DatatoolSettings settings)
   {
      this.optionList = optionList;
      this.settings = settings;
   }

   public void exportData(DatatoolDb db, String target)
      throws DatatoolExportException
   {
      try
      {
         DataToolTeXParserListener listener = settings.getTeXParserListener();
         TeXParser parser = listener.getParser();

         DataToolSty sty = listener.getDataToolSty();

         listener.applyCurrentSettings();

         IOSettings ioSettings = listener.getIOSettings();
         ioSettings.setFileOverwriteOption(FileOverwriteOption.ERROR);

         if (optionList != null)
         {
            TeXObjectList list = listener.createStack();
            parser.scan(optionList, list);
            ioSettings.apply(KeyValList.getList(parser, list), parser, parser);
         }

         exportData(db, ioSettings, target);
      }
      catch (TeXSyntaxException e)
      {
         throw new DatatoolExportException(
           String.format("%s%n%s",
            getMessageHandler().getLabelWithValues(
              "error.export.failed", target), e.getMessage(getTeXApp())), e);
      }
      catch (IOException e)
      {
         throw new DatatoolExportException(
           getMessageHandler().getLabelWithValues(
             "error.export.failed", target), e);
      }
   }

   public void exportData(DatatoolDb db, IOSettings ioSettings, String target)
      throws DatatoolExportException
   {
      exportData(db, ioSettings, new File(target));
   }

   public void exportData(DatatoolDb db, IOSettings ioSettings, File file)
      throws DatatoolExportException
   {
      try
      {
         DataToolTeXParserListener listener = settings.getTeXParserListener();

         TeXParser parser = listener.getParser();

         String name = ioSettings.getDefaultName();

         if (name != null)
         {
            db.setName(name);
            ioSettings.setDefaultName(null);
         }

         TeXPath texPath = getTeXPath(ioSettings.getFormat(), file);

         DataBase styDb = db.toDataBase();

         parser.startGroup();
         parser.push(listener.getControlSequence("endgroup"));
         styDb.write(parser, parser, texPath, ioSettings);
         listener.getDataToolSty().removeDataBase(db.getName());
      }
      catch (TeXSyntaxException e)
      {
         throw new DatatoolExportException(
           String.format("%s%n%s",
            getMessageHandler().getLabelWithValues(
              "error.export.failed", file), e.getMessage(getTeXApp())), e);
      }
      catch (IOException e)
      {
         throw new DatatoolExportException(
           getMessageHandler().getLabelWithValues(
             "error.export.failed", file), e);
      }
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
      try
      {
         DataToolTeXParserListener listener = settings.getTeXParserListener();

         DataToolSty sty = listener.getDataToolSty();
         TeXParser parser = listener.getParser();

         listener.applyCurrentSettings();

         IOSettings ioSettings = listener.getIOSettings();

         if (optionList != null)
         {
            TeXObjectList list = listener.createStack();
            parser.scan(optionList, list);
            ioSettings.apply(KeyValList.getList(parser, list), parser, parser);
         }

         return importData(ioSettings, file, checkForVerbatim);
      }
      catch (TeXSyntaxException e)
      {
         throw new DatatoolImportException(
           String.format("%s%n%s",
            getMessageHandler().getLabelWithValues(
              "error.import.failed", file), e.getMessage(getTeXApp())), e);
      }
      catch (IOException e)
      {
         throw new DatatoolImportException(
           getMessageHandler().getLabelWithValues(
             "error.import.failed", file), e);
      }
   }

   public DatatoolDb importData(IOSettings ioSettings, String source)
      throws DatatoolImportException
   {
      return importData(ioSettings, new File(source), true);
   }

   public DatatoolDb importData(IOSettings ioSettings, File file,
        boolean checkForVerbatim)
      throws DatatoolImportException
   {
      try
      {
         DataToolTeXParserListener listener = settings.getTeXParserListener();

         TeXParser parser = listener.getParser();

         TeXPath texPath = getTeXPath(ioSettings.getFormat(), file);

         String name = ioSettings.getDefaultName();
         ioSettings.setDefaultName(null);

         DatatoolDb db = DatatoolDb.loadTeXParser(settings,
            texPath, ioSettings, checkForVerbatim);

         if (name != null)
         {
            db.setName(name);
         }

         return db;
      }
      catch (TeXSyntaxException e)
      {
         throw new DatatoolImportException(
           String.format("%s%n%s",
            getMessageHandler().getLabelWithValues(
              "error.import.failed", file), e.getMessage(getTeXApp())), e);
      }
      catch (IOException e)
      {
         throw new DatatoolImportException(
           getMessageHandler().getLabelWithValues(
             "error.import.failed", file), e);
      }
   }

   @Override
   public DatatoolDb importData(ImportSettings importSettings, String source)
      throws DatatoolImportException
   {
      File file = new File(source);

      try
      {
         DataToolTeXParserListener listener = settings.getTeXParserListener();
         TeXParser parser = listener.getParser();
         listener.applyCurrentCsvSettings();
         IOSettings ioSettings = listener.getIOSettings();

         importSettings.applyTo(ioSettings, parser);
         ioSettings.setFileFormat(FileFormatType.DTLTEX);

         return importData(ioSettings, file,
           importSettings.isCheckForVerbatimOn());
      }
      catch (TeXSyntaxException e)
      {
         throw new DatatoolImportException(
          getMessageHandler().getLabelWithValues("error.import.failed", 
           file.toString(), e.getMessage(importSettings.getTeXApp())), e);
      }
      catch (IOException e)
      {
         throw new DatatoolImportException(
          getMessageHandler().getLabelWithValues("error.import.failed", 
           file.toString(), e.getMessage()), e);
      }
   }

   protected TeXPath getTeXPath(FileFormatType format, File file)
    throws IOException
   {
      DataToolTeXParserListener listener = settings.getTeXParserListener();
      TeXParser parser = listener.getParser();

      TeXPath texPath = new TeXPath(parser, file);
      Charset charset = null;

      if (format == FileFormatType.CSV || format == FileFormatType.TSV)
      {
         charset = settings.getCsvEncoding();
      }
      else
      {
         charset = settings.getTeXEncoding();
      }

      if (charset != null)
      {
         texPath.setEncoding(charset);
      }

      return texPath;
   }

   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
   }

   public TeXApp getTeXApp()
   {
      return settings.getTeXApp();
   }

   protected DatatoolSettings settings;
   protected String optionList;
}
