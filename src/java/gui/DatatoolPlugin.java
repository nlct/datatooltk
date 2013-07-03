package com.dickimawbooks.datatooltk.gui;

import java.io.*;
import java.util.regex.*;

import com.dickimawbooks.datatooltk.*;

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

      BufferedReader reader = null;
      PrintWriter writer = null;

      try
      {
         Process process = pb.start();

         writer = new PrintWriter(process.getOutputStream());

         writeData(writer);

         writer.println("--EOF--");

         writer.close();

         reader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));

         String line;

         while ((line = reader.readLine()) != null)
         {
System.out.println(line);
         }

         reader.close();

         reader = new BufferedReader(
            new InputStreamReader(process.getErrorStream()));

         String errMess = "";

         while ((line = reader.readLine()) != null)
         {
            errMess += line+"\n";
         }

         reader.close();

         int exitCode = process.waitFor();

         if (exitCode != 0)
         {
            DatatoolGuiResources.error(dbPanel,
               DatatoolTk.getLabelWithValues("error.plugin.exit_code", 
                 ""+exitCode, errMess));
         }

      }
      catch (Exception e)
      {
         DatatoolGuiResources.error(dbPanel, e);
      }

   }

   private void writeData(PrintWriter writer)
   {
      writer.println("<datatooltkplugin"
         +" selectedrow=\""
         +dbPanel.getSelectedRow()
         +"\""
         +" selectedcolumn=\""
         +dbPanel.getSelectedColumn()
         +"\""
         +" name=\""
         +encodeXml(dbPanel.getName())
         +"\""
         +">");

      writer.println("<headers>");

      int numCols = dbPanel.getColumnCount();

      for (int i = 0; i < numCols; i++)
      {
         DatatoolHeader header = dbPanel.db.getHeader(i);

         writer.println("<header>");
         writer.println("<label>"+encodeXml(header.getKey())+"</label>");
         writer.println("<title>"+encodeXml(header.getTitle())+"</title>");
         writer.println("<type>"+header.getType()+"</type>");
         writer.println("</header>");
      }

      writer.println("</headers>");

      writer.println("<rows>");

      for (int i = 0, n = dbPanel.getRowCount(); i < n; i++)
      {
         DatatoolRow row = dbPanel.db.getRow(i);

         writer.println("<row>");

         for (int j = 0; j < numCols; j++)
         {
            writer.println("<entry>"+encodeXml(row.get(j))+"</entry>");
         }

         writer.println("</row>");
      }

      writer.println("</rows>");

      writer.println("</datatooltkplugin>");
   }

   private String encodeXml(String string)
   {
      return string.replaceAll("\\&", "&amp;").replaceAll("\"", "&quot;")
         .replaceAll("<", "&lt;").replaceAll(">", "&gt;");
   }

   private String decodeXml(String string)
   {
      return string.replaceAll("\\&gt;", ">").replaceAll("\\&lt;", "<")
         .replaceAll("\\&quot;", "\"").replaceAll("\\&amp;", "\\&");
   }

   private File pluginFile;
   private String name, perl;

   private DatatoolDbPanel dbPanel;

}
