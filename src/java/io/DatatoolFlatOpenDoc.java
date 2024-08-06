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

import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.*;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

//import javax.swing.JOptionPane;

import org.xml.sax.SAXException;
//import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
//import org.xml.sax.helpers.XMLReaderAdapter;

/*
import com.dickimawbooks.texparserlib.TeXObject;
import com.dickimawbooks.texparserlib.TeXSyntaxException;

import com.dickimawbooks.texparserlib.latex.datatool.IOSettings;
import com.dickimawbooks.texparserlib.latex.datatool.CsvBlankOption;
import com.dickimawbooks.texparserlib.latex.datatool.DatumType;

import com.dickimawbooks.texjavahelplib.MessageSystem;
*/

import com.dickimawbooks.datatooltk.*;

/**
 * Class handling importing flat Open Document spreadsheets (FODS).
 */
public class DatatoolFlatOpenDoc implements DatatoolImport
{
   public DatatoolFlatOpenDoc(DatatoolSettings settings)
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
      BufferedReader in = null;
      DatatoolDb db;
      MessageHandler messageHandler = getMessageHandler();

      try
      {
         in = Files.newBufferedReader(file.toPath());
         in.mark(256);

         String line = in.readLine();

         Matcher m = XML_PATTERN.matcher(line);

         if (m.matches())
         {
            String encoding = m.group(1);

            if (!encoding.equals("UTF-8"))
            {
               Charset charset = Charset.forName(encoding);

               if (!charset.equals(StandardCharsets.UTF_8))
               {
                  in.close();
                  in = null;
                  in = Files.newBufferedReader(file.toPath(), charset);
               }
            }
         }

         in.reset();

         OpenDocReader reader = new OpenDocReader(importSettings);

         reader.parseFlat(new InputSource(in));

         db = reader.getDataBase();

         if (db == null)
         {
            throw new DatatoolImportException(messageHandler.getLabelWithValues(
              "error.dbload.missing_data", file));
         }

         if (reader.verbatimFound())
         {
            messageHandler.warning(
              messageHandler.getLabel("warning.verb_detected"));
         }
      }
      catch (SAXException e)
      {
         Throwable cause = e.getCause();

         if (cause instanceof UserCancelledException)
         {
            throw new DatatoolImportException(
              messageHandler.getLabelWithValues(
                "error.import.failed", file, cause.getMessage()), cause);
         }
         else
         {
            throw new DatatoolImportException(
              messageHandler.getLabelWithValues(
                "error.import.failed", file, e.getMessage()), e);
         }
      }
      catch (Throwable e)
      {
         throw new DatatoolImportException(
           messageHandler.getLabelWithValues(
             "error.import.failed", file, e.getMessage()), e);
      }
      finally
      {
         if (in != null)
         {
            try
            {
               in.close();
            }
            catch (IOException e)
            {
               messageHandler.warning(e);
            }
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
   public static final Pattern XML_PATTERN
    = Pattern.compile("<\\?xml.*\\s+encoding\\s*=\\s*\"([^\"]+)\".*\\?>");
}
