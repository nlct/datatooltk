package com.dickimawbooks.datatooltk.gui;

import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import javax.swing.*;

public class RowHeaderComponent extends JPanel
{
   public RowHeaderComponent(DatatoolDbPanel dbPanel)
   {
      super();
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

      this.panel = dbPanel;

      for (int i = 0, n = panel.getRowCount(); i < n; i++)
      {
         RowButton button = new RowButton(i, panel);
         add(button);
      }
   }

   private DatatoolDbPanel panel;
}

class RowButton extends JPanel
{
   private DatatoolDbPanel panel;
   private int row;
   private JLabel label;

   private int padx=10;

   public RowButton(int row, DatatoolDbPanel panel)
   {
      super(new BorderLayout());

      setBorder(BorderFactory.createRaisedBevelBorder());

      label = new JLabel(""+(row+1));

      add(label, BorderLayout.CENTER);

      this.row = row;
      this.panel = panel;

      addMouseListener(new MouseAdapter()
      {
         public void mouseClicked(MouseEvent event)
         {
            if (event.getClickCount() == 1)
            {
System.out.println("clicked on "+label.getText());
            }
         }
      });
   }

   public Dimension getPreferredSize()
   {
      Dimension dim = label.getPreferredSize();

      dim.width += padx;
      dim.height = panel.getRowHeight(row);

      return dim;
   }

   public Dimension getMinimumSize()
   {
      Dimension dim = label.getMinimumSize();

      dim.width += padx;
      dim.height = panel.getRowHeight(row);

      return dim;
   }

   public Dimension getMaximumSize()
   {
      Dimension dim = label.getMinimumSize();

      dim.width += padx;
      dim.height = panel.getRowHeight(row);

      return dim;
   }

   public Dimension getSize()
   {
      Dimension dim = label.getSize();

      dim.width += padx;
      dim.height = panel.getRowHeight(row);

      return dim;
   }
}
