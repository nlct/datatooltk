package com.dickimawbooks.datatooltk.gui;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;

import com.dickimawbooks.datatooltk.DatatoolTk;

/**
 * Panel for error messages.
 */
public class ErrorPanel extends JPanel
  implements ActionListener
{
   public ErrorPanel()
   {
      super(new BorderLayout());

      messageArea = DatatoolGuiResources.createMessageArea();
      stackTraceArea = DatatoolGuiResources.createMessageArea();

      onlyMessageArea = DatatoolGuiResources.createMessageArea();

      cardLayout = new CardLayout();

      cardPanel = new JPanel(cardLayout);
      add(cardPanel, "Center");

      cardPanel.add(new JScrollPane(onlyMessageArea), "onlymessage");
      
      tabbedPane = new JTabbedPane();

      tabbedPane.addTab(DatatoolTk.getLabelWithAlt("error.message", "Error Message"),
       new JScrollPane(messageArea));

      tabbedPane.setMnemonicAt(tabbedPane.getTabCount()-1,
         DatatoolTk.getMnemonic("error.message"));

      tabbedPane.addTab(DatatoolTk.getLabelWithAlt("error.stacktrace", "Stack Trace"),
       new JScrollPane(stackTraceArea));

      tabbedPane.setMnemonicAt(tabbedPane.getTabCount()-1,
         DatatoolTk.getMnemonic("error.stacktrace"));

      cardPanel.add(tabbedPane, "tabbedpane");

      JPanel buttonPanel = new JPanel();

      buttonPanel.add(DatatoolGuiResources.createActionButton(
         "button", "copy", this, 
         KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.SHIFT_MASK)));

      add(buttonPanel, "East");

   }

   public synchronized void updateMessage(String message)
   {
      updateMessage(message, null);
   }

   public synchronized void updateMessage(String message, Exception exception)
   {
      if (exception == null)
      {
         onlyMessageArea.setText(message);

         cardLayout.show(cardPanel, "onlymessage");
      }
      else
      {
         messageArea.setText(message == null ? exception.getMessage() : message);

         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

         exception.printStackTrace(new PrintStream(outputStream));

         stackTraceArea.setText(outputStream.toString());

         tabbedPane.setSelectedIndex(0);

         cardLayout.show(cardPanel, "tabbedpane");
      }
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null)
      {
         return;
      }

      if (action.equals("copy"))
      {
         JTextArea area;

         // which card is being viewed?

         if (tabbedPane.isVisible())
         {
            // Which tab is being viewed?

            int idx = tabbedPane.getSelectedIndex();

            if (idx == 0)
            {
               area = messageArea;
            }
            else
            {
               area = stackTraceArea;
            }
         }
         else
         {
            area = onlyMessageArea;
         }

         area.selectAll();
         area.copy();
      }
   }

   private JTextArea messageArea; // area for error messages.
   private JTextArea stackTraceArea; // area for stack traces

   private JTextArea onlyMessageArea; // area for error messages (without exceptions)

   private JTabbedPane tabbedPane;

   private CardLayout cardLayout;

   private JPanel cardPanel;
}
