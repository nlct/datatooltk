package com.dickimawbooks.datatooltk;

public class DatatoolExportException extends Exception
{
   public DatatoolExportException(String message)
   {
      super(message);
   }

   public DatatoolExportException(String message, Exception exception)
   {
      super(message, exception);
   }
}
