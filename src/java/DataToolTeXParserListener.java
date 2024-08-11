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

import java.io.File;
import java.io.IOException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texparserlib.latex.*;
import com.dickimawbooks.texparserlib.latex.datatool.*;
import com.dickimawbooks.texparserlib.latex.probsoln.*;

public class DataToolTeXParserListener extends PreambleParser
{
   public DataToolTeXParserListener(DatatoolSettings settings)
     throws IOException
   {
      super(settings.getTeXApp(), UndefAction.WARN);

      this.settings = settings;
      setStopAtBeginDocument(settings.isPreambleOnly());

      TeXParser parser = new TeXParser(this);
      parser.setBaseDir(new File(System.getProperty("user.dir")));

      setParser(parser);

      MessageHandler messageHandler = settings.getMessageHandler();
      messageHandler.setDebugModeForParser(parser);

      datatoolSty = new DataToolSty(null, this, false);
      usepackage(datatoolSty, parser);

      probSolnSty = new ProbSolnSty(
         settings.getInitialRowCapacity(), true,
        null, this, false);
      usepackage(probSolnSty, parser);

      addVerbEnv("lstlisting");
      addVerbEnv("alltt");

      parser.addVerbCommand("lstinline");

      ioSettings = new IOSettings(datatoolSty);
   }

   @Override
   protected void addPredefined()
   {
      super.addPredefined();

      // Some commands that may be encountered in the preamble that
      // should be ignored.

      parser.putControlSequence(new GobbleOpt("DTLsortdata", 1, 2));
      parser.putControlSequence(new GobbleOpt("dtlsort", 1, 3));
      parser.putControlSequence(new GobbleOpt("DTLsort", 1, 2, '*'));

      parser.putControlSequence(new AtGobble("SetStartMonth"));
      parser.putControlSequence(new AtGobble("SetStartYear"));
      parser.putControlSequence(new AtGobble("SetUsedFileName"));
      parser.putControlSequence(new AtGobble("ClearUsedFile"));
      parser.putControlSequence(new GobbleOpt("ExcludePreviousFile", 1, 1));
   }

   public DataToolSty getDataToolSty()
   {
      return datatoolSty;
   }

   public ProbSolnSty getProbSolnSty()
   {
      return probSolnSty;
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
      ioSettings.setColumnHeaders(null);
      ioSettings.setColumnKeys(null);
      ioSettings.setAppendAllowed(false);
      ioSettings.setExpandOption(IOExpandOption.NONE);
   }

   public void applyCurrentSettings()
     throws TeXSyntaxException
   {
      resetIOSettings();
      settings.applyDefaultOutputFormat(ioSettings);
      applyCurrentCsvSettings(false);
   }

   public void applyCurrentCsvSettings()
     throws TeXSyntaxException
   {
      applyCurrentCsvSettings(true);
   }

   public void applyCurrentCsvSettings(boolean changeDefaultFileSettings)
     throws TeXSyntaxException
   {
      int sep = settings.getSeparator();

      if (changeDefaultFileSettings)
      {
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
      }

      ioSettings.setDefaultName(null);

      ioSettings.setSeparator(sep);
      ioSettings.setDelimiter(settings.getDelimiter());
      ioSettings.setHeaderIncluded(settings.hasCSVHeader());
      ioSettings.setSkipLines(settings.getCSVskiplines());
      ioSettings.setCsvBlankOption(settings.getCsvBlankOption());

      ioSettings.setAutoKeys(settings.isAutoKeysOn());
      ioSettings.setColumnKeys(settings.getColumnKeys());

      String[] headers = settings.getColumnHeaders();

      if (headers == null || headers.length == 0)
      {
         ioSettings.setColumnHeaders(null);
      }
      else
      {
         TeXObject[] texHeaders = new TeXObject[headers.length];

         for (int i = 0; i < headers.length; i++)
         {
            texHeaders[i] = createString(headers[i]);
         }

         ioSettings.setColumnHeaders(texHeaders);
      }

      ioSettings.setEscapeCharsOption(settings.getEscapeCharsOption());
      ioSettings.setAddDelimiterOption(settings.getAddDelimiterOption());
      ioSettings.setCsvBlankOption(settings.getCsvBlankOption());

      ioSettings.setTrimElement(settings.isAutoTrimLabelsOn());
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
   protected ProbSolnSty probSolnSty;
   protected DatatoolSettings settings;
   protected IOSettings ioSettings;
}
