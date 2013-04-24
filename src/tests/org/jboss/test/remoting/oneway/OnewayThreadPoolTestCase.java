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
package org.jboss.test.remoting.oneway;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;

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
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.util.threadpool.BasicThreadPool;


/**
 * This test verifies that the default thread pool used by
 * org.jboss.remoting.Client and org.jboss.remoting.ServerInvoker
 * to do asynchronous method invocations can function properly under
 * heavy loads.
 * 
 * See JBREM-658.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 4221 $
 * <p>
 * Copyright Feb 6, 2007
 * </p>
 */
public class OnewayThreadPoolTestCase extends TestCase
{
   protected static String FAST = "fast";
   protected static String SLOW = "slow";
   
   protected static Logger log = Logger.getLogger(OnewayThreadPoolTestCase.class);
   protected static boolean firstTime = true;
   protected static boolean go;
   protected static Object lock = new Object();
   protected static int poolCounter;
   
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
    * This test verifies that thread and queue size are correctly set
    * on the server side and client side.
    */
   public void testConfiguration() throws Throwable
   {
         log.info("entering " + getName());
         String host = InetAddress.getLocalHost().getHostAddress();
         int port = PortUtil.findFreePort(host);
         String locatorURI = "socket://" + host + ":" + port;
         InvokerLocator locator = new InvokerLocator(locatorURI);
         HashMap serverConfig = new HashMap();
         serverConfig.put(ServerInvoker.MAX_NUM_ONEWAY_THREADS_KEY, "3");
         serverConfig.put(ServerInvoker.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "5");
         Connector connector = new Connector(locator, serverConfig);
         connector.create();
         TestHandler handler = new TestHandler();
         connector.addInvocationHandler("test", handler);
         connector.start();
         
         HashMap clientConfig = new HashMap();
         clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
         clientConfig.put(Client.MAX_NUM_ONEWAY_THREADS, "7");
         clientConfig.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "9");
         Client client = new Client(locator, clientConfig);
         client.connect();
         
         client.invokeOneway(FAST, null, true);
         client.invokeOneway(FAST, null, false);
         poolCounter += 2;
         Thread.sleep(1000);
         assertEquals(2, handler.startedCount);
         
         Field field = ServerInvoker.class.getDeclaredField("onewayThreadPool");
         field.setAccessible(true);
         BasicThreadPool pool = (BasicThreadPool) field.get(connector.getServerInvoker());
         assertEquals(3, pool.getMaximumPoolSize());
         assertEquals(5, pool.getMaximumQueueSize());
         
         field = Client.class.getDeclaredField("onewayThreadPool");
         field.setAccessible(true);
         pool = (BasicThreadPool) field.get(client);
         assertEquals(7, pool.getMaximumPoolSize());
         assertEquals(9, pool.getMaximumQueueSize());
         
         client.disconnect();
         connector.stop();
         log.info(getName() + " PASSES");
   }
   
   /**
    * This test exercises the client side thread pool using the http transport.
    * The http client invoker does not return until after the invocation has
    * returned a response.
    */
   public void testThreadPoolHttpClientSide() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "http://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Client.MAX_NUM_ONEWAY_THREADS, "2");
      config.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "1");
      Client client = new Client(locator, config);
      client.connect();
      
      Object response = client.invoke(FAST);
      assertEquals(FAST, response);
      
      long start = System.currentTimeMillis();
      
      // This invocation should run in pooled thread 1.
      log.info("making 1st oneway invocation");
      client.invokeOneway(SLOW + "1", null, true);
      poolCounter++;
      
      // This invocation should run in pooled thread 2.
      log.info("making 2nd oneway invocation");
      client.invokeOneway(SLOW + "2", null, true);
      
      // This invocation should go into the queue.
      log.info("making 3rd oneway invocation");
      client.invokeOneway(SLOW + "3", null, true);
      assertTrue((System.currentTimeMillis() - start < 1000));
      Thread.sleep(2000);
      log.info("handler.count: " + handler.startedCount);
      assertEquals(3, handler.startedCount);
      
      // This invocation should run in the current thread, and will not return
      // until after a response is received.
      start = System.currentTimeMillis();
      log.info("making 4th oneway invocation");
      client.invokeOneway(SLOW + "4", null, true);
      log.info("made 4th oneway invocation");
      log.info("wait: " + (System.currentTimeMillis() - start));
      assertTrue((System.currentTimeMillis() - start >= 5000));
      
      Thread.sleep(12000);
      assertEquals(5, handler.startedCount);
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }

   
   /**
    * This test exercises the client side thread pool using the socket transport
    * The socket client invoker waits for a response.  See JBREM-706.
    */
   public void testThreadPoolSocketClientSide() throws Throwable
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
      config.put(Client.MAX_NUM_ONEWAY_THREADS, "2");
      config.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "1");
      Client client = new Client(locator, config);
      client.connect();
      
      Object response = client.invoke(FAST);
      assertEquals(FAST, response);
      
      long start = System.currentTimeMillis();
      
      // NOTE.  The following commented code precedes the fix to JBREM-706.  The four
      // slow invocations were expected to take up to 16 seconds to start.
      
//      // The following four invocations are quite nondeterministic.  In the best
//      // case, two will execute in the thread pool and two will execute simultaneously
//      // in ServerThreads.  In the worst case, the last three invocations will 
//      // find a pooled connection and connect to a ServerThread busy with the
//      // previous invocation.
//      
//      // Will execute in first thread pool thread.
//      log.info("making 1st oneway invocation");
//      client.invokeOneway(SLOW + "1", null, true);
//      poolCounter++;
//      
//      // Will execute in first or second thread pool thread.
//      log.info("making 2nd oneway invocation");
//      client.invokeOneway(SLOW + "2", null, true);
//      
//      // Could execute in a thread pool thread or go on the queue.
//      log.info("making 3rd oneway invocation");
//      client.invokeOneway(SLOW + "3", null, true);
//      assertTrue((System.currentTimeMillis() - start < 1000));
//      
//      // Could execute in a thread pool thread, go on the queue, or execute
//      // in the main thread.
//      log.info("making 4th oneway invocation");
//      client.invokeOneway(SLOW + "4", null, true);
//      log.info("made 4th oneway invocation");
//      log.info("wait: " + (System.currentTimeMillis() - start));
//      assertTrue((System.currentTimeMillis() - start < 1000));
//      
//      // In the worst case, the four invocations could take as much as 15 seconds 
//      // to all start.
//      Thread.sleep(16000);
//      assertEquals(5, handler.startedCount);
      
      
      // The following code replaces the commented code just above.  After the fix
      // to JBREM-706, all four slow invocations should start within just over two
      // seconds.
      
      // Will execute in first thread pool thread.
      log.info("making 1st oneway invocation");
      client.invokeOneway(SLOW + "1", null, true);
      poolCounter++;
      
      // Will execute in second thread pool thread.
      log.info("making 2nd oneway invocation");
      client.invokeOneway(SLOW + "2", null, true);
      
      // Will go on the queue.  The attempt to read the response to the first 
      // invocation will fail after two seconds, and the first thread will be
      // available to execute this invocation.
      log.info("making 3rd oneway invocation");
      client.invokeOneway(SLOW + "3", null, true);
      assertTrue((System.currentTimeMillis() - start < 1000));
      Thread.sleep(500);
      
      // At this point, slow invocations 1 and 2 should have started.
      assertEquals(3, handler.startedCount);
      
      // Will execute on the main thread.  The call to client.invokeOneway() will
      // time out and return after two seconds.
      log.info("making 4th oneway invocation");
      client.invokeOneway(SLOW + "4", null, true);
      log.info("made 4th oneway invocation");
      log.info("wait: " + (System.currentTimeMillis() - start));
      assertTrue((System.currentTimeMillis() - start < 3000));
      
      // Give slow invocation 3 time to start.
      Thread.sleep(500);
      
      // At this point, all invocations should have started.
      assertEquals(5, handler.startedCount);
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * This test verifies that the client side thread pool can function under
    * a heavy load.  It uses the http transport.
    */
   public void testHeavyLoadClientSideHttp() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "http://" + host + ":" + port;
      locatorURI += "/?maxProcessors=400";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Client.MAX_NUM_ONEWAY_THREADS, "100");
      config.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "100");
      Client client = new Client(locator, config);
      client.connect();
      
      int INVOCATIONS = 400;
      OnewayThread[] threads = new OnewayThread[INVOCATIONS];
      
      for (int i = 0; i < INVOCATIONS; i++)
      {
         threads[i] = new OnewayThread(client, i, true);
         threads[i].start();
      }
      
      synchronized (lock)
      {
         go = true;
         lock.notifyAll();
      }
      
      poolCounter++;
      Thread.sleep(15000);
      
      // Verify INVOCATIONS invocations were received.
      assertEquals(INVOCATIONS, handler.startedCount);
      
      // Verify only one thread pool was created.
      Field field = Client.class.getDeclaredField("onewayThreadPool");
      field.setAccessible(true);
      Object pool = field.get(client);
      assertTrue(pool instanceof BasicThreadPool);
      BasicThreadPool basicThreadPool = (BasicThreadPool) pool;
      assertEquals(poolCounter, basicThreadPool.getPoolNumber());
      
      for (int i = 0; i < INVOCATIONS; i++)
      {
         assertTrue("failure in thread: " + i, threads[i].ok);
      }
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * This test verifies that the client side thread pool can function under
    * a heavy load.  It uses the socket transport.
    */
   public void testHeavyLoadClientSideSocket() throws Throwable
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
      config.put(Client.MAX_NUM_ONEWAY_THREADS, "100");
      config.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "100");
      Client client = new Client(locator, config);
      client.connect();
      
      int INVOCATIONS = 1000;
      OnewayThread[] threads = new OnewayThread[INVOCATIONS];
      
      for (int i = 0; i < INVOCATIONS; i++)
      {
         threads[i] = new OnewayThread(client, i, true);
         threads[i].start();
      }
      
      synchronized (lock)
      {
         go = true;
         lock.notifyAll();
      }
      
      poolCounter++;
      Thread.sleep(5000);
      
      // Verify INVOCATIONS invocations were received.
      assertEquals(INVOCATIONS, handler.startedCount);
      
      // Verify only one thread pool was created.
      Field field = Client.class.getDeclaredField("onewayThreadPool");
      field.setAccessible(true);
      Object pool = field.get(client);
      assertTrue(pool instanceof BasicThreadPool);
      BasicThreadPool basicThreadPool = (BasicThreadPool) pool;
      assertEquals(poolCounter, basicThreadPool.getPoolNumber());
      
      for (int i = 0; i < INVOCATIONS; i++)
      {
         assertTrue("failure in thread: " + i, threads[i].ok);
      }
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * This test exercises the server side thread pool using the http transport
    */ 
   public void testThreadPoolHttpServerSide() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "http://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put(ServerInvoker.MAX_NUM_ONEWAY_THREADS_KEY, "2");
      serverConfig.put(ServerInvoker.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "1");
      Connector connector = new Connector(locator, serverConfig);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      Client client = new Client(locator, clientConfig);
      client.connect();
      
      Object response = client.invoke(FAST);
      assertEquals(FAST, response);
      
      long start = System.currentTimeMillis();
      
      // This invocation should run in pooled thread 1.
      log.info("making 1st oneway invocation");
      client.invokeOneway(SLOW + "1", null, false);
      poolCounter++;
      
      // Wait for connection to return to pool.
      Thread.sleep(500);
      
      // This invocation should run in pooled thread 2.
      log.info("making 2nd oneway invocation");
      client.invokeOneway(SLOW + "2", null, false);
      
      // Wait for connection to return to pool.
      Thread.sleep(500);
      
      // This invocation should use the pooled connection and go into the queue.
      log.info("making 3rd oneway invocation");
      client.invokeOneway(SLOW + "3", null, false);
      assertTrue((System.currentTimeMillis() - start < 2000));
      Thread.sleep(2000);
      log.info("handler.count: " + handler.startedCount);
      assertEquals(3, handler.startedCount);
      
      // This invocation should run in the ServerThread, and will not return
      // until after a response is received.
      log.info("making 4th oneway invocation");
      client.invokeOneway(SLOW + "4", null, false);
      log.info("made 4th oneway invocation");
      log.info("wait: " + (System.currentTimeMillis() - start));
      assertTrue((System.currentTimeMillis() - start >= 8000));
      
      // By the time the 4th oneway invocation returns, the 3rd oneway invocation
      // should have started.
      assertEquals(5, handler.startedCount);
      assertEquals(4, handler.finishedCount);
      
      Thread.sleep(3000);
      assertEquals(5, handler.finishedCount);
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }

   
   /**
    * This test exercises the server side thread pool using the socket transport
    */ 
   public void testThreadPoolSocketServerSide() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "socket://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put(ServerInvoker.MAX_NUM_ONEWAY_THREADS_KEY, "2");
      serverConfig.put(ServerInvoker.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "1");
      Connector connector = new Connector(locator, serverConfig);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      Client client = new Client(locator, clientConfig);
      client.connect();
      
      Object response = client.invoke(FAST);
      assertEquals(FAST, response);
      
      long start = System.currentTimeMillis();
      
      // This invocation should run in pooled thread 1.
      log.info("making 1st oneway invocation");
      client.invokeOneway(SLOW + "1", null, false);
      poolCounter++;
      
      // Wait for the connection to return to the pool.
      Thread.sleep(500);
      
      // This invocation should run in pooled thread 2.
      log.info("making 2nd oneway invocation");
      client.invokeOneway(SLOW + "2", null, false);
      
      // Wait for the connection to return to the pool.
      Thread.sleep(500);
      
      // This invocation should use the pooled connection and go into the queue.
      log.info("making 3rd oneway invocation");
      client.invokeOneway(SLOW + "3", null, false);
      
      // Wait for the connection to return to the pool.
      Thread.sleep(500);
      
      // This invocation should use the pooled connection and get run by the
      // ServerThread.  The connection should go back into the pool but the
      // ServerThread will be busy for the next 5 seconds.
      log.info("making 4th oneway invocation");
      client.invokeOneway(SLOW + "4", null, false);
      
      // Wait for the connection to return to the pool.
      Thread.sleep(500);
      
      // This invocation should use the pooled connection and have to wait
      // for 5 seconds.
      log.info("making 5th oneway invocation");
      client.invokeOneway(SLOW + "5", null, false);
      
      assertTrue((System.currentTimeMillis() - start < 3000));
      assertEquals(4, handler.startedCount);
      
      // It's necessary to wait for more than 5000 ms here because one or two 
      // of the invocations might go out over preexisting pooled connections
      // and have to wait for the handler to finish the previous invocation.
      Thread.sleep(6000);
      assertEquals(6, handler.startedCount);
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * This test verifies that the server side thread pool can function under
    * a heavy load.  It uses the http transport.
    */
   public void testHeavyLoadServerSideHttp() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "http://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put(ServerInvoker.MAX_NUM_ONEWAY_THREADS_KEY, "100");
      serverConfig.put(ServerInvoker.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "100");
      Connector connector = new Connector(locator);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      Client client = new Client(locator, clientConfig);
      client.connect();
      
      // CoyoteInvoker defaults to 200 threads.
      int INVOCATIONS = 300;
      OnewayThread[] threads = new OnewayThread[INVOCATIONS];
      
      for (int i = 0; i < INVOCATIONS; i++)
      {
         threads[i] = new OnewayThread(client, i, false);
         threads[i].start();
      }
      
      synchronized (lock)
      {
         go = true;
         lock.notifyAll();
      }
      
      poolCounter++;
      Thread.sleep(5000);
      
      // Verify INVOCATIONS invocations were received.
      assertEquals(INVOCATIONS, handler.startedCount);
      
      for (int i = 0; i < INVOCATIONS; i++)
      {
         assertTrue("failure in thread: " + i, threads[i].ok);
      }
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * This test verifies that the server side thread pool can function under
    * a heavy load.  It uses the socket transport.
    */
   public void testHeavyLoadServerSideSocket() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "socket://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put(ServerInvoker.MAX_NUM_ONEWAY_THREADS_KEY, "100");
      serverConfig.put(ServerInvoker.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, "100");
      Connector connector = new Connector(locator, serverConfig);
      connector.create();
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      Client client = new Client(locator, clientConfig);
      client.connect();
      
      int INVOCATIONS = 1000;
      OnewayThread[] threads = new OnewayThread[INVOCATIONS];
      
      for (int i = 0; i < INVOCATIONS; i++)
      {
         threads[i] = new OnewayThread(client, i, false);
         threads[i].start();
      }
      
      synchronized (lock)
      {
         go = true;
         lock.notifyAll();
      }
      
      poolCounter++;
      Thread.sleep(10000);
      
      // Verify INVOCATION invocations were received.
      assertEquals(INVOCATIONS, handler.startedCount);
      
      for (int i = 0; i < INVOCATIONS; i++)
      {
         assertTrue("failure in thread: " + i, threads[i].ok);
      }
      
      // Verify only one thread pool was created.
      Field field = ServerInvoker.class.getDeclaredField("onewayThreadPool");
      field.setAccessible(true);
      Object pool = field.get(connector.getServerInvoker());
      assertTrue(pool instanceof BasicThreadPool);
      BasicThreadPool basicThreadPool = (BasicThreadPool) pool;
      assertEquals(poolCounter, basicThreadPool.getPoolNumber());
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   public class OnewayThread extends Thread
   {
      boolean ok;
      Client client;
      int id;
      boolean clientSide;
      
      public OnewayThread(Client client, int id, boolean clientSide)
      {
         this.client = client;
         this.id = id;
         this.clientSide = clientSide;
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

            client.invokeOneway(FAST + id, null, clientSide);
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
         }
         
         String command = (String) invocation.getParameter();
         
         if (command.startsWith(SLOW))
         {
            log.info("startedCount: " + startedCount);
            log.info("invocation: " + invocation.getParameter());
            Thread.sleep(5000);
            log.info("invocation done: " + invocation.getParameter());
            log.info("finishedCount: " + finishedCount);
         }
         
         synchronized (lock)
         {
            finishedCount++;
         }
         
         log.debug("invocation done: " + invocation.getParameter());
         log.debug("finishedCount: " + finishedCount);
         return invocation.getParameter();
      }

      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
   }
}