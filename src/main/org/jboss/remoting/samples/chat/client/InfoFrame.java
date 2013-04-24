package org.jboss.remoting.samples.chat.client;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jboss.remoting.samples.chat.exceptions.ConnectionException;
import org.jboss.remoting.samples.chat.exceptions.InfoConnectionException;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0 
 */
 
interface InfoConnectionStrategy
{ 
    void getId(ChatInfo chatInfo) throws ConnectionException;
}


public class InfoFrame extends CloseableFrame implements Closeable{
    JLabel jLabel1 = new JLabel();
    JLabel jLabel2 = new JLabel();
    JLabel jLabel3 = new JLabel();
    JTextField jTextField1 = new JTextField();
    JTextField jTextField2 = new JTextField();
    JTextField numberOfMessages = new JTextField();
    JButton joinButton = new JButton();
    JButton closeButton = new JButton();
    GridBagLayout gridBagLayout1 = new GridBagLayout();

    InfoConnectionStrategy ics;
    ChatInfo chatInfo;

  public InfoFrame(
      InfoConnectionStrategy ics,
      ChatInfo chatInfo,
      Closeable parent)
   {
    super(parent);
    this.ics = ics;
    this.chatInfo = chatInfo;

    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }


  private void jbInit() throws Exception {
    jLabel1.setFont(new java.awt.Font("SansSerif", 1, 12));
    jLabel1.setText("Description:");
    this.getContentPane().setLayout(gridBagLayout1);
    jLabel2.setFont(new java.awt.Font("SansSerif", 1, 12));
    jLabel2.setText("Current members:");
    jLabel3.setFont(new java.awt.Font("SansSerif", 1, 12));
    jLabel3.setAlignmentX((float) 0.0);
    jLabel3.setText("Number of messages:");
    jTextField1.setEditable(false);
    jTextField1.setText(chatInfo.get_description());
    jTextField2.setEditable(false);
    jTextField2.setText(Integer.toString(chatInfo.get_currentMembers()));
    numberOfMessages.setEditable(false);
    numberOfMessages.setText(Integer.toString(chatInfo.get_size()));
    joinButton.setFont(new java.awt.Font("SansSerif", 1, 12));
    joinButton.setText("Join");
    joinButton.addActionListener(new InfoFrame_joinButton_actionAdapter(this));
    closeButton.setFont(new java.awt.Font("SansSerif", 1, 12));
    closeButton.setText("Close");
    closeButton.addActionListener(new InfoFrame_closeButton_actionAdapter(this));
    this.getContentPane().setBackground(SystemColor.control);
    this.setLocale(java.util.Locale.getDefault());
    this.setTitle("Chat room information");
    this.getContentPane().add(jLabel2,    new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(11, 43, 0, 0), 11, 2));
    this.getContentPane().add(jLabel1,    new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(44, 43, 10, 40), 7, 2));
    this.getContentPane().add(jLabel3,    new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(21, 43, 0, 0), 15, 2));
    this.getContentPane().add(jTextField1,    new GridBagConstraints(2, 0, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(44, 0, 0, 38), 304, 7));
    this.getContentPane().add(jTextField2,    new GridBagConstraints(2, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(9, 0, 0, 0), 51, 0));
    this.getContentPane().add(numberOfMessages,    new GridBagConstraints(2, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(18, 0, 0, 0), 50, 0));
    this.getContentPane().add(closeButton,    new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 17, 50, 201), 6, 0));
    this.getContentPane().add(joinButton,      new GridBagConstraints(1, 3, 2, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(42, 0, 93, 8), 14, 0));

    pack();    // added by hand
    center();  // added by hand
  }


  public static void main(String[] args) {
    try
    {
      Date gc = new Date();

      ChatMember chatMember = new ChatMember();
      chatMember.set_name("jj");
      ChatInfo chatInfo = new ChatInfo();
      chatInfo.set_currentMembers(3);
      chatInfo.set_description("Black holes");
      chatInfo.set_origin(gc);

      InfoFrame infoFrame = new InfoFrame(
          new DummyInfoConnectionStrategy(),
          chatInfo,
          null);

      infoFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      infoFrame.show();
    }
    catch (InfoConnectionException ce) {
      System.out.println("unable to create chat frame");
    }
  }

  void joinButton_actionPerformed(ActionEvent e) {
    try
    {
      ics.getId(chatInfo);
      notifyOnClose(this);
    }
    catch (ConnectionException ce)
    {
        System.out.println("Join button catches exception:");
        System.out.println(ce);
    }
  }

  void closeButton_actionPerformed(ActionEvent e) {
    notifyOnClose(this);
  }

}


class DummyInfoConnectionStrategy implements InfoConnectionStrategy
{
    public DummyInfoConnectionStrategy() throws InfoConnectionException
    {
        System.out.println("DummyInfoConnectionStrategy()");
    }

    public void getId(ChatInfo chatInfo)
    {
        System.out.println("DummyInfoConnectionStrategy.getId(" + chatInfo.get_key() + ")");
    }
}

class InfoFrame_joinButton_actionAdapter implements java.awt.event.ActionListener {
  InfoFrame adaptee;

  InfoFrame_joinButton_actionAdapter(InfoFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.joinButton_actionPerformed(e);
  }
}

class InfoFrame_closeButton_actionAdapter implements java.awt.event.ActionListener {
  InfoFrame adaptee;

  InfoFrame_closeButton_actionAdapter(InfoFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.closeButton_actionPerformed(e);
  }
}