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
package org.jboss.test.remoting.callback.asynch;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;

/** 
 * AsynchCallbackTestClientRoot and AsynchCallbackTestServerRoot are the parent classes
 * for a set of transport specific tests of the asynchronous callback facility.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2920 $
 * <p>
 * Copyright Nov 25, 2006
 * </p>
 */
public abstract class AsynchCallbackTestClientRoot extends TestCase
{
   private static Logger log = Logger.getLogger(AsynchCallbackTestClientRoot.class);

   
   public void testSynchronousCallback() throws Throwable
   {
      String transport = getTransport();
      String host = InetAddress.getLocalHost().getHostName();
      int port = AsynchCallbackTestServerRoot.port;
      String locatorURI = transport + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      addTransportSpecificConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      SampleCallbackHandler callbackHandler = new SampleCallbackHandler();
      client.addListener(callbackHandler, null, null, true);
      log.info("client added callback handler");
      client.invokeOneway(AsynchCallbackTestServerRoot.SYNCHRONOUS_TEST);
      Thread.sleep(1000);
      // Should still be waiting on client.
      Boolean done = (Boolean) client.invoke(AsynchCallbackTestServerRoot.GET_STATUS);
      log.info("done 1: " + done);
      assertFalse(done.booleanValue());
      Thread.sleep(8000);
      done = (Boolean) client.invoke(AsynchCallbackTestServerRoot.GET_STATUS);
      log.info("done 2: " + done);
      assertTrue(done.booleanValue());
      assertTrue(callbackHandler.receivedCallback);
      client.invoke(AsynchCallbackTestServerRoot.RESET);
      client.removeListener(callbackHandler);
      log.info("disconnecting");
      client.disconnect();
      log.info("disconnected");
   }
   
   
   public void testASynchronousCallbackClientSide() throws Throwable
   {
      String transport = getTransport();
      String host = InetAddress.getLocalHost().getHostName();
      int port = AsynchCallbackTestServerRoot.port;
      String locatorURI = transport + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      addTransportSpecificConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      SampleCallbackHandler callbackHandler = new SampleCallbackHandler();
      client.addListener(callbackHandler, null, null, true);
      log.info("client added callback handler");
      client.invokeOneway(AsynchCallbackTestServerRoot.ASYNCHRONOUS_CLIENT_SIDE_TEST);
      Thread.sleep(4000);
      // Should have returned.
      Boolean done = (Boolean) client.invoke(AsynchCallbackTestServerRoot.GET_STATUS);
      assertTrue(done.booleanValue());
      assertTrue(callbackHandler.receivedCallback);
      client.invoke(AsynchCallbackTestServerRoot.RESET);
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   public void testASynchronousCallbackServerSide() throws Throwable
   {
      String transport = getTransport();
      String host = InetAddress.getLocalHost().getHostName();
      int port = AsynchCallbackTestServerRoot.port;
      String locatorURI = transport + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Connecting to: " + serverLocator);
      HashMap config = new HashMap();
      addTransportSpecificConfig(config);
      Client client = new Client(serverLocator, config);
      client.connect();
      log.info("client is connected");
      SampleCallbackHandler callbackHandler = new SampleCallbackHandler();
      client.addListener(callbackHandler, null, null, true);
      log.info("client added callback handler");
      client.invokeOneway(AsynchCallbackTestServerRoot.ASYNCHRONOUS_SERVER_SIDE_TEST);
      Thread.sleep(4000);
      
      // Should have returned.
      Boolean done = (Boolean) client.invoke(AsynchCallbackTestServerRoot.GET_STATUS);
      assertTrue(done.booleanValue());
      assertTrue(callbackHandler.receivedCallback);
      Thread.sleep(5000);
      
      // Callback should be handled.
      assertTrue(callbackHandler.done);
      
      String threadCount = (String) client.invoke(AsynchCallbackTestServerRoot.GET_THREAD_COUNT);
      assertEquals(AsynchCallbackTestServerRoot.THREAD_COUNT, threadCount);
      String queueSize = (String) client.invoke(AsynchCallbackTestServerRoot.GET_QUEUE_SIZE);
      assertEquals(AsynchCallbackTestServerRoot.QUEUE_SIZE, queueSize);
      client.invoke(AsynchCallbackTestServerRoot.RESET);
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   protected abstract String getTransport();
   
   
   protected void addTransportSpecificConfig(Map config)
   {
   }
   
   static class SampleCallbackHandler implements InvokerCallbackHandler
   {
      boolean receivedCallback;
      boolean done;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("received callback");
         receivedCallback = true;
         try
         {
            Thread.sleep(5000);
         }
         catch (InterruptedException e)
         {
         }
         done = true;
      }
   }
}
