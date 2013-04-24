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
package org.jboss.test.remoting.transport.bisocket;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.xml.parsers.DocumentBuilderFactory;

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
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.remoting.transport.bisocket.BisocketClientInvoker;
import org.jboss.remoting.transport.bisocket.BisocketServerInvoker;
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;
import org.jboss.remoting.transport.socket.ServerThread;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import org.jboss.remoting.transport.socket.SocketWrapper;
import org.w3c.dom.Document;


/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 5489 $
 * <p>
 * Copyright Nov 25, 2006
 * </p>
 */
public class BisocketTestCase extends TestCase
{
   public static int port = 5413;
   
   private static Logger log = Logger.getLogger(BisocketTestCase.class);
   
   private static final String INVOCATION_TEST =     "invocationTest";
   private static final String CALLBACK_TEST =       "callbackTest";
   private static final String COUNTER =             "counter";
   
   private static final String TEST_PING_FREQUENCY_STRING = "1000";
   private static final int    TEST_PING_FREQUENCY = 1000;
   private static final String TEST_PING_WINDOW_FACTOR_STRING = "3";
   private static final int    TEST_PING_WINDOW_FACTOR = 3;
   private static final String TEST_MAX_RETRIES_STRING = "5";
   private static final int    TEST_MAX_RETRIES = 5;
   private static final String TEST_CONTROL_CONNECTION_RESTARTS_STRING = "7";
   private static final int    TEST_CONTROL_CONNECTION_RESTARTS = 7;
   private static final String TEST_MAX_POOL_SIZE_STRING = "9";
   private static final int    TEST_MAX_POOL_SIZE = 9;
   
   private static boolean firstTime = true;
   
   // remoting server connector
   private Connector connector;
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
     
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      internalSetUp(port);
   }
   
   
   protected void internalSetUp(int port) throws Exception
   {
      String host = InetAddress.getLocalHost().getHostAddress();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.PING_FREQUENCY, TEST_PING_FREQUENCY_STRING);
      config.put(MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG, TEST_MAX_POOL_SIZE_STRING);
      addExtraServerConfig(config);

      for (int i = 0; i < 5; i++)
      {
         try
         {
            if (i > 0)
            {
               log.info("will retry to start Connector");
            }
            
            connector = new Connector(serverLocator, config);
            connector.create();
            invocationHandler = new SampleInvocationHandler();
            connector.addInvocationHandler("sample", invocationHandler);
            connector.start();
            break;
         }
         catch (Exception e)
         {
            log.info("unable to start Connector for " + serverLocator, e);
            connector.stop();
            Thread.sleep(60000);
         }
      }
      
      if (connector == null || !connector.isStarted())
      {
         log.error("Unable to start Connector");
         throw new Exception("Unable to start Connector: " + serverLocator);
      }
   }

   
   /**
    * Shuts down the server
    */
   public void tearDown()
   {
      connector.stop();
      connector.destroy();
   }
   
   
   public void testConfiguration() throws Throwable
   {
      log.info("entering " + getName());
      connector.stop();
      
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      serverConfig.put(Bisocket.PING_FREQUENCY, TEST_PING_FREQUENCY_STRING);
      serverConfig.put(Bisocket.MAX_RETRIES, TEST_MAX_RETRIES_STRING);
      addExtraServerConfig(serverConfig);
      connector = new Connector(serverLocator, serverConfig);
      connector.create();
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Bisocket.IS_CALLBACK_SERVER, "true");
      clientConfig.put(Bisocket.PING_FREQUENCY, TEST_PING_FREQUENCY_STRING);
      clientConfig.put(Bisocket.PING_WINDOW_FACTOR, TEST_PING_WINDOW_FACTOR_STRING);
      clientConfig.put(Bisocket.MAX_RETRIES, TEST_MAX_RETRIES_STRING);
      clientConfig.put(Bisocket.MAX_CONTROL_CONNECTION_RESTARTS, TEST_CONTROL_CONNECTION_RESTARTS_STRING);

      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      log.info("client added callback handler");
      
      Integer count = new Integer(17);
      HashMap metadata = new HashMap();
      metadata.put(COUNTER, count);
      Integer response = (Integer) client.invoke(INVOCATION_TEST, metadata);
      assertEquals(17, response.intValue());
      
      // Test server invoker configuration.
      // Actually, none of these parameters are used by the server invoker.
      
      // Test callback client invoker configuration.
      Set callbackHandlers = invocationHandler.callbackHandlers;
      assertEquals(1, callbackHandlers.size());
      ServerInvokerCallbackHandler sich = (ServerInvokerCallbackHandler) callbackHandlers.iterator().next();
      Client callbackClient = sich.getCallbackClient();
      assertTrue(callbackClient.getInvoker() instanceof BisocketClientInvoker);
      BisocketClientInvoker callbackClientInvoker = (BisocketClientInvoker) callbackClient.getInvoker();
      assertEquals(TEST_PING_FREQUENCY, callbackClientInvoker.getPingFrequency());
      assertEquals(TEST_MAX_RETRIES, callbackClientInvoker.getMaxRetries());

      // Test client invoker configuration.
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      BisocketClientInvoker clientInvoker = (BisocketClientInvoker) client.getInvoker();
      assertEquals(TEST_PING_FREQUENCY, clientInvoker.getPingFrequency());
      assertEquals(TEST_MAX_RETRIES, clientInvoker.getMaxRetries());
      
      // Test callback server invoker configuration.
      Field field = Client.class.getDeclaredField("callbackConnectors");
      field.setAccessible(true);
      Map callbackConnectorsMap = (Map) field.get(client);
      assertEquals(1, callbackConnectorsMap.size());
      Set callbackConnectorsSet = (Set) callbackConnectorsMap.values().iterator().next();
      assertEquals(1, callbackConnectorsSet.size());
      Connector callbackConnector = (Connector) callbackConnectorsSet.iterator().next();
      assertTrue(connector.getServerInvoker() instanceof BisocketServerInvoker);
      BisocketServerInvoker callbackServerInvoker = (BisocketServerInvoker) callbackConnector.getServerInvoker();
      assertEquals(TEST_PING_FREQUENCY, callbackServerInvoker.getPingFrequency());
      assertEquals(TEST_PING_WINDOW_FACTOR, callbackServerInvoker.getPingWindowFactor());
      field = BisocketServerInvoker.class.getDeclaredField("pingWindow");
      field.setAccessible(true);
      int pingWindow = ((Integer) field.get(callbackServerInvoker)).intValue();
      assertEquals(TEST_PING_WINDOW_FACTOR * TEST_PING_FREQUENCY, pingWindow);
      assertEquals(TEST_MAX_RETRIES, callbackServerInvoker.getSocketCreationRetries());
      assertEquals(TEST_CONTROL_CONNECTION_RESTARTS, callbackServerInvoker.getControlConnectionRestarts());
 
      client.removeListener(callbackHandler);
      client.disconnect();
   }

   
   public void testXMLConfiguration() throws Throwable
   {
      log.info("entering " + getName());
      connector.stop();
      
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(serverConfig);
      connector = new Connector(serverConfig);
      
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("   <invoker transport=\"" + getTransport() + "\">");
      buf.append("      <attribute name=\"serverBindAddress\">" + host + "</attribute>");
      buf.append("      <attribute name=\"serverBindPort\">" + port + "</attribute>");
      buf.append("      <attribute name=\"pingFrequency\" isParam=\"true\">" + TEST_PING_FREQUENCY_STRING + "</attribute>");
      buf.append("      <attribute name=\"pingWindowFactor\" isParam=\"true\">" + TEST_PING_WINDOW_FACTOR_STRING + "</attribute>");
      buf.append("      <attribute name=\"maxRetries\" isParam=\"true\">" + TEST_MAX_RETRIES_STRING + "</attribute>");
      buf.append("      <attribute name=\"maxControlConnectionRestarts\" isParam=\"true\">" + TEST_CONTROL_CONNECTION_RESTARTS_STRING + "</attribute>");
      buf.append("   </invoker>");
      buf.append("</config>");
      log.info(buf);
      ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      InvokerLocator serverLocator = connector.getLocator();
      System.out.println("started connector with locator uri of: " + serverLocator);
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Bisocket.IS_CALLBACK_SERVER, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      log.info("client added callback handler");
      
      Integer count = new Integer(17);
      HashMap metadata = new HashMap();
      metadata.put(COUNTER, count);
      Integer response = (Integer) client.invoke(INVOCATION_TEST, metadata);
      assertEquals(17, response.intValue());
      
      // Test server invoker configuration.
      // Actually, none of these parameters are used by the server invoker.
         
      // Test callback client invoker configuration.
      Set callbackHandlers = invocationHandler.callbackHandlers;
      assertEquals(1, callbackHandlers.size());
      ServerInvokerCallbackHandler sich = (ServerInvokerCallbackHandler) callbackHandlers.iterator().next();
      Client callbackClient = sich.getCallbackClient();
      assertTrue(callbackClient.getInvoker() instanceof BisocketClientInvoker);
      BisocketClientInvoker callbackClientInvoker = (BisocketClientInvoker) callbackClient.getInvoker();
      assertEquals(TEST_PING_FREQUENCY, callbackClientInvoker.getPingFrequency());
      assertEquals(TEST_MAX_RETRIES, callbackClientInvoker.getMaxRetries());
 
      // Test client invoker configuration.
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      BisocketClientInvoker clientInvoker = (BisocketClientInvoker) client.getInvoker();
      assertEquals(TEST_PING_FREQUENCY, clientInvoker.getPingFrequency());
      assertEquals(TEST_MAX_RETRIES, clientInvoker.getMaxRetries());
      
      // Test callback server invoker configuration.
      Field field = Client.class.getDeclaredField("callbackConnectors");
      field.setAccessible(true);
      Map callbackConnectorsMap = (Map) field.get(client);
      assertEquals(1, callbackConnectorsMap.size());
      Set callbackConnectorsSet = (Set) callbackConnectorsMap.values().iterator().next();
      assertEquals(1, callbackConnectorsSet.size());
      Connector callbackConnector = (Connector) callbackConnectorsSet.iterator().next();
      assertTrue(connector.getServerInvoker() instanceof BisocketServerInvoker);
      BisocketServerInvoker callbackServerInvoker = (BisocketServerInvoker) callbackConnector.getServerInvoker();
      assertEquals(TEST_PING_FREQUENCY, callbackServerInvoker.getPingFrequency());
      assertEquals(TEST_PING_WINDOW_FACTOR, callbackServerInvoker.getPingWindowFactor());
      field = BisocketServerInvoker.class.getDeclaredField("pingWindow");
      field.setAccessible(true);
      int pingWindow = ((Integer) field.get(callbackServerInvoker)).intValue();
      assertEquals(TEST_PING_WINDOW_FACTOR * TEST_PING_FREQUENCY, pingWindow);
      assertEquals(TEST_MAX_RETRIES, callbackServerInvoker.getSocketCreationRetries());
      assertEquals(TEST_CONTROL_CONNECTION_RESTARTS, callbackServerInvoker.getControlConnectionRestarts());
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   public void testInvocations() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      
      int invocationCount = 1000;
      HashMap metadata = new HashMap(1);
      for (int i = 0; i < invocationCount; i++)
      {
         Integer count = new Integer(i);
         metadata.put(COUNTER, count);
         Object response = client.invoke(INVOCATION_TEST, metadata);
         response.equals(count);
      }
      
      client.disconnect();
   }
   
   
   public void testInvocationsThenCallbacksThenInvocations() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      
      int invocationCount = 100;
      HashMap metadata = new HashMap(1);
      for (int i = 0; i < invocationCount; i++)
      {
         Integer count = new Integer(i);
         metadata.put(COUNTER, count);
         Object response = client.invoke(INVOCATION_TEST, metadata);
         assertTrue(response.equals(count));
      }
      
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      log.info("client added callback handler");
      
      for (int i = 0; i < invocationCount; i++)
      {
         client.invoke(CALLBACK_TEST);
      }
      
      assertEquals(invocationCount, callbackHandler.callbackCounter);
      
      for (int i = 0; i < invocationCount; i++)
      {
         Integer count = new Integer(i);
         metadata.put(COUNTER, count);
         Object response = client.invoke(INVOCATION_TEST, metadata);
         assertTrue(response.equals(count));
      }
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   public void testAlternatingInvocationsAndCallbacks() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      log.info("client added callback handler");
      
      int invocationCount = 100;
      HashMap metadata = new HashMap(1);
      for (int i = 0; i < invocationCount; i++)
      {
         Integer count = new Integer(i);
         metadata.put(COUNTER, count);
         Object response = client.invoke(INVOCATION_TEST, metadata);
         client.invoke(CALLBACK_TEST);
         assertTrue(response.equals(count));
      }
      
      assertEquals(invocationCount, callbackHandler.callbackCounter);
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   public void testPullCallbacks() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler);
      log.info("client added pull callback handler");
      
      HashMap metadata = new HashMap(1);
      Integer count = new Integer(3);
      metadata.put(COUNTER, count);
      Object response = client.invoke(INVOCATION_TEST, metadata);
      assertTrue(response.equals(count));
      client.invoke(CALLBACK_TEST);
      List callbacks = client.getCallbacks(callbackHandler);
      assertEquals(1, callbacks.size());
      
      Field field = BisocketClientInvoker.class.getDeclaredField("listenerIdToClientInvokerMap");
      field.setAccessible(true);
      Map listenerIdToClientInvokerMap = (Map) field.get(client.getInvoker());
      assertEquals(0, listenerIdToClientInvokerMap.size());
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   public void testMaxSocketPoolSize() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      
      DelayedCallbackHandler callbackHandler = new DelayedCallbackHandler(4000);
      client.addListener(callbackHandler, new HashMap());
      assertEquals(1, invocationHandler.callbackHandlers.size()); 
      log.info("client added callback handler");
      
      Iterator it = invocationHandler.callbackHandlers.iterator();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) it.next();
      Client callbackClient = serverInvokerCallbackHandler.getCallbackClient();
      assertTrue(callbackClient.getInvoker() instanceof BisocketClientInvoker);
      MicroSocketClientInvoker clientInvoker = (MicroSocketClientInvoker) callbackClient.getInvoker();
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      List pool = (List) field.get(clientInvoker);
//      field = MicroSocketClientInvoker.class.getDeclaredField("usedPooled");
//      field.setAccessible(true);
      assertEquals(0, pool.size());
//      Long usedPooled = (Long) field.get(clientInvoker);
//      assertEquals(0, usedPooled.intValue());
      assertEquals(0, clientInvoker.getNumberOfUsedConnections());
      
      int invocationCount = TEST_MAX_POOL_SIZE;
      for (int i = 0; i < invocationCount; i++)
      {
         client.invokeOneway(CALLBACK_TEST);
      }
      
      Thread.sleep(2000); // +2000
      assertEquals(invocationCount, callbackHandler.callbackCounter);
      assertEquals(0, pool.size());
//      usedPooled = (Long) field.get(clientInvoker);
//      assertEquals(invocationCount, usedPooled.intValue());
      assertEquals(invocationCount, clientInvoker.getNumberOfUsedConnections());
      
      for (int i = 0; i < invocationCount; i++)
      {
         client.invokeOneway(CALLBACK_TEST);
      }
      
      assertEquals(invocationCount, callbackHandler.callbackCounter);
      assertEquals(0, pool.size());
//      usedPooled = (Long) field.get(clientInvoker);
//      assertEquals(invocationCount, usedPooled.intValue());
      assertEquals(invocationCount, clientInvoker.getNumberOfUsedConnections());

      Thread.sleep(1000); // +3000
      assertEquals(invocationCount, callbackHandler.callbackCounter);
      assertEquals(0, pool.size());
//      usedPooled = (Long) field.get(clientInvoker);
//      assertEquals(invocationCount, usedPooled.intValue());
      assertEquals(invocationCount, clientInvoker.getNumberOfUsedConnections());

      
      Thread.sleep(3000); // +6000
      assertEquals(2 * invocationCount, callbackHandler.callbackCounter);
      assertEquals(0, pool.size());
//      usedPooled = (Long) field.get(clientInvoker);
//      assertEquals(invocationCount, usedPooled.intValue());
      assertEquals(invocationCount, clientInvoker.getNumberOfUsedConnections());

      
      Thread.sleep(4000); // +10000
      assertEquals(2 * invocationCount, callbackHandler.callbackCounter);
      assertEquals(invocationCount, pool.size());
//      usedPooled = (Long) field.get(clientInvoker);
//      assertEquals(0, usedPooled.intValue());
      assertEquals(0, clientInvoker.getNumberOfUsedConnections());

      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   public void testOneClientOneConnectorOneHandler() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      log.info("client is connected");
      
      String callbackLocatorURI = getTransport() + "://" + host + ":1";
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      Connector callbackConnector = new Connector(callbackLocatorURI, config);
      callbackConnector.start();
      DelayedCallbackHandler callbackHandler = new DelayedCallbackHandler();
      client.addListener(callbackHandler, new InvokerLocator(callbackLocatorURI));
      assertEquals(1, invocationHandler.callbackHandlers.size()); 
      log.info("client added callback handler");
      
      client.invokeOneway(CALLBACK_TEST);
      client.invokeOneway(CALLBACK_TEST);
      client.invokeOneway(CALLBACK_TEST);
      Thread.sleep(500);
      assertEquals(3, callbackHandler.callbackCounter);
      
      Iterator it = invocationHandler.callbackHandlers.iterator();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) it.next();
      Client callbackClient = serverInvokerCallbackHandler.getCallbackClient();
      assertTrue(callbackClient.getInvoker() instanceof BisocketClientInvoker);
      MicroSocketClientInvoker clientInvoker = (MicroSocketClientInvoker) callbackClient.getInvoker();
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      List pool = (List) field.get(clientInvoker);
//      field = MicroSocketClientInvoker.class.getDeclaredField("usedPooled");
      field.setAccessible(true);
      assertEquals(0, pool.size());
//      Long usedPooled = (Long) field.get(clientInvoker);
//      assertEquals(3, usedPooled.intValue());
      assertEquals(3, clientInvoker.getNumberOfUsedConnections());
      
      Thread.sleep(3000);
      assertEquals(3, pool.size());
//      usedPooled = (Long) field.get(clientInvoker);
//      assertEquals(0, usedPooled.intValue());
      assertEquals(0, clientInvoker.getNumberOfUsedConnections());
      
      client.removeListener(callbackHandler);
      client.disconnect();
      callbackConnector.stop();
   }
   
   
   public void testOneClientOneConnectorTwoHandlers() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      
      String callbackLocatorURI = getTransport() + "://" + host + ":1";
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      Connector callbackConnector = new Connector(callbackLocatorURI, config);
      callbackConnector.start();
      
      DelayedCallbackHandler callbackHandler1 = new DelayedCallbackHandler();
      client.addListener(callbackHandler1, new InvokerLocator(callbackLocatorURI));
      DelayedCallbackHandler callbackHandler2 = new DelayedCallbackHandler();
      client.addListener(callbackHandler2, new InvokerLocator(callbackLocatorURI));
      assertEquals(2, invocationHandler.callbackHandlers.size()); 
      log.info("clients added callback handler");
      
      Iterator it = invocationHandler.callbackHandlers.iterator();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler1 = (ServerInvokerCallbackHandler) it.next();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler2 = (ServerInvokerCallbackHandler) it.next();
      
      Client callbackClient1 = serverInvokerCallbackHandler1.getCallbackClient();
      Client callbackClient2 = serverInvokerCallbackHandler2.getCallbackClient();
      assertNotSame(callbackClient1, callbackClient2);
  
      ClientInvoker clientInvoker1 = callbackClient1.getInvoker();
      assertTrue(clientInvoker1 instanceof BisocketClientInvoker);
      ClientInvoker clientInvoker2 = callbackClient2.getInvoker();
      assertTrue(clientInvoker2 instanceof BisocketClientInvoker);
      assertNotSame(clientInvoker1, clientInvoker2);
      
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      List pool1 = (List) field.get(clientInvoker1);
      List pool2 = (List) field.get(clientInvoker2);
      assertNotSame(pool1, pool2);
      
//      field = MicroSocketClientInvoker.class.getDeclaredField("usedPooled");
//      field.setAccessible(true);
      
      assertEquals(0, callbackHandler1.callbackCounter);
      assertEquals(0, callbackHandler2.callbackCounter);
      assertEquals(0, pool1.size());
      assertEquals(0, pool2.size());
//      Long usedPooled1 = (Long) field.get(clientInvoker1);
//      Long usedPooled2 = (Long) field.get(clientInvoker2);
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
 
      client.invokeOneway(CALLBACK_TEST);
      client.invokeOneway(CALLBACK_TEST);
      
      Thread.sleep(500);
      int count1 = callbackHandler1.callbackCounter;
      int count2 = callbackHandler2.callbackCounter;
      assertTrue(count1 == 2 && count2 == 0 || count1 == 0 && count2 == 2);
      if (count1 == 0)
      {
         Object temp = callbackHandler1;
         callbackHandler1 = callbackHandler2;
         callbackHandler2 = (DelayedCallbackHandler) temp;;
      }
      
      assertEquals(2, callbackHandler1.callbackCounter);
      assertEquals(0, callbackHandler2.callbackCounter);
      assertEquals(0, pool1.size());
      assertEquals(0, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(2, usedPooled1.longValue());
//      assertEquals(0, usedPooled2.longValue());
      assertEquals(2, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      Thread.sleep(2000);
      assertEquals(2, callbackHandler1.callbackCounter);
      assertEquals(2, callbackHandler2.callbackCounter);
      assertEquals(2, pool1.size());
      assertEquals(0, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.intValue());
//      assertEquals(2, usedPooled2.intValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(2, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      Thread.sleep(2000);
      assertEquals(2, callbackHandler1.callbackCounter);
      assertEquals(2, callbackHandler2.callbackCounter);
      assertEquals(2, pool1.size());
      assertEquals(2, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.intValue());
//      assertEquals(0, usedPooled2.intValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      client.removeListener(callbackHandler1);
      client.removeListener(callbackHandler2);
      client.disconnect();
      callbackConnector.stop();
   }
   
   
   public void testOneClientTwoConnectorsOneHandler() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      log.info("client is connected");
      
      String callbackLocatorURI1 = getTransport() + "://" + host + ":1";
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      Connector callbackConnector1 = new Connector(callbackLocatorURI1, config);
      callbackConnector1.start();
      String callbackLocatorURI2 = getTransport() + "://" + host + ":2";
      Connector callbackConnector2 = new Connector(callbackLocatorURI2, config);
      callbackConnector2.start();
      
      DelayedCallbackHandler callbackHandler = new DelayedCallbackHandler();
      client.addListener(callbackHandler, new InvokerLocator(callbackLocatorURI1));
      client.addListener(callbackHandler, new InvokerLocator(callbackLocatorURI2));
      assertEquals(2, invocationHandler.callbackHandlers.size()); 
      log.info("client added callback handlers");
      
      client.invokeOneway(CALLBACK_TEST);
      client.invokeOneway(CALLBACK_TEST);
      Thread.sleep(500);
      assertEquals(2, callbackHandler.callbackCounter);
      
      Iterator it = invocationHandler.callbackHandlers.iterator();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler1 = (ServerInvokerCallbackHandler) it.next();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler2 = (ServerInvokerCallbackHandler) it.next();
      Client callbackClient1 = serverInvokerCallbackHandler1.getCallbackClient();
      Client callbackClient2 = serverInvokerCallbackHandler2.getCallbackClient();
      assertNotSame(callbackClient1, callbackClient2);
      assertTrue(callbackClient1.getInvoker() instanceof BisocketClientInvoker);
      MicroSocketClientInvoker clientInvoker1 = (MicroSocketClientInvoker) callbackClient1.getInvoker();
      assertTrue(callbackClient2.getInvoker() instanceof BisocketClientInvoker);
      MicroSocketClientInvoker clientInvoker2 = (MicroSocketClientInvoker) callbackClient2.getInvoker();
     
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      List pool1 = (List) field.get(clientInvoker1);
      List pool2 = (List) field.get(clientInvoker2);
      assertEquals(0, pool1.size());
      assertEquals(0, pool2.size());
//      field = MicroSocketClientInvoker.class.getDeclaredField("usedPooled");
//      field.setAccessible(true);
//      Long usedPooled1 = (Long) field.get(clientInvoker1);
//      assertEquals(2, usedPooled1.intValue());
      assertEquals(2, clientInvoker1.getNumberOfUsedConnections());
//      Long usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled2.intValue());
//      assertEquals(0, usedPooled2);
      assertEquals(0, clientInvoker2.getNumberOfUsedConnections());
      
      Thread.sleep(3000);
      assertEquals(2, pool1.size());
      assertEquals(0, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      assertEquals(0, usedPooled1.intValue());
      assertEquals(0, clientInvoker1.getNumberOfUsedConnections());
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(2, usedPooled2.intValue());
      assertEquals(2, clientInvoker2.getNumberOfUsedConnections());
      
      Thread.sleep(3000);
      assertEquals(2, pool1.size());
      assertEquals(2, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      assertEquals(0, usedPooled1.intValue());
      assertEquals(0, clientInvoker1.getNumberOfUsedConnections());
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled2.intValue());
      assertEquals(0, clientInvoker2.getNumberOfUsedConnections());
      
      client.removeListener(callbackHandler);
      client.disconnect();
      callbackConnector1.stop();
      callbackConnector2.stop();
   }
   
  
   public void testOneClientTwoConnectorsTwoHandlers() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      
      String callbackLocatorURI1 = getTransport() + "://" + host + ":1";
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      Connector callbackConnector1 = new Connector(callbackLocatorURI1, config);
      callbackConnector1.start();
      String callbackLocatorURI2 = getTransport() + "://" + host + ":2";
      Connector callbackConnector2 = new Connector(callbackLocatorURI2, config);
      callbackConnector2.start();
      
      DelayedCallbackHandler callbackHandler1 = new DelayedCallbackHandler();
      client.addListener(callbackHandler1, new InvokerLocator(callbackLocatorURI1));
      DelayedCallbackHandler callbackHandler2 = new DelayedCallbackHandler();
      client.addListener(callbackHandler2, new InvokerLocator(callbackLocatorURI2));
      assertEquals(2, invocationHandler.callbackHandlers.size()); 
      log.info("clients added callback handler");
      
      Iterator it = invocationHandler.callbackHandlers.iterator();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler1 = (ServerInvokerCallbackHandler) it.next();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler2 = (ServerInvokerCallbackHandler) it.next();
      
      Client callbackClient1 = serverInvokerCallbackHandler1.getCallbackClient();
      Client callbackClient2 = serverInvokerCallbackHandler2.getCallbackClient();
      assertNotSame(callbackClient1, callbackClient2);
  
      ClientInvoker clientInvoker1 = callbackClient1.getInvoker();
      assertTrue(clientInvoker1 instanceof BisocketClientInvoker);
      ClientInvoker clientInvoker2 = callbackClient2.getInvoker();
      assertTrue(clientInvoker2 instanceof BisocketClientInvoker);
      assertNotSame(clientInvoker1, clientInvoker2);
      
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      List pool1 = (List) field.get(clientInvoker1);
      List pool2 = (List) field.get(clientInvoker2);
      
//      field = MicroSocketClientInvoker.class.getDeclaredField("usedPooled");
//      field.setAccessible(true);
      
      assertEquals(0, callbackHandler1.callbackCounter);
      assertEquals(0, callbackHandler2.callbackCounter);
      assertEquals(0, pool1.size());
      assertEquals(0, pool2.size());
//      Long usedPooled1 = (Long) field.get(clientInvoker1);
//      Long usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.longValue());
//      assertEquals(0, usedPooled2.longValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      client.invokeOneway(CALLBACK_TEST);
      client.invokeOneway(CALLBACK_TEST);
      
      Thread.sleep(2000);
      int count1 = callbackHandler1.callbackCounter;
      int count2 = callbackHandler2.callbackCounter;
      assertTrue(count1 == 2 && count2 == 0 || count1 == 0 && count2 == 2);
      if (count1 == 0)
      {
         Object temp = callbackHandler1;
         callbackHandler1 = callbackHandler2;
         callbackHandler2 = (DelayedCallbackHandler) temp;;
      }
      
      assertEquals(2, callbackHandler1.callbackCounter);
      assertEquals(0, callbackHandler2.callbackCounter);
      assertEquals(0, pool1.size());
      assertEquals(0, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(2, usedPooled1.longValue());
//      assertEquals(0, usedPooled2.longValue());
      assertEquals(2, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      Thread.sleep(2000);
      assertEquals(2, callbackHandler1.callbackCounter);
      assertEquals(2, callbackHandler2.callbackCounter);
      assertEquals(2, pool1.size());
      assertEquals(0, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.intValue());
//      assertEquals(2, usedPooled2.intValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(2, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      Thread.sleep(2000);
      assertEquals(2, callbackHandler1.callbackCounter);
      assertEquals(2, callbackHandler2.callbackCounter);
      assertEquals(2, pool1.size());
      assertEquals(2, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.intValue());
//      assertEquals(0, usedPooled2.intValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      client.removeListener(callbackHandler1);
      client.removeListener(callbackHandler2);
      client.disconnect();
      callbackConnector1.stop();
      callbackConnector2.stop();
   }
   
   
   public void testTwoClientsOneConnectorOneHandler() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(config);
      Client client1 = new Client(serverLocator, config);
      client1.connect();
      Client client2 = new Client(serverLocator, config);
      client2.connect();
      log.info("clients are connected");
      assertTrue(client1.getInvoker() instanceof BisocketClientInvoker);
      assertTrue(client2.getInvoker() instanceof BisocketClientInvoker);
      
      String callbackLocatorURI = getTransport() + "://" + host + ":1";
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      Connector callbackConnector = new Connector(callbackLocatorURI, config);
      callbackConnector.start();
      
      DelayedCallbackHandler callbackHandler = new DelayedCallbackHandler();
      client1.addListener(callbackHandler, new InvokerLocator(callbackLocatorURI));
      client2.addListener(callbackHandler, new InvokerLocator(callbackLocatorURI));
      assertEquals(1, invocationHandler.callbackHandlers.size()); 
      log.info("clients added callback handler");
      
      Iterator it = invocationHandler.callbackHandlers.iterator();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) it.next();
      Client callbackClient = serverInvokerCallbackHandler.getCallbackClient();
      ClientInvoker clientInvoker = callbackClient.getInvoker();
      assertTrue(clientInvoker instanceof BisocketClientInvoker);
      
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      List pool = (List) field.get(clientInvoker);
//      field = MicroSocketClientInvoker.class.getDeclaredField("usedPooled");
//      field.setAccessible(true);
      
      assertEquals(0, callbackHandler.callbackCounter);
      assertEquals(0, pool.size());
//      Long usedPooled = (Long) field.get(clientInvoker);
//      assertEquals(0, usedPooled.longValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker).getNumberOfUsedConnections());
 
      client1.invokeOneway(CALLBACK_TEST);
      client2.invokeOneway(CALLBACK_TEST);
      
      Thread.sleep(500);
      assertEquals(2, callbackHandler.callbackCounter);
      assertEquals(0, pool.size());
//      usedPooled = (Long) field.get(clientInvoker);
//      assertEquals(2, usedPooled.longValue());
      assertEquals(2, ((MicroSocketClientInvoker) clientInvoker).getNumberOfUsedConnections());
      
      Thread.sleep(2000);
      assertEquals(2, callbackHandler.callbackCounter);
      assertEquals(2, pool.size());
//      usedPooled = (Long) field.get(clientInvoker);
//      assertEquals(0, usedPooled.intValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker).getNumberOfUsedConnections());
      
      client1.removeListener(callbackHandler);
      client1.disconnect();
      client2.removeListener(callbackHandler);
      client2.disconnect();
      callbackConnector.stop();
   }
   
   
   public void testTwoClientsOneConnectorTwoHandlers() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      
      String callbackLocatorURI = getTransport() + "://" + host + ":1";
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      Connector callbackConnector = new Connector(callbackLocatorURI, config);
      callbackConnector.start();
      
      DelayedCallbackHandler callbackHandler1 = new DelayedCallbackHandler();
      client.addListener(callbackHandler1, new InvokerLocator(callbackLocatorURI));
      DelayedCallbackHandler callbackHandler2 = new DelayedCallbackHandler();
      client.addListener(callbackHandler2, new InvokerLocator(callbackLocatorURI));
      assertEquals(2, invocationHandler.callbackHandlers.size()); 
      log.info("clients added callback handler");
      
      Iterator it = invocationHandler.callbackHandlers.iterator();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler1 = (ServerInvokerCallbackHandler) it.next();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler2 = (ServerInvokerCallbackHandler) it.next();
      Client callbackClient1 = serverInvokerCallbackHandler1.getCallbackClient();
      Client callbackClient2 = serverInvokerCallbackHandler2.getCallbackClient();
      assertNotSame(callbackClient1, callbackClient2);
      ClientInvoker clientInvoker1 = callbackClient1.getInvoker();
      assertTrue(clientInvoker1 instanceof BisocketClientInvoker);
      ClientInvoker clientInvoker2 = callbackClient2.getInvoker();
      assertTrue(clientInvoker2 instanceof BisocketClientInvoker);
      assertNotSame(clientInvoker1, clientInvoker2);
      
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      List pool1 = (List) field.get(clientInvoker1);
      List pool2 = (List) field.get(clientInvoker2);
      assertNotSame(pool1, pool2);
      
//      field = MicroSocketClientInvoker.class.getDeclaredField("usedPooled");
//      field.setAccessible(true);
      
      assertEquals(0, callbackHandler1.callbackCounter);
      assertEquals(0, callbackHandler2.callbackCounter);
      assertEquals(0, pool1.size());
//      Long usedPooled1 = (Long) field.get(clientInvoker1);
//      Long usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.longValue());
//      assertEquals(0, usedPooled2.longValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
 
      client.invokeOneway(CALLBACK_TEST);
      client.invokeOneway(CALLBACK_TEST);
      
      Thread.sleep(500);
      int count1 = callbackHandler1.callbackCounter;
      int count2 = callbackHandler2.callbackCounter;
      assertTrue(count1 == 2 && count2 == 0 || count1 == 0 && count2 == 2);
      if (count1 == 0)
      {
         Object temp = callbackHandler1;
         callbackHandler1 = callbackHandler2;
         callbackHandler2 = (DelayedCallbackHandler) temp;;
      }
      
      assertEquals(2, callbackHandler1.callbackCounter);
      assertEquals(0, callbackHandler2.callbackCounter);
      assertEquals(0, pool1.size());
      assertEquals(0, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(2, usedPooled1.longValue());
//      assertEquals(0, usedPooled2.longValue());
      assertEquals(2, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      Thread.sleep(2000);
      assertEquals(2, callbackHandler1.callbackCounter);
      assertEquals(2, callbackHandler2.callbackCounter);
      assertEquals(2, pool1.size());
      assertEquals(0, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.intValue());
//      assertEquals(2, usedPooled2.intValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(2, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      Thread.sleep(2000);
      assertEquals(2, callbackHandler1.callbackCounter);
      assertEquals(2, callbackHandler2.callbackCounter);
      assertEquals(2, pool1.size());
      assertEquals(2, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.intValue());
//      assertEquals(0, usedPooled2.intValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      client.removeListener(callbackHandler1);
      client.removeListener(callbackHandler2);
      client.disconnect();
      callbackConnector.stop();
   }
   
   
   public void testTwoClientsTwoConnectorsOneHandler() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(config);
      Client client1 = new Client(serverLocator, config);
      client1.connect();
      Client client2 = new Client(serverLocator, config);
      client2.connect();
      log.info("clients are connected");
      assertTrue(client1.getInvoker() instanceof BisocketClientInvoker);
      assertTrue(client2.getInvoker() instanceof BisocketClientInvoker);
      
      String callbackLocatorURI1 = getTransport() + "://" + host + ":1";
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      Connector callbackConnector1 = new Connector(callbackLocatorURI1, config);
      callbackConnector1.start();
      String callbackLocatorURI2 = getTransport() + "://" + host + ":2";
      Connector callbackConnector2 = new Connector(callbackLocatorURI2, config);
      callbackConnector2.start();
      
      DelayedCallbackHandler callbackHandler = new DelayedCallbackHandler();
      client1.addListener(callbackHandler, new InvokerLocator(callbackLocatorURI1));
      client2.addListener(callbackHandler, new InvokerLocator(callbackLocatorURI2));
      assertEquals(2, invocationHandler.callbackHandlers.size()); 
      log.info("clients added callback handler");
      
      Iterator it = invocationHandler.callbackHandlers.iterator();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler1 = (ServerInvokerCallbackHandler) it.next();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler2 = (ServerInvokerCallbackHandler) it.next();
      Client callbackClient1 = serverInvokerCallbackHandler1.getCallbackClient();
      Client callbackClient2 = serverInvokerCallbackHandler2.getCallbackClient();
      assertNotSame(callbackClient1, callbackClient2);
      ClientInvoker clientInvoker1 = callbackClient1.getInvoker();
      assertTrue(clientInvoker1 instanceof BisocketClientInvoker);
      ClientInvoker clientInvoker2 = callbackClient2.getInvoker();
      assertTrue(clientInvoker2 instanceof BisocketClientInvoker);
      assertNotSame(clientInvoker1, clientInvoker2);
      
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      List pool1 = (List) field.get(clientInvoker1);
      List pool2 = (List) field.get(clientInvoker2);
      
//      field = MicroSocketClientInvoker.class.getDeclaredField("usedPooled");
//      field.setAccessible(true);
      
      assertEquals(0, callbackHandler.callbackCounter);
      assertEquals(0, pool1.size());
      assertEquals(0, pool2.size());
//      Long usedPooled1 = (Long) field.get(clientInvoker1);
//      Long usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.longValue());
//      assertEquals(0, usedPooled2.longValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
 
      client1.invokeOneway(CALLBACK_TEST);
      client2.invokeOneway(CALLBACK_TEST);
      
      Thread.sleep(500);
      assertEquals(2, callbackHandler.callbackCounter);
      assertEquals(0, pool1.size());
      assertEquals(0, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(2, usedPooled1.longValue());
//      assertEquals(0, usedPooled2.longValue());
      assertEquals(2, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      Thread.sleep(2000);
      assertEquals(4, callbackHandler.callbackCounter);
      assertEquals(2, pool1.size());
      assertEquals(0, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.intValue());
//      assertEquals(2, usedPooled2.intValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(2, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      Thread.sleep(2000);
      assertEquals(4, callbackHandler.callbackCounter);
      assertEquals(2, pool1.size());
      assertEquals(2, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.intValue());
//      assertEquals(0, usedPooled2.intValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      client1.removeListener(callbackHandler);
      client1.disconnect();
      client2.removeListener(callbackHandler);
      client2.disconnect();
      callbackConnector1.stop();
      callbackConnector2.stop();
   }
   
   
   public void testTwoClientsTwoConnectorsTwoHandlers() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      
      String callbackLocatorURI1 = getTransport() + "://" + host + ":1";
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      Connector callbackConnector1 = new Connector(callbackLocatorURI1, config);
      callbackConnector1.start();
      String callbackLocatorURI2 = getTransport() + "://" + host + ":2";
      Connector callbackConnector2 = new Connector(callbackLocatorURI2, config);
      callbackConnector2.start();
      
      DelayedCallbackHandler callbackHandler1 = new DelayedCallbackHandler();
      client.addListener(callbackHandler1, new InvokerLocator(callbackLocatorURI1));
      DelayedCallbackHandler callbackHandler2 = new DelayedCallbackHandler();
      client.addListener(callbackHandler2, new InvokerLocator(callbackLocatorURI2));
      assertEquals(2, invocationHandler.callbackHandlers.size()); 
      log.info("clients added callback handler");
      
      Iterator it = invocationHandler.callbackHandlers.iterator();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler1 = (ServerInvokerCallbackHandler) it.next();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler2 = (ServerInvokerCallbackHandler) it.next();
      Client callbackClient1 = serverInvokerCallbackHandler1.getCallbackClient();
      Client callbackClient2 = serverInvokerCallbackHandler2.getCallbackClient();
      assertNotSame(callbackClient1, callbackClient2);
      ClientInvoker clientInvoker1 = callbackClient1.getInvoker();
      assertTrue(clientInvoker1 instanceof BisocketClientInvoker);
      ClientInvoker clientInvoker2 = callbackClient2.getInvoker();
      assertTrue(clientInvoker2 instanceof BisocketClientInvoker);
      assertNotSame(clientInvoker1, clientInvoker2);
      
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      List pool1 = (List) field.get(clientInvoker1);
      List pool2 = (List) field.get(clientInvoker2);
      
//      field = MicroSocketClientInvoker.class.getDeclaredField("usedPooled");
//      field.setAccessible(true);
      
      assertEquals(0, callbackHandler1.callbackCounter);
      assertEquals(0, callbackHandler2.callbackCounter);
      assertEquals(0, pool1.size());
//      Long usedPooled1 = (Long) field.get(clientInvoker1);
//      Long usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.longValue());
//      assertEquals(0, usedPooled2.longValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
 
      client.invokeOneway(CALLBACK_TEST);
      client.invokeOneway(CALLBACK_TEST);
      
      Thread.sleep(500);
      int count1 = callbackHandler1.callbackCounter;
      int count2 = callbackHandler2.callbackCounter;
      assertTrue(count1 == 2 && count2 == 0 || count1 == 0 && count2 == 2);
      if (count1 == 0)
      {
         Object temp = callbackHandler1;
         callbackHandler1 = callbackHandler2;
         callbackHandler2 = (DelayedCallbackHandler) temp;;
      }
      
      assertEquals(2, callbackHandler1.callbackCounter);
      assertEquals(0, callbackHandler2.callbackCounter);
      assertEquals(0, pool1.size());
      assertEquals(0, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(2, usedPooled1.longValue());
//      assertEquals(0, usedPooled2.longValue());
      assertEquals(2, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      Thread.sleep(2000);
      assertEquals(2, callbackHandler1.callbackCounter);
      assertEquals(2, callbackHandler2.callbackCounter);
      assertEquals(2, pool1.size());
      assertEquals(0, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.intValue());
//      assertEquals(2, usedPooled2.intValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(2, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      Thread.sleep(2000);
      assertEquals(2, callbackHandler1.callbackCounter);
      assertEquals(2, callbackHandler2.callbackCounter);
      assertEquals(2, pool1.size());
      assertEquals(2, pool2.size());
//      usedPooled1 = (Long) field.get(clientInvoker1);
//      usedPooled2 = (Long) field.get(clientInvoker2);
//      assertEquals(0, usedPooled1.intValue());
//      assertEquals(0, usedPooled2.intValue());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker1).getNumberOfUsedConnections());
      assertEquals(0, ((MicroSocketClientInvoker) clientInvoker2).getNumberOfUsedConnections());
      
      client.removeListener(callbackHandler1);
      client.removeListener(callbackHandler2);
      client.disconnect();
      callbackConnector1.stop();
      callbackConnector2.stop();
   }
   
   
   public void testHearbeat() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      config.put(Bisocket.PING_FREQUENCY, TEST_PING_FREQUENCY_STRING);
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      DelayedCallbackHandler callbackHandler = new DelayedCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      log.info("client added callback handler");
      client.invoke(CALLBACK_TEST);
      client.invoke(CALLBACK_TEST);
      client.invoke(CALLBACK_TEST);
      
      Thread.sleep(500);
      assertEquals(3, callbackHandler.callbackCounter);
      
      Field field = Client.class.getDeclaredField("callbackConnectors");
      field.setAccessible(true);
      Map callbackConnectors = (Map) field.get(client);
      assertEquals(1, callbackConnectors.size());
      Set callbackConnectorSet = (Set) callbackConnectors.values().iterator().next();
      assertEquals(1, callbackConnectorSet.size());
      Connector callbackConnector = (Connector) callbackConnectorSet.iterator().next();
      BisocketServerInvoker invoker = (BisocketServerInvoker) callbackConnector.getServerInvoker();
      field = BisocketServerInvoker.class.getDeclaredField("controlConnectionThreadMap");
      field.setAccessible(true);
      Map controlConnectionThreadMap = (Map) field.get(invoker);
      assertEquals(1, controlConnectionThreadMap.size());
      Thread t = (Thread) controlConnectionThreadMap.values().iterator().next();
      Class[] classes = BisocketServerInvoker.class.getDeclaredClasses();
      Class invokerClass = null;
      for (int i = 0; i < classes.length; i++)
      {
         log.info(classes[i]);
         String fqn = classes[i].getName();
         String className = fqn.substring(fqn.lastIndexOf('.') + 1);
         log.info(className);
         if ("BisocketServerInvoker$ControlConnectionThread".equals(className))
         {
            invokerClass = classes[i];
            break;
         }
      }
      
      assertTrue(invokerClass != null);
      field = invokerClass.getDeclaredField("lastPing");
      field.setAccessible(true);
      int pingFrequency = Integer.parseInt(TEST_PING_FREQUENCY_STRING);
      Thread.sleep(2 * pingFrequency);
      Long lastPing = (Long) field.get(t);
      log.info("current: " + System.currentTimeMillis() + ", lastPing: " + lastPing);
      assertTrue((System.currentTimeMillis() - lastPing.longValue()) < pingFrequency);
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   public void testControlConnectionFailureServerSide() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      config.put(Bisocket.PING_FREQUENCY, TEST_PING_FREQUENCY_STRING);
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      DelayedCallbackHandler callbackHandler = new DelayedCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      log.info("client added callback handler");
      client.invoke(CALLBACK_TEST);
      assertEquals(1, callbackHandler.callbackCounter);
      
      ServerInvoker serverInvoker = connector.getServerInvoker();
      assertTrue(serverInvoker instanceof BisocketServerInvoker);
      Field field = ServerInvoker.class.getDeclaredField("callbackHandlers");
      field.setAccessible(true);
      Map callbackHandlers = (Map) field.get(serverInvoker);
      assertEquals(1, callbackHandlers.size());
      ServerInvokerCallbackHandler serverInvokerCallbackHandler;
      serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) callbackHandlers.values().toArray()[0]; 
      Client callbackClient = serverInvokerCallbackHandler.getCallbackClient();
      assertTrue(callbackClient.getInvoker() instanceof BisocketClientInvoker);
      MicroSocketClientInvoker clientInvoker = (MicroSocketClientInvoker) callbackClient.getInvoker();
      field = BisocketClientInvoker.class.getDeclaredField("controlSocket");
      field.setAccessible(true);
      Socket controlSocket = (Socket) field.get(clientInvoker);
      controlSocket.close();
      log.info("CLOSED CONTROL SOCKET");
      
      // Shut down the only existing ServerThread on the server side, and therefore
      // the only existing pooled connection on the client side, forcing the
      // next invocation to depend on the creation of a new control connection.
      field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(serverInvoker);
      Set serverThreads = clientpool.getContents();
      assertEquals(1, serverThreads.size());
      ((ServerThread)serverThreads.iterator().next()).shutdown();
      HashMap metadata = new HashMap();
      metadata.put(COUNTER, new Integer(0));
      client.invoke(INVOCATION_TEST, metadata);
      
      Thread.sleep(TEST_PING_FREQUENCY * 4);
      client.invoke(CALLBACK_TEST);
      assertEquals(2, callbackHandler.callbackCounter);
      
      field = BisocketClientInvoker.class.getDeclaredField("controlSocket");
      field.setAccessible(true);
      Socket newControlSocket = (Socket) field.get(clientInvoker);
      assertTrue(!controlSocket.equals(newControlSocket));
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   public void testControlConnectionFailureClientSide() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      config.put(Bisocket.PING_FREQUENCY, TEST_PING_FREQUENCY_STRING);
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      DelayedCallbackHandler callbackHandler = new DelayedCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      log.info("client added callback handler");
      client.invoke(CALLBACK_TEST);
      assertEquals(1, callbackHandler.callbackCounter);
      
      Iterator it = invocationHandler.callbackHandlers.iterator();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) it.next();
      Client callbackClient = serverInvokerCallbackHandler.getCallbackClient();
      assertTrue(callbackClient.getInvoker() instanceof BisocketClientInvoker);
      BisocketClientInvoker clientInvoker = (BisocketClientInvoker) callbackClient.getInvoker();
      Field field = BisocketClientInvoker.class.getDeclaredField("controlSocket");
      field.setAccessible(true);
      Socket serverSideControlSocket = (Socket) field.get(clientInvoker);
      
      field = Client.class.getDeclaredField("callbackConnectors");
      field.setAccessible(true);
      Map callbackConnectors = (Map) field.get(client);
      assertEquals(1, callbackConnectors.size());
      Set callbackConnectorSet = (Set) callbackConnectors.values().iterator().next();
      assertEquals(1, callbackConnectorSet.size());
      Connector callbackConnector = (Connector) callbackConnectorSet.iterator().next();
      BisocketServerInvoker invoker = (BisocketServerInvoker) callbackConnector.getServerInvoker();
      field = BisocketServerInvoker.class.getDeclaredField("controlConnectionThreadMap");
      field.setAccessible(true);
      Map controlConnectionThreadMap = (Map) field.get(invoker);
      assertEquals(1, controlConnectionThreadMap.size());
      Collection controlConnectionThreads = controlConnectionThreadMap.values();
      Thread controlConnectionThread = (Thread) controlConnectionThreads.iterator().next();
      Class[] classes = BisocketServerInvoker.class.getDeclaredClasses();
      Class controlConnectionThreadClass = null;
      for (int i = 0; i < classes.length; i++)
      {
         log.info(classes[i]);
         String fqn = classes[i].getName();
         String className = fqn.substring(fqn.lastIndexOf('.') + 1);
         log.info(className);
         if ("BisocketServerInvoker$ControlConnectionThread".equals(className))
         {
            controlConnectionThreadClass = classes[i];
            break;
         }
      }
      
      assertTrue(controlConnectionThreadClass != null);
      field = controlConnectionThreadClass.getDeclaredField("controlSocket");
      field.setAccessible(true);
      Socket clientSideControlSocket = (Socket) field.get(controlConnectionThread);
      clientSideControlSocket.close();
      log.info("CLOSED CONTROL SOCKET");
      
      // Shut down the only existing ServerThread on the server side, and therefore
      // the only existing pooled connection on the client side, forcing the
      // next invocation to depend on the creation of a new control connection.
      field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(connector.getServerInvoker());
      Set serverThreads = clientpool.getContents();
      assertEquals(1, serverThreads.size());
      ((ServerThread)serverThreads.iterator().next()).shutdown();
      HashMap metadata = new HashMap();
      metadata.put(COUNTER, new Integer(0));
      client.invoke(INVOCATION_TEST, metadata);
      
      Thread.sleep(TEST_PING_FREQUENCY * 4);
      client.invoke(CALLBACK_TEST);
      assertEquals(2, callbackHandler.callbackCounter);
      Thread newControlConnectionThread = (Thread) controlConnectionThreads.iterator().next();
      assertTrue(!controlConnectionThread.equals(newControlConnectionThread));
      
      field = BisocketClientInvoker.class.getDeclaredField("controlSocket");
      field.setAccessible(true);
      Socket newServerSideControlSocket = (Socket) field.get(clientInvoker);
      assertTrue(!serverSideControlSocket.equals(newServerSideControlSocket));
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   /**
    * Disabling this test because recent changes to BisocketClientInvoker 
    * (JBREM-1147: "BisocketClientInvoker.createSocket() in callback mode should check for replaced control socket")
    * allow better recovery, so that the failure expected in this test doesn't occur.
    * 
    * @throws Throwable
    */
   public void DISABLEDtestControlConnectionFailureBeforeFirstCallback() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      config.put(Bisocket.PING_FREQUENCY, TEST_PING_FREQUENCY_STRING);
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      DelayedCallbackHandler callbackHandler = new DelayedCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      log.info("client added callback handler");
      
      // Shut down control connection on client side, so that pings can't be sent
      // from server.
      Field field = Client.class.getDeclaredField("callbackConnectors");
      field.setAccessible(true);
      Map callbackConnectors = (Map) field.get(client);
      assertEquals(1, callbackConnectors.size());
      Set callbackConnectorSet = (Set) callbackConnectors.values().iterator().next();
      assertEquals(1, callbackConnectorSet.size());
      Connector callbackConnector = (Connector) callbackConnectorSet.iterator().next();
      ServerInvoker cbsi = callbackConnector.getServerInvoker();
      assertTrue(cbsi instanceof BisocketServerInvoker);
      field = BisocketServerInvoker.class.getDeclaredField("controlConnectionThreadMap");
      field.setAccessible(true);
      Map controlConnectionThreadMap = (Map) field.get(cbsi);
      assertEquals(1, controlConnectionThreadMap.size());
      Thread t1 = (Thread) controlConnectionThreadMap.values().iterator().next();
      Class[] classes = BisocketServerInvoker.class.getDeclaredClasses();
      Class controlConnectionThreadClass = null;
      
      for (int i = 0; i < classes.length; i++)
      {
         log.info(classes[i]);
         String fqn = classes[i].getName();
         String className = fqn.substring(fqn.lastIndexOf('.') + 1);
         log.info(className);
         if ("BisocketServerInvoker$ControlConnectionThread".equals(className))
         {
            controlConnectionThreadClass = classes[i];
            break;
         }
      }
      
      assertNotNull(controlConnectionThreadClass);
      assertEquals(t1.getClass(), controlConnectionThreadClass);
      field = controlConnectionThreadClass.getDeclaredField("controlSocket");
      field.setAccessible(true);
      Socket controlSocket = (Socket) field.get(t1);
      assertNotNull(controlSocket);
      controlSocket.close();
      log.info("CLOSED CONTROL SOCKET");
      
      log.info("*****************  PingTimerTask failure EXPECTED *********************");
      field = controlConnectionThreadClass.getDeclaredField("MAX_INITIAL_ATTEMPTS");
      field.setAccessible(true);
      int MAX_INITIAL_ATTEMPTS = ((Integer) field.get(null)).intValue();
      Thread.sleep(TEST_PING_FREQUENCY * (MAX_INITIAL_ATTEMPTS + 1) + 5000);
      log.info("***********************************************************************");
      
      // Callback should be possible because control connection has been replaced.
      assertEquals(1, controlConnectionThreadMap.size());
      Thread t2 = (Thread) controlConnectionThreadMap.values().iterator().next();
      assertNotSame(t2, t1);
      client.invoke(CALLBACK_TEST);
      assertEquals(1, callbackHandler.callbackCounter);
      
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   public void testNoControlConnectionRestart() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      config.put(Bisocket.PING_FREQUENCY, TEST_PING_FREQUENCY_STRING);
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      log.info("client added callback handler");
      
      Iterator it = invocationHandler.callbackHandlers.iterator();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) it.next();
      Client callbackClient = serverInvokerCallbackHandler.getCallbackClient();
      assertTrue(callbackClient.getInvoker() instanceof BisocketClientInvoker);
      MicroSocketClientInvoker clientInvoker = (MicroSocketClientInvoker) callbackClient.getInvoker();
      Field field = BisocketClientInvoker.class.getDeclaredField("controlSocket");
      field.setAccessible(true);
      Socket controlSocketBefore = (Socket) field.get(clientInvoker);

      Thread.sleep(4 * TEST_PING_FREQUENCY);
      Socket controlSocketAfter = (Socket) field.get(clientInvoker);      
      assertEquals(controlSocketBefore, controlSocketAfter);
      log.info("control socket: " + controlSocketBefore);

      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   /**
    * This method tests the ability of a client to connect to a restarted
    * server after a failure has been detected on the control connection.
    */
   public void testServerSlowRestart() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      config.put(Bisocket.PING_FREQUENCY, TEST_PING_FREQUENCY_STRING);
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      log.info("client added callback handler");
      client.invoke(CALLBACK_TEST);
      assertEquals(1, callbackHandler.callbackCounter);
      
      Connector oldConnector = connector;
      SocketServerInvoker invoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(invoker);
      Set threads = clientpool.getContents();
      for (Iterator it = threads.iterator(); it.hasNext(); )
      {
         ServerThread t = (ServerThread) it.next();
         field = ServerThread.class.getDeclaredField("socketWrapper");
         field.setAccessible(true);
         SocketWrapper socketWrapper = (SocketWrapper) field.get(t);
         socketWrapper.close();
      }
      connector.stop();
      log.info("STOPPED CONNECTOR");
      
      for (int i = 0; i < 5; i++)
      {
         try
         {
            internalSetUp(port);
            break;
         }
         catch (Exception e)
         {
            log.info("unable to restart connector: retrying in 60 seconds");
            tearDown();
            Thread.sleep(60000);
         }
      }
      
      log.info("RESTARTED CONNECTOR");
      assertNotSame(connector, oldConnector);
      
      // Wait until a failure has been detected on the control connection.
      Thread.sleep(TEST_PING_FREQUENCY * 5);
      
      // It is beyond the scope of Remoting to fail over to a new server,
      // complete with registered callback handlers.
      log.info("adding callback handler");
      client.addListener(callbackHandler, new HashMap());
      log.info("client added callback handler");
      
      client.invoke(CALLBACK_TEST);
      assertEquals(2, callbackHandler.callbackCounter);

      client.removeListener(callbackHandler);
      client.disconnect();
      
      // The ControlMonitorTimerTask from the first callback handler tries to recreate
      // its control connection, which adds an entry to
      // BisocketClientInvoker.listenerIdToSocketsMap which cannot be removed, which
      // interferes with testForLeaks().
      //
      // TODO: Should the possibility of a leak because of this phenomenon be handled?
      field = BisocketClientInvoker.class.getDeclaredField("listenerIdToSocketsMap");
      field.setAccessible(true);
      Map listenerIdToSocketsMap = (Map) field.get(null);
      listenerIdToSocketsMap.clear();
   }
   
   
   /**
    * This method tests the ability of a client to connect to a restarted
    * server before a failure has been detected on the control connection.
    * It guards against the bug in JBREM-731.
    */
   public void testServerQuickRestart() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      config.put(Bisocket.PING_FREQUENCY, TEST_PING_FREQUENCY_STRING);
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      log.info("client added callback handler");
      client.invoke(CALLBACK_TEST);
      assertEquals(1, callbackHandler.callbackCounter);
      
      Connector oldConnector = connector;
      SocketServerInvoker invoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(invoker);
      Set threads = clientpool.getContents();
      for (Iterator it = threads.iterator(); it.hasNext(); )
      {
         ServerThread t = (ServerThread) it.next();
         field = ServerThread.class.getDeclaredField("socketWrapper");
         field.setAccessible(true);
         SocketWrapper socketWrapper = (SocketWrapper) field.get(t);
         socketWrapper.close();
      }
      connector.stop();
      log.info("STOPPED CONNECTOR");
      
      for (int i = 0; i < 5; i++)
      {
         try
         {
            internalSetUp(port);
            break;
         }
         catch (Exception e)
         {
            log.info("unable to restart connector: retrying in 60 seconds");
            tearDown();
            Thread.sleep(60000);
         }
      }
      
      log.info("RESTARTED CONNECTOR");
      assertNotSame(connector, oldConnector);
      
      // It is beyond the scope of Remoting to fail over to a new server,
      // complete with registered callback handlers.
      log.info("adding callback handler");
      client.addListener(callbackHandler, new HashMap());
      log.info("client added callback handler");
      
      client.invoke(CALLBACK_TEST);
      assertEquals(2, callbackHandler.callbackCounter);

      client.removeListener(callbackHandler);
      client.disconnect();
      
      // The ControlMonitorTimerTask from the first callback handler tries to recreate
      // its control connection, which adds an entry to
      // BisocketClientInvoker.listenerIdToSocketsMap which cannot be removed, which
      // interferes with testForLeaks().
      //
      // TODO: Should the possibility of a leak because of this phenomenon be handled?
      field = BisocketClientInvoker.class.getDeclaredField("listenerIdToSocketsMap");
      field.setAccessible(true);
      Map listenerIdToSocketsMap = (Map) field.get(null);
      listenerIdToSocketsMap.clear();
   }
   
   
   public void testDeadControlConnectionShutdown() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      config.put(Bisocket.PING_FREQUENCY, TEST_PING_FREQUENCY_STRING);
      int MAX_RETRIES = 3;
      config.put(Bisocket.MAX_CONTROL_CONNECTION_RESTARTS, Integer.toString(MAX_RETRIES));
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      
      String callbackLocatorURI = getTransport() + "://" + host + ":1";
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      Connector callbackConnector = new Connector(callbackLocatorURI, config);
      callbackConnector.start();
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler, new InvokerLocator(callbackLocatorURI));
      log.info("client added callback handler");
      client.invoke(CALLBACK_TEST);
      assertEquals(1, callbackHandler.callbackCounter);
      
      SocketServerInvoker invoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(invoker);
      Set threads = clientpool.getContents();
      for (Iterator it = threads.iterator(); it.hasNext(); )
      {
         ServerThread t = (ServerThread) it.next();
         field = ServerThread.class.getDeclaredField("socketWrapper");
         field.setAccessible(true);
         SocketWrapper socketWrapper = (SocketWrapper) field.get(t);
         socketWrapper.close();
      }
      connector.stop();
      log.info("STOPPED CONNECTOR");
      
      for (int i = 0; i < 5; i++)
      {
         try
         {
            internalSetUp(port);
            break;
         }
         catch (Exception e)
         {
            log.info("unable to restart connector: retrying in 60 seconds");
            Thread.sleep(60000);
         }
      }
      
      log.info("RESTARTED CONNECTOR");
      
      // Wait until a failure has been detected on the control connection.
      Thread.sleep(TEST_PING_FREQUENCY * MAX_RETRIES * 8);
      
      ServerInvoker callbackServerInvoker = callbackConnector.getServerInvoker();
      field = BisocketServerInvoker.class.getDeclaredField("controlConnectionThreadMap");
      field.setAccessible(true);
      Map controlConnectionThreadMap = (Map) field.get(callbackServerInvoker);
      assertEquals(0, controlConnectionThreadMap.size());
      
      client.setDisconnectTimeout(0);
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   public void testForLeaks() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      config.put(Bisocket.PING_FREQUENCY, TEST_PING_FREQUENCY_STRING);
      
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      
      int CALLBACK_HANDLER_COUNT = 5;
      SimpleCallbackHandler[] callbackHandlers = new SimpleCallbackHandler[CALLBACK_HANDLER_COUNT];
      for (int i = 0; i < CALLBACK_HANDLER_COUNT; i++)
      {
         callbackHandlers[i] = new SimpleCallbackHandler();
         client.addListener(callbackHandlers[i], new HashMap());
      }

      client.invoke(CALLBACK_TEST);
      for (int i = 0; i < CALLBACK_HANDLER_COUNT; i++)
      {
         assertEquals(1, callbackHandlers[i].callbackCounter);
      }
      
      // Static fields.
      Field field = BisocketServerInvoker.class.getDeclaredField("listenerIdToServerInvokerMap");
      field.setAccessible(true);
      Map listenerIdToServerInvokerMap = (Map) field.get(null);
      field = BisocketClientInvoker.class.getDeclaredField("listenerIdToClientInvokerMap");
      field.setAccessible(true);
      Map listenerIdToClientInvokerMap = (Map) field.get(null);
      field = BisocketClientInvoker.class.getDeclaredField("listenerIdToCallbackClientInvokerMap");
      field.setAccessible(true);
      Map listenerIdToCallbackClientInvokerMap = (Map) field.get(null);
      field = BisocketClientInvoker.class.getDeclaredField("listenerIdToSocketsMap");
      field.setAccessible(true);
      Map listenerIdToSocketsMap = (Map) field.get(null);
      
      // Non-static fields.
      field = Client.class.getDeclaredField("callbackConnectors");
      field.setAccessible(true);
      Map callbackConnectors = (Map) field.get(client);
      assertEquals(CALLBACK_HANDLER_COUNT, callbackConnectors.size());
      ServerInvoker[] serverInvokers = new ServerInvoker[CALLBACK_HANDLER_COUNT];
      Map[] listenerIdToInvokerLocatorMaps = new Map[CALLBACK_HANDLER_COUNT];
      Map[] controlConnectionThreadMaps = new Map[CALLBACK_HANDLER_COUNT];

      int i = 0;
      Iterator it = callbackConnectors.values().iterator();
      while (it.hasNext())
      {
         Set set = (Set) it.next();
         assertEquals(1, set.size());
         Connector c = (Connector) set.iterator().next();
         serverInvokers[i] = c.getServerInvoker();
         assertTrue(serverInvokers[i] instanceof BisocketServerInvoker);
         field = BisocketServerInvoker.class.getDeclaredField("listenerIdToInvokerLocatorMap");
         field.setAccessible(true);
         listenerIdToInvokerLocatorMaps[i] = (Map) field.get(serverInvokers[i]);
         field = BisocketServerInvoker.class.getDeclaredField("controlConnectionThreadMap");
         field.setAccessible(true);
         controlConnectionThreadMaps[i] = (Map) field.get(serverInvokers[i]);
         i++;
      }
      
      assertEquals(CALLBACK_HANDLER_COUNT, listenerIdToServerInvokerMap.size());
      assertEquals(CALLBACK_HANDLER_COUNT, listenerIdToClientInvokerMap.size());
      assertEquals(CALLBACK_HANDLER_COUNT, listenerIdToCallbackClientInvokerMap.size());
      assertEquals(CALLBACK_HANDLER_COUNT, listenerIdToSocketsMap.size());
      
      for (int j = 0; j < CALLBACK_HANDLER_COUNT; j++)
      {
         assertEquals(1, listenerIdToInvokerLocatorMaps[j].size());
         assertEquals(1, controlConnectionThreadMaps[j].size());
         field = BisocketServerInvoker.class.getDeclaredField("controlMonitorTimerTask");
         field.setAccessible(true);
         assertNotNull(field.get(serverInvokers[j]));
      }
      
      for (int j = 0; j < CALLBACK_HANDLER_COUNT; j++)
      {
         client.removeListener(callbackHandlers[j]);
      }
      
      assertEquals(0, listenerIdToServerInvokerMap.size());
      assertEquals(0, listenerIdToClientInvokerMap.size());
      assertEquals(0, listenerIdToCallbackClientInvokerMap.size());
      assertEquals(0, listenerIdToSocketsMap.size());
      
      Class[] classes = BisocketServerInvoker.class.getDeclaredClasses();
      Class controlMonitorTimerTaskClass = null;
      for (int j = 0; j < classes.length; j++)
      {
         log.info(classes[j]);
         String fqn = classes[j].getName();
         String className = fqn.substring(fqn.lastIndexOf('.') + 1);
         log.info(className);
         if ("BisocketServerInvoker$ControlMonitorTimerTask".equals(className))
         {
            controlMonitorTimerTaskClass = classes[j];
            break;
         }
      }
      assertNotNull(controlMonitorTimerTaskClass);
      
      for (int j = 0; j < CALLBACK_HANDLER_COUNT; j++)
      {
         assertEquals("server invoker: " + j, 0, listenerIdToInvokerLocatorMaps[j].size());
         assertEquals("server invoker: " + j, 0, controlConnectionThreadMaps[j].size());
         
         field = BisocketServerInvoker.class.getDeclaredField("controlMonitorTimerTask");
         field.setAccessible(true);
         Object controlMonitorTimerTask = field.get(serverInvokers[j]);
         field = controlMonitorTimerTaskClass.getDeclaredField("listenerIdToInvokerLocatorMap");
         field.setAccessible(true);
         assertNull("server invoker: " + j, field.get(controlMonitorTimerTask));
         field = controlMonitorTimerTaskClass.getDeclaredField("controlConnectionThreadMap");
         field.setAccessible(true);
         assertNull("server invoker: " + j, field.get(controlMonitorTimerTask));    
      }
      
      client.disconnect();
   }
   
   
   public void testForLeaksQuickRemoveListener() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostName();
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
      config.put(Bisocket.PING_FREQUENCY, TEST_PING_FREQUENCY_STRING);
      
      addExtraClientConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      
      int CALLBACK_HANDLER_COUNT = 5;
      SimpleCallbackHandler[] callbackHandlers = new SimpleCallbackHandler[CALLBACK_HANDLER_COUNT];
      for (int i = 0; i < CALLBACK_HANDLER_COUNT; i++)
      {
         callbackHandlers[i] = new SimpleCallbackHandler();
         client.addListener(callbackHandlers[i], new HashMap());
      }

      client.invoke(CALLBACK_TEST);
      for (int i = 0; i < CALLBACK_HANDLER_COUNT; i++)
      {
         assertEquals(1, callbackHandlers[i].callbackCounter);
      }
      
      // Static fields.
      Field field = BisocketServerInvoker.class.getDeclaredField("listenerIdToServerInvokerMap");
      field.setAccessible(true);
      Map listenerIdToServerInvokerMap = (Map) field.get(null);
      field = BisocketClientInvoker.class.getDeclaredField("listenerIdToClientInvokerMap");
      field.setAccessible(true);
      Map listenerIdToClientInvokerMap = (Map) field.get(null);
      
      // Non-static fields.
      field = Client.class.getDeclaredField("callbackConnectors");
      field.setAccessible(true);
      Map callbackConnectors = (Map) field.get(client);
      assertEquals(CALLBACK_HANDLER_COUNT, callbackConnectors.size());
      ServerInvoker[] serverInvokers = new ServerInvoker[CALLBACK_HANDLER_COUNT];
      Map[] listenerIdToInvokerLocatorMaps = new Map[CALLBACK_HANDLER_COUNT];
      Map[] controlConnectionThreadMaps = new Map[CALLBACK_HANDLER_COUNT];

      int i = 0;
      Iterator it = callbackConnectors.values().iterator();
      while (it.hasNext())
      {
         Set set = (Set) it.next();
         assertEquals(1, set.size());
         Connector c = (Connector) set.iterator().next();
         serverInvokers[i] = c.getServerInvoker();
         assertTrue(serverInvokers[i] instanceof BisocketServerInvoker);
         field = BisocketServerInvoker.class.getDeclaredField("listenerIdToInvokerLocatorMap");
         field.setAccessible(true);
         listenerIdToInvokerLocatorMaps[i] = (Map) field.get(serverInvokers[i]);
         field = BisocketServerInvoker.class.getDeclaredField("controlConnectionThreadMap");
         field.setAccessible(true);
         controlConnectionThreadMaps[i] = (Map) field.get(serverInvokers[i]);
         i++;
      }
      
      assertEquals(CALLBACK_HANDLER_COUNT, listenerIdToServerInvokerMap.size());
      assertEquals(CALLBACK_HANDLER_COUNT, listenerIdToClientInvokerMap.size());
      
      for (int j = 0; j < CALLBACK_HANDLER_COUNT; j++)
      {
         assertEquals(1, listenerIdToInvokerLocatorMaps[j].size());
         assertEquals(1, controlConnectionThreadMaps[j].size());
         field = BisocketServerInvoker.class.getDeclaredField("controlMonitorTimerTask");
         field.setAccessible(true);
         assertNotNull(field.get(serverInvokers[j]));
      }
      
      client.setDisconnectTimeout(0);
      
      for (int j = 0; j < CALLBACK_HANDLER_COUNT; j++)
      {
         client.removeListener(callbackHandlers[j]);
      }
      
      assertEquals(0, listenerIdToServerInvokerMap.size());
      assertEquals(0, listenerIdToClientInvokerMap.size());
      
      Class[] classes = BisocketServerInvoker.class.getDeclaredClasses();
      Class controlMonitorTimerTaskClass = null;
      for (int j = 0; j < classes.length; j++)
      {
         log.info(classes[j]);
         String fqn = classes[j].getName();
         String className = fqn.substring(fqn.lastIndexOf('.') + 1);
         log.info(className);
         if ("BisocketServerInvoker$ControlMonitorTimerTask".equals(className))
         {
            controlMonitorTimerTaskClass = classes[j];
            break;
         }
      }
      assertNotNull(controlMonitorTimerTaskClass);
      
      for (int j = 0; j < CALLBACK_HANDLER_COUNT; j++)
      {
         assertEquals("server invoker: " + j, 0, listenerIdToInvokerLocatorMaps[j].size());
         assertEquals("server invoker: " + j, 0, controlConnectionThreadMaps[j].size());
         
         field = BisocketServerInvoker.class.getDeclaredField("controlMonitorTimerTask");
         field.setAccessible(true);
         Object controlMonitorTimerTask = field.get(serverInvokers[j]);
         field = controlMonitorTimerTaskClass.getDeclaredField("listenerIdToInvokerLocatorMap");
         field.setAccessible(true);
         assertNull("server invoker: " + j, field.get(controlMonitorTimerTask));
         field = controlMonitorTimerTaskClass.getDeclaredField("controlConnectionThreadMap");
         field.setAccessible(true);
         assertNull("server invoker: " + j, field.get(controlMonitorTimerTask));    
      }
      
      client.disconnect();
   }
   
   
   public static void main(String[] args)
   {
      BisocketTestCase testCase = new BisocketTestCase();
      try
      {
         testCase.setUp();
//         testCase.testConfiguration();
         testCase.tearDown();
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }
   
   
   protected String getTransport()
   {
      return "bisocket";
   }
   
   
   protected void addExtraClientConfig(Map config)
   {
   }
   
   
   protected void addExtraServerConfig(Map config)
   {
   }
   

   /**
    * Simple invocation handler implementation.  When callback client's are registered, will
    * generate callbacks periodically.
    */
   static class SampleInvocationHandler implements ServerInvocationHandler
   {
      public Set callbackHandlers = new HashSet();
      private int counter = 0;
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         System.out.println("Adding callback listener.");
         callbackHandlers.add(callbackHandler);
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
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
            try
            {
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
      public int callbackCounter;
      private Object lock = new Object();
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.debug("received callback: " + callback.getParameter());
         synchronized (lock)
         {
            callbackCounter++;
         }
      }
   }
   
   
   static class DelayedCallbackHandler implements InvokerCallbackHandler
   {
      public int callbackCounter;
      private Object lock = new Object();
      private int delay = 2000;
      
      public DelayedCallbackHandler()
      {
      }
      
      public DelayedCallbackHandler(int delay)
      {
    	  this.delay = delay;
      }
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.debug("received callback: " + callback.getParameter());
         synchronized (lock)
         {
            callbackCounter++;
         }
         try
         {
            Thread.sleep(delay);
         }
         catch (InterruptedException e)
         {
         }
      }
   }
}