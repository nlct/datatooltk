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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.net.URL;
import java.net.URISyntaxException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Vector;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;

import org.xml.sax.SAXException;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.texparserlib.latex.datatool.FileFormatType;
import com.dickimawbooks.texjavahelplib.*;

import com.dickimawbooks.datatooltk.base.*;
import com.dickimawbooks.datatooltk.base.io.*;

/**
 * Main GUI window.
 */
public class DatatoolGUI extends JFrame
  implements ActionListener,MenuListener
{
   private DatatoolGUI()
   {
      super(DatatoolTk.APP_NAME);
   }

   public DatatoolGUI(DatatoolProperties settings, LoadSettings loadSettings)
    throws IOException
   {
      super(settings.getApplicationName());

      this.settings = settings;
      resources = new DatatoolGuiResources(this, 
          (CombinedMessageHandler)settings.getMessageHandler());

      initGui();
      setVisible(true);

      if (loadSettings.hasInputAction())
      {
         DatatoolFileLoader loader = new DatatoolFileLoader(this, loadSettings);
         loader.execute();
      }
   }

   private void initGui() throws IOException
   {
      CombinedMessageHandler messageHandler = getMessageHandler();

      // Need to set L&F before creating components

      String lookAndFeel = settings.getLookAndFeel();

      if (lookAndFeel == null || lookAndFeel.isEmpty())
      {
         lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
         settings.setLookAndFeel(lookAndFeel);
      }

      try
      {
         UIManager.setLookAndFeel(lookAndFeel);
      }
      catch (Exception e)
      {
         LookAndFeel currentLF = UIManager.getLookAndFeel();

         if (currentLF == null)
         {
            settings.setLookAndFeel("");

            messageHandler.debug(String.format("Can't set look and feel '%s'",
             lookAndFeel), e);
         }
         else
         {
            String currentLFName = currentLF.getClass().getName();
            settings.setLookAndFeel(currentLFName);

            messageHandler.debug(String.format("Can't set look and feel '%s' (fallback '%s')",
             lookAndFeel, currentLFName), e);
         }
      }

      setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

      addWindowListener(new WindowAdapter()
         {
            public void windowClosing(WindowEvent evt)
            {
               quit();
            }
         }
      );

      progressMessages = new DatatoolProgressMessages(this);

      DEFAULT_UNTITLED = messageHandler.getLabel("default.untitled");

      Image img = getLogoImage();

      if (img != null)
      {
         setIconImage(img);
      }

      try
      {
         initHelp();
      }
      catch (Exception e)
      {
         getMessageHandler().error((Component)null, e);
      }

      TeXJavaHelpLib helpLib = getHelpLib();

      // main panel

      tabbedPane = new JTabbedPane();

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

               updateTitle(panel);
            }
         }
      });

      JMenuBar mbar = new JMenuBar();
      setJMenuBar(mbar);

      toolBar = new ScrollToolBar(messageHandler, SwingConstants.HORIZONTAL);

      JMenu fileM = resources.createJMenu("file");
      mbar.add(fileM);

      fileM.add(resources.createJMenuItem(
        "file", "new", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK),
        toolBar));

      fileM.add(resources.createJMenuItem(
        "file", "new_from_template", this, toolBar));

      recentM = resources.createJMenu("file.recent");
      recentM.addMenuListener(this);
      fileM.add(recentM);

      clearRecentItem = resources.createJMenuItem(
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
                  getMessageHandler().debug("Invalid recent file index "+index);
                  return;
               }

               load(settings.getRecentFileName(index));
            }
            catch (NumberFormatException e)
            {
               getMessageHandler().debug("Invalid recent file index "+action);
            }
         }
      };

      fileM.add(resources.createJMenuItem(
        "file", "open", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK),
        toolBar));

      fileM.add(resources.createJMenuItem(
        "file", "import", this, toolBar));

      saveItem = resources.createJMenuItem(
        "file", "save", this, toolBar);
      saveItem.setEnabled(false);

      fileM.add(saveItem);

      saveAsItem = resources.createJMenuItem(
        "file", "save_as", this, toolBar);
      saveAsItem.setEnabled(false);

      fileM.add(saveAsItem);

      fileM.addSeparator();

      closeItem = resources.createJMenuItem(
        "file", "close_db", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK),
         toolBar);

      fileM.add(closeItem);
      closeItem.setEnabled(false);

      fileM.add(resources.createJMenuItem(
        "file", "quit", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK),
        toolBar));

      JMenu editM = resources.createJMenu("edit");
      mbar.add(editM);

      undoItem = resources.createJMenuItem(
         "edit", "undo", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK),
         toolBar);
      editM.add(undoItem);

      undoItem.setEnabled(false);

      redoItem = resources.createJMenuItem(
         "edit", "redo", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK),
         toolBar);
      editM.add(redoItem);

      redoItem.setEnabled(false);

      deselectItem = resources.createJMenuItem(
         "edit", "deselect", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_A, 
            InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
         toolBar);
      editM.add(deselectItem);
      deselectItem.setEnabled(false);

      editM.addSeparator();

      editDbNameItem = resources.createJMenuItem(
         "edit", "edit_dbname", this, toolBar);
      editM.add(editDbNameItem);
      editDbNameItem.setEnabled(false);

      editCellItem = resources.createJMenuItem(
         "edit", "edit_cell", "edit", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK),
         toolBar);
      editM.add(editCellItem);
      editCellItem.setEnabled(false);

      setToNullItem = resources.createJMenuItem(
         "edit", "cell_to_null", this, null, toolBar);
      editM.add(setToNullItem);
      setToNullItem.setEnabled(false);

      setEmptyToNullItem = resources.createJMenuItem(
         "edit", "empty_to_null", this, null, toolBar);
      editM.add(setEmptyToNullItem);
      setEmptyToNullItem.setEnabled(false);

      setNullToEmptyItem = resources.createJMenuItem(
         "edit", "null_to_empty", this, null, toolBar);
      editM.add(setNullToEmptyItem);
      setNullToEmptyItem.setEnabled(false);

      JMenu colM = resources.createJMenu("edit.column");
      editM.add(colM);

      editHeaderItem = resources.createJMenuItem(
         "edit.column", "edit_header", this, toolBar);
      colM.add(editHeaderItem);
      editHeaderItem.setEnabled(false);

      addColumnBeforeItem = resources.createJMenuItem(
         "edit.column", "add_column_before", this, toolBar);
      colM.add(addColumnBeforeItem);
      addColumnBeforeItem.setEnabled(false);

      addColumnAfterItem = resources.createJMenuItem(
         "edit.column", "add_column_after", this, toolBar);
      colM.add(addColumnAfterItem);
      addColumnAfterItem.setEnabled(false);

      removeColumnItem = resources.createJMenuItem(
         "edit.column", "remove_column", this, toolBar);
      colM.add(removeColumnItem);
      removeColumnItem.setEnabled(false);

      setColToNullItem = resources.createJMenuItem(
         "edit.column", "column_to_null", this, null, toolBar);
      colM.add(setColToNullItem);
      setColToNullItem.setEnabled(false);

      JMenu rowM = resources.createJMenu("edit.row");
      editM.add(rowM);

      addRowBeforeItem = resources.createJMenuItem(
         "edit.row", "add_row_before", this, toolBar);
      rowM.add(addRowBeforeItem);
      addRowBeforeItem.setEnabled(false);

      addRowAfterItem = resources.createJMenuItem(
         "edit.row", "add_row_after", this, toolBar);
      rowM.add(addRowAfterItem);
      addRowAfterItem.setEnabled(false);

      removeRowItem = resources.createJMenuItem(
         "edit.row", "remove_row", this, toolBar);
      rowM.add(removeRowItem);
      removeRowItem.setEnabled(false);

      setRowToNullItem = resources.createJMenuItem(
         "edit.row", "row_to_null", this, null, toolBar);
      rowM.add(setRowToNullItem);
      setRowToNullItem.setEnabled(false);

      editM.addSeparator();

      editM.add(resources.createJMenuItem(
         "edit", "preferences", this, toolBar));

      propertiesDialog = new PropertiesDialog(this);

      JMenu searchM = resources.createJMenu("search");
      mbar.add(searchM);

      findCellItem = resources.createJMenuItem(
         "search", "find_cell", "search", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK),
         toolBar);

      searchM.add(findCellItem);

      findCellDialog = new FindCellDialog(this);

      findNextItem = resources.createJMenuItem(
         "search", "find_again", "search_again", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK),
         toolBar);
      searchM.add(findNextItem);

      replaceItem = resources.createJMenuItem(
         "search", "replace_text", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK),
         toolBar);

      replaceAllDialog = new ReplaceAllDialog(this);

      searchM.add(replaceItem);

      toolsM = resources.createJMenu("tools");
      mbar.add(toolsM);

      sortItem = resources.createJMenuItem(
         "tools", "sort", this, toolBar);
      toolsM.add(sortItem);

      sortDialog = new SortDialog(this);

      shuffleItem = resources.createJMenuItem(
         "tools", "shuffle", this, toolBar);
      toolsM.add(shuffleItem);

      pluginsM = resources.createJMenu("tools.plugins");
      toolsM.add(pluginsM);

      initPlugins();

      JMenu helpM = resources.createJMenu("help");
      mbar.add(helpM);

      helpM.add(resources.createJMenuItem("help", "manual", this,
       helpLib.getKeyStroke("menu.help.manual"), toolBar));

      helpM.add(resources.createJMenuItem(
         "help", "about", "information", this, null, toolBar));

      aboutDialog = new MessageDialog(this,
       getMessageHandler().getLabelWithValues("about.title", 
           getMessageHandler().getApplicationName()),
       true, helpLib, getMessageHandler().getDatatoolTk().getAppInfo(true));

      createLicenceDialog();

      if (licenceDialog != null)
      {
         helpM.add(resources.createJMenuItem("help", "licence", this));
      }

      settings.setPasswordReader(new GuiPasswordReader(helpLib, this));

      getContentPane().add(tabbedPane, BorderLayout.CENTER);
      getContentPane().add(toolBar, BorderLayout.PAGE_START);

      // File filters

      texFilter = new TeXFileFilter(messageHandler);

      dbdtltexFilter = new DbDtlTeXFileFilter(messageHandler);
      dbtex3Filter = new DbTeXVersionFileFilter(messageHandler, "3.0");
      dbtex2Filter = new DbTeXVersionFileFilter(messageHandler, "2.0");
      dtltex3Filter = new DtlTeXVersionFileFilter(messageHandler, "3.0");
      dtltex2Filter = new DtlTeXVersionFileFilter(messageHandler, "2.0");

      fileChooser = new JFileChooser(settings.getStartUpDirectory());

      importDialog = new ImportDialog(this);

      headerDialog = new HeaderDialog(this);
      cellEditor = new CellDialog(this);

      int width = settings.getWindowWidth();
      int height = settings.getWindowHeight();

      if (width == 0 || height == 0)
      {
         // Set default dimensions

         Toolkit tk = Toolkit.getDefaultToolkit();

         Dimension d = tk.getScreenSize();

         width = 3*d.width/4;
         height = d.height/2;
      }

      setSize(width, height);

      setLocationRelativeTo(null);

      updateTools();
   }

   private void initHelp()
     throws IOException,SAXException
   {
      TeXJavaHelpLib helpLib = settings.getHelpLib();

      settings.initHelpSetSettings();

      helpLib.setHelpsetSubDirPrefix(DatatoolProperties.RESOURCE_PREFIX);
      helpLib.initHelpSet(DatatoolProperties.HELPSETS);
      helpFrame = helpLib.getHelpFrame();

      Image img = getLogoImage();

      if (img != null)
      {
         helpFrame.setIconImage(img);
      }

      helpFrame.setLocationRelativeTo(this);

      HelpSetLocale dictLocale = helpLib.getMessagesLocale();

      String tag = dictLocale.getTag();
      String prop = DatatoolProperties.DICT_DIR + "plugins-"+tag+".prop";
      pluginDictURL = getClass().getResource(prop);

      if (pluginDictURL == null)
      {
         Locale locale = dictLocale.getLocale();
         tag = locale.toLanguageTag();
         prop = DatatoolProperties.DICT_DIR + "plugins-"+tag+".prop";
         pluginDictURL = getClass().getResource(prop);

         if (pluginDictURL == null)
         {
            tag = locale.getLanguage();

            if (!tag.equals(dictLocale.getTag()))
            {
               prop = DatatoolProperties.DICT_DIR + "plugins-"+tag+".prop";
               pluginDictURL = getClass().getResource(prop);
            }
         }

         if (pluginDictURL == null)
         {
            tag = "en";
            prop = DatatoolProperties.DICT_DIR + "plugins-"+tag+".prop";
            pluginDictURL = getClass().getResource(prop);

            if (pluginDictURL == null)
            {
               getMessageHandler().error(this,
                 helpLib.getMessage("error.no_plugin_dict", dictLocale, "en"));
            }
         }
      }
   }

   protected void createLicenceDialog()
   {
      TeXJavaHelpLib helpLib = getHelpLib();

      try
      {
         URL url = getClass().getResource(LICENCE_PATH);
   
         if (url == null)
         {
            throw new FileNotFoundException(helpLib.getMessage(
              "error.resource_not_found", LICENCE_PATH));
         }

         licenceDialog = new MessageDialog(this,
           helpLib.getMessage("licence.title"), true,
           helpLib, url);
      }
      catch (IOException e)
      {
         getMessageHandler().error(this, e);
      }
   }

   public URL getPluginDictionaryUrl()
   {
      return pluginDictURL;
   }

   public Image getLogoImage()
   {
      Image img = getIconImage();

      if (img != null)
      {
         return img;
      }

      String imgFile = settings.getApplicationIconPath();

      URL imageURL = DatatoolTk.class.getResource(imgFile);

      if (imageURL != null)
      {
         return new ImageIcon(imageURL).getImage();
      }
      else
      {
         getMessageHandler().warning("Logo image not found", 
           new FileNotFoundException(String.format("Can't find resource: '%s'",
           imgFile)));
      }

      return null;
   }

   public TeXJavaHelpLib getHelpLib()
   {
      return settings.getHelpLib();
   }

   public KeyStroke getKeyStroke(String id)
   {
      return getHelpLib().getKeyStroke(id);
   }

   /**
    * Create help button for modeless (non-blocking) components. 
    */
   public JButton createHelpButton(String id, JFrame owner)
   {
      TeXJavaHelpLib helpLib = getHelpLib();
      NavigationNode node = helpLib.getNavigationNodeById(id);

      if (node == null)
      {
         node = helpLib.getNavigationNodeById("sec:"+id);

         if (node == null)
         {
            getMessageHandler().error(this, 
             helpLib.getMessageWithFallback(
             "error.node_id_not_found", "Node with ID ''{0}'' not found", id));
            return null;
         }
      }

      return helpLib.createHelpDialogButton(owner, node);
   }

   /**
    * Create help button for modal dialog. 
    */
   public JButton createHelpButton(JDialog owner, String id)
   {
      TeXJavaHelpLib helpLib = getHelpLib();
      NavigationNode node = helpLib.getNavigationNodeById(id);

      if (node == null)
      {
         node = helpLib.getNavigationNodeById("sec:"+id);

         if (node == null)
         {
            getMessageHandler().error(this, 
             helpLib.getMessageWithFallback(
             "error.node_id_not_found", "Node with ID ''{0}'' not found", id));
            return null;
         }
      }

      return helpLib.createHelpDialogButton(owner, node);
   }

   /**
    * Create help action to open primary help window at the given
    * page.
    */
   public TJHAbstractAction createHelpAction(String id)
   {
      return createHelpAction(id, null, null);
   }

   public TJHAbstractAction createHelpAction(String id,
     KeyStroke keyStroke, JComponent comp)
   {
      TeXJavaHelpLib helpLib = getHelpLib();
      NavigationNode node = helpLib.getNavigationNodeById(id);

      if (node == null)
      {
         node = helpLib.getNavigationNodeById("sec:"+id);

         if (node == null)
         {
            getMessageHandler().error(this, 
             helpLib.getMessageWithFallback(
             "error.node_id_not_found", "Node with ID ''{0}'' not found", id));
            return null;
         }
      }

      if (keyStroke == null)
      {
         return helpLib.createHelpAction(node, "action", "help",
          "help", "help", null, null, Action.ACCELERATOR_KEY);
      }
      else
      {
         return helpLib.createHelpAction(node, "action", "help",
          "help", "help", keyStroke, comp);
      }
   }

   public Vector<HelpSetLocale> getAvailableDictionaries()
   {
      Vector<HelpSetLocale> list;

      try
      {
         list = getHelpLib().getMessageSystem().getDictionaries();
      }
      catch (Exception e)
      {// shouldn't happen unless there's a bug
         list = new Vector<HelpSetLocale>();
         list.add(new HelpSetLocale("en"));
         getMessageHandler().debug(e);
      }

      return list;
   }

   public Vector<HelpSetLocale> getAvailableHelpSets()
   {
      Vector<HelpSetLocale> list;

      try
      {
         list = getHelpLib().getHelpSets();
      }
      catch (Exception e)
      {// shouldn't happen unless there's a bug
         list = new Vector<HelpSetLocale>();
         list.add(new HelpSetLocale("en"));
         getMessageHandler().debug(e);
      }

      return list;
   }

   public boolean cancelProgress()
   {
      if (resources.getProgressMonitor() == null)
      {
         return true;
      }

      if (JOptionPane.showConfirmDialog(progressMessages,
           getMessageHandler().getLabel("progress.confirm.cancel"),
           getMessageHandler().getLabel("progress.confirm.cancel.title"),
           JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
      {
         return resources.getProgressMonitor().cancelProgress();
      }

      return false;
   }

   public void startProgressMessages(ProgressMonitor progressMonitor)
   {
      resources.setProgressMonitor(progressMonitor);

      progressMessages.reset();
      progressMessages.setVisible(true);

      progressMessages.setCursor(
         Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
   }

   public void stopProgressMessages()
   {
      resources.setProgressMonitor(null);

      progressMessages.setCursor(Cursor.getDefaultCursor());
      setCursor(Cursor.getDefaultCursor());

      progressMessages.setVisible(false);
   }

   public void addProgressMessage(String msg)
   {
      progressMessages.addMessage(msg);
   }

   public void setProgress(int value)
   {
      progressMessages.setProgress(value);
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
        undoItem.setText(getMessageHandler().getLabelWithValues("menu.edit.undo",
           undoName));
      }

      if (redoName != null)
      {
        redoItem.setText(getMessageHandler().getLabelWithValues("menu.edit.redo",
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
         getMessageHandler().error(this, e);
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
                  getMessageHandler().error((Component)null, e);
               }
            }
         });
      }
   }

   @Override
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
      else if (action.equals("import"))
      {
         importData();
      }
      else if (action.equals("close_db"))
      {
         close();
      }
      else if (action.equals("new"))
      {
         DatatoolDb db = new DatatoolDb(settings);

         String name = JOptionPane.showInputDialog(this,
            getMessageHandler().getLabel("message.input_database_name"),
            DEFAULT_UNTITLED);

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

            dbPanel.startCompoundEdit(
               getMessageHandler().getLabel("undo.nullify_column"));

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

            dbPanel.startCompoundEdit(
              getMessageHandler().getLabel("undo.nullify_row"));

            int selectedRow = dbPanel.getModelSelectedRow();
            for (int column = 0, n = dbPanel.getColumnCount(); column < n; column++)
            {
               dbPanel.updateCell(selectedRow, column, DatatoolDb.NULL_VALUE);
            }

            dbPanel.commitCompoundEdit();
         }
      }
      else if (action.equals("empty_to_null"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            DatatoolDbPanel dbPanel = ((DatatoolDbPanel)tab);
            dbPanel.emptyToNull();
         }
      }
      else if (action.equals("null_to_empty"))
      {
         Component tab = tabbedPane.getSelectedComponent();

         if (tab != null && (tab instanceof DatatoolDbPanel))
         {
            DatatoolDbPanel dbPanel = ((DatatoolDbPanel)tab);
            dbPanel.nullToEmpty();
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
         aboutDialog.setVisible(true);
      }
      else if (action.equals("manual"))
      {
         settings.getHelpLib().openHelp();
      }
      else if (action.equals("licence"))
      {
         if (licenceDialog != null)
         {
            licenceDialog.setVisible(true);
         }
      }
      else if (action.equals("undo"))
      {
         DatatoolDbPanel panel 
            = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

         if (panel != null)
         {
            panel.undo();
         }
      }
      else if (action.equals("redo"))
      {
         DatatoolDbPanel panel  
            = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

         if (panel != null)
         {
            panel.redo();
         }
      }
      else if (action.equals("sort"))
      {
         DatatoolDbPanel panel 
            = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

         if (panel != null)
         {
            panel.sortData(); 
         }
      }
      else if (action.equals("shuffle"))
      {
         DatatoolDbPanel panel 
            = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

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
         DatatoolDbPanel panel 
           = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

         if (panel != null)
         {
            findCellDialog.display(panel);
         }
      }
      else if (action.equals("find_again"))
      {
         DatatoolDbPanel panel 
            = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

         if (panel != null)
         {
            findCellDialog.findNext(panel);
         }
      }
      else if (action.equals("replace_text"))
      {
         DatatoolDbPanel panel 
           = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

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

      if ((getExtendedState() & 
          (MAXIMIZED_BOTH | ICONIFIED | MAXIMIZED_VERT | MAXIMIZED_HORIZ)) == 0)
      {
         settings.setWindowSize(getSize());
      }

      try
      {
         settings.directoryOnExit(fileChooser.getCurrentDirectory());
         settings.updateHelpSetSettings();
         settings.saveProperties();
      }
      catch (IOException e)
      {
         getMessageHandler().debug(e);
      }

      getDatatoolTk().exit(0);
   }

   public File getCurrentChooserDirectory()
   {
      return fileChooser.getCurrentDirectory();
   }

   private void setTeXFileFilters(boolean includeTeXFilter)
   {
      setTeXFileFilters(includeTeXFilter, false, FileFormatType.DBTEX, "3.0");
   }

   private void setTeXFileFilters(boolean includeTeXFilter,
      boolean separateVersions, FileFormatType defFmtType, String defVersion)
   {
      File file = fileChooser.getSelectedFile();
      FileFilter current = fileChooser.getFileFilter();
      DatatoolFileFilter defFilter=null;

      fileChooser.resetChoosableFileFilters();

      FileFilter all = fileChooser.getAcceptAllFileFilter();

      fileChooser.removeChoosableFileFilter(all);

      if (separateVersions)
      {
         fileChooser.addChoosableFileFilter(dbtex3Filter);
         fileChooser.addChoosableFileFilter(dbtex2Filter);
         fileChooser.addChoosableFileFilter(dtltex3Filter);
         fileChooser.addChoosableFileFilter(dtltex2Filter);

         if (current != dbtex3Filter && current != dbtex2Filter
              && current != dtltex3Filter && current != dtltex2Filter)
         {
            switch (defFmtType)
            {
               case DBTEX:
                 if ("3.0".equals(defVersion))
                 {
                    defFilter = dbtex3Filter;
                 }
                 else
                 {
                    defFilter = dbtex2Filter;
                 }
               break;
               case DTLTEX:
                 if ("3.0".equals(defVersion))
                 {
                    defFilter = dtltex3Filter;
                 }
                 else
                 {
                    defFilter = dtltex2Filter;
                 }
               break;
            }
         }
      }
      else
      {
         fileChooser.addChoosableFileFilter(dbdtltexFilter);

         if (current != dbdtltexFilter)
         {
            defFilter = dbdtltexFilter;
         }
      }

      if (includeTeXFilter)
      {
         fileChooser.addChoosableFileFilter(texFilter);
      }

      fileChooser.addChoosableFileFilter(all);

      if (defFilter != null)
      {
         fileChooser.setFileFilter(defFilter);

         if (file != null && !defFilter.accept(file))
         {
            String name = file.getName();

            int idx = name.lastIndexOf(".");

            if (idx > 0)
            {
               file = new File(file.getParent(), 
                 String.format("%s.%s", name.substring(0, idx),
                    defFilter.getDefaultExtension()));

               if (file.exists())
               {
                  fileChooser.setSelectedFile(file);
               }
            }
         }
      }
   }

   private void setTeXFileFilter()
   {
      File file = fileChooser.getSelectedFile();
      fileChooser.resetChoosableFileFilters();

      FileFilter all = fileChooser.getAcceptAllFileFilter();

      fileChooser.removeChoosableFileFilter(all);

      fileChooser.addChoosableFileFilter(texFilter);
      fileChooser.addChoosableFileFilter(all);

      fileChooser.setFileFilter(texFilter);

      if (file != null && !texFilter.accept(file))
      {
         String name = file.getName();

         int idx = name.lastIndexOf(".");

         if (idx > 0)
         {
            file = new File(file.getParent(), 
              String.format("%s.%s", name.substring(0, idx),
                 texFilter.getDefaultExtension()));

            if (file.exists())
            {
               fileChooser.setSelectedFile(file);
            }
         }
      }
   }

   public DatatoolDbPanel getPanel(DatatoolDb db)
   {
      for (int i = 0, n = tabbedPane.getTabCount(); i < n; i++)
      {
         DatatoolDbPanel panel 
            = (DatatoolDbPanel)tabbedPane.getComponentAt(i);

         if (panel.getDatabase() == db)
         {
            return panel;
         }
      }

      return null;
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
           getMessageHandler().getLabelWithValues("message.unsaved_data_query",
             panel.getName()),
           getMessageHandler().getLabel("message.unsaved_data"),
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
         getMessageHandler().error(this, 
           getMessageHandler().getLabel("error.nopanel"));
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
         getMessageHandler().error(this, 
           getMessageHandler().getLabel("error.nopanel"));
         return;
      }

      DatatoolDb db = panel.getDatabase();
      FileFormatType fmtType = db.getDefaultFormat();
      String ver = db.getDefaultFileVersion();

      if (settings.getOverrideInputFormat())
      {
         String fmt = settings.getDefaultOutputFormat();

         if (fmt != null)
         {
            Matcher m = DatatoolDb.FORMAT_PATTERN.matcher(fmt);

            if (m.matches())
            { 
               fmtType = FileFormatType.valueOf(m.group(1));
               ver = m.group(1);
            }
         }
      }

      setTeXFileFilters(false, true, fmtType, ver);

      String filename = panel.getName();

      if (fmtType == FileFormatType.DBTEX)
      {
         filename += ".dbtex";
      }
      else
      {
         filename += ".dtltex";
      }

      fileChooser.setSelectedFile(new File(filename));

      if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
      {
         return;
      }

      File file = fileChooser.getSelectedFile();
      FileFilter current = fileChooser.getFileFilter();

      if (current == dbtex2Filter)
      {
         fmtType = FileFormatType.DBTEX;
         ver = "2.0";
      }
      else if (current == dbtex3Filter)
      {
         fmtType = FileFormatType.DBTEX;
         ver = "3.0";
      }
      else if (current == dtltex2Filter)
      {
         fmtType = FileFormatType.DTLTEX;
         ver = "2.0";
      }
      else if (current == dtltex3Filter)
      {
         fmtType = FileFormatType.DTLTEX;
         ver = "3.0";
      }

      panel.save(file, fmtType, ver);
   }

   public void addRecentFile(File file)
   {
      settings.addRecentFile(file);
   }

   public void load()
   {
      setTeXFileFilters(true);

      if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
      {
         load(fileChooser.getSelectedFile());
      }
   }

   public void load(String filename)
   {
      load(new File(filename));
   }

   public void load(File file)
   {
      DatatoolFileLoader loader = new DatatoolFileLoader(this, file);
      loader.execute();
   }

   public DatatoolDb getCurrentDatabase()
   {
      Component tab = tabbedPane.getSelectedComponent();

      if (tab != null && (tab instanceof DatatoolDbPanel))
      {
         return ((DatatoolDbPanel)tab).getDatabase();
      }

      return null;
   }

   public void createNewTab(DatatoolDb db)
   {
      createNewTab(db, false);
   }

   public void createNewTab(DatatoolDb db, boolean modified)
   {
      DatatoolDbPanel panel = new DatatoolDbPanel(this, db);

      panel.setModified(modified);

      tabbedPane.addTab(panel.getName(), null, panel, panel.getToolTipText());

      int idx = tabbedPane.getTabCount()-1;

      tabbedPane.setTabComponentAt(idx, panel.getButtonTabComponent());
      tabbedPane.setSelectedIndex(idx);

      updateTools();
   }

   public void importData()
   {
      importDialog.display();
   }

   public void importData(DatatoolImport imp, String source)
   {
      LoadSettings loadSettings = new LoadSettings(settings);
      loadSettings.setDataImport(imp, source);

      DatatoolFileLoader loader = new DatatoolFileLoader(this, loadSettings);
      loader.execute();
   }

   public void newFromTemplate()
   {
      try
      {
         Template[] templates = settings.getTemplates();

         if (templates.length == 0)
         {
            getMessageHandler().error(this, "error.no_templates");
            return;
         }

         Object result = JOptionPane.showInputDialog(this,
           getMessageHandler().getLabel("template.message"),
           getMessageHandler().getLabel("template.title"),
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
          getMessageHandler().error(this, e);
      }
   }

   public void selectTab(DatatoolDbPanel panel)
   {
      tabbedPane.setSelectedComponent(panel);
   }

   public boolean requestSortDialog(DatatoolDb db)
   {
      return requestSortDialog(db, -1);
   }

   public boolean requestSortDialog(DatatoolDb db, int initColIdx)
   {
      return sortDialog.requestInput(db, initColIdx);
   }

   public String getDefaultUntitled()
   {
      return DEFAULT_UNTITLED;
   }

   public DatatoolHeader requestNewHeader(DatatoolDbPanel panel)
   {
      String label = DEFAULT_UNTITLED;

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

   public void updateTitle()
   {
      int idx = tabbedPane.getSelectedIndex();

      updateTitle(idx == -1 ? null : 
        (DatatoolDbPanel)tabbedPane.getComponentAt(idx));
   }

   public void updateTitle(DatatoolDbPanel panel)
   {
      if (panel == null)
      {
         setTitle(settings.getApplicationName());
      }
      else
      {
         File file = panel.getDatabase().getFile();

         setTitle(String.format("%s - %s", 
            settings.getApplicationName(), 
            file == null ? getDefaultUntitled() : file.getName()));

         tabbedPane.setToolTipTextAt(tabbedPane.indexOfComponent(panel), 
            panel.getToolTipText());
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

      updateTitle(panel);

      boolean enable = (panel != null);

      addColumnBeforeItem.setEnabled(enable);
      addColumnAfterItem.setEnabled(enable);
      editDbNameItem.setEnabled(enable);
      setEmptyToNullItem.setEnabled(enable);
      setNullToEmptyItem.setEnabled(enable);
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

      enable = (tabbedPane.getTabCount() > 0);

      saveItem.setEnabled(enable);
      saveAsItem.setEnabled(enable);
      closeItem.setEnabled(enable);
   }

   public Font getCellFont()
   {
      return settings.getFont();
   }

   public int getCellHeight()
   {
      return settings.getCellHeight();
   }

   @Deprecated
   public int getCellWidth(int type)
   {
      return settings.getCellWidth(type);
   }

   public int getCellWidth(DatumType type)
   {
      return settings.getCellWidth(type);
   }

   public DatatoolProperties getSettings()
   {
      return settings;
   }

   public void updatedCellEditorSettings()
   {
       cellEditor.settingsUpdated();
   }

   public DatatoolTk getDatatoolTk()
   {
      return settings.getDatatoolTk();
   }

   public CombinedMessageHandler getMessageHandler()
   {
      return (CombinedMessageHandler)settings.getMessageHandler();
   }

   public DatatoolGuiResources getResources()
   {
      return resources;
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

   private DatatoolProperties settings;

   private DatatoolGuiResources resources;

   private JTabbedPane tabbedPane;

   private JFileChooser fileChooser;

   private DatatoolFileFilter texFilter, dbdtltexFilter,
     dbtex2Filter, dbtex3Filter, dtltex2Filter, dtltex3Filter;

   private HeaderDialog headerDialog;

   private CellDialog cellEditor;

   private ImportDialog importDialog;

   private HelpFrame helpFrame;

   private ScrollToolBar toolBar;

   private JMenu recentM, pluginsM, toolsM;

   private JMenuItem undoItem, redoItem, editCellItem, 
      addColumnAfterItem, addRowAfterItem,
      addColumnBeforeItem, addRowBeforeItem,
      removeColumnItem, editHeaderItem, editDbNameItem,
      removeRowItem, clearRecentItem, sortItem, shuffleItem,
      findCellItem, findNextItem, replaceItem,
      setToNullItem, deselectItem, setColToNullItem, setRowToNullItem,
      saveItem, saveAsItem, closeItem,
      setNullToEmptyItem, setEmptyToNullItem;

   private ActionListener recentFilesListener;

   private PropertiesDialog propertiesDialog;

   private SortDialog sortDialog;

   private FindCellDialog findCellDialog;

   private ReplaceAllDialog replaceAllDialog;

   private MessageDialog aboutDialog, licenceDialog;

   public static final String LICENCE_PATH = "/gpl-3.0-standalone.html";

   private DatatoolPlugin[] plugins;
   private URL pluginDictURL;

   private String DEFAULT_UNTITLED;

   private DatatoolProgressMessages progressMessages;
}
