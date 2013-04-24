package org.jboss.test.remoting.callback.store.blocking;

import java.net.InetAddress;
import java.util.ArrayList;
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
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


public class BlockingStoreDeadlockTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(BlockingStoreDeadlockTestCase.class);
   
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
   
   
   public void testForDeadlock() throws Throwable
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
      
      // Add callback handler.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler);
      log.info("callback handler added");
      
      // Get callback.
      log.info("client getting callback");
      HashMap metadata = new HashMap();
      metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
      List callbacks = client.getCallbacks(callbackHandler, metadata);
      assertEquals(1, callbacks.size());
      log.info("got callback");
      
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
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "/?" +  metadata;
      }
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvokerCallbackHandler.CALLBACK_MEM_CEILING, "100");
      config.put(ServerInvokerCallbackHandler.CALLBACK_STORE_KEY, "org.jboss.remoting.callback.BlockingCallbackStore");
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
      InvokerCallbackHandler handler;
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         handler = callbackHandler;
         Runtime runtime = Runtime.getRuntime();
         long max = 0;
         long total = 0;
//         long free = runtime.freeMemory();
//         float percentage = 100 * free / total;
//         if(max == total && memPercentCeiling >= percentage)
            
         ArrayList list = new ArrayList();
         do
         {
            try
            {
               list.add(new Byte[1000000]);
               max = runtime.maxMemory();
               total = runtime.totalMemory();
               log.info("max = " + max + ", total = " + total);
            }
            catch (OutOfMemoryError e) 
            {
               log.info("heap space is full");
               break;
            }
         }
         while (max != total);
         log.info("heap is full");
       log.info("thread: " + Thread.currentThread().getName());
         new Thread()
         {
            public void run()
            {
               try
               {
                  log.info("thread: " + Thread.currentThread().getName());
                  log.info("adding callback");
                  handler.handleCallback(new Callback("callback"));
                  log.info("added callback");
               }
               catch (HandleCallbackException e)
               {
                  log.error("Error", e);
               }     
            }
         }.start();
         log.info("server added callback handler");
      }
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler){}
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
}