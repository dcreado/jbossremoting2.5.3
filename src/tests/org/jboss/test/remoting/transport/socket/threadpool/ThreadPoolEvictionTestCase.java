package org.jboss.test.remoting.transport.socket.threadpool;


import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
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
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.SocketServerInvoker;


/**
 * Unit test for JBREM-890.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Dec 21, 2007
 * </p>
 */
public class ThreadPoolEvictionTestCase extends TestCase
{
   protected static Logger log = Logger.getLogger(ThreadPoolEvictionTestCase.class);
   protected static String DELAY = "delay";
   private static boolean firstTime = true;
   
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
      }
   }

   
   public void tearDown()
   {
   }
   
   
   /**
    * Verifies that a ServerThread can be evicted while it's in versionRead().
    */
   public void testEvictionDuringVersionRead() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server - no connection checking.
      setupServer(false);
      
      // Create first client.
      InvokerLocator clientLocator1 = new InvokerLocator(locatorURI);
      HashMap clientConfig1 = new HashMap();
      clientConfig1.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig1);
      Client client1 = new Client(clientLocator1, clientConfig1);
      client1.connect();
      
      // Use up server's threadpool.
      ConnectThread ct1 = new ConnectThread(client1, 500, "ct1");
      ConnectThread ct2 = new ConnectThread(client1, 500, "ct2");
      ct1.start();
      ct2.start();
      Thread.sleep(2000);
      assertTrue(ct1.ok);
      assertTrue(ct2.ok);
      
      // Get clientpool from server.
      assertTrue(connector.getServerInvoker() instanceof SocketServerInvoker);
      SocketServerInvoker serverInvoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(serverInvoker);
      assertEquals(2, clientpool.size());
      
      // Now make another invocation that with a Client with a
      // separate connection pool.  It will create a new connection,
      // which will require an eviction from the clientpool.
      String newLocatorURI = locatorURI + "/?timeout=10000";
      InvokerLocator clientLocator2 = new InvokerLocator(newLocatorURI);
      HashMap clientConfig2 = new HashMap();
      clientConfig2.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig2);
      Client client2 = new Client(clientLocator2, clientConfig2);
      client2.connect();
      ConnectThread ct3 = new ConnectThread(client2, 0, "ct3");
      ct3.start();
      
      // Verify that eviction succeeded.
      Thread.sleep(2000);
      assertTrue(ct3.ok);
      
      // Verify that clientpool is the same, i.e., that a ServerThread
      // was reused.
      LRUPool newClientpool = (LRUPool) field.get(serverInvoker);
      assertEquals(clientpool, newClientpool);

      client1.disconnect();
      client2.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Verifies that a ServerThread can be evicted when it's in acknowledge().
    */
   public void testEvictionDuringAcknowledge() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server - do connection checking.
      setupServer(true);
      
      // Create first client.
      InvokerLocator clientLocator1 = new InvokerLocator(locatorURI);
      HashMap clientConfig1 = new HashMap();
      clientConfig1.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig1.put(SocketServerInvoker.CHECK_CONNECTION_KEY, "true");
      addExtraClientConfig(clientConfig1);
      Client client1 = new Client(clientLocator1, clientConfig1);
      client1.connect();
      
      // Use up server's threadpool.
      ConnectThread ct1 = new ConnectThread(client1, 500, "ct1");
      ConnectThread ct2 = new ConnectThread(client1, 500, "ct2");
      ct1.start();
      ct2.start();
      Thread.sleep(4000);
      assertTrue(ct1.ok);
      assertTrue(ct2.ok);
      
      // Get clientpool from server.
      assertTrue(connector.getServerInvoker() instanceof SocketServerInvoker);
      SocketServerInvoker serverInvoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(serverInvoker);
      assertEquals(2, clientpool.size());
      
      // Now make another invocation that with a Client with a
      // separate connection pool.  It will create a new connection,
      // which will require an eviction from the clientpool.
      String newLocatorURI = locatorURI + "/?timeout=10000";
      InvokerLocator clientLocator2 = new InvokerLocator(newLocatorURI);
      HashMap clientConfig2 = new HashMap();
      clientConfig2.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig2.put(SocketServerInvoker.CHECK_CONNECTION_KEY, "true");
      addExtraClientConfig(clientConfig2);
      Client client2 = new Client(clientLocator2, clientConfig2);
      client2.connect();
      ConnectThread ct3 = new ConnectThread(client2, 0, "ct3");
      ct3.start();
      
      // Verify that eviction succeeded.
      Thread.sleep(2000);
      assertTrue(ct3.ok);
      
      // Verify that clientpool is the same, i.e., that a ServerThread
      // was reused.
      LRUPool newClientpool = (LRUPool) field.get(serverInvoker);
      assertEquals(clientpool, newClientpool);

      client1.disconnect();
      client2.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Verifies that a ServerThread can be evicted when it's in acknowledge().
    */
   public void testEvictionOfSecondThread() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server - do connection checking.
      setupServer(true);
      
      // Create first client.
      InvokerLocator clientLocator1 = new InvokerLocator(locatorURI);
      HashMap clientConfig1 = new HashMap();
      clientConfig1.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig1.put(SocketServerInvoker.CHECK_CONNECTION_KEY, "true");
      addExtraClientConfig(clientConfig1);
      Client client1 = new Client(clientLocator1, clientConfig1);
      client1.connect();
      
      // Use up server's threadpool.
      ConnectThread ct1 = new ConnectThread(client1, 5000, "ct1");
      ConnectThread ct2 = new ConnectThread(client1, 0, "ct2");
      ct1.start();
      ct2.start();
      Thread.sleep(2000);
      assertFalse(ct1.ok);
      assertTrue(ct2.ok);
      
      // Get clientpool from server.
      assertTrue(connector.getServerInvoker() instanceof SocketServerInvoker);
      SocketServerInvoker serverInvoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(serverInvoker);
      assertEquals(2, clientpool.size());
      
      // Now make another invocation that with a Client with a
      // separate connection pool.  It will create a new connection,
      // which will require an eviction from the clientpool.
      String newLocatorURI = locatorURI + "/?timeout=10000";
      InvokerLocator clientLocator2 = new InvokerLocator(newLocatorURI);
      HashMap clientConfig2 = new HashMap();
      clientConfig2.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig2.put(SocketServerInvoker.CHECK_CONNECTION_KEY, "true");
      addExtraClientConfig(clientConfig2);
      Client client2 = new Client(clientLocator2, clientConfig2);
      client2.connect();
      ConnectThread ct3 = new ConnectThread(client2, 0, "ct3");
      ct3.start();
      
      // Verify that eviction succeeded.
      Thread.sleep(2000);
      assertTrue(ct3.ok);
      
      // Verify that clientpool is the same, i.e., that a ServerThread
      // was reused.
      LRUPool newClientpool = (LRUPool) field.get(serverInvoker);
      assertEquals(clientpool, newClientpool);

      client1.disconnect();
      client2.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Verifies that a ServerThread can be evicted when it's in acknowledge().
    */
   public void testDirectEviction() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server - no connection checking, maxPoolSize == 3.
      setupServer(false, "4");
      
      // Create first client.
      InvokerLocator clientLocator1 = new InvokerLocator(locatorURI);
      HashMap clientConfig1 = new HashMap();
      clientConfig1.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig1.put(SocketServerInvoker.CHECK_CONNECTION_KEY, "true");
      addExtraClientConfig(clientConfig1);
      Client client = new Client(clientLocator1, clientConfig1);
      client.connect();
      
      // Get clientpool from server.
      assertTrue(connector.getServerInvoker() instanceof SocketServerInvoker);
      SocketServerInvoker serverInvoker = (SocketServerInvoker) connector.getServerInvoker();
      Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
      field.setAccessible(true);
      LRUPool clientpool = (LRUPool) field.get(serverInvoker);
      
      // Use up server's threadpool.
      ConnectThread ct1 = new ConnectThread(client, 16000, "ct1");
      ConnectThread ct2 = new ConnectThread(client, 16000, "ct2");
      ConnectThread ct3 = new ConnectThread(client, 16000, "ct3");
      ConnectThread ct4 = new ConnectThread(client, 0, "ct4");
 
      ct1.start();
      Thread.sleep(2000); // +2000
      List threads = clientpool.getContentsByAscendingAge();
      assertEquals(1, threads.size());
      Thread thread0 = (Thread) threads.get(0);
      
      ct2.start();
      Thread.sleep(2000); // +4000
      threads = clientpool.getContentsByAscendingAge();
      assertEquals(2, threads.size());
      assertEquals(thread0, threads.get(1));
      Thread thread1 = (Thread) threads.get(0);
      
      ct3.start();
      Thread.sleep(2000); // +6000
      ct4.start();
      Thread.sleep(2000); // +8000
      threads = clientpool.getContentsByAscendingAge();
      assertEquals(4, threads.size());
      assertEquals(thread0, threads.get(3));
      assertEquals(thread1, threads.get(2));
      Thread thread2 = (Thread) threads.get(1);
      
      // Verify newest thread gets evicted.
      Thread.sleep(4000); // +12000
      clientpool.evict();
      Thread.sleep(2000); // +14000
      threads = clientpool.getContentsByAscendingAge();
      assertEquals(3, threads.size());
      assertEquals(thread0, threads.get(2));
      assertEquals(thread1, threads.get(1));
      assertEquals(thread2, threads.get(0));
      
      // Verify none of the other threads is ready to be evicted.
      clientpool.evict();
      threads = clientpool.getContentsByAscendingAge();
      assertEquals(3, threads.size());
      assertEquals(thread0, threads.get(2));
      assertEquals(thread1, threads.get(1));
      assertEquals(thread2, threads.get(0));
      
      // Wait until all of the threads are done and verify that
      // the oldest one gets evicted first.
      Thread.sleep(12000); // +26000
      threads = clientpool.getContentsByAscendingAge();
      assertEquals(2, threads.size());
      assertEquals(thread1, threads.get(1));
      assertEquals(thread2, threads.get(0));
      
      clientpool.evict();
      Thread.sleep(2000); // +29000
      threads = clientpool.getContentsByAscendingAge();
      assertEquals(1, threads.size());
      assertEquals(thread2, threads.get(0));
      
      clientpool.evict();
      Thread.sleep(2000); // +30000
      threads = clientpool.getContentsByAscendingAge();
      assertEquals(0, threads.size());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Verifies that a ServerThread cannot be evicted in the middle of
    * an invocation.  Also verifies that if all threads are uninteruptible,
    * then a thread finishes an invocation will check with LRUPool and
    * return itself to threadpool.
    */
   public void testNoEvictionDuringInvocation() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server - no connection checking.
      setupServer(false);
      
      // Create first client.
      InvokerLocator clientLocator1 = new InvokerLocator(locatorURI);
      HashMap clientConfig1 = new HashMap();
      clientConfig1.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig1.put(SocketServerInvoker.CHECK_CONNECTION_KEY, "true");
      addExtraClientConfig(clientConfig1);
      Client client1 = new Client(clientLocator1, clientConfig1);
      client1.connect();
      
      // Use up server's threadpool.
      ConnectThread ct1 = new ConnectThread(client1, 10000, "ct1");
      ConnectThread ct2 = new ConnectThread(client1, 10000, "ct2");
      ct1.start();
      ct2.start();
      Thread.sleep(5000); // +5000
      
      // Now make another invocation that with a Client with a
      // separate connection pool.  It will create a new connection,
      // which will require an eviction from the clientpool.
      String newLocatorURI = locatorURI + "/?timeout=100000";
      InvokerLocator clientLocator2 = new InvokerLocator(newLocatorURI);
      HashMap clientConfig2 = new HashMap();
      clientConfig2.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig2.put(SocketServerInvoker.CHECK_CONNECTION_KEY, "true");
      addExtraClientConfig(clientConfig2);
      Client client2 = new Client(clientLocator2, clientConfig2);
      client2.connect();
      ConnectThread ct3 = new ConnectThread(client2, 10000, "ct3");
      ct3.start();
      
      assertFalse(ct1.ok); // +5000
      assertFalse(ct2.ok);
      assertFalse(ct3.ok);      
      
      Thread.sleep(10000); // +15000
      assertTrue(ct1.ok);
      assertTrue(ct2.ok);
      assertFalse(ct3.ok);
      
      Thread.sleep(10000); // + 25000
      assertTrue(ct3.ok);

      client1.disconnect();
      client2.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }

   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}

   
   protected void setupServer(boolean checkConnection) throws Exception
   {
      setupServer(checkConnection, "2");
   }

   
   protected void setupServer(boolean checkConnection, String maxPoolSize) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put("maxPoolSize", maxPoolSize);
      
      if (checkConnection)
      {
         config.put(SocketServerInvoker.CHECK_CONNECTION_KEY, "true");   
      }
      
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
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         log.info("invoke() entered");
         Map metadata = invocation.getRequestPayload();
         int delay = ((Integer) metadata.get(DELAY)).intValue();
         Thread.sleep(delay);
         log.info("invoke() returning");
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }

   
   static class ConnectThread extends Thread
   {
      public boolean started;
      public boolean ok;
      private Client client;
      private int delay;
      private String name;
      
      public ConnectThread(Client client, int delay, String name)
      {
         this.client = client;
         this.delay = delay;
         this.name = name;
      }
      public void run()
      {
         Map metadata = new HashMap();
         metadata.put(DELAY, new Integer(delay));
         try
         {
            log.info(name + " making invocation");
            assertEquals("abc", client.invoke("abc", metadata));
            log.info(name + " got response");
            ok = true;
         }
         catch (Throwable e)
         {
            log.error("Error", e);
         }
      }
   }
}