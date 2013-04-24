package org.jboss.test.remoting.callback.multiple.server;

import junit.framework.TestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class CallbackTestClient extends TestCase
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5500;

   private String locatorURI = transport + "://" + host + ":" + port;

   private Connector connector;
   private Connector connector2;

   private static final Logger log = Logger.getLogger(CallbackTestClient.class);

   public static final String RESPONSE_VALUE = "This is the return to SampleInvocationHandler invocation";


   public void tearDown() throws Exception
   {
      if (connector != null)
      {
         connector.stop();
         connector.destroy();
      }
      if (connector2 != null)
      {
         connector2.stop();
         connector2.destroy();
      }
   }

   public void testPushCallback() throws Throwable
   {

      CallbackTestClient.CallbackHandler callbackHandler = new CallbackTestClient.CallbackHandler();

      InvokerLocator locator = new InvokerLocator(locatorURI);
      Client remotingClient = new Client(locator);
      remotingClient.connect();

      try
      {

         // Using loctor with port value one higher than the target server
         String callbackLocatorURI = transport + "://" + host + ":" + (port + 5);
         InvokerLocator callbackLocator = new InvokerLocator(callbackLocatorURI);

         String callbackLocatorURI2 = transport + "://" + host + ":" + (port + 10);
         InvokerLocator callbackLocator2 = new InvokerLocator(callbackLocatorURI2);

         log.info("testPushCallback - Setting up server.");
         // call to create remoting server to
         // receive client callbacks.
         connector = new Connector();
         connector.setInvokerLocator(callbackLocator.getLocatorURI());
         connector.start();

         CallbackTestClient.SampleInvocationHandler invocationHandler = new CallbackTestClient.SampleInvocationHandler();
         connector.addInvocationHandler("sample", invocationHandler);

         connector2 = new Connector();
         connector2.setInvokerLocator(callbackLocator2.getLocatorURI());
         connector2.start();

         connector.addInvocationHandler("sample", invocationHandler);

         // Callback handle object will be passed back as part of the callback object
         String callbackHandleObject = "myCallbackHandleObject";
         log.info("testPushCallback - adding listener.");
         // by passing only the callback handler, will indicate pull callbacks
         remotingClient.addListener(callbackHandler, callbackLocator, callbackHandleObject);
         remotingClient.addListener(callbackHandler, callbackLocator2, callbackHandleObject);
         log.info("testPushCallback - make invocation");
         // now make invocation on server, which should cause a callback to happen
         Object response = remotingClient.invoke("Do something", null);

         // need to wait for brief moment so server can callback
         Thread.sleep(5000);

         int count = callbackHandler.getCallbackCount();
         System.out.println("callback count = " + count);
         assertEquals(2, count);
      }
      finally
      {
         if (remotingClient != null)
         {
            // remove callback handler from server
            remotingClient.removeListener(callbackHandler);
            remotingClient.disconnect();
         }
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
         while (itr.hasNext())
         {
            InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler) itr.next();
            try
            {
               callbackHandler.handleCallback(callback);
            }
            catch (HandleCallbackException e)
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
