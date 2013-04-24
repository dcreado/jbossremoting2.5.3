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
package org.jboss.test.remoting.invoker;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit tests from JBREM-877.
 * Adjusted for JBREM-1176.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Feb 22, 2008
 * </p>
 */
public class ClientInvokerDelayedDestructionTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ClientInvokerDelayedDestructionTestCase.class);
   
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
   
   
   public void testNoDelayedDestruction() throws Throwable
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
      ClientInvoker invoker1 = client.getInvoker();
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good for first client");
      client.disconnect();
      
      client = new Client(clientLocator, clientConfig);
      client.connect();
      ClientInvoker invoker2 = client.getInvoker();
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good for second client"); 
      assertNotSame(invoker2, invoker1);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testZeroDelay() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.INVOKER_DESTRUCTION_DELAY, "0");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      ClientInvoker invoker1 = client.getInvoker();
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good for first client");
      client.disconnect();
      
      client = new Client(clientLocator, clientConfig);
      client.connect();
      ClientInvoker invoker2 = client.getInvoker();
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good for second client"); 
      assertNotSame(invoker2, invoker1);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testDelayThenGetNewInvoker() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.INVOKER_DESTRUCTION_DELAY, "5000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      ClientInvoker invoker1 = client.getInvoker();
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good for first client");
      client.disconnect();
      
      Thread.sleep(10000);
      client = new Client(clientLocator, clientConfig);
      client.connect();
      ClientInvoker invoker2 = client.getInvoker();
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good for second client"); 
      assertNotSame(invoker2, invoker1);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testDelayThenReuseInvoker() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.INVOKER_DESTRUCTION_DELAY, "10000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      ClientInvoker invoker1 = client.getInvoker();
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good for first client");
      client.disconnect();
      
      Thread.sleep(5000);
      client = new Client(clientLocator, clientConfig);
      client.connect();
      ClientInvoker invoker2 = client.getInvoker();
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good for second client"); 
      assertSame(invoker2, invoker1);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testEventualDestruction() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.INVOKER_DESTRUCTION_DELAY, "5000");
      addExtraClientConfig(clientConfig);

      for (int i = 0; i < 50; i++)
      {
         Client client = new Client(clientLocator, clientConfig);
         client.connect();
         assertEquals("abc", client.invoke("abc"));
         client.disconnect();
      }
      
      Thread.sleep(10000);
      assertEquals(0, InvokerRegistry.getClientInvokers().length);

      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testStaticTimer() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      
      // Verify Timer hasn't been created.
      Field field = Client.class.getDeclaredField("invokerDestructionTimer");
      field.setAccessible(true);
      Timer invokerDestructionTimer = (Timer) field.get(null);
      assertNull(invokerDestructionTimer);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.INVOKER_DESTRUCTION_DELAY, "5000");
      addExtraClientConfig(clientConfig);
      Client[] clients = new Client[50];

      for (int i = 0; i < 50; i++)
      {
         clients[i] = new Client(clientLocator, clientConfig);
         clients[i].connect();
         assertEquals("abc", clients[i].invoke("abc"));
         clients[i].disconnect();
      }
      
      // Verify Timer has been created.
      invokerDestructionTimer = (Timer) field.get(null);
      assertNotNull(invokerDestructionTimer);
      
      // Verify all Clients are using the same Timer.
      for (int i = 0; i < 50; i++)
      {
         assertEquals("Should be the same Timer", invokerDestructionTimer, field.get(clients[i]));
      }
      
      Thread.sleep(10000);
      assertEquals(0, InvokerRegistry.getClientInvokers().length);
      
      // Verify Timer has been destroyed.
      invokerDestructionTimer = (Timer) field.get(null);
      assertNull(invokerDestructionTimer);
      
      // Recreate Clients to verify that a new Timer is created.
      for (int i = 0; i < 50; i++)
      {
         clients[i] = new Client(clientLocator, clientConfig);
         clients[i].connect();
         assertEquals("abc", clients[i].invoke("abc"));
         clients[i].disconnect();
      }
      
      // Verify Timer has been created.
      invokerDestructionTimer = (Timer) field.get(null);
      assertNotNull(invokerDestructionTimer);
      
      // Verify all Clients are using the same Timer.
      for (int i = 0; i < 50; i++)
      {
         assertEquals("Should be the same Timer", invokerDestructionTimer, field.get(clients[i]));
      }

      Thread.sleep(10000);
      assertEquals(0, InvokerRegistry.getClientInvokers().length);
      
      // Verify Timer has been destroyed.
      invokerDestructionTimer = (Timer) field.get(null);
      assertNull(invokerDestructionTimer);
      
      shutdownServer();
      log.info(getName() + " PASSES");
   }
    
   
   public void testConfigByInvokerLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI + "/?invokerDestructionDelay=10000";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      ClientInvoker invoker1 = client.getInvoker();
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good for first client");
      client.disconnect();
      
      Thread.sleep(5000);
      client = new Client(clientLocator, clientConfig);
      client.connect();
      ClientInvoker invoker2 = client.getInvoker();
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good for second client"); 
      assertSame(invoker2, invoker1);
      
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