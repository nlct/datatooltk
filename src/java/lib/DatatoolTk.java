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

import java.util.Enumeration;
import java.util.Locale;
import java.util.Locale.Builder;
import java.util.Vector;

import java.util.regex.Matcher;

import java.text.MessageFormat;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.dickimawbooks.texparserlib.TeXParser;
import com.dickimawbooks.texparserlib.TeXSyntaxException;
import com.dickimawbooks.texparserlib.latex.datatool.CsvBlankOption;
import com.dickimawbooks.texparserlib.latex.datatool.EscapeCharsOption;
import com.dickimawbooks.texparserlib.html.HtmlTag;
import com.dickimawbooks.texjavahelplib.*;

import com.dickimawbooks.datatooltk.io.*;

/**
 * Abstract application class.
 */
public abstract class DatatoolTk extends CLITeXAppAdapter
{
   public DatatoolTk(boolean allowsGUI, boolean allowsSQL, boolean allowsXLS)
     throws IOException
   {
      super(true);

      this.allowsGUI = allowsGUI;
      this.allowsSQL = allowsSQL;
      this.allowsXLS = allowsXLS;

      if (!allowsGUI)
      {
         noConsoleAction = ConsolePasswordReader.NO_CONSOLE_ERROR;
      }
   }

   @Override
   protected void loadDictionaries(MessageSystem msgSys) throws IOException
   {
      super.loadDictionaries(msgSys);

      msgSys.loadDictionary(
        "/com/dickimawbooks/datatooltk/dictionaries/",
        "datatooltk");
   }

   @Override
   protected void preparse() throws IOException
   {
      settings = createSettings();

      settings.loadLangTags();

      HelpSetLocale msgLocale = settings.getDictionaryLocale();
      HelpSetLocale hsLocale = settings.getHelpSetLocale();

      if (msgLocale != null)
      {
         getCLITeXHelpLib().setMessageLocale(msgLocale);
      }

      if (hsLocale != null)
      {
         getCLITeXHelpLib().setHelpSetLocale(hsLocale);
      }

      loadSettings = new LoadSettings(settings);
   }

   @Override
   public String getCLIApplicationName()
   {
      return APP_NAME;
   }

   @Override
   public String getCLIApplicationVersion()
   {
      return VERSION;
   }

   @Override
   public String getCLIApplicationVersionDate()
   {
      return VERSION_DATE;
   }

   @Override
   public void printCLISyntax()
   {
      versionInfo();

      System.out.println();
      System.out.println(getMessage("syntax.title"));
      System.out.println();

      if (allowsGUI)
      {
         System.out.println(getApplicationName()+" --gui");
         System.out.println(getMessage("syntax.or"));
      }

      System.out.println(getMessage("syntax.opt_db", getApplicationName()));
      System.out.println(getMessage("syntax.or"));
      System.out.println(getMessage("syntax.opt_import", getApplicationName()));

      if (allowsSQL)
      {
         System.out.println(getMessage("syntax.or"));
         System.out.println(getMessage("syntax.opt_sql", getApplicationName()));
      }

      System.out.println();

      printSyntaxItem(getMessage("syntax.general"));

      if (allowsGUI)
      {
         printSyntaxItem(getMessage("syntax.gui", "--gui", "-g"));
         printSyntaxItem(getMessage("syntax.batch", "--batch", "-b"));
      }

      printSyntaxItem(getMessage("syntax.in", "--in", "-i", getApplicationName()));

      printSyntaxItem(getMessage("syntax.name", "--name"));

      printSyntaxItem(getMessage("syntax.out", "--output", "-o"));

      printSyntaxItem(getMessage("syntax.tex_encoding", "--tex-encoding"));

      printSyntaxItem(getMessage("syntax.output_format", "--output-format"));

      printSyntaxItem(getMessage("syntax.dtl_read", "--dtl-read"));
      printSyntaxItem(getMessage("syntax.dtl_write", "--dtl-write"));

      printSyntaxItem(getMessage("syntax.literal",
          "--[no]literal",
         (settings.isLiteralContent() ? "--literal" : "--noliteral")));

      printSyntaxItem(getMessage("syntax.maptexspecials",
          "--[no]map-tex-specials",
          "--[no]literal"));

      printSyntaxItem(getMessage("syntax.autotrimlabels",
          "--[no]auto-trim-labels",
          (settings.isAutoTrimLabelsOn() ? 
             "--auto-trim-labels" : "--noauto-trim-labels")));

      printSyntaxItem(getMessage("syntax.seed", "--seed"));
      printSyntaxItem(getMessage("syntax.shuffle", "--[no]shuffle"));
      printSyntaxItem(getMessage("syntax.sort", "--sort"));

      printSyntaxItem(getMessage("syntax.sort_case_sensitive",
         "--sort-case-sensitive"));

      printSyntaxItem(getMessage("syntax.sort_case_insensitive",
         "--sort-case-insensitive"));

      printSyntaxItem(getMessage("syntax.sort_locale", "--sort-locale"));

      printSyntaxItem(getMessage("syntax.owner_only",
         "--[no]owner_only",
         (settings.isOwnerOnly() ?  "--owner-only" : "--[no]owner-only")));

      printSyntaxItem(getMessage("syntax.truncate", "--truncate"));

      printSyntaxItem(getMessage("syntax.remove_cols", "--remove-columns"));

      printSyntaxItem(getMessage("syntax.remove_except_cols",
         "--remove-except-columns"));

      printSyntaxItem(getMessage("syntax.filter_or", "--filter-or"));

      printSyntaxItem(getMessage("syntax.filter_and", "--filter-and"));

      printSyntaxItem(getMessage("syntax.filter_include", "--filter-include"));

      printSyntaxItem(getMessage("syntax.filter_exclude", "--filter-exclude"));

      printSyntaxItem(getMessage("syntax.filter", "--filter"));

      printSyntaxItem(getMessage("syntax.merge", "--merge"));

      System.out.println();

      helpImportOptions();

      System.out.println();
      System.out.println(getMessage("clisyntax.other.options"));
      System.out.println();
      
      printSyntaxItem(getMessage("syntax.compat", "--compat"));

      printCommonCLISyntax();
      
      System.out.println();

      System.out.println(getMessage("clisyntax.bugreport",
        "https://github.com/nlct/datatooltk"));
   }

   @Override
   public void printCLIAbout()
   {
      System.out.println(getAboutInfo(false));
   }

   @Override
   protected int getCLIArgCount(String arg)
   {
      if (
           arg.equals("--csv-sep") || arg.equals("--sep")
         || arg.equals("--tab-sep")
         || arg.equals("--csv-delim") || arg.equals("--delim")
         || arg.equals("--csv-header") || arg.equals("--csvheader")
         || arg.equals("--csv-escape-chars")
         || arg.equals("--csv-escape") || arg.equals("--csvescape")
         || arg.equals("--csv-skiplines")
         || arg.equals("--csv-empty-rows")
         || arg.equals("--csv-encoding") || arg.equals("--csvencoding")
         || arg.equals("--output") || arg.equals("-o")
         || arg.equals("--in") || arg.equals("-i")
         || arg.equals("--import")
         || arg.equals("--csv")
         || arg.equals("--tsv")
         || arg.equals("--xls")
         || arg.equals("--xlsx")
         || arg.equals("--ods")
         || arg.equals("--odf") || arg.equals("--fods")
         || arg.equals("--probsoln")
         || arg.equals("--sql")
         || arg.equals("--sheet")
         || arg.equals("--sqldb")
         || arg.equals("--sqlprefix")
         || arg.equals("--sqlhost")
         || arg.equals("--sqluser")
         || arg.equals("--sqlport")
         || arg.equals("--sqlpassword")
         || arg.equals("--noconsole-action") || arg.equals("--missing-console-action")
         || arg.equals("--headers")
         || arg.equals("--keys")
         || arg.equals("--seed")
         || arg.equals("--compat")
         || arg.equals("--shuffle-iterations")
         || arg.equals("--sort")
         || arg.equals("--sort-locale")
         || arg.equals("--tex-encoding")
         || arg.equals("--output-format")
         || arg.equals("--truncate")
         || arg.equals("--remove-columns")
         || arg.equals("--remove-except-columns")
         || arg.equals("--name")
         )
      {
         return 1;
      }
      else if (
             arg.equals("--dtl-read")
          || arg.equals("--dtl-write")
          || arg.equals("--merge")
          || arg.startsWith("--merge-")
        )
      {
         return 2;
      }
      else if (
          arg.equals("--filter")
        )
      {
         return 3;
      }

      return super.getCLIArgCount(arg);
   }

   @Override
   protected int getCLIMaxArgParams()
   {
      return 3;
   }

   @Override
   protected void parseNoSwitchCLIArg(String arg)
      throws InvalidSyntaxException
   {
      // if no option specified, assume --in

      if (loadSettings.getInputFile() != null)
      {
         throw new InvalidSyntaxException(
           getMessage("error.syntax.only_one_input"));
      }

      if (loadSettings.getDataImport() != null)
      {
         throw new InvalidSyntaxException(
           getMessage("error.syntax.input_clash"));
      }

      loadSettings.setInputFile(arg);
   }

   @Override
   protected boolean parseCLIArg(String arg, CLIArgValue[] returnVals)
     throws InvalidSyntaxException
   {
      CLISyntaxParser cliParser = getSyntaxParser();
   
      if (cliParser.isArg(arg, null, "--sep", "--csv-sep", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         settings.setSeparator(returnVals[0].toString().codePointAt(0));
      }
      else if (arg.equals("--tab-sep"))
      {
         settings.setSeparator('\t');
      }
      else if (cliParser.isArg(arg, null, "--delim", "--csv-delim", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         settings.setDelimiter(returnVals[0].toString().codePointAt(0));
      }
      else if (arg.equals("--csv-header") || arg.equals("--csvheader"))
      {
         settings.setHasCSVHeader(true);
      }
      else if (arg.equals("--nocsv-header") || arg.equals("--nocsvheader"))
      {
         settings.setHasCSVHeader(false);
      }
      else if (cliParser.isArg(arg, "--csv-escape-chars", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         String optName = returnVals[0].toString();

         EscapeCharsOption opt = EscapeCharsOption.fromOptionName(optName);

         if (opt == null)
         {
            StringBuilder builder = new StringBuilder();

            for (EscapeCharsOption o : EscapeCharsOption.values())
            {
               if (builder.length() > 0)
               {
                  builder.append(", ");
               }

               builder.append(o.getName());
            }

            throw new InvalidSyntaxException(
              getMessage("error.syntax.invalid_set_value",
                  optName, arg, builder.toString()));
         }
         else
         {
            settings.setEscapeCharsOption(opt);
         }
      }
      else if (cliParser.isArg(arg, null, "--csv-escape", "--csvescape", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         String val = returnVals[0].toString();

         if (val.equals("\\"))
         {
            settings.setEscapeCharsOption(EscapeCharsOption.ESC_DELIM_BKSL);
         }
         else if (val.isEmpty())
         {
            settings.setEscapeCharsOption(EscapeCharsOption.NONE);
         }
         else if (val.codePointAt(0) == settings.getDelimiter())
         {
            settings.setEscapeCharsOption(EscapeCharsOption.DOUBLE_DELIM);
         }
         else
         {
            throw new InvalidSyntaxException(
               getMessage("error.syntax.deprecated",
                 arg, "--csv-escape-chars"));
         }
      }
      else if (arg.equals("--nocsv-escape") || arg.equals("--nocsvescape"))
      {
         settings.setEscapeCharsOption(EscapeCharsOption.NONE);
      }
      else if (cliParser.isIntArg(arg, "--csv-skiplines", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         settings.setCSVSkipLines(returnVals[0].intValue());
      }
      else if (arg.equals("--csv-strictquotes"))
      {
         settings.setCSVStrictQuotes(true);
      }
      else if (arg.equals("--nocsv-strictquotes"))
      {
         settings.setCSVStrictQuotes(false);
      }
      else if (cliParser.isArg(arg, null, "--csv-encoding", "--csvencoding", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         String val = returnVals[0].toString();

         if (val.equals("default") || val.isEmpty())
         {
            settings.setCsvEncoding(null);
         }
         else
         {
            try
            {
               if (Charset.isSupported(val))
               {
                  settings.setCsvEncoding(Charset.forName(val));
               }
               else
               {
                 throw new InvalidSyntaxException(
                   getMessage("error.syntax.unknown.encoding", val));
               }
            }
            catch (Exception e)
            {
               throw new InvalidSyntaxException(
                 getMessage("error.syntax.unknown.encoding",
                 val), e);
            }
         }
      }
      else if (cliParser.isArg(arg, "-o", "--output", returnVals))
      {
         if (loadSettings.getOutputFile() != null)
         {
            throw new InvalidSyntaxException(
              getMessage("error.syntax.only_one", arg));
         }

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         loadSettings.setOutputFile(returnVals[0].toString());
      }
      else if (cliParser.isArg(arg, "--dtl-read", returnVals))
      {
         if (loadSettings.getImportSource() != null)
         {
            throw new InvalidSyntaxException(
              getMessage("error.syntax.only_one_import"));
         }

         if (loadSettings.getInputFile() != null)
         {
            throw new InvalidSyntaxException(
              getMessage("error.syntax.import_clash", arg));
         }

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.syntax.missing_optionlist", arg));
         }

         String optList = returnVals[0].toString();

         if (returnVals[1] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.syntax.missing_filename",
                  arg+" \""+optList+"\""));
         }

         loadSettings.setImportSource(returnVals[1].toString());
         loadSettings.setDataImport(new DatatoolTeX(optList, settings));
      }
      else if (cliParser.isArg(arg, "--dtl-write", returnVals))
      {
         if (loadSettings.getDataExport() != null)
         {
            throw new InvalidSyntaxException(
              getMessage("error.syntax.only_one_export"));
         }

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.syntax.missing_optionlist", arg));
         }

         String optList = returnVals[0].toString();

         if (returnVals[1] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.syntax.missing_filename",
                  arg+" \""+optList+"\""));
         }

         loadSettings.setExportTarget(returnVals[1].toString());
         loadSettings.setDataExport(new DatatoolTeX(optList, settings));
      }
      else if (arg.equals("--preamble-only"))
      {
         settings.setPreambleOnly(true);
      }
      else if (arg.equals("--nopreamble-only"))
      {
         settings.setPreambleOnly(false);
      }
      else if (cliParser.isArg(arg, "--sheet", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         String val = returnVals[0].toString();

         try
         {
            settings.getImportSettings().setSheetIndex(Integer.valueOf(val));
         }
         catch (NumberFormatException e)
         {
            settings.getImportSettings().setSheetName(val);
         }
      }
      else if (cliParser.isArg(arg, "--sqldb", returnVals))
      {
         if (!allowsSQL)
         {
            throw new InvalidSyntaxException(
              getMessage("error.unsupported_option", arg));
         }

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         settings.setSqlDbName(returnVals[0].toString());
      }
      else if (cliParser.isArg(arg, "--sqlprefix", returnVals))
      {
         if (!allowsSQL)
         {
            throw new InvalidSyntaxException(
              getMessage("error.unsupported_option", arg));
         }

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         settings.setSqlPrefix(returnVals[0].toString());
      }
      else if (cliParser.isArg(arg, "--sqlhost", returnVals))
      {
         if (!allowsSQL)
         {
            throw new InvalidSyntaxException(
              getMessage("error.unsupported_option", arg));
         }

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         settings.setSqlHost(returnVals[0].toString());
      }
      else if (cliParser.isArg(arg, "--sqluser", returnVals))
      {
         if (!allowsSQL)
         {
            throw new InvalidSyntaxException(
              getMessage("error.unsupported_option", arg));
         }

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         settings.setSqlUser(returnVals[0].toString());
      }
      else if (cliParser.isArg(arg, "--sqlpassword", returnVals))
      {
         if (!allowsSQL)
         {
            throw new InvalidSyntaxException(
              getMessage("error.unsupported_option", arg));
         }

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         settings.getImportSettings().setSqlPassword(returnVals[0].toString().toCharArray());
         returnVals[0] = null;
      }
      else if (arg.equals("--wipepassword"))
      {
         if (!allowsSQL)
         {
            throw new InvalidSyntaxException(
              getMessage("error.unsupported_option", arg));
         }

         settings.setWipePassword(true);
      }
      else if (arg.equals("--nowipepassword"))
      {
         if (!allowsSQL)
         {
            throw new InvalidSyntaxException(
              getMessage("error.unsupported_option", arg));
         }

         settings.setWipePassword(false);
      }
      else if (cliParser.isArg(arg,
                 null, "--noconsole-action", "--missing-console-action",
                 returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         String val = returnVals[0].toString();

         if (val.equals("stdin"))
         {
            noConsoleAction = ConsolePasswordReader.NO_CONSOLE_STDIN;
         }
         else if (val.equals("gui"))
         {
            noConsoleAction = ConsolePasswordReader.NO_CONSOLE_GUI;
         }
         else if (val.equals("error"))
         {
            noConsoleAction = ConsolePasswordReader.NO_CONSOLE_ERROR;
         }
         else
         {
            throw new InvalidSyntaxException(
               getMessage("error.syntax.invalid_noconsole_action",
                 arg, val));
         }
      }
      else if (cliParser.isIntArg(arg, "--sqlport", returnVals))
      {
         if (!allowsSQL)
         {
            throw new InvalidSyntaxException(
              getMessage("error.unsupported_option", arg));
         }

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         settings.setSqlPort(returnVals[0].intValue());
      }
      else if (arg.equals("--gui") || arg.equals("-g"))
      {
         if (!allowsGUI)
         {
            throw new InvalidSyntaxException(
              getMessage("error.unsupported_option", arg));
         }

         setBatchMode(false);
      }
      else if (arg.equals("--batch") || arg.equals("-b"))
      {
         setBatchMode(true);
      }
      else if (arg.equals("--map-tex-specials") || arg.equals("--literal"))
      {
         settings.setLiteralContent(true);
      }
      else if (arg.equals("--nomap-tex-specials") || arg.equals("--noliteral"))
      {
         settings.setLiteralContent(false);
      }
      else if (cliParser.isListArg(arg, "--headers", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         if (returnVals[0].toString().trim().isEmpty())
         {
            settings.setColumnHeaders(null);
         }
         else
         {
            settings.setColumnHeaders(returnVals[0].listValue());
         }
      }
      else if (cliParser.isListArg(arg, "--keys", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         if (returnVals[0].toString().trim().isEmpty())
         {
            settings.setColumnKeys(null);
         }
         else
         {
            settings.setColumnKeys(returnVals[0].listValue());
         }
      }
      else if (arg.equals("--auto-keys"))
      {
         settings.setAutoKeys(true);
      }
      else if (arg.equals("--noauto-keys"))
      {
         settings.setAutoKeys(false);
      }
      else if (arg.equals("--auto-trim-labels"))
      {
         settings.setAutoTrimLabels(true);
      }
      else if (arg.equals("--noauto-trim-labels"))
      {
         settings.setAutoTrimLabels(false);
      }
      else if (arg.equals("--csv-skip-empty-rows"))
      {
         settings.setCsvBlankOption(CsvBlankOption.IGNORE);
      }
      else if (arg.equals("--nocsv-skip-empty-rows"))
      {
         settings.setCsvBlankOption(CsvBlankOption.EMPTY_ROW);
      }
      else if (cliParser.isArg(arg, "--csv-empty-rows", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         String val = returnVals[0].toString();

         CsvBlankOption opt = CsvBlankOption.fromOptionName(val);

         if (opt == null)
         {
            StringBuilder builder = new StringBuilder();

            for (CsvBlankOption blankOpt : CsvBlankOption.values())
            {
               if (builder.length() > 0)
               {
                  builder.append(", ");
               }

               builder.append(blankOpt.getName());
            }

            throw new InvalidSyntaxException(
             getMessage("error.syntax.invalid_set_value", val,
               arg, builder.toString()));
         }

         settings.setCsvBlankOption(opt);
      }
      else if (arg.equals("--owner-only"))
      {
         settings.setOwnerOnly(true);
      }
      else if (arg.equals("--noowner-only"))
      {
         settings.setOwnerOnly(false);
      }
      else if (cliParser.isArg(arg, "--seed", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         String val = returnVals[0].toString();

         if (val.isEmpty())
         {
            settings.setRandomSeed(null);
         }
         else
         {
            try
            {
               settings.setRandomSeed(Long.valueOf(val));
            }
            catch (NumberFormatException e)
            {
               throw new InvalidSyntaxException(
                 getMessage("error.clisyntax.invalid.long_value",
                   arg, val));
            }
         }
      }
      else if (cliParser.isArg(arg, "--compat", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         String val = returnVals[0].toString();

         if (val.equals("latest"))
         {
            settings.setCompatibilityLevel(settings.COMPAT_LATEST);
         }
         else if (val.equals("1.6"))
         {
            settings.setCompatibilityLevel(settings.COMPAT_1_6);
         }
         else
         {
            throw new InvalidSyntaxException(
              getMessage("error.syntax.invalid_compat",
                val));
         }
      }
      else if (cliParser.isIntArg(arg, "--shuffle-iterations", returnVals))
      {
         // only provided for backward compatibility when using
         // --compat=1.6 (not documented anymore)

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         settings.setShuffleIterations(returnVals[0].intValue());
      }
      else if (arg.equals("--shuffle"))
      {
         loadSettings.setShuffle(true);
      }
      else if (arg.equals("--noshuffle"))
      {
         loadSettings.setShuffle(false);
      }
      else if (arg.equals("--sort-case-sensitive"))
      {
         loadSettings.setCaseSensitive(true);
      }
      else if (arg.equals("--sort-case-insensitive"))
      {
         loadSettings.setCaseSensitive(false);
      }
      else if (cliParser.isArg(arg, "--sort", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         loadSettings.setSort(returnVals[0].toString());
      }
      else if (cliParser.isArg(arg, "--sort-locale", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         String val = returnVals[0].toString();

         if (val.equals("none"))
         {
            settings.setSortLocale((String)null);
         }
         else
         {
            // check argument is valid

            try
            {
               Locale locale = new Builder().setLanguageTag(val).build();

               settings.setSortLocale(locale.toLanguageTag());
            }
            catch (Exception e)
            {
               throw new InvalidSyntaxException(
                  getMessage("error.syntax.invalid_locale",
                  val), e);
            }
         }
      }
      else if (cliParser.isArg(arg, "--tex-encoding", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         String val = returnVals[0].toString();

         if (val.equals("default") || val.isEmpty())
         {
            settings.setTeXEncoding(null);
         }
         else
         {
            try
            {
               if (Charset.isSupported(val))
               {
                  settings.setTeXEncoding(Charset.forName(val));
               }
               else
               {
                 throw new InvalidSyntaxException(
                   getMessage("error.syntax.unknown.encoding",
                   val));
               }
            }
            catch (Exception e)
            {
               throw new InvalidSyntaxException(
                 getMessage("error.syntax.unknown.encoding",
                 val), e);
            }
         }
      }
      else if (cliParser.isArg(arg, "--output-format", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         String val = returnVals[0].toString();

         if (val.isEmpty() || val.equals("default"))
         {
            settings.setDefaultOutputFormat(null);
         }
         else
         {
            String fmt = val.toUpperCase();

            Matcher m = DatatoolDb.FORMAT_PATTERN.matcher(fmt);

            if (!m.matches())
            {
               throw new InvalidSyntaxException(
                  getMessage("error.syntax.invalid_output_format", val));
            }

            settings.setDefaultOutputFormat(fmt);
         }
      }
      else if (arg.equals("--filter-or"))
      {
         loadSettings.setFilterOp(true);
      }
      else if (arg.equals("--filter-and"))
      {
         loadSettings.setFilterOp(false);
      }
      else if (arg.equals("--filter-include"))
      {
         loadSettings.setFilterInclude(true);
      }
      else if (arg.equals("--filter-exclude"))
      {
         loadSettings.setFilterInclude(false);
      }
      else if (cliParser.isArg(arg, "--filter", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.syntax.missing_filter_label", arg));
         }

         String label = returnVals[0].toString();

         if (returnVals[1] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.syntax.missing_filter_operator", arg, label));
         }

         String operator = returnVals[1].toString();

         if (returnVals[2] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.syntax.missing_filter_value", arg, label, operator));
         }

         loadSettings.addFilterInfo(new FilterInfo(getMessageHandler(),
           label, operator, returnVals[2].toString()));
      }
      else if (cliParser.isIntArg(arg, "--truncate", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         loadSettings.setTruncate(returnVals[0].intValue());
      }
      else if (cliParser.isArg(arg, "--remove-columns", returnVals))
      {
         if (loadSettings.getRemoveExceptColumnList() != null)
         {
            throw new InvalidSyntaxException(
              getMessage("error.syntax.option_clash",
                arg, "--remove-except-columns"));
         }

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         loadSettings.setRemoveColumnList(returnVals[0].toString());
      }
      else if (cliParser.isArg(arg, "--remove-except-columns", returnVals))
      {
         if (loadSettings.getRemoveColumnList() != null)
         {
            throw new InvalidSyntaxException(
              getMessage("error.syntax.option_clash",
                arg, "--remove-columns"));
         }

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         loadSettings.setRemoveExceptColumnList(returnVals[0].toString());
      }
      else if (cliParser.isArg(arg, "--name", returnVals))
      {
         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         loadSettings.setDbName(returnVals[0].toString());
      }
      else if (cliParser.isArg(arg, "-i", "--in", returnVals))
      {
         if (loadSettings.getInputFile() != null)
         {
            throw new InvalidSyntaxException(
              getMessage("error.syntax.only_one_input"));
         }

         if (loadSettings.getDataImport() != null)
         {
            throw new InvalidSyntaxException(
              getMessage("error.syntax.input_clash"));
         }

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.clisyntax.missing_value", arg));
         }

         loadSettings.setInputFile(returnVals[0].toString());
      }
      else if (cliParser.isArg(arg, "--merge", returnVals))
      {
         if (loadSettings.getMergeImportSource() != null
             || loadSettings.getMergeFile() != null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.syntax.only_one_merge"));
         }

         if (returnVals[0] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.syntax.missing_merge_key", arg));
         }

         String mergeKey = returnVals[0].toString();

         if (returnVals[1] == null)
         {
            throw new InvalidSyntaxException(
               getMessage("error.syntax.missing_merge_file", arg, mergeKey));
         }

         File mergeFile = new File(returnVals[1].toString());

         if (!mergeFile.exists())
         {
            try
            {
               throw new FileNotFoundException(
                 getMessage("error.io.file_not_found", returnVals[1]));
            }
            catch (IOException e)
            {
               error(getMessage("error.syntax.merge_omitted",
                  arg, returnVals[0], returnVals[1], e.getMessage()), e);
            }
         }
         else
         {
            loadSettings.setMergeKey(mergeKey);
            loadSettings.setMergeFile(mergeFile);
         }
      }
      else if (cliParser.isArgPrefix(arg, "--merge-", returnVals))
      {
         processMergeOption(arg, returnVals, arg.substring(8));
      }
      else if (cliParser.isArg(arg, "--import", returnVals))
      {
         processImportOption(arg, returnVals, "import");
      }
      else if (cliParser.isArg(arg, "--csv", returnVals))
      {
         processImportOption(arg, returnVals, "csv");
      }
      else if (cliParser.isArg(arg, "--tsv", returnVals))
      {
         processImportOption(arg, returnVals, "tsv");
      }
      else if (cliParser.isArg(arg, "--xls", returnVals))
      {
         processImportOption(arg, returnVals, "xls");
      }
      else if (cliParser.isArg(arg, "--xlsx", returnVals))
      {
         processImportOption(arg, returnVals, "xlsx");
      }
      else if (cliParser.isArg(arg, "--ods", returnVals))
      {
         processImportOption(arg, returnVals, "ods");
      }
      else if (cliParser.isArg(arg, null, "--odf", "--fods", returnVals))
      {
         processImportOption(arg, returnVals, "fods");
      }
      else if (cliParser.isArg(arg, "--probsoln", returnVals))
      {
         processImportOption(arg, returnVals, "probsoln");
      }
      else if (cliParser.isArg(arg, "--sql", returnVals))
      {
         processImportOption(arg, returnVals, "sql");
      }
      else
      {
         // common options
         return super.parseCLIArg(arg, returnVals);
      }

      return true;
   }

   @Override
   protected void postCLIProcess()
     throws InvalidSyntaxException
   {
      process();
   }

   protected abstract void process();

   public void setBatchMode(boolean mode)
   {
      isBatchMode = mode;
   }

   public boolean isBatchMode()
   {
      return isBatchMode;
   }

   @Override
   public boolean isGUI()
   {
      return !isBatchMode;
   }

   /**
    * Checks if this application supports SQL imports.
    */
   public boolean isSQLSupported()
   {
      return allowsSQL;
   }

   /**
    * Checks if this application supports Binary Excel format
    * imports.
    * That is, the non-XML format (which requires Apache POI library).
    */
   public boolean isBinaryExcelSupported()
   {
      return allowsXLS;
   }

   public int getSupportedFileFormatFlags()
   {
      int flags = DatatoolFileFormat.FILE_FORMAT_FLAG_TEX
                | DatatoolFileFormat.FILE_FORMAT_CSV_OR_TSV
                | DatatoolFileFormat.FILE_FORMAT_FLAG_XLSX
                | DatatoolFileFormat.FILE_FORMAT_FLAG_ODS
                | DatatoolFileFormat.FILE_FORMAT_FLAG_FODS;

      if (allowsSQL)
      {
         flags = flags | DatatoolFileFormat.FILE_FORMAT_FLAG_SQL;
      }

      if (allowsXLS)
      {
         flags = flags | DatatoolFileFormat.FILE_FORMAT_FLAG_XLS;
      }

      return flags;
   }

   protected abstract DatatoolSettings createSettings() throws IOException;

   public MessageHandler getMessageHandler()
   {
      return settings == null ? null : settings.getMessageHandler();
   }

   public DatatoolSettings getSettings()
   {
      return settings;
   }

   public DatatoolImport getDatatoolImport(DatatoolFileFormat fmt)
   throws UnsupportedFileFormatException
   {
      return getDatatoolImport(fmt, settings);
   }

   public DatatoolImport getDatatoolImport(DatatoolFileFormat fmt,
     DatatoolSettings settings)
   throws UnsupportedFileFormatException
   {
      return getDatatoolImport(fmt.getFileFormat(), settings);
   }

   public DatatoolImport getDatatoolImport(int fmtId)
   throws UnsupportedFileFormatException
   {
      return getDatatoolImport(fmtId, settings);
   }

   /**
    * Gets a new DatatoolImport corresponding to the given file format
    * identifier. This method will need to be override if support
    * for SQL or Binary Excel is required.
    */
   public DatatoolImport getDatatoolImport(int fmtId,
     DatatoolSettings settings)
   throws UnsupportedFileFormatException
   {
      switch (fmtId)
      {
         case DatatoolFileFormat.FILE_FORMAT_FLAG_SQL:

            if (allowsSQL)
            {
               return new DatatoolSql(settings);
            }
            else
            {
               throw new UnsupportedFileFormatException(
                 getLabelWithValues("error.unsupported_option", "sql"));
            }

         case DatatoolFileFormat.FILE_FORMAT_FLAG_XLS:

            throw new UnsupportedFileFormatException(
              getLabelWithValues("error.unsupported_option", "xls"));

         case DatatoolFileFormat.FILE_FORMAT_FLAG_TSV:
            return new DatatoolCsv(settings, '\t');
         case DatatoolFileFormat.FILE_FORMAT_FLAG_CSV:
            return new DatatoolCsv(settings);
         case DatatoolFileFormat.FILE_FORMAT_FLAG_ODS:
           return new DatatoolOpenDoc(settings, false);
         case DatatoolFileFormat.FILE_FORMAT_FLAG_FODS:
           return new DatatoolOpenDoc(settings, true);
         case DatatoolFileFormat.FILE_FORMAT_FLAG_XLSX:
           return new DatatoolOfficeOpenXML(settings);
         default: // assume TeX
           return new DatatoolTeX(settings);
      }
   }

   public DatatoolImport getDatatoolImport(String type)
     throws UnsupportedFileFormatException
   {
      if (type.equals("sql"))
      {
         return getDatatoolImport(DatatoolFileFormat.FILE_FORMAT_FLAG_SQL);
      }
      else if (type.equals("csv"))
      {
         return getDatatoolImport(DatatoolFileFormat.FILE_FORMAT_FLAG_CSV);
      }
      else if (type.equals("tsv"))
      {
         return getDatatoolImport(DatatoolFileFormat.FILE_FORMAT_FLAG_TSV);
      }
      else if (type.equals("probsoln"))
      {
         return new DatatoolProbSoln(settings);
      }
      else if (type.equals("xls"))
      {
         return getDatatoolImport(DatatoolFileFormat.FILE_FORMAT_FLAG_XLS);
      }
      else if (type.equals("xlsx"))
      {
         return getDatatoolImport(DatatoolFileFormat.FILE_FORMAT_FLAG_XLSX);
      }
      else if (type.equals("ods"))
      {
         return getDatatoolImport(DatatoolFileFormat.FILE_FORMAT_FLAG_ODS);
      }
      else if (type.equals("fods"))
      {
         return getDatatoolImport(DatatoolFileFormat.FILE_FORMAT_FLAG_FODS);
      }
      else
      {
         throw new UnsupportedFileFormatException(
            getLabelWithValues("error.unsupported_option", type));
      }
   }

   public void processMergeOption(String opt, CLIArgValue[] returnVals, String type)
     throws InvalidSyntaxException
   {
      boolean isSql = allowsSQL && type.equals("sql");

      if (loadSettings.getMergeImport() != null)
      {
         throw new InvalidSyntaxException(
            getMessage("error.syntax.only_one", opt));
      }

      if (returnVals[0] == null)
      {
         throw new InvalidSyntaxException(
           getMessage("error.syntax.missing_merge_key", opt));
      }

      String mergeKey = returnVals[0].toString();

      if (returnVals[1] == null)
      {
         throw new InvalidSyntaxException(
            getMessage(
               isSql ? "error.syntax.missing_merge_statement" :
               "error.syntax.missing_merge_file",
               opt, mergeKey));
      }

      loadSettings.setMergeKey(mergeKey);
      loadSettings.setMergeImportSource(returnVals[1].toString());

      if (type.equals("import"))
      {
         String filename = returnVals[1].toString();
         File file = new File(filename);

         if (!file.exists())
         {
            if (allowsSQL && filename.lastIndexOf('.') == -1)
            {
               throw new InvalidSyntaxException(
                  getMessage("error.io.file_not_found_query_sql",
                     filename, "--merge-sql"));
            }
            else
            {
               throw new InvalidSyntaxException(
                  getMessage("error.io.file_not_found", filename));
            }
         }

         try
         {
            DatatoolFileFormat fmt = DatatoolFileFormat.valueOf(
              getMessageHandler(), file);

            loadSettings.setMergeImport(getDatatoolImport(fmt));
         }
         catch (IOException e)
         {
            throw new InvalidSyntaxException(
              getMessage("error.syntax.merge_io_error",
              opt, mergeKey, filename, e.getMessage()), e);
         }
      }
      else
      {
         try
         {
            loadSettings.setMergeImport(getDatatoolImport(type));
         }
         catch (UnsupportedFileFormatException e)
         {
            throw new InvalidSyntaxException( 
              getMessage("error.syntax.unknown_option", opt), e);
         }
      }
   }

   public void processImportOption(String opt, CLIArgValue[] returnVals, String type)
     throws InvalidSyntaxException
   {
      boolean isSql = allowsSQL && type.equals("sql");

      if (loadSettings.getDataImport() != null)
      {
         throw new InvalidSyntaxException(
            getMessage("error.syntax.only_one_import", opt));
      }

      if (returnVals[0] == null)
      {
         throw new InvalidSyntaxException(
            getMessage(
               isSql ? "error.syntax.missing_sql" :
               "error.syntax.missing_filename",
               opt));
      }

      String src = returnVals[0].toString();

      loadSettings.setImportSource(src);

      if (type.equals("import"))
      {
         File file = new File(src);

         if (!file.exists())
         {
            if (allowsSQL && src.lastIndexOf('.') == -1)
            {
               throw new InvalidSyntaxException(
                  getMessage("error.io.file_not_found_query_sql",
                     src, "--sql"));
            }
            else
            {
               throw new InvalidSyntaxException(
                  getMessage("error.io.file_not_found", src));
            }
         }

         try
         {
            DatatoolFileFormat fmt = DatatoolFileFormat.valueOf(
              getMessageHandler(), file);

            loadSettings.setDataImport(getDatatoolImport(fmt));
         }
         catch (IOException e)
         {
            throw new InvalidSyntaxException(
             getMessage("error.syntax.import_io_error", opt, src, e.getMessage()), e);
         }
      }
      else
      {
         try
         {
            loadSettings.setDataImport(getDatatoolImport(type));
         }
         catch (UnsupportedFileFormatException e)
         {
            throw new InvalidSyntaxException( 
              getMessage("error.syntax.unknown_option", opt), e);
         }
      }
   }

   public void runApplication(String[] args)
    throws InvalidSyntaxException,IOException
   {
      initialiseHelpAndParse(args);
      process();
   }

   public void doBatchProcess()
   {
      settings.setPasswordReader(new ConsolePasswordReader(
        getMessageHandler(), noConsoleAction));

      File inFile = loadSettings.getInputFile();
      DatatoolImport imp = loadSettings.getDataImport();

      if (imp == null && inFile == null)
      {
         if (allowsGUI)
         {
            getMessageHandler().error(getMessage("error.cli.no_data",
              "--gui", "--help"), MessageHandler.SYNTAX_FAILURE);
         }
         else
         {
            getMessageHandler().error(
              getMessage("error.cli.no_data_no_gui", "--help"),
              MessageHandler.SYNTAX_FAILURE);
         }

         exit(EXIT_SYNTAX);
      }

      if (!loadSettings.hasOutputAction())
      {
         if (allowsGUI)
         {
            getMessageHandler().error(getLabelWithValues("error.cli.no_out",
              "--output", "--dtl-write", "--gui", "--help"),
              MessageHandler.SYNTAX_FAILURE);
         }
         else
         {
            getMessageHandler().error(
              getLabelWithValues("error.cli.no_out_no_gui",
              "--output", "--dtl-write", "--help"),
              MessageHandler.SYNTAX_FAILURE);
         }

         exit(EXIT_SYNTAX);
      }

      DatatoolDb db = null;

      try
      {
         if (inFile != null)
         {
            debug("Loading '"+inFile+"'");
            db = DatatoolDb.load(settings, inFile);

            if (settings.getOverrideInputFormat())
            {
               db.updateDefaultFormat();
            }
         }
         else
         {
            String source = loadSettings.getImportSource();

            debug("Importing data via '"+source+"'");
            db = imp.importData(source);
            db.updateDefaultFormat();
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

         Vector<SortCriteria> sort = loadSettings.getSortCriteria();

         if (sort != null)
         {
            debug("sorting");

            db.setSortCaseSensitive(loadSettings.isCaseSensitive());
            db.setMissingSortValueAction(
              loadSettings.getMissingSortValueAction());

            db.setSortCriteria(sort);
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

         File outFile = loadSettings.getOutputFile();

         if (outFile != null)
         {
            debug("Saving '"+outFile+"'");
            db.save(outFile);
         }

         DatatoolExport dataExport = loadSettings.getDataExport();

         if (dataExport != null)
         {
            String target = loadSettings.getExportTarget();
            dataExport.exportData(db, target);
         }
      }
      catch (InvalidSyntaxException e)
      {
         getMessageHandler().error(e, MessageHandler.SYNTAX_FAILURE);
         exit(EXIT_SYNTAX);
      }
      catch (DatatoolImportException e)
      {
         getMessageHandler().error(e, MessageHandler.OPEN_FAILURE);
         exit(EXIT_IO);
      }
      catch (DatatoolExportException e)
      {
         getMessageHandler().error(e, MessageHandler.WRITE_FAILURE);
         exit(EXIT_IO);
      }
      catch (Throwable e)
      {
         getMessageHandler().error(e);
         exit(EXIT_OTHER);
      }

      debug("Completed");
      exit(0);
   }

   public void helpImportOptions()
   {
      printSyntaxItem(getMessage("syntax.import", "--import"));

      printSyntaxItem(
         getMessage("syntax.merge_import", "--merge-import"));

      printSyntaxItem(getMessage("syntax.preamble_only", "--[no]preamble-only"));

      System.out.println();

      helpTeXImportOptions();
      helpSQLImportOptions();
      helpCSVImportOptions();
      helpSpreadSheetImportOptions();
   }

   public void helpTeXImportOptions()
   {
      System.out.println(getLabel("syntax.probsoln_opts"));

      printSyntaxItem(getMessage("syntax.probsoln", "--probsoln"));

      printSyntaxItem(getMessage("syntax.merge_probsoln", "--merge-probsoln"));

      System.out.println();

   }

   public void helpCSVImportOptions()
   {
      System.out.println(getLabel("syntax.csv_opts"));

      printSyntaxItem(getMessage("syntax.csv", "--csv"));

      printSyntaxItem(getMessage("syntax.merge_csv", "--merge-csv"));

      printSyntaxItem(
        getMessage("syntax.tsv_sep", "--tab-sep", "--csv-sep"));

      String sep = new String(Character.toChars(settings.getSeparator()));

      if (sep.equals("\t"))
      {
         sep = "[TAB]";
      }

      printSyntaxItem(getMessage("syntax.csv_sep", "--csv-sep", sep, "--sep"));

      printSyntaxItem(getMessage("syntax.csv_delim", "--csv-delim", 
        new String(Character.toChars(settings.getDelimiter())), "--delim"));

      printSyntaxItem(getMessage("syntax.csv_header",
        "--[no]csv-header",
        (settings.hasCSVHeader()?"--csv-header":"--nocsv-header"),
        "--[no]csvheader"));

      CsvBlankOption blankOpt = settings.getCsvBlankOption();

      printSyntaxItem(getMessage("syntax.csv_empty_rows",
        "--csv-empty-rows", blankOpt.getName()));

      printSyntaxItem(getMessage("syntax.skipemptyrows",
          "--csv-skip-empty-rows",
          "--csv-empty-rows "+CsvBlankOption.IGNORE.getName()));

      printSyntaxItem(getMessage("syntax.noskipemptyrows",
          "--nocsv-skip-empty-rows",
          "--csv-empty-rows "+CsvBlankOption.EMPTY_ROW));

      printSyntaxItem(getMessage("syntax.csv_escape_chars",
        "--csv-escape-chars",
        settings.getEscapeCharsOption()));

      printSyntaxItem(getMessage("syntax.csv_escape",
        "--[no]csv-escape", "--csv-escape-chars"));

      printSyntaxItem(getMessage("syntax.csv_skiplines", "--csv-skiplines"));

      printSyntaxItem(getMessage("syntax.csv_strictquotes", "--[no]csv-strictquotes"));

      printSyntaxItem(getMessage("syntax.csv_encoding", "--csv-encoding", "--csvencoding"));

      System.out.println();
   }

   public void helpSpreadSheetImportOptions()
   {
      printSyntaxItem(getMessage("syntax.xls_opts"));

      printSyntaxItem(getMessage("syntax.xlsx", "--xlsx"));

      if (allowsXLS)
      {
         printSyntaxItem(getMessage("syntax.xls", "--xls"));
      }

      printSyntaxItem(getMessage("syntax.merge_xlsx", "--merge-xlsx"));

      if (allowsXLS)
      {
         printSyntaxItem(getMessage("syntax.merge_xls", "--merge-xls"));
      }

      System.out.println();

      printSyntaxItem(getMessage("syntax.ods_opts"));

      printSyntaxItem(getMessage("syntax.ods", "--ods"));
      printSyntaxItem(getMessage("syntax.merge_ods", "--merge-ods"));
      System.out.println();

      printSyntaxItem(getMessage("syntax.xlsods_opts"));
      printSyntaxItem(getMessage("syntax.sheet", "--sheet"));
      System.out.println();

   }

   public void helpSQLImportOptions()
   {
      if (allowsSQL)
      {
         printSyntaxItem(getMessage("syntax.sql_opts"));

         printSyntaxItem(getMessage("syntax.sql", "--sql"));

         printSyntaxItem(getMessage("syntax.merge_sql", "--merge-sql"));

         printSyntaxItem(getMessage("syntax.sql_db", "--sqldb"));

         printSyntaxItem(getMessage("syntax.sql_prefix",
           "--sqlprefix", settings.getSqlPrefix()));

         printSyntaxItem(getMessage("syntax.sql_port",
           "--sqlport", ""+settings.getSqlPort()));

         printSyntaxItem(getMessage("syntax.sql_host",
           "--sqlhost", settings.getSqlHost()));

         printSyntaxItem(getMessage("syntax.sql_user", "--sqluser"));

         printSyntaxItem(getMessage("syntax.sql_password",
           "--sqlpassword"));

         printSyntaxItem(getMessage("syntax.sql_wipepassword",
           "--[no]wipepassword", 
           (settings.isWipePasswordEnabled()?
              "--wipepassword":"--nowipepassword")));

         printSyntaxItem(getMessage("syntax.sql_missing_console_action",
           "--missing-console-action"));

         System.out.println();
      }

   }

   public String getAboutInfo(boolean html)
   {
      String par = html ? "<p>" : String.format("%n%n");
      String nl = html ? "<br>" : String.format("%n");

      StringBuilder builder = new StringBuilder();

      if (allowsSQL)
      {
         String url = String.format("%s%s:%d",
           settings.getSqlPrefix(),
           settings.getSqlHost(),
           settings.getSqlPort()
         );

         int found = 0;

         for (Enumeration<Driver> en = DriverManager.getDrivers();
              en.hasMoreElements(); )
         {
            builder.append(par);

            Driver driver = en.nextElement();
            String dbInfo = null;

            int majorVersion = driver.getMajorVersion();
            int minorVersion = driver.getMinorVersion();

            try
            {
               if (driver.acceptsURL(url))
               {
                  dbInfo = getHelpLib().getMessage(
                    "about.database_driver.supports",
                     driver.getClass().getName(),
                     majorVersion, minorVersion, url);
               }
            }
            catch (SQLException e)
            {// ignore
            }

            if (dbInfo == null)
            {
               dbInfo = getHelpLib().getMessage(
                 "about.database_driver", driver.getClass().getName(),
                  majorVersion, minorVersion);
            }

            if (html)
            {
               dbInfo = TeXJavaHelpLib.encodeHTML(dbInfo, false);
            }

            builder.append(dbInfo);

            found++;
         }

         if (found == 0)
         {
            builder.append(par);

            String dbInfo = getLabel("about.no_database_drivers");

            if (html)
            {
               dbInfo = TeXJavaHelpLib.encodeHTML(dbInfo, false);
            }

            builder.append(dbInfo);
         }
      }
      else
      {
         builder.append(par);

         String dbInfo = getLabel("about.no_database_support");

         if (html)
         {
            dbInfo = TeXJavaHelpLib.encodeHTML(dbInfo, false);
         }

         builder.append(dbInfo);
      }

      return getHelpLib().getAboutInfo(html,
        TeXJavaHelpLib.VERSION,
        TeXJavaHelpLib.VERSION_DATE,
        String.format(
         "Copyright (C) %s Nicola L. C. Talbot (%s)",
          COPYRIGHT_YEAR,
          getHelpLib().getInfoUrl(false, "www.dickimaw-books.com")),
         TeXJavaHelpLib.LICENSE_GPL3,
         true, 
         builder.toString()
      );
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

   public String getLabelWithAlt(String label, String alt)
   {
      return getMessageWithFallback(label, alt);
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

   public String getLabelWithValues(String label, Object... values)
   {
      return getMessage(label, values);
   }

   public static void exit(DatatoolTk datatooltk, int exitCode,
     Throwable e, String noAppMessage, String appMessage, int msgCode,
     boolean stackTrace)
   {
      if (datatooltk == null)
      {
         System.err.format("%s: %s: %s", APP_NAME, noAppMessage, e.getMessage());

         if (stackTrace && e != null)
         {
            e.printStackTrace();
         }

         System.exit(exitCode);
      }
      else
      {
         MessageHandler msgHandler = datatooltk.getMessageHandler();

         if (msgHandler == null)
         {
            System.err.format("%s: %s: %s", APP_NAME, noAppMessage, e.getMessage());

            if (stackTrace && e != null)
            {
               e.printStackTrace();
            }
         }
         else
         {
            msgHandler.error(null, appMessage, e, msgCode);
         }

         datatooltk.exit(exitCode);
      }
   }

   public void exit(int exitCode)
   {
      MessageHandler msgHandler = getMessageHandler();

      if (msgHandler != null)
      {
         msgHandler.closeLogFile();
      }

      System.exit(exitCode);
   }

   public static final String APP_NAME = "datatooltk";
   public static final String VERSION = "1.9.20260201";
   public static final String VERSION_DATE = "2026-02-01";
   public static final String START_COPYRIGHT_YEAR = "2014";
   public static final String COPYRIGHT_YEAR
    = START_COPYRIGHT_YEAR+"-"+VERSION_DATE.substring(0,4);

   protected LoadSettings loadSettings;

   private int noConsoleAction = ConsolePasswordReader.NO_CONSOLE_GUI;

   protected DatatoolSettings settings;

   protected boolean allowsGUI, allowsSQL, allowsXLS;
   protected boolean isBatchMode = true;

   public static final int EXIT_SYNTAX = TeXJavaHelpLibAppAdapter.EXIT_SYNTAX;//1
   public static final int EXIT_IO = TeXJavaHelpLibAppAdapter.EXIT_IO;//2
   public static final int EXIT_USER_FORCED = 8;
   public static final int EXIT_OTHER = 255;
}
