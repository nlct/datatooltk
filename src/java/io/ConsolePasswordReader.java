/*
    Copyright (C) 2013 Nicola L.C. Talbot
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

import java.io.Console;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.awt.Frame;

import com.dickimawbooks.datatooltk.DatatoolTk;
import com.dickimawbooks.datatooltk.UserCancelledException;
import com.dickimawbooks.datatooltk.MessageHandler;

/**
 * Class for reading password in from the console.
 */
public class ConsolePasswordReader implements DatatoolPasswordReader
{
   public ConsolePasswordReader(MessageHandler messageHandler, 
     int noConsoleAction)
   {
      this.noConsoleAction = noConsoleAction;
      this.messageHandler = messageHandler;
   }

   public char[] requestPassword()
     throws UserCancelledException
   {
      Console cons = System.console();

      char[] passwd;

      if (cons == null)
      {
         switch (noConsoleAction)
         {
            case NO_CONSOLE_STDIN:

              try
              {
                 return stdinRequestPassword().toCharArray();
              }
              catch (IOException e)
              {
                 throw new UserCancelledException(e);
              }

            case NO_CONSOLE_GUI:
               GuiPasswordReader reader = new GuiPasswordReader(
                  messageHandler.getHelpLib(), (Frame)null);
               
               passwd = reader.requestPassword();

               reader = null;

               return passwd;

            case NO_CONSOLE_ERROR:
               throw new UserCancelledException(
                 messageHandler.getLabel("error.no_console"));
            default:
              throw new IllegalArgumentException(
                "Invalid noConsoleAction "+noConsoleAction);
         }
      }

      if ((passwd = cons.readPassword("%s",
           messageHandler.getLabel("password.prompt"))) != null)
      {
         return passwd;
      }

      throw new UserCancelledException(messageHandler);
   }

   // Adapted from
   // http://www.javaxt.com/Tutorials/Console_Apps/How_To_Prompt_a_User_for_a_Username_and_Password_from_the_Command_Line
   // Provided in the event that there's no console available and
   // user has requested STDIN alternative.

   private String stdinRequestPassword()
     throws IOException
   {
      String passwd = "";
      ConsoleEraser eraser = new ConsoleEraser();
      System.out.print("Password (No Console): ");
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      eraser.start();

      try
      {
         passwd = in.readLine();
      }
      finally
      {
         eraser.halt();
         System.out.print("\b");
      }

      return passwd;
   }

   private static class ConsoleEraser extends Thread
   {
       private boolean running = true;

       public void run()
       {
           while (running)
           {
               System.out.print("\b ");
           }
       }

       public synchronized void halt()
       {
           running = false;
       }
   }

   public static final int NO_CONSOLE_STDIN = 0,
     NO_CONSOLE_GUI = 1,
     NO_CONSOLE_ERROR = 2;

   private int noConsoleAction = NO_CONSOLE_GUI;

   private MessageHandler messageHandler;
}
