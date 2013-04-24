/*
* JBoss, Home of Professional Open Source
* Copyright 2009, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.remoting.connection.identity;

import java.io.IOException;
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
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.ConnectionValidator;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.LeasePinger;
import org.jboss.remoting.MicroRemoteClientInvoker;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.ClientFactory;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.ServerFactory;
import org.jboss.remoting.transport.socket.SocketClientInvoker;
import org.jboss.remoting.transport.socket.SocketServerInvoker;


/**
/**
 * Unit test for JBREM-1132.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright May 08, 2009
 * </p>
 */
public class ConnectionIdentityTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ConnectionIdentityTestCase.class);
   
   private static boolean firstTime = true;
   
   protected static final int LEASE_PERIOD = 2000;
   protected static final int VALIDATOR_PING_TIMEOUT = 1000;
   protected static final int VALIDATOR_PING_PERIOD = 2000;
   protected static final int PING_PERIODS_TO_WAIT = 2;
   
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
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);
      }
      InvokerRegistry.registerInvokerFactories(getTransport(), TestClientFactory.class, TestServerFactory.class);
   }

   
   public void tearDown()
   {
   }
   
   
   public void testIdentityTrueOneClientRestarts() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, "true");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_PERIOD, Integer.toString(VALIDATOR_PING_PERIOD));
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_TIMEOUT, Integer.toString(VALIDATOR_PING_TIMEOUT));
      clientConfig.put(ConnectionValidator.FAILURE_DISCONNECT_TIMEOUT, "0");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      TestConnectionListener clientConnectionListener = new TestConnectionListener("clientConnectionListener");
      client.connect(clientConnectionListener, null);
      Field field = MicroRemoteClientInvoker.class.getDeclaredField("leasePinger");
      field.setAccessible(true);
      LeasePinger leasePinger1 = (LeasePinger) field.get(client.getInvoker());
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Test client side connection failure notifications.
      int wait = (PING_PERIODS_TO_WAIT + 1) * VALIDATOR_PING_PERIOD + VALIDATOR_PING_TIMEOUT + 2000;
      log.info(getName() + " going to sleep for " + wait + " ms");
      Thread.sleep(wait);
      log.info("checking connection failure notifications");
      assertEquals(1, clientConnectionListener.calledCounter);
      assertTrue(clientConnectionListener.throwable instanceof Exception);
      assertEquals("Could not connect to server!", clientConnectionListener.throwable.getMessage());
      
      // Test server side connection failure notifications.
      wait = 2 * LEASE_PERIOD;
      log.info(getName() + " going to sleep for " + wait + " ms");
      Thread.sleep(wait);
      assertEquals(1, serverConnectionListener.calledCounter);
      assertNull(serverConnectionListener.throwable);
      
      // Verify new LeasePinger is created if Client reconnects.
      client.connect(clientConnectionListener, null);
      assertNotSame(leasePinger1, field.get(client.getInvoker()));
      
      client.removeConnectionListener(clientConnectionListener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testIdentityTrueTwoClientsOneConnectionValidator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, "true");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_PERIOD, Integer.toString(VALIDATOR_PING_PERIOD));
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_TIMEOUT, Integer.toString(VALIDATOR_PING_TIMEOUT));
      clientConfig.put(ConnectionValidator.FAILURE_DISCONNECT_TIMEOUT, "0");
      addExtraClientConfig(clientConfig);
      Client client1 = new Client(clientLocator, clientConfig);
      TestConnectionListener clientConnectionListener1 = new TestConnectionListener("clientConnectionListener1");
      client1.connect(clientConnectionListener1, null);
      Client client2 = new Client(clientLocator, clientConfig);
      TestConnectionListener clientConnectionListener2 = new TestConnectionListener("clientConnectionListener2");
      client2.connect(clientConnectionListener2, null);
      log.info("clients are connected");
      
      // Test connection.
      assertEquals("abc", client1.invoke("abc"));
      assertEquals("abc", client2.invoke("abc"));
      log.info("connections are good");
      
      // Verify Clients share ConnectionValidator.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator connectionValidator1 = (ConnectionValidator) field.get(client1);
      ConnectionValidator connectionValidator2 = (ConnectionValidator) field.get(client2);
      assertSame(connectionValidator1, connectionValidator2);
      
      // Test client side connection failure notifications.
      int wait = (PING_PERIODS_TO_WAIT + 1) * VALIDATOR_PING_PERIOD + VALIDATOR_PING_TIMEOUT + 2000;
      log.info(getName() + " going to sleep for " + wait + " ms");
      Thread.sleep(wait);
      log.info("checking connection failure notifications");
      assertEquals(1, clientConnectionListener1.calledCounter);
      assertTrue(clientConnectionListener1.throwable instanceof Exception);
      assertEquals("Could not connect to server!", clientConnectionListener1.throwable.getMessage());
      assertEquals(1, clientConnectionListener2.calledCounter);
      assertTrue(clientConnectionListener2.throwable instanceof Exception);
      assertEquals("Could not connect to server!", clientConnectionListener2.throwable.getMessage());
      
      client1.removeConnectionListener(clientConnectionListener1);
      client1.disconnect();
      client2.removeConnectionListener(clientConnectionListener2);
      client2.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testIdentityTrueTwoClientsTwoConnectionValidators() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, "true");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_PERIOD, Integer.toString(VALIDATOR_PING_PERIOD));
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_TIMEOUT, Integer.toString(VALIDATOR_PING_TIMEOUT));
      clientConfig.put(ConnectionValidator.FAILURE_DISCONNECT_TIMEOUT, "0");
      addExtraClientConfig(clientConfig);
      Client client1 = new Client(clientLocator, clientConfig);
      TestConnectionListener clientConnectionListener1 = new TestConnectionListener("clientConnectionListener1");
      Map metadata = new HashMap();
      metadata.put("abc", "xyz");
      client1.connect(clientConnectionListener1, metadata);
      Client client2 = new Client(clientLocator, clientConfig);
      TestConnectionListener clientConnectionListener2 = new TestConnectionListener("clientConnectionListener2");
      metadata.put("abc", "123");
      client2.connect(clientConnectionListener2, metadata);
      log.info("clients are connected");
      
      // Test connection.
      assertEquals("abc", client1.invoke("abc"));
      assertEquals("abc", client2.invoke("abc"));
      log.info("connections are good");
      
      // Verify Clients have distinct ConnectionValidators.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator connectionValidator1 = (ConnectionValidator) field.get(client1);
      ConnectionValidator connectionValidator2 = (ConnectionValidator) field.get(client2);
      assertNotSame(connectionValidator1, connectionValidator2);
      
      // Test client side connection failure notifications.
      int wait = (PING_PERIODS_TO_WAIT + 1) * VALIDATOR_PING_PERIOD + VALIDATOR_PING_TIMEOUT + 2000;
      log.info(getName() + " going to sleep for " + wait + " ms");
      Thread.sleep(wait);
      log.info("checking connection failure notifications");
      assertEquals(1, clientConnectionListener1.calledCounter);
      assertTrue(clientConnectionListener1.throwable instanceof Exception);
      assertEquals("Could not connect to server!", clientConnectionListener1.throwable.getMessage());
      assertEquals(1, clientConnectionListener2.calledCounter);
      assertTrue(clientConnectionListener2.throwable instanceof Exception);
      assertEquals("Could not connect to server!", clientConnectionListener2.throwable.getMessage());
      
      client1.removeConnectionListener(clientConnectionListener1);
      client1.disconnect();
      client2.removeConnectionListener(clientConnectionListener2);
      client2.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testIdentityTrueTwoClientsTwoConnectionValidatorsFourConnectionListeners() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, "true");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_PERIOD, Integer.toString(VALIDATOR_PING_PERIOD));
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_TIMEOUT, Integer.toString(VALIDATOR_PING_TIMEOUT));
      clientConfig.put(ConnectionValidator.FAILURE_DISCONNECT_TIMEOUT, "0");
      addExtraClientConfig(clientConfig);
      Client client1 = new Client(clientLocator, clientConfig);
      TestConnectionListener clientConnectionListener1a = new TestConnectionListener("clientConnectionListener1a");
      TestConnectionListener clientConnectionListener1b = new TestConnectionListener("clientConnectionListener1b");
      Map metadata = new HashMap();
      metadata.put("abc", "xyz");
      client1.connect(clientConnectionListener1a, metadata);
      client1.addConnectionListener(clientConnectionListener1b);
      Client client2 = new Client(clientLocator, clientConfig);
      TestConnectionListener clientConnectionListener2a = new TestConnectionListener("clientConnectionListener2a");
      TestConnectionListener clientConnectionListener2b = new TestConnectionListener("clientConnectionListener2b");
      metadata.put("abc", "123");
      client2.connect(clientConnectionListener2a, metadata);
      client2.addConnectionListener(clientConnectionListener2b);
      log.info("clients are connected");
      
      // Test connection.
      assertEquals("abc", client1.invoke("abc"));
      assertEquals("abc", client2.invoke("abc"));
      log.info("connections are good");
      
      // Verify Clients have distinct ConnectionValidators.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator connectionValidator1 = (ConnectionValidator) field.get(client1);
      ConnectionValidator connectionValidator2 = (ConnectionValidator) field.get(client2);
      assertNotSame(connectionValidator1, connectionValidator2);
      
      // Test client side connection failure notifications.
      int wait = (PING_PERIODS_TO_WAIT + 1) * VALIDATOR_PING_PERIOD + VALIDATOR_PING_TIMEOUT + 2000;
      log.info(getName() + " going to sleep for " + wait + " ms");
      Thread.sleep(wait);
      log.info("checking connection failure notifications");
      assertEquals(1, clientConnectionListener1a.calledCounter);
      assertTrue(clientConnectionListener1a.throwable instanceof Exception);
      assertEquals("Could not connect to server!", clientConnectionListener1a.throwable.getMessage());
      assertEquals(1, clientConnectionListener1b.calledCounter);
      assertTrue(clientConnectionListener1b.throwable instanceof Exception);
      assertEquals("Could not connect to server!", clientConnectionListener1b.throwable.getMessage());
      assertEquals(1, clientConnectionListener2a.calledCounter);
      assertTrue(clientConnectionListener2a.throwable instanceof Exception);
      assertEquals("Could not connect to server!", clientConnectionListener2a.throwable.getMessage());
      assertEquals(1, clientConnectionListener2b.calledCounter);
      assertTrue(clientConnectionListener2b.throwable instanceof Exception);
      assertEquals("Could not connect to server!", clientConnectionListener2b.throwable.getMessage());
      
      client1.removeConnectionListener(clientConnectionListener1a);
      client1.removeConnectionListener(clientConnectionListener1b);
      client1.disconnect();
      client2.removeConnectionListener(clientConnectionListener2a);
      client2.removeConnectionListener(clientConnectionListener2b);
      client2.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "test";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer(boolean addUseClientConnectionIdentity, String useClientConnectionIdentity) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port + "/?x=x";
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "&" + metadata;
      }
      if (addUseClientConnectionIdentity)
      {
         locatorURI += "&" + Remoting.USE_CLIENT_CONNECTION_IDENTITY + "=" + useClientConnectionIdentity;
      }
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.setLeasePeriod(LEASE_PERIOD);
      serverConnectionListener = new TestConnectionListener("serverConnectionListener");
      connector.addConnectionListener(serverConnectionListener);
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

   
   static class TestConnectionListener implements ConnectionListener
   {
      String name;
      Throwable throwable;
      int calledCounter;
      
      public TestConnectionListener(String name)
      {
         this.name = name;
      }
      public void handleConnectionException(Throwable throwable, Client client)
      {
         calledCounter++;
         this.throwable = throwable;
         log.info(name + " notified: throwable = " + throwable);
      }  
   }
   
   
   static class TestServerInvoker extends SocketServerInvoker
   {
      int counter;
      
      public TestServerInvoker(InvokerLocator locator, Map configuration)
      {
         super(locator, configuration);
      }
      public TestServerInvoker(InvokerLocator locator)
      {
         super(locator);
      }
    
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         Object param = invocation.getParameter();
//         log.info("TestServerInvoker.invoke() entered: " + param);
         if ("$PING$".equals(param))
         {
            Map metadata = invocation.getRequestPayload();
            if (metadata != null)
            {
//               log.info("metadata: " + metadata);
               String invokerSessionId = (String) metadata.get(INVOKER_SESSION_ID);
               if (invokerSessionId != null)
               {
                  log.info(this + " got a ConnectionValidator $PING$");
                  if (++counter > PING_PERIODS_TO_WAIT)
                  {
                     int wait = 2 * VALIDATOR_PING_TIMEOUT;
                     log.info(this + " going to sleep for " + wait + " ms");
                     Thread.sleep(wait);
                  }
               }
            }
         }
         return super.invoke(invocation);
      }
      
      public String toString()
      {
         String s = super.toString();
         int i = s.indexOf('[');
         return "TestServerInvoker" + s.substring(i);
      }
   }
   
   
   public static class TestClientFactory implements ClientFactory
   {
      public ClientInvoker createClientInvoker(InvokerLocator locator, Map config) throws IOException
      {
         log.info("TestClientFaotory.createClientInvoker() called");
         return new SocketClientInvoker(locator, config);
      }
      public boolean supportsSSL()
      {
         return false;
      }  
   }
   
   
   public static class TestServerFactory implements ServerFactory
   {
      public ServerInvoker createServerInvoker(InvokerLocator locator, Map config) throws IOException
      {
         log.info("TestServerFactory.createServerInvoker() called");
         return new TestServerInvoker(locator, config);
      }
      public boolean supportsSSL()
      {
         return false;
      }
   }
}