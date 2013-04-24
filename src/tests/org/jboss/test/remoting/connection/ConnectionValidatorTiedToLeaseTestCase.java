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
package org.jboss.test.remoting.connection;

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
import org.jboss.remoting.ConnectionValidator;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.LeasePinger;
import org.jboss.remoting.MicroRemoteClientInvoker;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit tests for JBREM-891.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Jan 19, 2008
 * </p>
 */
public class ConnectionValidatorTiedToLeaseTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ConnectionValidatorTiedToLeaseTestCase.class);
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;
   protected TestConnectionListener serverListener;

   
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
   
   
   public void testTiedToLeaseDefaultConfigurationLocalInvoker() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Register client side listener.
      Object lock = new Object();
      TestConnectionListener clientListener = new TestConnectionListener(lock);
      client.addConnectionListener(clientListener);
      log.info("connection listener added on client side");
      
      // Verify ConnectionValidator is running.
      assertTrue(client.getPingPeriod() > -1);
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Verify LeasePinger is running.
      assertTrue(client.getInvoker() instanceof MicroRemoteClientInvoker);
      ClientInvoker clientInvoker = client.getInvoker();
      Field field = MicroRemoteClientInvoker.class.getDeclaredField("leasePinger");
      field.setAccessible(true);
      LeasePinger pinger = (LeasePinger) field.get(clientInvoker);
      assertNotNull(pinger);
      field = LeasePinger.class.getDeclaredField("timerTask");
      field.setAccessible(true);
      TimerTask timerTask = (TimerTask) field.get(pinger);
      assertNotNull(timerTask);
      
      // Shut down lease on server side.
      connector.removeConnectionListener(serverListener);
      
      // Wait for client side listener to be notified.
      synchronized (lock)
      {
         lock.wait(30000);
      }
      
      // Verify ConnectionValidator has stopped.
      Thread.sleep(4000);
      assertTrue(clientListener.notified);
      assertEquals(-1, client.getPingPeriod());
      
      // Verfiy LeasePinger has stopped.
      timerTask = (TimerTask) field.get(pinger);
      assertNull(timerTask);
      
      client.removeConnectionListener(clientListener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testTiedToLeaseDefaultConfigurationRemoteInvoker() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Register client side listener.
      Object lock = new Object();
      TestConnectionListener clientListener = new TestConnectionListener(lock);
      Map metadata = new HashMap();
      metadata.put(InvokerLocator.FORCE_REMOTE, "true");
      client.addConnectionListener(clientListener, metadata);
      log.info("connection listener added on client side");
      
      // Verify ConnectionValidator is running.
      assertTrue(client.getPingPeriod() > -1);
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Verify LeasePinger is running.
      assertTrue(client.getInvoker() instanceof MicroRemoteClientInvoker);
      ClientInvoker clientInvoker = client.getInvoker();
      Field field = MicroRemoteClientInvoker.class.getDeclaredField("leasePinger");
      field.setAccessible(true);
      LeasePinger pinger = (LeasePinger) field.get(clientInvoker);
      assertNotNull(pinger);
      field = LeasePinger.class.getDeclaredField("timerTask");
      field.setAccessible(true);
      TimerTask timerTask = (TimerTask) field.get(pinger);
      assertNotNull(timerTask);
      
      // Shut down lease on server side.
      connector.removeConnectionListener(serverListener);
      
      // Wait for client side listener to be notified.
      synchronized (lock)
      {
         lock.wait(30000);
      }
      
      // Verify ConnectionValidator has stopped.
      Thread.sleep(4000);
      assertTrue(clientListener.notified);
      assertEquals(-1, client.getPingPeriod());
      
      // Verfiy LeasePinger has stopped.
      timerTask = (TimerTask) field.get(pinger);
      assertNull(timerTask);
      
      client.removeConnectionListener(clientListener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testTiedToLeaseDontStopLease() throws Throwable
   {
     log.info("entering " + getName());
      
      // Start server.
      setupServer(true);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Register client side listener.
      Object lock = new Object();
      TestConnectionListener clientListener = new TestConnectionListener(lock);
      Map metadata = new HashMap();
      metadata.put(InvokerLocator.FORCE_REMOTE, "true");
      metadata.put(ConnectionValidator.TIE_TO_LEASE, "true");
      metadata.put(ConnectionValidator.STOP_LEASE_ON_FAILURE, "false");
      client.addConnectionListener(clientListener, metadata);
      log.info("connection listener added on client side");
      
      // Verify ConnectionValidator is running.
      assertTrue(client.getPingPeriod() > -1);
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Verify LeasePinger is running.
      assertTrue(client.getInvoker() instanceof MicroRemoteClientInvoker);
      ClientInvoker clientInvoker = client.getInvoker();
      Field field = MicroRemoteClientInvoker.class.getDeclaredField("leasePinger");
      field.setAccessible(true);
      LeasePinger pinger = (LeasePinger) field.get(clientInvoker);
      assertNotNull(pinger);
      field = LeasePinger.class.getDeclaredField("timerTask");
      field.setAccessible(true);
      TimerTask timerTask = (TimerTask) field.get(pinger);
      assertNotNull(timerTask);
      
      // Shut down lease on server side.
      connector.removeConnectionListener(serverListener);
      
      // Wait for client side listener to be notified.
      synchronized (lock)
      {
         lock.wait(30000);
      }
      
      // Verify ConnectionValidator has stopped.
      Thread.sleep(4000);
      assertTrue(clientListener.notified);
      assertEquals(-1, client.getPingPeriod());
      
      // Verfiy LeasePinger has not stopped.
      timerTask = (TimerTask) field.get(pinger);
      assertNotNull(timerTask);
      
      client.removeConnectionListener(clientListener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNotTiedToLeaseDontStopLease() throws Throwable
   {
     log.info("entering " + getName());
      
      // Start server.
      setupServer(true);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Register client side listener.
      Object lock = new Object();
      TestConnectionListener clientListener = new TestConnectionListener(lock);
      Map metadata = new HashMap();
      metadata.put(InvokerLocator.FORCE_REMOTE, "true");
      metadata.put(ConnectionValidator.TIE_TO_LEASE, "false");
      metadata.put(ConnectionValidator.STOP_LEASE_ON_FAILURE, "false");
      client.addConnectionListener(clientListener, metadata);
      log.info("connection listener added on client side");
      
      // Verify ConnectionValidator is running.
      assertTrue(client.getPingPeriod() > -1);
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Verify LeasePinger is running.
      assertTrue(client.getInvoker() instanceof MicroRemoteClientInvoker);
      ClientInvoker clientInvoker = client.getInvoker();
      Field field = MicroRemoteClientInvoker.class.getDeclaredField("leasePinger");
      field.setAccessible(true);
      LeasePinger pinger = (LeasePinger) field.get(clientInvoker);
      assertNotNull(pinger);
      field = LeasePinger.class.getDeclaredField("timerTask");
      field.setAccessible(true);
      TimerTask timerTask = (TimerTask) field.get(pinger);
      assertNotNull(timerTask);
      
      // Shut down lease on server side.
      connector.removeConnectionListener(serverListener);
      
      // Wait for client side listener to be notified.
      synchronized (lock)
      {
         lock.wait(30000);
      }
      
      // Verify ConnectionValidator has not stopped.
      Thread.sleep(4000);
      assertFalse(clientListener.notified);
      assertTrue(client.getPingPeriod() > -1);
      
      // Verfiy LeasePinger has not stopped.
      timerTask = (TimerTask) field.get(pinger);
      assertNotNull(timerTask);
      
      client.removeConnectionListener(clientListener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNotTiedToLeaseStopLease() throws Throwable
   {
     log.info("entering " + getName());
      
      // Start server.
      setupServer(true);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Register client side listener.
      Object lock = new Object();
      TestConnectionListener clientListener = new TestConnectionListener(lock);
      Map metadata = new HashMap();
      metadata.put(InvokerLocator.FORCE_REMOTE, "true");
      metadata.put(ConnectionValidator.TIE_TO_LEASE, "false");
      metadata.put(ConnectionValidator.STOP_LEASE_ON_FAILURE, "true");
      client.addConnectionListener(clientListener, metadata);
      log.info("connection listener added on client side");
      
      // Verify ConnectionValidator is running.
      assertTrue(client.getPingPeriod() > -1);
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Verify LeasePinger is running.
      assertTrue(client.getInvoker() instanceof MicroRemoteClientInvoker);
      ClientInvoker clientInvoker = client.getInvoker();
      Field field = MicroRemoteClientInvoker.class.getDeclaredField("leasePinger");
      field.setAccessible(true);
      LeasePinger pinger = (LeasePinger) field.get(clientInvoker);
      assertNotNull(pinger);
      field = LeasePinger.class.getDeclaredField("timerTask");
      field.setAccessible(true);
      TimerTask timerTask = (TimerTask) field.get(pinger);
      assertNotNull(timerTask);
      
      // Prevent server from responding to ConnectionValidator pings.
      ServerInvoker serverInvoker = connector.getServerInvoker();
      field = ServerInvoker.class.getDeclaredField("started");
      field.setAccessible(true);
      field.set(serverInvoker, new Boolean(false));
      assertFalse(field.getBoolean(serverInvoker));
      log.info("Stopped server invoker");
      
      // Wait for client side listener to be notified.
      synchronized (lock)
      {
         lock.wait(30000);
      }
      
      // Verify ConnectionValidator has stopped.
      Thread.sleep(4000);
      assertTrue(clientListener.notified);
      assertEquals(-1, client.getPingPeriod());
      
      // Verfiy LeasePinger has stopped.
      field = LeasePinger.class.getDeclaredField("timerTask");
      field.setAccessible(true);
      timerTask = (TimerTask) field.get(pinger);
      assertNull(timerTask);
      
      client.removeConnectionListener(clientListener);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testTiedToLeaseServerLeasingTurnedOff() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Register client side listener.
      Object lock = new Object();
      TestConnectionListener clientListener = new TestConnectionListener(lock);
      Map metadata = new HashMap();
      metadata.put(InvokerLocator.FORCE_REMOTE, "true");
      client.addConnectionListener(clientListener, metadata);
      log.info("connection listener added on client side");
      
      // Verify ConnectionValidator is running.
      assertTrue(client.getPingPeriod() > -1);
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Wait for client side listener to be notified.
      synchronized (lock)
      {
         lock.wait(30000);
      }
      
      // Verify ConnectionValidator has not stopped.
      Thread.sleep(4000);
      assertFalse(clientListener.notified);
      assertTrue(client.getPingPeriod() > -1);
      
      client.removeConnectionListener(clientListener);
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
   

   protected void setupServer(boolean startLeasing) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port; 
      locatorURI += "/?" + InvokerLocator.CLIENT_LEASE + "=true";
      locatorURI += "&" + InvokerLocator.CLIENT_LEASE_PERIOD + "=4000";
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.CLIENT_LEASE_PERIOD, "4000");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      if (startLeasing) 
      {
         serverListener = new TestConnectionListener();
         connector.addConnectionListener(serverListener);
      }
      connector.start();
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
      {
         connector.removeConnectionListener(serverListener);
         connector.stop();
      }
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
   
   
   static class TestConnectionListener implements ConnectionListener
   {
      public boolean notified;
      private Object lock;
      
      TestConnectionListener()
      {
         this(new Object());
      }
      TestConnectionListener(Object lock)
      {
         this.lock = lock;
      }
      public void handleConnectionException(Throwable throwable, Client client)
      {
         notified = true;
         synchronized (lock)
         {
            try
            {
               // Give listener a chance to wait on lock.
               Thread.sleep(4000);
            }
            catch (InterruptedException e)
            {
               log.info("Unexpected interrupt in TestConnectionListener");
            }
            lock.notifyAll();
         }
      }
   }
}