package com.dickimawbooks.datatooltk.gui;

import java.io.File;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.*;
import com.dickimawbooks.datatooltk.io.*;

public class ImportSqlDialog extends JDialog
  implements ActionListener
{
   public ImportSqlDialog(DatatoolGUI gui)
   {
      super(gui, DatatoolTk.getLabel("importsql.title"), true);

      this.gui = gui;
      this.settings = new DatatoolSettings();
      this.settings.setPasswordReader(new GuiPasswordReader(this));

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
         DatatoolGuiResources.createOkayCancelPanel(this),
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
      return DatatoolGuiResources.createJLabel(label, comp);
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
         DatatoolGuiResources.error(this,
            DatatoolTk.getLabel("error.missing_port"));
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
         DatatoolGuiResources.error(this,
            DatatoolTk.getLabel("error.missing_prefix"));
         return;
      }

      if (host.isEmpty())
      {
         DatatoolGuiResources.error(this,
            DatatoolTk.getLabel("error.missing_host"));
         return;
      }

      if (database.isEmpty())
      {
         DatatoolGuiResources.error(this,
            DatatoolTk.getLabel("error.missing_dbname"));
         return;
      }

      if (user.isEmpty())
      {
         DatatoolGuiResources.error(this,
            DatatoolTk.getLabel("error.missing_user"));
         return;
      }

      if (select.isEmpty())
      {
         DatatoolGuiResources.error(this,
            DatatoolTk.getLabel("error.missing_select"));
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

   private JTextField hostField, prefixField, databaseField, userField,
      selectField;

   private NonNegativeIntField portField;

   private DatatoolGUI gui;

   private DatatoolSettings settings;
}
