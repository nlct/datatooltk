package com.dickimawbooks.datatooltk.gui;

import java.util.Vector;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import javax.activation.DataHandler;
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

   private DatatoolDbPanel panel;

   private Vector<RowButton> buttons;

   private int selectedRow = -1;
}

class RowButton extends JLabel implements DropTargetListener
{
   private DatatoolDbPanel panel;
   private int row;

   private int padx=10;

   public RowButton(final int rowIdx, final DatatoolDbPanel panel)
   {
      super(""+(rowIdx+1));

      setBorder(BorderFactory.createRaisedBevelBorder());
      setOpaque(true);

      this.row = new Integer(rowIdx);
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

         public void mousePressed(MouseEvent evt)
         {
             JComponent comp = (JComponent)evt.getSource();
             TransferHandler th = comp.getTransferHandler();
             th.exportAsDrag(comp, evt, TransferHandler.MOVE);
         }
      });

      setTransferHandler(new RowTransferHandler());
      setDropTarget(new DropTarget(this, this));
   }

   public void dragEnter(DropTargetDragEvent evt)
   {
   }

   public void dragExit(DropTargetEvent evt)
   {
   }

   public void dragOver(DropTargetDragEvent evt)
   {
   }

   public void drop(DropTargetDropEvent evt)
   {
      try
      {
          int fromIndex = Integer.parseInt(evt.getTransferable()
           .getTransferData(DataFlavor.stringFlavor).toString());

          if (fromIndex != row)
          {
             panel.moveRow(fromIndex, row);
          }
      }
      catch (Exception e)
      {
      }
   }

   public void dropActionChanged(DropTargetDragEvent evt)
   {
   }

   public Dimension getPreferredSize()
   {
      Dimension dim = super.getPreferredSize();

      dim.width += padx;
      dim.height = panel.getRowHeight(row);

      return dim;
   }

   public Dimension getMinimumSize()
   {
      Dimension dim = super.getMinimumSize();

      dim.width += padx;
      dim.height = panel.getRowHeight(row);

      return dim;
   }

   public Dimension getMaximumSize()
   {
      Dimension dim = super.getMinimumSize();

      dim.width += padx;
      dim.height = panel.getRowHeight(row);

      return dim;
   }

   public Dimension getSize()
   {
      Dimension dim = super.getSize();

      dim.width += padx;
      dim.height = panel.getRowHeight(row);

      return dim;
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
