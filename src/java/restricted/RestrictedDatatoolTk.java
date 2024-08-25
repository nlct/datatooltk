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
package com.dickimawbooks.datatooltk.restricted;

import java.io.IOException;

import com.dickimawbooks.texjavahelplib.InvalidSyntaxException;

import com.dickimawbooks.datatooltk.base.DatatoolSettings;
import com.dickimawbooks.datatooltk.base.DatatoolTk;
import com.dickimawbooks.datatooltk.base.MessageHandler;

/**
 * Main application class with just a restricted batch mode.
 * There's no GUI, no support for SQL or Binary Excel and
 * no application properties directory.
 */
public class RestrictedDatatoolTk extends DatatoolTk
{
   public RestrictedDatatoolTk() throws IOException
   {
      // No support for GUI, SQL or Binary Excel.
      super(false, false, false);
   }

   @Override
   protected DatatoolSettings createSettings() throws IOException
   {
      return new DatatoolSettings(this);
   }

   @Override
   public String getApplicationName()
   {
      return APP_NAME+"-restricted";
   }

   @Override
   protected void process()
   {
      doBatchProcess();
   }

   public static void main(String[] args)
   {
      DatatoolTk datatooltk = null;

      try
      {
         datatooltk = new RestrictedDatatoolTk();
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
