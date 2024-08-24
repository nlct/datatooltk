/*
    Copyright (C) 2024 Nicola L.C. Talbot
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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;

import com.dickimawbooks.texjavahelplib.JLabelGroup;
import com.dickimawbooks.texjavahelplib.TeXJavaHelpLib;

import com.dickimawbooks.datatooltk.base.*;

/**
 * Component for editor settings.
 */
public class EditorPropertiesComponent extends JPanel
  implements ActionListener
{
   public EditorPropertiesComponent(DatatoolGUI gui)
   {
      this(gui, DEFAULT_PARENT_TAG);
   }

   public EditorPropertiesComponent(DatatoolGUI gui, String parentTag)
   {
      super();
      this.gui = gui;
      this.parentTag = parentTag;

      init();
   }

   private void init()
   {
      DatatoolGuiResources resources = getResources();
      MessageHandler messageHandler = getMessageHandler();

      setAlignmentY(0);
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

      JComponent box = createNewRow();

      samplerDocument = new FontSampleDocument(this);
      samplerComp = new JTextPane(samplerDocument);
      samplerComp.setText(messageHandler.getLabel(getTag("sampler")));
      samplerComp.setEditable(false);

      box.add(new JScrollPane(samplerComp));

      JLabelGroup labelGrp = new JLabelGroup();

      box = createNewRow();

      GraphicsEnvironment env =
         GraphicsEnvironment.getLocalGraphicsEnvironment();

      fontBox = new JComboBox<String>(env.getAvailableFontFamilyNames());
      fontBox.addItemListener(new ItemListener()
       {
          @Override
          public void itemStateChanged(ItemEvent evt)
          {
             updateFontSampler();
          }
       });

      box.add(createLabel(labelGrp, getTag("font"), fontBox));
      box.add(fontBox);

      box = createNewRow();
      fontSizeModel = new SpinnerNumberModel(10, 1, 1000, 1);
      JSpinner sizeField = new JSpinner(fontSizeModel);
      sizeField.addChangeListener(new ChangeListener()
       {
          @Override
          public void stateChanged(ChangeEvent evt)
          {
             updateFontSampler();
          }
       });

      box.add(createLabel(labelGrp, getTag("fontsize"), sizeField));
      box.add(sizeField);

      JComponent fgbgComp = createNewRow();

      JComponent fgbgSwatchesComp = Box.createVerticalBox();
      fgbgSwatchesComp.setAlignmentX(0);
      fgbgComp.add(fgbgSwatchesComp);

      box = createNewRow(fgbgSwatchesComp,
         new FlowLayout(FlowLayout.LEADING, 0, 0));

      JButton button = resources.createActionButton(
        getParentTag("choose", "cellforeground"),
       "cellforeground", "choose_colour", true, this, null, true);

      box.add(createLabel(labelGrp, getTag("cellforeground"), button));

      editorForegroundSwatch = createSwatch(Color.BLACK);
      box.add(editorForegroundSwatch);
      box.add(button);

      box = createNewRow(fgbgSwatchesComp,
         new FlowLayout(FlowLayout.LEADING, 0, 0));

      button = resources.createActionButton(
        getParentTag("choose", "cellbackground"),
       "cellbackground", "choose_colour", true, this, null, true);

      box.add(createLabel(labelGrp, getTag("cellbackground"), button));

      editorBackgroundSwatch = createSwatch(Color.WHITE);
      box.add(editorBackgroundSwatch);
      box.add(button);

      fgbgComp.add(resources.createActionButton(getParentTag("cellfgbgswap"),
       "cellfgbgswap", "swap", true, this, null, true));

      syntaxHighlightingBox = resources.createJCheckBox
        (getParentTag("syntax"), "syntax", this);
      add(syntaxHighlightingBox);

      highlightCsComp = createNewRow();

      button = resources.createActionButton(
        getParentTag("choose", "highlightcs"),
       "highlightcs", "choose_colour", true, this, null, true);

      highlightCsComp.add(createLabel(labelGrp,
        getTag("highlightcs"), button));

      highlightCsSwatch = createSwatch(Color.BLACK);
      highlightCsComp.add(highlightCsSwatch);
      highlightCsComp.add(button);

      highlightCommentComp = createNewRow();

      button = resources.createActionButton(
       getParentTag("choose", "highlightcomment"),
       "highlightcomment", "choose_colour", true, this, null, true);

      highlightCommentComp.add(createLabel(labelGrp,
         getTag("highlightcomment"), button));

      highlightCommentSwatch = createSwatch(Color.BLACK);
      highlightCommentComp.add(highlightCommentSwatch);
      highlightCommentComp.add(button);

      box = createNewRow();
      box.add(resources.createMessageArea(0, 32, getTag("info")));

      box = createNewRow();

      labelGrp = new JLabelGroup();

      editorHeightModel = new SpinnerNumberModel(10, 1, 1000, 1);
      JSpinner editorHeightField = new JSpinner(editorHeightModel);

      box.add(createLabel(labelGrp, getTag("height"),
         editorHeightField));
      box.add(editorHeightField);

      box = createNewRow();

      editorWidthModel = new SpinnerNumberModel(8, 1, 1000, 1);
      JSpinner editorWidthField = new JSpinner(editorWidthModel);

      box.add(createLabel(labelGrp, getTag("width"),
         editorWidthField));
      box.add(editorWidthField);

      updateFontSampler();
   }

   private JComponent createSwatch(Color c)
   {
      JComponent swatch = new JPanel();
      swatch.setPreferredSize(SWATCH_SIZE);
      swatch.setBackground(c);

      return swatch;
   }

   protected String getParentTag(String subTag)
   {
      return getParentTag(null, subTag);
   }

   protected String getParentTag(String tag, String subTag)
   {
      TeXJavaHelpLib helpLib = getHelpLib();

      if (tag == null || tag.isEmpty())
      {
         String label = parentTag+"."+subTag;

         if (helpLib.isMessageLabelValid(label))
         {
            return parentTag;
         }

         return DEFAULT_PARENT_TAG;
      }
      else
      {
         String label = parentTag+"."+tag+"."+subTag;

         if (helpLib.isMessageLabelValid(label))
         {
            return parentTag+"."+tag;
         }

         return DEFAULT_PARENT_TAG+"."+tag;
      }
   }

   protected String getTag(String tag)
   {
      TeXJavaHelpLib helpLib = getHelpLib();

      String label = parentTag+"."+tag;

      if (helpLib.isMessageLabelValid(label))
      {
         return label;
      }

      return DEFAULT_PARENT_TAG+"."+tag;
   }

   private JComponent createNewRow()
   {
      return createNewRow(this);
   }

   private JComponent createNewRow(JComponent parent)
   {
      return createNewRow(parent, new FlowLayout(FlowLayout.LEADING, 4, 1));
   }

   private JComponent createNewRow(JComponent parent, LayoutManager layout)
   {
      JComponent comp = new JPanel(layout);
      comp.setAlignmentX(0);
      parent.add(comp);

      return comp;
   }

   private JComponent createNewRow(JComponent parent, String constraint)
   {
      return createNewRow(parent, new FlowLayout(FlowLayout.LEADING, 4, 1),
        constraint);
   }

   private JComponent createNewRow(JComponent parent, LayoutManager layout,
     Object constraints)
   {
      JComponent comp = new JPanel(layout);
      comp.setAlignmentX(0);
      parent.add(comp, constraints);

      return comp;
   }

   private JLabel createLabel(String label)
   {
      return getResources().createJLabel(label);
   }

   private JLabel createLabel(JLabelGroup grp, String label)
   {
      return getResources().createJLabel(grp, label, null);
   }

   private JLabel createLabel(String label, JComponent comp)
   {
      return getResources().createJLabel(label, comp);
   }

   private JLabel createLabel(JLabelGroup grp, String label, JComponent comp)
   {
      return getResources().createJLabel(grp, label, comp);
   }

   private JTextArea createTextArea(String label)
   {
      return createTextArea(2, 40, label);
   }

   private JTextArea createTextArea(int rows, int columns, String label)
   {
      return getResources().createMessageArea(rows, columns, label);
   }

   private JCheckBox createCheckBox(String parentLabel, String label)
   {
      return createCheckBox(parentLabel, label, this);
   }

   private JCheckBox createCheckBox(String parentLabel, String label,
     ActionListener listener)
   {
      JCheckBox checkBox =
         getResources().createJCheckBox(parentLabel, label, listener);

      checkBox.setAlignmentX(0);

      return checkBox;
   }

   public void resetFrom(DatatoolProperties settings)
   {
      fontSizeModel.setValue(Integer.valueOf(settings.getFontSize()));
      fontBox.setSelectedItem(settings.getFontName());

      editorHeightModel.setValue(
        Integer.valueOf(settings.getCellEditorPreferredHeight()));
      editorWidthModel.setValue(
        Integer.valueOf(settings.getCellEditorPreferredWidth()));

      syntaxHighlightingBox.setSelected(settings.isSyntaxHighlightingOn());
      highlightCsSwatch.setBackground(settings.getControlSequenceHighlight());
      highlightCommentSwatch.setBackground(settings.getCommentHighlight());

      updateSwatchVisiblity();

      editorForegroundSwatch.setBackground(settings.getCellForeground());
      editorBackgroundSwatch.setBackground(settings.getCellBackground());

      updateFontSampler();

      updateCellEditor = false;
   }

   @Override
   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("highlightcs"))
      {
         Color col = JColorChooser.showDialog(this, 
            getMessageHandler().getLabel(getTag("highlightcs")),
            highlightCsSwatch.getBackground());

         if (col != null)
         {
            highlightCsSwatch.setBackground(col);
         }

         updateFontSampler();
      }
      else if (action.equals("highlightcomment"))
      {
         Color col = JColorChooser.showDialog(this, 
            getMessageHandler().getLabel(getTag("highlightcomment")),
            highlightCommentSwatch.getBackground());

         if (col != null)
         {
            highlightCommentSwatch.setBackground(col);
         }

         updateFontSampler();
      }
      else if (action.equals("cellforeground"))
      {
         Color col = JColorChooser.showDialog(this, 
            getMessageHandler().getLabel(getTag("cellforeground")),
            editorForegroundSwatch.getBackground());

         if (col != null)
         {
            editorForegroundSwatch.setBackground(col);
         }

         updateFontSampler();
      }
      else if (action.equals("cellbackground"))
      {
         Color col = JColorChooser.showDialog(this, 
            getMessageHandler().getLabel(getTag("cellbackground")),
            editorBackgroundSwatch.getBackground());

         if (col != null)
         {
            editorBackgroundSwatch.setBackground(col);
         }

         updateFontSampler();
      }
      else if (action.equals("syntax"))
      {
         updateFontSampler();
         updateSwatchVisiblity();
      }
      else if (action.equals("cellfgbgswap"))
      {
         Color bg = getSelectedCellBackground();
         editorBackgroundSwatch.setBackground(
            editorForegroundSwatch.getBackground());
         editorForegroundSwatch.setBackground(bg);
         updateFontSampler();
      }
      else
      {
         getMessageHandler().debug("Unknown action '"+action+"'");
      }
   }

   protected Color getCommentHighlight()
   {
      return highlightCommentSwatch.getBackground();
   }

   protected Color getControlSequenceHighlight()
   {
      return highlightCsSwatch.getBackground();
   }

   protected Color getSelectedCellForeground()
   {
      return editorForegroundSwatch.getBackground();
   }

   protected Color getSelectedCellBackground()
   {
      return editorBackgroundSwatch.getBackground();
   }

   protected boolean isSyntaxHighlightingOn()
   {
      return syntaxHighlightingBox.isSelected();
   }

   protected int getSelectedFontSize()
   {
      return fontSizeModel.getNumber().intValue();
   }

   protected String getSelectedFontName()
   {
      return fontBox.getSelectedItem().toString();
   }

   protected void updateSwatchVisiblity()
   {
      boolean selected = syntaxHighlightingBox.isSelected();
      highlightCsComp.setVisible(selected);
      highlightCommentComp.setVisible(selected);
   }

   protected void updateFontSampler()
   {
      Font sampleFont = new Font(getSelectedFontName(), Font.PLAIN,
        getSelectedFontSize());
      samplerComp.setFont(sampleFont);
      samplerComp.setBackground(getSelectedCellBackground());
      samplerComp.setForeground(getSelectedCellForeground());

      samplerDocument.update();

      updateCellEditor = true;
   }

   public void applySelected(DatatoolProperties settings)
   {
      settings.setFontName(getSelectedFontName());
      settings.setFontSize(getSelectedFontSize());

      settings.setCellEditorPreferredHeight(
        editorHeightModel.getNumber().intValue());
      settings.setCellEditorPreferredWidth(
        editorWidthModel.getNumber().intValue());

      settings.setSyntaxHighlighting(syntaxHighlightingBox.isSelected());

      if (syntaxHighlightingBox.isSelected())
      {
         settings.setControlSequenceHighlight(highlightCsSwatch.getBackground());
         settings.setCommentHighlight(highlightCommentSwatch.getBackground());
      }

      settings.setCellForeground(getSelectedCellForeground());
      settings.setCellBackground(getSelectedCellBackground());

      if (updateCellEditor)
      {
         gui.updatedCellEditorSettings();
      }
   }

   public DatatoolProperties getSettings()
   {
      return gui.getSettings();
   }

   public MessageHandler getMessageHandler()
   {
      return getSettings().getMessageHandler();
   }

   public DatatoolGuiResources getResources()
   {
      return gui.getResources();
   }

   public TeXJavaHelpLib getHelpLib()
   {
      return getSettings().getHelpLib();
   }

   private JCheckBox syntaxHighlightingBox;

   private SpinnerNumberModel fontSizeModel,
      editorHeightModel, editorWidthModel;

   private JComboBox<String> fontBox;

   private JComponent highlightCsSwatch, highlightCommentSwatch,
    editorForegroundSwatch, editorBackgroundSwatch;

   private JComponent highlightCsComp, highlightCommentComp;

   private static final Dimension SWATCH_SIZE = new Dimension(38,20);

   private JTextPane samplerComp;
   private FontSampleDocument samplerDocument;

   private DatatoolGUI gui;
   private String parentTag;

   boolean updateCellEditor = false;

   public static final String DEFAULT_PARENT_TAG = "preferences.editor";
}

class FontSampleDocument extends DefaultStyledDocument
{
   public FontSampleDocument(EditorPropertiesComponent propComp)
   {
      super();

      this.propComp = propComp;

      attrPlain = new SimpleAttributeSet();
      attrControlSequence = new SimpleAttributeSet();

      attrComment = new SimpleAttributeSet();
      StyleConstants.setItalic(attrComment, true);
   }

   public void update()
   {
      Color defaultForeground = propComp.getSelectedCellForeground();

      if (propComp.isSyntaxHighlightingOn())
      {
         Color commentHighlight = propComp.getCommentHighlight();
         Color csHighlight = propComp.getControlSequenceHighlight();

         StyleConstants.setForeground(attrControlSequence, csHighlight);
         StyleConstants.setForeground(attrComment, commentHighlight);
         StyleConstants.setItalic(attrComment, true);
      }
      else
      {
         StyleConstants.setForeground(attrControlSequence, defaultForeground);
         StyleConstants.setForeground(attrComment, defaultForeground);
         StyleConstants.setItalic(attrComment, false);
      }

      try
      {
         updateHighlight();
      }
      catch (BadLocationException e)
      {
      }
   }

   private void updateHighlight()
   throws BadLocationException
   {
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

   EditorPropertiesComponent propComp;

   private SimpleAttributeSet attrPlain;
   private SimpleAttributeSet attrControlSequence;
   private SimpleAttributeSet attrComment;

}
