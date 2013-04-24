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
package org.jboss.remoting.samples.bisocket;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.remoting.transport.bisocket.BisocketServerInvoker;
import org.jboss.remoting.transport.socket.SocketServerInvoker;


/**
 * This class and org.jboss.remoting.samples.bisocket.BisocketSampleServer
 * demonstrate how to how to make an invocation and how to set up push callbacks
 * over the bisocket transport.
 * 
 * The reason for the existance of the bisocket transport, which is derived from the
 * socket transport, is to make it possible to do push callbacks to a client which 
 * is unable to create a ServerSocket, either due to security restrictions or due
 * to the fact that it is behind a firewall. 
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 1, 2008
 * </p>
 */
public class BisocketSampleClient
{
   private static Logger log = Logger.getLogger(BisocketSampleClient.class);

   
   public void makeInvocation(String locatorURI) throws Throwable
   {
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      Client client = new Client(clientLocator);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      if ("abc".equals(client.invoke("abc")))
         log.info("connection is good");
      else
         log.info("Should have gotten \"abc\" in reply");
      
      // Set up push callbacks.  Tell the Connector created by Client.addListener()
      // that it is a callback Connector, which tells the BisocketServerInvoker
      // not to create a ServerSocket.
      HashMap metadata = new HashMap();
      metadata.put(Bisocket.IS_CALLBACK_SERVER, "true");
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, metadata);
      
      // Use reflection to verify that the callback BisocketServerInvoker did not
      // create a ServerSocket.
      Set callbackConnectors = client.getCallbackConnectors(callbackHandler);
      if (callbackConnectors.size() != 1)
      {
         log.info("There should be one callback Connector");
      }
      else
      {
         Connector callbackConnector = (Connector) callbackConnectors.iterator().next();
         BisocketServerInvoker serverInvoker = (BisocketServerInvoker) callbackConnector.getServerInvoker();
         Field field = SocketServerInvoker.class.getDeclaredField("serverSockets");
         field.setAccessible(true);
         List serverSockets = (List) field.get(serverInvoker);
         log.info("number of ServerSockets held by callback BisocketServerInvoker: " + serverSockets.size());
      }
 
      // Request callback.
      client.invoke("CALLBACK");
      if (callbackHandler.getCounter() == 1)
         log.info("received callback");
      else
         log.info("didn't receive callback");
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   public static void main(String[] args)
   {
      // Configure logging.
      Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
      Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
      String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
      PatternLayout layout = new PatternLayout(pattern);
      ConsoleAppender consoleAppender = new ConsoleAppender(layout);
      Logger.getRootLogger().addAppender(consoleAppender);  
      
      try
      {
         String host = InetAddress.getLocalHost().getHostName();
         int port = BisocketSampleServer.port;
         String locatorURI = "bisocket://" + host + ":" + port;
         BisocketSampleClient client = new BisocketSampleClient();
         client.makeInvocation(locatorURI);
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }
   
   /**
    * An InvokerCallbackHandler is registered with the callback Connector, which
    * passes push callbacks to it by way of the handleCallback() method.
    * </p>
    */
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      private int counter;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         counter++;
      }
      
      public int getCounter()
      {
         return counter;
      }
   }
}