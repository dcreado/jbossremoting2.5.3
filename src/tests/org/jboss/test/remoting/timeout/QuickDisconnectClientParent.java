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
package org.jboss.test.remoting.timeout;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.BidirectionalClientInvoker;
import org.jboss.remoting.transport.ClientInvoker;

/** 
 * QuickDisconnectTestParent verifies that LeasePinger can set its
 * own short timeout value when it is called during Client.disconnect(), so that,
 * even if the server is unavailable, Client.disconnect() can finish quickly.
 * 
 * It also tests that Client can set its own short timeout value when in
 * removeListener().
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2922 $
 * <p>
 * Copyright Jan 24, 2007
 * </p>
 */
public abstract class QuickDisconnectClientParent extends TestCase
{
   protected static Logger log = Logger.getLogger(QuickDisconnectClientParent.class);
   
   protected boolean receivedConnectionException;
   protected int port;

   
   public void setUp() throws Exception
   {
      System.out.println("*******************************************************");
      System.out.println("*****           EXCEPTIONS ARE EXPECTED           *****");
      System.out.println("*******************************************************");
      log.info("entering setUp() for " + getName());
      Client client = null;
      
      // Ask server to create a Connector.
      try
      {
         String host = InetAddress.getLocalHost().getHostAddress();
         int mainPort = QuickDisconnectServerParent.port;
         String locatorURI = getTransport() + "://" + host + ":" + mainPort;
         InvokerLocator locator = new InvokerLocator(locatorURI);
         client = new Client(locator);
         client.connect();
         Object response = client.invoke(QuickDisconnectServerParent.START_SERVER);
         port = ((Integer) response).intValue();
         Thread.sleep(1000);
         log.info("leaving setUp() for " + getName());
      }
      catch (Throwable e)
      {
         e.printStackTrace();
      }
   }

   
   /**
    * This test starts a server, connects a client to it, disables the server,
    * and sets the client's disconnectTimeout to 0.  The client should be
    * able to terminate the lease and disconnect quickly even though it cannot
    * complete an invocation on the server.
    */
   public void testQuickDisconnectZeroTimeout() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.TIMEOUT, "60000");
      config.put(Client.ENABLE_LEASE, "true");
      addClientConfig(config);
      final Client client = new Client(locator, config);
      try
      {
         client.connect();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      log.info("making first invocation");
      Object response = client.invoke("test");
      assertEquals("test", response);
      log.info("first invocation succeeds");
      
      
      new Thread()
      {
         public void run()
         {
            try
            {
               // Wait for the server to be disabled.
               Thread.sleep(15000);
               
               try
               {
               // This invocation may use up a listening connection,
               // depending on transport.
                  HashMap metadata = new HashMap();
                  metadata.put("timeout", shortTimeoutString());
                  log.info("making invocation");
                  client.invoke("test", metadata);
                  log.info("made invocation");
               }
               catch (Exception e)
               {
                  log.info("client.invoke(\"test\") failed (that's OK)");
               }
               
               
               // Set disconnectTimeout to 0.
               log.info("calling client.disconnect()");
               client.setDisconnectTimeout(0);
               client.disconnect();
               log.info("returned from client.disconnect()");
            }
            catch (Throwable e)
            {
               log.info("error in client.disconnect()", e);
            }
         }
      }.start();
      
      // It should take the Client a little while for LeasePinger's attempts to contact
      // the server to time out.  Wait for about 4 seconds after the call to
      // Client.disconnect() and then verify that the Client has successfully
      // disconnected even though the server is disabled.
      Thread.sleep(21000);
      assertFalse(client.isConnected());
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * This test starts a server, connects a client to it, disables the server,
    * and sets the client's disconnectTimeout to 1 second.  The client should be
    * able to terminate the lease and disconnect quickly even though it cannot
    * complete an invocation on the server.
    */
   public void testQuickDisconnect() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.TIMEOUT, "60000");
      config.put(Client.ENABLE_LEASE, "true");
      addClientConfig(config);
      final Client client = new Client(locator, config);
      try
      {
         client.connect();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      log.info("making first invocation");
      Object response = client.invoke("test");
      assertEquals("test", response);
      log.info("first invocation succeeds");
      
      
      new Thread()
      {
         public void run()
         {
            try
            {
               // Wait for the server to be disabled.
               Thread.sleep(15000);
               
               try
               {
               // This invocation may use up a listening connection,
               // depending on transport.
                  HashMap metadata = new HashMap();
                  metadata.put("timeout", shortTimeoutString());
                  log.info("making invocation");
                  client.invoke("test", metadata);
                  log.info("made invocation");
               }
               catch (Exception e)
               {
                  log.info("client.invoke(\"test\") failed (that's OK)");
               }
               
               
               // Set disconnectTimeout to 1 second.
               log.info("calling client.disconnect()");
               client.setDisconnectTimeout(shortTimeout());
               client.disconnect();
               log.info("returned from client.disconnect()");
            }
            catch (Throwable e)
            {
               log.info("error in client.disconnect()", e);
            }
         }
      }.start();
      
      // It should take the Client a little while for LeasePinger's attempts to contact
      // the server to time out.  Wait for about 4 seconds after the call to
      // Client.disconnect() and then verify that the Client has successfully
      // disconnected even though the server is disabled.
      Thread.sleep(21000);
      assertFalse(client.isConnected());
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * This test is identical to testQuickDisconnectTimeout() except that the
    * disconnectTimeout value is not set on the client.  Therefore, the client
    * will not be able to disconnect as quickly as it does in
    * testQuickDisconnectTimeout().
    * 
    * This test fails in linux - apparently the connection gets feedback from a
    * failed i/o operation more quickly, and the call to Client.disconnect() is
    * able to conclude.
    * 
    * Therefore, the test has been disabled.  That's OK, since it doesn't verify
    * any functionality.  It was intended only to verify the design of
    * testQuickDisconnect(), and it passes in Windows.
    */
   public void xtestSlowDisconnect() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.TIMEOUT, "60000");
      config.put(Client.ENABLE_LEASE, "true");
      addClientConfig(config);
      final Client client = new Client(locator, config);
      try
      {
         client.connect();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      log.info("making first invocation");
      Object response = client.invoke("test");
      assertEquals("test", response);
      log.info("first invocation succeeds");
      
      Thread t = new Thread()
      {
         public void run()
         {
            try
            {
               // Wait for the server to be disabled.
               Thread.sleep(15000);
               
               try
               {
               // This invocation may use up a listening connection,
               // depending on transport.
                  HashMap metadata = new HashMap();
                  metadata.put("timeout", shortTimeoutString());
                  log.info("making invocation");
                  client.invoke("test", metadata);
                  log.info("made invocation");
               }
               catch (Exception e)
               {
                  log.info("client.invoke(\"test\") failed (that's OK)");
               }

               log.info("calling client.disconnect()");
               client.disconnect();
               log.info("returned from client.disconnect()");
            }
            catch (Throwable e)
            {
               log.info("error in client.disconnect()", e);
            }
         }
      };
      
      t.setDaemon(true);
      t.start();
      
      // Since no disconnectTimeout has been specified, timeout value specified
      // when the Client was created, 60 seconds, will apply.  The client should
      // not be disconnected after rougly 8 seconds.
      Thread.sleep(20000);
      assertTrue(client.isConnected());
      log.info(getName() + " PASSES");
   }
  
   
   /**
    * This test starts a server, connects a client to it, disables the server,
    * and sets the client's disconnectTimeout to 0.  The client should be
    * able to remove the callback handler quickly even though it cannot
    * complete an invocation on the server.
    * 
    * Push callbacks are used.
    */
   public void testQuickRemovePushListenerZeroTimeout() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.TIMEOUT, "60000");
      config.put(Client.ENABLE_LEASE, "true");
      addClientConfig(config);
      final Client client = new Client(locator, config);
      try
      {
         client.connect();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      log.info("making first invocation");
      Object response = client.invoke("test");
      assertEquals("test", response);
      log.info("first invocation succeeds");
      final InvokerCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, new HashMap(), null, true);
      
      final Holder removeListener = new Holder();
      new Thread()
      {
         public void run()
         {
            try
            {
               // Wait for the server to be disabled.
               Thread.sleep(15000);
               
               try
               {
               // This invocation may use up a listening connection,
               // depending on transport.
                  HashMap metadata = new HashMap();
                  metadata.put("timeout", shortTimeoutString());
                  log.info("making invocation");
                  client.invoke("test", metadata);
                  log.info("made invocation");
               }
               catch (Exception e)
               {
                  log.info("client.invoke(\"test\") failed (that's OK)");
               }
               
               // Set disconnectTimeout to 0.
               client.setDisconnectTimeout(0);
               client.removeListener(callbackHandler);
               removeListener.done = true;
               log.info("returned from client.removeListener()");
            }
            catch (Throwable e)
            {
               log.info("error in client.removeListener()", e);
            }
         }
      }.start();
      
      
      // Verify that a callback Connector has been created.
      Field field = Client.class.getDeclaredField("callbackConnectors");
      field.setAccessible(true);
      Map callbackConnectors = (Map) field.get(client);
      assertEquals(1, callbackConnectors.size());
      
      // Wait for about 4 seconds after the call to Client.removeListener() and then
      // verify that the Client has successfully removed the callback handler even
      // though the server is disabled.
      Thread.sleep(21000);
      assertEquals(0, callbackConnectors.size());
      
      // Verify that attempt to remove callback handler on server has timed out and
      // client was able to complete call to removeListener().
      assertTrue(removeListener.done);
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * This test starts a server, connects a client to it, disables the server,
    * and sets the client's disconnectTimeout to 0.  The client should be
    * able to remove the callback handler quickly even though it cannot
    * complete an invocation on the server.
    * 
    * If the transport is not bidirectional, pull callbacks are used.
    */
   public void testQuickRemovePullListenerZeroTimeout() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.TIMEOUT, "60000");
      config.put(Client.ENABLE_LEASE, "true");
      addClientConfig(config);
      final Client client = new Client(locator, config);
      try
      {
         client.connect();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      
      // Client won't set up pull callbacks for bidirectional transport.
      ClientInvoker invoker = client.getInvoker();
      if (invoker instanceof BidirectionalClientInvoker)
      {
         log.info(getTransport() + " is bidirectional");
         log.info(getName() + " PASSES");
         return;
      }
      
      log.info("making first invocation");
      Object response = client.invoke("test");
      assertEquals("test", response);
      log.info("first invocation succeeds");
      final InvokerCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, new HashMap(), null, false);
      
      final Holder removeListener = new Holder();
      new Thread()
      {
         public void run()
         {
            try
            {
               // Wait for the server to be disabled.
               Thread.sleep(15000);
               
               try
               {
               // This invocation may use up a listening connection,
               // depending on transport.
                  HashMap metadata = new HashMap();
                  metadata.put("timeout", shortTimeoutString());
                  log.info("making invocation");
                  client.invoke("test", metadata);
                  log.info("made invocation");
               }
               catch (Exception e)
               {
                  log.info("client.invoke(\"test\") failed (that's OK)");
               }
               
               // Set disconnectTimeout to 0.
               client.setDisconnectTimeout(0);
               client.removeListener(callbackHandler);
               removeListener.done = true;
               log.info("returned from client.removeListener()");
            }
            catch (Throwable e)
            {
               log.info("error in client.removeListener()", e);
            }
         }
      }.start();
      
      
      // Verify that a callback Connector has been created.
      Field field = Client.class.getDeclaredField("callbackPollers");
      field.setAccessible(true);
      Map callbackPollers = (Map) field.get(client);
      assertEquals(1, callbackPollers.size());
      
      // Wait for about 4 seconds after the call to Client.removeListener() and then
      // verify that the Client has successfully removed the callback handler even
      // though the server is disabled.
      Thread.sleep(21000);
      assertEquals(0, callbackPollers.size());
      
      // Verify that attempt to remove callback handler on server has timed out and
      // client was able to complete call to removeListener().
      assertTrue(removeListener.done);
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * This test starts a server, connects a client to it, disables the server,
    * and sets the client's disconnectTimeout to 1 second.  The client should be
    * able to remove the callback handler quickly even though it cannot
    * complete an invocation on the server.
    * 
    * Push callbacks are used.
    */
   public void testQuickRemovePushListener() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.TIMEOUT, "60000");
      config.put(Client.ENABLE_LEASE, "true");
      addClientConfig(config);
      final Client client = new Client(locator, config);
      try
      {
         client.connect();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      log.info("making first invocation");
      Object response = client.invoke("test");
      assertEquals("test", response);
      log.info("first invocation succeeds");
      final InvokerCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, new HashMap(), null, true);
      
      final Holder removeListener = new Holder();
      new Thread()
      {
         public void run()
         {
            try
            {
               // Wait for the server to be disabled.
               Thread.sleep(15000);
               
               try
               {
               // This invocation may use up a listening connection,
               // depending on transport.
                  HashMap metadata = new HashMap();
                  metadata.put("timeout", shortTimeoutString());
                  log.info("making invocation");
                  client.invoke("test", metadata);
                  log.info("made invocation");
               }
               catch (Exception e)
               {
                  log.info("client.invoke(\"test\") failed (that's OK)");
               }
               
               // Set disconnectTimeout to 1 second.
               client.setDisconnectTimeout(shortTimeout());
               log.info("calling client.removeListener()");
               client.removeListener(callbackHandler);
               removeListener.done = true;
               log.info("returned from client.removeListener()");
            }
            catch (Throwable e)
            {
               log.info("error in client.removeListener()", e);
            }
         }
      }.start();
      
      
      // Verify that a callback Connector has been created.
      Field field = Client.class.getDeclaredField("callbackConnectors");
      field.setAccessible(true);
      Map callbackConnectors = (Map) field.get(client);
      assertEquals(1, callbackConnectors.size());
      
      // Wait for about 4 seconds after the call to Client.removeListener() and then
      // verify that the Client has successfully removed the callback handler even
      // though the server is disabled.
      Thread.sleep(21000);
      assertEquals(0, callbackConnectors.size());
      
      // Verify that attempt to remove callback handler on server has timed out and
      // client was able to complete call to removeListener().
      assertTrue(removeListener.done);
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * This test starts a server, connects a client to it, disables the server,
    * and sets the client's disconnectTimeout to 1 second.  The client should be
    * able to remove the callback handler quickly even though it cannot
    * complete an invocation on the server.
    * 
    * If the transport is not bidirectional, pull callbacks are used.
    */
   public void testQuickRemovePullListener() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.TIMEOUT, "60000");
      config.put(Client.ENABLE_LEASE, "true");
      addClientConfig(config);
      final Client client = new Client(locator, config);
      try
      {
         client.connect();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      
      // Client won't set up pull callbacks for bidirectional transport.
      ClientInvoker invoker = client.getInvoker();
      if (invoker instanceof BidirectionalClientInvoker)
      {
         log.info(getTransport() + " is bidirectional");
         log.info(getName() + " PASSES");
         return;
      }
      
      log.info("making first invocation");
      Object response = client.invoke("test");
      assertEquals("test", response);
      log.info("first invocation succeeds");
      final InvokerCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, new HashMap(), null, false);
      
      final Holder removeListener = new Holder();
      new Thread()
      {
         public void run()
         {
            try
            {
               // Wait for the server to be disabled.
               Thread.sleep(15000);
               
               try
               {
               // This invocation may use up a listening connection,
               // depending on transport.
                  HashMap metadata = new HashMap();
                  metadata.put("timeout", shortTimeoutString());
                  log.info("making invocation");
                  client.invoke("test", metadata);
                  log.info("made invocation");
               }
               catch (Exception e)
               {
                  log.info("client.invoke(\"test\") failed (that's OK)");
               }
               
               // Set disconnectTimeout to 1 second.
               client.setDisconnectTimeout(shortTimeout());
               log.info("calling client.removeListener()");
               client.removeListener(callbackHandler);
               removeListener.done = true;
               log.info("returned from client.removeListener()");
            }
            catch (Throwable e)
            {
               log.info("error in client.removeListener()", e);
            }
         }
      }.start();
      
      
      // Verify that a CallbackPoller has been created.
      Field field = Client.class.getDeclaredField("callbackPollers");
      field.setAccessible(true);
      Map callbackPollers = (Map) field.get(client);
      assertEquals(1, callbackPollers.size());
      
      // Wait for about 4 seconds after the call to Client.removeListener() and then
      // verify that the Client has successfully removed the callback handler even
      // though the server is disabled.
      Thread.sleep(21000);
      assertEquals(0, callbackPollers.size());
      
      // Verify that attempt to remove callback handler on server has timed out and
      // client was able to complete call to removeListener().
      assertTrue(removeListener.done);
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * This test is identical to testQuickRemoveListener() except that the
    * disconnectTimeout value is not set on the client.  Therefore, the client
    * will not be able to remove the callback handler as quickly as it does in
    * testQuickRemoveListener().
    * 
    * This test fails in linux - apparently the connection gets feedback from a
    * failed i/o operation more quickly, and the call to Client.removeListener()
    * is able to conclude.
    * 
    * Therefore, the test has been disabled.  That's OK, since it doesn't verify
    * any functionality.  It was intended only to verify the design of
    * testQuickRemoveListener(), and it passes in Windows.
    */
   public void xtestSlowRemoveListener() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.TIMEOUT, "60000");
      config.put(Client.ENABLE_LEASE, "true");
      addClientConfig(config);
      final Client client = new Client(locator, config);
      try
      {
         client.connect();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      log.info("making first invocation");
      Object response = client.invoke("test");
      assertEquals("test", response);
      
      final InvokerCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, new HashMap(), null, true);
      
      final Holder removeListener = new Holder();
      new Thread()
      {
         public void run()
         {
            try
            {
               // Wait for the server to be disabled.
               Thread.sleep(15000);
               
               try
               {
               // This invocation may use up a listening connection,
               // depending on transport.
                  log.info("making invocation");
                  client.invoke("test");
                  log.info("made invocation");
               }
               catch (Exception e)
               {
                  log.info("client.invoke(\"test\") failed (that's OK)");
               }
               
               log.info("calling client.removeListener()");
               client.removeListener(callbackHandler);
               removeListener.done = true;
               log.info("returned from client.removeListener()");
            }
            catch (Throwable e)
            {
               log.info("error in client.removeListener()", e);
            }
         }
      }.start();
      
      // Verify that a callback Connector has been created.
      Field field = Client.class.getDeclaredField("callbackConnectors");
      field.setAccessible(true);
      Map callbackConnectors = (Map) field.get(client);
      assertEquals(1, callbackConnectors.size());

      // Wait for about 4 seconds after the call to Client.removeListener() and then
      // verify that the Client has not been able to complete the call to removeListener().
      Thread.sleep(21000);
      assertEquals(1, callbackConnectors.size());
      
      // Verify that attempt to remove callback handler from server has not timed out
      // and client was not able to shut down callback Connector.
      assertFalse(removeListener.done);
      log.info(getName() + " PASSES");
   }
   
   
   public void handleConnectionException(Throwable throwable, Client client)
   {
      receivedConnectionException = true;
   }
   
   
   protected abstract String getTransport();

   
   protected void addClientConfig(Map config)
   {  
   }
   
   protected int shortTimeout()
   {
      return 1000;
   }
   
   protected String shortTimeoutString()
   {
      return "1000";
   }
   
   public class Holder {public boolean done;}
   
   
   public class TestHandler implements ServerInvocationHandler
   {

      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }

      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
   }
  
   
   public class TestListener implements ConnectionListener
   {
      public boolean notified;
      
      public void handleConnectionException(Throwable throwable, Client client)
      {
         notified = true;
      }
   }
   
   
   public class TestCallbackHandler implements InvokerCallbackHandler
   {
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("received callback");
      }
   }
}
