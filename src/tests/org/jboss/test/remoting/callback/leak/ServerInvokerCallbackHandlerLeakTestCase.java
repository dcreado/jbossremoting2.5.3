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

package org.jboss.test.remoting.callback.leak;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.ConnectionNotifier;
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
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit tests for JBREM-1113.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright Apr 13, 2009
 * </p>
 */
public class ServerInvokerCallbackHandlerLeakTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ServerInvokerCallbackHandlerLeakTestCase.class);
   
   private static boolean firstTime = true;
   private static int COUNT = 10;
   private static Object lock = new Object();
   
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
      TestConnectionListener.count = 0;
   }

   
   public void tearDown()
   {
   }
   
   
   public void testLeakWithCallbackHandlersListening() throws Throwable
   {
      doLeakTest(true);
   }
   
   
   public void testLeakWithoutCallbackHandlersListening() throws Throwable
   {
      doLeakTest(false);
   }
   
   
   public void doLeakTest(boolean registerCallbackListener) throws Throwable
   {
      log.info("entering " + getName());
      setupServer(registerCallbackListener);
      
      // Get fields.
      ServerInvoker serverInvoker = connector.getServerInvoker();
      Field field = ServerInvoker.class.getDeclaredField("connectionNotifier");
      field.setAccessible(true);
      ConnectionNotifier connectionNotifier = (ConnectionNotifier) field.get(serverInvoker);
      field = ServerInvoker.class.getDeclaredField("callbackHandlers");
      field.setAccessible(true);
      Map callbackHandlers = (Map) field.get(serverInvoker);
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = null;
      
      for (int i = 0; i < COUNT; i++)
      {
         client = new Client(serverLocator, clientConfig);
         client.connect();
         log.info("client is connected");

         // Test connections.
         assertEquals("abc", client.invoke("abc"));
         log.info("connection is good");

         TestCallbackHandler callbackHandler = new TestCallbackHandler();
         client.addListener(callbackHandler, null, null, true);
      }
      
      field = MicroRemoteClientInvoker.class.getDeclaredField("leasePinger");
      field.setAccessible(true);
      LeasePinger pinger = (LeasePinger) field.get(client.getInvoker());
      field = LeasePinger.class.getDeclaredField("timerTask");
      field.setAccessible(true);
      TimerTask timerTask = (TimerTask) field.get(pinger);
      timerTask.cancel();
      
      synchronized(lock)
      {
         lock.wait();
      }
      Thread.sleep(2000);
      
      assertEquals(COUNT, TestConnectionListener.count);
      assertEquals(1, connectionNotifier.size());
      assertTrue(callbackHandlers.isEmpty());
      
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer(boolean registerCallbackListener) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port;
      locatorURI += "/?leasing=true";
      if (registerCallbackListener)
      {
         locatorURI += "&" + ServerInvoker.REGISTER_CALLBACK_LISTENER + "=true";
      }
      else
      {
         locatorURI += "&" + ServerInvoker.REGISTER_CALLBACK_LISTENER + "=false";
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
      connector.setLeasePeriod(2000);
      TestConnectionListener listener = new TestConnectionListener(!registerCallbackListener);
      connector.addConnectionListener(listener);
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      static public Set callbackHandlers = new HashSet();
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         callbackHandlers.add(callbackHandler);
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
   
   
   static class TestConnectionListener implements ConnectionListener
   {
      static public int count;
      boolean shutdownCallbackHandlers;
      
      public TestConnectionListener(boolean shutdownCallbackHandlers)
      {
         this.shutdownCallbackHandlers = shutdownCallbackHandlers;
      }

      public synchronized void handleConnectionException(Throwable throwable, Client client)
      {
         log.info("got connection exception");
         if(++count == COUNT)
         {
            if (shutdownCallbackHandlers)
            {
               Iterator it = TestInvocationHandler.callbackHandlers.iterator();
               while (it.hasNext())
               {
                  ServerInvokerCallbackHandler callbackHandler = (ServerInvokerCallbackHandler) it.next();
                  callbackHandler.shutdown();
                  log.info("shut down: " + callbackHandler);
               }
               TestInvocationHandler.callbackHandlers.clear();
            }
            synchronized(lock)
            {
               lock.notify();
            }
         }
      }  
   }
}