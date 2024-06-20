/*
    Copyright (C) 2013 Nicola L.C. Talbot
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.texparserlib.latex.datatool.DatumType;

import com.dickimawbooks.datatooltk.*;

/**
 * Dialog box to allow user to edit column header information.
 */
public class HeaderDialog extends JDialog
  implements ActionListener
{
   public HeaderDialog(DatatoolGUI gui)
   {
      super(gui, gui.getMessageHandler().getLabel("header.title"), true);

      messageHandler = gui.getMessageHandler();

      DatatoolGuiResources resources = messageHandler.getDatatoolGuiResources();

      Box box = Box.createVerticalBox();
      getContentPane().add(box, BorderLayout.CENTER);

      JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
      box.add(p);

      Dimension dim;
      int idx = 0;
      int maxWidth = 0;
      JLabel[] labels = new JLabel[3];

      titleField = new JTextField(32);
      labels[idx] = resources.createJLabel("header.column_title",
        titleField);

      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);

      p.add(labels[idx++]);
      p.add(titleField);

      p = new JPanel(new FlowLayout(FlowLayout.LEFT));
      box.add(p);

      labelField = new JTextField(10);

      labels[idx] = resources.createJLabel("header.column_label",
         labelField);

      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);

      p.add(labels[idx++]);
      p.add(labelField);
      p.add(resources.createJLabel("header.column_label_note"));

      p = new JPanel(new FlowLayout(FlowLayout.LEFT));
      box.add(p);

      typeBox = new DatumTypeComboBox(getSettings());

      labels[idx] = resources.createJLabel("header.column_type",
         typeBox);

      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);

      p.add(labels[idx++]);
      p.add(typeBox);

      for (idx = 0; idx < labels.length; idx++)
      {
         dim = labels[idx].getPreferredSize();
         dim.width = maxWidth;
         labels[idx].setPreferredSize(dim);
      }

      getContentPane().add(
       resources.createOkayCancelHelpPanel(this,
        gui, "editheader"),
       BorderLayout.SOUTH);

      pack();

      setLocationRelativeTo(null);
   }

   public DatatoolHeader requestEdit(int colIdx, DatatoolDb db)
   {
      this.colIdx = colIdx;
      return requestEdit((DatatoolHeader)db.getHeader(colIdx).clone(), db, false);
   }

   public DatatoolHeader requestEdit(DatatoolHeader header, DatatoolDb db)
   {
      return requestEdit(header, db, true);
   }

   public DatatoolHeader requestEdit(DatatoolHeader aHeader, DatatoolDb db,
      boolean checkUnique)
   {
      this.db = db;
      this.header = aHeader;
      this.checkUnique = checkUnique;

      modified = false;
      setTitle(messageHandler.getLabelWithValues("header.title", header.getKey()));

      titleField.setText(header.getTitle());
      labelField.setText(header.getKey());
      typeBox.setSelectedType(header.getDatumType());

      titleField.requestFocusInWindow();

      setVisible(true);

      return modified ? this.header : null;
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
         colIdx = -1;
         setVisible(false);
      }
   }

   private void okay()
   {
      DatumType type = typeBox.getSelectedType();

      DatatoolSettings settings = getSettings();

      String[] typeLabels = settings.getTypeLabels();

      if (colIdx > -1 && type != DatumType.UNKNOWN
           && type != DatumType.STRING)
      {
         // Is the requested data type valid for this column?

         int rowIdx = 0;

         for (ColumnEnumeration en = db.getColumnEnumeration(colIdx);
            en.hasMoreElements(); )
         {
            rowIdx++;

            Datum element = en.nextElement();

            DatumType thisType = element.getDatumType();

            switch (type)
            {
               case INTEGER:
                  if (thisType == DatumType.DECIMAL
                     || thisType == DatumType.CURRENCY
                     || thisType == DatumType.STRING)
                  {
                     messageHandler.error(this, 
                        messageHandler.getLabelWithValues(
                            "error.invalid_header_choice",
                            typeLabels[type.getValue()+1],
                            rowIdx,
                            typeLabels[thisType.getValue()+1]
                          ));

                     return;
                  }
               break;
               case DECIMAL:
                  if (thisType == DatumType.CURRENCY
                   || thisType == DatumType.STRING)
                  {
                     messageHandler.error(this, 
                        messageHandler.getLabelWithValues(
                            "error.invalid_header_choice",
                            typeLabels[type.getValue()+1],
                            rowIdx,
                            typeLabels[thisType.getValue()+1]
                          ));

                     return;
                  }
               break;
               case CURRENCY:
                  if (thisType == DatumType.STRING)
                  {
                     messageHandler.error(this, 
                        messageHandler.getLabelWithValues(
                          "error.invalid_header_choice",
                          typeLabels[type.getValue()+1],
                          rowIdx,
                          typeLabels[thisType.getValue()+1]
                         ));

                     return;
                  }
               break;
            }
         }
      }

      String key = labelField.getText();

      if (settings.isAutoTrimLabelsOn())
      {// trim here, to help empty and non-unique checks
         key = key.trim();
      }

      if (key.isEmpty())
      {
         messageHandler.error(this, 
            messageHandler.getLabel("error.missing_key"));
         return;
      }

      if (checkUnique || !header.getKey().equals(key))
      {
         // Only test if key has been changed unless checkUnique
         // set.

         if (db.getHeader(key) != null)
         {
            messageHandler.error(this, 
               messageHandler.getLabelWithValues("error.key_exists", key));

            return;
         }
      }

      String title = titleField.getText();

      header.setTitle(title.isEmpty() ? key : title);

      header.setKey(key);
      header.setType(type);
      modified = true;

      colIdx = -1;
      setVisible(false);
   }

   public MessageHandler getMessageHandler()
   {
      return messageHandler;
   }

   public DatatoolSettings getSettings()
   {
      return messageHandler.getSettings();
   }

   private JTextField titleField, labelField;

   private DatumTypeComboBox typeBox;

   private DatatoolHeader header;

   private DatatoolDb db;

   private MessageHandler messageHandler;

   private boolean checkUnique;

   private boolean modified;

   private int colIdx = -1;
}
