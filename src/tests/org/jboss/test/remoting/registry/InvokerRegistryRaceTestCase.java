package org.jboss.test.remoting.registry;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.bisocket.Bisocket;

import EDU.oswego.cs.dl.util.concurrent.Rendezvous;


/**
 * Unit test for JBREM-1056.
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 * <p>
 * Copyright Nov 8, 2008
 * </p>
 */
public class InvokerRegistryRaceTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(InvokerRegistryRaceTestCase.class);
   
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
         Logger.getLogger("org.jboss.remoting").setLevel(Level.INFO);
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
   
   
   public void testRaceCondition() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      int THREADS = 20;
      int LOOPS = 1000;
      Rendezvous barrier = new Rendezvous(THREADS * 2 + 1);
      CreateCallbackThread[] createCallbackThreads = new CreateCallbackThread[THREADS];
      InvokerLocatorUpdateThread[] invokerLocatorUpdateThreads = new InvokerLocatorUpdateThread[THREADS];
      for (int i = 0; i < THREADS; i++)
      {
         createCallbackThreads[i] = new CreateCallbackThread(i, client, barrier, LOOPS);
         invokerLocatorUpdateThreads[i] = new InvokerLocatorUpdateThread(i, barrier, LOOPS * 100);
         createCallbackThreads[i].start();
         invokerLocatorUpdateThreads[i].start();
      }
      log.info("main thread going to rendezvous");
      barrier.rendezvous(null);
      barrier.rendezvous(null);
      log.info("main thread leaving second rendezvous");
      
      client.disconnect();
      shutdownServer();
      
      for (int i = 0; i < THREADS; i++)
      {
         assertTrue(createCallbackThreads[i].ok);
         assertTrue(invokerLocatorUpdateThreads[i].ok);
      }
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "bisocket";
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
         locatorURI += "/?" + metadata;
      }
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
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
   
   static class CreateCallbackThread extends Thread
   {
      int id;
      Client client;
      Rendezvous barrier;
      int counter;
      public boolean ok = true;
      
      public CreateCallbackThread(int id, Client client, Rendezvous barrier, int counter)
      {
         this.id = id;
         this.client = client;
         this.barrier = barrier;
         this.counter = counter;
         setName("CreateCallbackThread:" + id);
      }
      
      public void run()
      {
         HashMap metadata = new HashMap();
         metadata.put(Client.CALLBACK_SERVER_PORT, "8888888" + id);
         metadata.put(Bisocket.IS_CALLBACK_SERVER, "true");
         TestCallbackHandler callbackHandler = new TestCallbackHandler();
         rendezvous();
         log.info(this + " passed barrier");
         for (int i = 0; i < counter; i++)
         {
            try
            {
               if ((i + 1) % (counter / 5) == 0)
               {
                  log.info(this + " adding listener: " + (i + 1));
               }
               client.addListener(callbackHandler, metadata, null, true);
               if ((i + 1) % (counter / 5) == 0)
               {
                  log.info(this + " added listener: " + (i + 1));
               }
               client.removeListener(callbackHandler);
               if ((i + 1) % (counter / 5) == 0)
               {
                  log.info(this + " removed listener: " + (i + 1));
               }
            }
            catch (Throwable t)
            {
               log.error("unable to register callback handler", t);
               ok = false;
            }
         }
         log.info(this + " entering final rendezvous");
         rendezvous();
      }
      
      private void rendezvous()
      {
         try
         {
            barrier.rendezvous(null);
         }
         catch (Exception e)
         {
            log.error("error in rendezvous", e);
         }
      }
   }
   
   static class InvokerLocatorUpdateThread extends Thread
   {
      int id;
      InvokerLocator locator;
      Rendezvous barrier;
      int counter;
      boolean ok = true;
      
      public InvokerLocatorUpdateThread(int id, Rendezvous barrier, int counter) throws MalformedURLException
      {
         this.id = id;
         this.barrier = barrier;
         this.counter = counter;
         setName("InvokerLocatorUpdateThread:" + id);
         locator = new InvokerLocator("socket://localhost:8888");
      }
      
      public void run()
      {
         rendezvous();
         log.info(this + " passed barrier");
         for (int i = 0; i < counter; i++)
         {
            try
            {
               AccessController.doPrivileged( new PrivilegedAction()
               {
                  public Object run()
                  {
                     InvokerRegistry.updateServerInvokerLocator(locator, locator);
                     return null;
                  }
               });
               if ((i + 1) % (counter / 5) == 0)
               {
                  log.info(this + " updated locator: " + (i + 1));
               }
            }
            catch (Exception e)
            {
               ok = false;
               log.error("error updated locator", e);
            }
         }
         log.info(this + " entering final rendezvous");
         rendezvous();
      }
      private void rendezvous()
      {
         try
         {
            barrier.rendezvous(null);
         }
         catch (Exception e)
         {
            log.error("error in rendezvous", e);
         }
      }
   }
}