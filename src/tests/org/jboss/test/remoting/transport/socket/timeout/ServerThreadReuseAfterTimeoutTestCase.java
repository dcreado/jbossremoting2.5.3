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
package org.jboss.test.remoting.transport.socket.timeout;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
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
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.ServerThread;
import org.jboss.remoting.transport.socket.SocketServerInvoker;


public class ServerThreadReuseAfterTimeoutTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ServerThreadReuseAfterTimeoutTestCase.class);
   
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

   
   public void testJavaSerializationDefault() throws Throwable
   {
      log.info("entering " + getName());
      Map config = new HashMap();
      doJavaSerializationTest(config);
      log.info(getName() + " PASSES");
   }
   
   
   public void testJavaSerializationConfigured() throws Throwable
   {
      log.info("entering " + getName());
      Map config = new HashMap();
      config.put(ServerThread.CONTINUE_AFTER_TIMEOUT, "false");
      doJavaSerializationTest(config);
      log.info(getName() + " PASSES");
   }
   

   public void testJBossSerializationDefault() throws Throwable
   {
      log.info("entering " + getName());
      Map config = new HashMap();
      doJBossSerializationTest(config);
      log.info(getName() + " PASSES");
   }
   
   
   public void testJBossSerializationConfigured() throws Throwable
   {
      log.info("entering " + getName());
      Map config = new HashMap();
      config.put(ServerThread.CONTINUE_AFTER_TIMEOUT, "true");
      doJBossSerializationTest(config);
      log.info(getName() + " PASSES");
   }
   
   
   public void doJavaSerializationTest(Map clientConfig) throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer("java");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Get ServerThread.
      SocketServerInvoker invoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(invoker);
      assertEquals(1, clientpool.size());
      Set s =  clientpool.getContents();
      ServerThread serverThread1 = (ServerThread) s.iterator().next();
      
      // Get threadpool.
      field = SocketServerInvoker.class.getDeclaredField("threadpool");
      field.setAccessible(true);
      LinkedList threadpool = (LinkedList) field.get(invoker);
      assertEquals(0, threadpool.size());
      
      // Wait for ServerThread to time out.
      Thread.sleep(6000);
      for (int i = 0; i < 5; i++)
      {
         Thread.sleep(2000);
         if (clientpool.size() == 0) break;
      }
      
      if (clientpool.size() > 0)
      {
         fail("expect clientpool.size() == 0");
      }
      
      // Verify original ServerThread was returned to threadpool.
      assertEquals(1, threadpool.size());
      assertEquals(serverThread1, threadpool.iterator().next());
      
      // Make another invocation and verify ServerThread was reused.
      client.invoke("xyz");
      assertEquals(1, clientpool.size());
      s =  clientpool.getContents();
      ServerThread serverThread2 = (ServerThread) s.iterator().next();
      assertEquals(serverThread1, serverThread2);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void doJBossSerializationTest(Map clientConfig) throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer("jboss");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Get clientpool and ServerThread.
      SocketServerInvoker invoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(invoker);
      assertEquals(1, clientpool.size());
      Set clientpoolContents =  clientpool.getContents();
      ServerThread serverThread1 = (ServerThread) clientpoolContents.iterator().next();
      
      // Get threadpool.
      field = SocketServerInvoker.class.getDeclaredField("threadpool");
      field.setAccessible(true);
      LinkedList threadpool = (LinkedList) field.get(invoker);
      assertEquals(0, threadpool.size());
      
      // Wait for ServerThread to time out.
      Thread.sleep(8000);
      
      // Verify original ServerThread remains in clientpool.
      assertEquals(0, threadpool.size());
      assertEquals(1, clientpool.size());
      clientpoolContents =  clientpool.getContents();
      assertEquals(serverThread1, clientpoolContents.iterator().next());
      
      // Make another invocation and verify ServerThread was reused.
      client.invoke("xyz");
      assertEquals(1, clientpool.size());
      clientpoolContents =  clientpool.getContents();
      ServerThread serverThread2 = (ServerThread) clientpoolContents.iterator().next();
      assertEquals(serverThread1, serverThread2);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer(String serializationType) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port + "/?timeout=4000";
      locatorURI += "&serializationtype=" + serializationType;
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
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("received callback");
      }  
   }
}