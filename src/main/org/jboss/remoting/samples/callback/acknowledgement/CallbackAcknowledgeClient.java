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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.CallbackPoller;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;


public class CallbackAcknowledgeClient
{  
   private static String transport = "socket";
   private static String host;
   private static int port = 5401;
   
   private Client client;

   
   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
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
      CallbackAcknowledgeClient acknowledgeClient = new CallbackAcknowledgeClient();
      try
      {
         acknowledgeClient.createRemotingClient(locatorURI);

         acknowledgeClient.testPullCallbackAcknowledgements();
         acknowledgeClient.testPolledCallbackApplicationAcknowledgements();
         acknowledgeClient.testPolledCallbackRemotingAcknowledgements();
         acknowledgeClient.testPushCallbackApplicationAcknowledgements();
         acknowledgeClient.testPushCallbackRemotingAcknowledgements();
         
         acknowledgeClient.disconnectRemotingClient();
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
      System.out.println("done.");
   }
   
   
   public void createRemotingClient(String locatorURI) throws Exception
   {
      client = new Client(new InvokerLocator(locatorURI));
      client.connect();
   }
   
   
   public void disconnectRemotingClient()
   {
      if (client != null)
         client.disconnect();
   }
   
   
   /**
    * In this test, the connection is configured for pull callbacks, and
    * acknowledgements are made by an explicit call to Client.acknowledgeCallback()
    * after the callbacks have been received.
    */
   public void testPullCallbackAcknowledgements()
   {
      try
      {
         // Register callback handler.
         InvokerCallbackHandler callbackHandler = new NonAcknowledgingCallbackHandler();
         client.addListener(callbackHandler);
         
         // Request callbacks from server.
         client.invoke(CallbackAcknowledgeServer.APPLICATION_ACKNOWLDEGEMENTS);
         
         // Get callbacks.
         List callbacks = client.getCallbacks(callbackHandler);
         
         // Create responses.
         ArrayList responses = new ArrayList(callbacks.size());
         Iterator it = callbacks.iterator();
         while (it.hasNext())
         {
            Callback callback = (Callback) it.next();
            System.out.println("received pull callback: " + callback.getParameter());
            responses.add(callback.getParameter() + ": acknowledged");
         }
         
         // Acknowledge callbacks.
         client.acknowledgeCallbacks(callbackHandler, callbacks, responses);
         
         // Unregister callback handler.
         client.removeListener(callbackHandler);
      }
      catch (Throwable e)
      {
         System.out.println("failure: " + e.getMessage());
      }
   }
   
   
   /**
    * In this test the connection is configured for push callbacks implemented in
    * Remoting by polling the server for callbacks and pushing them (on the client 
    * side) to the InvokerCallbackHandler.  Acknowledgements are made from 
    * TestCallbackHandler.handleCallback() by an explicit call to
    * Client.acknowledgeCallback() after the callbacks have been received.
    */
   public void testPolledCallbackApplicationAcknowledgements()
   {
      try
      {
         // Register callback handler.
         InvokerCallbackHandler callbackHandler = new AcknowledgingCallbackHandler(client);
         HashMap metadata = new HashMap();
         metadata.put(CallbackPoller.CALLBACK_POLL_PERIOD, "1000");
         client.addListener(callbackHandler, metadata);
         
         // Request callbacks from server.
         client.invoke(CallbackAcknowledgeServer.APPLICATION_ACKNOWLDEGEMENTS);
         Thread.sleep(2000);
         
         // Unregister callback handler.
         client.removeListener(callbackHandler);
      }
      catch (Throwable e)
      {
         System.out.println("failure: " + e.getMessage());
      }
   }
   
   
   /**
    * In this test the connection is configured for push callbacks implemented in
    * Remoting by polling the server for callbacks and pushing them (on the client 
    * side) to the InvokerCallbackHandler.  Acknowledgements are handled implicitly
    * by Remoting.
    */
   public void testPolledCallbackRemotingAcknowledgements()
   {
      try
      {
         // Register callback handler.
         InvokerCallbackHandler callbackHandler = new NonAcknowledgingCallbackHandler();
         HashMap metadata = new HashMap();
         metadata.put(CallbackPoller.CALLBACK_POLL_PERIOD, "1000");
         client.addListener(callbackHandler, metadata);
         
         // Request callbacks from server.
         client.invoke(CallbackAcknowledgeServer.REMOTING_ACKNOWLDEGEMENTS);
         Thread.sleep(2000);
         
         // Unregister callback handler.
         client.removeListener(callbackHandler);
      }
      catch (Throwable e)
      {
         System.out.println("failure: " + e.getMessage());
      }
   }
   
   
   /**
    * In this test the connection is configured for true push callbacks.
    * Acknowledgements are made from TestCallbackHandler.handleCallback()
    * by an explicit call to Client.acknowledgeCallback() after the callbacks
    * have been received.
    */
   public void testPushCallbackApplicationAcknowledgements()
   {
      try
      {
         // Register callback handler.
         InvokerCallbackHandler callbackHandler = new AcknowledgingCallbackHandler(client);
         client.addListener(callbackHandler, null, null, true);
         
         // Request callbacks from servrr.
         client.invoke(CallbackAcknowledgeServer.APPLICATION_ACKNOWLDEGEMENTS);
         
         // Unregister callback handler.
         client.removeListener(callbackHandler);
      }
      catch (Throwable e)
      {
         System.out.println("failure: " + e.getMessage());
      }
   }
   
   
   /**
    * In this test the connection is configured for true push callbacks, and
    * Acknowledgements are handled implicitly by Remoting.
    */
   public void testPushCallbackRemotingAcknowledgements()
   {
      try
      {
         // Register callback handler.
         InvokerCallbackHandler callbackHandler = new NonAcknowledgingCallbackHandler();
         client.addListener(callbackHandler, null, null, true);
         
         // Request callbacks from server.
         client.invoke(CallbackAcknowledgeServer.REMOTING_ACKNOWLDEGEMENTS);
         
         // Unregister callback handler.
         client.removeListener(callbackHandler);
      }
      catch (Throwable e)
      {
         System.out.println("failure: " + e.getMessage());
      }
   }
  
   
   static class NonAcknowledgingCallbackHandler implements InvokerCallbackHandler
   {  
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         System.out.println("received push callback: " + callback.getParameter());
      }
   }
   
   
   static class AcknowledgingCallbackHandler implements InvokerCallbackHandler
   {  
      private Client client;
      
      public AcknowledgingCallbackHandler(Client client)
      {
         this.client = client;
      }
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         System.out.println("received push callback: " + callback.getParameter());
         Object response = callback.getParameter() + ": acknowledged";
         try
         {
            client.acknowledgeCallback(this, callback, response);
         }
         catch (Throwable e)
         {
            System.out.println("Unable to acknowledge callback: " + callback.getParameter());
         }
      }
   }
}
