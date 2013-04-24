package org.jboss.test.remoting.callback.multiple;

import junit.framework.TestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.AbstractInvoker;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * These tests supplement the tests in org.jboss.test.remoting.callback.multiple.client
 * and org.jboss.test.remoting.callback.multiple.server.
 *  
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1663 $
 * <p>
 * Copyright Dec 6, 2006
 * </p>
 */
public class CallbackTestClient extends TestCase
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5530;
   private static int nextPort = port + 10;

   private String locatorURI = transport + "://" + host + ":" + port;

   private static final Logger log = Logger.getLogger(CallbackTestClient.class);

   public static final String RESPONSE_VALUE = "This is the return to SampleInvocationHandler invocation";

   
   public void testPush1Client1Connector2Handlers() throws Throwable
   {
      log.info("entering " + getName());
      CallbackTestClient.CallbackHandler callbackHandler1 = new CallbackTestClient.CallbackHandler();
      CallbackTestClient.CallbackHandler callbackHandler2 = new CallbackTestClient.CallbackHandler();
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Client client = new Client(locator);
      client.connect();
      Connector connector = null;

      try
      {
         String callbackLocatorURI = transport + "://" + host + ":" + nextPort++;
         InvokerLocator callbackLocator = new InvokerLocator(callbackLocatorURI);
         connector = setupServer(callbackLocator);
         client.addListener(callbackHandler1, callbackLocator);
         client.addListener(callbackHandler2, callbackLocator);
         client.invoke("Do something", null);
         client.invoke("Do something else", null);
         
         // need to wait for brief moment so server can callback
         Thread.sleep(5000);

         int count = callbackHandler1.getCallbackCount();
         assertEquals(2, count);
         count = callbackHandler2.getCallbackCount();
         assertEquals(2, count);
      }
      finally
      {
         if(client != null)
         {
            // remove callback handler from server
            client.removeListener(callbackHandler1);
            client.removeListener(callbackHandler2);
            client.disconnect();
         }
         teardownServer(connector);
      }
   }
   
   public void testPush1Client2Connectors1Handler() throws Throwable
   {
      log.info("entering " + getName());
      CallbackTestClient.CallbackHandler callbackHandler = new CallbackTestClient.CallbackHandler();
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Client client = new Client(locator);
      client.connect();
      Connector connector1 = null;
      Connector connector2 = null;

      try
      {
         String callbackLocatorURI1 = transport + "://" + host + ":" + nextPort++;
         InvokerLocator callbackLocator1 = new InvokerLocator(callbackLocatorURI1);
         connector1 = setupServer(callbackLocator1);
         client.addListener(callbackHandler, callbackLocator1);
         String callbackLocatorURI2 = transport + "://" + host + ":" + nextPort++;
         InvokerLocator callbackLocator2 = new InvokerLocator(callbackLocatorURI2);
         connector2 = setupServer(callbackLocator2);
         client.addListener(callbackHandler, callbackLocator2);
         client.invoke("Do something", null);
         client.invoke("Do something else", null);
         
         // need to wait for brief moment so server can callback
         Thread.sleep(5000);

         int count = callbackHandler.getCallbackCount();
         assertEquals(4, count);
      }
      finally
      {
         if(client != null)
         {
            // remove callback handler from server
            client.removeListener(callbackHandler);
            client.disconnect();
         }
         teardownServer(connector1);
         teardownServer(connector2);
      }
   }

   public void testPush1Client2Connectors2Handlers() throws Throwable
   {
      log.info("entering " + getName());
      CallbackTestClient.CallbackHandler callbackHandler1 = new CallbackTestClient.CallbackHandler();
      CallbackTestClient.CallbackHandler callbackHandler2 = new CallbackTestClient.CallbackHandler();
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Client client = new Client(locator);
      client.connect();
      Connector connector1 = null;
      Connector connector2 = null;

      try
      {
         String callbackLocatorURI1 = transport + "://" + host + ":" + nextPort++;
         InvokerLocator callbackLocator1 = new InvokerLocator(callbackLocatorURI1);
         connector1 = setupServer(callbackLocator1);
         client.addListener(callbackHandler1, callbackLocator1);
         String callbackLocatorURI2 = transport + "://" + host + ":" + nextPort++;
         InvokerLocator callbackLocator2 = new InvokerLocator(callbackLocatorURI2);
         connector2 = setupServer(callbackLocator2);
         client.addListener(callbackHandler2, callbackLocator2);
         client.invoke("Do something", null);
         client.invoke("Do something else", null);
         
         // need to wait for brief moment so server can callback
         Thread.sleep(5000);

         int count = callbackHandler1.getCallbackCount();
         assertEquals(2, count);
         count = callbackHandler2.getCallbackCount();
         assertEquals(2, count);
      }
      finally
      {
         if(client != null)
         {
            // remove callback handler from server
            client.removeListener(callbackHandler1);
            client.removeListener(callbackHandler2);
            client.disconnect();
         }
         teardownServer(connector1);
         teardownServer(connector2);
      }
   }
   
   public void testPush2Clients1Connector1Handler() throws Throwable
   {
      log.info("entering " + getName());
      CallbackTestClient.CallbackHandler callbackHandler = new CallbackTestClient.CallbackHandler();
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Client client1 = new Client(locator);
      client1.connect();
      Client client2 = new Client(locator);
      client2.connect();
      Connector connector = null;

      try
      {
         String callbackLocatorURI = transport + "://" + host + ":" + nextPort++;
         InvokerLocator callbackLocator = new InvokerLocator(callbackLocatorURI);
         connector = setupServer(callbackLocator);
         log.info("created callback server: " + connector.getLocator());
         client1.addListener(callbackHandler, callbackLocator);
         client2.addListener(callbackHandler, callbackLocator);
         client1.invoke("Do something", null);
         client2.invoke("Do something else", null);
         
         // need to wait for brief moment so server can callback
         Thread.sleep(5000);

         // The CallbackHandler should be registered just once.
         int count = callbackHandler.getCallbackCount();
         assertEquals(2, count);
      }
      finally
      {
         if(client1 != null)
         {
            // remove callback handler from server
            client1.removeListener(callbackHandler);
            client1.disconnect();
         }
         teardownServer(connector);
      }
   }
   
   public void testPush2Clients1Connector2Handlers() throws Throwable
   {
      log.info("entering " + getName());
      CallbackTestClient.CallbackHandler callbackHandler1 = new CallbackTestClient.CallbackHandler();
      CallbackTestClient.CallbackHandler callbackHandler2 = new CallbackTestClient.CallbackHandler();
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Client client1 = new Client(locator);
      client1.connect();
      Client client2 = new Client(locator);
      client2.connect();
      Connector connector = null;

      try
      {
         String callbackLocatorURI = transport + "://" + host + ":" + nextPort++;
         InvokerLocator callbackLocator = new InvokerLocator(callbackLocatorURI);
         connector = setupServer(callbackLocator);
         client1.addListener(callbackHandler1, callbackLocator);
         client2.addListener(callbackHandler2, callbackLocator);
         client1.invoke("Do something", null);
         client2.invoke("Do something else", null);
         
         // need to wait for brief moment so server can callback
         Thread.sleep(5000);

         int count = callbackHandler1.getCallbackCount();
         assertEquals(2, count);
         count = callbackHandler1.getCallbackCount();
         assertEquals(2, count);
      }
      finally
      {
         Field field = AbstractInvoker.class.getDeclaredField("localServerLocators");
         field.setAccessible(true);
         
         if(client1 != null)
         {
            ClientInvoker invoker = client1.getInvoker();
            Map localServerLocators = (Map) field.get(invoker);
            assertEquals(2, localServerLocators.size());
            client1.removeListener(callbackHandler1);
            assertEquals(1, localServerLocators.size());
            client1.removeListener(callbackHandler2);
            // client1 shouldn't be able to remove callbackHandler2.
            assertEquals(1, localServerLocators.size());
            client1.disconnect();
         }
         if (client2 != null)
         {
            ClientInvoker invoker = client2.getInvoker();
            Map localServerLocators = (Map) field.get(invoker);
            assertEquals(1, localServerLocators.size());
            client2.removeListener(callbackHandler2);
            assertEquals(0, localServerLocators.size());
            client2.disconnect();
         }
         teardownServer(connector);
      }
   }
   
   public void testPush2Clients2Connectors1Handler() throws Throwable
   {
      log.info("entering " + getName());
      CallbackTestClient.CallbackHandler callbackHandler = new CallbackTestClient.CallbackHandler();
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Client client1 = new Client(locator);
      client1.connect();
      Client client2 = new Client(locator);
      client2.connect();
      Connector connector1 = null;
      Connector connector2 = null;

      try
      {
         String callbackLocatorURI1 = transport + "://" + host + ":" + nextPort++;
         InvokerLocator callbackLocator1 = new InvokerLocator(callbackLocatorURI1);
         connector1 = setupServer(callbackLocator1);
         String callbackLocatorURI2 = transport + "://" + host + ":" + nextPort++;
         InvokerLocator callbackLocator2 = new InvokerLocator(callbackLocatorURI2);
         connector2 = setupServer(callbackLocator2);
         client1.addListener(callbackHandler, callbackLocator1);
         client2.addListener(callbackHandler, callbackLocator2);
         client1.invoke("Do something", null);
         client2.invoke("Do something else", null);
         
         // need to wait for brief moment so server can callback
         Thread.sleep(5000);

         int count = callbackHandler.getCallbackCount();
         assertEquals(4, count);
      }
      finally
      {
         Field field = AbstractInvoker.class.getDeclaredField("localServerLocators");
         field.setAccessible(true);
         
         if(client1 != null)
         {
            ClientInvoker invoker = client1.getInvoker();
            Map localServerLocators = (Map) field.get(invoker);
            assertEquals(2, localServerLocators.size());
            client1.removeListener(callbackHandler);
            // callbackHandler should still be registered with connector2.
            assertEquals(1, localServerLocators.size());
            client1.disconnect();
         }
         if (client2 != null)
         {
            ClientInvoker invoker = client2.getInvoker();
            Map localServerLocators = (Map) field.get(invoker);
            assertEquals(1, localServerLocators.size());
            client2.removeListener(callbackHandler);
            assertEquals(0, localServerLocators.size());
            client2.disconnect();
         }
         teardownServer(connector1);
         teardownServer(connector2);
      }
   }
   
   public void testPush2Clients2Connectors2Handlers() throws Throwable
   {
      log.info("entering " + getName());
      CallbackTestClient.CallbackHandler callbackHandler1 = new CallbackTestClient.CallbackHandler();
      CallbackTestClient.CallbackHandler callbackHandler2 = new CallbackTestClient.CallbackHandler();
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Client client1 = new Client(locator);
      client1.connect();
      Client client2 = new Client(locator);
      client2.connect();
      Connector connector1 = null;
      Connector connector2 = null;

      try
      {
         String callbackLocatorURI1 = transport + "://" + host + ":" + nextPort++;
         InvokerLocator callbackLocator1 = new InvokerLocator(callbackLocatorURI1);
         connector1 = setupServer(callbackLocator1);
         String callbackLocatorURI2 = transport + "://" + host + ":" + nextPort++;
         InvokerLocator callbackLocator2 = new InvokerLocator(callbackLocatorURI2);
         connector2 = setupServer(callbackLocator2);
         client1.addListener(callbackHandler1, callbackLocator1);
         client2.addListener(callbackHandler2, callbackLocator2);
         client1.invoke("Do something", null);
         client2.invoke("Do something else", null);
         
         // need to wait for brief moment so server can callback
         Thread.sleep(5000);

         int count = callbackHandler1.getCallbackCount();
         assertEquals(2, count);
         count = callbackHandler1.getCallbackCount();
         assertEquals(2, count);
      }
      finally
      {
         Field field = AbstractInvoker.class.getDeclaredField("localServerLocators");
         field.setAccessible(true);
         
         if(client1 != null)
         {
            ClientInvoker invoker = client1.getInvoker();
            Map localServerLocators = (Map) field.get(invoker);
            assertEquals(2, localServerLocators.size());
            client1.removeListener(callbackHandler1);
            assertEquals(1, localServerLocators.size());
            client1.removeListener(callbackHandler2);
            // client1 shouldn't be able to remove callbackHandler2.
            assertEquals(1, localServerLocators.size());
            client1.disconnect();
         }
         if (client2 != null)
         {
            ClientInvoker invoker = client2.getInvoker();
            Map localServerLocators = (Map) field.get(invoker);
            assertEquals(1, localServerLocators.size());
            client2.removeListener(callbackHandler2);
            assertEquals(0, localServerLocators.size());
            client2.disconnect();
         }
         teardownServer(connector1);
         teardownServer(connector2);
      }
   }
   
   
   public Connector setupServer(InvokerLocator locator) throws Exception
   {
      log.info("Starting remoting server with locator uri of: " + locator);
      try
      {
         Connector connector = new Connector();
         connector.setInvokerLocator(locator.getLocatorURI());
         connector.start();
         log.info("started callback Connector: " + connector.getLocator());
         
         CallbackTestClient.SampleInvocationHandler invocationHandler = new CallbackTestClient.SampleInvocationHandler();
         // first parameter is sub-system name.  can be any String value.
         connector.addInvocationHandler("sample", invocationHandler);
         return connector;
      }
      catch(Exception e)
      {
         log.error("Error starting callback server", e);
         throw e;
      }
   }
   
   public void teardownServer(Connector connector)
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   /**
    * Simple invocation handler implementation.
    */
   public static class SampleInvocationHandler implements ServerInvocationHandler
   {

      private List listeners = new ArrayList();


      /**
       * called to handle a specific invocation
       *
       * @param invocation
       * @return
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         // Just going to return static string as this is just simple example code.

         // Will also fire callback to listeners if they were to exist using
         // simple invocation request.
         Callback callback = new Callback("This is the payload of callback invocation.");
         Iterator itr = listeners.iterator();
         while(itr.hasNext())
         {
            InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler) itr.next();
            try
            {
               callbackHandler.handleCallback(callback);
            }
            catch(HandleCallbackException e)
            {
               e.printStackTrace();
            }
         }

         return RESPONSE_VALUE;

      }

      /**
       * Adds a callback handler that will listen for callbacks from
       * the server invoker handler.
       *
       * @param callbackHandler
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         listeners.add(callbackHandler);
      }

      /**
       * Removes the callback handler that was listening for callbacks
       * from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         listeners.remove(callbackHandler);
      }

      /**
       * set the mbean server that the handler can reference
       *
       * @param server
       */
      public void setMBeanServer(MBeanServer server)
      {
         // NO OP as do not need reference to MBeanServer for this handler
      }

      /**
       * set the invoker that owns this handler
       *
       * @param invoker
       */
      public void setInvoker(ServerInvoker invoker)
      {
         // NO OP as do not need reference back to the server invoker
      }

   }


   public class CallbackHandler implements InvokerCallbackHandler
   {
      private Callback callback;
      private int callbackNumber = 0;

      /**
       * Will take the callback message and send back to client.
       * If client locator is null, will store them till client polls to get them.
       *
       * @param callback
       * @throws org.jboss.remoting.callback.HandleCallbackException
       *
       */
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         callbackNumber++;
         this.callback = callback;
         System.out.println("Received callback value of: " + callback.getCallbackObject());
         System.out.println("Received callback handle object of: " + callback.getCallbackHandleObject());
         System.out.println("Received callback server invoker of: " + callback.getServerLocator());
      }

      public Callback getCallback()
      {
         return callback;
      }

      public int getCallbackCount()
      {
         return callbackNumber;
      }
   }

}
