package com.dickimawbooks.datatooltk.io;

import com.dickimawbooks.datatooltk.UserCancelledException;

public interface DatatoolPasswordReader
{
   public char[] requestPassword() throws UserCancelledException;
}
