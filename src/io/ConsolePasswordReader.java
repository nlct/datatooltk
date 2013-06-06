package com.dickimawbooks.datatooltk.io;

import java.io.Console;

import com.dickimawbooks.datatooltk.DatatoolTk;
import com.dickimawbooks.datatooltk.UserCancelledException;

public class ConsolePasswordReader implements DatatoolPasswordReader
{
   public char[] requestPassword()
     throws UserCancelledException
   {
      Console cons;
      char[] passwd;

      if ((cons = System.console()) != null
       && (passwd = cons.readPassword("%s",
           DatatoolTk.getLabel("password.prompt"))) != null)
      {
         return passwd;
      }

      throw new UserCancelledException();
   }
}
