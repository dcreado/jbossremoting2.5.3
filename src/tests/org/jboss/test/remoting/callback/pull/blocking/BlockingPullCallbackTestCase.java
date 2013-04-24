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
package org.jboss.test.remoting.callback.pull.blocking;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * Tests the blocking mode for pull callbacks.
 * 
 * See JBREM-641.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3015 $
 * <p>
 * Copyright May 2, 2007
 * </p>
 */
public class BlockingPullCallbackTestCase extends TestCase
{
   public static int port;
   
   private static Logger log = Logger.getLogger(BlockingPullCallbackTestCase.class);
   
   private static final String INVOCATION_TEST =     "invocationTest";
   private static final String CALLBACK_TEST =       "callbackTest";
   private static final String COUNTER =             "counter";
   private static final String CALLBACK_DELAY_KEY =  "callbackDelay";
   
   private static boolean firstTime = true;
   
   // remoting server connector
   private Connector connector;
   private InvokerLocator serverLocator;
   private SampleInvocationHandler invocationHandler;

   
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

   
   /**
    * Shuts down the server
    */
   public void tearDown()
   {
      if (connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }
   
   
   /**
    * Tests blocking and nonblocking direct calls to Client.getCallbacks().
    */
   public void testBlockingPullCallback() throws Throwable
   {
      log.info("entering " + getName());
      int CALLBACK_DELAY = 5000;
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler);
      log.info("client added callback handler for pull callbacks");
      
      // Test for good connection.
      Integer count = new Integer(17);
      HashMap metadata = new HashMap();
      metadata.put(COUNTER, count);
      Integer response = (Integer) client.invoke(INVOCATION_TEST, metadata);
      assertEquals(17, response.intValue());
      log.info("client.invoke(INVOCATION_TEST, metadata) successful");

      // Test nonblocking callbacks.
      HashMap pullerMetadata = new HashMap();
      pullerMetadata.put(CALLBACK_DELAY_KEY, Integer.toString(CALLBACK_DELAY));
      pullerMetadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.NONBLOCKING);
      CallbackPuller puller = new CallbackPuller(client, callbackHandler, pullerMetadata);
      puller.start();
      Thread.sleep(2000);
      // Should have received empty list of callbacks.
      assertTrue(puller.done);
      assertNotNull(puller.callbacks);
      assertTrue(puller.callbacks.isEmpty());
      
      // Drain stored callback.
      Thread.sleep(CALLBACK_DELAY);
      metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.NONBLOCKING);
      List callbacks = client.getCallbacks(callbackHandler, metadata);
      assertNotNull(callbacks);
      assertEquals(1, callbacks.size());
      
      // Test blocking callbacks.
      pullerMetadata.clear();
      pullerMetadata.put(CALLBACK_DELAY_KEY, Integer.toString(CALLBACK_DELAY));
      pullerMetadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
      pullerMetadata.put(ServerInvoker.BLOCKING_TIMEOUT, Integer.toString(CALLBACK_DELAY * 2));
      puller = new CallbackPuller(client, callbackHandler, pullerMetadata);
      puller.start();
      Thread.sleep(2000);
      assertFalse(puller.done);
      // Should not have returned from getCallbacks() yet.
      assertNull(puller.callbacks);
      Thread.sleep(CALLBACK_DELAY);
      assertTrue(puller.done);
      assertNotNull(puller.callbacks);
      assertEquals(1, puller.callbacks.size());
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   /**
    * Tests configuration of blocking timeout in server configuration map.
    */
   public void testBlockingPullCallbackServerConfiguration() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      
      int blockingTimeout = ServerInvoker.DEFAULT_BLOCKING_TIMEOUT + 2000;
      config.put(ServerInvoker.BLOCKING_TIMEOUT, Integer.toString(blockingTimeout));
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler);
      log.info("client added callback handler for pull callbacks");
      
      // Test for good connection.
      Integer count = new Integer(17);
      HashMap metadata = new HashMap();
      metadata.put(COUNTER, count);
      Integer response = (Integer) client.invoke(INVOCATION_TEST, metadata);
      assertEquals(17, response.intValue());
      log.info("client.invoke(INVOCATION_TEST, metadata) successful");
      
      // Test blocking callbacks.
      //
      // Set CALLBACK_DELAY == default blocking timeout + 1000.  If the default blocking
      // timeout were in effect, Client.getCallbacks() would time out and return without
      // getting a callback.
      int CALLBACK_DELAY = blockingTimeout - 1000;
      Map pullerMetadata = new HashMap();
      pullerMetadata.put(CALLBACK_DELAY_KEY, Integer.toString(CALLBACK_DELAY));
      pullerMetadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
      CallbackPuller puller = new CallbackPuller(client, callbackHandler, pullerMetadata);
      puller.start();
      Thread.sleep(CALLBACK_DELAY - 1000);
      
      // Callback has not been created yet, so should not have returned from getCallbacks().      
      assertFalse(puller.done);
      assertNull(puller.callbacks);
      Thread.sleep(2000);
      
      // Callback has been created, so should have returned from getCallbacks().
      assertTrue(puller.done);
      assertNotNull(puller.callbacks);
      assertEquals(1, puller.callbacks.size());
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   /**
    * Tests configuration of blocking timeout by Client.addListener() metadata.
    * In this case, a CallbackPoller is created.
    */
   public void testBlockingCallbackPollerInitialMetadataConfiguration() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      int serverBlockingTimeout = ServerInvoker.DEFAULT_BLOCKING_TIMEOUT + 2000;
      config.put(ServerInvoker.BLOCKING_TIMEOUT, Integer.toString(serverBlockingTimeout));
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test for good connection.
      Integer count = new Integer(17);
      HashMap metadata = new HashMap();
      metadata.put(COUNTER, count);
      Integer response = (Integer) client.invoke(INVOCATION_TEST, metadata);
      assertEquals(17, response.intValue());
      log.info("client.invoke(INVOCATION_TEST, metadata) successful");
      
      // Test blocking callbacks.
      //
      // Reset blocking timeout to server blocking timeout + 2000, and set
      // CALLBACK_DELAY to server blocking timeout + 1000.  If the server's blocking
      // timeout were in effect, Client.getCallbacks() would time out and return without
      // getting a callback.
      int clientBlockingTimeout = serverBlockingTimeout + 2000;
      int CALLBACK_DELAY = serverBlockingTimeout + 1000;
      metadata.clear();
      metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
      metadata.put(ServerInvoker.BLOCKING_TIMEOUT, Integer.toString(clientBlockingTimeout));
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler, metadata);
      log.info("client added callback handler for pull callbacks");
      
      // Create callback.
      CallbackCreator creator = new CallbackCreator(client, CALLBACK_DELAY);
      creator.start();
      Thread.sleep(CALLBACK_DELAY - 1000);
      
      // Callback has not been created yet, so should not have returned from getCallbacks().
      assertEquals(0, callbackHandler.callbacks.size());
      Thread.sleep(2000);
      
      // Callback has been created, so should have returned from getCallbacks().
      assertEquals(1, callbackHandler.callbacks.size());
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   /**
    * Tests configuration of blocking timeout by Client.getCallbacks() metadata.
    */
   public void testBlockingPullCallbackInvocationMetadataConfiguration() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      
      int serverBlockingTimeout = ServerInvoker.DEFAULT_BLOCKING_TIMEOUT + 2000;
      config.put(ServerInvoker.BLOCKING_TIMEOUT, Integer.toString(serverBlockingTimeout));
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler);
      log.info("client added callback handler for pull callbacks");
      
      // Test for good connection.
      Integer count = new Integer(17);
      HashMap metadata = new HashMap();
      metadata.put(COUNTER, count);
      Integer response = (Integer) client.invoke(INVOCATION_TEST, metadata);
      assertEquals(17, response.intValue());
      log.info("client.invoke(INVOCATION_TEST, metadata) successful");
      
      // Test blocking callbacks.
      //
      // Reset blocking timeout to server blocking timeout + 2000, and set
      // CALLBACK_DELAY to server blocking timeout + 1000.  If the server's blocking
      // timeout were in effect, Client.getCallbacks() would time out and return without
      // getting a callback.
      int clientBlockingTimeout = serverBlockingTimeout + 2000;
      int CALLBACK_DELAY = serverBlockingTimeout + 1000;
      Map pullerMetadata = new HashMap();
      pullerMetadata.put(CALLBACK_DELAY_KEY, Integer.toString(CALLBACK_DELAY));
      pullerMetadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
      pullerMetadata.put(ServerInvoker.BLOCKING_TIMEOUT, Integer.toString(clientBlockingTimeout));
      CallbackPuller puller = new CallbackPuller(client, callbackHandler, pullerMetadata);
      puller.start();
      Thread.sleep(CALLBACK_DELAY - 1000);
      
      // Callback has not been created yet, so should not have returned from getCallbacks().      
      assertFalse(puller.done);
      assertNull(puller.callbacks);
      Thread.sleep(2000);
      
      // Callback has been created, so should have returned from getCallbacks().
      assertTrue(puller.done);
      assertNotNull(puller.callbacks);
      assertEquals(1, puller.callbacks.size());
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   /**
    * Tests CallbackPoller in nonblocking mode.
    */
   public void testNonBlockingCallbackPoller() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      int CALLBACK_DELAY = 5000;
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      Map metadata = new HashMap();
      metadata.put(CallbackPoller.CALLBACK_POLL_PERIOD, Integer.toString(CALLBACK_DELAY));
      metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.NONBLOCKING);
      client.addListener(callbackHandler, metadata);
      log.info("client added callback handler for pull callbacks");
      
      // Test for good connection.
      Integer count = new Integer(17);
      metadata.clear();
      metadata = new HashMap();
      metadata.put(COUNTER, count);
      Integer response = (Integer) client.invoke(INVOCATION_TEST, metadata);
      assertEquals(17, response.intValue());
      log.info("client.invoke(INVOCATION_TEST, metadata) successful");
      
      // Test nonblocking behavior.
      Thread.sleep(1000);
      // Callback will be created after CallbackPoller's first poll.
      CallbackCreator creator = new CallbackCreator(client, CALLBACK_DELAY);
      creator.start();
      Thread.sleep(CALLBACK_DELAY);
      assertEquals(0, callbackHandler.callbacks.size());
      Thread.sleep(2000);
      // CallbackPoller hasn't made second poll yet.
      assertEquals(0, callbackHandler.callbacks.size());
      Thread.sleep(CALLBACK_DELAY); 
      assertEquals(1, callbackHandler.callbacks.size());
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }

   
   /**
    * Tests CallbackPoller in blocking mode.
    */
   public void testBlockingCallbackPoller() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      int CALLBACK_DELAY = 5000;
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      Map metadata = new HashMap();
      metadata.put(CallbackPoller.CALLBACK_POLL_PERIOD, Integer.toString(CALLBACK_DELAY));
      metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
      client.addListener(callbackHandler, metadata);
      log.info("client added callback handler for pull callbacks");
      
      // Test for good connection.
      Integer count = new Integer(17);
      metadata.clear();
      metadata = new HashMap();
      metadata.put(COUNTER, count);
      Integer response = (Integer) client.invoke(INVOCATION_TEST, metadata);
      assertEquals(17, response.intValue());
      log.info("client.invoke(INVOCATION_TEST, metadata) successful");
      
      // Test blocking behavior.
      CallbackCreator creator = new CallbackCreator(client, CALLBACK_DELAY);
      creator.start();
      assertEquals(0, callbackHandler.callbacks.size());
      Thread.sleep(CALLBACK_DELAY + 1000);
      assertEquals(1, callbackHandler.callbacks.size());
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }

   
   /**
    * Tests blocking timeout set on server.
    */
   public void testBlockingCallbackPollerShutdownServerConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.BLOCKING_TIMEOUT, "4000");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      final Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test for good connection.
      Integer count = new Integer(17);
      Map metadata = new HashMap();
      metadata.clear();
      metadata = new HashMap();
      metadata.put(COUNTER, count);
      Integer response = (Integer) client.invoke(INVOCATION_TEST, metadata);
      assertEquals(17, response.intValue());
      log.info("client.invoke(INVOCATION_TEST, metadata) successful");
      
      // Register callback handler.
      Field field = Client.class.getDeclaredField("callbackPollers");
      field.setAccessible(true);
      Map callbackPollers = (Map) field.get(client);
      final SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
      client.addListener(callbackHandler, metadata);
      assertEquals(1, callbackPollers.size());
      CallbackPoller callbackPoller = (CallbackPoller) callbackPollers.values().iterator().next();
      field = CallbackPoller.class.getDeclaredField("blockingPollerThread");
      field.setAccessible(true);
      Thread blockingPollerThread = (Thread) field.get(callbackPoller);
      assertNotNull(blockingPollerThread);
      log.info("client added callback handler for pull callbacks");
      
      // Test blocking timeout.
      new Thread()
      {
         public void run()
         {
            try
            {
               client.removeListener(callbackHandler);
            }
            catch (Throwable e)
            {
               e.printStackTrace();
            }
         }
      }.start();
      
      Thread.sleep(6000);
      assertFalse(blockingPollerThread.isAlive());
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   /**
    * Tests blocking timeout set on client.
    */
   public void testBlockingCallbackPollerShutdownClientConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.BLOCKING_TIMEOUT, "4000");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      final Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test for good connection.
      Integer count = new Integer(17);
      Map metadata = new HashMap();
      metadata.clear();
      metadata = new HashMap();
      metadata.put(COUNTER, count);
      Integer response = (Integer) client.invoke(INVOCATION_TEST, metadata);
      assertEquals(17, response.intValue());
      log.info("client.invoke(INVOCATION_TEST, metadata) successful");
      
      // Register callback handler.
      Field field = Client.class.getDeclaredField("callbackPollers");
      field.setAccessible(true);
      Map callbackPollers = (Map) field.get(client);
      final SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
      metadata.put(ServerInvoker.BLOCKING_TIMEOUT, "10000");
      client.addListener(callbackHandler, metadata);
      assertEquals(1, callbackPollers.size());
      CallbackPoller callbackPoller = (CallbackPoller) callbackPollers.values().iterator().next();
      field = CallbackPoller.class.getDeclaredField("blockingPollerThread");
      field.setAccessible(true);
      Thread blockingPollerThread = (Thread) field.get(callbackPoller);
      assertNotNull(blockingPollerThread);
      log.info("client added callback handler for pull callbacks");
      
      // Test blocking timeout.
      new Thread()
      {
         public void run()
         {
            try
            {
               client.removeListener(callbackHandler);
            }
            catch (Throwable e)
            {
               e.printStackTrace();
            }
         }
      }.start();
      
      Thread.sleep(2000);
      assertTrue(blockingPollerThread.isAlive());
      Thread.sleep(4000);
      assertTrue(blockingPollerThread.isAlive());
      Thread.sleep(15000);
      assertFalse(blockingPollerThread.isAlive());
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   static class SampleInvocationHandler implements ServerInvocationHandler
   {
      public Set callbackHandlers = new HashSet();
      private int counter = 0;
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         log.info("Adding callback listener.");
         callbackHandlers.add(callbackHandler);
      }

      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         Object payload = invocation.getParameter();
         if (INVOCATION_TEST.equals(payload))
         {
            Map requestMap = invocation.getRequestPayload();
            Integer counter = (Integer) requestMap.get(COUNTER);
            return counter;
         }
         else if (CALLBACK_TEST.equals(payload))
         {
            new Thread()
            {
               public void run()
               {
                  try
                  {
                     Map requestMap = invocation.getRequestPayload();
                     int delay = Integer.parseInt((String) requestMap.get(CALLBACK_DELAY_KEY));
                     Thread.sleep(delay);
                     Iterator it = callbackHandlers.iterator();
                     while (it.hasNext())
                     {
                        InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler) it.next();
                        log.info("sending callback: " + ++counter);
                        callbackHandler.handleCallback(new Callback("callback"));
                     }
                     log.info("sent callback");
                  }
                  catch (HandleCallbackException e)
                  {
                     log.error("Unable to send callback");
                  }
                  catch (InterruptedException e)
                  {
                     e.printStackTrace();
                  }
               }
            }.start();

            return null;
         }
         else
         {
            throw new Exception("unrecognized invocation: " + payload);
         }
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class SimpleCallbackHandler implements InvokerCallbackHandler
   {
      List callbacks = new ArrayList();
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         callbacks.add(callback);
         log.info("received callback");
      }
   }
   
   static class CallbackPuller extends Thread
   {
      Client client;
      InvokerCallbackHandler callbackHandler;
      Map metadata;
      List callbacks;
      boolean done;
      
      public CallbackPuller(Client client,
                            InvokerCallbackHandler callbackHandler,
                            Map metadata)
      {
         this.client = client;
         this.callbackHandler = callbackHandler;
         this.metadata = metadata;
      }
      
      public void run()
      {

         try
         {
            client.invoke(CALLBACK_TEST, metadata);
            log.info("back from client.invoke(CALLBACK_TEST, metadata)");
            callbacks = client.getCallbacks(callbackHandler, metadata);
            log.info("callbacks count: " + callbacks.size());
            done = true;
         }
         catch (Throwable e)
         {
            e.printStackTrace();
         }
      }
   }
   
   static class CallbackCreator extends Thread
   {
      Client client;
      int delay;
      boolean done;
      
      public CallbackCreator(Client client,int delay)
      {
         this.client = client;
         this.delay = delay;
      }
      
      public void run()
      {

         try
         {
            HashMap metadata = new HashMap();
            metadata.put(CALLBACK_DELAY_KEY, Integer.toString(delay));
            client.invoke(CALLBACK_TEST, metadata);
            log.info("back from client.invoke(CALLBACK_TEST)");
            done = true;
         }
         catch (Throwable e)
         {
            e.printStackTrace();
         }
      }
   }
}