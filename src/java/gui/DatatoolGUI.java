package com.dickimawbooks.datatooltk.gui;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.help.*;

import com.dickimawbooks.datatooltk.*;
import com.dickimawbooks.datatooltk.io.*;

public class DatatoolGUI extends JFrame
  implements ActionListener
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

      String imgFile = "/resources/icons/logosmall.png";

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

      toolBar = new JToolBar(JToolBar.HORIZONTAL);

      JMenu fileM = DatatoolGuiResources.createJMenu("file");
      mbar.add(fileM);

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "new", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK),
        toolBar));

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "open", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK),
        toolBar));

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "importcsv", this, toolBar));

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "save", this, toolBar));

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "save_as", this, toolBar));

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "close", this,
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

               enableEditItems(panel.getSelectedRow(), panel.getSelectedColumn());
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

      fileChooser = new JFileChooser();

      headerDialog = new HeaderDialog(this);
      cellEditor = new CellDialog(this);

      // Set default dimensions

      Toolkit tk = Toolkit.getDefaultToolkit();

      Dimension d = tk.getScreenSize();

      int width = 3*d.width/4;
      int height = d.height/2;

      setSize(width, height);

      setLocationRelativeTo(null);
   }

   private void initHelp()
     throws HelpSetException,FileNotFoundException
   {
      if (mainHelpBroker == null)
      {
         HelpSet mainHelpSet = null;

         String resource = "datatooltk";

         String helpsetLocation = "/resources/helpsets/"+resource;

         String lang    = DatatoolTk.getLanguage();
         String country = DatatoolTk.getCountry();

         URL hsURL = getClass().getResource(helpsetLocation
          + "-" + lang + "-" + country + "/" + resource + ".hs");

         if (hsURL == null)
         {
            hsURL = getClass().getResource(helpsetLocation
              + "-"+lang + "/" + resource + ".hs");

            if (hsURL == null)
            {
               hsURL = getClass().getResource(helpsetLocation
                 + "-en-US/" + resource + ".hs");

               if (hsURL == null)
               {
                  throw new FileNotFoundException(
                    "Can't find helpset files. Tried: \n"
                   + helpsetLocation + "-" + lang + "-" 
                   + country + "/" + resource + ".hs\n"
                   + helpsetLocation + "-" + lang + "/" 
                   + resource + ".hs\n"
                   + helpsetLocation + "-en-US/" + resource + ".hs");
               }
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
      else if (action.equals("close"))
      {
         close();
      }
      else if (action.equals("new"))
      {
         DatatoolDb db = new DatatoolDb();
         db.setName(DatatoolTk.getLabel("default.untitled"));
         createNewTab(db);
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
      setTeXFileFilters();

      if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
      {
         return;
      }

      DatatoolDbPanel panel 
         = (DatatoolDbPanel)tabbedPane.getSelectedComponent();

      if (panel == null)
      {
         DatatoolGuiResources.error(this, 
           DatatoolTk.getLabel("error.nopanel"));
         return;
      }

      panel.save(fileChooser.getSelectedFile());
   }

   public void addRecentFile(File file)
   {
      settings.addRecentFile(file);
   }

   public void load()
   {
      setTeXFileFilters();

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
      try
      {
         createNewTab(DatatoolDb.load(file));

         settings.addRecentFile(file);
      }
      catch (IOException e)
      {
         DatatoolGuiResources.error(this,
           DatatoolTk.getLabelWithValues(
             "error.load.failed", file.toString(), e.getMessage()));
      }
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

   public void importCsv()
   {
      setCsvFileFilters();

      if (fileChooser.showDialog(this, DatatoolTk.getLabel("button.import"))
       != JFileChooser.APPROVE_OPTION)
      {
         return;
      }

      DatatoolCsv imp = new DatatoolCsv(settings);

      try
      {
         createNewTab(imp.importData(fileChooser.getSelectedFile()));
      }
      catch (DatatoolImportException e)
      {
         DatatoolGuiResources.error(this, e);
      }
   }

   public void importData(DatatoolImport imp, String source)
   {
      try
      {
         createNewTab(imp.importData(source));
      }
      catch (DatatoolImportException e)
      {
         DatatoolGuiResources.error(this, e);
      }
   }

   public void selectTab(DatatoolDbPanel panel)
   {
      tabbedPane.setSelectedComponent(panel);
   }

   public DatatoolHeader requestNewHeader(DatatoolDbPanel panel)
   {
      String label = DatatoolTk.getLabel("default.untitled");

      DatatoolHeader header = new DatatoolHeader(label, label);

      if (headerDialog.requestEdit(header, panel.db))
      {
         return header;
      }

      return null;
   }

   public void requestHeaderEditor(int colIdx, DatatoolDbPanel panel)
   {
      if (headerDialog.requestEdit(colIdx, panel.db))
      {
         panel.setModified(true);
         panel.updateColumnHeader(colIdx);
      }
   }

   public void requestCellEditor(int row, int col, DatatoolDbPanel panel)
   {
      if (cellEditor.requestEdit(row, col, panel))
      {
         panel.setModified(true);
      }
   }

   public void enableEditItems(int rowIdx, int colIdx)
   {
      editCellItem.setEnabled(rowIdx > -1 && colIdx > -1);
      removeColumnItem.setEnabled(colIdx > -1);
      removeRowItem.setEnabled(rowIdx > -1);
      editHeaderItem.setEnabled(colIdx > -1);
   }

   public void updateTools()
   {
      DatatoolDbPanel panel = 
         (DatatoolDbPanel)tabbedPane.getSelectedComponent();

      boolean enable = (panel != null);

      addColumnBeforeItem.setEnabled(enable);
      addColumnAfterItem.setEnabled(enable);
      editDbNameItem.setEnabled(enable);

      enable = (enable && panel.getColumnCount() > 0);

      addRowBeforeItem.setEnabled(enable);
      addRowAfterItem.setEnabled(enable);
   }

   private DatatoolSettings settings;

   private JTabbedPane tabbedPane;

   private JFileChooser fileChooser;

   private FileFilter texFilter, dbtexFilter, csvFilter, txtFilter,
     csvtxtFilter;

   private HeaderDialog headerDialog;

   private CellDialog cellEditor;

   private HelpBroker mainHelpBroker;
   private CSH.DisplayHelpFromSource csh;

   private JToolBar toolBar;

   private JMenuItem undoItem, redoItem, editCellItem, 
      addColumnAfterItem, addRowAfterItem,
      addColumnBeforeItem, addRowBeforeItem,
      removeColumnItem, editHeaderItem, editDbNameItem,
      removeRowItem;
}
