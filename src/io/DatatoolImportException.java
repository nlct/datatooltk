package com.dickimawbooks.datatooltk.io;

public class DatatoolImportException extends Exception
{
   public DatatoolImportException(String message)
   {
      super(message);
   }

   public DatatoolImportException(String message, Exception exception)
   {
      super(message, exception);
   }

   public DatatoolImportException(Exception exception)
   {
      super(exception);
   }
}
