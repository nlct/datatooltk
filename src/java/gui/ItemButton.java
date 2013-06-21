package com.dickimawbooks.datatooltk.gui;

import java.net.URL;
import java.awt.Dimension;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

// Provides a JMenuItem that optionally has an associated button in
// the toolbar. The button is only created if there is an associated
// image file.

public class ItemButton extends JMenuItem
{
   public ItemButton(String parentLabel, String actionLabel,
     ActionListener listener, KeyStroke keyStroke, ScrollToolBar toolBar)
   {
      super(DatatoolTk.getLabelRemoveArgs(parentLabel, actionLabel));
      setMnemonic(DatatoolTk.getMnemonic(parentLabel, actionLabel));
      setActionCommand(actionLabel);

      button = null;

      if (toolBar != null)
      {
         URL imageURL = DatatoolGuiResources.getImageUrl(actionLabel);

         if (imageURL != null)
         {
            Icon icon = new ImageIcon(imageURL);
            button = new JButton(icon);
            button.setActionCommand(actionLabel);
            button.putClientProperty("hideActionText", Boolean.TRUE);
            button.setHorizontalTextPosition(JButton.CENTER);
            button.setVerticalTextPosition(JButton.BOTTOM);
            button.setPreferredSize(
             new Dimension(icon.getIconWidth()+border,
             icon.getIconHeight()+border));
            button.setMaximumSize(button.getPreferredSize());
            button.setContentAreaFilled(false);

            Border border = button.getBorder();
            button.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createEmptyBorder(2,2,2,2), border));

            toolBar.addButton(button);
         } 
      }

      String tooltip = DatatoolTk.getToolTip(parentLabel, actionLabel);

      if (tooltip != null)
      {
         setToolTipText(tooltip);

         if (button != null)
         {
            button.setToolTipText(tooltip);
         }
      }
      else if (button != null)
      {
         button.setToolTipText(getText());
      }

      if (listener != null)
      {
         addActionListener(listener);

         if (button != null)
         {
            button.addActionListener(listener);
         }
      }

      if (keyStroke != null)
      {
         setAccelerator(keyStroke);
      }
   }

   public JButton getButton()
   {
      return button;
   }

   public void setEnabled(boolean enabled)
   {
      super.setEnabled(enabled);

      if (button != null)
      {
         button.setEnabled(enabled);
      }
   }

   private JButton button;

   private int border=10;
}
