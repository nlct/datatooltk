package com.dickimawbooks.datatooltk;

import java.io.File;
import java.io.IOException;

public interface DatatoolImport
{
   public DatatoolDb importData(String source)
    throws DatatoolImportException;
}
