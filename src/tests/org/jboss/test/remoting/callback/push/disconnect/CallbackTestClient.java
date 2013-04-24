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

package org.jboss.test.remoting.callback.push.disconnect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.management.MBeanServer;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class CallbackTestClient extends TestCase
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5500;
   private static String callbackHost = "localhost";
   private static int callbackPort = 5501;

   private String locatorURI = transport + "://" + host + ":" + port;

//   public static final String RESPONSE_VALUE = "This is the return to SampleInvocationHandler invocation";

   public void testPullCallbackDisconnect() throws Throwable
   {
      String callbackServerURI = "socket://" + callbackHost + ":" + callbackPort;
      Connector callbackServer = new Connector();
      callbackServer.setInvokerLocator(callbackServerURI);
      callbackServer.start();
//      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();

//      callbackServer.getServerInvoker().addInvocationHandler("callbackTest", invocationHandler);

      Client client = new Client(new InvokerLocator(locatorURI), "test");
      client.connect();
      CallbackHandler callbackHandler = new CallbackHandler();
      client.addListener(callbackHandler, new InvokerLocator(callbackServerURI));

      // send invocation that will trigger sending of callbacks.
      client.invoke("foo");

      // wait to get some callbacks.
      System.out.println("Waiting three seconds for callbacks.");
      Thread.currentThread().sleep(3000);

      System.out.println("Done waiting for callbacks, will shutdown callback server and client.");
      // shutdown client and callback server
//      callbackServer.getServerInvoker().removeInvocationHandler(
//            callbackServer.getServerInvoker().getInvocationHandlers()[0].toString()
//      );
      client.removeListener(callbackHandler);
      client.disconnect();
      callbackServer.stop();
      callbackServer.destroy();

      System.out.println("Callback total number after shutdown is " + callbackHandler.getCallbackNumber());
      int callbackNumber = callbackHandler.getCallbackNumber();

      // sleep to allow server to send more callbacks if is going to
      System.out.println("Waiting ten seconds to make sure no more callbacks are delivered by server.");
      Thread.currentThread().sleep(10000);

      // check to make sure no more callbacks have been delivered.
      System.out.println("Callback total number after waiting ten seconds is " + callbackHandler.getCallbackNumber());
      assertEquals(callbackNumber, callbackHandler.getCallbackNumber());

   }

   /**
    * Simple invocation handler implementation.
    */
//   public static class SampleInvocationHandler implements ServerInvocationHandler
//   {
//
//      private List listeners = new ArrayList();
//      private boolean sendCallbacks = false;
//      private int callbackCounter = 1;
//
//      public SampleInvocationHandler()
//      {
//         sendCallbacks();
//      }
//
//      /**
//       * called to handle a specific invocation
//       *
//       * @param invocation
//       * @return
//       * @throws Throwable
//       */
//      public Object invoke(InvocationRequest invocation) throws Throwable
//      {
//         // Just going to return static string as this is just simple example code.
//
//         sendCallbacks = true;
//         return RESPONSE_VALUE;
//
//      }
//
//      private void sendCallbacks()
//      {
//         new Thread(new Runnable()
//         {
//            public void run()
//            {
//               while(true)
//               {
//
//                  try
//                  {
//                     Thread.currentThread().sleep(1000);
//                  }
//                  catch(InterruptedException e)
//                  {
//                     e.printStackTrace();
//                  }
//
//                  while(sendCallbacks)
//                  {
//
//                     // Will also fire callback to listeners if they were to exist using
//                     // simple invocation request.
//                     Callback callback = new Callback(new Integer(callbackCounter));
//                     Iterator itr = listeners.iterator();
//                     while(itr.hasNext())
//                     {
//                        InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler) itr.next();
//                        try
//                        {
//                           callbackHandler.handleCallback(callback);
//                        }
//                        catch(HandleCallbackException e)
//                        {
//                           e.printStackTrace();
//                        }
//                     }
//                  }
//               }
//
//            }
//         }).start();
//      }
//
//      /**
//       * Adds a callback handler that will listen for callbacks from
//       * the server invoker handler.
//       *
//       * @param callbackHandler
//       */
//      public void addListener(InvokerCallbackHandler callbackHandler)
//      {
//         listeners.add(callbackHandler);
//      }
//
//      /**
//       * Removes the callback handler that was listening for callbacks
//       * from the server invoker handler.
//       *
//       * @param callbackHandler
//       */
//      public void removeListener(InvokerCallbackHandler callbackHandler)
//      {
//         listeners.remove(callbackHandler);
//      }
//
//      /**
//       * set the mbean server that the handler can reference
//       *
//       * @param server
//       */
//      public void setMBeanServer(MBeanServer server)
//      {
//         // NO OP as do not need reference to MBeanServer for this handler
//      }
//
//      /**
//       * set the invoker that owns this handler
//       *
//       * @param invoker
//       */
//      public void setInvoker(ServerInvoker invoker)
//      {
//         // NO OP as do not need reference back to the server invoker
//      }
//
//   }


   public class CallbackHandler implements InvokerCallbackHandler
   {
      private Callback callback;
      private Integer callbackNumber;

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

         callbackNumber = (Integer) callback.getCallbackObject();
      }

      public int getCallbackNumber()
      {
         if(callbackNumber != null)
         {
            return callbackNumber.intValue();
         }
         else
         {
            return 0;
         }
      }

      public Callback getCallback()
      {
         return callback;
      }

   }

}