package com.dickimawbooks.datatooltk;

public class UserCancelledException extends InterruptedException
{
   public UserCancelledException()
   {
      this(DatatoolTk.getLabel("message.cancelled"));
   }

   public UserCancelledException(String message)
   {
      super(message);
   }
}
