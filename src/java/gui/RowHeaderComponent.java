package com.dickimawbooks.datatooltk.gui;

import java.util.Vector;
import java.awt.geom.AffineTransform;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import javax.activation.DataHandler;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

public class RowHeaderComponent extends JPanel implements DropTargetListener
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
         RowButton button = addButton(i);
      }

   }

   protected RowButton addButton(int row)
   {
      RowButton button = new RowButton(row, panel, this);
      button.setBackground(panel.getSelectionBackground());
      button.setOpaque(false);
      add(button);
      buttons.add(button);

      return button;
   }

   public void addButton()
   {
      addButton(buttons.size());
   }

   public void removeButton()
   {
      int n = buttons.size()-1;

      if (selectedRow == n)
      {
         selectedRow = -1;
      }

      buttons.remove(n);
      remove(n);
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

   private void updateDropLocation(Point location)
   {
      if (location == null || dragRow == null)
      {
         dropLocation = null;
      }
      else
      {
         dropLocation = getMousePosition();
      }

      repaint();
   }

   public void dragEnter(DropTargetDragEvent evt)
   {
      updateDropLocation(evt.getLocation());
   }

   public void dragExit(DropTargetEvent evt)
   {
      updateDropLocation(null);
   }

   public void dragOver(DropTargetDragEvent evt)
   {
      updateDropLocation(evt.getLocation());
   }

   public void drop(DropTargetDropEvent evt)
   {
      panel.setInfo("");
      dropLocation = null;
      dragRow = null;

      try
      {
          int fromIndex = Integer.parseInt(evt.getTransferable()
           .getTransferData(DataFlavor.stringFlavor).toString());

           RowButton target = (RowButton)
              ((DropTarget)evt.getSource()).getComponent();

          int toIndex = target.getIndex();

          if (fromIndex != toIndex)
          {
             panel.moveRow(fromIndex, toIndex);
          }
      }
      catch (Exception e)
      {
      }
   }

   public void dropActionChanged(DropTargetDragEvent evt)
   {
   }

   public void startDnD(MouseEvent evt, int row)
   {
       dragRow = buttons.get(row);

       if (dragRow != null)
       {
          TransferHandler th = dragRow.getTransferHandler();
          th.exportAsDrag(dragRow, evt, TransferHandler.MOVE);
       }
   }

   public void paintComponent(Graphics g)
   {
      super.paintComponent(g);

      Graphics2D g2 = (Graphics2D)g;

      if (dragRow != null && dropLocation != null)
      {
         Paint oldPaint = g2.getPaint();

         Rectangle rect = dragRow.getBounds();

         int xOffset = 0;
         int yOffset = dropLocation.y-rect.height/2;

         g2.translate(xOffset, yOffset);
         g2.setPaint(panel.getSelectionBackground());
         g2.fillRect(0, 0, rect.width, rect.height);
         dragRow.paint(g2);

         g2.translate(-xOffset, -yOffset);

         g2.setPaint(oldPaint);
      }
   }

   private DatatoolDbPanel panel;

   private Vector<RowButton> buttons;

   private Point dropLocation = null;

   private RowButton dragRow = null;

   private int selectedRow = -1;
}

class RowButton extends JLabel
{
   private DatatoolDbPanel panel;
   private int row;

   private static final int dx = 10;

   public RowButton(final int rowIdx, final DatatoolDbPanel panel,
     final RowHeaderComponent rowHeaderPanel)
   {
      super(""+(rowIdx+1));

      setBorder(BorderFactory.createRaisedBevelBorder());
      setOpaque(true);

      this.row = rowIdx;
      this.panel = panel;

      addMouseListener(new MouseAdapter()
      {
         public void mouseClicked(MouseEvent event)
         {
            panel.setInfo(DatatoolTk.getLabel("info.move_row"));

            if (event.getClickCount() == 1)
            {
               panel.selectModelRow(row);
            }
         }

         public void mousePressed(MouseEvent event)
         {
            panel.selectModelRow(row);
         }

      });

      addMouseMotionListener(new MouseMotionAdapter()
      {
         public void mouseDragged(MouseEvent evt)
         {
             rowHeaderPanel.startDnD(evt, row);
         }
      });

      setTransferHandler(new RowTransferHandler());
      setDropTarget(new DropTarget(this, rowHeaderPanel));
   }

   public Dimension getPreferredSize()
   {
      FontMetrics fm = getFontMetrics(getFont());

      Dimension dim = new Dimension
      (
          fm.stringWidth(""+panel.getRowCount()+dx),
          panel.getRowHeight(row-1)
      );

      return dim;
   }

   public Dimension getMinimumSize()
   {
      return getPreferredSize();
   }

   public Dimension getMaximumSize()
   {
      return getPreferredSize();
   }

   public Dimension getSize()
   {
      return getPreferredSize();
   }

   public int getIndex()
   {
      return row;
   }
}

class RowTransferHandler extends TransferHandler
{
   public boolean canImport(TransferHandler.TransferSupport info)
   {
      if (!info.isDataFlavorSupported(DataFlavor.stringFlavor))
      {
         return false;
      }

      return true;
   }

   protected Transferable createTransferable(JComponent comp)
   {
      RowButton button = (RowButton)comp;

      return new StringSelection(""+button.getIndex());
   }

   public int getSourceActions(JComponent c)
   {
      return TransferHandler.MOVE;
   }

   public boolean importData(TransferHandler.TransferSupport info)
   {
      if (!info.isDrop())
      {
         return false;
      }

      Transferable t = info.getTransferable();
      DropLocation dl = info.getDropLocation();

      int data;

      try
      {
         data = Integer.parseInt(
            (String)t.getTransferData(DataFlavor.stringFlavor));
      }
      catch (Exception e)
      {
         return false;
      }

      return true;
   }

   protected void exportDone(JComponent c, Transferable data, int action)
   {
   }
}
