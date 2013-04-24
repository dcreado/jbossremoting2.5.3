package org.jboss.remoting.samples.chat.client;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class ShutDownDialog extends JDialog {
  JPanel panel1 = new JPanel();
  JLabel jLabel1 = new JLabel();
  JLabel jLabel2 = new JLabel();
  JButton OKButton = new JButton();
  GridBagLayout gridBagLayout1 = new GridBagLayout();

  public ShutDownDialog(Frame frame, String title, boolean modal) {
    super(frame, title, modal);
    try {
      jbInit();
      pack();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

  public ShutDownDialog() {
    this(null, "", false);
  }
  private void jbInit() throws Exception {
    panel1.setLayout(gridBagLayout1);
    jLabel1.setFont(new java.awt.Font("Dialog", 1, 12));
    jLabel1.setText("Chat system is shutting down.");
    jLabel2.setFont(new java.awt.Font("Dialog", 1, 12));
    jLabel2.setText("Please try again later.");
    OKButton.setFont(new java.awt.Font("Dialog", 1, 14));
    OKButton.setText("OK");
    OKButton.addActionListener(new ShutDownDialog_OKButton_actionAdapter(this));
    getContentPane().add(panel1);
    panel1.add(jLabel1,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(71, 109, 0, 84), 37, 15));
    panel1.add(jLabel2,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(12, 134, 0, 100), 43, 3));
    panel1.add(OKButton,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(46, 156, 96, 171), 18, -4));
  }

  void OKButton_actionPerformed(ActionEvent e) {
    hide();
  }
}

class ShutDownDialog_OKButton_actionAdapter implements java.awt.event.ActionListener {
  ShutDownDialog adaptee;

  ShutDownDialog_OKButton_actionAdapter(ShutDownDialog adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.OKButton_actionPerformed(e);
  }
}