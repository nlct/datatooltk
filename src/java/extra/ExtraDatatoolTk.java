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
package com.dickimawbooks.datatooltk.extra;

import java.io.*;

import javax.swing.JOptionPane;

import com.dickimawbooks.texjavahelplib.*;

import com.dickimawbooks.datatooltk.*;
import com.dickimawbooks.datatooltk.io.*;

import com.dickimawbooks.datatooltk.gui.DatatoolProperties;
import com.dickimawbooks.datatooltk.gui.DatatoolGuiResources;
import com.dickimawbooks.datatooltk.gui.DatatoolGUI;


/**
 * Main application class with batch and GUI mode.
 */
public class ExtraDatatoolTk extends DatatoolTk
{
   public ExtraDatatoolTk() throws IOException
   {
      // Allows GUI and SQL and Binary Excel.
      super(true, true, true);
   }

   @Override
   protected DatatoolSettings createSettings() throws IOException
   {
      properties = new DatatoolProperties(this);
      properties.loadProperties();
      return properties;
   }

   @Override
   public String getApplicationName()
   {
      return APP_NAME+"-extra";
   }

   @Override
   public DatatoolImport getDatatoolImport(int fmtId,
       DatatoolSettings settings)
   throws UnsupportedFileFormatException
   {
      if (fmtId == DatatoolFileFormat.FILE_FORMAT_FLAG_XLS)
      {
         if (allowsXLS)
         {
            return new DatatoolExcel(settings);
         }
         else
         {
            throw new UnsupportedFileFormatException(
              getLabelWithValues("error.unsupported_option", "xls"));
         }
      }

      return super.getDatatoolImport(fmtId, settings);
   }

   @Override
   public String getAppInfo(boolean html)
   {
      String par = html ? "<p>" : String.format("%n%n");
      String nl = html ? "<br>" : String.format("%n");

      StringBuilder builder = new StringBuilder(super.getAppInfo(html));

      String log4jVersion = org.apache.logging.log4j.util.PropertiesUtil.class.getPackage().getImplementationVersion();

      builder.append(par);

      String info = getHelpLib().getMessage("about.extra");

      if (html)
      {
         info = TeXJavaHelpLib.encodeHTML(info, false);
      }

      builder.append(info);

      builder.append(nl);

      if (log4jVersion == null)
      {
         log4jVersion = "??";
      }

      info = getHelpLib().getMessage("about.log4j_details", log4jVersion);

      if (html)
      {
         info = TeXJavaHelpLib.encodeHTML(info, false);
      }

      builder.append(info);

      return builder.toString();
   }

   @Override
   protected void process()
   {
      if (settings.isBatchMode())
      {
         doBatchProcess();
         exit(0);
      }
      else
      {
         javax.swing.SwingUtilities.invokeLater(new Runnable()
          {
             public void run()
             {
                try
                {
                   new DatatoolGUI(properties, loadSettings);
                }
                catch (IOException e)
                {
                   JOptionPane.showMessageDialog(null, 
                    String.format("%s: Fatal I/O error: %s",
                      getApplicationName(), e.getMessage()),
                    "Error", JOptionPane.ERROR_MESSAGE);
                   e.printStackTrace();
                   exit(EXIT_IO);
                }
             }
          });
      } 
   }

   public static void main(String[] args)
   {
      DatatoolTk datatooltk = null;

      try
      {
         datatooltk = new ExtraDatatoolTk();
         datatooltk.runApplication(args);
      }
      catch (InvalidSyntaxException e)
      {
         exit(datatooltk, EXIT_SYNTAX, e, "Fatal syntax error", null, 
          MessageHandler.FORMAT_FAILURE, false);
      }
      catch (IOException e)
      {
         exit(datatooltk, EXIT_IO, e, "Fatal I/O error", null, 
          MessageHandler.OPEN_FAILURE, true);
      }
      catch (Throwable e)
      {
         exit(datatooltk, EXIT_OTHER, e, "Fatal runtime error", null, 
          MessageHandler.RUNTIME_FAILURE, true);
      }
   }

   DatatoolProperties properties;
}
