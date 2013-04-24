package org.jboss.remoting.samples.chat.client;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jboss.remoting.samples.chat.exceptions.ConnectionException;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class ChatFrame extends CloseableFrame {

  public static void main(String[] args)
  {
      try
      {
          ChatFrame frame = new ChatFrame(new DummyConnectionStrategy());
          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          frame.show();
      }
      catch (ConnectionException ce)
      {
          System.out.println("unable to create chat frame");
      }
  }

  JPanel contentPane;
  JLabel statusBar = new JLabel();
  JLabel LeChat = new JLabel();
  JButton ListButton = new JButton();
  JButton CreateButton = new JButton();
  JButton ExitButton = new JButton();
  GridBagLayout gridBagLayout1 = new GridBagLayout();

  private ConnectionStrategy cs;

  //Construct the frame
  public ChatFrame(ConnectionStrategy cs) {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    this.cs = cs;
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }
  //Component initialization
  private void jbInit() throws Exception  {
    contentPane = (JPanel) this.getContentPane();
    contentPane.setLayout(gridBagLayout1);
    this.setSize(new Dimension(400, 300));
    this.setTitle("Le Chat");
    statusBar.setText(" ");
    LeChat.setFont(new java.awt.Font("Serif", 1, 36));
    LeChat.setHorizontalAlignment(SwingConstants.CENTER);
    LeChat.setHorizontalTextPosition(SwingConstants.CENTER);
    LeChat.setText("Le Chat");
    ListButton.setFont(new java.awt.Font("SansSerif", 1, 14));
    ListButton.setText("List");
    ListButton.addActionListener(new ChatFrame_ListButton_actionAdapter(this));
    CreateButton.setFont(new java.awt.Font("SansSerif", 1, 14));
    CreateButton.setText("Create");
    CreateButton.addActionListener(new ChatFrame_CreateButton_actionAdapter(this));
    ExitButton.setFont(new java.awt.Font("SansSerif", 1, 14));
    ExitButton.setText("Exit");
    ExitButton.addActionListener(new ChatFrame_ExitButton_actionAdapter(this));

    contentPane.add(statusBar,  new GridBagConstraints(0, 2, 3, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(80, 0, 0, 0), 397, 0));
    contentPane.add(LeChat,  new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(106, 80, 0, 85), 111, -10));
    contentPane.add(ListButton,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(38, 57, 0, 0), 14, -4));
    contentPane.add(ExitButton,  new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(36, 24, 0, 64), 12, -4));
    contentPane.add(CreateButton,  new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(38, 24, 0, 0), 4, -4));

    center();
    pack();
  }

  //Overridden so we can exit when window is closed
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      System.exit(0);
    }
  }

  void ListButton_actionPerformed(ActionEvent e) {
    try
    {
      cs.list();
    }
    catch (ConnectionException ce)
    {
      System.out.println("list button catches exception:");
      System.out.println(ce);
    }
  }

  void CreateButton_actionPerformed(ActionEvent e) {
    try
    {
      cs.create();
    }
    catch (ConnectionException ce)
    {
      System.out.println("create button catches exception:");
      System.out.println(ce);
    }
  }

  void ExitButton_actionPerformed(ActionEvent e) {
    TalkFrame.exit();
  }
}


class ChatFrame_ListButton_actionAdapter implements java.awt.event.ActionListener {
  ChatFrame adaptee;

  ChatFrame_ListButton_actionAdapter(ChatFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.ListButton_actionPerformed(e);
  }
}

class ChatFrame_CreateButton_actionAdapter implements java.awt.event.ActionListener {
  ChatFrame adaptee;

  ChatFrame_CreateButton_actionAdapter(ChatFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.CreateButton_actionPerformed(e);
  }
}

class ChatFrame_ExitButton_actionAdapter implements java.awt.event.ActionListener {
  ChatFrame adaptee;

  ChatFrame_ExitButton_actionAdapter(ChatFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.ExitButton_actionPerformed(e);
  }
}

class DummyConnectionStrategy implements ConnectionStrategy
{
    public DummyConnectionStrategy() throws ConnectionException
    {
        System.out.println("DummyConnectionStrategy()");
    }

    public void list() throws ConnectionException
    {
        System.out.println("DummyConnectionStrategy.list()");
    }

    public void create() throws ConnectionException
    {
        System.out.println("DummyConnectionStrategy.create()");
    }
}
