/*
    Copyright (C) 2017 Nicola L.C. Talbot
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

import java.util.logging.ErrorManager;
import java.awt.Component;
import java.io.File;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.datatooltk.gui.DatatoolGuiResources;

/**
 * Handler for dealing with messages.
 */
public class MessageHandler extends ErrorManager 
  implements ErrorHandler
{
   public MessageHandler(DatatoolTk datatooltk)
   {
      this(true, datatooltk);
   }

   public MessageHandler(boolean isBatchMode, DatatoolTk datatooltk)
   {
      super();
      this.datatooltk = datatooltk;
      init(isBatchMode);
   }

   private void init(boolean isBatchMode)
   {
      this.isBatchMode = isBatchMode;

      texApp = new TeXAppAdapter()
      {
         public String getMessage(String label, Object... params)
         {
            return getLabelWithValues(label, params);
         }

         public void message(String text)
         {
            message(text);
         }

         public void warning(TeXParser parser, String message)
         {
            warning(parser, message);
         }
      
         public void error(Exception e)
         {
            error(e);
         }
      };
   }

   public TeXApp getTeXApp()
   {
      return texApp;
   }

   public void setBatchMode(boolean isBatchMode)
   {
      this.isBatchMode = isBatchMode;
   }

   public boolean isBatchMode()
   {
      return isBatchMode;
   }

   public void setDebugMode(boolean enabled)
   {
      debugMode = enabled;
   }

   public boolean isDebugMode()
   {
      return debugMode;
   }

   public void message(String msg)
   {
      if (isBatchMode)
      {
         System.out.println(msg);
      }
   }

   public void warning(String message)
   {
      warning((Component)null, message);
   }

   public void warning(TeXParser parser, String message)
   {
      File file = null;
      int lineNumber = -1;

      if (parser != null)
      {
         file = parser.getCurrentFile();
         TeXReader reader = parser.getReader();

         if (reader != null)
         {
            lineNumber = reader.getLineNumber();
         }
      }

      if (file != null)
      {
         if (lineNumber > 0)
         {
            message = String.format("%s:%d: %s", file,
             lineNumber, message);
         }
         else
         {
            message = String.format("%s: %s", file, message);
         }
      }

      warning((Component)null, message);
   }

   public void warning(Component parent, String message)
   {
      if (isBatchMode)
      {
         System.err.println(String.format("%s: %s", 
           DatatoolTk.APP_NAME, message));
      }
      else
      {
         guiResources.warning(parent, message);
      }
   }

   public void warning(Exception e)
   {
      warning(null, e);
   }

   public void warning(Component parent, Exception e)
   {
      warning(parent, e.getMessage());

      if (debugMode)
      {
         e.printStackTrace();
      }
   }

   public void debug(String message)
   {
      if (debugMode)
      {
         System.err.println(String.format("%s: %s", 
           DatatoolTk.APP_NAME, message));
      }
   }

   public void debug(String message, Exception e)
   {
      if (debugMode)
      {
         System.err.println(String.format("%s: %s", 
           DatatoolTk.APP_NAME, message));
         e.printStackTrace();
      }
   }

   public void debug(Exception e)
   {
      debug(e.getMessage(), e);
   }

   public void error(Throwable throwable)
   {
      if (throwable instanceof Exception)
      {
         error(null, null, (Exception)throwable, GENERIC_FAILURE);
      }
      else if (throwable instanceof TeXSyntaxException)
      {
         TeXSyntaxException e = (TeXSyntaxException)throwable;

         error(null, e.getMessage(texApp), e, SYNTAX_FAILURE);
      }
      else
      {
         error(throwable.getMessage(), null, RUNTIME_FAILURE);

         if (debugMode)
         {
            throwable.printStackTrace();
         }
      }
   }

   public void error(Exception exception, int code)
   {
      error(null, null, exception, code);
   }

   public void error(String msg)
   {
      error(msg, null, GENERIC_FAILURE);
   }

   public void error(String msg, int code)
   {
      error(msg, null, code);
   }

   public void error(Component parent, String msg)
   {
      error(parent, msg, null, GENERIC_FAILURE);
   }

   public void error(Component parent, String msg, int code)
   {
      error(parent, msg, null, code);
   }

   public void error(Component parent, Exception e)
   {
      error(parent, null, e, GENERIC_FAILURE);
   }

   public void error(Component parent, String message, Exception e)
   {
      error(parent, message, e, GENERIC_FAILURE);
   }

   public void error(Component parent, Exception e, int code)
   {
      error(parent, null, e, code);
   }

   public void error(String msg, Exception exception, int code)
   {
      error(null, msg, exception, code);
   }

   public void error(Component parent, String msg, 
     Exception exception, int code)
   {
      if (isBatchMode)
      {
         if (msg == null)
         {
            System.err.println(String.format("%s: %s", 
              DatatoolTk.APP_NAME, exception.getMessage()));
         }
         else
         {
            System.err.println(String.format("%s: %s", 
              DatatoolTk.APP_NAME, msg));
         }

         if (exception != null)
         {
            Throwable cause = exception.getCause();

            if (cause != null)
            {
               System.err.println(cause.getMessage());
            }
         }
      }
      else
      {
         if (msg != null && exception != null)
         {
            guiResources.error(parent, msg, exception);
         }
         else if (exception == null)
         {
            guiResources.error(parent, msg);
         }
         else
         {
            guiResources.error(parent, exception);
         }
      }

      if (exception != null && debugMode)
      {
         exception.printStackTrace();
      }
   }

   public void error(SAXParseException exception)
   {
      error(exception.getMessage(), exception, FORMAT_FAILURE);
   }

   public void warning(SAXParseException exception)
   {
      warning(exception);
   }

   public void fatalError(SAXParseException exception)
   {
      error(exception.getMessage(), exception, GENERIC_FAILURE);
      exception.printStackTrace();
   }

   public String getLabelWithAlt(String label, String alt)
   {
      return datatooltk.getLabelWithAlt(label, alt);
   }

   public String getLabelRemoveArgs(String parent, String label)
   {
      return datatooltk.getLabelRemoveArgs(parent, label);
   }

   public String getLabel(String label)
   {
      return datatooltk.getLabel(label);
   }

   public String getLabel(String parent, String label)
   {
      return datatooltk.getLabel(parent, label);
   }

   public String getLabelWithValue(String label, String value)
   {
      return datatooltk.getLabelWithValue(label, value);
   }

   public String getLabelWithValue(String label, int value)
   {
      return datatooltk.getLabelWithValue(label, value);
   }

   public String getLabelWithValues(String label, int value1,
     int value2)
   {
      return datatooltk.getLabelWithValues(label, value1, value2);
   }

   public String getLabelWithValues(String label, String value1,
     String value2)
   {
      return datatooltk.getLabelWithValues(label, value1, value2);
   }

   public String getLabelWithValues(String label, Object... values)
   {
      return datatooltk.getLabelWithValues(label, values);
   }

   public String getToolTip(String label)
   {
      return datatooltk.getToolTip(label);
   }

   public String getToolTip(String parent, String label)
   {
      return datatooltk.getToolTip(parent, label);
   }

   public char getMnemonic(String label)
   {
      return datatooltk.getMnemonic(label);
   }

   public char getMnemonic(String parent, String label)
   {
      return datatooltk.getMnemonic(parent, label);
   }

   public int getMnemonicInt(String label)
   {
      return datatooltk.getMnemonicInt(label);
   }

   public int getMnemonicInt(String parent, String label)
   {
      return datatooltk.getMnemonicInt(parent, label);
   }

   public DatatoolTk getDatatoolTk()
   {
      return datatooltk;
   }

   public DatatoolGuiResources getDatatoolGuiResources()
   {
      return guiResources;
   }

   public void setDatatoolGuiResources(DatatoolGuiResources guiResources)
   {
      this.guiResources = guiResources;
   }

   public DatatoolSettings getSettings()
   {
      return datatooltk.getSettings();
   }

   private boolean isBatchMode, debugMode=false;
   private DatatoolTk datatooltk;
   private DatatoolGuiResources guiResources;

   private TeXApp texApp;

   public static final int SYNTAX_FAILURE=7;
   public static final int RUNTIME_FAILURE=8;
}
