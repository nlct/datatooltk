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
package com.dickimawbooks.datatooltk;

import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.*;
import com.dickimawbooks.texparserlib.latex.datatool.*;

public class DataToolTeXParserListener extends PreambleParser
{
   public DataToolTeXParserListener(DatatoolSettings settings)
     throws IOException
   {
      super(settings.getTeXApp(), UndefAction.WARN);

      setParser(new TeXParser(this));

      MessageHandler messageHandler = settings.getMessageHandler();
      messageHandler.setDebugModeForParser(parser);

      datatoolSty = new DataToolSty(null, this, false);
      usepackage(datatoolSty, parser);

      addVerbEnv("lstlisting");
      addVerbEnv("alltt");

      parser.addVerbCommand("lstinline");

      ioSettings = new IOSettings(datatoolSty);
   }

   public DataToolSty getDataToolSty()
   {
      return datatoolSty;
   }

   public DatatoolSettings getSettings()
   {
      return settings;
   }

   public IOSettings getIOSettings()
   {
      return ioSettings;
   }

   public void resetIOSettings()
     throws TeXSyntaxException
   {
      ioSettings.setDefaultExtension("dbtex");
      ioSettings.setFileFormat(FileFormatType.DBTEX, "3.0");
      ioSettings.setFileOverwriteOption(FileOverwriteOption.ALLOW);
      ioSettings.setHeaderIncluded(true);
      ioSettings.setTrimElement(true);
      ioSettings.setAutoKeys(false);
      ioSettings.setAppendAllowed(false);
      ioSettings.setExpandOption(IOExpandOption.NONE);
   }

   public void applyCurrentCsvSettings()
     throws TeXSyntaxException
   {
      int sep = settings.getSeparator();

      if (sep == '\t')
      {
         ioSettings.setDefaultExtension("tsv");
         ioSettings.setFileFormat(FileFormatType.TSV);
      }
      else
      {
         ioSettings.setDefaultExtension("csv");
         ioSettings.setFileFormat(FileFormatType.CSV);
      }

      ioSettings.setSeparator(sep);
      ioSettings.setDelimiter(settings.getDelimiter());
      ioSettings.setHeaderIncluded(settings.hasCSVHeader());
      ioSettings.setSkipLines(settings.getCSVskiplines());
      ioSettings.setCsvBlankOption(settings.getCsvBlankOption());

      ioSettings.setEscapeCharsOption(settings.getEscapeCharsOption());
      ioSettings.setAddDelimiterOption(settings.getAddDelimiterOption());
      ioSettings.setCsvBlankOption(settings.getCsvBlankOption());

      ioSettings.setCsvStrictQuotes(settings.hasCSVstrictquotes());

      if (settings.isTeXMappingOn())
      {
         ioSettings.setCsvLiteral(true);
         datatoolSty.setCsvLiteralMappingOn(true);
      }
      else
      {
         ioSettings.setCsvLiteral(false);
         datatoolSty.setCsvLiteralMappingOn(false);
      }
   }

   protected DataToolSty datatoolSty;
   protected DatatoolSettings settings;
   protected IOSettings ioSettings;
}
