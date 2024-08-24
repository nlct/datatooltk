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
package com.dickimawbooks.datatooltk.gui;

import java.awt.Component;

import javax.swing.JOptionPane;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texjavahelplib.*;

import com.dickimawbooks.datatooltk.base.DatatoolTk;
import com.dickimawbooks.datatooltk.base.MessageHandler;

/**
 * Handler for dealing with gui/batch messages.
 */
public class CombinedMessageHandler extends MessageHandler
{
   public CombinedMessageHandler(DatatoolTk datatooltk)
   {
      this(true, datatooltk);
   }

   public CombinedMessageHandler(boolean isBatchMode, DatatoolTk datatooltk)
   {
      super(isBatchMode, datatooltk);
   }


   @Override
   public void progress(int percentage)
   {
      if (isBatchMode) return;

      guiResources.progress(percentage);
   }

   @Override
   public void progress(String msg)
   {
      if (isBatchMode)
      {
         message(msg);
      }
      else
      {
         guiResources.progress(msg);
      }
   }

   @Override
   public void startBuffering()
   {
      if (isBatchMode) return;

      errorBuffer = new StringBuffer();
      warningBuffer = new StringBuffer();
   }

   @Override
   public void stopBuffering()
   {
      if (isBatchMode) return;

      if (errorBuffer.length() > 0)
      {
         guiResources.error(bufferingComponent, errorBuffer.toString());
      }

      if (warningBuffer.length() > 0)
      {
         guiResources.warning(bufferingComponent, warningBuffer.toString());
      }

      errorBuffer = null;
      warningBuffer = null;
   }

   @Override
   public boolean buffering()
   {
      return errorBuffer != null;
   }

   @Override
   public void warning(Component parent, String message)
   {
      logMessage(message);

      if (isBatchMode)
      {
         System.err.println(String.format("%s: %s", 
           getApplicationName(), message));
      }
      else
      {
         if (buffering())
         {
            bufferingComponent = parent;
            warningBuffer.append(String.format("%s%n", message));
         }
         else if (guiResources == null)
         {
            JOptionPane.showMessageDialog(null, message,
              getMessageWithFallback("warning.title", "Warning"),
              JOptionPane.WARNING_MESSAGE);
            System.err.println(message);
         }
         else
         {
            guiResources.warning(parent, message);
         }
      }
   }

   @Override
   public void error(Component parent, String msg, 
     Throwable exception, int code)
   {
      logMessage(msg);
      logMessage(exception);

      if (isBatchMode)
      {
         if (msg == null)
         {
            System.err.println(String.format("%s: %s", 
              getApplicationName(), getMessage(exception)));
         }
         else
         {
            System.err.println(String.format("%s: %s", 
              getApplicationName(), msg));
         }

         if (exception != null)
         {
            Throwable cause = exception.getCause();

            if (cause != null)
            {
               System.err.println(getMessage(cause));
            }
         }
      }
      else
      {
         if (buffering())
         {
            bufferingComponent = parent;

            if (msg == null)
            {
               errorBuffer.append(getMessage(exception));
            }
            else
            {
               errorBuffer.append(msg);
            }
         }
         else
         {
            // guiResources may be null if there's an error
            // in the command line syntax

            if (msg != null && exception != null)
            {
               if (guiResources == null)
               {
                  JOptionPane.showMessageDialog(null, msg, 
                    getLabel("error.title"),
                    JOptionPane.ERROR_MESSAGE);
                  System.err.println(msg);

                  debug(exception);
               }
               else
               {
                  guiResources.error(parent, msg, exception);
               }
            }
            else if (exception == null)
            {
               if (guiResources == null)
               {
                  JOptionPane.showMessageDialog(null, msg, 
                    getLabel("error.title"),
                    JOptionPane.ERROR_MESSAGE);
                  System.err.println(msg);
               }
               else
               {
                  guiResources.error(parent, msg);
               }
            }
            else
            {
               if (guiResources == null)
               {
                  JOptionPane.showMessageDialog(null, 
                    exception.getMessage(), 
                    getLabel("error.title"),
                    JOptionPane.ERROR_MESSAGE);

                  System.err.println(exception.getMessage());
                  debug(exception);
               }
               else
               {
                  guiResources.error(parent, exception);
               }
            }
         }
      }

      if (exception != null && debugMode)
      {
         exception.printStackTrace();
      }
   }

   public DatatoolGuiResources getDatatoolGuiResources()
   {
      return guiResources;
   }

   public void setDatatoolGuiResources(DatatoolGuiResources guiResources)
   {
      this.guiResources = guiResources;
   }

   private DatatoolGuiResources guiResources;

   private StringBuffer errorBuffer, warningBuffer;

   private Component bufferingComponent=null;
}
