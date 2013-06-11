package com.dickimawbooks.datatooltk.gui;

import java.net.URL;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

// Provides a JMenuItem that optionally has an associated button in
// the toolbar. The button is only created if there is an associated
// image file.

public class ItemButton extends JMenuItem
{
   public ItemButton(String parentLabel, String actionLabel,
     ActionListener listener, KeyStroke keyStroke, JToolBar toolBar)
   {
      super(DatatoolTk.getLabel(parentLabel, actionLabel));
      setMnemonic(DatatoolTk.getMnemonic(parentLabel, actionLabel));
      setActionCommand(actionLabel);

      button = null;

      if (toolBar != null)
      {
         URL imageURL = DatatoolGuiResources.getImageUrl(actionLabel);

         if (imageURL != null)
         {
            button = new JButton(new ImageIcon(imageURL));

            toolBar.add(button);
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
}
