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
package org.jboss.test.remoting.transport.socket.connectionpool;

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
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;


/**
 * 
 * Unit test for JBREM-858.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Dec 3, 2007
 * </p>
 */
public class ConnectionPoolSizeTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ConnectionPoolSizeTestCase.class);
   
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
   
   
   public void testClientMaxPoolSizeInConnectionPoolKey() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create clients.
      InvokerLocator clientLocator1 = new InvokerLocator(locatorURI + "/?" + MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG + "=3");
      InvokerLocator clientLocator2 = new InvokerLocator(locatorURI + "/?" + MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG + "=7");
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client1 = new Client(clientLocator1, clientConfig);
      client1.connect();
      log.info("client1 is connected: " + clientLocator1);
      Client client2 = new Client(clientLocator2, clientConfig);
      client2.connect();
      log.info("client2 is connected: " + clientLocator2);
      
      // Test connections.
      assertEquals("abc", client1.invoke("abc"));
      log.info("connection 1 is good");
      assertEquals("def", client2.invoke("def"));
      log.info("connection 2 is good");
      
      log.info("invoker1: " + client1.getInvoker());
      log.info("invoker2: " + client2.getInvoker());
      assertTrue(client1.getInvoker() instanceof MicroSocketClientInvoker);
      assertTrue(client2.getInvoker() instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker invoker1 = (MicroSocketClientInvoker) client1.getInvoker();
      MicroSocketClientInvoker invoker2 = (MicroSocketClientInvoker) client2.getInvoker();
      assertNotSame(invoker1, invoker2);
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      assertNotSame(field.get(invoker1), field.get(invoker2));
      
      field = MicroSocketClientInvoker.class.getDeclaredField("address");
      field.setAccessible(true);
      log.info("invoker1.address: " + field.get(invoker1));
      log.info("invoker2.address: " + field.get(invoker2));
      
      client1.disconnect();
      client2.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "socket";
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