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

import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.*;

import com.dickimawbooks.texjavahelplib.JLabelGroup;
import com.dickimawbooks.datatooltk.*;

public class SqlPanel extends JPanel
{
   public SqlPanel(DatatoolGUI gui, String tagPrefix)
   {
      super(null);
      setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

      this.gui = gui;

      init(tagPrefix);
   }

   private void init(String tagPrefix)
   {
      JLabelGroup labelGrp = new JLabelGroup();

      JComponent box = createNewRow();

      hostField = new JTextField(16);

      box.add(createLabel(labelGrp, tagPrefix+".sql.host", hostField));
      box.add(hostField);

      box = createNewRow();

      portField = new NonNegativeIntField(3306);

      box.add(createLabel(labelGrp, tagPrefix+".sql.port", portField));

      box.add(portField);

      box = createNewRow();

      prefixField = new JTextField(16);

      box.add(createLabel(labelGrp, tagPrefix+".sql.prefix", prefixField));
      box.add(prefixField);

      box = createNewRow();

      databaseField = new JTextField(16);

      box.add(createLabel(labelGrp, tagPrefix+".sql.database", databaseField));
      box.add(databaseField);

      box = createNewRow();

      userField = new JTextField(16);

      box.add(createLabel(labelGrp, tagPrefix+".sql.user", userField));
      box.add(userField);

      wipeBox = getResources().createJCheckBox(tagPrefix+".sql", "wipe", null, 0f);
      add(wipeBox);
   }

   private JComponent createNewRow()
   {
      return createNewRow(this);
   }

   private JComponent createNewRow(Container parent)
   {
      JComponent comp = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 1));
      comp.setAlignmentX(0);

      parent.add(comp);

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

   private JLabel createLabel(JLabelGroup grp, String label, JComponent comp)
   {
      return getResources().createJLabel(grp, label, comp);
   }

   private JLabel createLabel(String label, JComponent comp)
   {
      return getResources().createJLabel(label, comp);
   }

   public void resetFrom(DatatoolProperties settings)
   {
      hostField.setText(settings.getSqlHostDefault());
      prefixField.setText(settings.getSqlPrefixDefault());
      portField.setValue(settings.getSqlPortDefault());
      wipeBox.setSelected(settings.isWipePasswordEnabledDefault());

      String user = settings.getSqlUserDefault();

      userField.setText(user == null ? "" : user);

      String db = settings.getSqlDbNameDefault();

      databaseField.setText(db == null ? "" : db);
   }

   public void applyTo(DatatoolProperties settings, boolean updateProperty)
    throws IllegalArgumentException
   {
      String host = hostField.getText();

      if (host.isEmpty())
      {
         throw new IllegalArgumentException(
            getMessageHandler().getLabel("error.missing_host"));
      }

      String prefix = prefixField.getText();

      if (prefix.isEmpty())
      {
         throw new IllegalArgumentException(
            getMessageHandler().getLabel("error.missing_prefix"));
      }

      if (portField.getText().isEmpty())
      {
         throw new IllegalArgumentException(
            getMessageHandler().getLabel("error.missing_port"));
      }

      if (updateProperty)
      {
         settings.setSqlHostProperty(host);
         settings.setSqlPrefixProperty(prefix);
         settings.setSqlPortProperty(portField.getValue());
         settings.setSqlUserProperty(userField.getText());
         settings.setSqlDbNameDefault(databaseField.getText());
      }
      else
      {
         settings.setSqlHostDefault(host);
         settings.setSqlPrefixDefault(prefix);
         settings.setSqlPortDefault(portField.getValue());
         settings.setSqlUserDefault(userField.getText());
         settings.setSqlDbNameDefault(databaseField.getText());
      }
   }

   public void resetFrom(ImportSettings settings)
   {
      hostField.setText(settings.getSqlHost());
      prefixField.setText(settings.getSqlPrefix());
      portField.setValue(settings.getSqlPort());
      wipeBox.setSelected(settings.isWipePasswordOn());

      String user = settings.getSqlUser();

      userField.setText(user == null ? "" : user);

      String db = settings.getSqlDbName();

      databaseField.setText(db == null ? "" : db);
   }

   public void applyTo(ImportSettings settings)
    throws IllegalArgumentException
   {
      String host = hostField.getText();

      if (host.isEmpty())
      {
         throw new IllegalArgumentException(
            getMessageHandler().getLabel("error.missing_host"));
      }

      settings.setSqlHost(host);

      String prefix = prefixField.getText();

      if (prefix.isEmpty())
      {
         throw new IllegalArgumentException(
            getMessageHandler().getLabel("error.missing_prefix"));
      }

      settings.setSqlPrefix(prefix);

      if (portField.getText().isEmpty())
      {
         throw new IllegalArgumentException(
            getMessageHandler().getLabel("error.missing_port"));
      }

      settings.setSqlPort(portField.getValue());

      settings.setSqlUser(userField.getText());
      settings.setSqlDbName(databaseField.getText());

      settings.setWipePassword(wipeBox.isSelected());
   }

   public DatatoolGuiResources getResources()
   {
      return gui.getResources();
   }

   public MessageHandler getMessageHandler()
   {
      return gui.getMessageHandler();
   }

   DatatoolGUI gui;

   private JCheckBox wipeBox;

   private NonNegativeIntField portField;

   private JTextField hostField, prefixField, databaseField, userField;

}
