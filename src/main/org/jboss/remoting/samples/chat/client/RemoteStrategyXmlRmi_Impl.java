//package org.jboss.remoting.samples.chat.client;
//
//import java.rmi.RemoteException;
//import java.util.*;
//import java.net.*;
//import org.apache.xmlrpc.*;
//import org.jboss.remoting.samples.chat.exceptions.NameInUseException;
//import org.jboss.remoting.samples.chat.exceptions.RemoteConnectionException;
//import org.jboss.remoting.samples.chat.exceptions.ShuttingDownException;
//import org.jboss.remoting.samples.chat.utility.ReadWriteArrayList;
//
//import xmlrmi.*;
//import xmlrmi.exceptions.*;
//import xmlrmi.xmlrpc.XmlrmiNativeClient;
//import chat.server.ChatManager;
//import chat.server.ChatServer;
//import chat.server.ejb.*;
//import chat.utility.*;
//import chat.utility.Parameters;
//import chat.exceptions.*;
//
///**
// * <p>Title: Chat</p*>
// * <p>Description: </p>
// * <p>Copyright: Copyright (c) 2003</p>
// * <p>Company: </p>
// * @author not attributable
// * @version 1.0
// */
//
//public class RemoteStrategyXmlRmi_Impl implements RemoteStrategy {
//
//  private ChatManager chatManager;
//  private XmlrmiClient xmlrmiClient;
//  private ChatServer chatServer;
//  private ArrayList messages;
//  private ChatInfo chatInfo;
//  private int key;
//  private ChatMember member;
//  private boolean locallyShuttingDown = false;
//  private boolean remotelyShuttingDown = false;
//  private WebServer webServer;
//
//  public RemoteStrategyXmlRmi_Impl() throws RemoteConnectionException
//  {
//    System.out.println("RemoteStrategy: version RemoteStrategy_XmlRmi_Impl");
//
//    messages = new ArrayList();
//    key = -1;
//
//    String clientUriString = Parameters.getParameter("clientUri");
//    if (clientUriString == null)
//      clientUriString = "http://localhost:1970";
//
//    URI clientUri = null;
//    try {
//      clientUri = new URI(clientUriString);
//    } catch (URISyntaxException e)
//    {
//      System.out.println("RemoteStrategyXmlRmi_Impl(): invalid client uri: " + clientUri);
//      System.out.println(e);
//      System.exit(-1);
//    }
//
//    System.out.println("RemoteStrategy: client uri = " + clientUri);
//    try {
//      xmlrmiClient = new XmlrmiNativeClient(XmlrmiNativeClient.STANDALONE, clientUri);
//    } catch (XmlrmiException xre)
//    {
//      System.out.println("unable to create XmlrmiClient");
//      System.out.println(xre);
//      throw new RemoteConnectionException();
//    }
//
//    String chatManagerUriString = Parameters.getParameter("chatManagerUri");
//    if (chatManagerUriString == null)
//      chatManagerUriString="http://localhost:1969/chat/chat/chatManager";
//
//    URI chatManagerUri = null;
//    try {
//      chatManagerUri = new URI(chatManagerUriString);
//    } catch (URISyntaxException e)
//    {
//      System.out.println("RemoteStrategyXmlRmi_Impl(): invalid chatManagerUri property: " + chatManagerUriString);
//      System.out.println(e);
//      System.exit(-1);
//    }
//
//    System.out.println("RemoteStrategy: chat manager uri = " + chatManagerUri);
//
//    try {
//      Object o = xmlrmiClient.lookup(chatManagerUri);
//      chatManager = (ChatManager) o;
//    } catch (Exception e)
//    {
//      System.out.println("RemoteStrategy: unable to connect to chat manager at: " + chatManagerUri);
//      throw new RemoteConnectionException();
//    }
//
//    System.out.println("RemoteStrategy: chatManager found");
//  }
//
//
//  public static void main(String[] args)
//  {
//    try
//    {
//      RemoteStrategyXmlRmi_Impl remoteStrategyImplXmlRmi = new RemoteStrategyXmlRmi_Impl();
//
//      ArrayList chats = null;
//      try {
//        chats = remoteStrategyImplXmlRmi.list();
//
//        for (int i = 0; i < chats.size(); i++)
//          System.out.println(chats.get(i));
//
//      } catch (Exception e) {
//        System.out.println("RemoteStrategy: cannot list chats");
//        System.out.println(e);
//      }
//    } catch (RemoteConnectionException rce)
//    {
//      System.out.println("RemoteStrategy: uable to create remoteStrategyImpl: " + rce);
//    }
//  }
//
//  public void setShuttingDown()
//  {
//    locallyShuttingDown = true;
//  }
//
//  public ArrayList list() throws RemoteConnectionException, ShuttingDownException
//  {
//      ArrayList chats;
//
//      try {
//        chats = chatManager.list();
//      } catch (RemoteConnectionException re)
//      {
//        System.out.println("RemoteStrategyImpl: unable to get list of chat rooms");
//        System.out.println(re.toString());
//        throw new RemoteConnectionException();
//      }
//
//      return chats;
//  }
//
//  public ChatServer createChat(String description,
//                                     ChatMember owner,
//                                     TalkFrame talkFrame,
//                                     final ReadWriteArrayList outgoingLines)
//      throws NameInUseException,RemoteConnectionException, ShuttingDownException
//  {
//    ChatReceiver chatReceiver = new ChatReceiver_Impl(talkFrame);
//
//    final ChatServer chatServer;
//	chatServer = chatManager.createChat(description, owner, chatReceiver);
//   
//   Thread sendThread = new Thread() {
//    public void run() {
//        while (!locallyShuttingDown && !remotelyShuttingDown)
//        {
//          try {
//            chatServer.send( (ChatMessage) outgoingLines.firstElement());
//            outgoingLines.remove(0);
//          } catch (RemoteConnectionException re)
//          {
//            System.out.println("RemoteStrategyImpl.createChat(): unable to send next line:");
//            System.out.println("  " + (String) outgoingLines.firstElement());
//          } catch (ShuttingDownException sde) {
//            System.out.println("RemoteStrategyImpl.createChat(): ChatServer is shutting down");
//            remotelyShuttingDown = true;
//          }
//        }
//      }
//    };
//
//    sendThread.start();
//    return chatServer;
////	return null; // dummy statement - should never reach here
//  }
//
//
//  public ChatServer join(int key,
//                               ChatMember newMember,
//                               TalkFrame talkFrame,
//                               final ReadWriteArrayList outgoingLines)
//      throws NameInUseException, RemoteConnectionException, ShuttingDownException
//  {
//      ChatReceiver chatReceiver = new ChatReceiver_Impl(talkFrame);
//
//      final ChatServer chatServer;
//
//      chatServer = chatManager.join(new Integer(key), newMember, chatReceiver);
//
//      Thread sendThread = new Thread() {
//        public void run() {
//          while (!locallyShuttingDown && !remotelyShuttingDown) {
//            try {
//              chatServer.send( (ChatMessage) outgoingLines.remove(0));
//            }
//            catch (RemoteConnectionException re) {
//              System.out.println("RemoteStrategyImpl.createChat(): unable to send next line:");
//              System.out.println("  " + (String) outgoingLines.firstElement());
//            }           
//            catch (ShuttingDownException sde) {
//              System.out.println("RemoteStrategyImpl.createChat(): ChatServer is shutting down");
//              remotelyShuttingDown = true;
//            }
////            catch (InterruptedException ie) {
////              System.out.println("RemoteStrategyXmlRmi_Impl.createChat(): unexpected InterruptedException");
////            }
//          }
//        }
//      };
//
//      sendThread.start();
//
//      return chatServer;
//  }
//
///*  public void leave() throws RemoteConnectionException, ShuttingDownException
//  {
//      if (chatServer == null) {
//        throw new RemoteConnectionException();
//      }
//
//      try
//      {
//        chatManager.leave(key, member);
//      }
//      catch (RemoteConnectionException re)
//      {
//        System.out.println("createChat(): unable to leave chatroom");
//        System.out.println(re.toString());
//        throw new RemoteConnectionException();
//      }
//      finally
//      {
//        chatServer = null;
//        key = -1;
//      }
//  }
//*/
//}
