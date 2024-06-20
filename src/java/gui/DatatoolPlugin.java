/*
    Copyright (C) 2013 Nicola L.C. Talbot
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
package com.dickimawbooks.datatooltk.gui;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import com.dickimawbooks.datatooltk.*;

/**
 * Class representing a plugin.
 */
public class DatatoolPlugin implements Runnable
{
   public DatatoolPlugin(File pluginFile, MessageHandler messageHandler)
   {
      this.pluginFile = pluginFile;
      this.messageHandler = messageHandler;

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
            messageHandler.getLabel("error.plugin.no_perl"));
      }

      this.dbPanel = dbPanel;

      Thread thread = new Thread(this);

      thread.start();
      thread = null;
   }

   public void run()
   {
      BufferedReader reader = null;
      PrintWriter writer = null;

      try
      {
         File mainPluginDir = 
           new File(DatatoolTk.class.getResource("/resources/plugins").toURI());

         ProcessBuilder pb = new ProcessBuilder(perl,
           "-I", mainPluginDir.getAbsolutePath(), pluginFile.getName());
         pb.directory(pluginFile.getParentFile());

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
               messageHandler.getLabelWithValues("error.plugin.exit_code", 
                 ""+exitCode, errMess));
         }

         if (xml.length() > 0)
         {
            parseResult(xml);
         }
      }
      catch (Exception e)
      {
         messageHandler.error(dbPanel, e);
      }

   }

   private void writeData(PrintWriter writer)
   {
      DatatoolTk datatooltk = messageHandler.getDatatoolTk();

      writer.format("<datatooltkplugin selectedrow=\"%d\"",
        dbPanel.getModelSelectedRow());
      writer.format(" selectedcolumn=\"%d\"",
        dbPanel.getModelSelectedColumn());
      writer.format(" name=\"%s\"", encodeXml(dbPanel.getName()));
      writer.format(" dict=\"%s\"", dbPanel.getDatatoolGUI().getPluginDictionaryUrl());
      writer.format(" resources=\"%s\"", 
         DatatoolTk.class.getResource("/resources/"));

      writer.println(">");

      writer.println("<headers>");

      int numCols = dbPanel.getColumnCount();

      for (int i = 0; i < numCols; i++)
      {
         DatatoolHeader header = dbPanel.db.getHeader(i);

         writer.println("<header>");
         writer.println("<label>"+encodeXml(header.getKey())+"</label>");
         writer.println("<title>"+encodeXml(header.getTitle())+"</title>");
         writer.println("<type>"+header.getDatumType().getValue()+"</type>");
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
            writer.println("<entry>"
              +encodeXml(row.get(j).toString())+"</entry>");
         }

         writer.println("</row>");
      }

      writer.println("</rows>");

      writer.println("</datatooltkplugin>");
   }

   private String encodeXml(String string)
   {
      return string
         .replaceAll("\\&", "&amp;").replaceAll("\"", "&quot;")
         .replaceAll("<", "&lt;").replaceAll(">", "&gt;")
         .replaceAll("\\\\DTLpar ", "<br/><br/>").replaceAll("\n", "<br/>");
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

      PluginHandler handler = new PluginHandler(dbPanel, name);
      xr.setContentHandler(handler);
      xr.setErrorHandler(messageHandler);

      StringReader reader = new StringReader(xml.toString());

      try
      {
         xr.parse(new InputSource(reader));
      }
      finally
      {
         reader.close();
         dbPanel.cancelCompoundEdit();
      }
   }

   private File pluginFile;
   private String name, perl;

   private DatatoolDbPanel dbPanel;

   private MessageHandler messageHandler;
}

class PluginHandler extends DefaultHandler
{
   public PluginHandler(DatatoolDbPanel panel, String name)
   {
      super();
      this.dbPanel = panel;
      this.pluginName = name;
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
         currentBuffer.append(String.format("%n"));
      }
      else if (localName.equals("datatooltk"))
      {
         dbPanel.startCompoundEdit(
            getMessageHandler().getLabelWithValues(
               "undo.plugin_action", pluginName));
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
         else if (currentAction.equals("remove"))
         {
            dbPanel.removeRow(getCurrentIndex());
         }

         currentRow = null;
         currentValue = null;
         currentAction = null;
      }
      else if (localName.equals("entry"))
      {
         String text = currentBuffer.toString().replaceAll(
           "(\\n\\s*\\n)+", "\\\\DTLpar ");

         currentRow.add(Datum.valueOf(text, dbPanel.getSettings()));
         currentBuffer = null;
      }
      else if (localName.equals("datatooltk"))
      {
         dbPanel.commitCompoundEdit();
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

   public MessageHandler getMessageHandler()
   {
      return dbPanel.getMessageHandler();
   }

   private DatatoolDbPanel dbPanel;
   private ArrayDeque<String> stack;

   private DatatoolRow currentRow;
   private String currentAction;
   private String currentValue;

   private StringBuffer currentBuffer;

   private String pluginName;
}
