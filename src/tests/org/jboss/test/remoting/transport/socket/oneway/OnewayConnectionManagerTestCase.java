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
package org.jboss.test.remoting.transport.socket.oneway;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;
import org.jboss.remoting.transport.socket.SocketWrapper;


/**
 * Tests the oneway connection timer task in MicroSocketClientInvoker.
 * 
 * November 3, 2007:  This class was originally written as a unit test for
 * JBREM-706.  It will also serve as unit test for JBREM-843.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 4115 $
 * <p>
 * Copyright Jun 23, 2007
 * </p>
 */
public class OnewayConnectionManagerTestCase extends TestCase
{
   protected static String FAST = "fast";
   protected static String SLOW = "slow";
   protected static String DELAY = "delay";
   
   protected static Logger log = Logger.getLogger(OnewayConnectionManagerTestCase.class);
   protected static boolean firstTime = true;
   protected static boolean go;
   protected static Object lock = new Object();
   
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
   
   
   /**
    * Connections time out.  Uses default oneway timeout.
    */
   public void testDefaultTimeoutWithTimeouts() throws Throwable
   { 
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "socket://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Client.MAX_NUM_ONEWAY_THREADS, "5");
      config.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "1");
      Client client = new Client(locator, config);
      client.connect();
      
      Object o = client.getInvoker();
      assertTrue(o instanceof MicroSocketClientInvoker); 
      MicroSocketClientInvoker invoker = (MicroSocketClientInvoker) o;
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(invoker);
      assertEquals(0, pool.size());
      assertEquals(0, invoker.getNumberOfUsedConnections());
      
      int INVOCATIONS = 5;
      HashMap metadata = new HashMap();
      metadata.put(DELAY, "10000");
      long start = System.currentTimeMillis();
      for (int i = 0; i < INVOCATIONS; i++)
      {
         client.invokeOneway(SLOW + i, metadata, true);
      }

      assertTrue((System.currentTimeMillis() - start < 2000));
      
      for (int i = 0; i < 5; i++)
      {
         Thread.sleep(1000);
         if (INVOCATIONS == invoker.getNumberOfUsedConnections())
            break;
      }

      assertEquals(0, pool.size());
      for (int i = 0; i < 5; i++)
      {
         if (handler.startedCount == INVOCATIONS) break;
         Thread.sleep(1000);
      }
      assertEquals(INVOCATIONS, handler.startedCount);
      Thread.sleep(4000);
      
      // All sockets should have timed out by now.
      assertEquals(0, invoker.getNumberOfUsedConnections());
      assertEquals(0, pool.size());
      Thread.sleep(10000);
      
      // All invocations should be done by now.
      assertEquals(0, invoker.getNumberOfUsedConnections());
      assertEquals(0, pool.size());
      assertEquals(INVOCATIONS, handler.finishedCount);
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Connections don't time out.  Uses default oneway timeout.
    */
   public void testDefaultTimeoutWithNoTimeouts() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "socket://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Client.MAX_NUM_ONEWAY_THREADS, "5");
      config.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "1");
      Client client = new Client(locator, config);
      client.connect();
      
      Object o = client.getInvoker();
      assertTrue(o instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker invoker = (MicroSocketClientInvoker) o;
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(invoker);
      assertEquals(0, pool.size());
      assertEquals(0, invoker.getNumberOfUsedConnections());
      
      int INVOCATIONS = 5;
      HashMap metadata = new HashMap();
      metadata.put(DELAY, "1000");
      long start = System.currentTimeMillis();
      for (int i = 0; i < INVOCATIONS; i++)
      {
         client.invokeOneway(SLOW + i, metadata, true);
      }

      assertTrue((System.currentTimeMillis() - start < 1000));
      Thread.sleep(500);
      assertEquals(INVOCATIONS, invoker.getNumberOfUsedConnections());
      assertEquals(0, pool.size());
      assertEquals(INVOCATIONS, handler.startedCount);
      Thread.sleep(2000);
      
      // All invocations should have finished by now, and all sockets should
      // have been returned to the pool.
      assertEquals(INVOCATIONS, handler.finishedCount);
      assertEquals(0, invoker.getNumberOfUsedConnections());
      assertEquals(INVOCATIONS, pool.size());
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   

   /**
    * Connections time out.  Uses configured oneway timeout.
    */
   public void testConfiguredTimeoutWithTimeouts() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "socket://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Client.MAX_NUM_ONEWAY_THREADS, "5");
      config.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "1");
      config.put(MicroSocketClientInvoker.ONEWAY_CONNECTION_TIMEOUT, "500");
      Client client = new Client(locator, config);
      client.connect();
      
      Object o = client.getInvoker();
      assertTrue(o instanceof MicroSocketClientInvoker); 
      MicroSocketClientInvoker invoker = (MicroSocketClientInvoker) o;
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(invoker);
      assertEquals(0, pool.size());
      assertEquals(0, invoker.getNumberOfUsedConnections());
      
      int INVOCATIONS = 5;
      HashMap metadata = new HashMap();
      metadata.put(DELAY, "1000");
      long start = System.currentTimeMillis();
      for (int i = 0; i < INVOCATIONS; i++)
      {
         client.invokeOneway(SLOW + i, metadata, true);
      }

      assertTrue((System.currentTimeMillis() - start < 1000));
      Thread.sleep(100);
      assertEquals(INVOCATIONS, invoker.getNumberOfUsedConnections());
      assertEquals(0, pool.size());
      assertEquals(INVOCATIONS, handler.startedCount);
      Thread.sleep(2000);
      
      // All sockets should have timed out by now.  Note that they would not have
      // timed out with default oneway timeout.
      assertEquals(0, invoker.getNumberOfUsedConnections());
      assertEquals(0, pool.size());
      Thread.sleep(1000);
      
      // All invocations should be done by now.
      assertEquals(INVOCATIONS, handler.finishedCount);
      assertEquals(0, invoker.getNumberOfUsedConnections());
      assertEquals(0, pool.size());
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Connections don't time out.  Uses configured oneway timeout.
    */
   public void testConfiguredTimeoutWithNoTimeouts() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "socket://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Client.MAX_NUM_ONEWAY_THREADS, "5");
      config.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "1");
      config.put(MicroSocketClientInvoker.ONEWAY_CONNECTION_TIMEOUT, "6000");
      Client client = new Client(locator, config);
      client.connect();
      
      Object o = client.getInvoker();
      assertTrue(o instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker invoker = (MicroSocketClientInvoker) o;
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(invoker);
      assertEquals(0, pool.size());
      assertEquals(0, invoker.getNumberOfUsedConnections());
      
      int INVOCATIONS = 5;
      HashMap metadata = new HashMap();
      metadata.put(DELAY, "4000");
      long start = System.currentTimeMillis();
      for (int i = 0; i < INVOCATIONS; i++)
      {
         client.invokeOneway(SLOW + i, metadata, true);
      }

      assertTrue((System.currentTimeMillis() - start < 1000));
      Thread.sleep(1000);
      assertEquals(INVOCATIONS, invoker.getNumberOfUsedConnections());
      assertEquals(0, pool.size());
      assertEquals(INVOCATIONS, handler.startedCount);
      Thread.sleep(5000);
      
      // All invocations should have finished by now, and all sockets should
      // have been returned to the pool.  Note that sockets would have timeed out
      // if the default oneway timeout value were used.
      assertEquals(INVOCATIONS, handler.finishedCount);
      assertEquals(0, invoker.getNumberOfUsedConnections());
      assertEquals(INVOCATIONS, pool.size());
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Does server side oneway invocations.  Connections don't have to wait
    * for invocation to complete.
    */
   public void testOnewayServerSide() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "socket://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Client.MAX_NUM_ONEWAY_THREADS, "5");
      config.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "1");
      Client client = new Client(locator, config);
      client.connect();
      
      Object o = client.getInvoker();
      assertTrue(o instanceof MicroSocketClientInvoker); 
      MicroSocketClientInvoker invoker = (MicroSocketClientInvoker) o;
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(invoker);
      assertEquals(0, pool.size());
      assertEquals(0, invoker.getNumberOfUsedConnections());
      
      int INVOCATIONS = 5;
      HashMap metadata = new HashMap();
      metadata.put(DELAY, "5000");
      for (int i = 0; i < INVOCATIONS; i++)
      {
         client.invokeOneway(SLOW + i, metadata, false);
         Thread.sleep(500);
      }

      Thread.sleep(2000);
      
      // Ony one connection should have been created, and it should have been
      // returned to the pool by now.
      assertEquals(0, invoker.getNumberOfUsedConnections());
      assertEquals(1, pool.size());
      assertEquals(INVOCATIONS, handler.startedCount);
      Thread.sleep(5000);
      
      // All invocations should be done by now.
      assertEquals(INVOCATIONS, handler.finishedCount);
      assertEquals(0, invoker.getNumberOfUsedConnections());
      assertEquals(1, pool.size());
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Verify connection management works under heavy load, with some timeouts.
    */
   public void testHeavyLoadWithTimeouts() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "socket://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put("MaxPoolSize", "300");
      Connector connector = new Connector(locator, serverConfig);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.MAX_NUM_ONEWAY_THREADS, "100");
      clientConfig.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "100");
      clientConfig.put(MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG, "100");
      clientConfig.put(MicroSocketClientInvoker.ONEWAY_CONNECTION_TIMEOUT, "1000");
      Client client = new Client(locator, clientConfig);
      client.connect();
      
      Object o = client.getInvoker();
      assertTrue(o instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker invoker = (MicroSocketClientInvoker) o;
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(invoker);
      assertEquals(0, pool.size());
      assertEquals(0, invoker.getNumberOfUsedConnections());
      
      int INVOCATIONS = 500;
      OnewayThread[] threads = new OnewayThread[INVOCATIONS];
      
      for (int i = 0; i < INVOCATIONS; i++)
      {
         threads[i] = new OnewayThread(client, i, "5000");
         threads[i].start();
      }
      
      synchronized (lock)
      {
         go = true;
         lock.notifyAll();
      }
      
      Thread.sleep(4000);
      
      // Verify first set of invocations were received.
      assertTrue(100 <= handler.startedCount);
      Thread.sleep(10000);
      
      // Verify rest of invocations were received.
      assertEquals(INVOCATIONS, handler.startedCount);
      Thread.sleep(8000);
      
      // All invocations should be complete by now.
      for (int i = 0; i < INVOCATIONS; i++)
      {
         assertTrue("failure in thread: " + i, threads[i].ok);
      }
      
      assertEquals(0, invoker.getNumberOfUsedConnections());
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Verify connection management works under heavy load, with no timeouts.
    */
   public void testHeavyLoadWithoutTimeouts() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "socket://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put("MaxPoolSize", "300");
      Connector connector = new Connector(locator, serverConfig);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.MAX_NUM_ONEWAY_THREADS, "100");
      clientConfig.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "100");
      clientConfig.put(MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG, "100");
      clientConfig.put(MicroSocketClientInvoker.ONEWAY_CONNECTION_TIMEOUT, "4000");
      Client client = new Client(locator, clientConfig);
      client.connect();
      
      Object o = client.getInvoker();
      assertTrue(o instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker invoker = (MicroSocketClientInvoker) o;
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(invoker);
      assertEquals(0, pool.size());
      assertEquals(0, invoker.getNumberOfUsedConnections());
      
      int INVOCATIONS = 500;
      OnewayThread[] threads = new OnewayThread[INVOCATIONS];
      
      for (int i = 0; i < INVOCATIONS; i++)
      {
         threads[i] = new OnewayThread(client, i, "100");
         threads[i].start();
      }
      
      synchronized (lock)
      {
         go = true;
         lock.notifyAll();
      }
      
      Thread.sleep(4000);
      
      // Verify invocations were received.
      assertEquals(INVOCATIONS, handler.startedCount);
      
      // All invocations should be complete by now.
      for (int i = 0; i < INVOCATIONS; i++)
      {
         assertTrue("failure in thread: " + i, threads[i].ok);
      }
      
      Thread.sleep(2000);
      assertEquals(0, invoker.getNumberOfUsedConnections());
      
      // Commenting out the following test.  If the server is busy, it is possible
      // for some oneway connections to time out, in which case they would not
      // be returned to the connection pool.
      // assertEquals(100, pool.size());
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Make sure timeout value is reset before socket is returned to pool.
    */
   public void testTimeoutReset() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "socket://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Client.MAX_NUM_ONEWAY_THREADS, "5");
      config.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "1");
      config.put(ServerInvoker.TIMEOUT, "5000");
      config.put(MicroSocketClientInvoker.ONEWAY_CONNECTION_TIMEOUT, "7000");
      Client client = new Client(locator, config);
      client.connect();
      
      Object o = client.getInvoker();
      assertTrue(o instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker invoker = (MicroSocketClientInvoker) o;
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      LinkedList pool = (LinkedList) field.get(invoker);
      assertEquals(0, pool.size());
      assertEquals(0, invoker.getNumberOfUsedConnections());
      
      int INVOCATIONS = 5;
      HashMap metadata = new HashMap();
      metadata.put(DELAY, "1000");
      long start = System.currentTimeMillis();
      for (int i = 0; i < INVOCATIONS; i++)
      {
         client.invokeOneway(SLOW + i, metadata, true);
      }

      assertTrue((System.currentTimeMillis() - start < 1000));
      Thread.sleep(500);
      assertEquals(INVOCATIONS, invoker.getNumberOfUsedConnections());
      assertEquals(0, pool.size());
      assertEquals(INVOCATIONS, handler.startedCount);
      Thread.sleep(2000);
      
      // All invocations should have finished by now, and all sockets should
      // have been returned to the pool.
      assertEquals(INVOCATIONS, handler.finishedCount);
      assertEquals(0, invoker.getNumberOfUsedConnections());
      assertEquals(INVOCATIONS, pool.size());
      
      for (int i = 0; i < INVOCATIONS; i++)
      {
         SocketWrapper wrapper = (SocketWrapper) pool.get(i);
         assertEquals("invalid timeout value: socket" + i, 5000, wrapper.getTimeout());
      }
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   public class OnewayThread extends Thread
   {
      boolean ok;
      Client client;
      int id;
      String delay;
      
      public OnewayThread(Client client, int id, String delay)
      {
         this.client = client;
         this.id = id;
         this.delay = delay;
      }
      
      public void run()
      {
         try
         {
            synchronized (lock)
            {
               while (!go)
               {
                  try {lock.wait();} catch (InterruptedException e) {}
               }
            }
            
            HashMap metadata = new HashMap();
            metadata.put(DELAY, delay);
            client.invokeOneway(SLOW + id, metadata, true);
            ok = true;
         }
         catch (Throwable e)
         {
            e.printStackTrace();
         }
      }
   }
   
   
   public class TestHandler implements ServerInvocationHandler
   {
      public int startedCount;
      public int finishedCount;
      public Object lock = new Object();
      
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         log.debug("invocation: " + invocation.getParameter());
         synchronized (lock)
         {
            startedCount++;
            log.debug("startedCount: " + startedCount);
         }
         
         String command = (String) invocation.getParameter();
         
         if (command.startsWith(SLOW))
         {
            Map metadata = invocation.getRequestPayload();
            String delayString = (String) metadata.get(DELAY);
            int delay = Integer.valueOf(delayString).intValue();
            Thread.sleep(delay);
         }
         
         synchronized (lock)
         {
            finishedCount++;
            log.debug("invocation done: " + invocation.getParameter());
            log.debug("finishedCount: " + finishedCount);
         }
         return null;
      }

      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
   }
}