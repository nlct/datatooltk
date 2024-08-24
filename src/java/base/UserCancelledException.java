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
package com.dickimawbooks.datatooltk.base;

import com.dickimawbooks.texjavahelplib.MessageSystem;

/**
 * Exception thrown when user cancels a process.
 */
public class UserCancelledException extends InterruptedException
{
   private UserCancelledException()
   {
      this("Cancelled");
   }

   public UserCancelledException(MessageHandler messageHandler)
   {
      this(messageHandler.getLabel("message.cancelled"));
   }

   public UserCancelledException(MessageSystem messageSystem)
   {
      this(messageSystem.getMessage("message.cancelled"));
   }

   public UserCancelledException(Exception e)
   {
      this(e.getMessage());
      initCause(e);
   }

   public UserCancelledException(String message)
   {
      super(message);
   }
}
