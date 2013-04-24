package org.jboss.remoting.samples.chat.server;

/**
 * <p>Title: Chat4</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

import java.util.ArrayList;

import org.jboss.logging.Logger;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.RemoteInvocation;
import org.jboss.remoting.samples.chat.utility.ReadWriteArrayList;
import org.jboss.remoting.samples.chat.utility.ShutDownGate;

public class CallbackThread extends Thread
{
   protected static final Logger log = Logger.getLogger(CallbackThread.class);
   
   private InvokerCallbackHandler callbackHandler;
   private ShutDownGate shutDownGate;
   private boolean memberLeaving;
   private ReadWriteArrayList messages;
   private int backChatSize;

   
   public CallbackThread(InvokerCallbackHandler callbackHandler, ShutDownGate shutDownGate,
         ReadWriteArrayList messages)
   {
      this.callbackHandler = callbackHandler;
      this.memberLeaving = false;
      this.shutDownGate = shutDownGate;
      this.messages = messages;

      backChatSize = messages.size();
      ArrayList backChat = new ArrayList(backChatSize);
      backChat = messages.copy();
      RemoteInvocation invocation = new RemoteInvocation("sendMultiple", new Object[] {backChat});
      Callback callback = new Callback(invocation);

      try
      {
         callbackHandler.handleCallback(callback);
      }
      catch (HandleCallbackException e)
      {
         log.error(e);
      }
   }

   public void setMemberLeaving()
   {
      memberLeaving = true;
   }
   
   public void run()
   {
      int i = backChatSize;
      
      while (!shutDownGate.isShuttingDown() && !memberLeaving)
      {
         RemoteInvocation invocation = new RemoteInvocation("send", new Object[] {messages.get(i)});
         Callback callback = new Callback(invocation);
         
         try
         {
            callbackHandler.handleCallback(callback);
         }
         catch (HandleCallbackException e)
         {
            log.error(e);
         }
         
         i++;
      }
   }
}