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
      PrintWriter writer = null;

      try
      {
         Process process = pb.start();

         writer = new PrintWriter(process.getOutputStream());

         writer.println(dbPanel.getRowCount());
         writer.println(dbPanel.getColumnCount());
         writer.println(dbPanel.getSelectedRow());
         writer.println(dbPanel.getSelectedColumn());

         writer.println("--EOF--");

         writer.close();

         reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));


         String line;

         while ((line = reader.readLine()) != null)
         {
         }

         reader.close();

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


   private File pluginFile;
   private String name, perl;

   private DatatoolDbPanel dbPanel;

}
