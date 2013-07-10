package com.dickimawbooks.datatooltk.gui;

import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.*;
import javax.swing.text.*;

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

      editM.addSeparator();

      editM.add(DatatoolGuiResources.createJMenuItem(
         "edit", "select_all", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK),
         toolBar));

      cutItem = DatatoolGuiResources.createJMenuItem(
         "edit", "cut", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK),
         toolBar);

      editM.add(cutItem);

      copyItem = DatatoolGuiResources.createJMenuItem(
         "edit", "copy", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK),
         toolBar);

      editM.add(copyItem);

      editM.add(DatatoolGuiResources.createJMenuItem(
         "edit", "paste", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK),
         toolBar));

      JMenu searchM = DatatoolGuiResources.createJMenu("search");
      mbar.add(searchM);

      searchM.add(DatatoolGuiResources.createJMenuItem(
         "search", "find", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK),
         toolBar));

      findAgainItem = DatatoolGuiResources.createJMenuItem(
         "search", "find_again", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK),
         toolBar);
      searchM.add(findAgainItem);
      findAgainItem.setEnabled(false);

      searchM.add(DatatoolGuiResources.createJMenuItem(
         "search", "replace", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK),
         toolBar));

      document = new CellDocument(this);
      textPane = new JTextPane(document);

      textPane.setFont(gui.getCellFont());

      FontMetrics fm = getFontMetrics(textPane.getFont());

      textPane.setPreferredSize(new Dimension
         (8*fm.getMaxAdvance(), 10*fm.getHeight()));

      textPane.setEditable(true);

      document.addDocumentListener(new DocumentListener()
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

      textPane.addCaretListener(new CaretListener()
      {
         public void caretUpdate(CaretEvent evt)
         {
            updateEditButtons();
         }
      });

      updateEditButtons();

      findDialog = new FindDialog(this, textPane);

      mainPanel.add(new JScrollPane(textPane), BorderLayout.CENTER);

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

      try
      {
         document.setText(
            db.getRow(row).get(col).replaceAll("\\\\DTLpar *", "\n\n"));
      }
      catch (BadLocationException e)
      {
         DatatoolGuiResources.error(this, e);
      }

      undoManager.discardAllEdits();
      undoItem.setEnabled(false);
      redoItem.setEnabled(false);

      revalidate();

      modified = false;
      textPane.requestFocusInWindow();
      textPane.setCaretPosition(0);

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
         textPane.selectAll();
      }
      else if (action.equals("copy"))
      {
         textPane.copy();
      }
      else if (action.equals("cut"))
      {
         textPane.cut();
      }
      else if (action.equals("paste"))
      {
         textPane.paste();
      }
      else if (action.equals("find"))
      {
         String selectedText = textPane.getSelectedText();

         if (selectedText != null)
         {
            findDialog.setSearchText(selectedText);
         }

         findDialog.display(false);
      }
      else if (action.equals("find_again"))
      {
         findDialog.find();
      }
      else if (action.equals("replace"))
      {
         String selectedText = textPane.getSelectedText();

         if (selectedText != null)
         {
            findDialog.setSearchText(selectedText);
         }

         findDialog.display(true);
      }
   }

   public void okay()
   {
      panel.updateCell(row, col,  
        textPane.getText().replaceAll("\n *\n+", "\\\\DTLpar "));
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

   private void updateEditButtons()
   {
      String selected = (textPane == null ? null : textPane.getSelectedText());

      if (selected == null || selected.isEmpty())
      {
         copyItem.setEnabled(false);
         cutItem.setEnabled(false);
      }
      else
      {
         copyItem.setEnabled(true);
         cutItem.setEnabled(true);
      }

      findAgainItem.setEnabled(findDialog == null ? false :
        !findDialog.getSearchText().isEmpty());
   }

   public void addEdit(UndoableEdit edit)
   {
      undoManager.addEdit(edit);

      undoItem.setEnabled(true);
   }

   private JTextPane textPane;

   private CellDocument document;

   private JMenuItem undoItem, redoItem, copyItem, cutItem,
     findAgainItem;

   private DatatoolDbPanel panel;

   private DatatoolDb db;

   private int row, col;

   private DatatoolGUI gui;

   private boolean modified;

   private FindDialog findDialog;

   private UndoManager undoManager;
}

class CellDocument extends DefaultStyledDocument
{
   public CellDocument(CellDialog dialog)
   {
      super();

      this.cellDialog = dialog;

      StyleContext context = StyleContext.getDefaultStyleContext();
      attrPlain = context.addAttribute(context.getEmptySet(),
         StyleConstants.Foreground, Color.BLACK);
      attrControlSequence = context.addAttribute(context.getEmptySet(),
         StyleConstants.Foreground, Color.BLUE);

      attrComment = new SimpleAttributeSet();
      StyleConstants.setItalic(attrComment, true);
      StyleConstants.setForeground(attrComment, Color.GRAY);

      addUndoableEditListener(
       new UndoableEditListener()
       {
          public void undoableEditHappened(UndoableEditEvent event)
          {
             if (compoundEdit == null)
             {
                cellDialog.addEdit(event.getEdit());
             }
             else
             {
                compoundEdit.addEdit(event.getEdit());
             }
          }
       });

   }

   public void remove(int offset, int length)
     throws BadLocationException
   {
      super.remove(offset, length);

      updateHighlight();
   }

   public void insertString(int offset, String str, AttributeSet at)
     throws BadLocationException
   {
      super.insertString(offset, str, at);

      updateHighlight();
   }

   private void updateHighlight()
   throws BadLocationException
   {
      String text = getText(0, getLength());

      setCharacterAttributes(0, getLength(), 
        attrPlain, true);

      Matcher matcher = PATTERN_CS.matcher(text);

      while (matcher.find())
      {
         int newOffset = matcher.start();
         int len = matcher.end() - newOffset;

         String group = matcher.group();

         if (group.startsWith("%"))
         {
            setCharacterAttributes(newOffset, len, attrComment, true);
         }
         else
         {
            setCharacterAttributes(newOffset, len, attrControlSequence, true);
         }
      }
   }

   public void replace(int offset, int length, String text,
      AttributeSet attrs)
   throws BadLocationException
   {
      compoundEdit = new CompoundEdit();

      try
      {
         super.replace(offset, length, text, attrs);

         compoundEdit.end();
         cellDialog.addEdit(compoundEdit);
      }
      finally
      {
         compoundEdit = null;
      }

      updateHighlight();
   }

   public void setText(String text)
     throws BadLocationException
   {
      remove(0, getLength());
      insertString(0, text, null);
   }

   private CellDialog cellDialog;

   private CompoundEdit compoundEdit;

   private AttributeSet attrPlain;
   private AttributeSet attrControlSequence;
   private SimpleAttributeSet attrComment;

   private static final Pattern PATTERN_CS = Pattern.compile(
      "((?:\\\\[^a-zA-Z]{1})|(?:\\\\[a-zA-Z]+)|(?:[#~\\{\\}\\^\\$_])|(?:%.*))");
}
