package com.dickimawbooks.datatooltk.gui;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

public class DatatoolGuiResources
{
    public static synchronized void error(Component parent, String message)
    {
       DatatoolTk.debug("Error: '"+message+"'");

       errorPanel.updateMessage(message);

       JOptionPane.showMessageDialog(parent, errorPanel,
          DatatoolTk.getLabelWithAlt("error.title", "Error"),
          JOptionPane.ERROR_MESSAGE);
    }

    public static synchronized void error(Component parent, Exception e)
    {
       String message = e.getMessage();

       if (message == null)
       {
          message = e.getClass().getName();
       }

       error(parent, message, e);
    }

    public static void error(Component parent, String message, Exception e)
    {
       DatatoolTk.debug("Exception: "+e.getClass().toString()+" '"+message+"'");

       errorPanel.updateMessage(message, e);

       JOptionPane.showMessageDialog(parent, errorPanel,
          DatatoolTk.getLabelWithAlt("error.title", "Error"),
         JOptionPane.ERROR_MESSAGE);

       e.printStackTrace();
    }

    public static void warning(Component parent, String message)
    {
       DatatoolTk.debug("Warning: '"+message+"'");

       JOptionPane.showMessageDialog(parent,
          message,
          DatatoolTk.getLabelWithAlt("error.warning", "Warning"),
          JOptionPane.WARNING_MESSAGE);
    }

    public static JButton createOkayButton(ActionListener listener)
    {
       return createActionButton("button", "okay", 
          listener, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
    }

    public static JButton createCancelButton(ActionListener listener)
    {
       return createActionButton("button", "cancel", listener,
          KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
    }

    public static JButton createActionButton(String parent, String label, 
      ActionListener listener, KeyStroke keyStroke)
    {
       return createActionButton(parent, label, listener, keyStroke,
         DatatoolTk.getToolTip(parent, label));
    }

    public static JButton createActionButton(String parent, String label, 
      ActionListener listener, KeyStroke keyStroke,
      String tooltipText)
    {
       String buttonLabel = DatatoolTk.getLabel(parent, label);
       char mnemonic = DatatoolTk.getMnemonic(parent, label);
       String actionCommand = label;

       // Is there an associated image?

       URL imageURL = DatatoolTk.class.getResource(
          "/resources/icons/"+label+".png");

       JButton button;

       if (imageURL == null)
       {
          button = new JButton(buttonLabel);
       }
       else
       {
          button = new JButton(buttonLabel, new ImageIcon(imageURL));
       }

       button.setMnemonic(mnemonic);

       if (listener != null)
       {
          button.addActionListener(listener);

          if (actionCommand != null)
          {
             button.setActionCommand(actionCommand);

             if (keyStroke != null)
             {
                button.registerKeyboardAction(listener,
                  actionCommand, keyStroke,
                  JComponent.WHEN_IN_FOCUSED_WINDOW);
             }
          }
       }

       if (tooltipText != null)
       {
          button.setToolTipText(tooltipText);
       }

       return button;
    }

    public static JLabel createJLabel(String label)
    {
       JLabel jLabel = new JLabel(DatatoolTk.getLabel(label));
       jLabel.setDisplayedMnemonic(DatatoolTk.getMnemonic(label));

       String tooltip = DatatoolTk.getToolTip(label);

       if (tooltip != null)
       {
          jLabel.setToolTipText(tooltip);
       }

       return jLabel;
    }

    public static JMenu createJMenu(String label)
    {
       return createJMenu(null, label);
    }

    public static JMenu createJMenu(String parent, String label)
    {
       JMenu menu = new JMenu(DatatoolTk.getLabel(parent, label));
       menu.setMnemonic(DatatoolTk.getMnemonic(parent, label));

       String tooltip = DatatoolTk.getToolTip(parent, label);

       if (tooltip != null)
       {
          menu.setToolTipText(tooltip);
       }

       return menu;
    }

    public static JMenuItem createJMenuItem(String parent, String label)
    {
       return createJMenuItem(parent, label, null, null, null);
    }

    public static JMenuItem createJMenuItem(String parent, String label,
       ActionListener listener)
    {
       return createJMenuItem(parent, label, listener, null, null);
    }

    public static JMenuItem createJMenuItem(String parent, String label,
       ActionListener listener, JToolBar toolBar)
    {
       return createJMenuItem(parent, label, listener, null, toolBar);
    }

    public static JMenuItem createJMenuItem(String parent, String label,
     ActionListener listener, KeyStroke keyStroke)
    {
       return createJMenuItem(parent, label, listener, keyStroke, null);
    }

    public static JMenuItem createJMenuItem(String parent, String label,
     ActionListener listener, KeyStroke keyStroke, JToolBar toolBar)
    {
       JMenuItem item = new JMenuItem(DatatoolTk.getLabel(parent, label));
       item.setMnemonic(DatatoolTk.getMnemonic(parent, label));
       item.setActionCommand(label);

       String tooltip = DatatoolTk.getToolTip(parent, label);

       if (tooltip != null)
       {
          item.setToolTipText(tooltip);
       }

       if (listener != null)
       {
          item.addActionListener(listener);
       }

       if (keyStroke != null)
       {
          item.setAccelerator(keyStroke);
       }

       if (toolBar != null)
       {
          URL imageURL = DatatoolTk.class.getResource(
             "/resources/icons/"+label+".png");

          if (imageURL != null)
          {
             JButton button = new JButton(new ImageIcon(imageURL));
             button.setActionCommand(label);

             if (listener != null)
             {
                button.addActionListener(listener);
             }

             toolBar.add(button);
          }
       }

       return item;
    }

    private static ErrorPanel errorPanel = new ErrorPanel();
}
