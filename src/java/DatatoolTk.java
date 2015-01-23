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
package com.dickimawbooks.datatooltk;

import java.io.*;
import java.util.Properties;
import java.util.Vector;
import java.awt.Cursor;
import java.net.URISyntaxException;
import java.net.URL;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import com.dickimawbooks.datatooltk.io.*;
import com.dickimawbooks.datatooltk.gui.DatatoolGuiResources;
import com.dickimawbooks.datatooltk.gui.DatatoolGUI;
import com.dickimawbooks.datatooltk.gui.DatatoolDbPanel;

/**
 * Main application class.
 */
public class DatatoolTk
{
   public static void doBatchProcess()
   {
      settings.setPasswordReader(new ConsolePasswordReader(noConsoleAction));
      settings.setErrorHandler(new ErrorHandler()
      {
         public void error(SAXParseException exception)
         {
            System.err.println(appName+": "+exception.getMessage());
         }

         public void warning(SAXParseException exception)
         {
            System.err.println(appName+": "+exception.getMessage());
         }

         public void fatalError(SAXParseException exception)
         {
            exception.printStackTrace();
         }
      });

      if (imp == null && dbtex == null)
      {
         System.err.println(appName+": "+getLabelWithValues("error.cli.no_data",
           "--gui", "--help"));
         System.exit(1);
      }

      if (out == null)
      {
         System.err.println(appName+": "+getLabelWithValues("error.cli.no_out",
           new String[]{"--output", "--gui", "--help"}));
         System.exit(1);
      }

      DatatoolDb db = null;

      try
      {
         if (dbtex != null)
         {
            debug("Loading '"+dbtex+"'");
            db = DatatoolDb.load(settings, dbtex);
         }
         else
         {
            debug("Importing data via '"+source+"'");
            db = imp.importData(source);
         }

         if (dbname != null)
         {
            debug("Setting name to '"+dbname+"'");
            db.setName(dbname);
         }

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

         if (!(sort == null || sort.isEmpty()))
         {
            debug("sorting");

            db.setSortCaseSensitive(isCaseSensitive);

            boolean ascending = true;

            char c = sort.charAt(0);

            if (c == '+')
            {
               ascending = true;
               sort = sort.substring(1);
            }
            else if (c == '-')
            {
               ascending = false;
               sort = sort.substring(1);
            }

            int colIndex = db.getColumnIndex(sort);

            if (colIndex == -1)
            {
               throw new InvalidSyntaxException(
                  appName+": "+
                  DatatoolTk.getLabelWithValue("error.syntax.unknown_field",
                  sort));
            }

            db.setSortColumn(colIndex);
            db.setSortAscending(ascending);
            db.sort();
         }

         if (doShuffle)
         {
            debug("Shuffling");
            db.shuffle();
         }

         if (filterInfo != null)
         {
            debug("Filtering");
            DataFilter filter = new DataFilter(db, filterOr);

            filter.addFilters(filterInfo);

            if (filterInclude)
            {
               db.removeNonMatching(filter);
            }
            else
            {
               db.removeMatching(filter);
            }
         }

         if (truncate > -1)
         {
            db.truncate(truncate);
         }

         debug("Saving '"+out+"'");
         db.save(out);
      }
      catch (DatatoolImportException e)
      {
         System.err.println(appName+": "+e.getMessage());

         Throwable cause = e.getCause();

         if (cause != null)
         {
            System.err.println(cause.getMessage());
         }

         System.exit(1);
      }
      catch (IOException e)
      {
         System.err.println(appName+": "+e.getMessage());
         System.exit(1);
      }
      catch (Throwable e)
      {
         System.err.println(appName+": "+e.getMessage());

         Throwable cause = e.getCause();

         if (cause != null)
         {
            System.err.println(cause.getMessage());
         }

         System.exit(1);
      }

      debug("Completed");
      System.exit(0);
   }

   public static void createAndShowGUI()
   {
      DatatoolGUI gui = new DatatoolGUI(settings);

      settings.setErrorHandler(new ErrorHandler()
      {
         public void error(SAXParseException exception)
         {
            DatatoolGuiResources.error(null, exception);
         }

         public void warning(SAXParseException exception)
         {
            DatatoolGuiResources.warning(null, exception.getMessage());
         }

         public void fatalError(SAXParseException exception)
         {
            DatatoolGuiResources.error(null, exception);
         }
      });

      gui.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

      DatatoolDb db = null;

      if (dbtex != null)
      {
         db = gui.load(dbtex);
      }
      else if (imp != null)
      {
         db = gui.importData(imp, source);
      }

      if (db == null)
      {
         if (mergeFile != null)
         {
            debug("Loading '"+mergeFile+"'");
            db = gui.load(mergeFile);
            mergeFile = null;
            mergeKey = null;
         }
         else if (mergeImportSource != null)
         {
            debug("Importing via '"+mergeImportSource+"'");
            db = gui.importData(mergeImport, mergeImportSource);
            mergeImportSource = null;
            mergeKey = null;
         }
      }

      if (db != null)
      {
         DatatoolDbPanel panel = gui.getPanel(db);
         boolean modified = false;

         if (dbname != null)
         {
            db.setName(dbname);
            modified = true;
         }

         if (mergeFile != null)
         {
            debug("Loading '"+mergeFile+"'");

            try
            {
               DatatoolDb mergeDb = DatatoolDb.load(settings, mergeFile);
               panel.merge(mergeDb, mergeKey);
            }
            catch (Exception e)
            {
               DatatoolGuiResources.warning(null, e.getMessage());
            }

            modified = true;
         }
         else if (mergeImportSource != null)
         {
            debug("Importing via '"+mergeImportSource+"'");

            try
            {
               DatatoolDb mergeDb = mergeImport.importData(mergeImportSource);
               panel.merge(mergeDb, mergeKey);
            }
            catch (Exception e)
            {
               DatatoolGuiResources.warning(null, e.getMessage());
            }

            modified = true;
         }

         if (!(sort == null || sort.isEmpty()))
         {
            db.setSortCaseSensitive(isCaseSensitive);

            boolean ascending = true;

            char c = sort.charAt(0);

            if (c == '+')
            {
               ascending = true;
               sort = sort.substring(1);
            }
            else if (c == '-')
            {
               ascending = false;
               sort = sort.substring(1);
            }

            int colIndex = db.getColumnIndex(sort);

            if (colIndex == -1)
            {
               DatatoolGuiResources.error(null,
                  DatatoolTk.getLabelWithValue("error.syntax.unknown_field",
                  sort));
            }
            else
            {
               db.setSortColumn(colIndex);
               db.setSortAscending(ascending);
               db.sort();
            }

            modified = true;
         }

         if (doShuffle)
         {
            db.shuffle();
            modified = true;
         }

         if (filterInfo != null)
         {
            debug("Filtering");
            DataFilter filter = new DataFilter(db, filterOr);
            filter.addFilters(filterInfo);

            if (filterInclude)
            {
               db.removeNonMatching(filter);
            }
            else
            {
               db.removeMatching(filter);
            }
            modified = true;
         }

         if (truncate > -1)
         {
            db.truncate(truncate);
            modified = true;
         }

         if (modified)
         {
            panel.setModified(modified);
         }
      }

      gui.setCursor(Cursor.getDefaultCursor());
      gui.setVisible(true);
   }

   public static void help()
   {
      version();
      System.out.println();
      System.out.println(getLabel("syntax.title"));
      System.out.println();
      System.out.println(appName+" --gui");
      System.out.println(getLabel("syntax.or"));
      System.out.println(getLabelWithValue("syntax.opt_db", appName));
      System.out.println(getLabel("syntax.or"));
      System.out.println(getLabelWithValue("syntax.opt_csv", appName));
      System.out.println(getLabel("syntax.or"));
      System.out.println(getLabelWithValue("syntax.opt_sql", appName));
      System.out.println();

      System.out.println(getLabel("syntax.general"));
      System.out.println(getLabelWithValues("syntax.gui", "--gui", "-g"));
      System.out.println(getLabelWithValues("syntax.batch", "--batch", "-b"));
      System.out.println(getLabelWithValues("syntax.in", 
        new String[]{"--in", "-i", appName}));
      System.out.println(getLabelWithValue("syntax.name", "--name"));
      System.out.println(getLabelWithValues("syntax.out", "--output", "-o"));
      System.out.println(getLabelWithValues("syntax.version", "--version", "-v"));
      System.out.println(getLabelWithValues("syntax.help", "--help", "-h"));
      System.out.println(getLabelWithValue("syntax.debug", "--debug"));
      System.out.println(getLabelWithValue("syntax.nodebug", "--nodebug"));
      System.out.println(getLabelWithValue("syntax.deletetmpfiles", "--delete-tmp-files"));
      System.out.println(getLabelWithValue("syntax.nodeletetmpfiles", "--nodelete-tmp-files"));
      System.out.println(getLabelWithValues("syntax.maptexspecials", "--map-tex-specials", (settings.isTeXMappingOn()?" ("+getLabel("syntax.default")+".)":"")));
      System.out.println(getLabelWithValues("syntax.nomaptexspecials",
         "--nomap-tex-specials",
         (settings.isTeXMappingOn() ?
         "": " ("+getLabel("syntax.default")+".)")));
      System.out.println(getLabelWithValue("syntax.seed", "--seed"));
      System.out.println(getLabelWithValue("syntax.shuffle_iter", "--shuffle-iterations"));
      System.out.println(getLabelWithValue("syntax.shuffle", "--shuffle"));
      System.out.println(getLabelWithValue("syntax.no_shuffle", "--noshuffle"));
      System.out.println(getLabelWithValue("syntax.sort", "--sort"));
      System.out.println(getLabelWithValue("syntax.sort_case_sensitive",
         "--sort-case-sensitive"));
      System.out.println(getLabelWithValue("syntax.sort_case_insensitive",
         "--sort-case-insensitive"));
      System.out.println(getLabelWithValues("syntax.owner_only",
         "--owner_only", (settings.isOwnerOnly() ?
         " ("+getLabel("syntax.default")+".)" : "")));
      System.out.println(getLabelWithValues("syntax.noowner_only",
         "--noowner_only", (settings.isOwnerOnly() ?
         "" : " ("+getLabel("syntax.default")+".)")));
      System.out.println(getLabelWithValue("syntax.truncate",
         "--truncate"));
      System.out.println(getLabelWithValue("syntax.filter_or",
         "--filter-or"));
      System.out.println(getLabelWithValue("syntax.filter_and",
         "--filter-and"));
      System.out.println(getLabelWithValue("syntax.filter_include",
         "--filter-include"));
      System.out.println(getLabelWithValue("syntax.filter_exclude",
         "--filter-exclude"));
      System.out.println(getLabelWithValue("syntax.filter",
         "--filter"));
      System.out.println(getLabelWithValue("syntax.merge",
         "--merge"));
      System.out.println();

      System.out.println(getLabel("syntax.csv_opts"));
      System.out.println(getLabelWithValue("syntax.csv", "--csv"));
      System.out.println(getLabelWithValue("syntax.merge_csv", "--merge-csv"));
      System.out.println(getLabelWithValues("syntax.csv_sep", "--sep", 
        ""+settings.getSeparator()));
      System.out.println(getLabelWithValues("syntax.csv_delim", "--delim", 
        ""+settings.getDelimiter()));
      System.out.println(getLabelWithValues("syntax.csv_header",
        "--csvheader",
        (settings.hasCSVHeader()?" ("+getLabel("syntax.default")+".)":"")));
      System.out.println(getLabelWithValues("syntax.csv_noheader",
        "--nocsvheader",
        (settings.hasCSVHeader()? "" : " ("+getLabel("syntax.default")+".)")));
      System.out.println();
      System.out.println(getLabel("syntax.sql_opts"));
      System.out.println(getLabelWithValue("syntax.sql", "--sql"));
      System.out.println(getLabelWithValue("syntax.merge_sql", "--merge-sql"));
      System.out.println(getLabelWithValue("syntax.sql_db", "--sqldb"));
      System.out.println(getLabelWithValues("syntax.sql_prefix",
        "--sqlprefix", settings.getSqlPrefix()));
      System.out.println(getLabelWithValues("syntax.sql_port",
        "--sqlport", ""+settings.getSqlPort()));
      System.out.println(getLabelWithValues("syntax.sql_host",
        "--sqlhost", settings.getSqlHost()));
      System.out.println(getLabelWithValue("syntax.sql_user", "--sqluser"));
      System.out.println(getLabelWithValue("syntax.sql_password",
        "--sqlpassword"));
      System.out.println(getLabelWithValues("syntax.sql_wipepassword",
        "--wipepassword", 
        (settings.isWipePasswordEnabled()?
           " ("+getLabel("syntax.default")+")":"")));
      System.out.println(getLabelWithValues("syntax.sql_nowipepassword",
        "--nowipepassword", 
        (settings.isWipePasswordEnabled()?
           "":" ("+getLabel("syntax.default")+")")));
      System.out.println(getLabelWithValue("syntax.sql_noconsole",
        "--noconsole-action"));
      System.out.println();

      System.out.println(getLabel("syntax.probsoln_opts"));
      System.out.println(getLabelWithValue("syntax.probsoln", "--probsoln"));
      System.out.println(getLabelWithValue("syntax.merge_probsoln", "--merge-probsoln"));
      System.out.println();

      System.out.println(getLabel("syntax.xls_opts"));
      System.out.println(getLabelWithValue("syntax.xls", "--xls"));
      System.out.println(getLabelWithValue("syntax.merge_xls", "--merge-xls"));
      System.out.println();

      System.out.println(getLabel("syntax.ods_opts"));
      System.out.println(getLabelWithValue("syntax.ods", "--ods"));
      System.out.println(getLabelWithValue("syntax.merge_ods", "--merge-ods"));
      System.out.println();

      System.out.println(getLabel("syntax.xlsods_opts"));
      System.out.println(getLabelWithValues("syntax.sheet", "--sheet",
        settings.getSheetRef()));
      System.out.println();

      System.out.println(getLabelWithValue("syntax.bugreport", 
        "http://www.dickimaw-books.com/bug-report.html"));
      System.out.println(getLabelWithValues("syntax.homepage", 
        appName,
        "http://www.dickimaw-books.com/apps/datatooltk/"));
   }

   public static String getAppInfo()
   {
      String eol = System.getProperty("line.separator", "\n");

      String info = getLabelWithValues("about.version",
        new String[]{ appName, appVersion, appDate})
        + eol
// Copyright line shouldn't get translated (according to
// http://www.gnu.org/prep/standards/standards.html)
        + "Copyright (C) 2013 Nicola L. C. Talbot (www.dickimaw-books.com)"
        + eol
        + getLabel("about.legal");

      String translator = dictionary.getProperty("about.translator_info");

      if (translator != null && !translator.isEmpty())
      {
         info += eol + translator;
      }

      String ack = dictionary.getProperty("about.acknowledgements");

      if (ack != null && !ack.isEmpty())
      {
         ack += eol + eol + ack;
      }

      return info;
   }

   public static void version()
   {
      System.out.println(getAppInfo());
   }

   public static void warning(String message)
   {
      if (guiMode)
      {
         DatatoolGuiResources.warning(null, message);
      }
      else
      {
         System.err.println(appName+": "+message);
      }
   }

   public static void debug(String message)
   {
      if (debugMode)
      {
         System.err.println(appName+": "+message);
      }
   }

   public static void debug(Exception e)
   {
      if (debugMode)
      {
         System.err.println(appName+":");
         e.printStackTrace();
      }
   }

   public static String getDictionary()
   {
      return dict;
   }

   public static URL getDictionaryUrl()
   {
      return DatatoolTk.class.getResource(dict);
   }

   public static void loadDictionary()
      throws IOException
   {
      String dictLanguage = settings.getDictionary();

      InputStream in = null;
      BufferedReader reader = null;

      try
      {
         dict = settings.getDictionaryLocation()+"-"
             + settings.getDictionary()+".prop";

         in = DatatoolTk.class.getResourceAsStream(dict);

         if (in == null)
         {
            throw new FileNotFoundException
            (
               "Can't find dictionary resource file " +dict
            );
         }

         reader = new BufferedReader(new InputStreamReader(in));

         dictionary = new Properties();
         dictionary.load(reader);
      }
      finally
      {
         if (reader != null)
         {
            reader.close();
         }

         if (in != null)
         {
            in.close();
         }
      }
   }

   public static String getLabelWithAlt(String label, String alt)
   {
      if (dictionary == null) return alt;

      String prop = dictionary.getProperty(label);

      if (prop == null)
      {
         return alt;
      }

      return prop;
   }

   public static String getLabelRemoveArgs(String parent, String label)
   {
      return getLabel(parent, label).replaceAll("\\$[0-9]", "");
   }

   public static String getLabel(String label)
   {
      return getLabel(null, label);
   }

   public static String getLabel(String parent, String label)
   {
      if (parent != null)
      {
         label = parent+"."+label;
      }

      String prop = dictionary.getProperty(label);

      if (prop == null)
      {
         System.err.println(appName+": no such dictionary property '"+label+"'");
         return "?"+label+"?";
      }

      return prop;
   }

   public static String getToolTip(String label)
   {
      return getToolTip(null, label);
   }

   public static String getToolTip(String parent, String label)
   {
      if (parent != null)
      {
         label = parent+"."+label;
      }

      return dictionary.getProperty(label+".tooltip");
   }

   public static char getMnemonic(String label)
   {
      return getMnemonic(null, label);
   }

   public static char getMnemonic(String parent, String label)
   {
      String prop = getLabel(parent, label+".mnemonic");

      if (prop.equals(""))
      {
         debug("empty dictionary property '"+prop+"'");
         return label.charAt(0);
      }

      return prop.charAt(0);
   }

   public static int getMnemonicInt(String label)
   {
      return getMnemonicInt(null, label);
   }

   public static int getMnemonicInt(String parent, String label)
   {
      String prop;

      if (parent == null)
      {
         prop = dictionary.getProperty(label+".mnemonic");
      }
      else
      {
         prop = dictionary.getProperty(parent+"."+label+".mnemonic");
      }

      if (prop == null || prop.isEmpty())
      {
         return -1;
      }

      return prop.codePointAt(0);
   }

   public static String getLabelWithValue(String label, String value)
   {
      String prop = getLabel(label);

      if (prop == null)
      {
         return null;
      }

      if (value == null)
      {
         value = "";
      }

      int n = prop.length();

      StringBuffer buffer = new StringBuffer(n);

      for (int i = 0; i < n; i++)
      {
         int c = prop.codePointAt(i);

         if (c == (int)'\\' && i != n-1)
         {
            buffer.appendCodePoint(prop.codePointAt(++i));
         }
         else if (c == (int)'$' && i != n-1)
         {
            c = prop.codePointAt(i+1);

            if (c == (int)'1')
            {
               buffer.append(value);
               i++;
            }
         }
         else
         {
            buffer.appendCodePoint(c);
         }
      }

      return new String(buffer);
   }

   public static String getLabelWithValue(String label, int value)
   {
      return getLabelWithValue(label, ""+value);
   }

   public static String getLabelWithValues(String label, int value1,
      String value2)
   {
      return getLabelWithValues(label, new String[] {""+value1, value2});
   }

   public static String getLabelWithValues(String label, String value1,
      String value2)
   {
      return getLabelWithValues(label, new String[] {value1, value2});
   }

   // Only works for up to nine values.

   public static String getLabelWithValues(String label, String[] values)
   {
      String prop = getLabel(label);

      if (prop == null)
      {
         return prop;
      }

      int n = prop.length();

      StringBuffer buffer = new StringBuffer(n);

      for (int i = 0; i < n; i++)
      {
         int c = prop.codePointAt(i);

         if (c == (int)'\\' && i != n-1)
         {
            buffer.appendCodePoint(prop.codePointAt(++i));
         }
         else if (c == (int)'$' && i != n-1)
         {
            c = prop.codePointAt(i+1);

            if (c >= 48 && c <= 57)
            {
               // Digit

               int index = c - 48 - 1;

               if (index >= 0 && index < values.length)
               {
                  buffer.append(values[index]);
               }

               i++;
            }
            else
            {
               buffer.append('$');
            }
         }
         else
         {
            buffer.appendCodePoint(c);
         }
      }

      return new String(buffer);
   }

   public static void removeFileOnExit(File file)
   {
      if (removeTmpFilesOnExit)
      {
         file.deleteOnExit();
      }
   }

   public static void main(String[] args)
   {
      settings = new DatatoolSettings();

      guiMode = false;

      try
      {
         settings.loadProperties();
      }
      catch (IOException e)
      {
         System.err.println(appName+": unable to load properties:\n" +
           e.getMessage());
      }

      try
      {
         loadDictionary();
      }
      catch (IOException e)
      {
         System.err.println(appName+": unable to load dictionary file:\n"
           + e.getMessage());
      }

      try
      {
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
            else if (args[i].equals("--sep"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                   getLabelWithValue("error.syntax.missing_char", args[i-1]));
               }

               if (args[i].length() > 1)
               {
                  throw new InvalidSyntaxException(
                    getLabel("error.syntax.invalid_sep"));
               }

               settings.setSeparator(args[i].charAt(0));
            }
            else if (args[i].equals("--delim"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_char", args[i-1]));
               }

               if (args[i].length() > 1)
               {
                  throw new InvalidSyntaxException(
                    getLabel("error.syntax.invalid_delim"));
               }

               settings.setDelimiter(args[i].charAt(0));
            }
            else if (args[i].equals("--csvheader"))
            {
               settings.setHasCSVHeader(true);
            }
            else if (args[i].equals("--nocsvheader"))
            {
               settings.setHasCSVHeader(false);
            }
            else if (args[i].equals("--output") || args[i].equals("-o"))
            {
               if (out != null)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.only_one", args[i]));
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_filename",
                      args[i-1]));
               }

               out = args[i];
            }
            else if (args[i].equals("--csv"))
            {
               if (source != null)
               {
                  throw new InvalidSyntaxException(
                    getLabel("error.syntax.only_one_import"));
               }

               if (dbtex != null)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.import_clash", args[i]));
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValue("error.syntax.missing_filename",
                        args[i-1]));
               }

               source = args[i];
               imp = new DatatoolCsv(settings);
            }
            else if (args[i].equals("--xls"))
            {
               if (source != null)
               {
                  throw new InvalidSyntaxException(
                    getLabel("error.syntax.only_one_import"));
               }

               if (dbtex != null)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.import_clash", args[i]));
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValue("error.syntax.missing_filename",
                        args[i-1]));
               }

               source = args[i];
               imp = new DatatoolExcel(settings);
            }
            else if (args[i].equals("--ods") || args[i].equals("--odf"))
            {
               if (source != null)
               {
                  throw new InvalidSyntaxException(
                    getLabel("error.syntax.only_one_import"));
               }

               if (dbtex != null)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.import_clash", args[i]));
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValue("error.syntax.missing_filename",
                        args[i-1]));
               }

               source = args[i];
               imp = new DatatoolOpenDoc(settings);
            }
            else if (args[i].equals("--sheet"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValue("error.syntax.missing_sheet_ref",
                        args[i-1]));
               }

               settings.setSheetRef(args[i]);
            }
            else if (args[i].equals("--probsoln"))
            {
               if (source != null)
               {
                  throw new InvalidSyntaxException(
                    getLabel("error.syntax.only_one_import"));
               }

               if (dbtex != null)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.import_clash", args[i]));
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValue("error.syntax.missing_filename",
                        args[i-1]));
               }

               source = args[i];
               imp = new DatatoolProbSoln(settings);
            }
            else if (args[i].equals("--sql"))
            {
               if (imp != null)
               {
                  throw new InvalidSyntaxException(
                    getLabel("error.syntax.only_one_import"));
               }

               if (dbtex != null)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.import_clash", args[i]));
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_sql", args[i-1]));
               }

               source = args[i];

               imp = new DatatoolSql(settings);
            }
            else if (args[i].equals("--sqldb"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_dbname",
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
                    getLabelWithValue("error.syntax.missing_prefix",
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
                    getLabelWithValue("error.syntax.missing_host",
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
                    getLabelWithValue("error.syntax.missing_user",
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
                    getLabelWithValue("error.syntax.missing_password",
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
                     getLabelWithValue("error.syntax.missing_noconsole_action",
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
                    getLabelWithValue("error.syntax.missing_port",
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
               guiMode = true;
            }
            else if (args[i].equals("--batch") || args[i].equals("-b"))
            {
               guiMode = false;
            }
            else if (args[i].equals("--debug"))
            {
               debugMode = true;
            }
            else if (args[i].equals("--nodebug"))
            {
               debugMode = false;
            }
            else if (args[i].equals("--delete-tmp-files"))
            {
               removeTmpFilesOnExit = true;
            }
            else if (args[i].equals("--nodelete-tmp-files"))
            {
               removeTmpFilesOnExit = false;
            }
            else if (args[i].equals("--map-tex-specials"))
            {
               settings.setTeXMapping(true);
            }
            else if (args[i].equals("--nomap-tex-specials"))
            {
               settings.setTeXMapping(false);
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
                    getLabelWithValue("error.syntax.missing_number",
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
                       getLabelWithValue("error.syntax.missing_number",
                         args[i-1]));
                  }
               }
            }
            else if (args[i].equals("--shuffle-iterations"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_number",
                      args[i-1]));
               }

               try
               {
                  settings.setShuffleIterations(new Integer(args[i]));
               }
               catch (NumberFormatException e)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_number",
                      args[i-1]));
               }
            }
            else if (args[i].equals("--shuffle"))
            {
               doShuffle = true;
            }
            else if (args[i].equals("--noshuffle"))
            {
               doShuffle = false;
            }
            else if (args[i].equals("--sort-case-sensitive"))
            {
               isCaseSensitive = true;
            }
            else if (args[i].equals("--sort-case-insensitive"))
            {
               isCaseSensitive = false;
            }
            else if (args[i].equals("--sort"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValue("error.syntax.missing_sort_field",
                     args[i-1]));
               }

               sort = args[i];
            }
            else if (args[i].equals("--filter-or"))
            {
               filterOr = true;
            }
            else if (args[i].equals("--filter-and"))
            {
               filterOr = false;
            }
            else if (args[i].equals("--filter-include"))
            {
               filterInclude = true;
            }
            else if (args[i].equals("--filter-exclude"))
            {
               filterInclude = false;
            }
            else if (args[i].equals("--filter"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_filter_label",
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
                      new String[] {args[i-3], args[i-2], args[i-1]}));
               }

               String value = args[i];

               if (filterInfo == null)
               {
                  filterInfo = new Vector<FilterInfo>();
               }

               filterInfo.add(new FilterInfo(label, operator, value));
            }
            else if (args[i].equals("--truncate"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_number",
                      args[i-1]));
               }

               try
               {
                  truncate = Integer.parseInt(args[i]);
               }
               catch (NumberFormatException e)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValues("error.syntax.not_a_number",
                     args[i-1], args[i]));
               }
            }
            else if (args[i].equals("--merge"))
            {
               if (mergeFile != null || mergeImportSource != null)
               {
                  throw new InvalidSyntaxException(
                     getLabel("error.syntax.only_one_merge"));
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_merge_key",
                      args[i-1]));
               }

               mergeKey = args[i];

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValues("error.syntax.missing_merge_file",
                      args[i-2], args[i-1]));
               }

               mergeFile = new File(args[i]);

               if (!mergeFile.exists())
               {
                  System.err.println(
                    getLabelWithValue("error.io.file_not_found",
                     args[i]));
                  mergeFile = null;
                  mergeKey = null;
               }
            }
            else if (args[i].equals("--merge-csv"))
            {
               if (mergeImportSource != null || mergeFile != null)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValue("error.syntax.only_one", args[i]));
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_merge_key",
                      args[i-1]));
               }

               mergeKey = args[i];

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValues("error.syntax.missing_merge_file",
                        args[i-2], args[i-1]));
               }

               File file = new File(args[i]);

               if (!file.exists())
               {
                  System.err.println(
                    getLabelWithValue("error.io.file_not_found",
                     args[i]));
                  mergeKey = null;
               }
               else
               {
                  mergeImportSource = args[i];
                  mergeImport = new DatatoolCsv(settings);
               }
            }
            else if (args[i].equals("--merge-sql"))
            {
               if (mergeImportSource != null || mergeFile != null)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValue("error.syntax.only_one", args[i]));
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_merge_key",
                      args[i-1]));
               }

               mergeKey = args[i];

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValues("error.syntax.missing_merge_statement",
                        args[i-2], args[i-1]));
               }

               mergeImportSource = args[i];
               mergeImport = new DatatoolSql(settings);
            }
            else if (args[i].equals("--merge-probsoln"))
            {
               if (mergeImportSource != null || mergeFile != null)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValue("error.syntax.only_one", args[i]));
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_merge_key",
                      args[i-1]));
               }

               mergeKey = args[i];

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValues("error.syntax.missing_merge_file",
                        args[i-2], args[i-1]));
               }

               File file = new File(args[i]);

               if (!file.exists())
               {
                  System.err.println(
                    getLabelWithValue("error.io.file_not_found",
                     args[i]));
                  mergeKey = null;
               }
               else
               {
                  mergeImportSource = args[i];
                  mergeImport = new DatatoolProbSoln(settings);
               }
            }
            else if (args[i].equals("--merge-xls"))
            {
               if (mergeImportSource != null || mergeFile != null)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValue("error.syntax.only_one", args[i]));
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_merge_key",
                      args[i-1]));
               }

               mergeKey = args[i];

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValues("error.syntax.missing_merge_file",
                        args[i-2], args[i-1]));
               }

               File file = new File(args[i]);

               if (!file.exists())
               {
                  System.err.println(
                    getLabelWithValue("error.io.file_not_found",
                     args[i]));
                  mergeKey = null;
               }
               else
               {
                  mergeImportSource = args[i];
                  mergeImport = new DatatoolExcel(settings);
               }
            }
            else if (args[i].equals("--merge-ods"))
            {
               if (mergeImportSource != null || mergeFile != null)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValue("error.syntax.only_one", args[i]));
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_merge_key",
                      args[i-1]));
               }

               mergeKey = args[i];

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                     getLabelWithValues("error.syntax.missing_merge_file",
                        args[i-2], args[i-1]));
               }

               File file = new File(args[i]);

               if (!file.exists())
               {
                  System.err.println(
                    getLabelWithValue("error.io.file_not_found",
                     args[i]));
                  mergeKey = null;
               }
               else
               {
                  mergeImportSource = args[i];
                  mergeImport = new DatatoolOpenDoc(settings);
               }
            }
            else if (args[i].equals("--name"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_dbname",
                      args[i-1]));
               }

               dbname = args[i];
            }
            else if (args[i].equals("--in") || args[i].equals("-i"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException(
                    getLabelWithValue("error.syntax.missing_input",
                      args[i-1]));
               }

               if (dbtex != null)
               {
                  throw new InvalidSyntaxException(
                    getLabel("error.syntax.only_one_input"));
               }

               if (imp != null)
               {
                  throw new InvalidSyntaxException(
                    getLabel("error.syntax.input_clash"));
               }

               dbtex = args[i];
            }
            else if (args[i].charAt(0) == '-')
            {
               throw new InvalidSyntaxException(
                getLabelWithValue("error.syntax.unknown_option",
                  args[i]));
            }
            else
            {
               // if no option specified, assume --in

               if (dbtex != null)
               {
                  throw new InvalidSyntaxException(
                    getLabel("error.syntax.only_one_input"));
               }

               if (imp != null)
               {
                  throw new InvalidSyntaxException(
                    getLabel("error.syntax.input_clash"));
               }

               dbtex = args[i];
            }
         }
      }
      catch (InvalidSyntaxException e)
      {
         if (guiMode)
         {
            DatatoolGuiResources.error(null, e);
         }
         else
         {
            System.err.println(appName+": "+
              getLabelWithValue("error.syntax", e.getMessage()));
         }

         System.exit(1);
      }

      if (guiMode)
      {
         javax.swing.SwingUtilities.invokeLater(new Runnable()
          {
             public void run()
             {
                createAndShowGUI();
             }
          });
      } 
      else
      {
         doBatchProcess();
      }
   }

   private static boolean guiMode = false;

   public static final String appVersion = "1.4";
   public static final String appName = "datatooltk";
   public static final String appDate = "2015-01-23";

   private static Properties dictionary;
   private static boolean debugMode = false;

   private static String out = null;
   private static String dbtex = null;
   private static String source = null;
   private static String mergeImportSource = null;

   private static File mergeFile = null;
   private static String mergeKey = null;

   private static Vector<FilterInfo> filterInfo = null;
   private static boolean filterOr = true;
   private static boolean filterInclude = true; 
   private static int truncate = -1;

   private static int noConsoleAction = ConsolePasswordReader.NO_CONSOLE_GUI;

   private static boolean removeTmpFilesOnExit=true;

   private static boolean doShuffle = false;

   private static boolean isCaseSensitive = false;

   private static String sort=null;

   private static String dbname = null;

   private static String dict = null;

   private static DatatoolImport imp = null;

   private static DatatoolImport mergeImport = null;

   private static DatatoolSettings settings;
}
