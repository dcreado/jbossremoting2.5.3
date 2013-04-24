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
package org.jboss.test.remoting.shutdown.local;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.MicroRemoteClientInvoker;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;

/** 
 * Provides unit tests that verifies that Client.disconnectLocal() and
 * Client.removeListenerLocal() do not attempt to contact the server.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2172 $
 * <p>
 * Copyright Jan 18, 2007
 * </p>
 */
public class LocalShutdownTestCase extends TestCase
{
   private Logger log = Logger.getLogger(LocalShutdownTestCase.class);
   protected static boolean firstTime = true;
   
   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(Level.DEBUG);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.DEBUG);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }
   }
  
   public void testDummy()
   {
      
   }
//   public void testDisconnectLocal() throws Throwable
//   {
//      log.info("entering " + getName());
//      
//      // Start server.
//      String host = InetAddress.getLocalHost().getHostAddress();
//      int port = PortUtil.findFreePort(host);
//      String locatorURI = "socket://" + host + ":" + port;
//      InvokerLocator locator = new InvokerLocator(locatorURI);
//      HashMap serverConfig = new HashMap();
//      Connector connector = new Connector(locator, serverConfig);
//      connector.create();
//      connector.addInvocationHandler("test", new TestHandler());
//      connector.addConnectionListener(new TestListener());
//      connector.start();
//      
//      // Connect client, verify that a lease exists, then disconnect client
//      // and verify that lease has been removed on server.
//      HashMap clientConfig = new HashMap();
//      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
//      clientConfig.put(Client.ENABLE_LEASE, "true");
//      clientConfig.put(InvokerLocator.CLIENT_LEASE_PERIOD, "1000");
//      Client client = new Client(locator, clientConfig);
//      client.connect();
//      Integer i = (Integer) client.invoke(new Integer(7));
//      assertEquals(8, i.intValue());
//      Thread.sleep(1000);
//      ServerInvoker serverInvoker = connector.getServerInvoker();
//      Field field = ServerInvoker.class.getDeclaredField("clientLeases");
//      field.setAccessible(true);
//      Map clientLeases = (Map) field.get(serverInvoker);
//      assertEquals(1, clientLeases.size());
//      field = MicroRemoteClientInvoker.class.getDeclaredField("invokerSessionId");
//      field.setAccessible(true);
//      String invokerSessionId = (String) field.get(client.getInvoker());
//      assertTrue(clientLeases.containsKey(invokerSessionId));
//      client.disconnect();
//      assertEquals(0, clientLeases.size());
//      
//      // Connect new client, verify that a lease exists, then locally disconnect
//      // client and verify that lease still exists on server.
//      client = new Client(locator, clientConfig);
//      client.connect();
//      i = (Integer) client.invoke(new Integer(11));
//      assertEquals(12, i.intValue());
//      Thread.sleep(1000);
//      assertEquals(1, clientLeases.size());
//      invokerSessionId = (String) field.get(client.getInvoker());
//      assertTrue(clientLeases.containsKey(invokerSessionId));
//      client.disconnectLocal();
//      assertEquals(1, clientLeases.size());
//      assertTrue(clientLeases.containsKey(invokerSessionId));
//      
//      connector.stop();
//      log.info(getName() + " PASSES");
//   }
   
   
//   public void testRemoveListenerLocalPushCallbacks() throws Throwable
   {
//      log.info("entering " + getName());
//      
//      // Start server.
//      String host = InetAddress.getLocalHost().getHostAddress();
//      int port = PortUtil.findFreePort(host);
//      String locatorURI = "socket://" + host + ":" + port;
//      InvokerLocator locator = new InvokerLocator(locatorURI);
//      HashMap serverConfig = new HashMap();
//      serverConfig.put(InvokerLocator.FORCE_REMOTE, "true");
//      Connector connector = new Connector(locator, serverConfig);
//      connector.create();
//      connector.addInvocationHandler("test", new TestHandler());
//      connector.start();
//      
//      // Connect client, add a callback handler, then call Client.removeListener()
//      // and verify that callback handler has been removed on server.
//      HashMap clientConfig = new HashMap();
//      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
//      Client client = new Client(locator, clientConfig);
//      client.connect();
//      Integer i = (Integer) client.invoke(new Integer(7));
//      assertEquals(8, i.intValue());
//      TestCallbackHandler callbackHandler = new TestCallbackHandler();
//      client.addListener(callbackHandler, null, null, true);
//      ServerInvoker serverInvoker = connector.getServerInvoker();
//      Field field = ServerInvoker.class.getDeclaredField("callbackHandlers");
//      field.setAccessible(true);
//      Map callbackHandlers = (Map) field.get(serverInvoker);
//      assertEquals(1, callbackHandlers.size());
//      client.removeListener(callbackHandler);
//      assertEquals(0, callbackHandlers.size());
//      client.disconnect();      
//
//      // Connect client, add a callback handler, then call Client.removeListenerLocal()
//      // and verify that callback handler still exists on server.
//      client = new Client(locator, clientConfig);
//      client.connect();
//      i = (Integer) client.invoke(new Integer(11));
//      assertEquals(12, i.intValue());
//      callbackHandler = new TestCallbackHandler();
//      client.addListener(callbackHandler, null, null, true);
//      assertEquals(1, callbackHandlers.size());
//      client.removeListenerLocal(callbackHandler);
//      assertEquals(1, callbackHandlers.size());
//      client.disconnect();  
//      
//      connector.stop();
//      log.info(getName() + " PASSES");
//   }
//   
//   
//   public void testRemoveListenerLocalPolledCallbacks() throws Throwable
//   {
//      log.info("entering " + getName());
//      
//      // Start server.
//      String host = InetAddress.getLocalHost().getHostAddress();
//      int port = PortUtil.findFreePort(host);
//      String locatorURI = "socket://" + host + ":" + port;
//      InvokerLocator locator = new InvokerLocator(locatorURI);
//      HashMap serverConfig = new HashMap();
//      serverConfig.put(InvokerLocator.FORCE_REMOTE, "true");
//      Connector connector = new Connector(locator, serverConfig);
//      connector.create();
//      connector.addInvocationHandler("test", new TestHandler());
//      connector.start();
//      
//      // Connect client, add a callback handler, then call Client.removeListener()
//      // and verify that callback handler has been removed on server.
//      HashMap clientConfig = new HashMap();
//      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
//      Client client = new Client(locator, clientConfig);
//      client.connect();
//      Integer i = (Integer) client.invoke(new Integer(7));
//      assertEquals(8, i.intValue());
//      TestCallbackHandler callbackHandler = new TestCallbackHandler();
//      client.addListener(callbackHandler, null, null, false);
//      ServerInvoker serverInvoker = connector.getServerInvoker();
//      Field field = ServerInvoker.class.getDeclaredField("callbackHandlers");
//      field.setAccessible(true);
//      Map callbackHandlers = (Map) field.get(serverInvoker);
//      assertEquals(1, callbackHandlers.size());
//      client.removeListener(callbackHandler);
//      assertEquals(0, callbackHandlers.size());
//      client.disconnect();      
//
//      // Connect client, add a callback handler, then call Client.removeListenerLocal()
//      // and verify that callback handler still exists on server.
//      client = new Client(locator, clientConfig);
//      client.connect();
//      i = (Integer) client.invoke(new Integer(11));
//      assertEquals(12, i.intValue());
//      callbackHandler = new TestCallbackHandler();
//      client.addListener(callbackHandler, null, null, false);
//      assertEquals(1, callbackHandlers.size());
//      client.removeListenerLocal(callbackHandler);
//      assertEquals(1, callbackHandlers.size());
//      client.disconnect();  
//      
//      connector.stop();
//      log.info(getName() + " PASSES");
//   }
//   
//   
//   public void testRemoveListenerLocalPullCallbacks() throws Throwable
//   {
//      log.info("entering " + getName());
//      
//      // Start server.
//      String host = InetAddress.getLocalHost().getHostAddress();
//      int port = PortUtil.findFreePort(host);
//      String locatorURI = "socket://" + host + ":" + port;
//      InvokerLocator locator = new InvokerLocator(locatorURI);
//      HashMap serverConfig = new HashMap();
//      serverConfig.put(InvokerLocator.FORCE_REMOTE, "true");
//      Connector connector = new Connector(locator, serverConfig);
//      connector.create();
//      connector.addInvocationHandler("test", new TestHandler());
//      connector.start();
//      
//      // Connect client, add a callback handler, then call Client.removeListener()
//      // and verify that callback handler has been removed on server.
//      HashMap clientConfig = new HashMap();
//      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
//      Client client = new Client(locator, clientConfig);
//      client.connect();
//      Integer i = (Integer) client.invoke(new Integer(7));
//      assertEquals(8, i.intValue());
//      TestCallbackHandler callbackHandler = new TestCallbackHandler();
//      client.addListener(callbackHandler);
//      ServerInvoker serverInvoker = connector.getServerInvoker();
//      Field field = ServerInvoker.class.getDeclaredField("callbackHandlers");
//      field.setAccessible(true);
//      Map callbackHandlers = (Map) field.get(serverInvoker);
//      assertEquals(1, callbackHandlers.size());
//      client.removeListener(callbackHandler);
//      assertEquals(0, callbackHandlers.size());
//      client.disconnect();      
//
//      // Connect client, add a callback handler, then call Client.removeListenerLocal()
//      // and verify that callback handler still exists on server.
//      client = new Client(locator, clientConfig);
//      client.connect();
//      i = (Integer) client.invoke(new Integer(11));
//      assertEquals(12, i.intValue());
//      callbackHandler = new TestCallbackHandler();
//      client.addListener(callbackHandler);
//      assertEquals(1, callbackHandlers.size());
//      client.removeListenerLocal(callbackHandler);
//      assertEquals(1, callbackHandlers.size());
//      client.disconnect();  
//      
//      connector.stop();
//      log.info(getName() + " PASSES");
   }
   
   
   public class TestHandler implements ServerInvocationHandler
   {

      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         Integer i = (Integer) invocation.getParameter();
         return new Integer(i.intValue() + 1);
      }

      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
   }
   
   
   public class TestListener implements ConnectionListener
   {
      public void handleConnectionException(Throwable throwable, Client client) {}
   }
   
   
   public class TestCallbackHandler implements InvokerCallbackHandler
   {
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("received callback: " + callback);
      }  
   }
}