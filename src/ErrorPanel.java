package com.dickimawbooks.datatooltk;

import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Panel for error messages.
 */
public class ErrorPanel extends JPanel
  implements ActionListener
{
   public ErrorPanel()
   {
      super(new BorderLayout());

      messageArea = createMessageArea();
      stackTraceArea = createMessageArea();

      onlyMessageArea = createMessageArea();

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
         DatatoolTk.getLabel("button.copy"),
         DatatoolTk.getMnemonic("button.copy"),
         "copy", this,
         KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.SHIFT_MASK),
         DatatoolTk.getLabel("button.copy.tooltip")));

      add(buttonPanel, "East");

   }

   private JTextArea createMessageArea()
   {
      JTextArea area = new JTextArea(8, 30);
      area.setWrapStyleWord(true);
      area.setLineWrap(true);
      area.setEditable(false);
      area.setBorder(null);
      area.setOpaque(false);

      return area;
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
