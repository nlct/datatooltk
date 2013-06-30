package com.dickimawbooks.datatooltk.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.*;

import com.dickimawbooks.datatooltk.*;

public class CellDialog extends JDialog
  implements ActionListener
{
   public CellDialog(DatatoolGUI gui)
   {
      super(gui, DatatoolTk.getLabel("celledit.title"), true);

      this.gui = gui;

      setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

      addWindowListener(new WindowAdapter()
      {
         public void windowClosing(WindowEvent evt)
         {
            cancel();
         }
      });

      JMenuBar mbar = new JMenuBar();
      setJMenuBar(mbar);

      ScrollToolBar toolBar = new ScrollToolBar(SwingConstants.HORIZONTAL);

      getContentPane().add(toolBar, BorderLayout.NORTH);

      JPanel mainPanel = new JPanel(new BorderLayout());

      getContentPane().add(mainPanel, BorderLayout.CENTER);

      undoManager = new UndoManager();

      JMenu editM = DatatoolGuiResources.createJMenu("edit");
      mbar.add(editM);

      undoItem = DatatoolGuiResources.createJMenuItem(
        "edit", "undo", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK),
        toolBar);

      editM.add(undoItem);

      redoItem = DatatoolGuiResources.createJMenuItem(
        "edit", "redo", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK),
        toolBar);

      editM.add(redoItem);

      editM.add(DatatoolGuiResources.createJMenuItem(
         "edit", "select_all", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK),
         toolBar));

      textArea = new JTextArea(20,40);
      textArea.setFont(gui.getCellFont());
      textArea.getDocument().addUndoableEditListener(
       new UndoableEditListener()
       {
          public void undoableEditHappened(UndoableEditEvent event)
          {
             undoManager.addEdit(event.getEdit());
             undoItem.setEnabled(true);
          }
       });

      textArea.setEditable(true);
      textArea.setLineWrap(true);
      textArea.setWrapStyleWord(true);

      textArea.getDocument().addDocumentListener(new DocumentListener()
      {
         public void changedUpdate(DocumentEvent e)
         {
            modified = true;
         }

         public void insertUpdate(DocumentEvent e)
         {
            modified = true;
         }

         public void removeUpdate(DocumentEvent e)
         {
            modified = true;
         }
      });

      mainPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);

      mainPanel.add(
         DatatoolGuiResources.createOkayCancelHelpPanel(this, gui, "celleditor"),
         BorderLayout.SOUTH);

      pack();

      setLocationRelativeTo(null);
   }

   public boolean requestEdit(int row, int col,
     DatatoolDbPanel panel)
   {
      this.panel = panel;
      this.db = panel.db;
      this.row = row;
      this.col = col;

      textArea.setText(
         db.getRow(row).get(col).replaceAll("\\\\DTLpar *", "\n\n"));

      undoManager.discardAllEdits();
      undoItem.setEnabled(false);
      redoItem.setEnabled(false);

      revalidate();

      modified = false;

      setVisible(true);

      return modified;
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("okay"))
      {
         okay();
      }
      else if (action.equals("cancel"))
      {
         cancel();
      }
      else if (action.equals("undo"))
      {
         try
         {
            undoManager.undo();
         }
         catch (CannotUndoException e)
         {
            DatatoolTk.debug(e);
         }

         undoItem.setEnabled(undoManager.canUndo());
         redoItem.setEnabled(undoManager.canRedo());
      }
      else if (action.equals("redo"))
      {
         try
         {
            undoManager.redo();
         }
         catch (CannotRedoException e)
         {
            DatatoolTk.debug(e);
         }

         undoItem.setEnabled(undoManager.canUndo());
         redoItem.setEnabled(undoManager.canRedo());
      }
      else if (action.equals("select_all"))
      {
         textArea.selectAll();
      }
   }

   public void okay()
   {
      panel.updateCell(row, col,  
        textArea.getText().replaceAll("\n *\n+", "\\\\DTLpar "));
      setVisible(false);
   }

   public void cancel()
   {
      if (modified)
      {
         if (JOptionPane.showConfirmDialog(this, 
            DatatoolTk.getLabel("message.discard_edit_query"),
            DatatoolTk.getLabel("message.confirm_discard"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE)
            != JOptionPane.YES_OPTION)
         {
            return;
         }

         modified = false;
      }

      setVisible(false);
   }

   private JTextArea textArea;

   private JMenuItem undoItem, redoItem;

   private DatatoolDbPanel panel;

   private DatatoolDb db;

   private int row, col;

   private DatatoolGUI gui;

   private boolean modified;

   private UndoManager undoManager;
}
