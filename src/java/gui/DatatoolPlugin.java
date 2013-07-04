package com.dickimawbooks.datatooltk.gui;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

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

         StringBuffer xml = new StringBuffer();

         while ((line = reader.readLine()) != null)
         {
            xml.append(line);
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
            throw new IOException(
               DatatoolTk.getLabelWithValues("error.plugin.exit_code", 
                 ""+exitCode, errMess));
         }

         if (xml.length() > 0)
         {
            parseResult(xml);
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
         .replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\n", "<br/>");
   }

   private String decodeXml(String string)
   {
      return string.replaceAll("\\&gt;", ">").replaceAll("\\&lt;", "<")
         .replaceAll("\\&quot;", "\"").replaceAll("\\&amp;", "\\&");
   }

   private void parseResult(CharSequence xml)
     throws SAXException,IOException
   {
      XMLReader xr = XMLReaderFactory.createXMLReader();

      PluginHandler handler = new PluginHandler(dbPanel);
      xr.setContentHandler(handler);
      xr.setErrorHandler(dbPanel.db.getErrorHandler());

      StringReader reader = new StringReader(xml.toString());

      try
      {
         xr.parse(new InputSource(reader));
      }
      finally
      {
         reader.close();
      }
   }

   private File pluginFile;
   private String name, perl;

   private DatatoolDbPanel dbPanel;

}

class PluginHandler extends DefaultHandler
{
   public PluginHandler(DatatoolDbPanel panel)
   {
      super();
      this.dbPanel = panel;
      stack = new ArrayDeque<String>();
   }

   public void startElement(String uri, String localName, String qName,
     Attributes attrs)
     throws SAXException
   {
      stack.push(localName);

      if (localName.equals("row"))
      {
         currentRow = new DatatoolRow(dbPanel.db);

         currentAction = attrs.getValue("action");
         currentValue = attrs.getValue("value");
      }
      else if (localName.equals("entry"))
      {
         currentBuffer = new StringBuffer();
      }
      else if (localName.equals("br") && currentBuffer != null)
      {
         currentBuffer.append("\n");
      }
   }

   private int getCurrentIndex()
     throws SAXException
   {
      int index;

      try
      {
         index = Integer.parseInt(currentValue);
      }
      catch (NumberFormatException e)
      {
         throw new SAXException(e);
      }

      return index;
   }

   public void endElement(String uri, String localName, String qName)
     throws SAXException
   {
      try
      {
         stack.pop();
      }
      catch (NoSuchElementException e)
      {
         throw new SAXException(e);
      }

      if (localName.equals("row"))
      {
         if (currentAction.equals("insert"))
         {
            dbPanel.insertRow(getCurrentIndex(), currentRow);
         }
         else if (currentAction.equals("replace"))
         {
            dbPanel.replaceRow(getCurrentIndex(), currentRow);
         }
         else if (currentAction.equals("append"))
         {
            dbPanel.appendRow(currentRow);
         }

         currentRow = null;
         currentValue = null;
         currentAction = null;
      }
      else if (localName.equals("entry"))
      {
         currentRow.add(currentBuffer.toString());
         currentBuffer = null;
      }
   }

   public void characters(char[] ch, int start, int length)
      throws SAXException
   {
      String current = stack.peek();

      if (current == null) return;

      if (current.equals("entry"))
      {
         currentBuffer.append(ch, start, length);
      }
   }

   private DatatoolDbPanel dbPanel;
   private ArrayDeque<String> stack;

   private DatatoolRow currentRow;
   private String currentAction;
   private String currentValue;

   private StringBuffer currentBuffer;
}
