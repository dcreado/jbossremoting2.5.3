package org.jboss.remoting.samples.chat.client;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.jboss.remoting.samples.chat.exceptions.TalkConnectionException;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable 
 * @version 1.0
 */

interface TalkConnectionStrategy
{
  void send(ChatMessage message) throws TalkConnectionException;
  void leave() throws TalkConnectionException;
}


public class TalkFrame extends CloseableFrame {
  JScrollPane jScrollPane1 = new JScrollPane();
  JButton closeButton = new JButton();
  JButton exitButton = new JButton();
  JTextArea backChatTextArea = new JTextArea();
  JTextField newMessage = new JTextField();
  JLabel talkLabel = new JLabel();
  GridBagLayout gridBagLayout1 = new GridBagLayout();

  private String description;
  private String nickname;
  private String[] backChat;
  private TalkConnectionStrategy tcs;
  private Font plainFont;
  private Font boldFont;
  private String chatKey;

  private static ArrayList talkWindows = new ArrayList();

  public final boolean NEW_CHAT = true;
  public final boolean OLD_CHAT = false;

  public TalkFrame(String description, String nickname, Closeable parent)
  {
    super(parent);
    this.description = description;
    this.nickname = nickname;

     try {
      jbInit();
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    plainFont = new Font("SanSerif", Font.PLAIN, 12);
    boldFont = new Font("SanSerif", Font.BOLD, 12);

    talkWindows.add(this);
  }

  private void jbInit() throws Exception {
    this.getContentPane().setLayout(gridBagLayout1);
    closeButton.setFont(new java.awt.Font("SansSerif", 1, 12));
    closeButton.setText("Close");
    closeButton.addActionListener(new TalkFrame_closeButton_actionAdapter(this));
    exitButton.setFont(new java.awt.Font("SansSerif", 1, 12));
    exitButton.setText("Exit");
    exitButton.addActionListener(new TalkFrame_exitButton_actionAdapter(this));
    backChatTextArea.setFont(new java.awt.Font("Monospaced", 0, 13));
    backChatTextArea.setDisabledTextColor(Color.white);
    backChatTextArea.setEditable(false);
    backChatTextArea.setText("");
    newMessage.setText("");
    newMessage.addActionListener(new TalkFrame_newMessage_actionAdapter(this));
    newMessage.requestFocus();
    talkLabel.setFont(new java.awt.Font("SansSerif", 1, 12));
    talkLabel.setText("Talk:");
    jScrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    jScrollPane1.getViewport().setBackground(Color.white);
    jScrollPane1.setAutoscrolls(true);
    this.getContentPane().setBackground(SystemColor.control);
    this.getContentPane().add(closeButton,  new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(28, 204, 21, 0), 6, 0));
    this.getContentPane().add(exitButton,  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(28, 17, 21, 280), 18, 0));
    this.getContentPane().add(jScrollPane1,  new GridBagConstraints(1, 0, 2, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 8, 0, 33), 602, 264));
    this.getContentPane().add(talkLabel,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(34, 15, 0, 0), 7, -1));
    this.getContentPane().add(newMessage,  new GridBagConstraints(1, 1, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(25, 10, 0, 33), 602, 6));
    jScrollPane1.getViewport().add(backChatTextArea, null);

    setTitle(description);

    pack();
    center();
  }

  public void registerStrategy(TalkConnectionStrategy tcs)
  {
    this.tcs = tcs;
  }
  
  
  public void registerChatKey(String chatKey)
  {
  	System.out.println("TalkFrame: registering chat key: " + chatKey);
  	this.chatKey = chatKey;
  }


  public void appendMessage(ChatMessage chatMessage)
  {
  	String message = chatMessage.get_message();
    int mark = message.indexOf(':');
    int boundedMark = java.lang.Math.max(0, mark); // if ':' is missing, mark == -1
    boundedMark = java.lang.Math.min(8, boundedMark); // restrict names to 8 characters
    String name = message.substring(0, boundedMark) + ":" + "        ".substring(0, 8 - boundedMark);
    String content = message.substring(mark + 1) + "\n";
    backChatTextArea.append(name + content);

//    backChatTextArea.append(message);
/*
//    System.out.println("****appendMessage: scrollbar position: " + jScrollPane1.getViewport().getViewPosition());
    int value = jScrollPane1.getVerticalScrollBar().getModel().getMaximum()
        - jScrollPane1.getVerticalScrollBar().getModel().getExtent();
    value = (int) Math.round(.9 * value);
//    System.out.println("****appendMessage: setting scrollbar value to " + value);
    jScrollPane1.getVerticalScrollBar().getModel().setValue(value);
*/
  }


  public void appendMessages(ArrayList messages) {
    for (int i = 0; i < messages.size(); i++) {
      appendMessage((ChatMessage) messages.get(i));
    }
  }


  public static void main(String[] args)
  {
      String[] backChat = {
          "roger: take this", "mike: take that"};

     TalkFrame talkFrame = new TalkFrame(
          "dummy title",
          "h_brewski",
          null);

      talkFrame.show();
  }

  void close()
  {
    if (tcs == null)
      throw new Error("TalkFrame '" + description + "' close(): no TalkStrategy registered");

    try {
      tcs.leave();
    }
    catch (TalkConnectionException rce) {
      System.out.println("TalkFrame: unable to close");
    }

    hide();
    System.out.println("member '" + nickname + "' is leaving chat room '" + description + "'");
    talkWindows.remove(this);
    notifyOnClose(this);
  }

  void closeButton_actionPerformed(ActionEvent e)
  {
    close();
  }

  public static void exit()
  {
    Iterator it =  new ArrayList(talkWindows).iterator(); // close() modifies talkWindows
    while (it.hasNext())
    {
      ((TalkFrame)it.next()).close();
    }
    System.exit(0);
  }

  void exitButton_actionPerformed(ActionEvent e)
  {
    exit();
  }

  void newMessage_actionPerformed(ActionEvent e)
  {
    if (tcs == null)
      throw new Error("TalkFrame exit button: no TalkStrategy registered");

    try {
      tcs.send(new ChatMessage(chatKey, nickname + ": " + newMessage.getText()));
    }
    catch (TalkConnectionException rce) {
      System.out.println("TalkFrame: unable to send new chat line");
    }

    newMessage.setText("");
  }


}


class TalkFrame_closeButton_actionAdapter implements java.awt.event.ActionListener {
  TalkFrame adaptee;

  TalkFrame_closeButton_actionAdapter(TalkFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.closeButton_actionPerformed(e);
  }
}

class TalkFrame_exitButton_actionAdapter implements java.awt.event.ActionListener {
  TalkFrame adaptee;

  TalkFrame_exitButton_actionAdapter(TalkFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.exitButton_actionPerformed(e);
  }
}

class TalkFrame_newMessage_actionAdapter implements java.awt.event.ActionListener {
  TalkFrame adaptee;

  TalkFrame_newMessage_actionAdapter(TalkFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.newMessage_actionPerformed(e);
  }
}

class DummyTalkConnectionStrategy implements TalkConnectionStrategy
{
    public DummyTalkConnectionStrategy() throws TalkConnectionException
    {
        System.out.println("DummyTalkConnectionStrategy()");
    }

    public void getBackChat(TalkFrame talkFrame) throws TalkConnectionException
    {
      System.out.println("DummyTalkConnectionStrategy.getBackChat()");
    }

    public void send(ChatMessage message)
    {
      System.out.println("DummyTalkConnectionStrategy.send()");
    }

    public void leave() throws TalkConnectionException
    {
        System.out.println("DummyTalkConnectionStrategy.leave()");
    }


}


