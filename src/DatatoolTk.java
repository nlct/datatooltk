package com.dickimawbooks.datatooltk;

public class DatatoolTk
{
   public static void doBatchProcess()
   {
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
      System.out.println(appName+" [<option>]+ [<datatool file>]");
      System.out.println();
      System.out.println("If <datatool file> is present, load that file. Can't be used with --in (or -i)");
      System.out.println();
      System.out.println("Available options: ");
      System.out.println("--gui (or -g)\tGUI interface.");
      System.out.println("--batch (or -b)\tNo GUI interface.");
      System.out.println("--import (or -i) <csv file>\tImport <csv file>. Can't be used with <datatool file>");
      System.out.println("--out (or -o) <filename>\tSave to <filename>.");
      System.out.println("--sep <string>\tSeparator used in CSV files. (Defaults to ',')");
      System.out.println("--version (or -v)\tPrint version and exit.");
      System.out.println("--help (or -h)\tPrint this help message and exit.");
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
                  throw new InvalidSyntaxException("string expected after --sep");
               }

               settings.setSeparator(args[i]);
            }
            else if (args[i].equals("--out") || args[i].equals("-o"))
            {
               if (out != null)
               {
                  throw new InvalidSyntaxException("Only one --out (or -o) permitted");
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException("Filename expected after --out (or -o)");
               }

               out = args[i];
            }
            else if (args[i].equals("--import") || args[i].equals("-i"))
            {
               if (in != null)
               {
                  throw new InvalidSyntaxException("Only one --import (or -i) permitted");
               }

               i++;

               if (i == args.length)
               {
                  throw new InvalidSyntaxException("Filename expected after --import (or -i)");
               }

               in = args[i];
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

               if (in != null)
               {
                  throw new InvalidSyntaxException("<datatool file> can't be used with --import (or -i)");
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
   private static String in  = null;
   private static String dbtex = null;

   private static DatatoolSettings settings = new DatatoolSettings();
}
