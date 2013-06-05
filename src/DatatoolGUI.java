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

      JMenu fileM = createMenu("file");
      mbar.add(fileM);

      fileM.add(createMenuItem("quit",
        KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK)));

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

   private JMenu createMenu(String label)
   {
      JMenu menu = new JMenu(label);

      return menu;
   }

   private JMenuItem createMenuItem(String name)
   {
      return createMenuItem(name, this, null);
   }

   private JMenuItem createMenuItem(String name, ActionListener listener)
   {
      return createMenuItem(name, listener, null);
   }

   private JMenuItem createMenuItem(String name, KeyStroke keyStroke)
   {
      return createMenuItem(name, this, keyStroke);
   }

   private JMenuItem createMenuItem(String name, ActionListener listener, KeyStroke keyStroke)
   {
      JMenuItem item = new JMenuItem(name);
      item.setActionCommand(name);

      if (listener != null)
      {
         item.addActionListener(listener);
      }

      if (keyStroke != null)
      {
         item.setAccelerator(keyStroke);
      }

      return item;
   }

   public static void error(Component parent, Exception e)
   {
      JOptionPane.showMessageDialog(parent, e.getMessage(), "Error",
         JOptionPane.ERROR_MESSAGE);

      e.printStackTrace();
   }

   public static void error(Component parent, String message)
   {
      JOptionPane.showMessageDialog(parent, message, "Error",
         JOptionPane.ERROR_MESSAGE);
   }

   public void error(Exception e)
   {
      error(this, e);
   }

   public void error(String message)
   {
      error(this, message);
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
      else if (action.equals("load"))
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
         error("No Current Panel!");
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
         error("No Current Panel!");
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
         error(e);
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
         error(e);
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
         error(e);
      }
   }

   private DatatoolSettings settings;

   private JTabbedPane tabbedPane;

   private JFileChooser fileChooser;
}
