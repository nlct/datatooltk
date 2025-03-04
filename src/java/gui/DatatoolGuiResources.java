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

import java.awt.*;
import java.util.*;
import java.util.regex.Pattern;
import java.io.*;
import java.net.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;

import com.dickimawbooks.texjavahelplib.IconSet;
import com.dickimawbooks.texjavahelplib.JLabelGroup;
import com.dickimawbooks.texjavahelplib.TeXJavaHelpLib;
import com.dickimawbooks.texjavahelplib.TJHAbstractAction;

import com.dickimawbooks.datatooltk.DatatoolTk;

/**
 * Application GUI resources.
 */
public class DatatoolGuiResources
{
    public DatatoolGuiResources(DatatoolGUI gui,
       CombinedMessageHandler messageHandler)
    {
       this.gui = gui;
       this.messageHandler = messageHandler;
       messageHandler.setDatatoolGuiResources(this);
       errorPanel = new ErrorPanel(this);
       initLabels();
    }

   private void initLabels()
   {
      DatumType[] types = DatumType.values();

      TYPE_LABELS = new String[types.length];
      TYPE_MNEMONICS = new int[types.length];

      for (int i = 0; i < types.length; i++)
      {
         String tag = types[i].getTag();
         TYPE_LABELS[i] = getMessage("header.type."+tag);
         TYPE_MNEMONICS[i] = getMnemonicInt("header.type."+tag);
      } 
   }

   public String getTypeLabel(DatumType type)
   {
      return TYPE_LABELS[type.getValue()+1];
   }

   public String[] getTypeLabels()
   {
      return TYPE_LABELS;
   }

   public int getTypeMnemonic(DatumType type)
   {
      return TYPE_MNEMONICS[type.getValue()+1];
   }

   public int[] getTypeMnemonics()
   {
      return TYPE_MNEMONICS;
   }

   public boolean isCellDatumVisible()
   {
      return gui.getSettings().isCellDatumVisible();
   }

   public DatatoolProperties getSettings()
   {
      return gui.getSettings();
   }

    public void error(Component parent, String message)
    {
       errorPanel.updateMessage(message);

       JOptionPane.showMessageDialog(parent, errorPanel,
          messageHandler.getLabelWithAlt("error.title", "Error"),
          JOptionPane.ERROR_MESSAGE);
    }

    public void error(Component parent, Throwable e)
    {
       String message = messageHandler.getMessage(e);

       if (message == null)
       {
          message = e.getClass().getName();
       }

       Throwable cause = e.getCause();

       if (cause != null)
       {
          message = String.format("%s%n%s", message, 
           messageHandler.getMessage(cause));
       }

       error(parent, message, e);
    }

    public void error(Component parent, String message, Throwable e)
    {
       errorPanel.updateMessage(message, e);

       JOptionPane.showMessageDialog(parent, errorPanel,
          messageHandler.getLabelWithAlt("error.title", "Error"),
         JOptionPane.ERROR_MESSAGE);
    }

    public void warning(Component parent, String message)
    {
       errorPanel.updateMessage(message);

       JOptionPane.showMessageDialog(parent,
          errorPanel,
          messageHandler.getLabelWithAlt("error.warning", "Warning"),
          JOptionPane.WARNING_MESSAGE);
    }

   public String getLabel(String parentLabel, String propLabel)
   {
      return messageHandler.getLabel(parentLabel, propLabel);
   }

   public String getMessage(String propLabel, Object... params)
   {
      return getHelpLib().getMessage(propLabel, params);
   }

   public String getToolTip(String label)
   {
      return getToolTip(null, label);
   }

   public String getToolTip(String parent, String label)
   {
      String propLabel;

      if (parent == null)
      {
         propLabel = String.format("%s.tooltip", label);
      }
      else
      {
         propLabel = String.format("%s.%s.tooltip", parent, label);
      }

      return getHelpLib().getMessageIfExists(propLabel);
   }

   public int getMnemonicInt(String label)
   {
      return getMnemonicInt(null, label);
   }

   public int getMnemonicInt(String parent, String label)
   {
      String propLabel;

      if (parent == null)
      {
         propLabel = String.format("%s.mnemonic", label);
      }
      else
      {
         propLabel = String.format("%s.%s.mnemonic", parent, label);
      }

      String msg = getHelpLib().getMessageIfExists(propLabel);

      if (msg == null || msg.isEmpty())
      {
         return -1;
      }

      return msg.codePointAt(0);
   }

   public KeyStroke getKeyStroke(String property)
   {
      return getHelpLib().getKeyStroke(property);
   }

   public KeyStroke getKeyStroke(String parent, String label)
   {
      String propLabel;

      if (parent == null)
      {
         propLabel = String.format("%s.mnemonic", label);
      }
      else
      {
         propLabel = String.format("%s.%s.mnemonic", parent, label);
      }

      return getHelpLib().getKeyStroke(propLabel);
   }

    public JButton createOkayButton(ActionListener listener)
    {
       return createActionButton("button", "okay", 
          listener, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
    }

    public JButton createCancelButton(ActionListener listener)
    {
       return createActionButton("button", "cancel", listener,
          KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
    }

    public JButton createActionButton(String parent, String label, 
      ActionListener listener, KeyStroke keyStroke)
    {
       return createActionButton(parent, label, listener, keyStroke,
         getToolTip(parent, label));
    }

    public JButton createActionButton(String parent, String label, 
      String iconPrefix, ActionListener listener, KeyStroke keyStroke)
    {
       return createActionButton(parent, label, iconPrefix, true,
         listener, keyStroke, getToolTip(parent, label));
    }

    public JButton createActionButton(String parent, String label, 
      ActionListener listener, KeyStroke keyStroke,
      String tooltipText)
    {
       return createActionButton(parent, label, listener, keyStroke,
         tooltipText, getImageIconSet(label, true));
    }

    public JButton createActionButton(String parent, String label,
      boolean prefSmallIconSet,
      ActionListener listener, KeyStroke keyStroke,
      boolean omitTextIfIcon)
    {
       return createActionButton(parent, label, label, prefSmallIconSet,
        listener, keyStroke, omitTextIfIcon);
    }

    public JButton createActionButton(String parent, String label,
      String iconPrefix, boolean prefSmallIconSet,
      ActionListener listener, KeyStroke keyStroke, boolean omitTextIfIcon)
    {
       return createActionButton(parent, label, iconPrefix, true,
         listener, keyStroke, getToolTip(parent, label),
         omitTextIfIcon);
    }

    public JButton createActionButton(String parent, String label,
      String iconPrefix, boolean prefSmallIconSet,
      ActionListener listener, KeyStroke keyStroke,
      String tooltipText)
    {
       return createActionButton(parent, label, iconPrefix, prefSmallIconSet,
         listener, keyStroke, tooltipText, false);
    }

    public JButton createActionButton(String parent, String label,
      String iconPrefix, boolean prefSmallIconSet,
      ActionListener listener, KeyStroke keyStroke,
      String tooltipText, boolean omitTextIfIcon)
    {
       IconSet iconSet = getImageIconSet(iconPrefix, prefSmallIconSet);

       if (prefSmallIconSet && iconSet == null)
       {
          iconSet = getImageIconSet(iconPrefix, false);
       }

       return createActionButton(parent, label, listener, keyStroke,
         tooltipText, iconSet, omitTextIfIcon);
    }

    public JButton createActionButton(String parent, String label, 
      ActionListener listener, KeyStroke keyStroke, IconSet iconSet,
      boolean omitTextIfIcon)
    {
       return createActionButton(parent, label, listener, keyStroke,
         getToolTip(parent, label), iconSet, omitTextIfIcon);
    }

    public JButton createActionButton(String parent, String label, 
      ActionListener listener, KeyStroke keyStroke,
      String tooltipText, IconSet iconSet)
    {
       return createActionButton(parent, label, 
      listener, keyStroke, tooltipText, iconSet, false);
    }

   public JButton createActionButton(String parent, String label, 
     ActionListener listener, KeyStroke keyStroke,
     String tooltipText, IconSet iconSet, boolean omitTextIfIcon)
   {
      String buttonLabel = messageHandler.getLabel(parent, label);
      int mnemonic = getMnemonicInt(parent, label);
      String tag = parent == null ? label : parent+"."+label;

      String actionCommand = label;

      JButton button;

      // Is there an associated image?

      if (iconSet == null)
      {
         button = new JButton(buttonLabel);
      }
      else
      {
         if (omitTextIfIcon)
         {
            button = new JButton(iconSet.getDefaultIcon());
            button.setMargin(new Insets(0, 0, 0, 0));

            if (tooltipText == null)
            {
               tooltipText = buttonLabel;
            }
         }
         else
         {
            button = new JButton(buttonLabel, iconSet.getDefaultIcon());
         }

         iconSet.setButtonExtraIcons(button);
      }

      if (mnemonic != -1)
      {
         button.setMnemonic(mnemonic);
      }

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

      String desc = messageHandler.getMessageIfExists(tag+".description");

      if (desc != null)
      {
         button.getAccessibleContext().setAccessibleDescription(desc);
      }

      return button;
   }

    public JButton createActionButton(String parent, String label, 
      ActionListener listener, KeyStroke keyStroke,
      String tooltipText, ImageIcon imageIcon)
    {
       String buttonLabel = messageHandler.getLabel(parent, label);
       int mnemonic = getMnemonicInt(parent, label);
       String actionCommand = label;

       JButton button;

       // Is there an associated image?

       if (imageIcon == null)
       {
          button = new JButton(buttonLabel);
       }
       else
       {
          button = new JButton(buttonLabel, imageIcon);
       }

       if (mnemonic != -1)
       {
          button.setMnemonic(mnemonic);
       }

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

    public JLabel createJLabel(String label)
    {
       return createJLabel(label, null);
    }

    public JLabel createJLabel(String label, float alignment)
    {
       return createJLabel(label, null, alignment);
    }

    public JLabel createJLabel(String label, JComponent comp)
    {
       JLabel jLabel = new JLabel(messageHandler.getLabel(label));

       int mnemonic = getMnemonicInt(label);

       if (mnemonic != -1)
       {
          jLabel.setDisplayedMnemonic(mnemonic);
       }

       String tooltip = getToolTip(label);

       if (tooltip != null)
       {
          jLabel.setToolTipText(tooltip);
       }

       if (comp != null)
       {
          jLabel.setLabelFor(comp);
       }

       return jLabel;
   }

   public JLabel createJLabel(JLabelGroup grp, String label, JComponent comp)
   {
      return grp.createJLabel(messageHandler.getLabel(label),
         getMnemonicInt(label), getToolTip(label), comp);
   }

    public JLabel createJLabel(String label, JComponent comp, float alignment)
    {
       JLabel jlabel = createJLabel(label, comp);
       jlabel.setAlignmentX(alignment);
       return jlabel;
    }

   public JRadioButton createJRadioButton(String parentLabel,
      String label, ButtonGroup bg, ActionListener listener)
   {
      return createJRadioButton(parentLabel, label, label, bg, listener);
   }

   public JRadioButton createJRadioButton(String parentLabel,
      String label, String action, ButtonGroup bg, ActionListener listener)
   {
      JRadioButton button = new JRadioButton(
        messageHandler.getLabel(parentLabel, label));

      button.setMnemonic(getMnemonicInt(parentLabel, label));

      String tooltip = getToolTip(parentLabel, label);

      if (tooltip != null)
      {
         button.setToolTipText(tooltip);
      }

      button.setActionCommand(action);

      if (listener != null)
      {
         button.addActionListener(listener);
      }

      bg.add(button);

      return button;
   }

   public JRadioButton createJRadioButton(String parentLabel,
      String label, ButtonGroup bg, ActionListener listener,
      int alignment)
   {
      JRadioButton button = createJRadioButton(parentLabel,
        label, bg, listener);
      button.setAlignmentX(alignment);
      return button;
   }

   public JRadioButton createJRadioButton(String parentLabel,
      String label, String action, ButtonGroup bg, ActionListener listener,
      float alignment)
   {
      JRadioButton button = createJRadioButton(parentLabel,
        label, action, bg, listener);
      button.setAlignmentX(alignment);
      return button;
   }

    public JCheckBox createJCheckBox(String parentLabel, String label,
       ActionListener listener)
    {
       return createJCheckBox(parentLabel, label, label, listener);
    }

    public JCheckBox createJCheckBox(String parentLabel, String label,
       String action, ActionListener listener)
    {
       JCheckBox checkBox = new JCheckBox(
          messageHandler.getLabel(parentLabel, label));
       checkBox.setMnemonic(getMnemonicInt(parentLabel, label));
       checkBox.setActionCommand(action);

       if (listener != null)
       {
          checkBox.addActionListener(listener);
       }

       return checkBox;
    }

    public JCheckBox createJCheckBox(String parentLabel, String label,
       ActionListener listener, float alignment)
    {
       JCheckBox comp = createJCheckBox(parentLabel, label, 
         listener);
       comp.setAlignmentX(alignment);
       return comp;
    }

   public JTextArea createMessageArea()
   {
      return createMessageArea(8, 30);
   }

   public JTextArea createMessageArea(String label)
   {
      return createMessageArea(8, 30, label);
   }

   public JTextArea createMessageArea(int rows, int cols, String label)
   {
      JTextArea area = createMessageArea(rows, cols);

      area.setText(messageHandler.getLabel(label));

      return area;
   }

   public JTextArea createMessageArea(int rows, int cols)
   {
      JTextArea area = new JTextArea(rows, cols);
      area.setWrapStyleWord(true);
      area.setLineWrap(true);
      area.setEditable(false);
      area.setBorder(null);
      area.setOpaque(false);

      return area;
   }

    public JComponent createOkayCancelHelpPanel(
       ActionListener listener, DatatoolGUI gui, String helpId)
    {
       JPanel buttonPanel = new JPanel();

       JButton okayButton = createOkayButton(listener);

       buttonPanel.add(okayButton);
       buttonPanel.add(createCancelButton(listener));

       TJHAbstractAction helpAction = gui.createHelpAction(helpId);

       if (helpAction != null)
       {
          buttonPanel.add(new JButton(helpAction));
       }

       return buttonPanel;
    }

    public JComponent createOkayCancelHelpPanel(
       JDialog owner, ActionListener listener, DatatoolGUI gui, String helpId)
    {
       JPanel buttonPanel = new JPanel();

       JButton okayButton = createOkayButton(listener);

       buttonPanel.add(okayButton);
       buttonPanel.add(createCancelButton(listener));

       JButton helpButton = gui.createHelpButton(owner, helpId);

       if (helpButton != null)
       {
          buttonPanel.add(helpButton);
       }

       owner.getRootPane().setDefaultButton(okayButton);

       return buttonPanel;
    }

    /**
     * Create okay/cancel/help panel for modal dialogs.
     */
    public JComponent createDialogOkayCancelHelpPanel(
       JDialog dialog, ActionListener listener, DatatoolGUI gui, String helpId)
    {
       JPanel buttonPanel = new JPanel();

       JButton okayButton = createOkayButton(listener);

       buttonPanel.add(okayButton);
       buttonPanel.add(createCancelButton(listener));

       JButton helpButton = gui.createHelpButton(dialog, helpId);

       if (helpButton != null)
       {
          buttonPanel.add(helpButton);
       }

       dialog.getRootPane().setDefaultButton(okayButton);

       return buttonPanel;
    }

    public JComponent createOkayCancelPanel(
       ActionListener listener)
    {
       return createOkayCancelPanel(null, listener);
    }

    public JComponent createOkayCancelPanel(
       JRootPane rootPane,
       ActionListener listener)
    {
       JPanel buttonPanel = new JPanel();

       JButton okayButton = createOkayButton(listener);

       buttonPanel.add(okayButton);
       buttonPanel.add(createCancelButton(listener));

       if (rootPane != null)
       {
          rootPane.setDefaultButton(okayButton);
       }

       return buttonPanel;
    }

    public JMenu createJMenu(String label)
    {
       return createJMenu(null, label);
    }

    public JMenu createJMenu(String parent, String label)
    {
       String propName;

       if (parent == null)
       {
          propName = "menu."+label;
       }
       else
       {
          propName = "menu."+parent+"."+label;
       }

       String text = messageHandler.getMessageIfExists(propName);

       if (text == null)
       {
          propName = (parent == null ? label : parent + "." + label);
       }

       text = messageHandler.getLabel(propName);

       JMenu menu = new JMenu(text);
       menu.setMnemonic(getMnemonicInt(propName));

       String tooltip = getToolTip(propName);

       if (tooltip != null)
       {
          menu.setToolTipText(tooltip);
       }

       return menu;
    }

    public JMenuItem createJMenuItem(String parent, String label)
    {
       return createJMenuItem(parent, label, null, null, null);
    }

    public JMenuItem createJMenuItem(String parent, String label,
       ActionListener listener)
    {
       return createJMenuItem(parent, label, listener, null, null);
    }

    public JMenuItem createJMenuItem(String parent, String label,
       ActionListener listener, ScrollToolBar toolBar)
    {
       return createJMenuItem(parent, label, listener, null, toolBar);
    }

    public JMenuItem createJMenuItem(String parent, String label,
     ActionListener listener, KeyStroke keyStroke)
    {
       return createJMenuItem(parent, label, listener, keyStroke, null);
    }

    public JMenuItem createJMenuItem(String parent, String label,
     ActionListener listener, KeyStroke keyStroke, ScrollToolBar toolBar)
    {
       return createJMenuItem(parent, label, label, listener, keyStroke, toolBar);
    }

    public JMenuItem createJMenuItem(String parent, String label, String iconPrefix,
     ActionListener listener, KeyStroke keyStroke, ScrollToolBar toolBar)
    {
       if (parent == null)
       {
          parent = "menu";
       }
       else if (!parent.contains("menu.") && !parent.endsWith("menu"))
       {
          parent = "menu."+parent;
       }

       return new ItemButton(this, parent, label, iconPrefix,
         listener, keyStroke, toolBar);
    }

    public ImageIcon getImageIcon(String action)
    {
       return getImageIcon(action, false);
    }

    public ImageIcon getImageIcon(String action, boolean small)
    {
       return getHelpLib().getHelpIcon(action, small);
    }

    public IconSet getImageIconSet(String action)
    {
       return getImageIconSet(action, false);
    }

    public IconSet getImageIconSet(String action, boolean small)
    {
       return getHelpLib().getHelpIconSet(action, small);
    }

    public CombinedMessageHandler getMessageHandler()
    {
       return messageHandler;
    }

    public TeXJavaHelpLib getHelpLib()
    {
       return messageHandler.getHelpLib();
    }

    public void progress(int percentage)
    {
       progress(null, percentage);
    }

    public void progress(String msg)
    {
       progress(msg, -1);
    }

    public void progress(String msg, int percentage)
    {
       if (progressMonitor != null)
       {
          progressMonitor.publishProgress(msg, percentage);
       }
    }

    public void setProgressMonitor(ProgressMonitor monitor)
    {
       this.progressMonitor = monitor;
    }

    public ProgressMonitor getProgressMonitor()
    {
       return progressMonitor;
    }

   public DatatoolGUI getGUI()
   {
      return gui;
   }

   public DatatoolTk getDatatoolTk()
   {
      return messageHandler.getDatatoolTk();
   }

   private ErrorPanel errorPanel;

   private CombinedMessageHandler messageHandler;

   private DatatoolGUI gui;

   private ProgressMonitor progressMonitor=null;

   public static final Pattern PATTERN_CS = Pattern.compile(
      "((?:\\\\[^a-zA-Z]{1})|(?:\\\\[a-zA-Z]+)|(?:[#~\\{\\}\\^\\$_])|(?:%.*))");

   private static String[] TYPE_LABELS = null;
   private static int[] TYPE_MNEMONICS = null;

}
