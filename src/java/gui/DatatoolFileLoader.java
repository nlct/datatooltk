/*
    Copyright (C) 2013 Nicola L.C. Talbot
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
package com.dickimawbooks.datatooltk.gui;

import java.io.IOException;
import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import java.util.Vector;
import java.util.List;

import java.awt.Cursor;
import javax.swing.SwingWorker;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import com.dickimawbooks.datatooltk.DatatoolDb;
import com.dickimawbooks.datatooltk.DataFilter;
import com.dickimawbooks.datatooltk.FilterInfo;
import com.dickimawbooks.datatooltk.LoadSettings;
import com.dickimawbooks.datatooltk.io.InvalidSyntaxException;
import com.dickimawbooks.datatooltk.io.DatatoolImport;
import com.dickimawbooks.datatooltk.io.DatatoolImportException;

public class DatatoolFileLoader extends SwingWorker<DatatoolDb,String>
  implements ProgressMonitor,PropertyChangeListener
{
   private final DatatoolGUI gui;
   private File file=null;
   private LoadSettings loadSettings = null;
   private boolean modified = false;

   public DatatoolFileLoader(DatatoolGUI gui, File file)
   {
      this.gui = gui;
      this.file = file;

      init();
   }

   public DatatoolFileLoader(DatatoolGUI gui,
     LoadSettings loadSettings)
   {
      this.gui = gui;
      this.loadSettings = loadSettings;

      init();
   }

   private void init()
   {
      gui.startProgressMessages(this);
      addPropertyChangeListener(this);
   }

   @Override
   public DatatoolDb doInBackground() 
    throws IOException, DatatoolImportException
   {
      if (loadSettings == null)
      {
         publish(gui.getMessageHandler().getLabelWithValues(
           "progress.loading", file));
         return DatatoolDb.load(gui.getSettings(), file);
      }

      DatatoolDb db = null;
      file = loadSettings.getInputFile();
      DatatoolImport imp = loadSettings.getDataImport();

      if (file != null)
      {
         publish(gui.getMessageHandler().getLabelWithValues(
           "progress.loading", file));
         db = DatatoolDb.load(loadSettings.getMainSettings(), file);
      }
      else if (imp != null)
      {
         String source = loadSettings.getImportSource();

         publish(gui.getMessageHandler().getLabelWithValues(
           "progress.importing", source));
         db = imp.importData(source);
      }

      File mergeFile = loadSettings.getMergeFile();
      String mergeImportSource = loadSettings.getMergeImportSource();
      String mergeKey = loadSettings.getMergeKey();
      DatatoolImport mergeImport = loadSettings.getMergeImport();

      if (db == null)
      {
         if (mergeFile != null)
         {
            publish(gui.getMessageHandler().getLabelWithValues(
              "progress.merging", mergeFile));
            db = DatatoolDb.load(loadSettings.getMainSettings(), mergeFile);
            file = mergeFile;
            mergeFile = null;
            mergeKey = null;
         }
         else if (mergeImportSource != null)
         {
            publish(gui.getMessageHandler().getLabelWithValues(
              "progress.merging", mergeImportSource));
            db = mergeImport.importData(mergeImportSource);
            mergeImportSource = null;
            mergeKey = null;
         }
      }

      if (db == null) return null;

      String dbname = loadSettings.getDbName();

      if (dbname != null)
      {
         db.setName(dbname);
         modified = true;
      }

      DatatoolDb mergeDb = null;

      if (mergeFile != null)
      {
         publish(gui.getMessageHandler().getLabelWithValues(
           "progress.merging", mergeFile));
         mergeDb = DatatoolDb.load(loadSettings.getMainSettings(), mergeFile);
      }
      else if (mergeImportSource != null)
      {
         publish(gui.getMessageHandler().getLabelWithValues(
           "progress.merging", mergeImportSource));
         mergeDb = mergeImport.importData(mergeImportSource);
      }

      if (mergeDb != null)
      {
         try
         {
            db.merge(mergeDb, mergeKey);
         }
         catch (InvalidSyntaxException e)
         {
            gui.getMessageHandler().warning(e);
         }

         modified = true;
      }

      String sort = loadSettings.getSort();

      if (sort != null)
      {
         publish(gui.getMessageHandler().getLabel(
           "progress.sorting"));

         db.setSortCaseSensitive(loadSettings.isCaseSensitive());

         int colIndex = db.getColumnIndex(sort);

         if (colIndex == -1)
         {
            throw new IOException(
               gui.getMessageHandler().getLabelWithValues("error.syntax.unknown_field",
               sort));
         }

         db.setSortColumn(colIndex);
         db.setSortAscending(loadSettings.isAscending());
         db.sort();
         modified = true;
      }

      if (loadSettings.isShuffleOn())
      {
         publish(gui.getMessageHandler().getLabel(
           "progress.shuffling"));

         db.shuffle();
         modified = true;
      }

      Vector<FilterInfo> filterData = loadSettings.getFilterInfo();

      if (filterData != null)
      {
         publish(gui.getMessageHandler().getLabel(
           "progress.filtering"));

         DataFilter filter = new DataFilter(db, loadSettings.isFilterOr());
         filter.addFilters(filterData);

         if (loadSettings.isFilterInclude())
         {
            db.removeNonMatching(filter);
         }
         else
         {
            db.removeMatching(filter);
         }

         modified = true;
      }

      int truncate = loadSettings.getTruncate();

      if (truncate > -1)
      {
         publish(gui.getMessageHandler().getLabel(
           "progress.truncating"));

         db.truncate(truncate);
         modified = true;
      }

      return db;
   }

   @Override
   protected void process(List<String> chunks)
   {
      for (String msg : chunks)
      {
         gui.addProgressMessage(msg);
      }
   }

   public void propertyChange(PropertyChangeEvent evt)
   {
      if ("progress".equals(evt.getPropertyName()))
      {
         gui.setProgress((Integer)evt.getNewValue());
      }
   }

   public void publishProgress(String msg, int progress)
   {
      if (msg != null)
      {
         publish(msg);
      }

      if (progress >= 0)
      {
         setProgress(progress);
      }
   }

   public boolean cancelProgress()
   {
      return cancel(true);
   }

   @Override
   public void done()
   {
      try
      {
         DatatoolDb db = get();

         if (db != null)
         {
            gui.createNewTab(db, modified);

            if (file != null)
            {
               gui.getSettings().addRecentFile(file);
            }
         }
      }
      catch (CancellationException ex)
      {
         gui.getMessageHandler().debug(ex);
      }
      catch (ExecutionException | InterruptedException ex)
      {
         gui.getMessageHandler().error(gui,
           gui.getMessageHandler().getLabelWithValues(
             "error.load.failed", file.toString(), 
              gui.getMessageHandler().getMessage(ex)), ex);
      }
      finally
      {
         gui.stopProgressMessages();
      }
   }
}
