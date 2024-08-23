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
package com.dickimawbooks.datatooltk.combined;

import java.io.*;

import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import com.dickimawbooks.texjavahelplib.*;

import com.dickimawbooks.datatooltk.io.*;
import com.dickimawbooks.datatooltk.gui.DatatoolGuiResources;
import com.dickimawbooks.datatooltk.gui.DatatoolGUI;

import com.dickimawbooks.datatooltk.*;

/**
 * Main application class with batch and GUI mode.
 */
public class FullDatatoolTk extends DatatoolTk
{
   public FullDatatoolTk() throws IOException
   {
      // Allows both GUI and SQL.
      super(true, true);

      MessageHandler messageHandler = getMessageHandler();

      try
      {
         settings.loadProperties();
      }
      catch (IOException e)
      {
         messageHandler.error(messageHandler.getLabel("error.load_props_failed"), e,
             MessageHandler.OPEN_FAILURE);
      }
   }

   @Override
   protected DatatoolSettings createSettings() throws IOException
   {
      return new DatatoolSettings(this);
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
                   new DatatoolGUI(settings, loadSettings);
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
         datatooltk = new FullDatatoolTk();
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

}