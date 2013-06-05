package com.dickimawbooks.datatooltk;

public class UserCancelledException extends InterruptedException
{
   public UserCancelledException()
   {
      this("Cancelled");
   }

   public UserCancelledException(String message)
   {
      super(message);
   }
}
