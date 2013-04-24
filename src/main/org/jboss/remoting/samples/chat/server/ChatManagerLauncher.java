package org.jboss.remoting.samples.chat.server;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;

import javax.swing.JButton;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.samples.chat.client.CloseableFrame;
import org.jboss.remoting.samples.chat.utility.Parameters;
import org.jboss.remoting.transport.Connector;

/**
 * <p>Title: Chat</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class ChatManagerLauncher extends CloseableFrame
{
   protected static final Logger log = Logger.getLogger(ChatManagerLauncher.class);
   private static final int PORT = 1969;
   
   private ChatManager chatManager;
   private InvokerLocator managerLocator;
   private Connector managerConnector;
   
   private JButton launchButton = new JButton();
   private JButton shutDownButton = new JButton();
   private JButton exitButton = new JButton();
   private GridBagLayout gridBagLayout1 = new GridBagLayout();

   public ChatManagerLauncher()
   {
      try
      {
         jbInit();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   public static void main(String[] args)
   {
      Parameters.initParameters(args);
      ChatManagerLauncher chatManagerLauncher = new ChatManagerLauncher();
   }

   private void jbInit() throws Exception
   {
      launchButton.setFont(new java.awt.Font("SansSerif", 1, 12));
      launchButton.setActionCommand("startButton");
      launchButton.setText("Launch");
      launchButton.addActionListener(new ChatManagerLauncher_launchButton_actionAdapter(this));
      this.getContentPane().setLayout(gridBagLayout1);
      shutDownButton.setFont(new java.awt.Font("SansSerif", 1, 12));
      shutDownButton.setText("Shut down");
      shutDownButton.addActionListener(new ChatManagerLauncher_shutDownButton_actionAdapter(this));
      exitButton.setFont(new java.awt.Font("SansSerif", 1, 12));
      exitButton.setText("Exit");
      exitButton.addActionListener(new ChatManagerLauncher_exitButton_actionAdapter(this));
      this.getContentPane().add(
            launchButton,
            new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                  new Insets(188, 102, 0, 0), 0, 0));
      this.getContentPane().add(
            shutDownButton,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                  new Insets(188, 23, 0, 103), 0, 0));
      this.getContentPane().add(
            exitButton,
            new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                  new Insets(23, 154, 39, 173), 0, 0));

      center();
      pack();
      show();
   }

   void launchButton_actionPerformed(ActionEvent ae)
   {
      String managerUriString = Parameters.getParameter("chatManagerUri");

      try
      {
         managerLocator = new InvokerLocator(managerUriString);
         log.info("ChatManagerLauncher: manager uri = " + managerUriString);
      }
      catch (MalformedURLException e)
      {
         log.error("ChatManagerLauncher(): invalid locator uri: " + managerUriString);
         e.printStackTrace();
         System.exit(-1);
      }
      
      managerConnector = new Connector();
      
      try
      {
         managerConnector.setInvokerLocator(managerLocator.getLocatorURI());
         managerConnector.create();
         chatManager = new ChatManager();
         managerConnector.addInvocationHandler("chatManager", chatManager);
         managerConnector.start();
      }
      catch (Exception e)
      {
         log.error("Unable to start Connector for chat manager: " + e.getMessage());
         e.printStackTrace();
      }
      
      log.info("ChatManagerLauncher: created chat manager");
   }

   void shutDownButton_actionPerformed(ActionEvent ae)
   {
      if (chatManager == null)
      {
         System.out.println("ChatManagerLauncher: chat manager has not been launched");
         return;
      }

      try
      {
         chatManager.shutdown();
         System.out.println("ChatManagerLauncher: shut down chatManager");
      }
      catch (Exception e)
      {
         System.out.println("ChatManagerLauncher: unable to shut down");
         return;
      }

      //    xmlrmiServer.getWebServer().shutdown();
      System.out.println("ChatManagerLauncher: shut down web server");
   }

   void exitButton_actionPerformed(ActionEvent e)
   {
      System.exit(0);
   }

}

class ChatManagerLauncher_launchButton_actionAdapter implements java.awt.event.ActionListener
{
   ChatManagerLauncher adaptee;

   ChatManagerLauncher_launchButton_actionAdapter(ChatManagerLauncher adaptee)
   {
      this.adaptee = adaptee;
   }

   public void actionPerformed(ActionEvent e)
   {
      adaptee.launchButton_actionPerformed(e);
   }
}

class ChatManagerLauncher_shutDownButton_actionAdapter implements java.awt.event.ActionListener
{
   ChatManagerLauncher adaptee;

   ChatManagerLauncher_shutDownButton_actionAdapter(ChatManagerLauncher adaptee)
   {
      this.adaptee = adaptee;
   }

   public void actionPerformed(ActionEvent e)
   {
      adaptee.shutDownButton_actionPerformed(e);
   }
}

class ChatManagerLauncher_exitButton_actionAdapter implements java.awt.event.ActionListener
{
   ChatManagerLauncher adaptee;

   ChatManagerLauncher_exitButton_actionAdapter(ChatManagerLauncher adaptee)
   {
      this.adaptee = adaptee;
   }

   public void actionPerformed(ActionEvent e)
   {
      adaptee.exitButton_actionPerformed(e);
   }
}