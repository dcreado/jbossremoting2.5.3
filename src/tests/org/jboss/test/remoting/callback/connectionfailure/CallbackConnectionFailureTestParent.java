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
package org.jboss.test.remoting.callback.connectionfailure;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.LeasePinger;
import org.jboss.remoting.MicroRemoteClientInvoker;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * 
 * Parent of unit tests for JBREM-873.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Dec 10, 2007
 * </p>
 */
abstract public class CallbackConnectionFailureTestParent extends TestCase
{
   private static Logger log = Logger.getLogger(CallbackConnectionFailureTestParent.class);
   
   private static boolean firstTime = true;
   private static String CALLBACK = "callback";
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;
   protected TestConnectionListener connectionListener;

   
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

   
   public void testCallbackConnectionFailureWithListener() throws Throwable
   {
      log.info("entering " + getName());
      doTest(true);
      log.info(getName() + " PASSES");
   }


   public void testCallbackConnectionFailureWithoutListener() throws Throwable
   {
      log.info("entering " + getName());
      doTest(false);
      log.info(getName() + " PASSES");
   }
   
   
   public void testCallbackConnectionFailureTwoClients() throws Throwable
   {
      // Start server.
      setupServer(true, 2);
      
      // Create clients.
      InvokerLocator clientLocator1 = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("enableLease", "true");
      
      addExtraClientConfig(clientConfig);
      Client client1 = new Client(clientLocator1, clientConfig);
      client1.connect();
      log.info("client1 is connected: " + client1.getSessionId());
      // Force the Clients to use distinct invokers.
      InvokerLocator clientLocator2 = new InvokerLocator(locatorURI + "/?a=b");
      Client client2 = new Client(clientLocator2, clientConfig);
      client2.connect();
      log.info("client2 is connected: " + client2.getSessionId());
      assertNotSame(client1.getInvoker(), client2.getInvoker());
      
      // Test connections.
      assertEquals("abc", client1.invoke("abc"));
      assertEquals("xyz", client1.invoke("xyz"));
      log.info("connections are good");
      
      // Add callback handlers.
      TestCallbackHandler callbackHandler1 = new TestCallbackHandler();
      Map metadata = new HashMap();
      addExtraCallbackMetadata(metadata);
      int callbackPort = PortUtil.findFreePort(host);
      metadata.put(Client.CALLBACK_SERVER_PORT, Integer.toString(callbackPort));
      client1.addListener(callbackHandler1, metadata, null, true);
      client1.invoke(CALLBACK);
      assertEquals(1, callbackHandler1.count);
      log.info("first callback handler is installed");
      
      TestCallbackHandler callbackHandler2 = new TestCallbackHandler();
      callbackPort = PortUtil.findFreePort(host);
      metadata.put(Client.CALLBACK_SERVER_PORT, Integer.toString(callbackPort));
      client2.addListener(callbackHandler2, metadata, null, true);
      client2.invoke(CALLBACK);
      assertEquals(2, callbackHandler1.count);
      assertEquals(1, callbackHandler2.count);
      log.info("second callback handler is installed");
      
      // Verify that first callback Client is disconnected when lease fails,
      // but second callback Client is not disconnected.
      
      // 1. Kill LeasePinger for Client 1.
      ClientInvoker invoker = client1.getInvoker();
      Field field = MicroRemoteClientInvoker.class.getDeclaredField("leasePinger");
      field.setAccessible(true);
      LeasePinger pinger = (LeasePinger) field.get(invoker);
      field = LeasePinger.class.getDeclaredField("timerTask");
      field.setAccessible(true);
      TimerTask timerTask = (TimerTask) field.get(pinger);
      timerTask.cancel();
      log.info("stopped LeasePinger");
      
      // 2. Verify that first callback Client is disconnected.
      TestInvocationHandler2 tih = (TestInvocationHandler2) invocationHandler;
      ServerInvokerCallbackHandler sich1 = tih.callbackHandlers[0];
      Client callbackClient1 = sich1.getCallbackClient();
      Thread.sleep(12000);
      assertFalse(callbackClient1.isConnected());
      log.info("first callback Client is disconnected");
      
      // 2. Verify that second callback Client is not disconnected.
      ServerInvokerCallbackHandler sich2 = tih.callbackHandlers[1];
      Client callbackClient2 = sich2.getCallbackClient();
      assertTrue(callbackClient2.isConnected());
      log.info("second callback Client is connected");

      client1.removeListener(callbackHandler1);
      client1.disconnect();
      client2.removeListener(callbackHandler1);
      client2.disconnect();
      shutdownServer();
   }
   
   
   protected void doTest(boolean registerAsListener) throws Throwable
   {
      // Start server.
      setupServer(registerAsListener, 1);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("enableLease", "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Add callback handler.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      Map metadata = new HashMap();
      addExtraCallbackMetadata(metadata);
      client.addListener(callbackHandler, metadata, null, true);
      client.invoke(CALLBACK);
      assertEquals(1, callbackHandler.count);
      log.info("callback handler is installed");
      
      // Verify that callback Client is disconnected when lease fails.
      
      // 1. Kill LeasePinger.
      ClientInvoker invoker = client.getInvoker();
      Field field = MicroRemoteClientInvoker.class.getDeclaredField("leasePinger");
      field.setAccessible(true);
      LeasePinger pinger = (LeasePinger) field.get(invoker);
      field = LeasePinger.class.getDeclaredField("timerTask");
      field.setAccessible(true);
      TimerTask timerTask = (TimerTask) field.get(pinger);
      timerTask.cancel();
      log.info("stopped LeasePinger");
      
      // 2. Verify that callback Client is disconnected.
      ServerInvokerCallbackHandler sich = invocationHandler.callbackHandler;
      Client callbackClient = sich.getCallbackClient();
      Thread.sleep(12000);
      assertEquals(registerAsListener, !callbackClient.isConnected());
      log.info("callback Client is disconnected");
      
      // 3. Verify that testConnectionListener gets called.
      Thread.sleep(20000);
      assertTrue(connectionListener.ok);


      client.removeListener(callbackHandler);
      client.disconnect();
      shutdownServer();
   }
   
   
   abstract protected String getTransport();
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   protected void addExtraCallbackMetadata(Map metadata) {}
   protected String extendInvokerLocator(String locatorURI) {return locatorURI;}
   

   protected void setupServer(boolean registerAsListener, int handlerVersion) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port;
      locatorURI = extendInvokerLocator(locatorURI);
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put("clientLeasePeriod", "2000");
      config.put(ServerInvoker.REGISTER_CALLBACK_LISTENER, Boolean.toString(registerAsListener));
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      
      if (handlerVersion == 1)
         invocationHandler = new TestInvocationHandler();
      else
         invocationHandler = new TestInvocationHandler2();
      
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
      connectionListener = new TestConnectionListener();
      connector.addConnectionListener(connectionListener);
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public ServerInvokerCallbackHandler callbackHandler;
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         this.callbackHandler = (ServerInvokerCallbackHandler) callbackHandler;
      }
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         if (CALLBACK.equals(invocation.getParameter()))
         {
            callbackHandler.handleCallback(new Callback(CALLBACK));
         }
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class TestInvocationHandler2 extends TestInvocationHandler
   {
      public ServerInvokerCallbackHandler[] callbackHandlers = new ServerInvokerCallbackHandler[2];
      private int i = 0;
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         callbackHandlers[i++] = (ServerInvokerCallbackHandler) callbackHandler;
         log.info("callback handlers registered: " + i);
      }
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         if (CALLBACK.equals(invocation.getParameter()))
         {
            if (i > 0)
               callbackHandlers[0].handleCallback(new Callback(CALLBACK));
            
            if (i > 1)
               callbackHandlers[1].handleCallback(new Callback(CALLBACK));
         }
         return invocation.getParameter();
      }
   }
   
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      public int count;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         count++;
         log.info(this + " received callback");
      }  
   }
   
   
   static class TestConnectionListener implements ConnectionListener
   {
      public boolean ok;
      
      public void handleConnectionException(Throwable throwable, Client client)
      {
         try {Thread.sleep(20000);} catch (InterruptedException e) {}
         ok = true;
         log.info("connection failed: " + client.getSessionId());
      }  
   }
}