/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/

/*
 * Created on Feb 21, 2006
 */
package org.jboss.remoting.samples.chat.client;

import java.net.MalformedURLException;
import java.util.ArrayList;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.RemoteInvocation;
import org.jboss.remoting.samples.chat.exceptions.NameInUseException;
import org.jboss.remoting.samples.chat.exceptions.RemoteConnectionException;
import org.jboss.remoting.samples.chat.exceptions.ShuttingDownException;
import org.jboss.remoting.samples.chat.server.ChatServer;
import org.jboss.remoting.samples.chat.utility.Parameters;
import org.jboss.remoting.samples.chat.utility.ReadWriteArrayList;
import org.jboss.remoting.transport.Connector;


/**
 * A RemoteStrategyRemoting.

 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 735 $
 * <p>
 * Copyright (c) 2005
 * </p>
 */

public class RemoteStrategyRemoting implements RemoteStrategy
{
   protected static final Logger log = Logger.getLogger(RemoteStrategyRemoting.class);
   private static String chatClientLocatorDefault = "socket://localhost:0";
   private static String chatManagerLocatorDefault = "socket://localhost:1969";
   
   private Client managerClient;
   private Connector receiverConnector;
   private boolean locallyShuttingDown = false;
   private boolean remotelyShuttingDown = false;
   
   
   public RemoteStrategyRemoting() throws Exception
   {
      String chatManagerUriString = Parameters.getParameter("chatManagerUri");
      
      if (chatManagerUriString == null)
         chatManagerUriString = chatManagerLocatorDefault;
 
      InvokerLocator chatManagerLocator = new InvokerLocator(chatManagerUriString);
      managerClient = new Client(chatManagerLocator);
      managerClient.connect();
   }

   
   public ArrayList list() throws RemoteConnectionException, ShuttingDownException
   {
      RemoteInvocation invocation = new RemoteInvocation("list", null);
      
      try
      {
         return (ArrayList) managerClient.invoke(invocation);
      }
      catch (Throwable t)
      {
         log.error(t);
         t.printStackTrace();
         throw new RemoteConnectionException();
      }
   }


   public ChatServer createChat(String description,
                                ChatMember owner,
                                TalkFrame talkFrame,
                                ReadWriteArrayList outgoingLines)
   throws NameInUseException, RemoteConnectionException, ShuttingDownException
   {
      Client serverClient = null;
      
      // Create new chat room and create Client for new chat room server.
      try
      {
         Object[] args = new Object[] {description, owner};
         RemoteInvocation invocation = new RemoteInvocation("createChat", args);
         InvokerLocator serverLocator = (InvokerLocator) managerClient.invoke(invocation);
         serverClient = new Client(serverLocator);
         serverClient.connect();
      }
      catch (Throwable e)
      {
         log.error(e);
         e.printStackTrace();
         throw new RemoteConnectionException();
      }
         
      // Create callback handler to process incoming messages.
      try
      {
         receiverConnector = new Connector();
         String receiverLocatorString = Parameters.getParameter("clientUriString", chatClientLocatorDefault);
         InvokerLocator receiverLocator = new InvokerLocator(receiverLocatorString);
         receiverConnector.setInvokerLocator(receiverLocator.getLocatorURI());
         log.info(receiverLocator.getLocatorURI());
         receiverConnector.start();
         receiverLocator = receiverConnector.getLocator();
         log.info(receiverConnector.getInvokerLocator());
         InvokerCallbackHandler receiverHandler = new ChatReceiverHandler(talkFrame);
         serverClient.addListener(receiverHandler, receiverLocator, owner);
      }
      catch (MalformedURLException e)
      {
         log.error(e);
         e.printStackTrace();
         throw new RemoteConnectionException();
      }
      catch (Throwable e)
      {
         log.error(e);
         e.printStackTrace();
         throw new RemoteConnectionException();
      }
      
      // Create stub for new chat room server.
      ChatServer chatServer = new ChatServerStub(serverClient);
      
      // Create thread to send new outgoing messages to chat room server.
      SendThread sendThread = new SendThread(chatServer, outgoingLines);
      sendThread.start();
      
      return chatServer;
   }
   
   
   public ChatServer join(String key, ChatMember newMember, TalkFrame talkFrame, ReadWriteArrayList outgoingLines)
         throws NameInUseException, RemoteConnectionException, ShuttingDownException
   {
      Client serverClient = null;
      
      // Join chat room and create Client for chat room server.
      try
      {
         Object[] args = new Object[] {key, newMember};
         RemoteInvocation invocation = new RemoteInvocation("join", args);
         managerClient.invoke(invocation);
         InvokerLocator serverLocator = new InvokerLocator(key);
         serverClient = new Client(serverLocator);
         serverClient.connect();
      }
      catch (Throwable e)
      {
         log.error(e);
         e.printStackTrace();
         throw new RemoteConnectionException();
      }
         
      // Create callback handler to process incoming messages.
      try
      {
         receiverConnector = new Connector();
         String receiverLocatorString = Parameters.getParameter("clientUriString", chatClientLocatorDefault);
         InvokerLocator receiverLocator = new InvokerLocator(receiverLocatorString);
         receiverConnector.setInvokerLocator(receiverLocator.getLocatorURI());
         log.info(receiverLocator.getLocatorURI());
         receiverConnector.start();
         receiverLocator = receiverConnector.getLocator();
         InvokerCallbackHandler receiverHandler = new ChatReceiverHandler(talkFrame);
         serverClient.addListener(receiverHandler, receiverLocator, newMember);
      }
      catch (MalformedURLException e)
      {
         log.error(e);
         e.printStackTrace();
         throw new RemoteConnectionException();
      }
      catch (Throwable e)
      {
         log.error(e);
         e.printStackTrace();
         throw new RemoteConnectionException();
      }
      
      
      // Create stub for new chat room server.
      ChatServer chatServer = new ChatServerStub(serverClient);
      
      // Create thread to send new outgoing messages to chat room server.
      SendThread sendThread = new SendThread(chatServer, outgoingLines);
      sendThread.start();
  
      return chatServer;
   }


   public void setShuttingDown()
   {
      locallyShuttingDown = true;
   }
   
   
   class SendThread extends Thread
   {
      private ChatServer chatServer;
      private ReadWriteArrayList outgoingLines;
      
      SendThread(ChatServer chatServer, ReadWriteArrayList outgoingLines)
      {
         this.chatServer = chatServer;
         this.outgoingLines = outgoingLines;
      }
      
      public void run() {
         while (!locallyShuttingDown && !remotelyShuttingDown)
         {
           try {
             chatServer.send( (ChatMessage) outgoingLines.firstElement());
             outgoingLines.remove(0);
           } catch (RemoteConnectionException re)
           {
             System.out.println("RemoteStrategyImpl.createChat(): unable to send next line:");
//             System.out.println("  " + (String) outgoingLines.firstElement());
           } catch (ShuttingDownException sde) {
             System.out.println("RemoteStrategyImpl.createChat(): ChatServer is shutting down");
             remotelyShuttingDown = true;
           }
         }
       }
     }
   
   
   class ChatServerStub implements ChatServer
   {
      private Client serverClient;
    
      public ChatServerStub(Client serverClient)
      {
         this.serverClient = serverClient;
      }
     
      public ArrayList getBackChat() throws RemoteConnectionException, ShuttingDownException
      {
         RemoteInvocation invocation = new RemoteInvocation("getBackChat", null);
         
         try
         {
            return (ArrayList) serverClient.invoke(invocation);
         }
         catch (Throwable e)
         {
            log.error(e);
            throw new RemoteConnectionException();
         }
      }
      
      public ChatInfo getChatInfo() throws RemoteConnectionException, ShuttingDownException
      {
         RemoteInvocation invocation = new RemoteInvocation("getChatInfo", null);
         
         try
         {
            return (ChatInfo) serverClient.invoke(invocation);
         }
         catch (Throwable e)
         {
            log.error(e);
            throw new RemoteConnectionException();
         }
      }
      
      public void leave(ChatMember member) throws RemoteConnectionException, ShuttingDownException
      {
         RemoteInvocation invocation = new RemoteInvocation("leave", new Object[] {member});
         
         try
         {
            serverClient.invoke(invocation);
         }
         catch (Throwable e)
         {
            log.error(e);
            throw new RemoteConnectionException();
         }
      }
      
      public void send(ChatMessage mesg) throws RemoteConnectionException, ShuttingDownException
      {
         RemoteInvocation invocation = new RemoteInvocation("send", new Object[] {mesg});
         
         try
         {
            serverClient.invoke(invocation);
         }
         catch (Throwable e)
         {
            log.error(e);
            throw new RemoteConnectionException();
         }
      }
   }
}