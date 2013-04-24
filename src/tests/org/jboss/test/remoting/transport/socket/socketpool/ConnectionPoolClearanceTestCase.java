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
package org.jboss.test.remoting.transport.socket.socketpool;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;


/**
 * Unit test for JBREM-
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2739 $
 * <p>
 * Copyright Jul 11, 2007
 * </p>
 */
public class ConnectionPoolClearanceTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ConnectionPoolClearanceTestCase.class);
   
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
    * Tests blocking and nonblocking direct calls to Client.getCallbacks().
    */
   public void testMultipleClientConnectionPools() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
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
      
      // Create clients.
      InvokerLocator clientLocator1 = new InvokerLocator(locatorURI + "/?timeout=1000");
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client1 = new Client(clientLocator1, clientConfig);
      client1.connect();
      log.info("client 1 is connected");
      InvokerLocator clientLocator2 = new InvokerLocator(locatorURI + "/?timeout=2000");
      Client client2 = new Client(clientLocator2, clientConfig);
      client2.connect();
      log.info("client 2 is connected");
      
      // Test connections.
      assertEquals("abc", client1.invoke("abc"));
      assertEquals("xyz", client2.invoke("xyz"));
      log.info("connections are good");
      
      // Verify there are two connection pools.
      Field field = MicroSocketClientInvoker.class.getDeclaredField("connectionPools");
      field.setAccessible(true);
      Map connectionPools = (Map) field.get(null);
      MicroSocketClientInvoker invoker = (MicroSocketClientInvoker) client1.getInvoker();
      assertEquals(2, connectionPools.size());
      
      // Verify client invoker 2's pool has one connection.
      field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(client2.getInvoker());
      assertEquals(1, pool.size());
      
      // Verify both connection pools are eliminated if one client disconnects.
      client1.disconnect();
      assertEquals(0, connectionPools.size());
      
      // Verify client invoker 2's pool is now empty.
      assertEquals(0, pool.size());
      
      // Verify client invoker 2's pool gets a new connection with an invocation.
      client2.invoke("def");
      assertEquals(1, pool.size());
      
      // Verify client invoker 2's pool is cleared when client 2 disconnects.
      client2.disconnect();
      assertEquals(0, pool.size());
      
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

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