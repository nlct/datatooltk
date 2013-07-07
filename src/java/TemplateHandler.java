package com.dickimawbooks.datatooltk;

import java.util.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

public class TemplateHandler extends DefaultHandler
{
   public TemplateHandler(DatatoolDb db, String templateName)
   {
      super();
      this.db = db;
      this.templateName = templateName;
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
            DatatoolTk.getLabelWithValues(
              "error.template.misplaced_tag",
              localName, "header"));
      }
      else if (localName.equals("header") 
         && !parent.equals("datatooltktemplate"))
      {
         throw new SAXException(
            DatatoolTk.getLabelWithValues(
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
            throw new SAXException(DatatoolTk.getLabelWithValue(
               "error.template.wrong_end_tag", localName));
         }

         if (localName.equals("header"))
         {
            DatatoolHeader header = headerStack.pop();

            if (header.getKey().isEmpty())
            {
               throw new SAXException(
                  DatatoolTk.getLabelWithValue("error.template.missing_tag",
                  "label"));
            }

            if (header.getTitle().isEmpty())
            {
               // Is there a plugin dictionary entry associated with
               // this key?

               header.setTitle
               (
                  DatatoolTk.getLabelWithAlt
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

   private String templateName;

   private ArrayDeque<String> stack;

   private ArrayDeque<DatatoolHeader> headerStack;
}
