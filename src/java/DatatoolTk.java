package com.dickimawbooks.datatooltk;

import java.io.*;
import java.util.Properties;
import java.util.Locale;
import java.awt.Cursor;

import com.dickimawbooks.datatooltk.io.*;
import com.dickimawbooks.datatooltk.gui.DatatoolGuiResources;
import com.dickimawbooks.datatooltk.gui.DatatoolGUI;

public class DatatoolTk
{
   public static void doBatchProcess()
   {
      settings.setPasswordReader(new ConsolePasswordReader());

      if (imp == null && dbtex == null)
      {
         System.err.println(getLabelWithValues("error.cli.no_data",
           "--gui", "--help"));
         System.exit(1);
      }

      if (out == null)
      {
         System.err.println(getLabelWithValues("error.cli.no_out",
           new String[]{"--out", "--gui", "--help"}));
         System.exit(1);
      }

      DatatoolDb db = null;

      try
      {
         if (dbtex != null)
         {
            db = DatatoolDb.load(settings, dbtex);
         }
         else
         {
            db = imp.importData(source);
         }

         if (!(sort == null || sort.isEmpty()))
         {
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
                  DatatoolTk.getLabelWithValue("error.syntax.unknown_field",
                  sort));
            }

            db.setSortColumn(colIndex);
            db.setSortAscending(ascending);
            db.sort();
         }

         if (doShuffle)
         {
            db.shuffle();
         }

         db.save(out);
      }
      catch (IOException e)
      {
         System.err.println(e.getMessage());
         System.exit(1);
      }
      catch (DatatoolImportException e)
      {
         System.err.println(e.getMessage());

         Throwable cause = e.getCause();

         if (cause != null)
         {
            System.err.println(cause.getMessage());
         }

         System.exit(1);
      }
   }

   public static void createAndShowGUI()
   {
      DatatoolGUI gui = new DatatoolGUI(settings);

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

      if (db != null)
      {
         if (!(sort == null || sort.isEmpty()))
         {
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
         }

         if (doShuffle)
         {
            db.shuffle();
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
      System.out.println(getLabelWithValues("syntax.in", "--in", "-i"));
      System.out.println(getLabelWithValues("syntax.out", "--out", "-o"));
      System.out.println(getLabelWithValues("syntax.version", "--version", "-v"));
      System.out.println(getLabelWithValues("syntax.help", "--help", "-h"));
      System.out.println(getLabelWithValue("syntax.debug", "--debug"));
      System.out.println(getLabelWithValue("syntax.nodebug", "--nodebug"));
      System.out.println(getLabelWithValue("syntax.deletetmpfiles", "--delete-tmp-files"));
      System.out.println(getLabelWithValue("syntax.nodeletetmpfiles", "--nodelete-tmp-files"));
      System.out.println(getLabelWithValues("syntax.maptexspecials", "--map-tex-specials", (settings.isTeXMappingOn()?" ("+getLabel("syntax.default")+".)":"")));
      System.out.println(getLabelWithValues("syntax.nomaptexspecials", "--nomap-tex-specials", (settings.isTeXMappingOn()?"":" ("+getLabel("syntax.default")+".)")));
      System.out.println(getLabelWithValue("syntax.seed", "--seed"));
      System.out.println(getLabelWithValue("syntax.shuffle_iter", "--shuffle-iterations"));
      System.out.println(getLabelWithValue("syntax.shuffle", "--shuffle"));
      System.out.println(getLabelWithValue("syntax.no_shuffle", "--noshuffle"));
      System.out.println(getLabelWithValue("syntax.sort", "--sort"));
      System.out.println();
      System.out.println(getLabel("syntax.csv_opts"));
      System.out.println(getLabelWithValue("syntax.csv", "--csv"));
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
      System.out.println();
      System.out.println(getLabel("syntax.probsoln_opts"));
      System.out.println(getLabelWithValue("syntax.probsoln", "--probsoln"));
   }

   public static String getAppInfo()
   {
      String eol = System.getProperty("line.separator", "\n");

      String info = getLabelWithValues("about.version",
        new String[]{ appName, appVersion, appDate})
        + eol
        + getLabelWithValue("about.copyright", "Nicola L. C. Talbot")
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
         System.err.println(message);
      }
   }

   public static void debug(String message)
   {
      if (debugMode)
      {
         System.out.println(message);
      }
   }

   public static void debug(Exception e)
   {
      if (debugMode)
      {
         e.printStackTrace();
      }
   }

   public static String getLanguage()
   {
      // TODO allow user to select a language other than the default

      return Locale.getDefault().getLanguage();
   }

   public static String getCountry()
   {
      // TODO allow user to select a country other than the default

      return Locale.getDefault().getCountry();
   }

   public static void loadDictionary()
      throws IOException
   {
      String lang    = getLanguage();
      String country = getCountry();

      String resource = "datatooltk";

      InputStream in = null;
      BufferedReader reader = null;

      try
      {
         in = DatatoolTk.class.getResourceAsStream(
           "/resources/dictionaries/"+resource+"-"+lang
           +"-"+country+".prop");

         if (in == null)
         {
            in = DatatoolTk.class.getResourceAsStream(
              "/resources/dictionaries/"+resource+"-"+lang+".prop");
         }

         if (in == null && !lang.equals("en"))
         {
            in = DatatoolTk.class.getResourceAsStream(
              "/resources/dictionaries/"+resource+"-en-US.prop");
         }

         if (in == null)
         {
            throw new FileNotFoundException
            (
               "Can't find dictionary resource file /resources/dictionaries/"
               +resource+"-en-US");
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
         System.err.println("No such dictionary property '"+label+"'");
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
         debug("Empty dictionary property '"+prop+"'");
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
         loadDictionary();
      }
      catch (IOException e)
      {
         System.err.println("Unable to load dictionary file:\n"
           + e.getMessage());
      }

      try
      {
         settings.loadProperties();
      }
      catch (IOException e)
      {
         System.err.println(getLabelWithValue("error.load.prop_failed", 
           e.getMessage()));
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
            else if (args[i].equals("--out") || args[i].equals("-o"))
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
            else if (args[i].charAt(0) == '-')
            {
               throw new InvalidSyntaxException(
                getLabelWithValue("error.syntax.unknown_option",
                  args[i]));
            }
            else
            {
               throw new InvalidSyntaxException(getLabelWithValue(
                  "error.syntax.missing_option", args[i]));
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
            System.err.println(
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

   public static final String appVersion = "0.1a";
   public static final String appName = "datatooltk";
   public static final String appDate = "2013-06-06";

   private static Properties dictionary;
   private static boolean debugMode = false;

   private static String out = null;
   private static String dbtex = null;
   private static String source = null;

   private static boolean removeTmpFilesOnExit=true;

   private static boolean doShuffle = false;

   private static String sort=null;

   private static DatatoolImport imp = null;

   private static DatatoolSettings settings;
}
