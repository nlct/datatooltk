/*
    Copyright (C) 2017-2024 Nicola L.C. Talbot
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

import java.util.logging.ErrorManager;
import java.io.File;
import java.nio.charset.Charset;

import java.awt.Component;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import com.dickimawbooks.texparserlib.*;
import com.dickimawbooks.datatooltk.gui.DatatoolGuiResources;

public class DatatoolTeXApp extends TeXAppAdapter 
{
   public DatatoolTeXApp(MessageHandler messageHandler)
   {
      super();
      this.messageHandler = messageHandler;
   }

   @Override
   public Charset getDefaultCharset()
   {
      Charset charset = Charset.defaultCharset();

      String encoding = messageHandler.getSettings().getTeXEncoding();

      if (encoding != null)
      {
         try
         {
            charset = Charset.forName(encoding);
         }
         catch (Exception e)
         {
            messageHandler.warning(e);
         }
      }

      return charset;
   }

   @Override
   public String getApplicationName()
   {
      return DatatoolTk.APP_NAME;
   }

   @Override
   public String getApplicationVersion()
   {
      return DatatoolTk.APP_VERSION;
   }

   @Override
   public String getMessage(String label, Object... params)
   {
      return messageHandler.getLabelWithValues(label, params);
   }

   @Override
   public void message(String text)
   {
      messageHandler.message(text);
   }

   @Override
   public void warning(TeXParser parser, String message)
   {
      messageHandler.warning(parser, message);
   }

   @Override
   public void error(Exception e)
   {
      if (e instanceof TeXSyntaxException)
      {
         messageHandler.error(((TeXSyntaxException)e).getMessage(this));
      }
      else
      {
         messageHandler.error(e);
      }
   }

   @Override
   public void progress(int percentage)
   {
      messageHandler.progress(percentage);
   }

   private MessageHandler messageHandler;
}
