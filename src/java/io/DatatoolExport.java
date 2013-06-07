package com.dickimawbooks.datatooltk.io;

import java.io.File;
import java.io.IOException;

import com.dickimawbooks.datatooltk.DatatoolDb;

public interface DatatoolExport
{
   public void exportData(DatatoolDb db, String target)
     throws DatatoolExportException;
}
