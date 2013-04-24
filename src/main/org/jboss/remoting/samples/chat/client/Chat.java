package org.jboss.remoting.samples.chat.client;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.UIManager;

import org.jboss.remoting.samples.chat.utility.Debug;
import org.jboss.remoting.samples.chat.utility.Parameters;

/**
 * <p>Title: Chat</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author Ron Sigal
 * @version 1.0
 */

public class Chat extends CloseableFrame {
  boolean packFrame = false;

  //Construct the application
  public Chat(String[] args)
  {
  	for (int i=0; i< args.length; i++)
  	{
  		System.out.println(args[i]);
  	}
    Parameters.initParameters(args);

    String debug = Parameters.getParameter("debug");
    if (debug != null && debug.charAt(0) == 'y')
    {
      Debug.turnOn();
//      xmlrmi.utility.Debug.turnOn();
    }

    RemoteStrategy remoteStrategy = null;
    ChatFrame frame = null;

    String remoteStrategyName = remoteStrategyName = Parameters.getParameter("remoteStrategy");
    if (remoteStrategyName == null)
        remoteStrategyName = "chat.client.RemoteStrategyXmlRmi_Impl";

    try
    {
      remoteStrategy = (RemoteStrategy) Class.forName(remoteStrategyName).newInstance();
      System.out.println("chat: created remote strategy: " + remoteStrategyName);
    } catch (Exception e)
    {
      System.out.println("chat: unable to create RemoteStrategyImpl: " + remoteStrategyName);
      System.out.println(e.toString());
      System.exit(-1);
    }

      LocalStrategy localStrategy = new LocalStrategy(this, remoteStrategy);
      frame = new ChatFrame(localStrategy);
      frame.show();

    //Validate frames that have preset sizes
    //Pack frames that have useful preferred size info, e.g. from their layout
    if (packFrame) {
      frame.pack();
    }
    else {
      frame.validate();
    }
    //Center the window
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = frame.getSize();
    if (frameSize.height > screenSize.height) {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width > screenSize.width) {
      frameSize.width = screenSize.width;
    }
    frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    frame.setVisible(true);
  }

  //Main method
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    new Chat(args);
  }
}