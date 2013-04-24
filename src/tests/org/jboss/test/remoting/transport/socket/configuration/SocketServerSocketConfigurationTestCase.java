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
package org.jboss.test.remoting.transport.socket.configuration;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
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
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.ServerThread;
import org.jboss.remoting.transport.socket.SocketServerInvoker;


/**
 * Unit test for JBREM-703.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Feb 28, 2008
 * </p>
 */
public class SocketServerSocketConfigurationTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(SocketServerSocketConfigurationTestCase.class);
   
   protected static boolean firstTime = true;
   
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
   
   
   public void testConfigureByConfigMap() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true);
      
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
      
      SocketServerInvoker invoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool pool = (LRUPool) field.get(invoker);
      Set keys = pool.getContents();
      assertEquals(1, keys.size());
      ServerThread t = (ServerThread) keys.iterator().next();
      field = ServerThread.class.getDeclaredField("socket");
      field.setAccessible(true);
      Socket socket = (Socket) field.get(t);
      doSocketTest(socket);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testConfigureBySetter() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false);
      
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
      
      SocketServerInvoker invoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool pool = (LRUPool) field.get(invoker);
      Set keys = pool.getContents();
      assertEquals(1, keys.size());
      ServerThread t = (ServerThread) keys.iterator().next();
      field = ServerThread.class.getDeclaredField("socket");
      field.setAccessible(true);
      Socket socket = (Socket) field.get(t);
      doSocketTest(socket);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }

   
   protected void doSocketTest(Socket s) throws SocketException
   {
      assertTrue(s.getKeepAlive());
      assertTrue(s.getOOBInline());
      suggestEquals(2345, s.getReceiveBufferSize(), "receiveBufferSize");
      suggestEquals(3456, s.getSendBufferSize(), "sendBufferSize");
      assertEquals(4567, s.getSoLinger());
      suggestEquals(0, s.getTrafficClass(), "trafficClass");
   }
   
   
   protected void suggestEquals(int i1, int i2, String s)
   {
      if (i1 != i2)
      {
         log.warn(s + " has not been set: expected " + i1 + ", got " + i2);
         log.warn("note that setting \"" + s + "\" is just a suggestion to the underlying network code");
      }
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer(boolean useConfigMap) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      if (useConfigMap)
      {
         addServerSocketConfig(config);
      }
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      if (!useConfigMap)
      {
         SocketServerInvoker invoker = (SocketServerInvoker) connector.getServerInvoker();
         configureServerInvoker(invoker);
      }
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
   }
   
   
   protected void addServerSocketConfig(Map config)
   {
      config.put("keepAlive", "true");
      config.put("oOBInline", "true");
      config.put("receiveBufferSize", "2345");
      config.put("sendBufferSize", "3456");
      config.put("soLinger", "true");
      config.put("soLingerDuration", "4567"); 
      config.put("trafficClass", "0");
   }
   
   
   protected void configureServerInvoker(SocketServerInvoker invoker)
   {
      invoker.setReceiveBufferSize(12345);
      
      invoker.setKeepAlive(true);
      invoker.setOOBInline(true);
      invoker.setReceiveBufferSize(2345);
      invoker.setSendBufferSize(3456);
      invoker.setSoLinger(true);
      invoker.setSoLingerDuration(4567); 
      invoker.setTrafficClass(0);
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
}