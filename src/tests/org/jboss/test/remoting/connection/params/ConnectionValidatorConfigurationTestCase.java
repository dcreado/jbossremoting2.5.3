/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.test.remoting.connection.params;

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
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit test for JBREM-1082.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Jan 17, 2009
 * </p>
 */
public class ConnectionValidatorConfigurationTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ConnectionValidatorConfigurationTestCase.class);
   
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
   
   
   public void testUseLocatorParamsDefault() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&validatorPingPeriod=111&validatorPingTimeout=222&tieToLease=false";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("validatorPingPeriod", "333");
      clientConfig.put("validatorPingTimeout", "444");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      HashMap metadata = new HashMap();
      metadata.put("validatorPingTimeout", "555");
      client.addConnectionListener(listener, metadata);
      
      // Test setting of parameters in ConnectionListener.
      doTestParameters(client, 333, 555, true);
      
      client.removeConnectionListener(listener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUseLocatorParamsFalseInLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&validatorPingPeriod=111&validatorPingTimeout=222&tieToLease=false";
      clientLocatorURI += "&" + Client.USE_ALL_PARAMS + "=false";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("validatorPingPeriod", "333");
      clientConfig.put("validatorPingTimeout", "444");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      HashMap metadata = new HashMap();
      metadata.put("validatorPingTimeout", "555");
      client.addConnectionListener(listener, metadata);
      
      // Test setting of parameters in ConnectionListener.
      doTestParameters(client, 333, 555, true);
      
      client.removeConnectionListener(listener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUseLocatorParamsFalseInConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&validatorPingPeriod=111&validatorPingTimeout=222&tieToLease=false";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("validatorPingPeriod", "333");
      clientConfig.put("validatorPingTimeout", "444");
      clientConfig.put(Client.USE_ALL_PARAMS, "false");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      HashMap metadata = new HashMap();
      metadata.put("validatorPingTimeout", "555");
      client.addConnectionListener(listener, metadata);
      
      // Test setting of parameters in ConnectionListener.
      doTestParameters(client, 333, 555, true);
      
      client.removeConnectionListener(listener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUseLocatorParamsFalseInMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&validatorPingPeriod=111&validatorPingTimeout=222&tieToLease=false";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("validatorPingPeriod", "333");
      clientConfig.put("validatorPingTimeout", "444");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      HashMap metadata = new HashMap();
      metadata.put("validatorPingTimeout", "555");
      metadata.put(Client.USE_ALL_PARAMS, "false");
      client.addConnectionListener(listener, metadata);
      
      // Test setting of parameters in ConnectionListener.
      doTestParameters(client, 333, 555, true);
      
      client.removeConnectionListener(listener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUseLocatorParamsTrueInLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&validatorPingPeriod=111&validatorPingTimeout=222&tieToLease=false";
      clientLocatorURI += "&" + Client.USE_ALL_PARAMS + "=true";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("validatorPingPeriod", "333");
      clientConfig.put("validatorPingTimeout", "444");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      HashMap metadata = new HashMap();
      metadata.put("validatorPingTimeout", "555");
      client.addConnectionListener(listener, metadata);
      
      // Test setting of parameters in ConnectionListener.
      doTestParameters(client, 333, 555, false);
      
      client.removeConnectionListener(listener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   

   public void testUseLocatorParamsTrueInConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&validatorPingPeriod=111&validatorPingTimeout=222&tieToLease=false";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("validatorPingPeriod", "333");
      clientConfig.put("validatorPingTimeout", "444");
      clientConfig.put(Client.USE_ALL_PARAMS, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      HashMap metadata = new HashMap();
      metadata.put("validatorPingTimeout", "555");
      client.addConnectionListener(listener, metadata);
      
      // Test setting of parameters in ConnectionListener.
      doTestParameters(client, 333, 555, false);
      
      client.removeConnectionListener(listener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUseLocatorParamsTrueInMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&validatorPingPeriod=111&validatorPingTimeout=222&tieToLease=false";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("validatorPingPeriod", "333");
      clientConfig.put("validatorPingTimeout", "444");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      HashMap metadata = new HashMap();
      metadata.put("validatorPingTimeout", "555");
      metadata.put(Client.USE_ALL_PARAMS, "true");
      client.addConnectionListener(listener, metadata);
      
      // Test setting of parameters in ConnectionListener.
      doTestParameters(client, 333, 555, false);
      
      client.removeConnectionListener(listener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUseLocatorParamsTrueInLocatorAllParamsInLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&validatorPingPeriod=111&validatorPingTimeout=222&tieToLease=false";
      clientLocatorURI += "&" + Client.USE_ALL_PARAMS + "=true";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      HashMap metadata = new HashMap();
      client.addConnectionListener(listener, metadata);
      
      // Test setting of parameters in ConnectionListener.
      doTestParameters(client, 111, 222, false);
      
      client.removeConnectionListener(listener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testExplicitPingPeriodOverridesLocatorAndConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&validatorPingPeriod=111";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.USE_ALL_PARAMS, "true");
      clientConfig.put("validatorPingPeriod", "222");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, 333);
      
      // Test setting of parameters in ConnectionListener.
      doTestParameters(client, 333, 1000, true);
      
      client.removeConnectionListener(listener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUnaryLocatorOverridesWithUseAllParams() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&validatorPingPeriod=111";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.USE_ALL_PARAMS, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener);
      
      // Test setting of parameters in ConnectionListener.
      doTestParameters(client, 111, 1000, true);
      
      client.removeConnectionListener(listener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUnaryLocatorDoesntOverrideWithoutUseAllParams() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&validatorPingPeriod=111";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener);
      
      // Test setting of parameters in ConnectionListener.
      doTestParameters(client, 2000, 1000, true);
      
      client.removeConnectionListener(listener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   
   public void testUnaryConfigOverrides() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("validatorPingPeriod", "111");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener);
      
      // Test setting of parameters in ConnectionListener.
      doTestParameters(client, 111, 1000, true);
      
      client.removeConnectionListener(listener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUnaryDefault() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener);
      
      // Test setting of parameters in ConnectionListener.
      doTestParameters(client, 2000, 1000, true);
      
      client.removeConnectionListener(listener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   
   protected void doTestParameters(Client client,
                                   int pingPeriodExpected,
                                   int pingTimeoutExpected,
                                   boolean tieToLeaseExpected)
   throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
   {
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long)field.get(validator)).longValue();
      field = ConnectionValidator.class.getDeclaredField("pingTimeout");
      field.setAccessible(true);
      int pingTimeout = ((Integer) field.get(validator)).intValue();
      field = ConnectionValidator.class.getDeclaredField("tieToLease");
      field.setAccessible(true);
      boolean tieToLease = ((Boolean) field.get(validator)).booleanValue();
      log.info("pingPeriod:  " + pingPeriod);
      log.info("pingTimeout: " + pingTimeout);
      log.info("tieToLease:  " + tieToLease);
      assertEquals(pingPeriodExpected, pingPeriod);
      assertEquals(pingTimeoutExpected, pingTimeout);
      assertEquals(tieToLeaseExpected, tieToLease);
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
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "/?" + metadata;
      }
      else
      {
         locatorURI += "/?" + "x=y";
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
      public void handleConnectionException(Throwable throwable, Client client)
      {
      }
   }
}