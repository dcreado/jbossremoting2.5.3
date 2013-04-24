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


/**
 * Tests Callback acknowledgements.
 *
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * <p/>
 * Copyright (c) 2006
 * </p>
 */
package org.jboss.test.remoting.callback.acknowledge;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.CallbackListener;
import org.jboss.remoting.callback.CallbackPoller;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


public class CallbackAcknowledgeTestCase extends TestCase
{
   private static String APPLICATION_ACKNOWLEDGEMENT_TEST = "AppAckTest";
   private static String REMOTING_ACKNOWLEDGEMENT_TEST = "remotingAckTest";
   private static Logger log = Logger.getLogger(CallbackAcknowledgeTestCase.class);

   private Connector connector;
   private InvokerLocator serverLocator;
   private String transport = "socket";
   private Client client;


   public void setUp() throws Exception
   {
      String host = InetAddress.getLocalHost().getHostAddress();
      int freePort = PortUtil.findFreePort(host);
      serverLocator = new InvokerLocator(transport + "://" + host + ":" + freePort);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      connector = new Connector(serverLocator, config);
      connector.start();
      connector.addInvocationHandler("test", new TestInvocationHandler());

      client = new Client(serverLocator, config);
      client.connect();

      TestInvocationHandler.callbacksAcknowledged = 0;
      TestInvocationHandler.callbackResponses.clear();
   }


   public void tearDown()
   {
      if (connector != null)
         connector.stop();

      if (client != null)
         client.disconnect();
   }


   /**
    * In this test, the connection is configured for pull callbacks, and the
    * acknowledgements should be made by an explicit call to Client.acknowledgeCallback()
    * after the callbacks have been retrieved and (presumably) processed. Two
    * InvokerCallbackHandlers are registered.
    */
   public void testNonblockingPullApplicationAckDifferentHandlers()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler();
         client.addListener(callbackHandler1);
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler();
         client.addListener(callbackHandler2);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         List callbacks1 = client.getCallbacks(callbackHandler1);
         assertEquals(2, callbacks1.size());
         List callbacks2 = client.getCallbacks(callbackHandler2);
         assertEquals(2, callbacks2.size());
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         assertEquals(2, client.acknowledgeCallbacks(callbackHandler1, callbacks1));
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         assertEquals(2, client.acknowledgeCallbacks(callbackHandler2, callbacks2));
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         Thread.sleep(1000);
         assertEquals(0, callbackHandler1.callbacksReceived);
         assertEquals(0, callbackHandler2.callbacksReceived);
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test, the connection is configured for pull callbacks, and the
    * acknowledgements should be made by an explicit call to Client.acknowledgeCallback()
    * after the callbacks have been retrieved and (presumably) processed. Two
    * InvokerCallbackHandlers are registered.
    */
   public void testBlockingPullApplicationAckDifferentHandlers()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler();
         client.addListener(callbackHandler1);
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler();
         client.addListener(callbackHandler2);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         HashMap metadata = new HashMap();
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
         List callbacks1 = client.getCallbacks(callbackHandler1, metadata);
         assertEquals(2, callbacks1.size());
         List callbacks2 = client.getCallbacks(callbackHandler2, metadata);
         assertEquals(2, callbacks2.size());
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         assertEquals(2, client.acknowledgeCallbacks(callbackHandler1, callbacks1));
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         assertEquals(2, client.acknowledgeCallbacks(callbackHandler2, callbacks2));
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         Thread.sleep(1000);
         assertEquals(0, callbackHandler1.callbacksReceived);
         assertEquals(0, callbackHandler2.callbacksReceived);
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }

   
   /**
    * In this test, the connection is configured for pull callbacks, and the
    * acknowledgements should be made by an explicit call to Client.acknowledgeCallback()
    * after the callbacks have been retrieved and (presumably) processed. A single
    * InvokerCallbackHandler is registered twice but treated as a single instance.
    */
   public void testNonblockingPullApplicationAckSameHandler()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler = new TestCallbackHandler();
         client.addListener(callbackHandler);
         client.addListener(callbackHandler);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         List callbacks = client.getCallbacks(callbackHandler);
         assertEquals(2, callbacks.size());
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         assertEquals(2, client.acknowledgeCallbacks(callbackHandler, callbacks));
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         Thread.sleep(1000);
         assertEquals(0, callbackHandler.callbacksReceived);
         client.removeListener(callbackHandler);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }

   
   /**
    * In this test, the connection is configured for pull callbacks, and the
    * acknowledgements should be made by an explicit call to Client.acknowledgeCallback()
    * after the callbacks have been retrieved and (presumably) processed. A single
    * InvokerCallbackHandler is registered twice but treated as a single instance.
    */
   public void testBlockingPullApplicationAckSameHandler()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler = new TestCallbackHandler();
         client.addListener(callbackHandler);
         client.addListener(callbackHandler);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         HashMap metadata = new HashMap();
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
         List callbacks = client.getCallbacks(callbackHandler, metadata);
         assertEquals(2, callbacks.size());
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         assertEquals(2, client.acknowledgeCallbacks(callbackHandler, callbacks));
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         Thread.sleep(1000);
         assertEquals(0, callbackHandler.callbacksReceived);
         client.removeListener(callbackHandler);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }
      

   /**
    * In this test, the connection is configured for pull callbacks.  The
    * server requests that Remoting handle push callback acknowledgements,
    * but that should have no effect.  Instead, callback acknowledgements
    * should be made by an explicit call to Client.acknowledgeCallback() after
    * the callback has been retrieved and (presumably) processed.
    * Two distinct InvokerCallbackHandlers are used.
    */
   public void testNonblockingPullRemotingAckDifferentHandlers()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler();
         client.addListener(callbackHandler1);
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler();
         client.addListener(callbackHandler2);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         List callbacks1 = client.getCallbacks(callbackHandler1);
         assertEquals(2, callbacks1.size());
         List callbacks2 = client.getCallbacks(callbackHandler2);
         assertEquals(2, callbacks2.size());
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         assertEquals(2, client.acknowledgeCallbacks(callbackHandler1, callbacks1));
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         assertEquals(2, client.acknowledgeCallbacks(callbackHandler2, callbacks2));
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         Thread.sleep(1000);
         assertEquals(0, callbackHandler1.callbacksReceived);
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }

   
   /**
    * In this test, the connection is configured for pull callbacks.  The
    * server requests that Remoting handle push callback acknowledgements,
    * but that should have no effect.  Instead, callback acknowledgements
    * should be made by an explicit call to Client.acknowledgeCallback() after
    * the callback has been retrieved and (presumably) processed.
    * Two distinct InvokerCallbackHandlers are used.
    */
   public void testPullRemotingAckDifferentHandlers()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler();
         client.addListener(callbackHandler1);
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler();
         client.addListener(callbackHandler2);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         HashMap metadata = new HashMap();
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
         List callbacks1 = client.getCallbacks(callbackHandler1, metadata);
         assertEquals(2, callbacks1.size());
         List callbacks2 = client.getCallbacks(callbackHandler2, metadata);
         assertEquals(2, callbacks2.size());
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         assertEquals(2, client.acknowledgeCallbacks(callbackHandler1, callbacks1));
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         assertEquals(2, client.acknowledgeCallbacks(callbackHandler2, callbacks2));
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         Thread.sleep(1000);
         assertEquals(0, callbackHandler1.callbacksReceived);
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }
   
   
   /**
    * In this test, the connection is configured pull callbacks.  The
    * server requests that Remoting handle push callback acknowledgements,
    * but that should have no effect. Instead, acknowledgements should be made
    * by an explicit call to Client.acknowledgeCallback() after the callback
    * has been retrieved and (presumably) processed. A single InvokerCallbackHandler
    * is registered twice but treated as a single instance.
    */
   public void testNonblockingPullRemotingAckSameHandler()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler = new TestCallbackHandler();
         client.addListener(callbackHandler);
         client.addListener(callbackHandler);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         List callbacks = client.getCallbacks(callbackHandler);
         assertEquals(2, callbacks.size());
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         assertEquals(2, client.acknowledgeCallbacks(callbackHandler, callbacks));
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         Thread.sleep(1000);
         assertEquals(0, callbackHandler.callbacksReceived);
         client.removeListener(callbackHandler);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }
   
   
   /**
    * In this test, the connection is configured pull callbacks.  The
    * server requests that Remoting handle push callback acknowledgements,
    * but that should have no effect. Instead, acknowledgements should be made
    * by an explicit call to Client.acknowledgeCallback() after the callback
    * has been retrieved and (presumably) processed. A single InvokerCallbackHandler
    * is registered twice but treated as a single instance.
    */
   public void testBlockingPullRemotingAckSameHandler()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler = new TestCallbackHandler();
         client.addListener(callbackHandler);
         client.addListener(callbackHandler);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         HashMap metadata = new HashMap();
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
         List callbacks = client.getCallbacks(callbackHandler, metadata);
         assertEquals(2, callbacks.size());
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         assertEquals(2, client.acknowledgeCallbacks(callbackHandler, callbacks));
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         Thread.sleep(1000);
         assertEquals(0, callbackHandler.callbacksReceived);
         client.removeListener(callbackHandler);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for push callbacks implemented in
    * Remoting by polling the server for callbacks and pushing them (on the client
    * side) to the InvokerCallbackHandler.  The acknowledgements should be made
    * explicitly by the InvokerCallbackHandler.  Two distinct InvokerCallbackHandlers
    * are registered.
    */
   public void testNonblockingPollApplicationAckDifferentHandlers()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler(client, 1);
         HashMap metadata = new HashMap();
         metadata.put(CallbackPoller.CALLBACK_POLL_PERIOD, "100");
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.NONBLOCKING);
         client.addListener(callbackHandler1, metadata);
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler(client, 2);
         client.addListener(callbackHandler2, metadata);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         Thread.sleep(2000);
         assertEquals(2, callbackHandler1.callbacksReceived);
         assertEquals(2, callbackHandler2.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         //key: message #: handler id: callbacks received
         String response111 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 1: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response111));
         String response212 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 1: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response212));
         String response121 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 2: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response121));
         String response222 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 2: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response222));
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for push callbacks implemented in
    * Remoting by polling the server for callbacks and pushing them (on the client
    * side) to the InvokerCallbackHandler.  The acknowledgements should be made
    * explicitly by the InvokerCallbackHandler.  Two distinct InvokerCallbackHandlers
    * are registered.
    */
   public void testBlockingPollApplicationAckDifferentHandlers()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler(client, 1);
         HashMap metadata = new HashMap();
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
         client.addListener(callbackHandler1, metadata);
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler(client, 2);
         client.addListener(callbackHandler2, metadata);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         Thread.sleep(3000);
         assertEquals(2, callbackHandler1.callbacksReceived);
         assertEquals(2, callbackHandler2.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         //key: message #: handler id: callbacks received
         String response111 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 1: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response111));
         String response212 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 1: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response212));
         String response121 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 2: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response121));
         String response222 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 2: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response222));
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for push callbacks implemented in
    * Remoting by polling the server for callbacks and pushing them (on the client
    * side) to the InvokerCallbackHandler.  A single InvokerCallbackHandler is
    * registered twice but the Client treats it as a single instance.  The
    * acknowledgements should be made explicitly by the InvokerCallbackHandler.
    */
   public void testNonblockingPollApplicationAckSameHandler()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler = new TestCallbackHandler(client);
         HashMap metadata = new HashMap();
         metadata.put(CallbackPoller.CALLBACK_POLL_PERIOD, "100");
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.NONBLOCKING);
         client.addListener(callbackHandler, metadata);
         client.addListener(callbackHandler, metadata);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         Thread.sleep(1000);
         assertEquals(2, callbackHandler.callbacksReceived);
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         //key: message #: handler id: callbacks received
         String response101 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 0: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response101));
         String response202 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 0: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response202));
         client.removeListener(callbackHandler);
         Thread.sleep(4000);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for push callbacks implemented in
    * Remoting by polling the server for callbacks and pushing them (on the client
    * side) to the InvokerCallbackHandler.  A single InvokerCallbackHandler is
    * registered twice but the Client treats it as a single instance.  The
    * acknowledgements should be made explicitly by the InvokerCallbackHandler.
    */
   public void testBlockingPollApplicationAckSameHandler()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler = new TestCallbackHandler(client);
         HashMap metadata = new HashMap();
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
         client.addListener(callbackHandler, metadata);
         client.addListener(callbackHandler, metadata);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         Thread.sleep(1000);
         assertEquals(2, callbackHandler.callbacksReceived);
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         //key: message #: handler id: callbacks received
         String response101 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 0: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response101));
         String response202 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 0: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response202));
         client.removeListener(callbackHandler);
         Thread.sleep(4000);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }

   
   /**
    * In this test the connection is configured for push callbacks implemented in
    * Remoting by polling the server for callbacks and pushing them (on the client
    * side) to the InvokerCallbackHandler.  The acknowledgement should be made
    * implicitly by CallbackPoller, which it does by calling Client.acknowledgeCallback()
    * after it has pushed the callback to the InvokerCallbackHandler.  Two
    * distinct InvokerCallbackHandlers are registered.
    */
   public void testNonblockingPollRemotingAckDifferentHandlers()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler();
         HashMap metadata = new HashMap();
         metadata.put(CallbackPoller.CALLBACK_POLL_PERIOD, "100");
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.NONBLOCKING);
         client.addListener(callbackHandler1, metadata);
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler();
         client.addListener(callbackHandler2, metadata);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         Thread.sleep(2000);
         assertEquals(2, callbackHandler1.callbacksReceived);
         assertEquals(2, callbackHandler2.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }

   
   /**
    * In this test the connection is configured for push callbacks implemented in
    * Remoting by polling the server for callbacks and pushing them (on the client
    * side) to the InvokerCallbackHandler.  The acknowledgement should be made
    * implicitly by CallbackPoller, which it does by calling Client.acknowledgeCallback()
    * after it has pushed the callback to the InvokerCallbackHandler.  Two
    * distinct InvokerCallbackHandlers are registered.
    */
   public void testBlockingPollRemotingAckDifferentHandlers()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler();
         HashMap metadata = new HashMap();
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
         client.addListener(callbackHandler1, metadata);
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler();
         client.addListener(callbackHandler2, metadata);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         Thread.sleep(1000);
         assertEquals(2, callbackHandler1.callbacksReceived);
         assertEquals(2, callbackHandler2.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }

   
   /**
    * In this test the connection is configured for push callbacks implemented in
    * Remoting by polling the server for callbacks and pushing them (on the client
    * side) to the InvokerCallbackHandler.  The acknowledgement should be made
    * implicitly by CallbackPoller, which it does by calling Client.acknowledgeCallback()
    * after it has pushed the callback to the InvokerCallbackHandler.  A single
    * InvokerCallbackHandler is registered twice, but the Client recognizes only a
    * single instance.
    */
   public void testNonblockingPollRemotingAckSameHandler()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler = new TestCallbackHandler();
         HashMap metadata = new HashMap();
         metadata.put(CallbackPoller.CALLBACK_POLL_PERIOD, "100");
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.NONBLOCKING);
         client.addListener(callbackHandler, metadata);
         client.addListener(callbackHandler, metadata);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         Thread.sleep(1000);
         assertEquals(2, callbackHandler.callbacksReceived);
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         client.removeListener(callbackHandler);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }

   
   /**
    * In this test the connection is configured for push callbacks implemented in
    * Remoting by polling the server for callbacks and pushing them (on the client
    * side) to the InvokerCallbackHandler.  The acknowledgement should be made
    * implicitly by CallbackPoller, which it does by calling Client.acknowledgeCallback()
    * after it has pushed the callback to the InvokerCallbackHandler.  A single
    * InvokerCallbackHandler is registered twice, but the Client recognizes only a
    * single instance.
    */
   public void testBlockingPollRemotingAckSameHandler()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler = new TestCallbackHandler();
         HashMap metadata = new HashMap();
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
         client.addListener(callbackHandler, metadata);
         client.addListener(callbackHandler, metadata);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         Thread.sleep(1000);
         assertEquals(2, callbackHandler.callbacksReceived);
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         client.removeListener(callbackHandler);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for true push callbacks, and the
    * acknowledgements should be made on the client side by the InvokerCallbackHandler.
    * Two distinct InvokerCallbackHandlers are registered.
    */
   public void testPushApplicationAckDifferentHandlers()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler(client, 1);
         client.addListener(callbackHandler1, null, null, true);
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler(client, 2);
         client.addListener(callbackHandler2, null, null, true);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         assertEquals(2, callbackHandler1.callbacksReceived);
         assertEquals(2, callbackHandler2.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         //key: message #: handler id: callbacks received
         String response111 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 1: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response111));
         String response212 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 1: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response212));
         String response121 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 2: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response121));
         String response222 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 2: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response222));
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for true push callbacks, and the
    * acknowledgement should be made on the client side by the InvokerCallbackHandler.
    * A single InvokerCallbackHandler is shared by two callback Connectors.
    */
   public void testPushApplicationAckSameHandler()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler = new TestCallbackHandler(client);
         client.addListener(callbackHandler, null, null, true);
         client.addListener(callbackHandler, null, null, true);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         assertEquals(4, callbackHandler.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         //key: message #: handler id: callbacks received
         String response101 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 0: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response101));
         String response202 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 0: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response202));
         String response103 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 0: 3";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response103));
         String response204 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 0: 4";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response204));
         client.removeListener(callbackHandler);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for true push callbacks, and the
    * acknowledgements should be made by ServerInvokerCallbackHandler.handleCallback()
    * after it has pushed the Callback to the client.  Two distinct
    * InvokerCallbackHandlers are registered.
    */
   public void testPushRemotingAckDifferentHandlers()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler();
         client.addListener(callbackHandler1, null, null, true);
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler();
         client.addListener(callbackHandler2, null, null, true);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         assertEquals(2, callbackHandler1.callbacksReceived);
         assertEquals(2, callbackHandler2.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for true push callbacks, and the
    * acknowledgements should be made by ServerInvokerCallbackHandler.handleCallback()
    * after it has pushed the Callback to the client.  A single InvokerCallbackHandler
    * is registered twice, and each is treated as a distinct instance.
    */
   public void testPushRemotingAckSameHandler()
   {
      log.info("entering " + getName());
      try
      {
         TestCallbackHandler callbackHandler = new TestCallbackHandler();
         client.addListener(callbackHandler, null, null, true);
         client.addListener(callbackHandler, null, null, true);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         assertEquals(4, callbackHandler.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         client.removeListener(callbackHandler);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for true push callbacks by creating a
    * Connector and passing its InvokerLocator when the InvokerCallbackHandlers are
    * registered.  Acknowledgements should be made on the client side by the
    * InvokerCallbackHandler. Two distinct InvokerCallbackHandlers are registered with
    * a single Connector.
    */
   public void testPushApplicationAckDifferentHandlersPassLocator()
   {
      log.info("entering " + getName());
      try
      {
         String host = InetAddress.getLocalHost().getHostAddress();
         int freePort = PortUtil.findFreePort(host);
         InvokerLocator callbackLocator = new InvokerLocator(transport + "://" + host + ":" + freePort);
         HashMap config = new HashMap();
         config.put(InvokerLocator.FORCE_REMOTE, "true");
         Connector connector = new Connector(callbackLocator, config);
         connector.start();
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler(client, 1);
         client.addListener(callbackHandler1, callbackLocator);
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler(client, 2);
         client.addListener(callbackHandler2, callbackLocator);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         assertEquals(2, callbackHandler1.callbacksReceived);
         assertEquals(2, callbackHandler2.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         //key: message #: handler id: callbacks received
         String response111 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 1: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response111));
         String response212 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 1: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response212));
         String response121 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 2: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response121));
         String response222 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 2: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response222));
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for true push callbacks by creating a
    * Connector and passing its InvokerLocator when the InvokerCallbackHandlers are
    * registered.  Acknowledgements should be made on the client side by the
    * InvokerCallbackHandler. A InvokerCallbackHandler is registered twice with
    * a single Connector and treated as a single instance.
    */
   public void testPushApplicationAckSameHandlerPassLocator()
   {
      log.info("entering " + getName());
      try
      {
         String host = InetAddress.getLocalHost().getHostAddress();
         int freePort = PortUtil.findFreePort(host);
         InvokerLocator callbackLocator = new InvokerLocator(transport + "://" + host + ":" + freePort);
         HashMap config = new HashMap();
         config.put(InvokerLocator.FORCE_REMOTE, "true");
         Connector connector = new Connector(callbackLocator, config);
         connector.start();
         TestCallbackHandler callbackHandler = new TestCallbackHandler(client);
         client.addListener(callbackHandler, callbackLocator);
         client.addListener(callbackHandler, callbackLocator);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         assertEquals(2, callbackHandler.callbacksReceived);
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         //key: message #: handler id: callbacks received
         String response101 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 0: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response101));
         String response202 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 0: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response202));
         client.removeListener(callbackHandler);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for true push callbacks by creating
    * two Connectors and passing their InvokerLocator when the InvokerCallbackHandlers
    * are registered.  Acknowledgements should be made on the client side by the
    * InvokerCallbackHandler. Each of two distinct InvokerCallbackHandlers is
    * registered with a distinct Connector
    */
   public void testPushApplicationAckDifferentHandlersPassTwoLocators()
   {
      log.info("entering " + getName());
      try
      {
         String host = InetAddress.getLocalHost().getHostAddress();
         int freePort = PortUtil.findFreePort(host);
         InvokerLocator callbackLocator1 = new InvokerLocator(transport + "://" + host + ":" + freePort);
         HashMap config = new HashMap();
         config.put(InvokerLocator.FORCE_REMOTE, "true");
         Connector connector1 = new Connector(callbackLocator1, config);
         connector1.start();
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler(client, 1);
         client.addListener(callbackHandler1, callbackLocator1);
         freePort = PortUtil.findFreePort(host);
         InvokerLocator callbackLocator2 = new InvokerLocator(transport + "://" + host + ":" + freePort);
         Connector connector2 = new Connector(callbackLocator2, config);
         connector2.start();
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler(client, 2);
         client.addListener(callbackHandler2, callbackLocator2);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         assertEquals(2, callbackHandler1.callbacksReceived);
         assertEquals(2, callbackHandler2.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         //key: message #: handler id: callbacks received
         String response111 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 1: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response111));
         String response212 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 1: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response212));
         String response121 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 2: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response121));
         String response222 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 2: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response222));
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for true push callbacks by creating
    * two Connectors and passing their InvokerLocator when the InvokerCallbackHandlers
    * are registered.  Acknowledgements should be made on the client side by the
    * InvokerCallbackHandler. A single InvokerCallbackHandlers is registered with
    * two distinct Connectors.
    */
   public void testPushApplicationAckSameHandlerPassTwoLocators()
   {
      log.info("entering " + getName());
      try
      {
         String host = InetAddress.getLocalHost().getHostAddress();
         int freePort = PortUtil.findFreePort(host);
         InvokerLocator callbackLocator1 = new InvokerLocator(transport + "://" + host + ":" + freePort);
         HashMap config = new HashMap();
         config.put(InvokerLocator.FORCE_REMOTE, "true");
         Connector connector1 = new Connector(callbackLocator1, config);
         connector1.start();
         TestCallbackHandler callbackHandler = new TestCallbackHandler(client);
         client.addListener(callbackHandler, callbackLocator1);
         freePort = PortUtil.findFreePort(host);
         InvokerLocator callbackLocator2 = new InvokerLocator(transport + "://" + host + ":" + freePort);
         Connector connector2 = new Connector(callbackLocator2, config);
         connector2.start();
         client.addListener(callbackHandler, callbackLocator2);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(APPLICATION_ACKNOWLEDGEMENT_TEST);
         assertEquals(4, callbackHandler.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         //key: message #: handler id: callbacks received
         String response101 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 0: 1";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response101));
         String response202 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 0: 2";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response202));
         String response103 = APPLICATION_ACKNOWLEDGEMENT_TEST + "1: 0: 3";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response103));
         String response204 = APPLICATION_ACKNOWLEDGEMENT_TEST + "2: 0: 4";
         assertTrue(TestInvocationHandler.callbackResponses.contains(response204));
         client.removeListener(callbackHandler);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for true push callbacks by creating a
    * Connector and passing its InvokerLocator when the InvokerCallbackHandlers are
    * registered.  Acknowledgements should be made implicitly on the server side by the
    * ServerInvokerCallbackHandler. Two distinct InvokerCallbackHandlers are registered
    * with a single Connector.
    */
   public void testPushRemotingAckDifferentHandlersPassLocator()
   {
      log.info("entering " + getName());
      try
      {
         String host = InetAddress.getLocalHost().getHostAddress();
         int freePort = PortUtil.findFreePort(host);
         InvokerLocator callbackLocator = new InvokerLocator(transport + "://" + host + ":" + freePort);
         HashMap config = new HashMap();
         config.put(InvokerLocator.FORCE_REMOTE, "true");
         Connector connector = new Connector(callbackLocator, config);
         connector.start();
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler(client, 1);
         client.addListener(callbackHandler1, callbackLocator);
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler(client, 2);
         client.addListener(callbackHandler2, callbackLocator);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         assertEquals(2, callbackHandler1.callbacksReceived);
         assertEquals(2, callbackHandler2.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for true push callbacks by creating a
    * Connector and passing its InvokerLocator when the InvokerCallbackHandlers are
    * registered.  Acknowledgements should be made implicitly on the server side by the
    * ServerInvokerCallbackHandler. A InvokerCallbackHandler is registered twice with
    * a single Connector and treated as a single instance.
    */
   public void testPushRemotingAckSameHandlerPassLocator()
   {
      log.info("entering " + getName());
      try
      {
         String host = InetAddress.getLocalHost().getHostAddress();
         int freePort = PortUtil.findFreePort(host);
         InvokerLocator callbackLocator = new InvokerLocator(transport + "://" + host + ":" + freePort);
         HashMap config = new HashMap();
         config.put(InvokerLocator.FORCE_REMOTE, "true");
         Connector connector = new Connector(callbackLocator, config);
         connector.start();
         TestCallbackHandler callbackHandler = new TestCallbackHandler(client);
         client.addListener(callbackHandler, callbackLocator);
         client.addListener(callbackHandler, callbackLocator);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         assertEquals(2, callbackHandler.callbacksReceived);
         assertEquals(2, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         client.removeListener(callbackHandler);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for true push callbacks by creating
    * two Connectors and passing their InvokerLocator when the InvokerCallbackHandlers
    * are registered.  Acknowledgements should be made implicitly on the server side by the
    * ServerInvokerCallbackHandler. Each of two distinct InvokerCallbackHandlers is
    * registered with a distinct Connector
    */
   public void testPushRemotingAckDifferentHandlersPassTwoLocators()
   {
      log.info("entering " + getName());
      try
      {
         String host = InetAddress.getLocalHost().getHostAddress();
         int freePort = PortUtil.findFreePort(host);
         InvokerLocator callbackLocator1 = new InvokerLocator(transport + "://" + host + ":" + freePort);
         HashMap config = new HashMap();
         config.put(InvokerLocator.FORCE_REMOTE, "true");
         Connector connector1 = new Connector(callbackLocator1, config);
         connector1.start();
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler(client, 1);
         client.addListener(callbackHandler1, callbackLocator1);
         freePort = PortUtil.findFreePort(host);
         InvokerLocator callbackLocator2 = new InvokerLocator(transport + "://" + host + ":" + freePort);
         Connector connector2 = new Connector(callbackLocator2, config);
         connector2.start();
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler(client, 2);
         client.addListener(callbackHandler2, callbackLocator2);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         assertEquals(2, callbackHandler1.callbacksReceived);
         assertEquals(2, callbackHandler2.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         client.removeListener(callbackHandler1);
         client.removeListener(callbackHandler2);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   /**
    * In this test the connection is configured for true push callbacks by creating
    * two Connectors and passing their InvokerLocator when the InvokerCallbackHandlers
    * are registered.  Acknowledgements should be made implicitly on the server side by the
    * ServerInvokerCallbackHandler. A single InvokerCallbackHandlers is registered with
    * two distinct Connectors.
    */
   public void testPushRemotingAckSameHandlerPassTwoLocators()
   {
      log.info("entering " + getName());
      try
      {
         String host = InetAddress.getLocalHost().getHostAddress();
         int freePort = PortUtil.findFreePort(host);
         InvokerLocator callbackLocator1 = new InvokerLocator(transport + "://" + host + ":" + freePort);
         HashMap config = new HashMap();
         config.put(InvokerLocator.FORCE_REMOTE, "true");
         Connector connector1 = new Connector(callbackLocator1, config);
         connector1.start();
         TestCallbackHandler callbackHandler = new TestCallbackHandler(client);
         client.addListener(callbackHandler, callbackLocator1);
         freePort = PortUtil.findFreePort(host);
         InvokerLocator callbackLocator2 = new InvokerLocator(transport + "://" + host + ":" + freePort);
         Connector connector2 = new Connector(callbackLocator2, config);
         connector2.start();
         client.addListener(callbackHandler, callbackLocator2);
         assertEquals(0, TestInvocationHandler.callbacksAcknowledged);
         client.invoke(REMOTING_ACKNOWLEDGEMENT_TEST);
         assertEquals(4, callbackHandler.callbacksReceived);
         assertEquals(4, TestInvocationHandler.callbacksAcknowledged);
         assertTrue(TestInvocationHandler.callbackResponses.isEmpty());
         client.removeListener(callbackHandler);
         log.info(getName() + " PASSES");
      }
      catch (Throwable e)
      {
         log.info(getName() + " FAILS");
         e.printStackTrace();
         fail();
      }
   }


   static class TestInvocationHandler implements ServerInvocationHandler, CallbackListener
   {
      static int callbacksAcknowledged;
      static HashSet callbackResponses = new HashSet();

      HashSet callbackHandlers = new HashSet();

      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         String command = (String) invocation.getParameter();
         System.out.println("command: " + command);

         for (Iterator it = callbackHandlers.iterator(); it.hasNext(); )
         {
            InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler) it.next();
            Callback cb1 = new Callback(command + "1");
            HashMap returnPayload1 = new HashMap();
            returnPayload1.put(ServerInvokerCallbackHandler.CALLBACK_ID, command + "1");
            returnPayload1.put(ServerInvokerCallbackHandler.CALLBACK_LISTENER, this);
            cb1.setReturnPayload(returnPayload1);
            if (REMOTING_ACKNOWLEDGEMENT_TEST.equals(command))
            {
               returnPayload1.put(ServerInvokerCallbackHandler.REMOTING_ACKNOWLEDGES_PUSH_CALLBACKS, "true");
            }
            else
            {
               returnPayload1.put(APPLICATION_ACKNOWLEDGEMENT_TEST, "true");
            }
            callbackHandler.handleCallback(cb1);

            Callback cb2 = new Callback(command + "2");
            HashMap returnPayload2 = new HashMap();
            returnPayload2.put(ServerInvokerCallbackHandler.CALLBACK_ID, command + "2");
            returnPayload2.put(ServerInvokerCallbackHandler.CALLBACK_LISTENER, this);
            cb2.setReturnPayload(returnPayload2);
            if (REMOTING_ACKNOWLEDGEMENT_TEST.equals(command))
            {
               returnPayload2.put(ServerInvokerCallbackHandler.REMOTING_ACKNOWLEDGES_PUSH_CALLBACKS, "true");
            }
            else
            {
               returnPayload2.put(APPLICATION_ACKNOWLEDGEMENT_TEST, "true");
            }
            callbackHandler.handleCallback(cb2);
         }
         return null;
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         callbackHandlers.add(callbackHandler);
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
      }

      public void acknowledgeCallback(InvokerCallbackHandler callbackHandler,
                                      Object callbackId, Object response)
      {
         callbacksAcknowledged++;
         if (response != null)
            callbackResponses.add(response);
      }
   }

   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      public int callbacksReceived;
      private Client client;
      private int id;

      public TestCallbackHandler()
      {
         this(null);
      }
      public TestCallbackHandler(Client client)
      {
         this(client, 0);
      }
      public TestCallbackHandler(Client client, int id)
      {
         this.client = client;
         this.id = id;
      }

      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("entering handleCallback()");
         callbacksReceived++;

         Map returnMap = callback.getReturnPayload();
         if (returnMap.get(APPLICATION_ACKNOWLEDGEMENT_TEST) == null)
            return;

         String test = (String) callback.getParameter();
         try
         {
            client.acknowledgeCallback(this, callback, test + ": " + id + ": " + callbacksReceived);
         }
         catch (Throwable e)
         {
            log.error(e);
            e.printStackTrace();
            throw new HandleCallbackException(e.getMessage());
         }
      }
   }
}
