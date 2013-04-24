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
package org.jboss.test.remoting.callback.timeout;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
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
import org.jboss.remoting.transport.socket.ClientSocketWrapper;
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;
import org.jboss.remoting.transport.socket.ServerSocketWrapper;
import org.jboss.remoting.transport.socket.ServerThread;
import org.jboss.remoting.transport.socket.SocketServerInvoker;



/**
 * Unit test for JBREM-765.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2751 $
 * <p>
 * Copyright Aug 5, 2007
 * </p>
 */
public class BisocketCallbackTimeoutTestCase extends TestCase
{
   public static int port;
   private static Logger log = Logger.getLogger(BisocketCallbackTimeoutTestCase.class);
   private static final String CALLBACK_TEST = "callbackTest";

   private static boolean firstTime = true;

   // remoting server connector
   private Connector connector;
   private InvokerLocator serverLocator;
   private TestInvocationHandler invocationHandler;


   /**
    * Sets up target remoting server.
    */
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(Level.INFO);
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


   /**
    * Verifies that "timeout" value is used in absence of "callbackTimeout" value.
    */
   public void testNoCallbackTimeout() throws Throwable
   {
      log.info("entering " + getName());

      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.TIMEOUT, "3000");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();

      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      final Client client = new Client(serverLocator, clientConfig);
      client.connect();
      assertEquals("abc", client.invoke("abc"));
      log.info("client is connected");

      // Add callback handler.
      TestInvokerCallbackHandler callbackHandler = new TestInvokerCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      client.invoke(CALLBACK_TEST);
      assertEquals(1, callbackHandler.counter);
      log.info("callback handler is installed");

      // Verify that "timeout" is used correctly.
      assertTrue(connector.getServerInvoker() instanceof SocketServerInvoker);
      SocketServerInvoker serverInvoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(serverInvoker);
      Set threads = clientpool.getContents();
      assertEquals(1, threads.size());
      ServerThread serverThread = (ServerThread) threads.iterator().next();
      field = ServerThread.class.getDeclaredField("socketWrapper");
      field.setAccessible(true);
      assertTrue(field.get(serverThread) instanceof ServerSocketWrapper);
      ServerSocketWrapper serverWrapper = (ServerSocketWrapper) field.get(serverThread);
      assertEquals(3000, serverWrapper.getTimeout());

      // Verify that "timeout" is used in the absence of "callbackTimeout".
      field = ServerInvoker.class.getDeclaredField("callbackHandlers");
      field.setAccessible(true);
      Map callbackHandlers = (Map) field.get(serverInvoker);
      assertEquals(1, callbackHandlers.size());
      ServerInvokerCallbackHandler serverInvokerCallbackHandler;
      serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) callbackHandlers.values().iterator().next();
      Client callbackClient = serverInvokerCallbackHandler.getCallbackClient();
      assertTrue(callbackClient.getInvoker() instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker callbackClientInvoker = (MicroSocketClientInvoker) callbackClient.getInvoker();
      field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(callbackClientInvoker);
      assertEquals(1, pool.size());
      Object o = pool.iterator().next();
      assertTrue(o instanceof ClientSocketWrapper);
      ClientSocketWrapper clientWrapper = (ClientSocketWrapper) o;
      assertEquals(3000, clientWrapper.getTimeout());

      client.removeListener(callbackHandler);
      client.disconnect();
      connector.stop();
   }


   /**
    * Verifies that "callbackTimeout" value overrides "timeout" value.
    * Values are specified in configuration map.
    */
   public void testDistinctCallbackTimeoutConfigMap() throws Throwable
   {
      log.info("entering " + getName());

      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.TIMEOUT, "3000");
      config.put(ServerInvokerCallbackHandler.CALLBACK_TIMEOUT, "7000");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();

      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      final Client client = new Client(serverLocator, clientConfig);
      client.connect();
      assertEquals("abc", client.invoke("abc"));
      log.info("client is connected");

      // Add callback handler.
      TestInvokerCallbackHandler callbackHandler = new TestInvokerCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      client.invoke(CALLBACK_TEST);
      assertEquals(1, callbackHandler.counter);
      log.info("callback handler is installed");

      // Verify that "timeout" is used correctly.
      assertTrue(connector.getServerInvoker() instanceof SocketServerInvoker);
      SocketServerInvoker serverInvoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(serverInvoker);
      Set threads = clientpool.getContents();
      assertEquals(1, threads.size());
      ServerThread serverThread = (ServerThread) threads.iterator().next();
      field = ServerThread.class.getDeclaredField("socketWrapper");
      field.setAccessible(true);
      assertTrue(field.get(serverThread) instanceof ServerSocketWrapper);
      ServerSocketWrapper serverWrapper = (ServerSocketWrapper) field.get(serverThread);
      assertEquals(3000, serverWrapper.getTimeout());

      // Verify that "callbackTimeout" is used correctly.
      field = ServerInvoker.class.getDeclaredField("callbackHandlers");
      field.setAccessible(true);
      Map callbackHandlers = (Map) field.get(serverInvoker);
      assertEquals(1, callbackHandlers.size());
      ServerInvokerCallbackHandler serverInvokerCallbackHandler;
      serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) callbackHandlers.values().iterator().next();
      Client callbackClient = serverInvokerCallbackHandler.getCallbackClient();
      assertTrue(callbackClient.getInvoker() instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker callbackClientInvoker = (MicroSocketClientInvoker) callbackClient.getInvoker();
      field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(callbackClientInvoker);
      assertEquals(1, pool.size());
      Object o = pool.iterator().next();
      assertTrue(o instanceof ClientSocketWrapper);
      ClientSocketWrapper clientWrapper = (ClientSocketWrapper) o;
      assertEquals(7000, clientWrapper.getTimeout());

      client.removeListener(callbackHandler);
      client.disconnect();
      connector.stop();
   }


   /**
    * Verifies that "callbackTimeout" value overrides "timeout" value.
    * Values are specified in InvokerLocator.
    */
   public void testDistinctCallbackTimeoutLocator() throws Throwable
   {
      log.info("entering " + getName());

      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      locatorURI += "/?timeout=3000&callbackTimeout=7000";
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();

      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      final Client client = new Client(serverLocator, clientConfig);
      client.connect();
      assertEquals("abc", client.invoke("abc"));
      log.info("client is connected");

      // Add callback handler.
      TestInvokerCallbackHandler callbackHandler = new TestInvokerCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      client.invoke(CALLBACK_TEST);
      assertEquals(1, callbackHandler.counter);
      log.info("callback handler is installed");

      // Verify that "timeout" is used correctly.
      assertTrue(connector.getServerInvoker() instanceof SocketServerInvoker);
      SocketServerInvoker serverInvoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(serverInvoker);
      Set threads = clientpool.getContents();
      assertEquals(1, threads.size());
      ServerThread serverThread = (ServerThread) threads.iterator().next();
      field = ServerThread.class.getDeclaredField("socketWrapper");
      field.setAccessible(true);
      assertTrue(field.get(serverThread) instanceof ServerSocketWrapper);
      ServerSocketWrapper serverWrapper = (ServerSocketWrapper) field.get(serverThread);
      assertEquals(3000, serverWrapper.getTimeout());

      // Verify that "callbackTimeout" is used correctly.
      field = ServerInvoker.class.getDeclaredField("callbackHandlers");
      field.setAccessible(true);
      Map callbackHandlers = (Map) field.get(serverInvoker);
      assertEquals(1, callbackHandlers.size());
      ServerInvokerCallbackHandler serverInvokerCallbackHandler;
      serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) callbackHandlers.values().iterator().next();
      Client callbackClient = serverInvokerCallbackHandler.getCallbackClient();
      assertTrue(callbackClient.getInvoker() instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker callbackClientInvoker = (MicroSocketClientInvoker) callbackClient.getInvoker();
      field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(callbackClientInvoker);
      assertEquals(1, pool.size());
      Object o = pool.iterator().next();
      assertTrue(o instanceof ClientSocketWrapper);
      ClientSocketWrapper clientWrapper = (ClientSocketWrapper) o;
      assertEquals(7000, clientWrapper.getTimeout());

      client.removeListener(callbackHandler);
      client.disconnect();
      connector.stop();
   }


   /**
    * Verifies that "timeout" value is used in absense of "callbackTimeout".
    * "timeout" value is specified in Client.addListener() metadata map.
    */
   public void testNoCallbackTimeoutMetadata() throws Throwable
   {
      log.info("entering " + getName());

      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();

      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      final Client client = new Client(serverLocator, clientConfig);
      client.connect();
      assertEquals("abc", client.invoke("abc"));
      log.info("client is connected");

      // Add callback handler.
      TestInvokerCallbackHandler callbackHandler = new TestInvokerCallbackHandler();
      Map metadata = new HashMap();
      metadata.put(ServerInvoker.TIMEOUT, "3000");
      client.addListener(callbackHandler, metadata);
      client.invoke(CALLBACK_TEST);
      assertEquals(1, callbackHandler.counter);
      log.info("callback handler is installed");

      // Verify that "timeout" is used correctly.
      ServerInvoker serverInvoker = connector.getServerInvoker();
      Field field = ServerInvoker.class.getDeclaredField("callbackHandlers");
      field.setAccessible(true);
      Map callbackHandlers = (Map) field.get(serverInvoker);
      assertEquals(1, callbackHandlers.size());
      ServerInvokerCallbackHandler serverInvokerCallbackHandler;
      serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) callbackHandlers.values().iterator().next();
      Client callbackClient = serverInvokerCallbackHandler.getCallbackClient();
      assertTrue(callbackClient.getInvoker() instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker callbackClientInvoker = (MicroSocketClientInvoker) callbackClient.getInvoker();
      field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(callbackClientInvoker);
      assertEquals(1, pool.size());
      Object o = pool.iterator().next();
      assertTrue(o instanceof ClientSocketWrapper);
      ClientSocketWrapper clientWrapper = (ClientSocketWrapper) o;
      assertEquals(3000, clientWrapper.getTimeout());

      client.removeListener(callbackHandler);
      client.disconnect();
      connector.stop();
   }


   /**
    * Verifies that "callbackTimeout" value overrides "timeout" value.
    * "callbackTimeout" is specified in Client.addListener() metadata map.
    */
   public void testDistinctCallbackTimeoutMetadata() throws Throwable
   {
      log.info("entering " + getName());

      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();

      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      final Client client = new Client(serverLocator, clientConfig);
      client.connect();
      assertEquals("abc", client.invoke("abc"));
      log.info("client is connected");

      // Add callback handler.
      TestInvokerCallbackHandler callbackHandler = new TestInvokerCallbackHandler();
      Map metadata = new HashMap();
      metadata.put(ServerInvokerCallbackHandler.CALLBACK_TIMEOUT, "7000");
      client.addListener(callbackHandler, metadata);
      client.invoke(CALLBACK_TEST);
      assertEquals(1, callbackHandler.counter);
      log.info("callback handler is installed");

      // Verify that "callbackTimeout" is used correctly.
      SocketServerInvoker serverInvoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = ServerInvoker.class.getDeclaredField("callbackHandlers");
      field.setAccessible(true);
      Map callbackHandlers = (Map) field.get(serverInvoker);
      assertEquals(1, callbackHandlers.size());
      ServerInvokerCallbackHandler serverInvokerCallbackHandler;
      serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) callbackHandlers.values().iterator().next();
      Client callbackClient = serverInvokerCallbackHandler.getCallbackClient();
      assertTrue(callbackClient.getInvoker() instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker callbackClientInvoker = (MicroSocketClientInvoker) callbackClient.getInvoker();
      field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(callbackClientInvoker);
      assertEquals(1, pool.size());
      Object o = pool.iterator().next();
      assertTrue(o instanceof ClientSocketWrapper);
      ClientSocketWrapper clientWrapper = (ClientSocketWrapper) o;
      assertEquals(7000, clientWrapper.getTimeout());

      client.removeListener(callbackHandler);
      client.disconnect();
      connector.stop();
   }


   protected String getTransport()
   {
      return "bisocket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public Set listeners = new HashSet();
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         listeners.add(callbackHandler);
      }
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         if (CALLBACK_TEST.equals(invocation.getParameter()))
         {
            Iterator it = listeners.iterator();
            while (it.hasNext())
            {
               InvokerCallbackHandler handler = (InvokerCallbackHandler) it.next();
               handler.handleCallback(new Callback("test"));
            }
         }
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class TestInvokerCallbackHandler implements InvokerCallbackHandler
   {
      public int counter;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         counter++;
      }
   }
}