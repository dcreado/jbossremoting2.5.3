package org.jboss.remoting.samples.chat.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;

import javax.management.MBeanServer;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.RemoteInvocation;
import org.jboss.remoting.samples.chat.client.ChatInfo;
import org.jboss.remoting.samples.chat.client.ChatMember;
import org.jboss.remoting.samples.chat.exceptions.InitializeException;
import org.jboss.remoting.samples.chat.exceptions.NameInUseException;
import org.jboss.remoting.samples.chat.exceptions.RemoteConnectionException;
import org.jboss.remoting.samples.chat.exceptions.ShuttingDownException;
import org.jboss.remoting.transport.Connector;

/**
 */

public class ChatManager implements ServerInvocationHandler
{
   protected static final Logger log = Logger.getLogger(ChatManager.class);
   private static final String chatServerLocator = "socket://localhost";
   
   private ChatStore chatStore;
   private final File chatStoreFile = new File("chatStore.ser");
   private boolean chatStoreSaved = false;
   private boolean shuttingDown = false;
   
   
   public ChatManager()
   {
      try
      {
         initialize();
      }
      catch (InitializeException ie)
      {
         log.error("ChatManager_Impl: cannot initialize: " + ie);
         System.exit(1);
      }
   }


   protected void initialize() throws InitializeException
   {
      
      if (chatStoreFile.exists())
      {
         try
         {
            log.info("ChatManager_Impl: reading existing ChatStore");
            FileInputStream file = new FileInputStream(chatStoreFile);
            ObjectInputStream input = new ObjectInputStream(file);
            chatStore = (ChatStore) input.readObject();
            chatStore.getShutDownGate().reset();
            log.info("ChatManager_Impl: read existing ChatStore");
         }
         catch (java.io.IOException ioe) {
            log.error("ChatManager_Impl: i/o error reading chatStore: " + ioe);
            System.exit(1);
         }
         catch (java.lang.ClassNotFoundException cnfe) {
            log.error("ChatManager_Impl: ChatStore class not found: " + cnfe);
            System.exit(2);
         }
      }
      else
      {
         chatStore = new ChatStore();
         log.info("ChatManager_Impl: created new ChatStore");
      }

      //       Runtime.getRuntime().addShutdownHook(
      //        new Thread() {
      //         public void run() {
      //           System.out.println("ShutDownHook: shutting down");
      ////           new ShutDownDialog().show();
      //           shutdown();
      //         }
      //       });

   }

   public void shutdown()
   {
      log.info("shutdown(): shutting down");

      if (chatStoreSaved)
      {
         log.info("shutdown(): chatStore already saved");
         return;
      }

      chatStore.getShutDownGate().shutDown();

      ObjectOutputStream out = null;
      try
      {
         out = new ObjectOutputStream(new FileOutputStream(chatStoreFile));
         out.writeObject(chatStore);
         out.flush();
      }
      catch (java.io.IOException ioe)
      {
         log.error("ChatManager_Impl: i/o error writing chatStore" + ioe);
      }
      finally
      {
         try
         {
            out.close();
         }
         catch (java.io.IOException ioe)
         {
            log.error("ChatManager_Impl: i/o error closing chatStore" + ioe);
         }
      }

      chatStoreSaved = true;
      log.info("shutdown(): shut down");
   }

   
   public Object invoke(InvocationRequest invocation) throws Throwable
   {
      if (!(invocation.getParameter() instanceof RemoteInvocation))
         throw new Exception("invalid request format: expecting RemoteInvocation");
         
      RemoteInvocation request = (RemoteInvocation) invocation.getParameter();
      String methodName = request.getMethodName();
      Object[] args = request.getParameters();

      if (methodName.equals("createChat"))
         return createChat(args);
      
      else if (methodName.equals("join"))
      {
         join(args);
         return null;
      }
      
      else if (methodName.equals("leave"))
      {
         leave(args);
         return null;
      }
      
      else if (methodName.equals("list"))
         return list(args);
      
      else
         throw new Exception("unrecognized method name: " + methodName);
   }
   
   
   protected ArrayList list(Object[] args) throws RemoteConnectionException, ShuttingDownException
   {
      return chatStore.listChats();
   }

   
   protected InvokerLocator createChat(Object[] args) throws Exception
   {
      String description = (String) args[0];
      ChatMember owner = (ChatMember) args[1];
      
      ChatInfo chatInfo = new ChatInfo();
      ExtendedChatInfo extendedChatInfo = new ExtendedChatInfo(chatInfo);
      
      Connector connector = new Connector();
      connector.setInvokerLocator(chatServerLocator);
      connector.create();
      connector.addInvocationHandler("chatServer", new ChatServer_Impl(extendedChatInfo, chatStore.getShutDownGate()));
      connector.start();
      
      InvokerLocator chatLocator = connector.getLocator();
      String key = chatLocator.getLocatorURI();
      chatInfo.set_key(key);
      chatInfo.set_description(description);
      chatInfo.set_owner(owner);
      chatInfo.set_origin(new Date());
      extendedChatInfo.addMember(owner);
      chatStore.addChat(extendedChatInfo);
      return chatLocator;
   }

   
   protected void join(Object[] args)
   throws NameInUseException, ShuttingDownException
   {
      String key = (String) args[0];
      ChatMember newMember = (ChatMember) args[1];
      ExtendedChatInfo eci = chatStore.getChat(key);
      eci.addMember(newMember);
   }

   
   protected void leave(Object[] args) throws ShuttingDownException
   {
      String key = (String) args[0];
      ChatMember member = (ChatMember) args[1];
      ExtendedChatInfo eci = chatStore.getChat(key);
      eci.getMembers().remove(member);
   }


   public void setMBeanServer(MBeanServer server)
   {
   }

   public void setInvoker(ServerInvoker invoker)
   {
   }

   public void addListener(InvokerCallbackHandler callbackHandler)
   {
   }

   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
   }
}