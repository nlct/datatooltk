/*
    Copyright (C) 2013-2024 Nicola L.C. Talbot
    www.dickimaw-books.com

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package com.dickimawbooks.datatooltk;

import java.io.File;
import java.util.Vector;

import com.dickimawbooks.texparserlib.latex.datatool.IOSettings;

import com.dickimawbooks.datatooltk.io.DatatoolImport;
import com.dickimawbooks.datatooltk.io.DatatoolExport;

public class LoadSettings
{
   public LoadSettings(DatatoolSettings settings)
   {
      this.settings = settings;
      settings.setLoadSettings(this);
   }

   public boolean hasOutputAction()
   {
      return outFile != null || exp != null;
   }

   public boolean hasInputAction()
   {
      return inFile != null || imp != null || mergeKey != null
           || dbname != null || sort != null || doShuffle
           || filterInfo != null || truncate > -1;
   }

   public void setOutputFile(String filename)
   {
      if (filename == null || filename.isEmpty())
      {
         outFile = null;
      }
      else
      {
         outFile = new File(filename);
      }
   }

   public File getOutputFile()
   {
      return outFile;
   }

   public void setInputFile(String filename)
   {
      if (filename == null || filename.isEmpty())
      {
         inFile = null;
      }
      else
      {
         inFile = new File(filename);
      }
   }

   public File getInputFile()
   {
      return inFile;
   }

   public void setMergeFile(String filename)
   {
      if (filename == null || filename.isEmpty())
      {
         mergeFile = null;
      }
      else
      {
         mergeFile = new File(filename);
      }
   }

   public void setMergeFile(File file)
   {
      mergeFile = file;
   }

   public File getMergeFile()
   {
      return mergeFile;
   }

   public void setDataExport(DatatoolExport dataExport,
     String exportTarget)
   {
      setDataExport(dataExport);
      setExportTarget(exportTarget);
   }

   public void setDataExport(DatatoolExport dataExport)
   {
      exp = dataExport;
   }

   public DatatoolExport getDataExport()
   {
      return exp;
   }

   public void setExportTarget(String exportTarget)
   {
      target = exportTarget;
   }

   public String getExportTarget()
   {
      return target;
   }

   public void setDataImport(DatatoolImport dataImport,
     String importSource)
   {
      setDataImport(dataImport);
      setImportSource(importSource);
   }

   public void setDataImport(DatatoolImport dataImport)
   {
      imp = dataImport;
   }

   public DatatoolImport getDataImport()
   {
      return imp;
   }

   public void setIOSettings(IOSettings ioSettings)
   {
      this.ioSettings = ioSettings;
   }

   public IOSettings getIOSettings()
   {
      return ioSettings;
   }

   public boolean hasIOSettings()
   {
      return ioSettings != null;
   }

   public void setMergeImport(DatatoolImport dataImport)
   {
      mergeImport = dataImport;
   }

   public DatatoolImport getMergeImport()
   {
      return mergeImport;
   }

   public void setImportSource(String impSource)
   {
      if ("".equals(impSource))
      {
         source = null;
      }
      else
      {
         source = impSource;
      }
   }

   public String getImportSource()
   {
      return source;
   }

   public void setMergeImportSource(String impSource)
   {
      if ("".equals(impSource))
      {
         mergeImportSource = null;
      }
      else
      {
         mergeImportSource = impSource;
      }
   }

   public String getMergeImportSource()
   {
      return mergeImportSource;
   }

   public void setMergeKey(String key)
   {
      mergeKey = key;
   }

   public String getMergeKey()
   {
      return mergeKey;
   }

   public void setSort(String sortValue)
   {
      if ("".equals(sortValue))
      {
         sort = null;
      }
      else
      {
         sort = sortValue;
      }

      if (sort == null) return;

      sortAscend = true;

      int codePoint = sort.codePointAt(0);

      if (codePoint == '+')
      {
         sortAscend = true;
         sort = sort.substring(1);
      }
      else if (codePoint == '-')
      {
         sortAscend = false;
         sort = sort.substring(1);
      }
   }

   public String getSort()
   {
      return sort;
   }

   public void setCaseSensitive(boolean isCase)
   {
      isCaseSensitive = isCase;
   }

   public boolean isCaseSensitive()
   {
      return isCaseSensitive;
   }

   public boolean isAscending()
   {
      return sortAscend;
   }

   public void setShuffle(boolean doShuffle)
   {
      this.doShuffle = doShuffle;
   }

   public boolean isShuffleOn()
   {
      return doShuffle;
   }

   public void addFilterInfo(FilterInfo info)
   {
      if (filterInfo == null)
      {
         filterInfo = new Vector<FilterInfo>();
      }

      filterInfo.add(info);
   }

   public Vector<FilterInfo> getFilterInfo()
   {
      return filterInfo;
   }

   public void setFilterOp(boolean filterOr)
   {
      this.filterOr = filterOr;
   }

   public boolean isFilterOr()
   {
      return filterOr;
   }

   public void setFilterInclude(boolean filterInclude)
   {
      this.filterInclude = filterInclude;
   }

   public boolean isFilterInclude()
   {
      return filterInclude;
   }

   public void setTruncate(int truncate)
   {
      this.truncate = truncate;
   }

   public int getTruncate()
   {
      return truncate;
   }

   public void setDbName(String name)
   {
      dbname = name;
   }

   public String getDbName()
   {
      return dbname;
   }

   public DatatoolSettings getMainSettings()
   {
      return settings;
   }

   public void setRemoveColumnList(String list)
   {
      removeColumnList = list;
   }

   public String getRemoveColumnList()
   {
      return removeColumnList;
   }

   public void setRemoveExceptColumnList(String list)
   {
      keepColumnList = list;
   }

   public String getRemoveExceptColumnList()
   {
      return keepColumnList;
   }

   private File inFile=null, mergeFile=null, outFile=null;
   private DatatoolImport imp=null, mergeImport=null;
   private String source=null, mergeImportSource=null, mergeKey=null;
   private String dbname=null, sort = null;
   private boolean sortAscend=true, isCaseSensitive=false;
   private boolean doShuffle=false;
   private Vector<FilterInfo> filterInfo=null;
   private boolean filterOr=true, filterInclude=true;
   private int truncate = -1;

   private String removeColumnList = null, keepColumnList = null;

   private DatatoolExport exp=null;
   private String target = null;

   private IOSettings ioSettings = null;

   private DatatoolSettings settings;
}
