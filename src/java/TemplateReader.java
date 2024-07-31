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

import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderAdapter;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.texjavahelplib.MessageSystem;

/**
 * Reader for parsing XML data describing a database template.
 * Template structure:
<pre>
&lt;datatooltktemplate&gt;
  &lt;header&gt;
    &lt;label&gt;<em>column-label</em>&lt;/label&gt;
    &lt;title&gt;<em>column-title</em>&lt;/title&gt;
    &lt;type&gt;<em>data-type</em>&lt;/type&gt;
  &lt;/header&gt;
  &lt;header&gt;
    &lt;label&gt;<em>column-label</em>&lt;/label&gt;
    &lt;title&gt;<em>column-title</em>&lt;/title&gt;
    &lt;type&gt;<em>data-type</em>&lt;/type&gt;
  &lt;/header&gt;
...
&lt;/datatooltktemplate&gt;
</pre>
 * The title and type are optional.
 */
public class TemplateReader extends XMLReaderAdapter
{
   public TemplateReader(DatatoolDb db)
   throws SAXException
   {
      super();
      this.db = db;
      this.messageSystem = db.getSettings().getHelpLib().getMessageSystem();
   }

   @Override
   public void startElement(String uri, String localName, String qName,
     Attributes attrs)
     throws SAXException
   {
      super.startElement(uri, localName, qName, attrs);

      if ("datatooltktemplate".equals(qName))
      {
         if (inRoot)
         {
            throw new SAXException(messageSystem.getMessage(
             "error.template.nested_tag", qName));
         }

         inRoot = true;
      }
      else if ("header".equals(qName))
      {
         if (!inRoot)
         {
            throw new SAXException(messageSystem.getMessage(
             "error.template.misplaced_tag", qName, "datatooltktemplate"));
         }

         if (currentHeader != null)
         {
            throw new SAXException(messageSystem.getMessage(
             "error.template.nested_tag", qName));
         }

         currentHeader = new DatatoolHeader(db);
      }
      else if ("label".equals(qName)
            || "title".equals(qName)
            || "type".equals(qName))
      {
         if (currentHeader == null)
         {
            throw new SAXException(messageSystem.getMessage(
             "error.template.misplaced_tag", qName, "header"));
         }

         if (currentBuilder != null)
         {
            throw new SAXException(messageSystem.getMessage(
             "error.template.tag_not_child", qName, "header"));
         }

         currentBuilder = new StringBuilder();
      }
      else
      {
         throw new SAXException(messageSystem.getMessageWithFallback(
          "error.xml.unknown_tag", "Unknown tag <{0}>", qName));
      }
   }

   @Override
   public void endElement(String uri, String localName, String qName)
     throws SAXException
   {
      super.endElement(uri, localName, qName);

      if ("datatooltktemplate".equals(qName))
      {
         inRoot = false;
      }  
      else if ("header".equals(qName))
      {
         if (currentHeader.getKey() == null)
         {
            throw new SAXException(messageSystem.getMessage(
             "error.template.missing_tag", "label"));
         }

         if (currentHeader.getTitle() == null)
         {
            currentHeader.setTitle(currentHeader.getKey());
         }

         db.addColumn(currentHeader);
         currentHeader = null;
      }  
      else if ("label".equals(qName))
      {
         currentHeader.setKey(currentBuilder.toString());
         currentBuilder = null;
      }
      else if ("title".equals(qName))
      {
         currentHeader.setTitle(currentBuilder.toString());
         currentBuilder = null;
      }
      else if ("type".equals(qName))
      {
         try
         {
            int type = Integer.parseInt(currentBuilder.toString());
            DatumType datumType = DatumType.toDatumType(type);
            currentHeader.setType(datumType);
         }
         catch (NumberFormatException e)
         {
            throw new SAXException(messageSystem.getMessage(
             "error.template.invalid_type", currentBuilder), e);
         }
         catch (IllegalArgumentException e)
         {
            throw new SAXException(messageSystem.getMessage(
             "error.template.invalid_type", currentBuilder), e);
         }

         currentBuilder = null;
      }
      else
      {
         throw new SAXException(messageSystem.getMessageWithFallback(
          "error.xml.unknown_end_tag",
          "Unknown end tag </{0}> found", qName));
      }
   }

   @Override
   public void characters(char[] ch, int start, int length)
      throws SAXException
   {
      super.characters(ch, start, length);

      if (currentBuilder == null)
      {
         for (int i = 0; i < length; i++)
         {
            if (!Character.isWhitespace(ch[start+i]))
            {
               throw new SAXException(
                 messageSystem.getMessageWithFallback("error.xml.unexpected_chars",
                   "Unexpected content ''{0}'' found",
                   new String(ch, start+i, length-i)));
            }
         }
      }
      else
      {
         currentBuilder.append(ch, start, length);
      }
   }

   private DatatoolDb db;

   private MessageSystem messageSystem;

   private StringBuilder currentBuilder;

   private boolean inRoot = false;

   private DatatoolHeader currentHeader;
}
