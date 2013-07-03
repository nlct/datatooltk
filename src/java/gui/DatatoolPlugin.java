package com.dickimawbooks.datatooltk.gui;

import java.io.*;
import java.util.regex.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

public class DatatoolPlugin
{
   public DatatoolPlugin(File pluginFile)
   {
      this.pluginFile = pluginFile;

      name = pluginFile.getName();

      int index = name.lastIndexOf(".");

      if (index != -1)
      {
         name = name.substring(0, index);
      }
   }

   public String toString()
   {
      return name;
   }

   public void process(DatatoolDbPanel dbPanel)
    throws IOException,InterruptedException
   {
      String perl = dbPanel.getPerl();

      if (perl == null || perl.isEmpty())
      {
         throw new FileNotFoundException(
            DatatoolTk.getLabel("error.plugin.no_perl"));
      }

      ProcessBuilder pb = new ProcessBuilder(perl, pluginFile.getName());
      pb.directory(pluginFile.getParentFile());

      Process process = pb.start();

      BufferedReader reader = null;
      PrintWriter writer = null;

      try
      {
         reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));

         writer = new PrintWriter(process.getOutputStream());

         String line;

         while ((line = reader.readLine()) != null)
         {
            if (!processLine(line, writer, dbPanel))
            {
               break;
            }
         }
      }
      finally
      {
         if (reader != null)
         {
            reader.close();
         }

         if (writer != null)
         {
            writer.close();
         }
      }

      int exitCode = process.exitValue();

      if (exitCode != 0)
      {
         DatatoolGuiResources.error(dbPanel,
            DatatoolTk.getLabelWithValue("error.plugin.exit_code", exitCode));
      }
   }

   private boolean processLine(String line, PrintWriter writer, 
      DatatoolDbPanel dbPanel)
     throws IOException
   {
      Matcher matcher = PATTERN_PLUGIN.matcher(line);

      if (matcher.matches())
      {
         String statement = matcher.group(1);

         if (statement.equals("QUERY"))
         {
            queryStatement(matcher.group(2), writer, dbPanel);
         }
         else
         {
            return commandStatement(matcher.group(2), writer, dbPanel);
         }
      }
      else
      {
         throw new IOException(DatatoolTk.getLabelWithValue(
            "error.plugin.unknown_message", line));
      }

      return true;
   }

   private boolean commandStatement(String command, PrintWriter writer, 
      DatatoolDbPanel dbPanel)
     throws IOException
   {
      if (command.equals("EXIT"))
      {
         return false;
      }
      else
      {
         throw new IOException(DatatoolTk.getLabelWithValue(
            "error.plugin.unknown_command", command));
      }
   }

   private void queryStatement(String query, PrintWriter writer, 
      DatatoolDbPanel dbPanel)
   throws IOException
   {
      if (query.equals("ROW COUNT"))
      {
         writer.println(dbPanel.getRowCount());
      }
      else if (query.equals("COLUMN COUNT"))
      {
         writer.println(dbPanel.getColumnCount());
      }
      else if (query.equals("SELECTED ROW"))
      {
         writer.println(dbPanel.getSelectedRow());
      }
      else if (query.equals("SELECTED COLUMN"))
      {
         writer.println(dbPanel.getSelectedColumn());
      }
      else
      {
         throw new IOException(DatatoolTk.getLabelWithValue(
            "error.plugin.unknown_query", query));
      }
   }

   private File pluginFile;
   private String name;

   private static final Pattern PATTERN_PLUGIN 
      = Pattern.compile("PLUGIN (QUERY|COMMAND) ([\\w\\s]+):>>");
}
