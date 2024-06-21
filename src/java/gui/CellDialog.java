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

import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.*;
import javax.swing.text.*;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;
import com.dickimawbooks.datatooltk.*;

/**
 * Dialog box for editing cell contents.
 */
public class CellDialog extends JDialog
  implements ActionListener,ItemListener
{
   public CellDialog(DatatoolGUI gui)
   {
      super(gui, gui.getMessageHandler().getLabel("celledit.title"), true);

      this.gui = gui;

      initGui();
   }

   private void initGui()
   {
      DatatoolGuiResources resources = gui.getResources();

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

      ScrollToolBar toolBar = new ScrollToolBar(
       gui.getMessageHandler(), SwingConstants.HORIZONTAL);

      getContentPane().add(toolBar, BorderLayout.NORTH);

      undoManager = new UndoManager();

      JMenu editM = resources.createJMenu("edit");
      mbar.add(editM);

      undoItem = resources.createJMenuItem(
        "edit", "undo", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK),
        toolBar);

      editM.add(undoItem);

      redoItem = resources.createJMenuItem(
        "edit", "redo", this,
        KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_MASK),
        toolBar);

      editM.add(redoItem);

      editM.addSeparator();

      editM.add(resources.createJMenuItem(
         "edit", "select_all", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK),
         toolBar));

      cutItem = resources.createJMenuItem(
         "edit", "cut", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK),
         toolBar);

      editM.add(cutItem);

      copyItem = resources.createJMenuItem(
         "edit", "copy", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK),
         toolBar);

      editM.add(copyItem);

      editM.add(resources.createJMenuItem(
         "edit", "paste", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK),
         toolBar));

      JMenu searchM = resources.createJMenu("search");
      mbar.add(searchM);

      searchM.add(resources.createJMenuItem(
         "search", "find", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK),
         toolBar));

      findAgainItem = resources.createJMenuItem(
         "search", "find_again", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK),
         toolBar);
      searchM.add(findAgainItem);
      findAgainItem.setEnabled(false);

      searchM.add(resources.createJMenuItem(
         "search", "replace", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK),
         toolBar));

      document = new CellDocument(this,
         gui.getSettings());
      textPane = new JTextPane(document);

      textPane.setFont(gui.getCellFont());

      FontMetrics fm = getFontMetrics(textPane.getFont());

      textPane.setPreferredSize(new Dimension
         (gui.getSettings().getCellEditorWidth()*fm.getMaxAdvance(),
          gui.getSettings().getCellEditorHeight()*fm.getHeight()));

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

      findDialog = new FindDialog(gui.getMessageHandler(), this, textPane);

      textPane.setMinimumSize(new Dimension(0,0));

      JComponent datumPanel = createDatumComponent();

      JSplitPane splitPane = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT,
        new JScrollPane(textPane),
        new JScrollPane(datumPanel));
      splitPane.setOneTouchExpandable(true);

      splitPane.setResizeWeight(0.9f);

      getContentPane().add(splitPane, BorderLayout.CENTER);

      getContentPane().add(
         resources.createOkayCancelHelpPanel(this, gui, "celleditor"),
         BorderLayout.SOUTH);

      pack();

      setLocationRelativeTo(null);
   }

   protected JComponent createDatumComponent()
   {
      DatatoolGuiResources resources = gui.getResources();

      JComponent datumPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

      typeBox = new DatumTypeComboBox(gui.getSettings());
      typeBox.addItemListener(this);
      datumPanel.add(resources.createJLabel("celledit.type", typeBox));
      datumPanel.add(typeBox);

      currencyComp = new JPanel();
      datumPanel.add(currencyComp);

      currencyField = new JTextField(12);
      currencyComp.add(resources.createJLabel("celledit.currency", currencyField));
      currencyComp.add(currencyField);

      valueCardLayout = new CardLayout();
      numericComp = new JPanel(valueCardLayout);
      datumPanel.add(numericComp);

      JComponent intComp = new JPanel();
   
      intSpinnerModel = new SpinnerNumberModel(
        0, - Datum.TEX_MAX_INT, Datum.TEX_MAX_INT, 1);
      intSpinner = new JSpinner(intSpinnerModel);
      JComponent editor = intSpinner.getEditor();
      JFormattedTextField tf = ((JSpinner.DefaultEditor)editor).getTextField();
      tf.setColumns(5);

      intComp.add(resources.createJLabel("celledit.numeric", intSpinner));
      intComp.add(intSpinner);
      numericComp.add(intComp, "int");

      JComponent decComp = new JPanel();

      decimalField = new JTextField(6);
      decComp.add(resources.createJLabel("celledit.numeric", decimalField));
      decComp.add(decimalField);
      numericComp.add(decComp, "dec");

      return datumPanel;
   }

   @Override
   public void itemStateChanged(ItemEvent evt)
   {
      if (evt.getStateChange() == ItemEvent.SELECTED)
      {
         intSpinnerModel.setValue(Integer.valueOf(orgValue.intValue()));
         decimalField.setText(orgValue.toString());
         updateDatumComp();
      }
   }

   protected void updateDatumComp()
   {
      DatumType type = typeBox.getSelectedType();

      if (type.isNumeric())
      {
         if (type == DatumType.INTEGER)
         {
            valueCardLayout.show(numericComp, "int");
         }
         else
         {
            valueCardLayout.show(numericComp, "dec");
         }

         currencyComp.setVisible(type==DatumType.CURRENCY);
         numericComp.setVisible(true);
      }
      else
      {
         currencyComp.setVisible(false);
         numericComp.setVisible(false);
      }
   }

   public boolean requestEdit(int row, int col,
     DatatoolDbPanel panel)
   {
      this.panel = panel;
      this.db = panel.db;
      this.row = row;
      this.col = col;

      textPane.setFont(gui.getCellFont());

      Datum datum = db.getRow(row).get(col);
      orgValue = datum.getNumber();

      if (orgValue == null)
      {
         orgValue = Integer.valueOf(0);
      }

      DatumType type = datum.getDatumType();

      if (datum.isNumeric())
      {
         String sym = datum.getCurrencySymbol();
         currencyField.setText(sym == null ? "" : sym);
         decimalField.setText(orgValue.toString());
         intSpinnerModel.setValue(Integer.valueOf(orgValue.intValue()));
      }
      else
      {
         currencyField.setText("");
         decimalField.setText("");
      }

      typeBox.setSelectedType(type);

      try
      {
         document.setText(
            datum.getText().replaceAll("\\\\DTLpar *", "\n\n"));
      }
      catch (BadLocationException e)
      {
         getMessageHandler().error(this, e);
      }

      updateDatumComp();

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
            getMessageHandler().debug(e);
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
            getMessageHandler().debug(e);
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
            getMessageHandler().getLabel("message.discard_edit_query"),
            getMessageHandler().getLabel("message.confirm_discard"),
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

   public MessageHandler getMessageHandler()
   {
      return gui.getMessageHandler();
   }

   private JTextPane textPane;

   private CellDocument document;

   private JMenuItem undoItem, redoItem, copyItem, cutItem,
     findAgainItem;

   private DatumTypeComboBox typeBox;
   private JTextField currencyField, decimalField;
   private JComponent currencyComp, numericComp;
   private JSpinner intSpinner;
   private SpinnerNumberModel intSpinnerModel;
   private CardLayout valueCardLayout;
   private Number orgValue;

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
   public CellDocument(CellDialog dialog, DatatoolSettings settings)
   {
      super();

      this.cellDialog = dialog;
      this.highlightOn = settings.isSyntaxHighlightingOn();

      StyleContext context = StyleContext.getDefaultStyleContext();
      attrPlain = context.addAttribute(context.getEmptySet(),
         StyleConstants.Foreground, Color.BLACK);
      attrControlSequence = context.addAttribute(context.getEmptySet(),
         StyleConstants.Foreground, settings.getControlSequenceHighlight());

      attrComment = new SimpleAttributeSet();
      StyleConstants.setItalic(attrComment, true);
      StyleConstants.setForeground(attrComment, 
         settings.getCommentHighlight());

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
      if (!highlightOn) return;

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
            setCharacterAttributes(newOffset, len, attrControlSequence, false);
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

   // This method is for initialising so bypass the undo/redo
   // stuff
   public void setText(String text)
     throws BadLocationException
   {
      super.remove(0, getLength());
      super.insertString(0, text, null);
      updateHighlight();
   }

   private CellDialog cellDialog;

   private boolean highlightOn;

   private CompoundEdit compoundEdit;

   private AttributeSet attrPlain;
   private AttributeSet attrControlSequence;
   private SimpleAttributeSet attrComment;

   private static final Pattern PATTERN_CS = Pattern.compile(
      "((?:\\\\[^a-zA-Z]{1})|(?:\\\\[a-zA-Z]+)|(?:[#~\\{\\}\\^\\$_])|(?:%.*))");
}
