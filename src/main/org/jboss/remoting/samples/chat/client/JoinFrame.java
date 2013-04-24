package org.jboss.remoting.samples.chat.client;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jboss.remoting.samples.chat.exceptions.JoinConnectionException;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

interface JoinConnectionStrategy
{
    void join(ChatInfo chatInfo, ChatMember newMember)
        throws JoinConnectionException;
}  


public class JoinFrame extends CloseableFrame {
  JLabel jLabel1 = new JLabel();
  JTextField IDField = new JTextField();
  JButton OKButton = new JButton();
  JButton CancelButton = new JButton();

  private ChatInfo chatInfo;
  private JoinConnectionStrategy jcs;
  GridBagLayout gridBagLayout1 = new GridBagLayout();


  public JoinFrame(
      ChatInfo chatInfo,
      JoinConnectionStrategy jcs,
      Closeable parent)
  {
    super(parent);
    this.chatInfo = chatInfo;
    this.jcs = jcs;

    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }


  private void jbInit() throws Exception {
    jLabel1.setFont(new java.awt.Font("SansSerif", 1, 12));
    jLabel1.setText("Your ID:");
    this.getContentPane().setLayout(gridBagLayout1);
    IDField.setText("");
    OKButton.setFont(new java.awt.Font("SansSerif", 1, 12));
    OKButton.setText("OK");
    OKButton.addActionListener(new JoinFrame_OKButton_actionAdapter(this));
    CancelButton.setFont(new java.awt.Font("SansSerif", 1, 12));
    CancelButton.setText("Cancel");
    CancelButton.addActionListener(new JoinFrame_CancelButton_actionAdapter(this));
    this.setTitle("Join a Chat Room");
    this.getContentPane().add(jLabel1,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(96, 82, 0, 0), 22, 5));
    this.getContentPane().add(IDField,  new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(96, 0, 0, 39), 215, 0));
    this.getContentPane().add(CancelButton,  new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(47, 16, 43, 139), 0, 0));
    this.getContentPane().add(OKButton,  new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(49, 108, 43, 0), 22, 0));

    pack();
    center();
  }


  public static void main(String[] args)
  {
      try
      {
        ChatMember chatMember = new ChatMember();
        chatMember.set_name("xyz");
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.set_currentMembers(3);
        chatInfo.set_description("abc");
        chatInfo.set_origin(new Date());
          JoinFrame joinFrame = new JoinFrame(
                          chatInfo,
                          new DummyJoinConnectionStrategy(),
                          null);
          joinFrame.show();
      }
      catch (JoinConnectionException ce)
      {
          System.out.println("unable to create chat frame");
      }
  }

  void OKButton_actionPerformed(ActionEvent e) {
    try
    {
        notifyOnClose(this);
        ChatMember chatMember = new ChatMember();
        chatMember.set_name(IDField.getText());
        jcs.join(chatInfo, chatMember);
    }
    catch (JoinConnectionException jce)
    {
        System.out.println("OK button catches exception:");
        System.out.println(jce);
    }
  }

  void CancelButton_actionPerformed(ActionEvent e) {
	notifyOnClose(this);
  }

}


class DummyJoinConnectionStrategy implements JoinConnectionStrategy
{
    public DummyJoinConnectionStrategy() throws JoinConnectionException
    {
        System.out.println("DummyJoinConnectionStrategy()");
    }

    public void join(ChatInfo chatInfo, ChatMember newMember) throws JoinConnectionException
    {
        System.out.println("DummyJoinConnectionStrategy.getId(" + chatInfo.get_key() + ", " + newMember.get_name() + ")");
    }
}

class JoinFrame_OKButton_actionAdapter implements java.awt.event.ActionListener {
  JoinFrame adaptee;

  JoinFrame_OKButton_actionAdapter(JoinFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.OKButton_actionPerformed(e);
  }
}

class JoinFrame_CancelButton_actionAdapter implements java.awt.event.ActionListener {
  JoinFrame adaptee;

  JoinFrame_CancelButton_actionAdapter(JoinFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.CancelButton_actionPerformed(e);
  }
}
