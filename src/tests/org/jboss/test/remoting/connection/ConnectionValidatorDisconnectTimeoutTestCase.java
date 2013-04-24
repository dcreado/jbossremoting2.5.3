/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.test.remoting.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.ClientDisconnectedException;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.ConnectionValidator;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.ServerFactory;
import org.jboss.remoting.transport.socket.SocketServerInvoker;


/**
 * Unit test for JBREM-1112.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Apr 3, 2009
 * </p>
 */
public class ConnectionValidatorDisconnectTimeoutTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ConnectionValidatorDisconnectTimeoutTestCase.class);
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;
   protected TestConnectionListener serverConnectionListener;

   
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
   
   
   public void testDefaultUnary() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Install ConnectionListener.
      TestConnectionListener clientConnectionListener = new TestConnectionListener("CLIENT");
      client.addConnectionListener(clientConnectionListener);
      
      // Wait for broken connection and test.
      Thread.sleep(4000);
      assertTrue(serverConnectionListener.notified);
      assertTrue(serverConnectionListener.throwable instanceof ClientDisconnectedException);
      assertTrue(clientConnectionListener.notified);
      assertTrue(clientConnectionListener.throwable instanceof Exception);
      assertEquals("Could not connect to server!", ((Exception)clientConnectionListener.throwable).getMessage());
      
      client.removeConnectionListener(clientConnectionListener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testDefaultFirstBinary() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Install ConnectionListener.
      TestConnectionListener clientConnectionListener = new TestConnectionListener("CLIENT");
      client.addConnectionListener(clientConnectionListener, 500);
      
      // Wait for broken connection and test.
      Thread.sleep(4000);
      assertTrue(serverConnectionListener.notified);
      assertTrue(serverConnectionListener.throwable instanceof ClientDisconnectedException);
      assertTrue(clientConnectionListener.notified);
      assertTrue(clientConnectionListener.throwable instanceof Exception);
      assertEquals("Could not connect to server!", ((Exception)clientConnectionListener.throwable).getMessage());
      
      client.removeConnectionListener(clientConnectionListener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testDefaultSecondBinary() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Install ConnectionListener.
      TestConnectionListener clientConnectionListener = new TestConnectionListener("CLIENT");
      client.addConnectionListener(clientConnectionListener, new HashMap());
      
      // Wait for broken connection and test.
      Thread.sleep(4000);
      assertTrue(serverConnectionListener.notified);
      assertTrue(serverConnectionListener.throwable instanceof ClientDisconnectedException);
      assertTrue(clientConnectionListener.notified);
      assertTrue(clientConnectionListener.throwable instanceof Exception);
      assertEquals("Could not connect to server!", ((Exception)clientConnectionListener.throwable).getMessage());
      
      client.removeConnectionListener(clientConnectionListener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testZeroInvokerLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&" + Client.USE_ALL_PARAMS + "=true";
      clientLocatorURI += "&" + ConnectionValidator.FAILURE_DISCONNECT_TIMEOUT + "=0";
      log.info("clientLocatorURI: " + clientLocatorURI);
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Install ConnectionListener.
      TestConnectionListener clientConnectionListener = new TestConnectionListener("CLIENT");
      client.addConnectionListener(clientConnectionListener, new HashMap());
      
      // Wait for broken connection and test.
      Thread.sleep(12000);
      assertTrue(serverConnectionListener.notified);
      assertNull(serverConnectionListener.throwable);
      assertTrue(clientConnectionListener.notified);
      assertTrue(clientConnectionListener.throwable instanceof Exception);
      assertEquals("Could not connect to server!", ((Exception)clientConnectionListener.throwable).getMessage());
      
      client.removeConnectionListener(clientConnectionListener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNonZeroInvokerLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&" + Client.USE_ALL_PARAMS + "=true";
      clientLocatorURI += "&" + ConnectionValidator.FAILURE_DISCONNECT_TIMEOUT + "=10000";
      log.info("clientLocatorURI: " + clientLocatorURI);
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Install ConnectionListener.
      TestConnectionListener clientConnectionListener = new TestConnectionListener("CLIENT");
      client.addConnectionListener(clientConnectionListener, new HashMap());
      
      // Wait for broken connection and test.
      Thread.sleep(4000);
      assertTrue(serverConnectionListener.notified);
      assertTrue(serverConnectionListener.throwable instanceof ClientDisconnectedException);
      assertTrue(clientConnectionListener.notified);
      assertTrue(clientConnectionListener.throwable instanceof Exception);
      assertEquals("Could not connect to server!", ((Exception)clientConnectionListener.throwable).getMessage());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testZeroConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      clientConfig.put(ConnectionValidator.FAILURE_DISCONNECT_TIMEOUT, "0");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Install ConnectionListener.
      TestConnectionListener clientConnectionListener = new TestConnectionListener("CLIENT");
      client.addConnectionListener(clientConnectionListener, new HashMap());
      
      // Wait for broken connection and test.
      Thread.sleep(12000);
      assertTrue(serverConnectionListener.notified);
      assertNull(serverConnectionListener.throwable);
      assertTrue(clientConnectionListener.notified);
      assertTrue(clientConnectionListener.throwable instanceof Exception);
      assertEquals("Could not connect to server!", ((Exception)clientConnectionListener.throwable).getMessage());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNonZeroConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      clientConfig.put(ConnectionValidator.FAILURE_DISCONNECT_TIMEOUT, "10000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Install ConnectionListener.
      TestConnectionListener clientConnectionListener = new TestConnectionListener("CLIENT");
      client.addConnectionListener(clientConnectionListener, new HashMap());
      
      // Wait for broken connection and test.
      Thread.sleep(4000);
      assertTrue(serverConnectionListener.notified);
      assertTrue(serverConnectionListener.throwable instanceof ClientDisconnectedException);
      assertTrue(clientConnectionListener.notified);
      assertTrue(clientConnectionListener.throwable instanceof Exception);
      assertEquals("Could not connect to server!", ((Exception)clientConnectionListener.throwable).getMessage());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testZeroMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Install ConnectionListener.
      TestConnectionListener clientConnectionListener = new TestConnectionListener("CLIENT");
      HashMap metadata = new HashMap();
      metadata.put(ConnectionValidator.FAILURE_DISCONNECT_TIMEOUT, "0");
      client.addConnectionListener(clientConnectionListener, metadata);
      
      // Wait for broken connection and test.
      Thread.sleep(10000);
      assertTrue(serverConnectionListener.notified);
      assertNull(serverConnectionListener.throwable);
      assertTrue(clientConnectionListener.notified);
      assertTrue(clientConnectionListener.throwable instanceof Exception);
      assertEquals("Could not connect to server!", ((Exception)clientConnectionListener.throwable).getMessage());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNonZeroMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Install ConnectionListener.
      TestConnectionListener clientConnectionListener = new TestConnectionListener("CLIENT");
      HashMap metadata = new HashMap();
      metadata.put(ConnectionValidator.FAILURE_DISCONNECT_TIMEOUT, "10000");
      client.addConnectionListener(clientConnectionListener, metadata);
      
      // Wait for broken connection and test.
      Thread.sleep(4000);
      assertTrue(serverConnectionListener.notified);
      assertTrue(serverConnectionListener.throwable instanceof ClientDisconnectedException);
      assertTrue(clientConnectionListener.notified);
      assertTrue(clientConnectionListener.throwable instanceof Exception);
      assertEquals("Could not connect to server!", ((Exception)clientConnectionListener.throwable).getMessage());
      
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
      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            InvokerRegistry.registerInvokerFactories("socket", org.jboss.remoting.transport.socket.TransportClientFactory.class, TestServerInvokerFactory.class);
            return null;
         }
      });
      
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port + "/?x=x";
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "&" + metadata;
      }
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put("leasePeriod", "2000");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
      serverConnectionListener = new TestConnectionListener("SERVER");
      connector.addConnectionListener(serverConnectionListener);
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
   
   
   static class TestServerInvoker extends SocketServerInvoker
   {
      public TestServerInvoker(InvokerLocator locator, Map configuration)
      {
         super(locator, configuration);  
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         Object param = invocation.getParameter();

         // check to see if this is a is alive ping
         if ("$PING$".equals(param))
         {
            Map metadata = invocation.getRequestPayload();
            if (metadata != null)
            {
               String invokerSessionId = (String) metadata.get(INVOKER_SESSION_ID);
               if (invokerSessionId != null)
               {
                  // Comes from ConnectionValidator configured to tie validation with lease.
                  log.info(this + " responding FALSE to $PING$ for invoker sessionId " + invokerSessionId);
                  return Boolean.FALSE;
               }
            }
         }

         return super.invoke(invocation);
      }
   }
   
   
   public static class TestServerInvokerFactory implements ServerFactory
   {
      public ServerInvoker createServerInvoker(InvokerLocator locator, Map config) throws IOException
      {
         return new TestServerInvoker(locator, config);
      }

      public boolean supportsSSL()
      {
         return false;
      }
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

   
   static class TestConnectionListener implements ConnectionListener
   {
      public boolean notified;
      public Throwable throwable;
      String name;
      
      TestConnectionListener(String name)
      {
         this.name = name;
      }
      
      public void handleConnectionException(Throwable throwable, Client client)
      {
         notified = true;
         this.throwable = throwable;
         log.info(this + " NOTIFIED, throwable = " + throwable);
      }
      
      public String toString()
      {
         return "TestConnectionListener[" + name + "]";
      }
   }
}