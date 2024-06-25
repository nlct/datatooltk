/*
    Copyright (C) 2013-2024 Nicola L.C. Talbot
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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;

import java.net.URISyntaxException;
import java.net.URL;

import java.util.Locale;
import java.util.Locale.Builder;
import java.util.Properties;
import java.util.Vector;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.text.MessageFormat;

import java.awt.Cursor;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import com.dickimawbooks.texparserlib.TeXParser;
import com.dickimawbooks.texparserlib.TeXSyntaxException;
import com.dickimawbooks.texparserlib.html.HtmlTag;
import com.dickimawbooks.texjavahelplib.*;

import com.dickimawbooks.datatooltk.io.*;
import com.dickimawbooks.datatooltk.gui.DatatoolGuiResources;
import com.dickimawbooks.datatooltk.gui.DatatoolGUI;
import com.dickimawbooks.datatooltk.gui.DatatoolDbPanel;

/**
 * Main application class.
 */
public class DatatoolTk
{
   public DatatoolTk() throws IOException
   {
      settings = new DatatoolSettings(this);

      MessageHandler messageHandler = settings.getMessageHandler();

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

   public MessageHandler getMessageHandler()
   {
      return settings.getMessageHandler();
   }

   public DatatoolSettings getSettings()
   {
      return settings;
   }

   public void doBatchProcess()
   {
      settings.setPasswordReader(new ConsolePasswordReader(
        getMessageHandler(), noConsoleAction));

      File inFile = loadSettings.getInputFile();
      DatatoolImport imp = loadSettings.getDataImport();

      if (imp == null && inFile == null)
      {
         getMessageHandler().error(getLabelWithValues("error.cli.no_data",
           "--gui", "--help"), MessageHandler.SYNTAX_FAILURE);
         System.exit(1);
      }

      File outFile = loadSettings.getOutputFile();

      if (outFile == null)
      {
         getMessageHandler().error(getLabelWithValues("error.cli.no_out",
           "--output", "--gui", "--help"), MessageHandler.SYNTAX_FAILURE);
         System.exit(1);
      }

      DatatoolDb db = null;

      try
      {
         if (inFile != null)
         {
            debug("Loading '"+inFile+"'");
            db = DatatoolDb.load(settings, inFile);
         }
         else
         {
            String source = loadSettings.getImportSource();

            debug("Importing data via '"+source+"'");
            db = imp.importData(source);
         }

         String dbname = loadSettings.getDbName();

         if (dbname != null)
         {
            debug("Setting name to '"+dbname+"'");
            db.setName(dbname);
         }

         File mergeFile = loadSettings.getMergeFile();
         DatatoolImport mergeImport = loadSettings.getMergeImport();
         String mergeImportSource = loadSettings.getMergeImportSource();
         String mergeKey = loadSettings.getMergeKey();

         if (mergeFile != null)
         {
            debug("Loading '"+mergeFile+"'");
            DatatoolDb mergeDb = DatatoolDb.load(settings, mergeFile);
            db.merge(mergeDb, mergeKey);
         }
         else if (mergeImportSource != null && mergeImport != null)
         {
            debug("Importing data via '"+mergeImportSource+"'");
            DatatoolDb mergeDb = mergeImport.importData(mergeImportSource);
            db.merge(mergeDb, mergeKey);
         }

         String sort = loadSettings.getSort();

         if (sort != null)
         {
            debug("sorting");

            db.setSortCaseSensitive(loadSettings.isCaseSensitive());

            int colIndex = db.getColumnIndex(sort);

            if (colIndex == -1)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.unknown_field",
                  sort));
            }

            db.setSortColumn(colIndex);
            db.setSortAscending(loadSettings.isAscending());
            db.sort();
         }

         if (loadSettings.isShuffleOn())
         {
            debug("Shuffling");
            db.shuffle();
         }

         Vector<FilterInfo> filterInfo = loadSettings.getFilterInfo();

         if (filterInfo != null)
         {
            debug("Filtering");
            DataFilter filter = new DataFilter(db, loadSettings.isFilterOr());

            filter.addFilters(filterInfo);

            if (loadSettings.isFilterInclude())
            {
               db.removeNonMatching(filter);
            }
            else
            {
               db.removeMatching(filter);
            }
         }

         int truncate = loadSettings.getTruncate();

         if (truncate > -1)
         {
            db.truncate(truncate);
         }

         String columnList = loadSettings.getRemoveColumnList();

         if (columnList != null)
         {
            db.removeColumns(columnList);
         }
         else
         {
            columnList = loadSettings.getRemoveExceptColumnList();

            if (columnList != null)
            {
               db.removeExceptColumns(columnList);
            }
         }

         debug("Saving '"+outFile+"'");
         db.save(outFile);
      }
      catch (InvalidSyntaxException e)
      {
         getMessageHandler().error(e, MessageHandler.SYNTAX_FAILURE);
         System.exit(1);
      }
      catch (Throwable e)
      {
         getMessageHandler().error(e);
         System.exit(1);
      }

      debug("Completed");
      System.exit(0);
   }

   public void help()
   {
      version();
      System.out.println();
      System.out.println(getLabel("syntax.title"));
      System.out.println();
      System.out.println(APP_NAME+" --gui");
      System.out.println(getLabel("syntax.or"));
      System.out.println(getLabelWithValues("syntax.opt_db", APP_NAME));
      System.out.println(getLabel("syntax.or"));
      System.out.println(getLabelWithValues("syntax.opt_csv", APP_NAME));
      System.out.println(getLabel("syntax.or"));
      System.out.println(getLabelWithValues("syntax.opt_sql", APP_NAME));
      System.out.println();

      System.out.println(getLabel("syntax.general"));
      System.out.println(getLabelWithValues("syntax.gui", "--gui", "-g"));
      System.out.println(getLabelWithValues("syntax.batch", "--batch", "-b"));
      System.out.println(getLabelWithValues("syntax.in", 
        "--in", "-i", APP_NAME));
      System.out.println(getLabelWithValues("syntax.name", "--name"));
      System.out.println(getLabelWithValues("syntax.out", "--output", "-o"));
      System.out.println(getLabelWithValues("syntax.version", "--version", "-v"));
      System.out.println(getLabelWithValues("syntax.help", "--help", "-h"));
      System.out.println(getLabelWithValues("syntax.debug", "--debug"));
      System.out.println(getLabelWithValues("syntax.nodebug", "--nodebug"));
      System.out.println(getLabelWithValues("syntax.debug-mode", "--debug-mode"));
      System.out.println(getLabelWithValues("syntax.log", "--log"));
      System.out.println(getLabelWithValues("syntax.compat", "--compat"));

      System.out.println(getLabelWithValues("syntax.tex_encoding",
         "--tex-encoding"));

      System.out.println(getLabelWithValues("syntax.maptexspecials",
          "--map-tex-specials",
          (settings.isTeXMappingOn()?" ("+getLabel("syntax.default")+".)":"")));
      System.out.println(getLabelWithValues("syntax.nomaptexspecials",
         "--nomap-tex-specials",
         (settings.isTeXMappingOn() ?
         "": " ("+getLabel("syntax.default")+".)")));

      System.out.println(getLabelWithValues("syntax.autotrimlabels",
          "--auto-trim-labels",
          (settings.isAutoTrimLabelsOn() ? 
             " ("+getLabel("syntax.default")+".)":"")));
      System.out.println(getLabelWithValues("syntax.noautotrimlabels",
          "--noauto-trim-labels",
          (settings.isAutoTrimLabelsOn() ? 
             "":" ("+getLabel("syntax.default")+".)")));

      System.out.println(getLabelWithValues("syntax.seed", "--seed"));
      System.out.println(getLabelWithValues("syntax.shuffle", "--shuffle"));
      System.out.println(getLabelWithValues("syntax.no_shuffle", "--noshuffle"));
      System.out.println(getLabelWithValues("syntax.sort", "--sort"));
      System.out.println(getLabelWithValues("syntax.sort_case_sensitive",
         "--sort-case-sensitive"));
      System.out.println(getLabelWithValues("syntax.sort_case_insensitive",
         "--sort-case-insensitive"));
      System.out.println(getLabelWithValues("syntax.sort_locale",
         "--sort-locale"));
      System.out.println(getLabelWithValues("syntax.owner_only",
         "--owner_only", (settings.isOwnerOnly() ?
         " ("+getLabel("syntax.default")+".)" : "")));
      System.out.println(getLabelWithValues("syntax.noowner_only",
         "--noowner_only", (settings.isOwnerOnly() ?
         "" : " ("+getLabel("syntax.default")+".)")));
      System.out.println(getLabelWithValues("syntax.truncate",
         "--truncate"));
      System.out.println(getLabelWithValues("syntax.remove_cols",
         "--remove-columns"));
      System.out.println(getLabelWithValues("syntax.remove_except_cols",
         "--remove-except-columns"));
      System.out.println(getLabelWithValues("syntax.filter_or",
         "--filter-or"));
      System.out.println(getLabelWithValues("syntax.filter_and",
         "--filter-and"));
      System.out.println(getLabelWithValues("syntax.filter_include",
         "--filter-include"));
      System.out.println(getLabelWithValues("syntax.filter_exclude",
         "--filter-exclude"));
      System.out.println(getLabelWithValues("syntax.filter",
         "--filter"));
      System.out.println(getLabelWithValues("syntax.merge",
         "--merge"));
      System.out.println();

      System.out.println(getLabel("syntax.csv_opts"));
      System.out.println(getLabelWithValues("syntax.csv", "--csv"));
      System.out.println(getLabelWithValues("syntax.merge_csv", "--merge-csv"));
      System.out.println(getLabelWithValues("syntax.csv_sep", "--csv-sep", 
        MessageHandler.codePointToString(settings.getSeparator()), "--sep"));
      System.out.println(getLabelWithValues("syntax.csv_delim", "--csv-delim", 
        MessageHandler.codePointToString(settings.getDelimiter()), "--delim"));
      System.out.println(getLabelWithValues("syntax.csv_header",
        "--csv-header",
        (settings.hasCSVHeader()?" ("+getLabel("syntax.default")+".)":""),
        "--csvheader"));
      System.out.println(getLabelWithValues("syntax.csv_noheader",
        "--nocsv-header",
        (settings.hasCSVHeader()? "" : " ("+getLabel("syntax.default")+".)"),
        "--nocsvheader"));
      System.out.println(getLabelWithValues("syntax.skipemptyrows",
          "--csv-skip-empty-rows",
          (settings.isSkipEmptyRowsOn() ? 
             " ("+getLabel("syntax.default")+".)":"")));
      System.out.println(getLabelWithValues("syntax.noskipemptyrows",
          "--nocsv-skip-empty-rows",
          (settings.isSkipEmptyRowsOn() ? 
             "":" ("+getLabel("syntax.default")+".)")));

      System.out.println(getLabelWithValues("syntax.csv_escape", "--csv-escape",
        "--csvescape"));
      System.out.println(getLabelWithValues("syntax.csv_noescape", 
        "--nocsv-escape", "--nocsvescape"));
      System.out.println(getLabelWithValues("syntax.csv_skiplines", "--csv-skiplines"));
      System.out.println(getLabelWithValues("syntax.csv_strictquotes", "--csv-strictquotes"));
      System.out.println(getLabelWithValues("syntax.csv_nostrictquotes", "--nocsv-strictquotes"));
      System.out.println(getLabelWithValues("syntax.csv_encoding", 
        "--csv-encoding", "--csvencoding"));
      System.out.println();
      System.out.println(getLabel("syntax.sql_opts"));
      System.out.println(getLabelWithValues("syntax.sql", "--sql"));
      System.out.println(getLabelWithValues("syntax.merge_sql", "--merge-sql"));
      System.out.println(getLabelWithValues("syntax.sql_db", "--sqldb"));
      System.out.println(getLabelWithValues("syntax.sql_prefix",
        "--sqlprefix", settings.getSqlPrefix()));
      System.out.println(getLabelWithValues("syntax.sql_port",
        "--sqlport", ""+settings.getSqlPort()));
      System.out.println(getLabelWithValues("syntax.sql_host",
        "--sqlhost", settings.getSqlHost()));
      System.out.println(getLabelWithValues("syntax.sql_user", "--sqluser"));
      System.out.println(getLabelWithValues("syntax.sql_password",
        "--sqlpassword"));
      System.out.println(getLabelWithValues("syntax.sql_wipepassword",
        "--wipepassword", 
        (settings.isWipePasswordEnabled()?
           " ("+getLabel("syntax.default")+")":"")));
      System.out.println(getLabelWithValues("syntax.sql_nowipepassword",
        "--nowipepassword", 
        (settings.isWipePasswordEnabled()?
           "":" ("+getLabel("syntax.default")+")")));
      System.out.println(getLabelWithValues("syntax.sql_noconsole",
        "--noconsole-action"));
      System.out.println();

      System.out.println(getLabel("syntax.probsoln_opts"));
      System.out.println(getLabelWithValues("syntax.probsoln", "--probsoln"));
      System.out.println(getLabelWithValues("syntax.merge_probsoln", "--merge-probsoln"));
      System.out.println();

      System.out.println(getLabel("syntax.xls_opts"));
      System.out.println(getLabelWithValues("syntax.xls", "--xls"));
      System.out.println(getLabelWithValues("syntax.merge_xls", "--merge-xls"));
      System.out.println();

      System.out.println(getLabel("syntax.ods_opts"));
      System.out.println(getLabelWithValues("syntax.ods", "--ods"));
      System.out.println(getLabelWithValues("syntax.merge_ods", "--merge-ods"));
      System.out.println();

      System.out.println(getLabel("syntax.xlsods_opts"));
      System.out.println(getLabelWithValues("syntax.sheet", "--sheet",
        settings.getSheetRef()));
      System.out.println();

      System.out.println(getLabelWithValues("syntax.bugreport", 
        "https://github.com/nlct/datatooltk/issues"));
      System.out.println(getLabelWithValues("syntax.homepage", 
        APP_NAME,
        "http://www.dickimaw-books.com/software/datatooltk/"));
   }

   public String getAppInfo()
   {
      return getAppInfo(false);
   }

   public String getAppInfo(boolean html)
   {
      String par = html ? "<p>" : String.format("%n%n");
      String nl = html ? "<br>" : String.format("%n");

      StringBuilder builder = new StringBuilder();

      builder.append(
        getLabelWithValues("about.version", APP_NAME, APP_VERSION, APP_DATE)
      );

      builder.append(nl);

// Copyright line shouldn't get translated (according to
// http://www.gnu.org/prep/standards/standards.html)

      builder.append(String.format(
        "Copyright (C) %s Nicola L. C. Talbot (%s)",
        COPYRIGHT_YEAR, getInfoUrl(html, "www.dickimaw-books.com")));

      builder.append(nl);

      String legalText = getLabel("about.legal");

      if (html)
      {
         legalText = TeXJavaHelpLib.encodeHTML(legalText, false).replaceAll("\n", nl);
      }

      builder.append(legalText);

      TeXJavaHelpLib helpLib = getHelpLib();

      String translator = helpLib.getMessageIfExists("about.translator_info");

      if (translator != null && !translator.isEmpty())
      {
         builder.append(par);

         if (html)
         {
            translator = TeXJavaHelpLib.encodeHTML(translator, false);
         }

         builder.append(translator);
      }

      String ack = helpLib.getMessageIfExists("about.acknowledgements");

      if (ack != null && !ack.isEmpty())
      {
         builder.append(par);

         if (html)
         {
            ack = TeXJavaHelpLib.encodeHTML(ack, false);
         }

         builder.append(ack);
      }

      builder.append(par);
      builder.append(getMessageWithFallback("about.library.version",
        "Bundled with {0} version {1} ({2})",
        "texjavahelplib.jar", TeXJavaHelpLib.VERSION, TeXJavaHelpLib.VERSION_DATE));
      builder.append(nl);

      builder.append(getInfoUrl(html, "https://github.com/nlct/texjavahelplib"));

      builder.append(par);
      builder.append(getMessageWithFallback("about.library.version",
        "Bundled with {0} version {1} ({2})",
        "texparserlib.jar", TeXParser.VERSION, TeXParser.VERSION_DATE));
      builder.append(nl);
      builder.append(getInfoUrl(html, "https://github.com/nlct/texparser"));

      return builder.toString();
   }

   public String getInfoUrl(boolean html, String url)
   {
      if (html)
      {
         String href = url;

         if (!url.startsWith("http"))
         {
            href = "https://"+url;
         }

         return String.format("<a href=\"%s\">%s</a>", 
           HtmlTag.encodeAttributeValue(href, true), 
           TeXJavaHelpLib.encodeHTML(url, false));
      }
      else
      {
         return url;
      }
   }

   public void version()
   {
      System.out.println(getAppInfo());
   }

   public void warning(String message)
   {
      settings.getMessageHandler().warning(message);
   }

   public void debug(String message)
   {
      settings.getMessageHandler().debug(message);
   }

   public void debug(Exception e)
   {
      settings.getMessageHandler().debug(e);
   }

   public TeXJavaHelpLib getHelpLib()
   {
      if (settings != null)
      {
         return settings.getHelpLib();
      }

      return null;
   }

   public String getMessageWithFallback(String label, String fallbackFormat,
     Object... params)
   {
      TeXJavaHelpLib helpLib = getHelpLib();

      if (helpLib == null)
      {
         MessageFormat fmt = new MessageFormat(fallbackFormat);
         return fmt.format(params);
      }
      else
      {
         return helpLib.getMessageWithFallback(label, fallbackFormat, params);
      }
   }

   public String getMessageIfExists(String label, Object... args)
   {
      TeXJavaHelpLib helpLib = getHelpLib();

      if (helpLib == null)
      {
         return null;
      }
      else
      {
         return helpLib.getMessageIfExists(label, args);
      }
   }

   public String getLabelWithAlt(String label, String alt)
   {
      TeXJavaHelpLib helpLib = getHelpLib();

      if (helpLib == null)
      {
         return alt;
      }
      else
      {
         return helpLib.getMessageWithFallback(label, alt);
      }
   }

   public String getLabelRemoveArgs(String parent, String label)
   {
      TeXJavaHelpLib helpLib = getHelpLib();

      if (helpLib == null)
      {
         debug("Dictionary not loaded.");
         return null;
      }

      String propLabel;

      if (parent == null)
      {
         propLabel = label;
      }
      else
      {
         propLabel = String.format("%s.%s", parent, label);
      }

      return helpLib.getMessage(propLabel, "", "", "");
   }

   public String getLabel(String label)
   {
      return getLabel(null, label);
   }

   public String getLabel(String parent, String label)
   {
      TeXJavaHelpLib helpLib = getHelpLib();

      if (helpLib == null)
      {
         debug("Dictionary not loaded.");
         return null;
      }

      String propLabel;

      if (parent == null)
      {
         propLabel = label;
      }
      else
      {
         propLabel = String.format("%s.%s", parent, label);
      }

      return helpLib.getMessage(propLabel);
   }

   public String getToolTip(String label)
   {
      return getToolTip(null, label);
   }

   public String getToolTip(String parent, String label)
   {
      TeXJavaHelpLib helpLib = getHelpLib();

      if (helpLib == null)
      {
         return null;
      }

      String propLabel;

      if (parent == null)
      {
         propLabel = String.format("%s.tooltip", label);
      }
      else
      {
         propLabel = String.format("%s.%s.tooltip", parent, label);
      }

      return helpLib.getMessageIfExists(propLabel);
   }

   @Deprecated
   public char getMnemonic(String label)
   {
      return getMnemonic(null, label);
   }

   @Deprecated
   public char getMnemonic(String parent, String label)
   {
      return (char)getMnemonicInt(parent, label);
   }

   public int getMnemonicInt(String label)
   {
      return getMnemonicInt(null, label);
   }

   public int getMnemonicInt(String parent, String label)
   {
      TeXJavaHelpLib helpLib = getHelpLib();

      if (helpLib == null)
      {
         return -1;
      }

      String propLabel;

      if (parent == null)
      {
         propLabel = String.format("%s.mnemonic", label);
      }
      else
      {
         propLabel = String.format("%s.%s.mnemonic", parent, label);
      }

      String msg = helpLib.getMessageIfExists(propLabel);

      if (msg == null || msg.isEmpty())
      {
         return -1;
      }

      return msg.codePointAt(0);
   }

   public KeyStroke getKeyStroke(String property)
   {
      TeXJavaHelpLib helpLib = getHelpLib();

      if (helpLib == null)
      {
         return null;
      }

      return helpLib.getKeyStroke(property);
   }

   public String getLabelWithValues(String label, Object... values)
   {
      TeXJavaHelpLib helpLib = getHelpLib();

      if (helpLib == null)
      {
         return null;
      }

      return helpLib.getMessage(label, values);
   }

   private void parseArgs(String[] args)
      throws InvalidSyntaxException,IOException
   {
      loadSettings = new LoadSettings(settings);

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--version") || args[i].equals("-v"))
         {
            version();
            System.exit(0);
         }
         else if (args[i].equals("--help") || args[i].equals("-h"))
         {
            help();
            System.exit(0);
         }
         else if (args[i].equals("--sep") || args[i].equals("--csv-sep"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                getLabelWithValues("error.syntax.missing_char", args[i-1]));
            }

            if (args[i].length() > 1)
            {
               throw new InvalidSyntaxException(
                 getLabel("error.syntax.invalid_sep"));
            }

            settings.setSeparator(args[i].charAt(0));
         }
         else if (args[i].equals("--delim") || args[i].equals("--csv-delim"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_char", args[i-1]));
            }

            if (args[i].length() > 1)
            {
               throw new InvalidSyntaxException(
                 getLabel("error.syntax.invalid_delim"));
            }

            settings.setDelimiter(args[i].charAt(0));
         }
         else if (args[i].equals("--csv-header")
                   || args[i].equals("--csvheader"))
         {
            settings.setHasCSVHeader(true);
         }
         else if (args[i].equals("--nocsv-header")
                   || args[i].equals("--nocsvheader"))
         {
            settings.setHasCSVHeader(false);
         }
         else if (args[i].equals("--csv-escape")
                   || args[i].equals("--csvescape"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_char", args[i-1]));
            }

            if (args[i].length() > 1)
            {
               throw new InvalidSyntaxException(
                 getLabel("error.syntax.invalid_esc"));
            }

            settings.setCSVescape(args[i]);
         }
         else if (args[i].equals("--nocsv-escape") 
                  || args[i].equals("--nocsvescape"))
         {
            settings.setCSVescape("");
         }
         else if (args[i].equals("--csv-skiplines"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_number", args[i-1]));
            }

            try
            {
               settings.setCSVskiplines(Integer.parseInt(args[i]));
            }
            catch (Exception e)
            {
               throw new InvalidSyntaxException(
                 getLabel("error.syntax.invalid_skiplines", args[i]), e);
            }
         }
         else if (args[i].equals("--csv-strictquotes"))
         {
            settings.setCSVstrictquotes(true);
         }
         else if (args[i].equals("--nocsv-strictquotes"))
         {
            settings.setCSVstrictquotes(false);
         }
         else if (args[i].equals("--csv-encoding") 
            || args[i].equals("--csvencoding"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_encoding", args[i-1]));
            }

            if (args[i].equals("default") || args[i].isEmpty())
            {
               settings.setCsvEncoding((String)null);
            }
            else
            {
               try
               {
                  if (Charset.isSupported(args[i]))
                  {
                     settings.setCsvEncoding(args[i]);
                  }
                  else
                  {
                    throw new InvalidSyntaxException(
                      getLabelWithValues("error.syntax.unknown.encoding",
                      args[i]));
                  }
               }
               catch (Exception e)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValues("error.syntax.unknown.encoding",
                    args[i]), e);
               }
            }
         }
         else if (args[i].equals("--output") || args[i].equals("-o"))
         {
            if (loadSettings.getOutputFile() != null)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.only_one", args[i]));
            }

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_filename",
                   args[i-1]));
            }

            loadSettings.setOutputFile(args[i]);
         }
         else if (args[i].equals("--csv"))
         {
            if (loadSettings.getImportSource() != null)
            {
               throw new InvalidSyntaxException(
                 getLabel("error.syntax.only_one_import"));
            }

            if (loadSettings.getInputFile() != null)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.import_clash", args[i]));
            }

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_filename",
                     args[i-1]));
            }

            loadSettings.setImportSource(args[i]);
            loadSettings.setDataImport(new DatatoolCsv(settings));
         }
         else if (args[i].equals("--xls"))
         {
            if (loadSettings.getImportSource() != null)
            {
               throw new InvalidSyntaxException(
                 getLabel("error.syntax.only_one_import"));
            }

            if (loadSettings.getInputFile() != null)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.import_clash", args[i]));
            }

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_filename",
                     args[i-1]));
            }

            loadSettings.setImportSource(args[i]);
            loadSettings.setDataImport(new DatatoolExcel(settings));
         }
         else if (args[i].equals("--ods") || args[i].equals("--odf"))
         {
            if (loadSettings.getImportSource() != null)
            {
               throw new InvalidSyntaxException(
                 getLabel("error.syntax.only_one_import"));
            }

            if (loadSettings.getInputFile() != null)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.import_clash", args[i]));
            }

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_filename",
                     args[i-1]));
            }

            loadSettings.setImportSource(args[i]);
            loadSettings.setDataImport(new DatatoolOpenDoc(settings));
         }
         else if (args[i].equals("--sheet"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_sheet_ref",
                     args[i-1]));
            }

            settings.setSheetRef(args[i]);
         }
         else if (args[i].equals("--probsoln"))
         {
            if (loadSettings.getImportSource() != null)
            {
               throw new InvalidSyntaxException(
                 getLabel("error.syntax.only_one_import"));
            }

            if (loadSettings.getInputFile() != null)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.import_clash", args[i]));
            }

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_filename",
                     args[i-1]));
            }

            loadSettings.setImportSource(args[i]);
            loadSettings.setDataImport(new DatatoolProbSoln(settings));
         }
         else if (args[i].equals("--sql"))
         {
            if (loadSettings.getDataImport() != null)
            {
               throw new InvalidSyntaxException(
                 getLabel("error.syntax.only_one_import"));
            }

            if (loadSettings.getInputFile() != null)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.import_clash", args[i]));
            }

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_sql", args[i-1]));
            }

            loadSettings.setImportSource(args[i]);
            loadSettings.setDataImport(new DatatoolSql(settings));
         }
         else if (args[i].equals("--sqldb"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_dbname",
                   args[i-1]));
            }

            settings.setSqlDbName(args[i]);
         }
         else if (args[i].equals("--sqlprefix"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_prefix",
                  args[i-1]));
            }

            settings.setSqlPrefix(args[i]);
         }
         else if (args[i].equals("--sqlhost"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_host",
                   args[i-1]));
            }

            settings.setSqlHost(args[i]);
         }
         else if (args[i].equals("--sqluser"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_user",
                   args[i-1]));
            }

            settings.setSqlUser(args[i]);
         }
         else if (args[i].equals("--sqlpassword"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_password",
                   args[i-1]));
            }

            settings.setSqlPassword(args[i].toCharArray());
            args[i] = "";
         }
         else if (args[i].equals("--wipepassword"))
         {
            settings.setWipePassword(true);
         }
         else if (args[i].equals("--nowipepassword"))
         {
            settings.setWipePassword(false);
         }
         else if (args[i].equals("--noconsole-action"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_noconsole_action",
                    args[i-1]));
            }

            if (args[i].equals("stdin"))
            {
               noConsoleAction = ConsolePasswordReader.NO_CONSOLE_STDIN;
            }
            else if (args[i].equals("gui"))
            {
               noConsoleAction = ConsolePasswordReader.NO_CONSOLE_GUI;
            }
            else if (args[i].equals("error"))
            {
               noConsoleAction = ConsolePasswordReader.NO_CONSOLE_ERROR;
            }
            else
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.invalid_noconsole_action",
                    args[i-1], args[i]));
            }
         }
         else if (args[i].equals("--sqlport"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_port",
                   args[i-1]));
            }

            try
            {
               settings.setSqlPort(Integer.parseInt(args[i]));
            }
            catch (NumberFormatException e)
            {
               throw new InvalidSyntaxException(
                getLabelWithValues("error.syntax.not_a_number",
                  args[i-1], args[i]));
            }
         }
         else if (args[i].equals("--gui") || args[i].equals("-g"))
         {
            settings.setBatchMode(false);
         }
         else if (args[i].equals("--batch") || args[i].equals("-b"))
         {
            settings.setBatchMode(true);
         }
         else if (args[i].equals("--debug"))
         {
            settings.getMessageHandler().setDebugMode(true);
         }
         else if (args[i].equals("--nodebug"))
         {
            settings.getMessageHandler().setDebugMode(false);
         }
         else if (args[i].equals("--debug-mode"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_mode", args[i-1]));
            }
      
            try
            {
               int val = Integer.parseInt(args[i]);
      
               if (val >= 0)
               {
                  settings.getMessageHandler().setTeXParserDebugLevel(val);
               }
            }
            catch (NumberFormatException e)
            {
               try
               {
                  settings.getMessageHandler().setTeXParserDebugLevel(
                    TeXParser.getDebugLevelFromModeList(args[i].split(",")));
               }
               catch (TeXSyntaxException e2)
               {
                  throw new InvalidSyntaxException(
                    e2.getMessage(settings.getMessageHandler().getTeXApp()), e2);
               }
            }

            settings.getMessageHandler().setDebugMode(
             settings.getMessageHandler().getTeXParserDebugLevel() > 0);
         }
         else if (args[i].equals("--log"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_filename",
                   args[i-1]));
            }

            settings.getMessageHandler().setLogFile(args[i]);
         }
         else if (args[i].equals("--nolog"))
         {
            settings.getMessageHandler().setLogFile((File)null);
         }
         else if (args[i].equals("--map-tex-specials"))
         {
            settings.setTeXMapping(true);
         }
         else if (args[i].equals("--nomap-tex-specials"))
         {
            settings.setTeXMapping(false);
         }
         else if (args[i].equals("--auto-trim-labels"))
         {
            settings.setAutoTrimLabels(true);
         }
         else if (args[i].equals("--noauto-trim-labels"))
         {
            settings.setAutoTrimLabels(false);
         }
         else if (args[i].equals("--csv-skip-empty-rows"))
         {
            settings.setSkipEmptyRows(true);
         }
         else if (args[i].equals("--nocsv-skip-empty-rows"))
         {
            settings.setSkipEmptyRows(false);
         }
         else if (args[i].equals("--owner-only"))
         {
            settings.setOwnerOnly(true);
         }
         else if (args[i].equals("--noowner-only"))
         {
            settings.setOwnerOnly(false);
         }
         else if (args[i].equals("--seed"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_number",
                   args[i-1]));
            }

            if (args[i].isEmpty())
            {
               settings.setRandomSeed(null);
            }
            else
            {
               try
               {
                  settings.setRandomSeed(new Long(args[i]));
               }
               catch (NumberFormatException e)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValues("error.syntax.missing_number",
                      args[i-1]));
               }
            }
         }
         else if (args[i].equals("--compat"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_value",
                   args[i-1]));
            }

            if (args[i].equals("latest"))
            {
               settings.setCompatibilityLevel(settings.COMPAT_LATEST);
            }
            else if (args[i].equals("1.6"))
            {
               settings.setCompatibilityLevel(settings.COMPAT_1_6);
            }
            else
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.invalid_compat",
                   args[i]));
            }
         }
         else if (args[i].equals("--shuffle-iterations"))
         {
            // only provided for backward compatibility when using
            // --compat=1.6 (not documented anymore)
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_number",
                   args[i-1]));
            }

            try
            {
               settings.setShuffleIterations(Integer.valueOf(args[i]));
            }
            catch (NumberFormatException e)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_number",
                   args[i-1]));
            }
         }
         else if (args[i].equals("--shuffle"))
         {
            loadSettings.setShuffle(true);
         }
         else if (args[i].equals("--noshuffle"))
         {
            loadSettings.setShuffle(false);
         }
         else if (args[i].equals("--sort-case-sensitive"))
         {
            loadSettings.setCaseSensitive(true);
         }
         else if (args[i].equals("--sort-case-insensitive"))
         {
            loadSettings.setCaseSensitive(false);
         }
         else if (args[i].equals("--sort"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_sort_field",
                  args[i-1]));
            }

            loadSettings.setSort(args[i]);
         }
         else if (args[i].equals("--sort-locale"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_sort_locale",
                  args[i-1]));
            }

            if (args[i].equals("none"))
            {
               settings.setSortLocale((String)null);
            }
            else
            {
               // check argument is valid

               try
               {
                  Locale locale = new Builder().setLanguageTag(args[i]).build();

                  settings.setSortLocale(locale.toLanguageTag());
               }
               catch (Exception e)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValues("error.syntax.invalid_locale",
                     args[i]), e);
               }
            }
         }
         else if (args[i].equals("--tex-encoding"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_encoding",
                  args[i-1]));
            }

            if (args[i].equals("default") || args[i].isEmpty())
            {
               settings.setTeXEncoding((String)null);
            }
            else
            {
               try
               {
                  if (Charset.isSupported(args[i]))
                  {
                     settings.setTeXEncoding(args[i]);
                  }
                  else
                  {
                    throw new InvalidSyntaxException(
                      getLabelWithValues("error.syntax.unknown.encoding",
                      args[i]));
                  }
               }
               catch (Exception e)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValues("error.syntax.unknown.encoding",
                    args[i]), e);
               }
            }
         }
         else if (args[i].equals("--filter-or"))
         {
            loadSettings.setFilterOp(true);
         }
         else if (args[i].equals("--filter-and"))
         {
            loadSettings.setFilterOp(false);
         }
         else if (args[i].equals("--filter-include"))
         {
            loadSettings.setFilterInclude(true);
         }
         else if (args[i].equals("--filter-exclude"))
         {
            loadSettings.setFilterInclude(false);
         }
         else if (args[i].equals("--filter"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_filter_label",
                   args[i-1]));
            }

            String label = args[i];

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_filter_operator",
                   args[i-2], args[i-1]));
            }

            String operator = args[i];

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_filter_value",
                   args[i-3], args[i-2], args[i-1]));
            }

            String value = args[i];

            loadSettings.addFilterInfo(new FilterInfo(getMessageHandler(),
              label, operator, value));
         }
         else if (args[i].equals("--truncate"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_number",
                   args[i-1]));
            }

            try
            {
               loadSettings.setTruncate(Integer.parseInt(args[i]));
            }
            catch (NumberFormatException e)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.not_a_number",
                  args[i-1], args[i]));
            }
         }
         else if (args[i].equals("--remove-columns"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_arg",
                   args[i-1]));
            }

            if (loadSettings.getRemoveExceptColumnList() != null)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.option_clash",
                   args[i-1], "--remove-except-columns"));
            }

            loadSettings.setRemoveColumnList(args[i]);
         }
         else if (args[i].equals("--remove-except-columns"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_arg",
                   args[i-1]));
            }

            if (loadSettings.getRemoveColumnList() != null)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.option_clash",
                   args[i-1], "--remove-columns"));
            }

            loadSettings.setRemoveExceptColumnList(args[i]);
         }
         else if (args[i].equals("--merge"))
         {
            if (loadSettings.getMergeImportSource() != null
                || loadSettings.getMergeFile() != null)
            {
               throw new InvalidSyntaxException(
                  getLabel("error.syntax.only_one_merge"));
            }

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_merge_key",
                   args[i-1]));
            }

            String mergeKey = args[i];

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_merge_file",
                   args[i-2], args[i-1]));
            }

            File mergeFile = new File(args[i]);

            if (!mergeFile.exists())
            {
               System.err.println(
                 getLabelWithValues("error.io.file_not_found",
                  args[i]));
               mergeFile = null;
               mergeKey = null;
            }

            loadSettings.setMergeKey(mergeKey);
            loadSettings.setMergeFile(mergeFile);
         }
         else if (args[i].equals("--merge-csv"))
         {
            if (loadSettings.getMergeImportSource() != null
                || loadSettings.getMergeFile() != null)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.only_one", args[i]));
            }

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_merge_key",
                   args[i-1]));
            }

            String mergeKey = args[i];

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_merge_file",
                     args[i-2], args[i-1]));
            }

            loadSettings.setMergeKey(mergeKey);
            loadSettings.setMergeImportSource(args[i]);
            loadSettings.setMergeImport(new DatatoolCsv(settings));
         }
         else if (args[i].equals("--merge-sql"))
         {
            if (loadSettings.getMergeImportSource() != null
                || loadSettings.getMergeFile() != null)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.only_one", args[i]));
            }

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_merge_key",
                   args[i-1]));
            }

            String mergeKey = args[i];

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_merge_statement",
                     args[i-2], args[i-1]));
            }

            loadSettings.setMergeKey(mergeKey);
            loadSettings.setMergeImportSource(args[i]);
            loadSettings.setMergeImport(new DatatoolSql(settings));
         }
         else if (args[i].equals("--merge-probsoln"))
         {
            if (loadSettings.getMergeImportSource() != null
                || loadSettings.getMergeFile() != null)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.only_one", args[i]));
            }

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_merge_key",
                   args[i-1]));
            }

            String mergeKey = args[i];

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_merge_file",
                     args[i-2], args[i-1]));
            }

            loadSettings.setMergeKey(mergeKey);
            loadSettings.setMergeImportSource(args[i]);
            loadSettings.setMergeImport(new DatatoolProbSoln(settings));
         }
         else if (args[i].equals("--merge-xls"))
         {
            if (loadSettings.getMergeImportSource() != null
                || loadSettings.getMergeFile() != null)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.only_one", args[i]));
            }

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_merge_key",
                   args[i-1]));
            }

            String mergeKey = args[i];

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_merge_file",
                     args[i-2], args[i-1]));
            }

            loadSettings.setMergeKey(mergeKey);
            loadSettings.setMergeImportSource(args[i]);
            loadSettings.setMergeImport(new DatatoolExcel(settings));
         }
         else if (args[i].equals("--merge-ods"))
         {
            if (loadSettings.getMergeImportSource() != null
                || loadSettings.getMergeFile() != null)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.only_one", args[i]));
            }

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_merge_key",
                   args[i-1]));
            }

            String mergeKey = args[i];

            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                  getLabelWithValues("error.syntax.missing_merge_file",
                     args[i-2], args[i-1]));
            }

            loadSettings.setMergeKey(mergeKey);
            loadSettings.setMergeImportSource(args[i]);
            loadSettings.setMergeImport(new DatatoolOpenDoc(settings));
         }
         else if (args[i].equals("--name"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_dbname",
                   args[i-1]));
            }

            loadSettings.setDbName(args[i]);
         }
         else if (args[i].equals("--in") || args[i].equals("-i"))
         {
            i++;

            if (i == args.length)
            {
               throw new InvalidSyntaxException(
                 getLabelWithValues("error.syntax.missing_input",
                   args[i-1]));
            }

            if (loadSettings.getInputFile() != null)
            {
               throw new InvalidSyntaxException(
                 getLabel("error.syntax.only_one_input"));
            }

            if (loadSettings.getDataImport() != null)
            {
               throw new InvalidSyntaxException(
                 getLabel("error.syntax.input_clash"));
            }

            loadSettings.setInputFile(args[i]);
         }
         else if (args[i].equals("--verbose"))
         {
            getMessageHandler().setVerbosity(1);
         }
         else if (args[i].equals("--noverbose"))
         {
            getMessageHandler().setVerbosity(0);
         }
         else if (args[i].charAt(0) == '-')
         {
            throw new InvalidSyntaxException(
             getLabelWithValues("error.syntax.unknown_option",
               args[i]));
         }
         else
         {
            // if no option specified, assume --in

            if (loadSettings.getInputFile() != null)
            {
               throw new InvalidSyntaxException(
                 getLabel("error.syntax.only_one_input"));
            }

            if (loadSettings.getDataImport() != null)
            {
               throw new InvalidSyntaxException(
                 getLabel("error.syntax.input_clash"));
            }

            loadSettings.setInputFile(args[i]);
         }
      }
   }

   private void process()
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
                    String.format("%s: Fatal I/O error: %s", APP_NAME, e.getMessage()),
                    "Error", JOptionPane.ERROR_MESSAGE);
                   e.printStackTrace();
                   exit(EXIT_IO);
                }
             }
          });
      } 
   }

   public static void exit(DatatoolTk datatooltk, int exitCode,
     Throwable e, String noAppMessage, String appMessage, int msgCode,
     boolean stackTrace)
   {
      if (datatooltk == null)
      {
         System.err.format("%s: %s: %s", APP_NAME, noAppMessage, e.getMessage());

         if (stackTrace)
         {
            e.printStackTrace();
         }

         System.exit(exitCode);
      }
      else
      {
         datatooltk.getMessageHandler().error(null, appMessage, e, msgCode);

         datatooltk.exit(exitCode);
      }
   }

   public void exit(int exitCode)
   {
      getMessageHandler().closeLogFile();
      System.exit(exitCode);
   }

   public static void main(String[] args)
   {
      DatatoolTk datatooltk = null;

      try
      {
         datatooltk = new DatatoolTk();
         datatooltk.parseArgs(args);
         datatooltk.process();
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

   public static final String APP_NAME = "datatooltk";
   public static final String APP_VERSION = "1.9.20240623";
   public static final String APP_DATE = "2024-06-23";
   public static final String START_COPYRIGHT_YEAR = "2014";
   public static final String COPYRIGHT_YEAR
    = START_COPYRIGHT_YEAR+"-"+APP_DATE.substring(0,4);

   private LoadSettings loadSettings;

   private int noConsoleAction = ConsolePasswordReader.NO_CONSOLE_GUI;

   private String dict = null;

   private DatatoolSettings settings;

   public static final int EXIT_SYNTAX=1, EXIT_IO=2, EXIT_USER_FORCED=3,
    EXIT_OTHER=255;
}
