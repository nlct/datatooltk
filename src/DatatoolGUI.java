package com.dickimawbooks.datatooltk;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

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

      settings.setPasswordReader(new GuiPasswordReader());

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

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("quit"))
      {
         quit();
      }
   }

   public void quit()
   {
      // TODO check for unsaved data

      System.exit(0);
   }

   public void load(String filename)
   {
   }

   public void importData(DatatoolImport imp, String source)
   {
   }

   private DatatoolSettings settings;
}
