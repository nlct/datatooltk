package com.dickimawbooks.datatooltk.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

public class ScrollToolBar extends JPanel
   implements ActionListener
{
   public ScrollToolBar(int orientation)
   {
      super();
      setLayout(new BorderLayout());

      beginComponent = Box.createVerticalBox();
      beginComponent.add(createNavButton("scrollback"));
      beginComponent.add(createNavButton("blockscrollback"));
      beginComponent.add(createNavButton("scrollhome"));

      super.add(beginComponent, BorderLayout.WEST);

      toolPanel = new JPanel(null);

      toolPanel.setLayout(new BoxLayout(toolPanel,
         orientation == SwingConstants.HORIZONTAL ?
           BoxLayout.LINE_AXIS : BoxLayout.PAGE_AXIS));

      scrollPane = new JScrollPane(toolPanel,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

      scrollPane.setBorder(BorderFactory.createEmptyBorder());

      super.add(scrollPane, BorderLayout.CENTER);

      endComponent = new JPanel();
      endComponent.add(createNavButton("scrollforward"));
      super.add(endComponent, BorderLayout.EAST);
   }

   private JButton createNavButton(String action)
   {
      JButton button = DatatoolGuiResources.createActionButton("button",
        action, this, null, DatatoolTk.getLabel("button", action));

      Icon icon = button.getIcon();

      if (icon != null)
      {
         button.setText(null);
         button.setContentAreaFilled(false);
         button.setPreferredSize(new Dimension(icon.getIconWidth()+2,
           icon.getIconHeight()+2));
         button.setBorder(BorderFactory.createEmptyBorder());
      }

      return button;
   }

   public void actionPerformed(ActionEvent event)
   {
   }

   public void addButton(JComponent comp)
   {
      toolPanel.add(comp);
   }

   public void addSeparator()
   {
      toolPanel.add(new JToolBar.Separator());
   }

   private JScrollPane scrollPane;
   private JComponent toolPanel;
   private JComponent beginComponent, endComponent;
}
