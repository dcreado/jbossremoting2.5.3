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
package org.jboss.test.remoting.transport.bisocket.timertask;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanServer;

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
import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.remoting.transport.bisocket.BisocketClientInvoker;
import org.jboss.remoting.transport.bisocket.BisocketServerInvoker;
import org.jboss.remoting.util.SecurityUtility;


/**
 * Unit test for JBREM-1005.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Jun 26, 2008
 * </p>
 */
public class TimerTaskTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(TimerTaskTestCase.class);
   
   private static boolean firstTime = true;
   private static boolean purgeMethodAvailable;
   
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
         
         try
         {
            getDeclaredMethod(Timer.class, "purge", new Class[]{});
            purgeMethodAvailable = true;
         }
         catch (Exception e)
         {
            log.info("Timer.purge() is not available: must be running with jdk 1.4");
         }
      }
   }

   
   public void tearDown()
   {
   }
   
   
   public void testZeroPingFrequency() throws Throwable
   {
      log.info("entering " + getName());
      
      if (!purgeMethodAvailable)
      {
         log.info(getName() + " PASSES (trivially)");
         return;
      }
      
      // Start server.
      setupServer("0");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Set up callback handler.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put(Bisocket.IS_CALLBACK_SERVER, "true");
      client.addListener(callbackHandler, metadata);
      assertEquals(1, callbackHandler.counter);
     
      // Verify ControlMonitorTimerTask is not created.
      Set callbackConnectors = client.getCallbackConnectors(callbackHandler);
      assertEquals(1, callbackConnectors.size());
      Connector callbackConnector = (Connector) callbackConnectors.iterator().next();
      Field field = BisocketServerInvoker.class.getDeclaredField("controlMonitorTimerTask");
      field.setAccessible(true);      
      BisocketServerInvoker serverInvoker = (BisocketServerInvoker) callbackConnector.getServerInvoker();
      TimerTask timerTask = (TimerTask) field.get(serverInvoker);
      assertNull(timerTask);
      log.info("ControlMonitorTimerTask was not created");
      
      // Verify PingTimerTask is not created.
      field = ServerInvoker.class.getDeclaredField("callbackHandlers");
      field.setAccessible(true);
      serverInvoker = (BisocketServerInvoker) connector.getServerInvoker();
      Map callbackHandlers = (Map) field.get(serverInvoker);
      assertEquals(1, callbackHandlers.size());
      Object o = callbackHandlers.values().iterator().next();
      Client callbackClient = ((ServerInvokerCallbackHandler) o).getCallbackClient();
      BisocketClientInvoker clientInvoker = (BisocketClientInvoker) callbackClient.getInvoker();
      field = BisocketClientInvoker.class.getDeclaredField("pingTimerTask");
      field.setAccessible(true);
      timerTask = (TimerTask) field.get(clientInvoker);
      assertNull(timerTask);
      log.info("PingTimerTask was not created");
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNonZeroPingFrequency() throws Throwable
   {
      log.info("entering " + getName());
      
      if (!purgeMethodAvailable)
      {
         log.info(getName() + " PASSES (trivially)");
         return;
      }
      
      // Start server.
      setupServer("20");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Set up callback handler.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put(Bisocket.IS_CALLBACK_SERVER, "true");
      client.addListener(callbackHandler, metadata);
      assertEquals(1, callbackHandler.counter);
     
      // Verify ControlMonitorTimerTask is created.
      Set callbackConnectors = client.getCallbackConnectors(callbackHandler);
      assertEquals(1, callbackConnectors.size());
      Connector callbackConnector = (Connector) callbackConnectors.iterator().next();
      Field field = BisocketServerInvoker.class.getDeclaredField("controlMonitorTimerTask");
      field.setAccessible(true);      
      BisocketServerInvoker serverInvoker = (BisocketServerInvoker) callbackConnector.getServerInvoker();
      TimerTask timerTask = (TimerTask) field.get(serverInvoker);
      assertNotNull(timerTask);
      log.info("ControlMonitorTimerTask was created");
      
      // Verify PingTimerTask is created.
      field = ServerInvoker.class.getDeclaredField("callbackHandlers");
      field.setAccessible(true);
      BisocketServerInvoker callbackServerInvoker = (BisocketServerInvoker) connector.getServerInvoker();
      Map callbackHandlers = (Map) field.get(callbackServerInvoker);
      assertEquals(1, callbackHandlers.size());
      Object o = callbackHandlers.values().iterator().next();
      Client callbackClient = ((ServerInvokerCallbackHandler) o).getCallbackClient();
      BisocketClientInvoker clientInvoker = (BisocketClientInvoker) callbackClient.getInvoker();
      field = BisocketClientInvoker.class.getDeclaredField("pingTimerTask");
      field.setAccessible(true);
      timerTask = (TimerTask) field.get(clientInvoker);
      assertNotNull(timerTask);
      log.info("PingTimerTask was created");
      
      // Get count of ControlMonitorTimerTasks.
      field = BisocketServerInvoker.class.getDeclaredField("timer");
      field.setAccessible(true);
      Timer timer = (Timer) field.get(null);
      field = Timer.class.getDeclaredField("queue");
      field.setAccessible(true);
      Object taskQueue = field.get(timer);
      Field serverTimerfield = taskQueue.getClass().getDeclaredField("size");
      serverTimerfield.setAccessible(true);
      int serverSize = ((Integer) serverTimerfield.get(taskQueue)).intValue();
      log.info("ControlMonitorTimerTasks: " + serverSize);
      
      // Get count of PingTimerTasks.
      field = BisocketClientInvoker.class.getDeclaredField("timer");
      field.setAccessible(true);
      timer = (Timer) field.get(null);
      field = Timer.class.getDeclaredField("queue");
      field.setAccessible(true);
      taskQueue = field.get(timer);
      Field clientTimerfield = taskQueue.getClass().getDeclaredField("size");
      clientTimerfield.setAccessible(true);
      int clientSize = ((Integer) clientTimerfield.get(taskQueue)).intValue();
      log.info("PingTimerTasks: " + clientSize);

      // Shut down callback connection and verify TimerTasks are removed from 
      // Timer queue.
      client.removeListener(callbackHandler);
      serverSize = ((Integer) serverTimerfield.get(taskQueue)).intValue();
      log.info("ControlMonitorTimerTasks: " + serverSize);
      assertEquals(0, serverSize);
      clientSize = ((Integer) clientTimerfield.get(taskQueue)).intValue();
      log.info("PingTimerTasks: " + clientSize);
      assertEquals(0, clientSize);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "bisocket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer(String pingFrequency) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port; 
      locatorURI += "/?" + Bisocket.PING_FREQUENCY + "=" + pingFrequency;
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
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         try
         {
            callbackHandler.handleCallback(new Callback("callback"));
         }
         catch (HandleCallbackException e)
         {
            log.error("Unable to send callback", e);
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
      public int counter;
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         counter++;
         log.info("received callback");
      }  
   }
   
   static private Method getDeclaredMethod(final Class c, final String name, final Class[] parameterTypes)
   throws NoSuchMethodException
   {
      if (SecurityUtility.skipAccessControl())
      {
         Method m = c.getDeclaredMethod(name, parameterTypes);
         m.setAccessible(true);
         return m;
      }

      try
      {
         return (Method) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws NoSuchMethodException
            {
               Method m = c.getDeclaredMethod(name, parameterTypes);
               m.setAccessible(true);
               return m;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NoSuchMethodException) e.getCause();
      }
   }
}