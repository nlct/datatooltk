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
         File file = new File(target);
         DataToolTeXParserListener listener = settings.getTeXParserListener();
// TODO
/*
         TeXParser parser = listener.getParser();
         IOSettings ioSettings = listener.getIOSettings();

         TeXPath texPath = new TeXPath(parser, file);

         DataBase styDb = db.toDataBase();

         parser.startGroup();
         parser.push(listener.getControlSequence("endgroup"));
         styDb.write(parser, parser, texPath, ioSettings);
         listener.getDataToolSty().removeDataBase(db.getName());
*/
      }
      catch (IOException e)
      {
         throw new DatatoolExportException(
           getMessageHandler().getLabelWithValues(
             "error.export.failed", target), e);
      }
   }

   public DatatoolDb importData(String source)
      throws DatatoolImportException
   {
      return importData(source, true);
   }

   public DatatoolDb importData(String source, boolean checkForVerbatim)
      throws DatatoolImportException
   {
      try
      {
         DataToolTeXParserListener listener = settings.getTeXParserListener();

         DataToolSty sty = listener.getDataToolSty();
         TeXParser parser = listener.getParser();

         listener.applyCurrentCsvSettings();

         IOSettings ioSettings = listener.getIOSettings();

         if (optionList != null)
         {
            TeXObjectList list = listener.createStack();
            parser.scan(optionList, list);
            ioSettings.apply(KeyValList.getList(parser, list), parser, parser);
         }

         return importData(ioSettings, source, checkForVerbatim);
      }
      catch (IOException e)
      {
         throw new DatatoolImportException(
           getMessageHandler().getLabelWithValues(
             "error.import.failed", source), e);
      }
   }

   public DatatoolDb importData(IOSettings ioSettings, String source,
        boolean checkForVerbatim)
      throws DatatoolImportException
   {
      try
      {
         DataToolTeXParserListener listener = settings.getTeXParserListener();

         TeXParser parser = listener.getParser();

         FileFormatType format = ioSettings.getFormat();

         TeXPath texPath;
         String charsetName = null;

         if (format == FileFormatType.CSV || format == FileFormatType.TSV)
         {
            texPath = new TeXPath(parser, source, "csv", "tsv");

            charsetName = settings.getCsvEncoding();
         }
         else
         {
            texPath = new TeXPath(parser, source,
               "dbtex", "dtltex", "tex", "ltx");

            charsetName = settings.getTeXEncoding();
         }

         if (charsetName != null)
         {
            Charset charset = Charset.forName(charsetName);
            texPath.setEncoding(charset);
         }

         return DatatoolDb.loadTeXParser(settings,
            texPath, ioSettings, checkForVerbatim);
      }
      catch (IOException e)
      {
         throw new DatatoolImportException(
           getMessageHandler().getLabelWithValues(
             "error.import.failed", source), e);
      }
   }

   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
   }

   private DatatoolSettings settings;
   private String optionList;
}
