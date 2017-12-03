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

import java.io.File;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.*;
import com.dickimawbooks.datatooltk.io.*;

/**
 * Dialog box for importing SQL data.
 */
public class ImportSqlDialog extends JDialog
  implements ActionListener
{
   public ImportSqlDialog(DatatoolGUI gui)
   {
      super(gui, gui.getMessageHandler().getLabel("importsql.title"), true);

      this.gui = gui;
      this.settings = new DatatoolSettings(gui.getDatatoolTk());
      this.settings.setPasswordReader(new GuiPasswordReader(
         gui.getMessageHandler(), this));

      JComponent mainPanel = Box.createVerticalBox();
      getContentPane().add(mainPanel, BorderLayout.CENTER);

      JLabel[] labels = new JLabel[5];
      int idx = 0;
      int maxWidth = 0;
      Dimension dim;
      JComponent box;

      box = createNewRow(mainPanel);

      databaseField = new JTextField(16);

      labels[idx] = createLabel("importsql.database", databaseField);
      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);
      box.add(labels[idx++]);
      box.add(databaseField);

      box = createNewRow(mainPanel);

      userField = new JTextField(16);

      labels[idx] = createLabel("importsql.user", userField);
      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);
      box.add(labels[idx++]);
      box.add(userField);

      box = createNewRow(mainPanel);

      hostField = new JTextField(16);

      labels[idx] = createLabel("importsql.host", hostField);
      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);
      box.add(labels[idx++]);
      box.add(hostField);

      box = createNewRow(mainPanel);

      portField = new NonNegativeIntField(3306);

      labels[idx] = createLabel("importsql.port", portField);
      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);
      box.add(labels[idx++]);

      box.add(portField);

      box = createNewRow(mainPanel);

      prefixField = new JTextField(16);

      labels[idx] = createLabel("importsql.prefix", prefixField);
      dim = labels[idx].getPreferredSize();
      maxWidth = Math.max(maxWidth, dim.width);
      box.add(labels[idx++]);
      box.add(prefixField);

      for (idx = 0; idx < labels.length; idx++)
      {
         dim = labels[idx].getPreferredSize();
         dim.width = maxWidth;
         labels[idx].setPreferredSize(dim);
      }

      box = Box.createHorizontalBox();
      getContentPane().add(box, BorderLayout.NORTH);

      selectField = new JTextField("* FROM tablename");
      box.add(createLabel("importsql.select", selectField));
      box.add(selectField);


      getContentPane().add(
         gui.getResources().createOkayCancelPanel(this),
         BorderLayout.SOUTH);

      pack();

      setLocationRelativeTo(null);
   }

   private JComponent createNewRow(JComponent tab)
   {
      JComponent comp = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
      comp.setAlignmentX(0);
      tab.add(comp);

      return comp;
   }

   private JLabel createLabel(String label, JComponent comp)
   {
      return gui.getResources().createJLabel(label, comp);
   }

   public void requestImport(DatatoolSettings settings)
   {
      hostField.setText(settings.getSqlHost());
      prefixField.setText(settings.getSqlPrefix());
      portField.setValue(settings.getSqlPort());

      String user = settings.getSqlUser();

      userField.setText(user == null ? "" : user);

      String db = settings.getSqlDbName();

      databaseField.setText(db == null ? "" : db);

      setVisible(true);
   }

   public void actionPerformed(ActionEvent event)
   {
      String action = event.getActionCommand();

      if (action == null) return;

      if (action.equals("okay"))
      {
         okay();
      }
      else if (action.equals("cancel"))
      {
         setVisible(false);
      }
   }

   private void okay()
   {
      if (portField.getText().isEmpty())
      {
         getMessageHandler().error(this,
            getMessageHandler().getLabel("error.missing_port"));
         return;
      }

      int port        = portField.getValue();
      String prefix   = prefixField.getText();
      String host     = hostField.getText();
      String database = databaseField.getText();
      String user     = userField.getText();
      String select   = selectField.getText();

      if (prefix.isEmpty())
      {
         getMessageHandler().error(this,
            getMessageHandler().getLabel("error.missing_prefix"));
         return;
      }

      if (host.isEmpty())
      {
         getMessageHandler().error(this,
            getMessageHandler().getLabel("error.missing_host"));
         return;
      }

      if (database.isEmpty())
      {
         getMessageHandler().error(this,
            getMessageHandler().getLabel("error.missing_dbname"));
         return;
      }

      if (user.isEmpty())
      {
         getMessageHandler().error(this,
            getMessageHandler().getLabel("error.missing_user"));
         return;
      }

      if (select.isEmpty())
      {
         getMessageHandler().error(this,
            getMessageHandler().getLabel("error.missing_select"));
         return;
      }

      settings.setSqlPort(port);
      settings.setSqlHost(host);
      settings.setSqlPrefix(prefix);
      settings.setSqlDbName(database);
      settings.setSqlUser(user);

      gui.importData(new DatatoolSql(settings), "SELECT "+select);

      setVisible(false);
   }

   public MessageHandler getMessageHandler()
   {
      return gui.getMessageHandler();
   }

   private JTextField hostField, prefixField, databaseField, userField,
      selectField;

   private NonNegativeIntField portField;

   private DatatoolGUI gui;

   private DatatoolSettings settings;
}
