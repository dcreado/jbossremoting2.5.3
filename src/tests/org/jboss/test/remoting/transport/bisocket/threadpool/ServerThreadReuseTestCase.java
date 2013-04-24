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
package org.jboss.test.remoting.transport.bisocket.threadpool;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.remoting.transport.bisocket.BisocketServerInvoker;
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.ServerThread;
import org.jboss.remoting.transport.socket.SocketServerInvoker;


/**
 * Unit test for JBREM-938.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Mar 22, 2008
 * </p>
 */
public class ServerThreadReuseTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ServerThreadReuseTestCase.class);
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;

   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }
   }

   
   public void tearDown()
   {
   }
   
   
   public void testServerThreadReuse() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Add callback handler.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put(Bisocket.IS_CALLBACK_SERVER, "true");
      client.addListener(callbackHandler, metadata, null, true);
      client.invoke("callback");
      assertEquals(1, callbackHandler.counter);
      log.info("first callback successful");
      
      // Get callback server thread pools.
      Set callbackConnectors = client.getCallbackConnectors(callbackHandler);
      assertEquals(1, callbackConnectors.size());
      Connector callbackConnector = (Connector) callbackConnectors.iterator().next();
      BisocketServerInvoker invoker = (BisocketServerInvoker) callbackConnector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("threadpool");
      field.setAccessible(true);
      List threadpool = (List) field.get(invoker);
      field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(invoker);
      assertEquals(0, threadpool.size());
      assertEquals(1, clientpool.size());
      
      // Kill worker thread's socket.
      Set clientpoolContents = clientpool.getContents();
      ServerThread serverThread1 = (ServerThread) clientpoolContents.iterator().next();
      field = ServerThread.class.getDeclaredField("socket");
      field.setAccessible(true);
      Socket socket = (Socket) field.get(serverThread1);
      socket.close();
      Thread.sleep(4000);
      assertEquals(1, threadpool.size());
      assertEquals(0, clientpool.size());
      
      // Make second callback and verify that worker thread gets reused.
      client.invoke("callback");
      assertEquals(2, callbackHandler.counter);
      log.info("second callback successful");
      assertEquals(0, threadpool.size());
      assertEquals(1, clientpool.size());
      clientpoolContents = clientpool.getContents();
      ServerThread serverThread2 = (ServerThread) clientpoolContents.iterator().next();
      assertEquals(serverThread2, serverThread1);
      log.info("ServerThread was reused");
      
      client.removeListener(callbackHandler);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "bisocket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer() throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      HashSet callbackHandlers = new HashSet();
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         callbackHandlers.add(callbackHandler);
      }
      
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         String command = (String) invocation.getParameter();
         
         if ("callback".equals(command))
         {
            Callback callback = new Callback("callback");
            Iterator it = callbackHandlers.iterator();
            while (it.hasNext())
            {
               ServerInvokerCallbackHandler handler;
               handler = (ServerInvokerCallbackHandler) it.next();
               handler.handleCallback(callback);
            }
         }
         
         return command;
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         callbackHandlers.remove(callbackHandler);
      }
      
      
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      public int counter;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         counter++;
         log.info("received callback");
      }  
   }
}