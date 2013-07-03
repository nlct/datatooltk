package com.dickimawbooks.datatooltk.gui;

import java.io.*;
import java.util.regex.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

public class DatatoolPlugin implements Runnable
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
      throws IOException
   {
      perl = dbPanel.getPerl();

      if (perl == null || perl.isEmpty())
      {
         throw new FileNotFoundException(
            DatatoolTk.getLabel("error.plugin.no_perl"));
      }

      this.dbPanel = dbPanel;

      Thread thread = new Thread(this);

      thread.start();
      thread = null;
   }

   public void run()
   {
      ProcessBuilder pb = new ProcessBuilder(perl, pluginFile.getName());
      pb.directory(pluginFile.getParentFile());
      pb.redirectErrorStream(true);

      BufferedReader reader = null;
      BufferedWriter writer = null;

      try
      {
         Process process = pb.start();

         reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));

         writer = new BufferedWriter(
            new OutputStreamWriter(process.getOutputStream()));

         String line;

         while ((line = reader.readLine()) != null)
         {
            if (!processLine(line, writer))
            {
               break;
            }
         }

         reader.close();
         writer.close();

         int exitCode = process.waitFor();

         if (exitCode != 0)
         {
            DatatoolGuiResources.error(dbPanel,
               DatatoolTk.getLabelWithValue("error.plugin.exit_code", exitCode));
         }

      }
      catch (Exception e)
      {
         DatatoolGuiResources.error(dbPanel, e);
      }

   }

   private boolean processLine(String line, BufferedWriter writer)
     throws IOException
   {
      Matcher matcher = PATTERN_PLUGIN.matcher(line);

      if (matcher.matches())
      {
         String statement = matcher.group(1);

         if (statement.equals("QUERY"))
         {
            queryStatement(matcher.group(2), writer);
         }
         else
         {
            return commandStatement(matcher.group(2), writer);
         }
      }
      else
      {
         throw new IOException(DatatoolTk.getLabelWithValue(
            "error.plugin.unknown_message", line));
      }

      return true;
   }

   private boolean commandStatement(String command, BufferedWriter writer)
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

   private void queryStatement(String query, BufferedWriter writer)
   throws IOException
   {
      if (query.equals("ROW COUNT"))
      {
         writer.write(dbPanel.getRowCount()+"\n");
      }
      else if (query.equals("COLUMN COUNT"))
      {
         writer.write(dbPanel.getColumnCount()+"\n");
      }
      else if (query.equals("SELECTED ROW"))
      {
         writer.write(dbPanel.getSelectedRow()+"\n");
      }
      else if (query.equals("SELECTED COLUMN"))
      {
         writer.write(dbPanel.getSelectedColumn()+"\n");
      }
      else
      {
         throw new IOException(DatatoolTk.getLabelWithValue(
            "error.plugin.unknown_query", query));
      }

      writer.flush();
   }

   private File pluginFile;
   private String name, perl;

   private DatatoolDbPanel dbPanel;

   private static final Pattern PATTERN_PLUGIN 
      = Pattern.compile("PLUGIN (QUERY|COMMAND) ([\\w\\s]+):>>");
}
