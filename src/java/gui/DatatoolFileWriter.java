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

import com.dickimawbooks.texparserlib.latex.datatool.FileFormatType;
import com.dickimawbooks.datatooltk.base.DatatoolDb;

public class DatatoolFileWriter extends SwingWorker<Void,String>
  implements ProgressMonitor,PropertyChangeListener
{
   private DatatoolDbPanel dbPanel;
   private int[] columnIndexes;
   private int[] rowIndexes;
   private FileFormatType fileFmtType;
   private String fileVersion;
   private DatatoolGUI gui;

   public DatatoolFileWriter(DatatoolDbPanel dbPanel,
     int[] columnIndexes, int[] rowIndexes,
     FileFormatType fileFmtType, String fileVersion)
   {
      this.dbPanel = dbPanel;
      this.columnIndexes = columnIndexes;
      this.rowIndexes = rowIndexes;
      this.fileFmtType = fileFmtType;
      this.fileVersion = fileVersion;

      gui = dbPanel.getDatatoolGUI();

      gui.startProgressMessages(this);
      addPropertyChangeListener(this);

      gui.addProgressMessage(gui.getMessageHandler().getLabelWithValues(
        "progress.saving", dbPanel.getDatabase().getFile()));

   }

   @Override
   public Void doInBackground() throws IOException
   {
      dbPanel.getDatabase().save(columnIndexes, rowIndexes,
       fileFmtType, fileVersion);

      return null;
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
      File file = dbPanel.getDatabase().getFile();

      try
      {
         get();

         dbPanel.setModified(false);
         gui.getSettings().addRecentFile(file);
         gui.updateTitle(dbPanel);
      }
      catch (CancellationException ex)
      {
         gui.getMessageHandler().debug(ex);
      }
      catch (ExecutionException | InterruptedException ex)
      {
         Throwable cause = ex.getCause();

         if (cause instanceof IOException)
         {
            gui.getMessageHandler().error(gui,
              gui.getMessageHandler().getLabelWithValues(
                "error.save.failed", file, 
                 gui.getMessageHandler().getMessage(cause)));

            gui.getMessageHandler().debug(ex);
         }
         else
         {
            gui.getMessageHandler().error(gui,
              gui.getMessageHandler().getLabelWithValues(
                "error.save.failed", file, 
                 gui.getMessageHandler().getMessage(ex)), ex);
         }
      }
      finally
      {
         gui.stopProgressMessages();
      }
   }
}
