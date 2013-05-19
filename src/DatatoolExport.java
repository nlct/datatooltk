package com.dickimawbooks.datatooltk;

import java.io.File;
import java.io.IOException;

public interface DatatoolExport
{
   public void exportData(DatatoolDb db, String target)
     throws DatatoolExportException;
}
