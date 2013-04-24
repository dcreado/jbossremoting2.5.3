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

package org.jboss.test.remoting.callback.exception;

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
import org.jboss.test.remoting.callback.CallbackTestServer;

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

   private Client remotingClient;
   private Connector connector;

   private static final Logger log = Logger.getLogger(CallbackTestClient.class);

   public static final String RESPONSE_VALUE = "This is the return to SampleInvocationHandler invocation";


   private void init() throws Exception
   {
      createRemotingClient();
   }

   public void createRemotingClient() throws Exception
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Calling remoting server with locator uri of: " + locatorURI);

      // This could have been new Client(locator), but want to show that subsystem param is null
      // Could have also been new Client(locator, "sample");
      remotingClient = new Client(locator);
      remotingClient.connect();

   }

   public void makeInvocation() throws Throwable
   {
      Object response = remotingClient.invoke("Do something", null);
      System.out.println("Invocation response: " + response);
   }

   public void setUp() throws Exception
   {
      init();
   }

   public void tearDown() throws Exception
   {
      if(remotingClient != null)
      {
         remotingClient.disconnect();
      }
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public void testPushCallback() throws Throwable
   {
      CallbackHandler callbackHandler = new CallbackHandler();

      try
      {
         // Using loctor with port value one higher than the target server
         String callbackLocatorURI = transport + "://" + host + ":" + (port + 5);
         InvokerLocator callbackLocator = new InvokerLocator(callbackLocatorURI);

         log.info("testPushCallback - Setting up server.");
         // call to create remoting server to
         // receive client callbacks.
         setupServer(callbackLocator);

         // Callback handle object will be passed back as part of the callback object
         String callbackHandleObject = "myCallbackHandleObject";
         log.info("testPushCallback - adding listener.");
         // by passing only the callback handler, will indicate pull callbacks
         remotingClient.addListener(callbackHandler, callbackLocator, callbackHandleObject);
         log.info("testPushCallback - make invocation");
         // now make invocation on server, which should cause a callback to happen
         makeInvocation();

         // need to wait for brief moment so server can callback
         Thread.sleep(5000);

         Callback callback = callbackHandler.getCallback();
         log.info("testPushCallback - Callback returned was " + callback);
         assertNotNull("Callback returned was null.", callback);

         Object callbackObj = callback.getCallbackObject();
         log.info("testPushCallback - Callback value should have been " + CallbackTestServer.CALLBACK_VALUE + ", and was " + callbackObj);
         assertEquals("Callback value should have been " + CallbackTestServer.CALLBACK_VALUE + ", but was " + callbackObj,
                      CallbackTestServer.CALLBACK_VALUE, callbackObj);
         Object callbackHandleObj = callback.getCallbackHandleObject();
         log.info("testPushCallback - Callback handle object should have been " + callbackHandleObject + ", and was " + callbackHandleObj);
         assertEquals("Callback handle object should have been " + callbackHandleObject + ", but was " + callbackHandleObj,
                      callbackHandleObject, callbackHandleObj);
         InvokerLocator serverLoc = callback.getServerLocator();
         log.info("testPushCallback - Callback server locator should have been " + remotingClient.getInvoker().getLocator() +
                  ", and was " + serverLoc);
         assertEquals("Callback server locator should have been " + remotingClient.getInvoker().getLocator() +
                      ", but was " + serverLoc,
                      remotingClient.getInvoker().getLocator(), serverLoc);

      }
      finally
      {
         if(remotingClient != null)
         {
            // remove callback handler from server
            remotingClient.removeListener(callbackHandler);
         }

         if(connector != null)
         {
            connector.stop();
            connector.destroy();
         }

         Thread.currentThread().sleep(50000);
      }
   }


   public void setupServer(InvokerLocator locator) throws Exception
   {
      log.info("Starting remoting server with locator uri of: " + locator);
      try
      {
         connector = new Connector();
         connector.setInvokerLocator(locator.getLocatorURI());
         connector.start();

         SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
         // first parameter is sub-system name.  can be any String value.
         connector.addInvocationHandler("sample", invocationHandler);
      }
      catch(Exception e)
      {
         log.error("Error starting callback server", e);
         throw e;
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
         System.out.println("listeners.size = " + listeners.size());
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
         this.callback = callback;
         System.out.println("Received callback value of: " + callback.getCallbackObject());
         System.out.println("Received callback handle object of: " + callback.getCallbackHandleObject());
         System.out.println("Received callback server invoker of: " + callback.getServerLocator());
      }

      public Callback getCallback()
      {
         return callback;
      }

   }

}