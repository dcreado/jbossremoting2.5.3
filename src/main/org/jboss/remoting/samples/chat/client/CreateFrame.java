package org.jboss.remoting.samples.chat.client;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jboss.remoting.samples.chat.exceptions.CreateConnectionException;

/**
 * <p>Title: Chat</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable 
 * @version 1.0
 */

interface CreateConnectionStrategy
{
    void createChat(String description, ChatMember owner)
        throws CreateConnectionException;
}
 
public class CreateFrame extends CloseableFrame {
  JLabel jDescriptionLabel = new JLabel();
  JLabel IDLabel = new JLabel();
  JTextField descriptionField = new JTextField();
  JTextField idField = new JTextField();
  JButton OKButton = new JButton();
  JButton CancelButton = new JButton();
  GridBagLayout gridBagLayout1 = new GridBagLayout();

  private CreateConnectionStrategy ccs;

  public CreateFrame(
      CreateConnectionStrategy ccs,
      Closeable parent)
  {
    super(parent);
    this.ccs = ccs;

    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void jbInit() throws Exception {
    jDescriptionLabel.setFont(new java.awt.Font("SansSerif", 1, 12));
    jDescriptionLabel.setText("Description:");
    this.getContentPane().setLayout(gridBagLayout1);
    IDLabel.setFont(new java.awt.Font("SansSerif", 1, 12));
    IDLabel.setText("Your ID:");
    descriptionField.setText("");
    idField.setText("");
    OKButton.setFont(new java.awt.Font("SansSerif", 1, 14));
    OKButton.setText("OK");
    OKButton.addActionListener(new CreateFrame_OKButton_actionAdapter(this));
    CancelButton.setFont(new java.awt.Font("SansSerif", 1, 14));
    CancelButton.setText("Cancel");
    CancelButton.addActionListener(new CreateFrame_CancelButton_actionAdapter(this));
    this.setLocale(java.util.Locale.getDefault());
    this.setResizable(false);
    this.setTitle("Create a chat room");
    this.setSize(new Dimension(00, 300));
    this.getContentPane().add(jDescriptionLabel,  new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(25, 21, 0, 0), 11, 4));
    this.getContentPane().add(IDLabel,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 21, 0, 0), 27, 0));
    this.getContentPane().add(descriptionField,  new GridBagConstraints(2, 0, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(25, 0, 0, 16), 270, 1));
    this.getContentPane().add(OKButton,  new GridBagConstraints(1, 2, 2, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(81, 0, 115, 0), 30, -5));
    this.getContentPane().add(CancelButton,  new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(81, 27, 115, 103), 11, -2));
    this.getContentPane().add(idField, new GridBagConstraints(2, 1, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(9, 0, 0, 22), 272, 2));

    center();
    pack();
  }

  void OKButton_actionPerformed(ActionEvent e) {
    try
    {
      notifyOnClose(this);
      ChatMember chatMember = new ChatMember();
      chatMember.set_name(idField.getText());
      ccs.createChat(descriptionField.getText(),chatMember);
    }
    catch (CreateConnectionException cce)
    {
      System.out.println("OK button catches exception:");
      System.out.println(cce);
    }
  }

  void CancelButton_actionPerformed(ActionEvent e) {
    notifyOnClose(this);
  }

  public static void main(String[] args) {
     CreateFrame createFrame;
     try
     {
       createFrame = new CreateFrame(
                     new DummyCreateConnectionStrategy(),
                     null);

       createFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       createFrame.show();
     }
     catch (CreateConnectionException ce)
     {
       System.out.println("unable to create create frame");
     }
   }
}


class CreateFrame_OKButton_actionAdapter implements java.awt.event.ActionListener {
  CreateFrame adaptee;

  CreateFrame_OKButton_actionAdapter(CreateFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.OKButton_actionPerformed(e);
  }
}

class CreateFrame_CancelButton_actionAdapter implements java.awt.event.ActionListener {
  CreateFrame adaptee;

  CreateFrame_CancelButton_actionAdapter(CreateFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.CancelButton_actionPerformed(e);
  }
}

class DummyCreateConnectionStrategy implements CreateConnectionStrategy
{
    public DummyCreateConnectionStrategy() throws CreateConnectionException
    {
        System.out.println("DummyCreateConnectionStrategy()");
    }

    public void createChat(String description, ChatMember owner)
        throws CreateConnectionException
    {
        System.out.println("description: " + description);
        System.out.println("nickName:    " + owner.get_name());
    }
}
