 package org.jboss.remoting.samples.chat.client;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.jboss.remoting.samples.chat.exceptions.ConnectionException;
import org.jboss.remoting.samples.chat.exceptions.ListConnectionException;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0 
 */

interface ListConnectionStrategy
{
    void getId(ChatInfo chatInfo) throws ConnectionException;
    void getInfo(ArrayList chatInfoList, int key) throws ListConnectionException;
}


public class ListFrame extends CloseableFrame {
  JButton joinButton = new JButton();
  JButton getInfoButton = new JButton();
  JButton closeButton = new JButton();
  JButton exitButton = new JButton();

  private ListConnectionStrategy lcs;
  private ArrayList chatRoomInfo;
  JScrollPane jScrollPane1 = new JScrollPane();
  JList chatRoomList = new JList();
  GridBagLayout gridBagLayout1 = new GridBagLayout();


  public ListFrame(
    ListConnectionStrategy lcs,
    ArrayList chatRoomInfo,
    Closeable parent)
  {
    super(parent);
    this.chatRoomInfo = chatRoomInfo;
    this.lcs = lcs;

    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }


  private void jbInit() throws Exception {
    this.getContentPane().setLayout(gridBagLayout1);
    joinButton.setFont(new java.awt.Font("SansSerif", 1, 12));
    joinButton.setMaximumSize(new Dimension(77, 25));
    joinButton.setMinimumSize(new Dimension(77, 25));
    joinButton.setText("Join");
    joinButton.addActionListener(new ListFrame_joinButton_actionAdapter(this));
    getInfoButton.setText("Get info");
    getInfoButton.addActionListener(new ListFrame_getInfoButton_actionAdapter(this));
    getInfoButton.setFont(new java.awt.Font("SansSerif", 1, 12));
    getInfoButton.setMinimumSize(new Dimension(77, 25));
    closeButton.setText("Close");
    closeButton.addActionListener(new ListFrame_closeButton_actionAdapter(this));
    closeButton.setFont(new java.awt.Font("SansSerif", 1, 12));
    closeButton.setMaximumSize(new Dimension(77, 25));
    closeButton.setMinimumSize(new Dimension(77, 25));
    closeButton.setFocusPainted(true);
    exitButton.setText("Exit");
    exitButton.addActionListener(new ListFrame_exitButton_actionAdapter(this));
    exitButton.setFont(new java.awt.Font("SansSerif", 1, 12));
    exitButton.setMaximumSize(new Dimension(77, 25));
    exitButton.setMinimumSize(new Dimension(77, 25));
    this.setTitle("Current chat rooms");

    chatRoomList = makeChatRoomList(chatRoomInfo);

    chatRoomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.getContentPane().add(exitButton,  new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(36, 7, 22, 11), 40, 0));
    this.getContentPane().add(closeButton,  new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(35, 21, 22, 0), 16, 0));
    this.getContentPane().add(joinButton,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(36, 9, 22, 0), 32, 0));
    this.getContentPane().add(getInfoButton,  new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(33, 6, 22, 0), 0, 4));
    this.getContentPane().add(jScrollPane1,  new GridBagConstraints(1, 0, 2, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(57, 21, 0, 12), -106, 27));
    jScrollPane1.getViewport().add(chatRoomList, null);

    pack();
    center();
  }

  protected JList makeChatRoomList(ArrayList chatRoomInfo)
  {
    String[] chats = new String[chatRoomInfo.size()];

    for (int i = 0; i < chatRoomInfo.size(); i++)
    {
        chats[i] = ((ChatInfo)chatRoomInfo.get(i)).get_description();
        if (chats[i].equals(""))  // empty string will not generate
          chats[i] = "*no name*"; // line on listbox
    }

    JList chatList = new JList(chats);
    chatList.setSelectedIndex(0);

    return chatList;
  }


  public static void main(String[] args) {
    try
    {
      ChatMember chatMember0 = new ChatMember();
      ChatMember chatMember1 = new ChatMember();
      ChatMember chatMember2 = new ChatMember();
      chatMember0.set_name("m1");
      chatMember1.set_name("m2");
      chatMember2.set_name("m3");
      ChatInfo chatInfo0 = new ChatInfo();
      ChatInfo chatInfo1 = new ChatInfo();
      ChatInfo chatInfo2 = new ChatInfo();
      chatInfo0.set_currentMembers(0);
      chatInfo0.set_description("Lord of the Rings");
      chatInfo0.set_owner(chatMember0);
      chatInfo1.set_currentMembers(1);
      chatInfo1.set_description("Cat's Cradle");
      chatInfo1.set_owner(chatMember1);
      chatInfo2.set_currentMembers(2);
      chatInfo2.set_description("Siddhartha");
      chatInfo2.set_owner(chatMember2);

      ArrayList chats = new ArrayList(3);
      chats.add(chatInfo0);
      chats.add(chatInfo1);
      chats.add(chatInfo2);

//          new ChatInfo(0, "Lord of the Rings", new ChatMember("m1"), new Date()),
//          new ChatInfo(1, "Cat's Cradle",      new ChatMember("m2"), new Date()),
//          new ChatInfo(2, "Siddhartha",        new ChatMember("m3"), new Date())};

      ListFrame frame = new ListFrame(
          new DummyListConnectionStrategy(),
          chats,
          null);

      frame.show();
    }
    catch (ListConnectionException lce) {
      System.out.println("ListFrameTest: unable to create chat frame");
    }

  }

  void joinButton_actionPerformed(ActionEvent e) {
    try
    {
      notifyOnClose(this);
      if (chatRoomList.getModel().getSize() <= chatRoomList.getSelectedIndex())
      {
        System.out.println("there are no chat rooms to join");
        return;
      }
      ChatInfo chatInfo = (ChatInfo)chatRoomInfo.get(chatRoomList.getSelectedIndex());
      lcs.getId(chatInfo);
    }
    catch (ConnectionException ce)
    {
        System.out.println("Join button catches exception:");
        System.out.println(ce);
    }
  }

  void getInfoButton_actionPerformed(ActionEvent e) {
    try
    {
      notifyOnClose(this);
      if (chatRoomList.getModel().getSize() <= chatRoomList.getSelectedIndex()) {
        System.out.println("there are no chat rooms");
        return;
      }

      lcs.getInfo(chatRoomInfo, chatRoomList.getSelectedIndex());
    }
    catch (ListConnectionException lce)
    {
        System.out.println("Info button catches exception:");
        System.out.println(lce);
    }
  }

  void closeButton_actionPerformed(ActionEvent e) {
    notifyOnClose(this);
  }

  void exitButton_actionPerformed(ActionEvent e) {
   System.exit(0);
  }

}

class DummyListConnectionStrategy implements ListConnectionStrategy
{
    public DummyListConnectionStrategy() throws ListConnectionException
    {
        System.out.println("DummyListConnectionStrategy()");
    }

    public void getId(ChatInfo chatInfo)
    {
        System.out.println("DummyListConnectionStrategy.getId(" + chatInfo.get_key() + ")");
    }

    public void getInfo(ArrayList chatInfoList, int key) throws ListConnectionException
    {
        System.out.println("DummyListConnectionStrategy.getInfo(" + key + ")");
    }
}

class ListFrame_joinButton_actionAdapter implements java.awt.event.ActionListener {
  ListFrame adaptee;

  ListFrame_joinButton_actionAdapter(ListFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.joinButton_actionPerformed(e);
  }
}

class ListFrame_getInfoButton_actionAdapter implements java.awt.event.ActionListener {
  ListFrame adaptee;

  ListFrame_getInfoButton_actionAdapter(ListFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.getInfoButton_actionPerformed(e);
  }
}

class ListFrame_closeButton_actionAdapter implements java.awt.event.ActionListener {
  ListFrame adaptee;

  ListFrame_closeButton_actionAdapter(ListFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.closeButton_actionPerformed(e);
  }
}

class ListFrame_exitButton_actionAdapter implements java.awt.event.ActionListener {
  ListFrame adaptee;

  ListFrame_exitButton_actionAdapter(ListFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.exitButton_actionPerformed(e);
  }
}
