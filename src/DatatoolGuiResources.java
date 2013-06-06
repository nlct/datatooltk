package com.dickimawbooks.datatooltk;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import javax.swing.*;

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
       return createOkayButton(listener, null);
    }

    public static JButton createOkayButton(ActionListener listener, String tooltipText)
    {
       return createActionButton(
          DatatoolTk.getLabel("button.okay"),
          DatatoolTk.getMnemonic("button.okay"),
          "okay", listener,
          KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), tooltipText);
    }

    public static JButton createCloseButton(ActionListener listener)
    {
       return createCloseButton(listener, null);
    }

    public static JButton createCloseButton(ActionListener listener, String tooltipText)
    {
       return createActionButton(
          DatatoolTk.getLabel("button.close"),
          DatatoolTk.getMnemonic("button.close"),
          "okay", listener,
          KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), tooltipText);
    }

    public static JButton createCancelButton(ActionListener listener)
    {
       return createCancelButton(listener, null);
    }

    public static JButton createCancelButton(ActionListener listener, String tooltipText)
    {
       return createActionButton(
          DatatoolTk.getLabel("button.cancel"),
          DatatoolTk.getMnemonic("button.cancel"),
          "cancel", listener,
          KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), tooltipText);
    }

    public static JButton createActionButton(String label, char mnemonic,
      String actionCommand, ActionListener listener, KeyStroke keyStroke, 
      String tooltipText)
    {
       JButton button = new JButton(label);
       button.setMnemonic(mnemonic);

       if (listener != null)
       {
          button.addActionListener(listener);

          if (actionCommand != null)
          {
             button.setActionCommand(actionCommand);

             if (keyStroke != null)
             {
                button.registerKeyboardAction(listener, actionCommand, keyStroke,
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

    private static ErrorPanel errorPanel = new ErrorPanel();
}
