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
package com.dickimawbooks.datatooltk;

import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handler for parsing XML data describing a database template.
 */
public class TemplateHandler extends DefaultHandler
{
   public TemplateHandler(DatatoolDb db, String templateName)
   {
      super();
      this.db = db;
      this.templateName = templateName;
      this.messageHandler = db.getMessageHandler();

      stack = new ArrayDeque<String>();
      headerStack = new ArrayDeque<DatatoolHeader>();
   }

   public void startElement(String uri, String localName, String qName,
     Attributes attrs)
     throws SAXException
   {
      String parent = stack.peek();

      if ((localName.equals("label") || localName.equals("title")
        || localName.equals("type")) && !parent.equals("header"))
      {
         throw new SAXException(
            messageHandler.getLabelWithValues(
              "error.template.misplaced_tag",
              localName, "header"));
      }
      else if (localName.equals("header") 
         && !parent.equals("datatooltktemplate"))
      {
         throw new SAXException(
            messageHandler.getLabelWithValues(
              "error.template.misplaced_tag",
              localName, "datatooltktemplate"));
      }

      if (localName.equals("header"))
      {
         DatatoolHeader header = new DatatoolHeader(db, "","");
         headerStack.push(header);
      }

      stack.push(localName);
   }

   public void endElement(String uri, String localName, String qName)
     throws SAXException
   {
      try
      {
         String element = stack.pop();

         if (!localName.equals(element))
         {
            throw new SAXException(messageHandler.getLabelWithValues(
               "error.template.wrong_end_tag", localName));
         }

         if (localName.equals("header"))
         {
            DatatoolHeader header = headerStack.pop();

            if (header.getKey().isEmpty())
            {
               throw new SAXException(
                  messageHandler.getLabelWithValues("error.template.missing_tag",
                  "label"));
            }

            if (header.getTitle().isEmpty())
            {
               // Is there a plugin dictionary entry associated with
               // this key?

               header.setTitle
               (
                  messageHandler.getLabelWithAlt
                  (
                    "plugin."+templateName+"."+header.getKey(),
                    header.getKey()
                  )
               );
            }

            db.addColumn(header);
         }
      }
      catch (NoSuchElementException e)
      {
      }
   }

   public void characters(char[] ch, int start, int length)
      throws SAXException
   {
      String current = stack.peek();

      if (current == null) return;

      String insertion = new String(ch, start, length);

      if (current.equals("label"))
      {
         DatatoolHeader header = headerStack.peek();

         header.setKey(header.getKey()+insertion);
      }
      else if (current.equals("title"))
      {
         DatatoolHeader header = headerStack.peek();

         header.setTitle(header.getTitle()+insertion);
      }
      else if (current.equals("type"))
      {
         DatatoolHeader header = headerStack.peek();

         try
         {
            header.setType(Integer.parseInt(insertion));
         }
         catch (NumberFormatException e)
         {
            throw new SAXException(e);
         }
      }
   }

   private DatatoolDb db;

   private MessageHandler messageHandler;

   private String templateName;

   private ArrayDeque<String> stack;

   private ArrayDeque<DatatoolHeader> headerStack;
}
