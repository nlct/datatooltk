package com.dickimawbooks.datatooltk;

import java.io.*;

public class DatatoolTk
{
   public static void doBatchProcess()
   {
      settings.setPasswordReader(new ConsolePasswordReader());

      if (imp == null && dbtex == null)
      {
         System.err.println("A database must either be loaded or imported batch mode.\n(If you want the GUI interface, use the --gui option.)");
         System.exit(1);
      }

      if (out == null)
      {
         System.err.println("--out <file> required for batch mode.\n(If you want the GUI interface, use the --gui option.)");
         System.exit(1);
      }

      DatatoolDb db = null;

      try
      {
         if (dbtex != null)
         {
            db = DatatoolDb.load(dbtex);
         }
         else
         {
            db = imp.importData(source);
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
   }

   public static void help()
   {
      version();
      System.out.println();
      System.out.println("Syntax: ");
      System.out.println();
      System.out.println(appName+" --gui");
      System.out.println("or");
      System.out.println(appName+" [<option>]+ <datatool file>");
      System.out.println("or");
      System.out.println(appName+" [<option>]+ --csv <csv file>");
      System.out.println("or");
      System.out.println(appName+" [<option>]+ --sqldb <name> --sql <sql statement>");
      System.out.println();
      System.out.println("General Options: ");
      System.out.println("--gui (or -g)\tGUI interface.");
      System.out.println("--batch (or -b)\tNo GUI interface. (Default.)");
      System.out.println("--out (or -o) <filename>\tSave to <filename>.");
      System.out.println("--version (or -v)\tPrint version and exit.");
      System.out.println("--help (or -h)\tPrint this help message and exit.");
      System.out.println();
      System.out.println("CSV Options: ");
      System.out.println("--csv <csv file>\tImport <csv file>.");
      System.out.println("--sep <character>\tSeparator used in CSV files. (Defaults to '"+settings.getSeparator()+"')");
      System.out.println("--delim <character>\tDelimiter used in CSV files. (Defaults to '"+settings.getDelimiter()+"')");
      System.out.println("--csvheader\tCSV files have a header row."+(settings.hasCSVHeader()?" (Default.)":""));
      System.out.println("--nocsvheader\tCSV files don't have a header row."+(settings.hasCSVHeader()?"":" (Default.)"));
      System.out.println();
      System.out.println("SQL Options: ");
      System.out.println("--sql <statement>\tImport data from SQL database where <statement> is a SQL SELECT statement.");
      System.out.println("--sqldb <name>\tSQL database name.");
      System.out.println("--sqlprefix <prefix>\tSQL prefix. (Default: '"
        + settings.getSqlPrefix()+"')");
      System.out.println("--sqlport <number>\tSQL port. (Default: "
        + settings.getSqlPort()+")");
      System.out.println("--sqlhost <host>\tSQL host. (Default: '"
        + settings.getSqlHost()+"')");
      System.out.println("--sqluser <user name>\tSQL user name."
       +(settings.getSqlUser() == null ? "" : " (Default: '"
         +settings.getSqlUser()+"')"));
      System.out.println("--sqlpassword <password>\tSQL password (insecure). If omitted, you will be prompted for the password if you try to import data from a SQL database.");
      System.out.println("--wipepassword\tFor extra security, wipe the password from memory as soon as it has been used to connect to a SQL database."
       + (settings.isWipePasswordEnabled()? " (Default)":""));
      System.out.println("--nowipepassword\tDon't wipe the password from memory as soon as it has been used to connect to a SQL database."
       + (settings.isWipePasswordEnabled()? "" : " (Default)"));
   }

   public static void version()
   {
      System.out.println(appName+" version "+appVersion);
   }

   public static void main(String[] args)
   {
      boolean gui = false;

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
                  throw new InvalidSyntaxException("character expected after --sep");
               }

               if (args[i].length() > 1)
               {
                  throw new InvalidSyntaxException("separator must be a single character not a string");
               }

               settings.setSeparator(args[i].charAt(0));
            }
            else if (args[i].equals("--delim"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException("character expected after "
                    +args[i-1]);
               }

               if (args[i].length() > 1)
               {
                  throw new InvalidSyntaxException("delimiter must be a single character not a string");
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
                  throw new InvalidSyntaxException("Only one "+args[i]
                    +" permitted");
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException("Filename expected after "
                   +args[i-1]);
               }

               out = args[i];
            }
            else if (args[i].equals("--csv"))
            {
               if (source != null)
               {
                  throw new InvalidSyntaxException("Only one import option permitted");
               }

               if (dbtex != null)
               {
                  throw new InvalidSyntaxException(args[i]+" can't be used with <datatool file>");
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException("Filename expected after "+args[i-1]);
               }

               source = args[i];
               imp = new DatatoolCsv(settings);
            }
            else if (args[i].equals("--sql"))
            {
               if (imp != null)
               {
                  throw new InvalidSyntaxException("Only one import option permitted");
               }

               if (dbtex != null)
               {
                  throw new InvalidSyntaxException(args[i]
                    +" can't be used with <datatool file>");
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException("SQL statement expected after "+args[i-1]);
               }

               source = args[i];
               imp = new DatatoolSql(settings);
            }
            else if (args[i].equals("--sqldb"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException("Name expected after "+args[i-1]);
               }

               settings.setSqlDbName(args[i]);
            }
            else if (args[i].equals("--sqlprefix"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException("Prefix expected after "+args[i-1]);
               }

               settings.setSqlPrefix(args[i]);
            }
            else if (args[i].equals("--sqlhost"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException("Host name expected after "+args[i-1]);
               }

               settings.setSqlHost(args[i]);
            }
            else if (args[i].equals("--sqluser"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException("User name expected after "+args[i-1]);
               }

               settings.setSqlUser(args[i]);
            }
            else if (args[i].equals("--sqlpassword"))
            {
               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException("Password expected after "+args[i-1]);
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
                  throw new InvalidSyntaxException("Port number expected after "+args[i-1]);
               }

               try
               {
                  settings.setSqlPort(Integer.parseInt(args[i]));
               }
               catch (NumberFormatException e)
               {
                  throw new InvalidSyntaxException(
                   "Number expected after "+args[i-1]+" ('"
                   + args[i]+"' found)");
               }
            }
            else if (args[i].equals("--gui") || args[i].equals("-g"))
            {
               gui = true;
            }
            else if (args[i].equals("--batch") || args[i].equals("-b"))
            {
               gui = false;
            }
            else if ("-".equals(args[i].charAt(0)))
            {
               throw new InvalidSyntaxException("Unknown option '"
                +args[i]+"'. Use --help or -h for help.");
            }
            else
            {
               if (dbtex != null)
               {
                  throw new InvalidSyntaxException("Only one <datatool file> permitted");
               }

               if (imp != null)
               {
                  throw new InvalidSyntaxException("<datatool file> can't be used with an import option");
               }

               dbtex = args[i];
            }
         }
      }
      catch (InvalidSyntaxException e)
      {
         System.err.println("Syntax error: "+e.getMessage());
         System.exit(1);
      }

      if (gui)
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

   public static final String appVersion = "0.1b";
   public static final String appName = "datatooltk";

   private static String out = null;
   private static String dbtex = null;
   private static String source = null;

   private static DatatoolImport imp = null;

   private static DatatoolSettings settings = new DatatoolSettings();
}
