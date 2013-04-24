package org.jboss.test.remoting.lease.socket.multiple;

import junit.framework.TestCase;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketLeaseTestClient extends TestCase
{
   private static Logger log = Logger.getLogger(SocketLeaseTestClient.class);
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;
   
   private String locatorURI = transport + "://" + host + ":" + port + "/?" + InvokerLocator.CLIENT_LEASE + "=" + "true";
   private String callbackLocatorURI = transport + "://" + host + ":" + (port + 1);
   
   private int COUNT = 50;
   private boolean[] success = new boolean[COUNT];
   private boolean[] done = new boolean[COUNT];
   
// public void setUp()
// {
// org.apache.log4j.BasicConfigurator.configure();
// org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
// org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(XLevel.TRACE);
// org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.FATAL);
// }
   
   public void setUp()
   {
      log.info("entering setUp()");
   }
   protected String getLocatorUri()
   {
      return locatorURI;
   }
   
   public void testMultipleLeases() throws Throwable
   {
      log.info("entering " + getName());

      class CallerThread extends Thread
      {
         int id;
         
         CallerThread(int id)
         {
            this.id = id;
         }
         
         public void run()
         {
            try
            {
               log.info("calling runClient(" + id + ")");
               runClient(id);
               log.info("runClient(" + id + ") returns");
               done[id] = true;
            }
            catch (Throwable e)
            {
               e.printStackTrace();
            }
         }
      }
      
      for (int i = 0; i < COUNT; i++)
      {  
         Thread t = new CallerThread(i);
         t.setName("client: " + i);
         t.start();
         log.info("started client thread " + i);
      }
      
      Thread.sleep(35000);
      
      boolean allDone = false;
      while (!allDone)
      {
         allDone = true;
         for (int i = 0; i < COUNT; i++)
         {
            allDone &= done[i];
            log.info("done[" + i + "]: " + done[i]);
         }
         Thread.sleep(1000);
      }
      
      boolean allSuccess = true;
      for (int i = 0; i < COUNT; i++)
      {
         allSuccess &= success[i];
      }
      
      log.info("success: " + allSuccess);
      assertTrue(allSuccess);
      log.info(getName() + " PASSES");
   }
   
   
   public void runClient(int id) throws Throwable
   {
      InvokerLocator locator = new InvokerLocator(getLocatorUri());
      System.out.println("Calling remoting server with locator uri of: " + getLocatorUri());
      
      //InvokerLocator callbackLocator = new InvokerLocator(callbackLocatorURI);
      //Connector callbackConnector = new Connector(callbackLocator);
      //callbackConnector.create();
      //callbackConnector.start();
      
      //TestCallbackHandler callbackHandler = new TestCallbackHandler();
      
      Map metadata = new HashMap();
      metadata.put("clientName", "test1");
      Client remotingClient1 = new Client(locator, metadata);
      remotingClient1.connect();
      
      //remotingClient1.addListener(callbackHandler, callbackLocator);
      
      Object ret = remotingClient1.invoke("test1");
      System.out.println("Response was: " + ret);
      
      Thread.currentThread().sleep(1000);
      
      // now create second client
      Map metadata2 = new HashMap();
      metadata2.put("clientName", "test1");
      Client remotingClient2 =new Client(locator, metadata2);
      remotingClient2.connect();
      //remotingClient2.addListener(callbackHandler, callbackLocator);
      
      ret = remotingClient2.invoke("test2");
      System.out.println("Response was: " + ret);
      
      ret = remotingClient1.invoke("test1");
      System.out.println("Response was: " + ret);
      
      Thread.currentThread().sleep(1000);
      
      if(remotingClient1 != null)
      {
         //remotingClient1.removeListener(callbackHandler);
         remotingClient1.disconnect();
      }
      
      System.out.println("remoting client 1 (thread " + id + ") disconnected");
      
      //Thread.currentThread().sleep(10000);
      Thread.currentThread().sleep(30000);
      
      ret = remotingClient2.invoke("test2");
      System.out.println("Response was: " + ret);
      
      if(remotingClient2 != null)
      {
         //remotingClient2.removeListener(callbackHandler);
         remotingClient2.disconnect();
      }
      
      success[id] = true;
      log.info("thread " + id + " successful");
   }

   public class TestCallbackHandler implements InvokerCallbackHandler
   {

      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         System.out.println("callback: " + callback);
      }
   }


}
