/*
    Copyright (C) 2013-2024 Nicola L.C. Talbot
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
import com.dickimawbooks.texparserlib.latex.datatool.*;

import com.dickimawbooks.datatooltk.*;

/**
 * Class handling importing and exporting CSV data.
 * This is now using the TeX Parser Library instead of opencsv,
 * partly to be more consistent with datatool.sty's \DTLread and
 * \DTLwrite and partly to reduce the number of dependent libraries.
 */
public class DatatoolCsv implements DatatoolImport,DatatoolExport
{
   public DatatoolCsv(DatatoolSettings settings)
   {
      this.settings = settings;
   }

   public void exportData(DatatoolDb db, String target)
      throws DatatoolExportException
   {
      try
      {
         File file = new File(target);
         DataToolTeXParserListener listener = settings.getTeXParserListener();
         TeXParser parser = listener.getParser();
         IOSettings ioSettings = listener.getIOSettings();

         TeXPath texPath = new TeXPath(parser, file);

         DataBase styDb = db.toDataBase();

         parser.startGroup();
         parser.push(listener.getControlSequence("endgroup"));
         styDb.write(parser, parser, texPath, ioSettings);
         listener.getDataToolSty().removeDataBase(db.getName());
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
      return importData(new File(source));
   }

   public DatatoolDb importData(File file)
      throws DatatoolImportException
   {
      return importData(file, null);
   }

   public DatatoolDb importData(File file, String name)
      throws DatatoolImportException
   {
      if (name == null)
      {
         name = file.getName();
         int idx = name.lastIndexOf(".");

         if (idx != -1)
         {
            name = name.substring(0, idx);
         }
      }

      boolean checkForVerbatim = true;

      try
      {
         DataToolTeXParserListener listener = settings.getTeXParserListener();
         TeXParser parser = listener.getParser();
         IOSettings ioSettings = listener.getIOSettings();

         FileFormatType formatType = ioSettings.getFormat();

         if (!(formatType == FileFormatType.CSV || formatType == FileFormatType.TSV))
         {
            if (ioSettings.getSeparator() == '\t')
            {
               ioSettings.setFileFormat(FileFormatType.TSV);
            }
            else
            {
               ioSettings.setFileFormat(FileFormatType.CSV);
            }
         }

         String charsetName = settings.getCsvEncoding();
         Charset charset = null;

         if (charsetName != null)
         {
            charset = Charset.forName(charsetName);
         }

         TeXPath texPath = new TeXPath(parser, file, charset);

         ioSettings.setDefaultName(name);

         return DatatoolDb.loadTeXParser(settings, 
           texPath, ioSettings, checkForVerbatim);
      }
      catch (IOException e)
      {
         throw new DatatoolImportException(
          getMessageHandler().getLabelWithValues("error.import.failed", 
           file.toString(), e.getMessage()), e);
      }
   }


   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
   }

   private DatatoolSettings settings;

}
