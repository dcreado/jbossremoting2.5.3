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


/**
 * Demonstrates Callback acknowledgements.
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * <p/>
 * Copyright (c) 2006
 * </p>
 */
package org.jboss.remoting.samples.callback.acknowledgement;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import javax.management.MBeanServer;

import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.CallbackListener;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.util.id.GUID;


public class CallbackAcknowledgeServer
{
   public static final String APPLICATION_ACKNOWLDEGEMENTS = "applicationAcknowledgements";
   public static final String REMOTING_ACKNOWLDEGEMENTS = "remotingAcknowledgements";
   
   private static String transport = "socket";
   private static String host;
   private static int port = 5401;
   
   private Connector connector;

   /**
    * Can pass transport and port to be used as parameters.
    * Default to "socket" and 5401.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      if(args != null && args.length == 2)
      {
         transport = args[0];
         port = Integer.parseInt(args[1]);
      }
      try
      {
         host = InetAddress.getLocalHost().getHostName();
      }
      catch (UnknownHostException e1)
      {
         System.err.println("cannot get local host name");
         return;
      }
      String locatorURI = transport + "://" + host + ":" + port;
      CallbackAcknowledgeServer server = new CallbackAcknowledgeServer();
      try
      {
         server.setupServer(locatorURI);

         // wait forever, let the user kill us at any point (at which point, the client will detect we went down)
         while(true)
         {
            Thread.sleep(1000);
         }

      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
   
   public void setupServer(String locatorURI) throws Exception
   {
      connector = new Connector(new InvokerLocator(locatorURI));
      connector.start();
      connector.addInvocationHandler("test", new TestInvoocationHandler());
   }

   
   static class TestInvoocationHandler implements ServerInvocationHandler, CallbackListener
   {
      InvokerCallbackHandler callbackHandler;
      int counter;
      
      public void setMBeanServer(MBeanServer server) {}

      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         String command = (String) invocation.getParameter();
         System.out.println("command: " + command);
         
         Callback cb = new Callback("callback " + ++counter);
         
         // Register as listener and pass callback id.
         HashMap returnPayload = new HashMap();
         returnPayload.put(ServerInvokerCallbackHandler.CALLBACK_LISTENER, this);
         returnPayload.put(ServerInvokerCallbackHandler.CALLBACK_ID, new GUID());
         cb.setReturnPayload(returnPayload);
         
         if (REMOTING_ACKNOWLDEGEMENTS.equals(command))
         {
            returnPayload.put(ServerInvokerCallbackHandler.REMOTING_ACKNOWLEDGES_PUSH_CALLBACKS, "true");
         }

         callbackHandler.handleCallback(cb);         
         return null;
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         this.callbackHandler = callbackHandler;
      }

      public void removeListener(InvokerCallbackHandler callbackHandler) {}

      /**
       * callbackSent() is called to acknowledgement the sending of a callback.
       */
      public void acknowledgeCallback(InvokerCallbackHandler callbackHandler, Object callbackId, Object response)
      {
         System.out.println("received acknowledgment for callback: " + callbackId);
         System.out.println("response: " + response);
         System.out.println("");
      }
   }
}
