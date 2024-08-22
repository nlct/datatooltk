/*
    Copyright (C) 2017-2024 Nicola L.C. Talbot
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
import java.io.PrintWriter;
import java.nio.file.*;
import java.net.URL;

import java.util.Vector;
import java.util.logging.ErrorManager;

import java.util.regex.Pattern;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.texjavahelplib.*;
import com.dickimawbooks.datatooltk.gui.DatatoolGuiResources;

/**
 * Handler for dealing with messages.
 */
public class MessageHandler extends ErrorManager 
  implements ErrorHandler,TeXJavaHelpLibApp
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

      texApp = new DatatoolTeXApp(this);
   }

   public TeXApp getTeXApp()
   {
      return texApp;
   }

   public DatatoolTeXApp getDatatoolTeXApp()
   {
      return texApp;
   }

   @Override
   public String getApplicationName()
   {
      return texApp.getApplicationName();
   }

   public static String codePointToString(int codePoint)
   {
      return new String(Character.toChars(codePoint));
   }

   public void progress(int percentage)
   {
      if (isBatchMode) return;

      guiResources.progress(percentage);
   }

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

   public void startBuffering()
   {
      if (isBatchMode) return;

      errorBuffer = new StringBuffer();
      warningBuffer = new StringBuffer();
   }

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

   public boolean buffering()
   {
      return errorBuffer != null;
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

      if (debugMode && verbosity < 1)
      {
         verbosity = 1;
      }
   }

   public boolean isDebugMode()
   {
      return debugMode;
   }

   @Override
   public boolean isDebuggingOn()
   {
      return debugMode;
   }

   public int getTeXParserDebugLevel()
   {
      return texParserDebugLevel;
   }

   public void setTeXParserDebugLevel(int level)
   {
      texParserDebugLevel = level;
   }

   public void setDebugModeForParser(TeXParser parser) throws IOException
   {
      checkLogWriterOpen();
      parser.setDebugMode(texParserDebugLevel, logWriter);
   }

  /**
      Checks the file extension for a requested output file.
      Just in case user has accidentally muddled the command line
      options, forbid potential input file extensions for output
      files.
      @param file the requested output file
      @throws IOException if extension is forbidden
   */
   public void checkOutputFileName(File file, boolean isLogFile) throws IOException
   {
      String name = file.getName();
      int idx = name.lastIndexOf(".");

      if (idx > -1)
      {
         String ext = name.substring(idx+1).toLowerCase();

         if (isLogFile && FORBIDDEN_LOG_EXTS_PATTERN.matcher(ext).matches())
         {
            throw new IOException(getHelpLib().getMessageWithFallback(
              "error.output.forbidden_log_ext",
              "File: {0}\nExtension ''{1}'' is forbidden for log files",
              file, ext));
         }

         if (FORBIDDEN_OUTPUT_EXTS_PATTERN.matcher(ext).matches())
         {
            throw new IOException(getHelpLib().getMessageWithFallback(
              "error.output.forbidden_ext",
              "File: {0}\nExtension ''{1}'' is forbidden for output files",
              file, ext));
         }
      }
   }

   public void setLogFile(String filename) throws IOException
   {
      if (filename == null || filename.isEmpty())
      {
         setLogFile((File)null);
      }
      else
      {
         setLogFile(new File(filename));
      }
   }

   public void setLogFile(File file) throws IOException
   {
      if (file != null)
      {
         checkOutputFileName(file, true);
      }

      closeLogFile();
      logFile = file;
   }

   public void closeLogFile()
   {
      if (logWriter != null)
      {
         logWriter.close();
         logWriter = null;
      }
   }

   protected void checkLogWriterOpen() throws IOException
   {
      if (logWriter == null && logFile != null)
      {
         logWriter = new PrintWriter(
           Files.newBufferedWriter(logFile.toPath()));
      }
   }

   public void logMessage(String msg)
   {
      if (logFile != null && msg != null)
      {
         try
         {
            checkLogWriterOpen();

            logWriter.println(msg);
         }
         catch (IOException e)
         {
            System.err.println(e);

            if (debugMode)
            {
               e.printStackTrace();
            }

            logFile = null;
            closeLogFile();
         }
      }
   }

   public void logMessage(Throwable t)
   {
      if (logFile != null && t != null)
      {
         try
         {
            if (logWriter == null)
            {
               logWriter = new PrintWriter(
                 Files.newBufferedWriter(logFile.toPath()));
            }

            t.printStackTrace(logWriter);
         }
         catch (IOException e)
         {
            System.err.println(e);

            if (debugMode)
            {
               e.printStackTrace();
            }

            logFile = null;
            closeLogFile();
         }
      }
   }

   public void setVerbosity(int level)
   {
      verbosity = level;
   }

   public int getVerbosityLevel()
   {
      return verbosity;
   }

   @Override
   public void message(String msg)
   {
      message(1, msg);
   }

   public void message(int level, String msg)
   {
      if (isBatchMode && level <= verbosity)
      {
         System.out.println(msg);
      }

      logMessage(msg);
   }

   public String getMessage(Throwable throwable)
   {
      if (throwable == null)
      {
         return "";
      }
      else if (throwable instanceof TeXSyntaxException)
      {
         return ((TeXSyntaxException)throwable).getMessage(texApp);
      }
      else
      {
         return throwable.getMessage();
      }
   }

   @Override
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

   @Override
   public void warning(Component parent, String message)
   {
      logMessage(message);

      if (isBatchMode)
      {
         System.err.println(String.format("%s: %s", 
           DatatoolTk.APP_NAME, message));
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
   public void warning(String msg, Throwable e)
   {
      warning(null, msg, e);
   }

   public void warning(Throwable e)
   {
      warning(null, getMessage(e), e);
   }

   public void warning(Component parent, Throwable e)
   {
      warning(parent, getMessage(e), e);
   }

   @Override
   public void warning(Component parent, String msg, Throwable e)
   {
      warning(parent, msg);

      if (debugMode)
      {
         e.printStackTrace();
         logMessage(e);
      }
   }

   @Override
   public void debug(String message)
   {
      if (debugMode)
      {
         System.err.println(String.format("%s: %s", 
           DatatoolTk.APP_NAME, message));

         logMessage(message);
      }
   }

   @Override
   public void debug(String message, Throwable e)
   {
      if (debugMode)
      {
         System.err.println(String.format("%s: %s", 
           DatatoolTk.APP_NAME, message));
         e.printStackTrace();

         logMessage(message);
      }
   }

   @Override
   public void debug(Component parent, Throwable e)
   {
      if (debugMode)
      {
         error(parent, e, GENERIC_FAILURE);
      }
   }

   @Override
   public void debug(Component parent, String message)
   {
      if (debugMode)
      {
         error(parent, message, null, GENERIC_FAILURE);
      }
   }

   @Override
   public void debug(Component parent, String message, Throwable e)
   {
      if (debugMode)
      {
         error(parent, message, e, GENERIC_FAILURE);
      }
   }

   @Override
   public void debug(Throwable e)
   {
      debug(getMessage(e), e);
   }

   @Override
   public void error(Throwable throwable)
   {
      if (throwable instanceof Exception)
      {
         error(null, null, (Exception)throwable, GENERIC_FAILURE);
      }
      else
      {
         error(getMessage(throwable), null, RUNTIME_FAILURE);

         if (debugMode)
         {
            throwable.printStackTrace();
            logMessage(throwable);
         }
      }
   }

   public void error(Throwable exception, int code)
   {
      error(null, null, exception, code);
   }

   @Override
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

   @Override
   public void error(Component parent, Throwable e)
   {
      error(parent, null, e, GENERIC_FAILURE);
   }

   @Override
   public void error(Component parent, String message, Throwable e)
   {
      error(parent, message, e, GENERIC_FAILURE);
   }

   public void error(Component parent, Throwable e, int code)
   {
      error(parent, null, e, code);
   }

   @Override
   public void error(String msg, Exception exception, int code)
   {
      error(null, msg, exception, code);
   }

   @Override
   public void error(String msg, Throwable exception)
   {
      error(null, msg, exception, GENERIC_FAILURE);
   }

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
              DatatoolTk.APP_NAME, getMessage(exception)));
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

   public void error(SAXParseException exception)
   {
      error(getMessage(exception), exception, FORMAT_FAILURE);
   }

   public void warning(SAXParseException exception)
   {
      warning(exception);
   }

   public void fatalError(SAXParseException exception)
   {
      error(getMessage(exception), exception, GENERIC_FAILURE);
      exception.printStackTrace();
      logMessage(exception);
   }

   public String getMessageWithFallback(String label, String fallbackFormat,
     Object... params)
   {
      return datatooltk.getMessageWithFallback(label, fallbackFormat, params);
   }

   public String getMessageIfExists(String label, Object... args)
   {
      return datatooltk.getMessageIfExists(label, args);
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

   public String getLabelWithValues(String label, Object... values)
   {
      return datatooltk.getLabelWithValues(label, values);
   }

   public String getLabelWithValues(int lineNumber, String label, Object... values)
   {
      String msg = datatooltk.getLabelWithValues(label, values);

      return datatooltk.getLabelWithValues("error.line",
       lineNumber, msg);
   }

   public String getToolTip(String label)
   {
      return datatooltk.getToolTip(label);
   }

   public String getToolTip(String parent, String label)
   {
      return datatooltk.getToolTip(parent, label);
   }

   public int getMnemonicInt(String label)
   {
      return datatooltk.getMnemonicInt(label);
   }

   public int getMnemonicInt(String parent, String label)
   {
      return datatooltk.getMnemonicInt(parent, label);
   }

   @Override
   public ImageIcon getSmallIcon(String base, String... extensions)
   {
      return null;
   }

   @Override
   public IconSet getSmallIconSet(String base, String... extensions)
   {
      return null;
   }

   @Override
   public ImageIcon getLargeIcon(String base, String... extensions)
   {
      return null;
   }

   @Override
   public IconSet getLargeIconSet(String base, String... extensions)
   {
      return null;
   }

   public KeyStroke getKeyStroke(String parentLabel, String actionLabel)
   {
      String property;

      if (parentLabel == null)
      {
         property = actionLabel;
      }
      else
      {
         property = parentLabel + "." + actionLabel;
      }

      return datatooltk.getKeyStroke(property);
   }

   public KeyStroke getKeyStroke(String property)
   {
      return datatooltk.getKeyStroke(property);
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

   public TeXJavaHelpLib getHelpLib()
   {
      return datatooltk.getHelpLib();
   }

   @Override
   public void dictionaryLoaded(URL url)
   {
      if (dictionarySources == null)
      {
         dictionarySources = new Vector<URL>();
      }

      dictionarySources.add(url);
   }

   public Vector<URL> getLoadedDictionaries()
   {
      return dictionarySources;
   }

   private int verbosity = 0;
   private boolean isBatchMode, debugMode=false;
   private DatatoolTk datatooltk;
   private DatatoolGuiResources guiResources;
   private File logFile;
   private PrintWriter logWriter;
   private int texParserDebugLevel=0;

   private Vector<URL> dictionarySources;

   private DatatoolTeXApp texApp;

   private StringBuffer errorBuffer, warningBuffer;

   private Component bufferingComponent=null;

   public static final int SYNTAX_FAILURE=7;
   public static final int RUNTIME_FAILURE=8;

   public static final Pattern FORBIDDEN_LOG_EXTS_PATTERN
    = Pattern.compile("(dbtex|dtltex)");
   public static final Pattern FORBIDDEN_OUTPUT_EXTS_PATTERN
    = Pattern.compile("(tex|ltx|csv|tsv|xlsx?|ods|sty|cls|def|ldf|bib)");
}
