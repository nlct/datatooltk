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

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.datatool.*;

import com.dickimawbooks.datatooltk.*;

/**
 * Class handling importing and exporting CSV data.
 * This is now using the TeX Parser Library instead of opencsv,
 * partly to be more consistent with datatool.sty's \DTLread and
 * \DTLwrite and partly to reduce the number of dependent libraries.
 */
public class DatatoolCsv extends DatatoolTeX
{
   public DatatoolCsv(DatatoolSettings settings)
   {
      super(settings);
   }

   public void exportData(DatatoolDb db, String target)
      throws DatatoolExportException
   {
      try
      {
         DataToolTeXParserListener listener = settings.getTeXParserListener();
         listener.applyCurrentCsvSettings();
         IOSettings ioSettings = listener.getIOSettings();
         ioSettings.setFileOverwriteOption(FileOverwriteOption.ALLOW);

         exportData(db, ioSettings, target);
      }
      catch (IOException e)
      {
         throw new DatatoolExportException(
           getMessageHandler().getLabelWithValues(
             "error.export.failed", target), e);
      }
   }

   public DatatoolDb importData(File file, String name)
      throws DatatoolImportException
   {
      return importData(file, name, true);
   }

   @Override
   public DatatoolDb importData(File file, boolean checkForVerbatim)
      throws DatatoolImportException
   {
      return importData(file, null, checkForVerbatim);
   }

   public DatatoolDb importData(File file, String name, boolean checkForVerbatim)
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

      try
      {
         DataToolTeXParserListener listener = settings.getTeXParserListener();
         TeXParser parser = listener.getParser();
         listener.applyCurrentCsvSettings();
         IOSettings ioSettings = listener.getIOSettings();

         ioSettings.setDefaultName(name);

         return importData(ioSettings, file, checkForVerbatim);
      }
      catch (IOException e)
      {
         throw new DatatoolImportException(
          getMessageHandler().getLabelWithValues("error.import.failed", 
           file.toString(), e.getMessage()), e);
      }
   }

   @Override
   public DatatoolDb importData(ImportSettings importSettings, String source)
      throws DatatoolImportException
   {
      File file = new File(source);
      String name = file.getName();

      name = file.getName();
      int idx = name.lastIndexOf(".");

      if (idx != -1)
      {
         name = name.substring(0, idx);
      }

      try
      {
         DataToolTeXParserListener listener = settings.getTeXParserListener();
         TeXParser parser = listener.getParser();
         listener.applyCurrentCsvSettings();
         IOSettings ioSettings = listener.getIOSettings();

         ioSettings.setDefaultName(name);
         importSettings.applyTo(ioSettings, parser);

         if (importSettings.getSeparator() == '\t')
         {
            ioSettings.setFileFormat(FileFormatType.TSV);
         }
         else
         {
            ioSettings.setFileFormat(FileFormatType.CSV);
         }

         return importData(ioSettings, file,
           importSettings.isCheckForVerbatimOn());
      }
      catch (IOException e)
      {
         throw new DatatoolImportException(
          getMessageHandler().getLabelWithValues("error.import.failed", 
           file.toString(), e.getMessage()), e);
      }
   }

}
