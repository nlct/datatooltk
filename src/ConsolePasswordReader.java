package com.dickimawbooks.datatooltk;

import java.io.Console;

public class ConsolePasswordReader implements DatatoolPasswordReader
{
   public char[] requestPassword()
   {
      Console cons;
      char[] passwd;

      if ((cons = System.console()) != null
       && (passwd = cons.readPassword("[%s]", "Password:")) != null)
      {
         return passwd;
      }

      return null;
   }
}
