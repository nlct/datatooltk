package com.dickimawbooks.datatooltk.io;

import java.io.*;
import java.util.regex.*;

import com.dickimawbooks.datatooltk.*;

public class DatatoolProbSoln implements DatatoolImport
{
   public DatatoolProbSoln(DatatoolSettings settings)
   {
      this.settings = settings;
   }

   public DatatoolDb importData(String source)
      throws DatatoolImportException
   {
      File file = new File(source);

      if (!file.exists())
      {
         throw new DatatoolImportException(
            DatatoolTk.getLabelWithValue("error.io.file_not_found",
            file.getAbsolutePath()));
      }

      File dir = file.getParentFile();

      DatatoolDb db = new DatatoolDb();
      db.setName(file.getName());

      String key = DatatoolTk.getLabel("probsoln.label");
      db.addColumn(new DatatoolHeader(key, key, DatatoolDb.TYPE_STRING));

      key = DatatoolTk.getLabel("probsoln.question");
      db.addColumn(new DatatoolHeader(key, key, DatatoolDb.TYPE_STRING));

      key = DatatoolTk.getLabel("probsoln.answer");
      db.addColumn(new DatatoolHeader(key, key, DatatoolDb.TYPE_STRING));

      PrintWriter out = null;
      BufferedReader in = null;

      try
      {
         // TeX is fiddly to parse, so get TeX to write the data out
         // in a format that's easier for Java to read.

         File texFile = File.createTempFile("dbt", ".tex", dir);
         DatatoolTk.removeFileOnExit(texFile);

         String name = texFile.getName();

         name = name.substring(0, name.lastIndexOf("."));

         File logFile = new File(dir, name+".log");
         DatatoolTk.removeFileOnExit(logFile);

         File auxFile = new File(dir, name+".aux");
         DatatoolTk.removeFileOnExit(auxFile);

         File tmpFile = new File(dir, name+".tmp");
         DatatoolTk.removeFileOnExit(tmpFile);

         out = new PrintWriter(texFile);

         out.println("\\batchmode");
         out.println("\\documentclass{article}");

         out.println("\\usepackage[usedefaultargs]{probsoln}");
         out.println("\\usepackage{etoolbox}");

         out.println("\\loadallproblems{"+
           (System.getProperty("os.name").toLowerCase().startsWith("win") ?
           file.getAbsolutePath().replaceAll("\\", "/") :
           file.getAbsolutePath())
           +"}");

         out.println("\\newwrite\\probout");
         out.println("\\immediate\\openout\\probout=\\jobname.tmp");

         out.println("\\makeatletter");
         out.println("  \\newcommand{\\writeprob}[1]{%");
         out.println("    \\immediate\\write\\probout{BEGIN PROBLEM: #1}%");
         out.println("    \\letcs\\tmp{prob@data@default@#1}%");
         out.println("    \\@onelevel@sanitize\\tmp");
         out.println("    \\immediate\\write\\probout{\\tmp}%");
         out.println("    \\immediate\\write\\probout{END PROBLEM: #1}%");
         out.println("  }");
         out.println("\\makeatother");

         out.println("\\begin{document}");
         out.println("\\foreachproblem{\\writeprob\\thisproblemlabel}");
         out.println("\\immediate\\closeout\\probout");
         out.println("\\end{document}");

         out.close();
         out = null;

         // Now run latex on this file

         ProcessBuilder pb = new ProcessBuilder(settings.getLaTeX(), name);
         pb.directory(dir);

         Process p = pb.start();

         int exitCode = p.waitFor();

         if (exitCode != 0)
         {
            throw new IOException(DatatoolTk.getLabelWithValues(
               "error.process_failed",
                new String[]
                {
                   settings.getLaTeX()+" \""+name+"\"",
                   dir.getAbsolutePath(),
                   ""+exitCode
                }));
         }

         in = new BufferedReader(new FileReader(tmpFile));

         String line;
         int linenum = 0;
         int rowIdx = 0;

         while ((line = in.readLine()) != null)
         {
            linenum++;
            Matcher m = PATTERN_BEGIN_PROB.matcher(line);

            if (!m.matches())
            {
               DatatoolTk.debug(DatatoolTk.getLabelWithValues(
                 "error.missing_begin_prob", ""+linenum,
                 tmpFile.getAbsolutePath()));
               throw new DatatoolImportException(
                 DatatoolTk.getLabel("error.internal.import_probsoln_failed"));
            }

            String label = m.group(1);

            StringBuilder buffer = new StringBuilder();

            while ((line = in.readLine()) != null)
            {
               linenum++;
               m = PATTERN_END_PROB.matcher(line);

               if (m.matches())
               {
                  if (!m.group(1).equals(label))
                  {
                     DatatoolTk.debug(DatatoolTk.getLabelWithValues(
                       "error.mismatched_end_prob",
                       new String[] {label, m.group(1), ""+linenum,
                        tmpFile.getAbsolutePath()}));

                     throw new DatatoolImportException(
                       DatatoolTk.getLabel("error.internal.import_probsoln_failed"));
                  }

                  break;
               }

               line = line.replaceAll("\\\\par ", "\\\\DTLpar ");
               line = line.replaceAll("\n\n+", "\\\\DTLpar ");
               line = line.replaceAll("([^\\\\])#(\\d{1})", "$1##$2");

               buffer.append(line);
            }

            String value = new String(buffer);

            String question = value.replaceAll(
               "\\\\begin *\\{onlysolution\\}.*?\\\\end *\\{onlysolution\\}", "");

            String answer = value.replaceAll(
               "\\\\begin *\\{onlyproblem\\}.*?\\\\end *\\{onlyproblem\\}", "");

            question = question.replaceAll("\\\\(begin|end) *\\{onlyproblem\\}", "");
            answer = answer.replaceAll("\\\\(begin|end) *\\{onlysolution\\}", "");

            DatatoolRow row = new DatatoolRow(db, 3);
            row.addCell(0, label);
            row.addCell(1, question);
            row.addCell(2, question.equals(answer) ? "" : answer);

            db.insertRow(rowIdx, row);

            rowIdx++;
         }

         in.close();
         in = null;
      }
      catch (IOException e)
      {
         throw new DatatoolImportException(e);
      }
      catch (InterruptedException e)
      {
         throw new DatatoolImportException(e);
      }
      finally
      {
         if (out != null)
         {
            out.close();
         }

         if (in != null)
         {
            try
            {
               in.close();
            }
            catch (IOException e)
            {
               throw new DatatoolImportException(e);
            }
         }
      }

      return db;
   }

   private DatatoolSettings settings;

   private static final Pattern PATTERN_BEGIN_PROB
      = Pattern.compile("BEGIN PROBLEM: (.*)");
   private static final Pattern PATTERN_END_PROB
      = Pattern.compile("END PROBLEM: (.*)");
}
