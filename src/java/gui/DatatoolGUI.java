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
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.help.*;

import com.dickimawbooks.datatooltk.*;
import com.dickimawbooks.datatooltk.io.*;

/**
 * Main GUI window.
 */
public class DatatoolGUI extends JFrame
  implements ActionListener,MenuListener
{
   public DatatoolGUI(DatatoolSettings settings)
   {
      super(DatatoolTk.appName);

      this.settings = settings;

      initGui();
   }

   private void initGui()
   {
      setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

      addWindowListener(new WindowAdapter()
         {
            public void windowClosing(WindowEvent evt)
            {
               quit();
            }
         }
      );

      String imgFile = "/resources/icons/datatooltk-logosmall.png";

      URL imageURL = DatatoolTk.class.getResource(imgFile);

      if (imageURL != null)
      {
         setIconImage(new ImageIcon(imageURL).getImage());
      }
      else
      {
         DatatoolGuiResources.error(null, 
            new FileNotFoundException("Can't find resource: '"
            +imgFile+"'"));
      }

      try
      {
         initHelp();
      }
      catch (HelpSetException e)
      {
         DatatoolGuiResources.error(null, e);
      }
      catch (FileNotFoundException e)
      {
         DatatoolGuiResources.error(null, e);
      }

      JMenuBar mbar = new JMenuBar();
      setJMenuBar(mbar);

      toolBar = new ScrollToolBar(SwingConstants.HORIZONTAL);

      JMenu fileM = DatatoolGuiResources.createJMenu("file");
      mbar.add(fileM);

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "new", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK),
        toolBar));

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "new_from_template", this, toolBar));

      recentM = DatatoolGuiResources.createJMenu("file.recent");
      recentM.addMenuListener(this);
      fileM.add(recentM);

      clearRecentItem = DatatoolGuiResources.createJMenuItem(
        "file.recent", "clearrecent", this, toolBar);

      recentFilesListener = new ActionListener()
      {
         public void actionPerformed(ActionEvent evt)
         {
            String action = evt.getActionCommand();

            if (action == null) return;

            try
            {
               int index = Integer.parseInt(action);

               if (index < 0 || index >= settings.getRecentFileCount())
               {
                  DatatoolTk.debug("Invalid recent file index "+index);
                  return;
               }

               load(settings.getRecentFileName(index));
            }
            catch (NumberFormatException e)
            {
               DatatoolTk.debug("Invalid recent file index "+action);
            }
         }
      };

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "open", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK),
        toolBar));

      JMenu importM = DatatoolGuiResources.createJMenu("file.import");
      fileM.add(importM);

      importM.add(DatatoolGuiResources.createJMenuItem(
        "file.import", "importcsv", this, toolBar));

      importM.add(DatatoolGuiResources.createJMenuItem(
        "file.import", "importsql", this, toolBar));

      importSqlDialog = new ImportSqlDialog(this);

      importM.add(DatatoolGuiResources.createJMenuItem(
        "file.import", "importprobsoln", this, toolBar));

      importM.add(DatatoolGuiResources.createJMenuItem(
        "file.import", "importxls", this, toolBar));

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "save", this, toolBar));

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "save_as", this, toolBar));

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "close_db", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_MASK),
         toolBar));

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "quit", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK),
        toolBar));

      JMenu editM = DatatoolGuiResources.createJMenu("edit");
      mbar.add(editM);

      undoItem = DatatoolGuiResources.createJMenuItem(
         "edit", "undo", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK),
         toolBar);
      editM.add(undoItem);

      undoItem.setEnabled(false);

      redoItem = DatatoolGuiResources.createJMenuItem(
         "edit", "redo", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK),
         toolBar);
      editM.add(redoItem);

      redoItem.setEnabled(false);

      deselectItem = DatatoolGuiResources.createJMenuItem(
         "edit", "deselect", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_A, 
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
         toolBar);
      editM.add(deselectItem);
      deselectItem.setEnabled(false);

      editM.addSeparator();

      editDbNameItem = DatatoolGuiResources.createJMenuItem(
         "edit", "edit_dbname", this, toolBar);
      editM.add(editDbNameItem);
      editDbNameItem.setEnabled(false);

      editCellItem = DatatoolGuiResources.createJMenuItem(
         "edit", "edit_cell", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK),
         toolBar);
      editM.add(editCellItem);
      editCellItem.setEnabled(false);

      setToNullItem = DatatoolGuiResources.createJMenuItem(
         "edit", "cell_to_null", this, null, toolBar);
      editM.add(setToNullItem);
      setToNullItem.setEnabled(false);

      JMenu colM = DatatoolGuiResources.createJMenu("edit.column");
      editM.add(colM);

      editHeaderItem = DatatoolGuiResources.createJMenuItem(
         "edit.column", "edit_header", this, toolBar);
      colM.add(editHeaderItem);
      editHeaderItem.setEnabled(false);

      addColumnBeforeItem = DatatoolGuiResources.createJMenuItem(
         "edit.column", "add_column_before", this, toolBar);
      colM.add(addColumnBeforeItem);
      addColumnBeforeItem.setEnabled(false);

      addColumnAfterItem = DatatoolGuiResources.createJMenuItem(
         "edit.column", "add_column_after", this, toolBar);
      colM.add(addColumnAfterItem);
      addColumnAfterItem.setEnabled(false);

      removeColumnItem = DatatoolGuiResources.createJMenuItem(
         "edit.column", "remove_column", this, toolBar);
      colM.add(removeColumnItem);
      removeColumnItem.setEnabled(false);

      setColToNullItem = DatatoolGuiResources.createJMenuItem(
         "edit.column", "column_to_null", this, null, toolBar);
      colM.add(setColToNullItem);
      setColToNullItem.setEnabled(false);

      JMenu rowM = DatatoolGuiResources.createJMenu("edit.row");
      editM.add(rowM);

      addRowBeforeItem = DatatoolGuiResources.createJMenuItem(
         "edit.row", "add_row_before", this, toolBar);
      rowM.add(addRowBeforeItem);
      addRowBeforeItem.setEnabled(false);

      addRowAfterItem = DatatoolGuiResources.createJMenuItem(
         "edit.row", "add_row_after", this, toolBar);
      rowM.add(addRowAfterItem);
      addRowAfterItem.setEnabled(false);

      removeRowItem = DatatoolGuiResources.createJMenuItem(
         "edit.row", "remove_row", this, toolBar);
      rowM.add(removeRowItem);
      removeRowItem.setEnabled(false);

      setRowToNullItem = DatatoolGuiResources.createJMenuItem(
         "edit.row", "row_to_null", this, null, toolBar);
      rowM.add(setRowToNullItem);
      setRowToNullItem.setEnabled(false);

      editM.add(DatatoolGuiResources.createJMenuItem(
         "edit", "preferences", this, toolBar));

      propertiesDialog = new PropertiesDialog(this);

      JMenu searchM = DatatoolGuiResources.createJMenu("search");
      mbar.add(searchM);

      findCellItem = DatatoolGuiResources.createJMenuItem(
         "search", "find_cell", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK),
         toolBar);

      searchM.add(findCellItem);

      findCellDialog = new FindCellDialog(this);

      findNextItem = DatatoolGuiResources.createJMenuItem(
         "search", "find_again", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK),
         toolBar);
      searchM.add(findNextItem);

      replaceItem = DatatoolGuiResources.createJMenuItem(
         "search", "replace", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK),
         toolBar);

      replaceAllDialog = new ReplaceAllDialog(this);

      searchM.add(replaceItem);

      toolsM = DatatoolGuiResources.createJMenu("tools");
      mbar.add(toolsM);

      sortItem = DatatoolGuiResources.createJMenuItem(
         "tools", "sort", this, toolBar);
      toolsM.add(sortItem);

      sortDialog = new SortDialog(this);

      shuffleItem = DatatoolGuiResources.createJMenuItem(
         "tools", "shuffle", this, toolBar);
      toolsM.add(shuffleItem);

      pluginsM = DatatoolGuiResources.createJMenu("tools.plugins");
      toolsM.add(pluginsM);

      initPlugins();

      JMenu helpM = DatatoolGuiResources.createJMenu("help");
      mbar.add(helpM);

      helpM.add(DatatoolGuiResources.createJMenuItem(
         "help", "manual", csh,
          KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
          toolBar));

      JMenuItem item = new JMenuItem(DatatoolTk.getLabel("help",
        "licence"));
      item.setMnemonic(DatatoolTk.getMnemonic("help", "licence"));

      enableHelpOnButton(item, "licence");

      helpM.add(item);

      helpM.add(DatatoolGuiResources.createJMenuItem(
         "help", "about", this, toolBar));

      settings.setPasswordReader(new GuiPasswordReader(this));

      // main panel

      tabbedPane = new JTabbedPane()
      {
         public String getToolTipText(MouseEvent event) 
         {
            javax.swing.plaf.TabbedPaneUI ui = getUI();

            if (ui != null)
            {
               int index = ui.tabForCoordinate(this, event.getX(), event.getY());

               if (index != -1)
               {
                  return ((DatatoolDbPanel)getComponentAt(index)).getToolTipText();
               }
            }

            return super.getToolTipText(event);
         }
      };

      tabbedPane.addChangeListener(new ChangeListener()
      {
         public void stateChanged(ChangeEvent event)
         {
            Component tab = tabbedPane.getSelectedComponent();

            if (tab != null && (tab instanceof DatatoolDbPanel))
            {
               DatatoolDbPanel panel = (DatatoolDbPanel)tab;

               enableEditItems(panel.getModelSelectedRow() > -1, 
                 panel.getModelSelectedColumn() > -1);
            }
         }
      });

      getContentPane().add(tabbedPane, BorderLayout.CENTER);
      getContentPane().add(toolBar, BorderLayout.PAGE_START);

      // File filters

      texFilter = new TeXFileFilter();
      dbtexFilter = new DbTeXFileFilter();
      csvtxtFilter = new CsvTxtFileFilter();
      csvFilter = new CsvFileFilter();
      txtFilter = new TxtFileFilter();
      xlsFilter = new XlsFileFilter();

      fileChooser = new JFileChooser(settings.getStartUpDirectory());

      headerDialog = new HeaderDialog(this);
      cellEditor = new CellDialog(this);

      // Set default dimensions

      Toolkit tk = Toolkit.getDefaultToolkit();

      Dimension d = tk.getScreenSize();

      int width = 3*d.width/4;
      int height = d.height/2;

      setSize(width, height);

      setLocationRelativeTo(null);

      updateTools();
   }

   private void initHelp()
     throws HelpSetException,FileNotFoundException
   {
      if (mainHelpBroker == null)
      {
         HelpSet mainHelpSet = null;

         String helpset = settings.getHelpSetLocation()
            + "-"+settings.getHelpSet()+"/"+settings.RESOURCE+".hs";

         URL hsURL = getClass().getResource(helpset);

         if (hsURL == null)
         {
            if (hsURL == null)
            {
               throw new FileNotFoundException(
                  DatatoolTk.getLabelWithValue("error.resource.not_found",
                    helpset));
            }
         }

         mainHelpSet = new HelpSet(null, hsURL);

         mainHelpBroker = mainHelpSet.createHelpBroker();

         csh = new CSH.DisplayHelpFromSource(mainHelpBroker);
      }
   }

   public void enableHelpOnButton(JComponent comp, String id)
   {
      if (mainHelpBroker != null)
      {
         try
         {
            mainHelpBroker.enableHelpOnButton(comp, id, 
               mainHelpBroker.getHelpSet());
         }
         catch (BadIDException e)
         {
            DatatoolGuiResources.error(null, e);
         }
      }
      else
      {
         DatatoolTk.debug("Can't enable help on button (id="+id
           +"): null help broker");
      }
   }

   public JButton createHelpButton(String id)
   {
      JButton button = DatatoolGuiResources.createActionButton(
         "button", "help", null, 
         KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));

      enableHelpOnButton(button, id);

      return button;
   }

   public String[] getDictionaries()
   {
      File dir = null;

      try
      {
         dir = new File(DatatoolTk.class.getResource(
            settings.DICT_DIR).toURI());
      }
      catch (URISyntaxException e)
      {
         DatatoolGuiResources.error(this, e);
         return null;
      }

      String[] list = dir.list(new FilenameFilter()
      {
         public boolean accept(File directory, String name)
         {
            Matcher m = DatatoolSettings.PATTERN_DICT.matcher(name);

            return m.matches();
         }
      });

      for (int i = 0; i < list.length; i++)
      {
         Matcher m = DatatoolSettings.PATTERN_DICT.matcher(list[i]);

         if (m.matches())
         {
            list[i] = (m.groupCount() == 1 ? m.group(1) : m.group(1)+m.group(2));
         }
      }

      Arrays.sort(list);

      return list;
   }

   public String[] getHelpSets()
   {
      File dir = null;

      try
      {
         dir = new File(DatatoolTk.class.getResource(
            settings.HELPSET_DIR).toURI());
      }
      catch (URISyntaxException e)
      {
         DatatoolGuiResources.error(this, e);
         return null;
      }

      String[] list = dir.list(new FilenameFilter()
      {
         public boolean accept(File directory, String name)
         {
            Matcher m = DatatoolSettings.PATTERN_HELPSET.matcher(name);

            return m.matches();
         }
      });

      for (int i = 0; i < list.length; i++)
      {
         Matcher m = DatatoolSettings.PATTERN_HELPSET.matcher(list[i]);

         if (m.matches())
         {
            list[i] = (m.groupCount() == 1 ? m.group(1) : m.group(1)+m.group(2));
         }
      }

      Arrays.sort(list);

      return list;
   }

   public void updateUndoRedoItems(DatatoolDbPanel panel)
   {
      undoItem.setEnabled(panel.canUndo());
      redoItem.setEnabled(panel.canRedo());
   }

   public void updateUndoRedoItems(DatatoolDbPanel panel, 
     String undoName, String redoName)
   {
      if (undoName != null)
      {
        undoItem.setText(DatatoolTk.getLabelWithValue("edit.undo",
           undoName));
      }

      if (redoName != null)
      {
        redoItem.setText(DatatoolTk.getLabelWithValue("edit.redo",
           redoName));
      }

      updateUndoRedoItems(panel);
   }

   private void initPlugins()
   {
      try
      {
         plugins = settings.getPlugins();
      }
      catch (URISyntaxException e)
      {
         DatatoolGuiResources.error(this, e);
         return;
      }

      for (int i = 0; i < plugins.length; i++)
      {
         JMenuItem item = new JMenuItem(plugins[i].toString());
         pluginsM.add(item);
         item.setActionCommand(""+i);

         item.addActionListener(new ActionListener()
         {
            public void actionPerformed(ActionEvent evt)
            {
               DatatoolDbPanel dbPanel 
                  = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

               if (dbPanel == null) return;

               try
               {
                  int index = Integer.parseInt(evt.getActionCommand());

                  DatatoolPlugin plugin = plugins[index];

                  plugin.process(dbPanel);
               }
               catch (NumberFormatException e)
               {
               }
               catch (IOException e)
               {
                  DatatoolGuiResources.error(null, e);
               }
            }
         });
      }
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("quit"))
      {
         quit();
      }
      else if (action.equals("save"))
      {
         save();
      }
      else if (action.equals("save_as"))
      {
         saveAs();
      }
      else if (action.equals("open"))
      {
         load();
      }
      else if (action.equals("importcsv"))
      {
         importCsv();
      }
      else if (action.equals("importsql"))
      {
         importSqlDialog.requestImport(settings);
      }
      else if (action.equals("importprobsoln"))
      {
         importProbSoln();
      }
      else if (action.equals("importxls"))
      {
         importXls();
      }
      else if (action.equals("close_db"))
      {
         close();
      }
      else if (action.equals("new"))
      {
         DatatoolDb db = new DatatoolDb(settings);

         String name = JOptionPane.showInputDialog(this,
            DatatoolTk.getLabel("message.input_database_name"),
            DatatoolTk.getLabel("default.untitled"));

         if (name != null)
         {
            db.setName(name);
            createNewTab(db);
         }
      }
      else if (action.equals("new_from_template"))
      {
         newFromTemplate();
      }
      else if (action.equals("deselect"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            ((DatatoolDbPanel)tab).selectViewCell(-1, -1);
         }
      }
      else if (action.equals("edit_dbname"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            ((DatatoolDbPanel)tab).requestName();
         }
      }
      else if (action.equals("edit_cell"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            ((DatatoolDbPanel)tab).requestSelectedCellEdit();
         }
      }
      else if (action.equals("cell_to_null"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            ((DatatoolDbPanel)tab).updateCell(DatatoolDb.NULL_VALUE);
         }
      }
      else if (action.equals("column_to_null"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            DatatoolDbPanel dbPanel = ((DatatoolDbPanel)tab);

            dbPanel.startCompoundEdit(DatatoolTk.getLabel("undo.nullify_column"));

            int selectedColumn = dbPanel.getModelSelectedColumn();
            for (int row = 0, n = dbPanel.getRowCount(); row < n; row++)
            {
               dbPanel.updateCell(row, selectedColumn, DatatoolDb.NULL_VALUE);
            }

            dbPanel.commitCompoundEdit();
         }
      }
      else if (action.equals("row_to_null"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            DatatoolDbPanel dbPanel = ((DatatoolDbPanel)tab);

            dbPanel.startCompoundEdit(DatatoolTk.getLabel("undo.nullify_row"));

            int selectedRow = dbPanel.getModelSelectedRow();
            for (int column = 0, n = dbPanel.getColumnCount(); column < n; column++)
            {
               dbPanel.updateCell(selectedRow, column, DatatoolDb.NULL_VALUE);
            }

            dbPanel.commitCompoundEdit();
         }
      }
      else if (action.equals("edit_header"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            ((DatatoolDbPanel)tab).requestSelectedHeaderEditor();
         }
      }
      else if (action.equals("add_column_after"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            ((DatatoolDbPanel)tab).requestNewColumnAfter();
         }
      }
      else if (action.equals("add_column_before"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            ((DatatoolDbPanel)tab).requestNewColumnBefore();
         }
      }
      else if (action.equals("remove_column"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            ((DatatoolDbPanel)tab).removeSelectedColumn();
         }
      }
      else if (action.equals("remove_row"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            ((DatatoolDbPanel)tab).removeSelectedRow();
         }
      }
      else if (action.equals("add_row_after"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            ((DatatoolDbPanel)tab).insertNewRowAfter();
         }
      }
      else if (action.equals("add_row_before"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            ((DatatoolDbPanel)tab).insertNewRowBefore();
         }
      }
      else if (action.equals("about"))
      {
         JOptionPane.showMessageDialog(this, 
           DatatoolTk.getAppInfo(),
           DatatoolTk.getLabelWithValue("about.title", DatatoolTk.appName),
           JOptionPane.PLAIN_MESSAGE);
      }
      else if (action.equals("undo"))
      {
         DatatoolDbPanel panel = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

         if (panel != null)
         {
            panel.undo();
         }
      }
      else if (action.equals("redo"))
      {
         DatatoolDbPanel panel = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

         if (panel != null)
         {
            panel.redo();
         }
      }
      else if (action.equals("sort"))
      {
         DatatoolDbPanel panel = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

         if (panel != null)
         {
            panel.sortData(); 
         }
      }
      else if (action.equals("shuffle"))
      {
         DatatoolDbPanel panel = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

         if (panel != null)
         {
            panel.shuffleData(settings.getRandom()); 
         }
      }
      else if (action.equals("preferences"))
      {
         propertiesDialog.display(settings);
      }
      else if (action.equals("find_cell"))
      {
         DatatoolDbPanel panel = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

         if (panel != null)
         {
            findCellDialog.display(panel);
         }
      }
      else if (action.equals("find_again"))
      {
         DatatoolDbPanel panel = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

         if (panel != null)
         {
            findCellDialog.findNext(panel);
         }
      }
      else if (action.equals("replace"))
      {
         DatatoolDbPanel panel = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

         if (panel != null)
         {
            replaceAllDialog.display(panel);
         }
      }
   }

   public void menuSelected(MenuEvent evt)
   {
      Object source = evt.getSource();

      if (source == recentM)
      {
         recentM.removeAll();

         // Add recent files

         for (int i = 0, n = Math.min(10, settings.getRecentFileCount());
              i < n; i++)
         {
            String name = settings.getRecentFileName(i);
            File file = new File(name);
            String num = ""+i;
            JMenuItem item = new JMenuItem(num+": "+file.getName());
            item.setMnemonic(num.charAt(0));
            item.setToolTipText(name);
            item.setActionCommand(num);
            item.addActionListener(recentFilesListener);

            recentM.add(item);
         }

         recentM.addSeparator();
         recentM.add(clearRecentItem);
      }
   }

   public void menuDeselected(MenuEvent evt)
   {
   }

   public void menuCanceled(MenuEvent evt)
   {
   }

   public void quit()
   {
      for (int i = tabbedPane.getTabCount()-1; i >= 0; i--)
      {
         if (!close((DatatoolDbPanel)tabbedPane.getComponentAt(i)))
         {
            return;
         }
      }

      try
      {
         settings.directoryOnExit(fileChooser.getCurrentDirectory());
         settings.saveProperties();
      }
      catch (IOException e)
      {
         DatatoolTk.debug(e);
      }

      System.exit(0);
   }

   private void setTeXFileFilters()
   {
      FileFilter current = fileChooser.getFileFilter();

      if (current == dbtexFilter || current == texFilter)
      {
         return;
      }

      fileChooser.resetChoosableFileFilters();

      FileFilter all = fileChooser.getAcceptAllFileFilter();

      fileChooser.removeChoosableFileFilter(all);

      fileChooser.addChoosableFileFilter(dbtexFilter);
      fileChooser.addChoosableFileFilter(texFilter);
      fileChooser.addChoosableFileFilter(all);
   }

   private void setTeXFileFilter()
   {
      FileFilter current = fileChooser.getFileFilter();

      if (current == dbtexFilter || current == texFilter)
      {
         return;
      }

      fileChooser.resetChoosableFileFilters();

      FileFilter all = fileChooser.getAcceptAllFileFilter();

      fileChooser.removeChoosableFileFilter(all);

      fileChooser.addChoosableFileFilter(texFilter);
      fileChooser.addChoosableFileFilter(all);
   }

   private void setCsvFileFilters()
   {
      FileFilter current = fileChooser.getFileFilter();

      if (current == csvFilter || current == txtFilter 
       || current == csvtxtFilter)
      {
         return;
      }

      fileChooser.resetChoosableFileFilters();

      FileFilter all = fileChooser.getAcceptAllFileFilter();

      fileChooser.removeChoosableFileFilter(all);

      fileChooser.addChoosableFileFilter(csvtxtFilter);
      fileChooser.addChoosableFileFilter(csvFilter);
      fileChooser.addChoosableFileFilter(txtFilter);
      fileChooser.addChoosableFileFilter(all);
   }

   private void setXlsFileFilters()
   {
      FileFilter current = fileChooser.getFileFilter();

      if (current == xlsFilter)
      {
         return;
      }

      fileChooser.resetChoosableFileFilters();

      FileFilter all = fileChooser.getAcceptAllFileFilter();

      fileChooser.removeChoosableFileFilter(all);

      fileChooser.addChoosableFileFilter(xlsFilter);
      fileChooser.addChoosableFileFilter(all);
   }

   public boolean close()
   {
      DatatoolDbPanel panel 
         = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

      return close(panel);
   }

   public boolean close(DatatoolDbPanel panel)
   {
      if (panel == null) return true;

      if (panel.isModified())
      {
         switch (JOptionPane.showConfirmDialog(this,
           DatatoolTk.getLabelWithValue("message.unsaved_data_query",
             panel.getName()),
           DatatoolTk.getLabel("message.unsaved_data"),
           JOptionPane.YES_NO_CANCEL_OPTION,
           JOptionPane.QUESTION_MESSAGE))
         {
            case JOptionPane.YES_OPTION:
               panel.save();
            break;
            case JOptionPane.NO_OPTION:
            break;
            default:
               return false;
         }
      }

      tabbedPane.remove(panel);

      panel = null;

      updateTools();

      return true;
   }

   public void save()
   {
      DatatoolDbPanel panel 
         = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

      if (panel == null)
      {
         DatatoolGuiResources.error(this, 
           DatatoolTk.getLabel("error.nopanel"));
         return;
      }

      panel.save();
   }

   public void saveAs()
   {
      DatatoolDbPanel panel 
         = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

      if (panel == null)
      {
         DatatoolGuiResources.error(this, 
           DatatoolTk.getLabel("error.nopanel"));
         return;
      }

      setTeXFileFilters();

      fileChooser.setSelectedFile(new File(panel.getName()+".dbtex"));

      if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
      {
         return;
      }

      panel.save(fileChooser.getSelectedFile());
   }

   public void addRecentFile(File file)
   {
      settings.addRecentFile(file);
   }

   public DatatoolDb load()
   {
      setTeXFileFilters();

      if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
      {
         return load(fileChooser.getSelectedFile());
      }

      return null;
   }

   public DatatoolDb load(String filename)
   {
      return load(new File(filename));
   }

   public DatatoolDb load(File file)
   {
      DatatoolDb db = null;

      try
      {
         db = DatatoolDb.load(settings, file);
         createNewTab(db);

         settings.addRecentFile(file);
      }
      catch (IOException e)
      {
         DatatoolGuiResources.error(this,
           DatatoolTk.getLabelWithValues(
             "error.load.failed", file.toString(), e.getMessage()), e);
      }

      return db;
   }

   private void createNewTab(DatatoolDb db)
   {
      DatatoolDbPanel panel = new DatatoolDbPanel(this, db);

      tabbedPane.addTab(panel.getName(), panel);

      int idx = tabbedPane.getTabCount()-1;

      tabbedPane.setToolTipTextAt(idx, db.getFileName());

      tabbedPane.setTabComponentAt(idx, panel.getButtonTabComponent());

      tabbedPane.setSelectedIndex(idx);

      updateTools();
   }

   public DatatoolDb importXls()
   {
      setXlsFileFilters();

      if (fileChooser.showDialog(this, DatatoolTk.getLabel("button.import"))
       != JFileChooser.APPROVE_OPTION)
      {
         return null;
      }

      DatatoolExcel imp = new DatatoolExcel(settings);

      DatatoolDb db = null;

      File file = fileChooser.getSelectedFile();

      try
      {
         Object ref = JOptionPane.showInputDialog(this,
            DatatoolTk.getLabel("importxls.sheet"),
            DatatoolTk.getLabel("importxls.title"),
            JOptionPane.PLAIN_MESSAGE,
            null, imp.getSheetNames(file),
            null);

         if (ref == null)
         {
            return null;
         }

         settings.setSheetRef(ref.toString());

         db = imp.importData(fileChooser.getSelectedFile());
         createNewTab(db);
      }
      catch (Exception e)
      {
         DatatoolGuiResources.error(this, e);
      }

      return db;
   }

   public DatatoolDb importCsv()
   {
      setCsvFileFilters();

      if (fileChooser.showDialog(this, DatatoolTk.getLabel("button.import"))
       != JFileChooser.APPROVE_OPTION)
      {
         return null;
      }

      DatatoolCsv imp = new DatatoolCsv(settings);

      DatatoolDb db = null;

      try
      {
         db = imp.importData(fileChooser.getSelectedFile());
         createNewTab(db);
      }
      catch (DatatoolImportException e)
      {
         DatatoolGuiResources.error(this, e);
      }

      return db;
   }

   public DatatoolDb importProbSoln()
   {
      setTeXFileFilter();

      if (fileChooser.showDialog(this, DatatoolTk.getLabel("button.import"))
       != JFileChooser.APPROVE_OPTION)
      {
         return null;
      }

      DatatoolProbSoln imp = new DatatoolProbSoln(settings);

      DatatoolDb db = null;

      try
      {
         db = imp.importData(
            fileChooser.getSelectedFile().getAbsolutePath());
         createNewTab(db);
      }
      catch (DatatoolImportException e)
      {
         DatatoolGuiResources.error(this, e);
      }

      return db;
   }

   public DatatoolDb importData(DatatoolImport imp, String source)
   {
      DatatoolDb db = null;

      try
      {
         db = imp.importData(source);
         createNewTab(db);
      }
      catch (DatatoolImportException e)
      {
         DatatoolGuiResources.error(this, e);
      }

      return db;
   }

   public void newFromTemplate()
   {
      try
      {
         Template[] templates = settings.getTemplates();

         if (templates.length == 0)
         {
            DatatoolGuiResources.error(this, "error.no_templates");
            return;
         }

         Object result = JOptionPane.showInputDialog(this,
           DatatoolTk.getLabel("template.message"),
           DatatoolTk.getLabel("template.title"),
           JOptionPane.PLAIN_MESSAGE,
           null, templates, templates[0]);

         if (result == null)
         {
            return;
         }

         DatatoolDb db = DatatoolDb.createFromTemplate(settings,
           (Template)result);
         createNewTab(db);
      }
      catch (Exception e)
      {
          DatatoolGuiResources.error(this, e);
      }
   }

   public void selectTab(DatatoolDbPanel panel)
   {
      tabbedPane.setSelectedComponent(panel);
   }

   public boolean requestSortDialog(DatatoolDb db)
   {
      return sortDialog.requestInput(db);
   }

   public DatatoolHeader requestNewHeader(DatatoolDbPanel panel)
   {
      String label = DatatoolTk.getLabel("default.untitled");

      DatatoolHeader header = new DatatoolHeader(panel.db, label, "");

      return headerDialog.requestEdit(header, panel.db);
   }

   public DatatoolHeader requestHeaderEditor(int colIdx, DatatoolDbPanel panel)
   {
      return headerDialog.requestEdit(colIdx, panel.db);
   }

   public void requestCellEditor(int row, int col, DatatoolDbPanel panel)
   {
      if (cellEditor.requestEdit(row, col, panel))
      {
         panel.setModified(true);
      }
   }

   public void enableEditItems(boolean hasSelectedRow, boolean hasSelectedColumn)
   {
      boolean hasBoth = hasSelectedRow && hasSelectedColumn;
      editCellItem.setEnabled(hasBoth);
      setToNullItem.setEnabled(hasBoth);
      setColToNullItem.setEnabled(hasSelectedColumn);
      setRowToNullItem.setEnabled(hasSelectedRow);
      removeColumnItem.setEnabled(hasSelectedColumn);
      removeRowItem.setEnabled(hasSelectedRow);
      editHeaderItem.setEnabled(hasSelectedColumn);
      deselectItem.setEnabled(hasSelectedRow || hasSelectedColumn);
   }

   public void updateTools()
   {
      DatatoolDbPanel panel = 
         (DatatoolDbPanel)tabbedPane.getSelectedComponent();

      boolean enable = (panel != null);

      addColumnBeforeItem.setEnabled(enable);
      addColumnAfterItem.setEnabled(enable);
      editDbNameItem.setEnabled(enable);
      toolsM.setEnabled(enable);

      enable = (enable && panel.getColumnCount() > 0);

      addRowBeforeItem.setEnabled(enable);
      addRowAfterItem.setEnabled(enable);
      sortItem.setEnabled(enable);
      shuffleItem.setEnabled(enable);
      findCellItem.setEnabled(enable);
      findNextItem.setEnabled(enable && !findCellDialog.getSearchText().isEmpty());
      replaceItem.setEnabled(enable);

      pluginsM.setEnabled(enable && plugins.length > 0 
        && settings.getPerl() != null);
   }

   public Font getCellFont()
   {
      return settings.getFont();
   }

   public int getCellHeight()
   {
      return settings.getCellHeight();
   }

   public int getCellWidth(int type)
   {
      return settings.getCellWidth(type);
   }

   public DatatoolSettings getSettings()
   {
      return settings;
   }

   public void updateTableSettings()
   {
      for (int i = 0, n = tabbedPane.getTabCount(); i < n; i++)
      {
         DatatoolDbPanel panel = (DatatoolDbPanel)tabbedPane.getComponentAt(i);
         panel.updateColumnHeaders();
         panel.updateTableSettings();
      }
   }

   private DatatoolSettings settings;

   private JTabbedPane tabbedPane;

   private JFileChooser fileChooser;

   private FileFilter texFilter, dbtexFilter, csvFilter, txtFilter,
     csvtxtFilter, xlsFilter;

   private HeaderDialog headerDialog;

   private CellDialog cellEditor;

   private HelpBroker mainHelpBroker;
   private CSH.DisplayHelpFromSource csh;

   private ScrollToolBar toolBar;

   private JMenu recentM, pluginsM, toolsM;

   private JMenuItem undoItem, redoItem, editCellItem, 
      addColumnAfterItem, addRowAfterItem,
      addColumnBeforeItem, addRowBeforeItem,
      removeColumnItem, editHeaderItem, editDbNameItem,
      removeRowItem, clearRecentItem, sortItem, shuffleItem,
      findCellItem, findNextItem, replaceItem,
      setToNullItem, deselectItem, setColToNullItem, setRowToNullItem;

   private ActionListener recentFilesListener;

   private PropertiesDialog propertiesDialog;

   private ImportSqlDialog importSqlDialog;

   private SortDialog sortDialog;

   private FindCellDialog findCellDialog;

   private ReplaceAllDialog replaceAllDialog;

   private DatatoolPlugin[] plugins;
}
