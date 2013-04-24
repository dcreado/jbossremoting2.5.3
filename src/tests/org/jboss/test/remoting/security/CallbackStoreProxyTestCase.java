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
package org.jboss.test.remoting.security;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit test for JBREM-977.
 *  
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 4, 2008
 * </p>
 */
public class CallbackStoreProxyTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(CallbackStoreProxyTestCase.class);
   
   private static boolean firstTime = true;
   private static boolean done;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;
   protected TestCallbackStore callbackStore;
   protected MBeanServer server;
   protected ObjectName callbackStoreObjectName;

   
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
   
   
   public void testCallbackStoreProxy() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Setup pull callbacks.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler);
      
      if (!done)
      {
         synchronized (CallbackStoreProxyTestCase.class)
         {
            long start = System.currentTimeMillis();
            while (true)
            {
               try
               {
                  log.info("testCallbackStoreProxy() waiting for notification");
                  CallbackStoreProxyTestCase.class.wait(60000 - (System.currentTimeMillis() - start));
                  log.info("testCallbackStoreProxy() received notification");
                  break;
               }
               catch (InterruptedException e)
               {
                  log.info("interrupted", e);
               }
            }
         }
      }

      // Verify TestCallbackStore got one callback.
//      assertEquals(1, client.getCallbacks(callbackHandler).size());
      int count = ((Integer)server.invoke(callbackStoreObjectName, "size", new Object[]{}, new String[]{})).intValue();
      assertEquals(1, count);
      
      client.removeListener(callbackHandler);
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
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put("callbackMemCeiling", "80");
      
      // Create CallbackStore.
      server = MBeanServerFactory.createMBeanServer();
      TestCallbackStore callbackStore = new TestCallbackStore();
      String objectNameString = "test:type=Callbackstore";
      callbackStoreObjectName = new ObjectName(objectNameString);
      server.registerMBean(callbackStore, callbackStoreObjectName);
      config.put(ServerInvokerCallbackHandler.CALLBACK_STORE_KEY, objectNameString);
      
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      server.registerMBean(connector, new ObjectName("test:type=Connector"));
      
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
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         ((ServerInvokerCallbackHandler) callbackHandler).setShouldPersist(true);
         TestCallbackThread callbackThread = new TestCallbackThread(callbackHandler);
         callbackThread.start();
         
         synchronized (TestCallbackStore.class)
         {
            long start = System.currentTimeMillis();
            while (true)
            {
               try
               {
                  TestCallbackStore.class.wait(60000 - (System.currentTimeMillis() - start));
                  break;
               }
               catch (InterruptedException e)
               {
                  log.info("interrupted", e);
               }
            }
         }
         
         callbackThread.shutdown();
         log.info("addListener() received notification");
         done = true;
         
         synchronized (CallbackStoreProxyTestCase.class)
         {
            CallbackStoreProxyTestCase.class.notifyAll();
         }
      }
      
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
   
   static class TestCallbackThread extends Thread
   {
      private boolean running = true;
      private InvokerCallbackHandler callbackHandler;
      private Callback callback = new Callback(new byte[100]);
      
      public TestCallbackThread(InvokerCallbackHandler callbackHandler)
      {
         this.callbackHandler = callbackHandler;
      }
      
      public void shutdown()
      {
         running = false;
      }
      
      public void run()
      {
         while (running)
         {
            try
            {
               callbackHandler.handleCallback(callback);
            }
            catch (Exception e)
            {
               log.error("Error", e);
               return;
            }
         }
         log.info("shutting down");
      }
   }
}