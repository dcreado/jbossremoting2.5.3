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

package org.jboss.test.remoting.callback.params;

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
import org.jboss.remoting.callback.CallbackPoller;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit test for JBREM-1084.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Jan 17, 2009
 * </p>
 */
public class UseAllParamsTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(UseAllParamsTestCase.class);
   
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
   
   
   public void testUseAllParamsDefault() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI + "&blockingMode=blocking&callbackPollPeriod=111&maxErrorCount=222";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("callbackPollPeriod", "333");
      clientConfig.put("maxErrorCount", "444");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure callback polling.
      TestCallbackHandler handler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put("maxErrorCount", "555");
      client.addListener(handler, metadata);
      
      // Test setting of parameters in CallbackPoller.
      testParameters(client, false, CallbackPoller.DEFAULT_POLL_PERIOD, 555);
      
      client.removeListener(handler);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUseAllParamsFalseinLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI + "&blockingMode=blocking&callbackPollPeriod=111&maxErrorCount=222&useAllParams=false";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("callbackPollPeriod", "333");
      clientConfig.put("maxErrorCount", "444");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure callback polling.
      TestCallbackHandler handler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put("maxErrorCount", "555");
      client.addListener(handler, metadata);
      
      // Test setting of parameters in CallbackPoller.
      testParameters(client, false, CallbackPoller.DEFAULT_POLL_PERIOD, 555);
      
      client.removeListener(handler);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUseAllParamsFalseinConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI + "&blockingMode=blocking&callbackPollPeriod=111&maxErrorCount=222";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("callbackPollPeriod", "333");
      clientConfig.put("maxErrorCount", "444");
      clientConfig.put(Client.USE_ALL_PARAMS, "false");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure callback polling.
      TestCallbackHandler handler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put("maxErrorCount", "555");
      client.addListener(handler, metadata);
      
      // Test setting of parameters in CallbackPoller.
      testParameters(client, false, CallbackPoller.DEFAULT_POLL_PERIOD, 555);
      
      client.removeListener(handler);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUseAllParamsFalseinMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI + "&blockingMode=blocking&callbackPollPeriod=111&maxErrorCount=222";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("callbackPollPeriod", "333");
      clientConfig.put("maxErrorCount", "444");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure callback polling.
      TestCallbackHandler handler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put("maxErrorCount", "555");
      metadata.put(Client.USE_ALL_PARAMS, "false");
      client.addListener(handler, metadata);
      
      // Test setting of parameters in CallbackPoller.
      testParameters(client, false, CallbackPoller.DEFAULT_POLL_PERIOD, 555);
      
      client.removeListener(handler);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUseAllParamsTrueInLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI + "&blockingMode=blocking&callbackPollPeriod=111&maxErrorCount=222&useAllParams=true";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("callbackPollPeriod", "333");
      clientConfig.put("maxErrorCount", "444");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure callback polling.
      TestCallbackHandler handler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put("maxErrorCount", "555");
      client.addListener(handler, metadata);
      
      // Test setting of parameters in CallbackPoller.
      testParameters(client, true, 333, 555);
      
      client.removeListener(handler);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUseAllParamsTrueInConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI + "&blockingMode=blocking&callbackPollPeriod=111&maxErrorCount=222";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("callbackPollPeriod", "333");
      clientConfig.put("maxErrorCount", "444");
      clientConfig.put(Client.USE_ALL_PARAMS, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure callback polling.
      TestCallbackHandler handler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put("maxErrorCount", "555");
      client.addListener(handler, metadata);
      
      // Test setting of parameters in CallbackPoller.
      testParameters(client, true, 333, 555);
      
      client.removeListener(handler);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testUseAllParamsTrueInMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI + "&blockingMode=blocking&callbackPollPeriod=111&maxErrorCount=222";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("callbackPollPeriod", "333");
      clientConfig.put("maxErrorCount", "444");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Configure callback polling.
      TestCallbackHandler handler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put("maxErrorCount", "555");
      metadata.put(Client.USE_ALL_PARAMS, "true");
      client.addListener(handler, metadata);
      
      // Test setting of parameters in CallbackPoller.
      testParameters(client, true, 333, 555);
      
      client.removeListener(handler);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected void testParameters(Client client,
                                 boolean blockingExpected,
                                 long callbackPollPeriodExpected,
                                 int maxErrorCountExpected)
   throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
   {
      Field field = Client.class.getDeclaredField("callbackPollers");
      field.setAccessible(true);
      Map pollers = (Map) field.get(client);
      assertEquals(1, pollers.size());
      CallbackPoller poller = (CallbackPoller) pollers.values().iterator().next();
      field = CallbackPoller.class.getDeclaredField("blocking");
      field.setAccessible(true);
      boolean blocking = ((Boolean)field.get(poller)).booleanValue();
      field = CallbackPoller.class.getDeclaredField("pollPeriod");
      field.setAccessible(true);
      long callbackPollPeriod = ((Long) field.get(poller)).longValue();
      field = CallbackPoller.class.getDeclaredField("maxErrorCount");
      field.setAccessible(true);
      int maxErrorCount = ((Integer) field.get(poller)).intValue();
      log.info("blocking:           " + blocking);
      log.info("callbackPollPeriod: " + callbackPollPeriod);
      log.info("maxErrorCount:      " + maxErrorCount);
      assertEquals(blockingExpected, blocking);
      assertEquals(callbackPollPeriodExpected, callbackPollPeriod);
      assertEquals(maxErrorCountExpected, maxErrorCount);
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
   
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("received callback");
      }  
   }
}