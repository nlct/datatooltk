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

      JMenu editM = createJMenu("edit");
      mbar.add(editM);

      undoItem = createJMenuItem("edit", "undo", toolBar);
      editM.add(undoItem);

      redoItem = createJMenuItem("edit", "redo", toolBar);
      editM.add(redoItem);

      editM.addSeparator();

      editM.add(createJMenuItem("edit", "select_all", toolBar));

      cutItem = createJMenuItem("edit", "cut", toolBar);
      editM.add(cutItem);

      copyItem = createJMenuItem("edit", "copy", toolBar);
      editM.add(copyItem);

      editM.add(createJMenuItem("edit", "paste", toolBar));

      editM.addSeparator();
      toolBar.addSeparator();

      editM.add(createJMenuItem("edit", "parse", toolBar));
      editM.add(createJMenuItem("edit", "reload", "reset", toolBar));
      editM.add(createJMenuItem("edit", "cell_to_null", toolBar));

      toolBar.addSeparator();

      JMenu searchM = createJMenu("search");
      mbar.add(searchM);

      searchM.add(createJMenuItem("search", "find", "search", toolBar));

      findAgainItem = createJMenuItem("search", "find_again",
        "search_again", toolBar);
      searchM.add(findAgainItem);
      findAgainItem.setEnabled(false);

      searchM.add(createJMenuItem("search", "replace", "replace_text", toolBar));

      document = new CellDocument(this, gui.getSettings());
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

      JComponent textPanel = new JPanel(new BorderLayout());
      textPanel.add(new JScrollPane(textPane), BorderLayout.CENTER);
      textPanel.add(resources.createJLabel("celledit.text", textPane),
        BorderLayout.NORTH);

      JSplitPane splitPane = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT,
        textPanel,
        new JScrollPane(datumPanel));
      splitPane.setOneTouchExpandable(true);

      splitPane.setResizeWeight(0.9f);

      getContentPane().add(splitPane, BorderLayout.CENTER);

      getContentPane().add(
         resources.createDialogOkayCancelHelpPanel(this, this, gui, "celleditor"),
         BorderLayout.SOUTH);

      pack();

      setLocationRelativeTo(null);
   }

   protected JMenu createJMenu(String label)
   {
      return gui.getResources().createJMenu("editormenu", label);
   }

   protected JMenuItem createJMenuItem(String parentLabel, String action,
     ScrollToolBar toolBar)
   {
      return createJMenuItem(parentLabel, action, action, toolBar);
   }

   protected JMenuItem createJMenuItem(String parentLabel, String action,
     String iconPrefix, ScrollToolBar toolBar)
   {
      parentLabel = "editormenu."+parentLabel; 

      return gui.getResources().createJMenuItem(parentLabel, action, iconPrefix,
        this, gui.getMessageHandler().getKeyStroke(parentLabel, action), toolBar);
   }

   protected JComponent createDatumComponent()
   {
      DatatoolGuiResources resources = gui.getResources();

      JComponent datumPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));

      typeBoxLabel = resources.createJLabel("celledit.type", typeBox);
      typeBox = new DatumTypeComboBox(gui.getSettings());
      typeBox.addItemListener(this);
      datumPanel.add(typeBoxLabel);
      datumPanel.add(typeBox);

      currencyComp = new JPanel();
      datumPanel.add(currencyComp);

      currencyField = new JTextField(10);
      currencyComp.add(resources.createJLabel("celledit.currency", currencyField));
      currencyComp.add(currencyField);

      valueCardLayout = new CardLayout();
      numericComp = new JPanel(valueCardLayout);
      datumPanel.add(numericComp);

      intComp = new JPanel();
   
      intSpinnerModel = new SpinnerNumberModel(
        0, - Datum.TEX_MAX_INT, Datum.TEX_MAX_INT, 1);
      intSpinner = new JSpinner(intSpinnerModel);
      JComponent editor = intSpinner.getEditor();
      JFormattedTextField tf = ((JSpinner.DefaultEditor)editor).getTextField();
      tf.setColumns(5);

      intComp.add(resources.createJLabel("celledit.numeric", intSpinner));
      intComp.add(intSpinner);
      numericComp.add(intComp, "int");

      decComp = new JPanel();

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
         DatumType type = typeBox.getSelectedType();

         switch (type)
         {
            case INTEGER:
              if (!intComp.isVisible())
              {
                 intSpinnerModel.setValue(Integer.valueOf((int)getDecimalValue()));
              }
            break;
            case CURRENCY:
            case DECIMAL:
              if (!decComp.isVisible())
              {
                 setDecimalValue(intSpinnerModel.getNumber().doubleValue());
              }
            break;
         }

         updateDatumComp();
      }
   }

   private String getCurrencySymbol()
   {
      return currencyField.getText();
   }

   private void setCurrencySymbol(String sym)
   {
      currencyField.setText(sym == null ? "" : sym);
   }

   private void setDecimalValue(double val)
   {
      decimalField.setText(String.format("%g", val));
   }

   private double getDecimalValue()
   {
      try
      {
         return Double.valueOf(decimalField.getText());
      }
      catch (NumberFormatException e)
      {
         return orgValue.doubleValue();
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

   private void setDatum(Datum datum)
   {
      orgValue = datum.getNumber();

      if (orgValue == null)
      {
         orgValue = Integer.valueOf(0);
      }

      DatumType type = datum.getDatumType();

      if (datum.isNumeric())
      {
         String sym = datum.getCurrencySymbol();
         setCurrencySymbol(sym);
         setDecimalValue(orgValue.doubleValue());
         intSpinnerModel.setValue(Integer.valueOf(orgValue.intValue()));
      }
      else
      {
         setCurrencySymbol(null);
         decimalField.setText("");
      }

      typeBox.setSelectedType(type);
   }

   private void reload()
   {
      Datum datum = db.getRow(row).get(col);

      setDatum(datum);

      try
      {
         document.setText(
            datum.getText().replaceAll("\\\\DTLpar *", "\n\n"));
      }
      catch (BadLocationException e)
      {
         getMessageHandler().error(this, e);
      }

      modified = false;
      textPane.requestFocusInWindow();
      textPane.setCaretPosition(0);
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
         setCurrencySymbol(datum.getCurrencySymbol());
         setDecimalValue(orgValue.doubleValue());
         intSpinnerModel.setValue(Integer.valueOf(orgValue.intValue()));
      }
      else
      {
         setCurrencySymbol(null);
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
      else if (action.equals("reload"))
      {
         reload();
      }
      else if (action.equals("parse"))
      {
         Datum datum = Datum.valueOf(textPane.getText(), gui.getSettings());
         setDatum(datum);

         if (!datum.isNumeric())
         {
            JOptionPane.showMessageDialog(this,
             gui.getMessageHandler().getLabel("message.celledit.not_numeric"));
         }
      }
      else if (action.equals("cell_to_null"))
      {
         Datum datum = Datum.createNull(gui.getSettings());
         textPane.setText(datum.getText());
         setDatum(datum);
      }
      else if (action.equals("format"))
      {
         DatumType type = typeBox.getSelectedType();

         if (type.isNumeric())
         {
            Number num;

            if (type == DatumType.INTEGER)
            {
               num = intSpinnerModel.getNumber();
            }
            else
            {
               num = Double.valueOf(getDecimalValue());
            }

            Datum datum = Datum.format(type, getCurrencySymbol(),
              num, gui.getSettings());
            textPane.setText(datum.getText());
         }
         else
         {
            gui.getMessageHandler().error(this, 
              gui.getMessageHandler().getLabelWithValues("error.not_decimal_type",
               typeBox.getSelectedItem(), typeBoxLabel.getText()));
         }
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
      String text = textPane.getText().replaceAll("\n *\n+", "\\\\DTLpar ");

      DatumType type = typeBox.getSelectedType();
      String currencySym = null;
      Number num = null;

      switch (type)
      {
         case INTEGER:
            num = intSpinnerModel.getNumber();
         break;
         case CURRENCY:
            currencySym = getCurrencySymbol();
         // fall through
         case DECIMAL:
            String decStr = decimalField.getText();

            try
            {
               num = Double.valueOf(decStr);
            }
            catch (NumberFormatException e)
            {
               gui.getMessageHandler().error(this, 
                gui.getMessageHandler().getLabelWithValues(
                  "error.not_decimal", decStr));
               return;
            }
      }

      Datum datum = new Datum(type, text, currencySym, num, gui.getSettings());

      panel.updateCell(datum, row, col);
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
   private JLabel typeBoxLabel;
   private JTextField currencyField, decimalField;
   private JComponent currencyComp, numericComp, intComp, decComp;
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

      Matcher matcher = DatatoolGuiResources.PATTERN_CS.matcher(text);

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
}
