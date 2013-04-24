package org.jboss.remoting.samples.chat.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.management.MBeanServer;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.RemoteInvocation;
import org.jboss.remoting.samples.chat.client.ChatInfo;
import org.jboss.remoting.samples.chat.client.ChatMember;
import org.jboss.remoting.samples.chat.client.ChatMessage;
import org.jboss.remoting.samples.chat.exceptions.ShuttingDownException;
import org.jboss.remoting.samples.chat.utility.ReadWriteArrayList;
import org.jboss.remoting.samples.chat.utility.ShutDownGate;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class ChatServer_Impl implements ServerInvocationHandler
{
   protected static final Logger log = Logger.getLogger(ChatServer_Impl.class);
   
   private ExtendedChatInfo extendedChatInfo;
   private Map callbackThreadMap = new HashMap();
   private ShutDownGate shutDownGate;
   private boolean shuttingDown = false;

   
   public ChatServer_Impl(ExtendedChatInfo eci, ShutDownGate sdg)
   {
      extendedChatInfo = eci;
      shutDownGate = sdg;
   }

   
   public Object invoke(InvocationRequest invocation) throws Throwable
   {
      if (!(invocation.getParameter() instanceof RemoteInvocation))
         throw new Exception("invalid request format: expecting NameBasedInvocation");
         
      RemoteInvocation request = (RemoteInvocation) invocation.getParameter();
      String methodName = request.getMethodName();
      Object[] args = request.getParameters();

      if (methodName.equals("getBackChat"))
         return getBackChat(args);
      
      if (methodName.equals("getChatInfo"))
         return getChatInfo(args);
      
      if (methodName.equals("leave"))
      {
         leave(args);
         return null;
      }
      
      if (methodName.equals("send"))
      {
         send(args);
         return null;
      }

      log.error("unrecognized method name: " + methodName);
      throw new Exception("unrecognized method name: " + methodName);
   }
   
   
   protected ArrayList getBackChat(Object[] args) throws ShuttingDownException
   {
      shutDownGate.enter();
      ReadWriteArrayList messages = extendedChatInfo.getMessages();

      if (messages == null)
      {
         System.out.println("messages == null");
         messages = new ReadWriteArrayList();
      }

      ArrayList returnMessages = messages.toArrayList();
      shutDownGate.leave();
      return returnMessages;
   }


   protected ChatInfo getChatInfo(Object[] args) throws ShuttingDownException
   {
      shutDownGate.check();
      ChatInfo chatInfo = extendedChatInfo.getChatInfo();
      chatInfo.set_currentMembers(extendedChatInfo.getMembers().size());
      chatInfo.set_size(extendedChatInfo.getMessages().size());
      return chatInfo;
   }

   
   protected void leave(Object[] args) throws ShuttingDownException
   {
      ChatMember member = (ChatMember) args[0];
      shutDownGate.enter();
      System.out.println("ChatServer.leave(): member leaving: " + member.get_name());
      extendedChatInfo.removeMember(member);
      shutDownGate.leave();
   }

   
   protected void send(Object[] args) throws ShuttingDownException
   {
      ChatMessage mesg = (ChatMessage) args[0];
      shutDownGate.enter();
      ReadWriteArrayList messages = extendedChatInfo.getMessages();
      messages.add(mesg);
      shutDownGate.leave();
   }   

   
   public void setMBeanServer(MBeanServer server)
   {
   }

   public void setInvoker(ServerInvoker invoker)
   {
   }

   public void addListener(InvokerCallbackHandler callbackHandler)
   {
      CallbackThread t = new CallbackThread(callbackHandler, shutDownGate, extendedChatInfo.getMessages());
      callbackThreadMap.put(callbackHandler, t);
      t.start();
   }

   public void removeListener(InvokerCallbackHandler callbackHandler)
   {
      CallbackThread t = (CallbackThread) callbackThreadMap.remove(callbackHandler);
      
      if (t != null)
         t.setMemberLeaving();
   }
}