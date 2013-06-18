package com.dickimawbooks.datatooltk.gui;

import java.util.Vector;
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

      int n = panel.getRowCount();
      buttons = new Vector<RowButton>(n);

      for (int i = 0; i < n; i++)
      {
         addButton(i);
      }
   }

   protected void addButton(int row)
   {
      RowButton button = new RowButton(row, panel);
      button.setBackground(panel.getSelectionBackground());
      button.setOpaque(false);
      add(button);
      buttons.add(button);
   }

   public void updateRowSelection(int row)
   {
      RowButton button;

      if (selectedRow != -1)
      {
         button = buttons.get(selectedRow);

         button.setOpaque(false);
         button.repaint();
      }

      selectedRow = row;

      if (selectedRow != -1)
      {
         button = buttons.get(selectedRow);

         button.setOpaque(true);
         button.repaint();
      }
   }

   private DatatoolDbPanel panel;

   private Vector<RowButton> buttons;

   private int selectedRow = -1;
}

class RowButton extends JPanel
{
   private DatatoolDbPanel panel;
   private int row;
   private JLabel label;

   private int padx=10;

   public RowButton(final int row, final DatatoolDbPanel panel)
   {
      super(new BorderLayout());

      setBorder(BorderFactory.createRaisedBevelBorder());
      setOpaque(true);

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
               panel.selectRow(row);
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
