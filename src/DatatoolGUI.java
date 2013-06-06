package com.dickimawbooks.datatooltk;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.IOException;
import java.io.File;

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

      JMenuBar mbar = new JMenuBar();
      setJMenuBar(mbar);

      JMenu fileM = DatatoolGuiResources.createJMenu("file");
      mbar.add(fileM);

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "quit", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK)));

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "open", this));

      fileM.add(DatatoolGuiResources.createJMenuItem(
        "file", "importcsv", this));

      settings.setPasswordReader(new GuiPasswordReader(this));

      // main panel

      tabbedPane = new JTabbedPane();
      getContentPane().add(tabbedPane, "Center");

      fileChooser = new JFileChooser();

      // Set default dimensions

      Toolkit tk = Toolkit.getDefaultToolkit();

      Dimension d = tk.getScreenSize();

      int width = 3*d.width/4;
      int height = d.height/2;

      setSize(width, height);

      setLocationRelativeTo(null);
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
      else if (action.equals("open"))
      {
         load();
      }
      else if (action.equals("importcsv"))
      {
         importCsv();
      }
   }

   public void quit()
   {
      // TODO check for unsaved data

      System.exit(0);
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

   public void load()
   {
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
         DatatoolDb db = DatatoolDb.load(file);

         DatatoolDbPanel panel = new DatatoolDbPanel(db);

         tabbedPane.addTab(panel.getName(), panel);
      }
      catch (IOException e)
      {
         DatatoolGuiResources.error(this, e);
      }
   }

   public void importCsv()
   {
      if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
      {
         return;
      }

      DatatoolCsv imp = new DatatoolCsv(settings);

      try
      {
         DatatoolDb db = imp.importData(fileChooser.getSelectedFile());

         DatatoolDbPanel panel = new DatatoolDbPanel(db);

         tabbedPane.addTab(panel.getName(), panel);
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
         DatatoolDb db = imp.importData(source);

         DatatoolDbPanel panel = new DatatoolDbPanel(db);

         tabbedPane.addTab(panel.getName(), panel);
      }
      catch (DatatoolImportException e)
      {
         DatatoolGuiResources.error(this, e);
      }
   }

   private DatatoolSettings settings;

   private JTabbedPane tabbedPane;

   private JFileChooser fileChooser;
}
