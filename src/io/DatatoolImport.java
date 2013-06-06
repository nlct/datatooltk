package com.dickimawbooks.datatooltk.io;

import java.io.File;
import java.io.IOException;

import com.dickimawbooks.datatooltk.DatatoolDb;

public interface DatatoolImport
{
   public DatatoolDb importData(String source)
    throws DatatoolImportException;
}
