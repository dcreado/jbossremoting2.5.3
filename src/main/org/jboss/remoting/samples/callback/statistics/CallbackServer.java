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

package org.jboss.remoting.samples.callback.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.management.MBeanServer;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.CallbackListener;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

/**
 * Simple remoting server.  Uses inner class SampleInvocationHandler
 * as the invocation target handler class, which will generate
 * callback messages upon callback listeners being added.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class CallbackServer
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;

   // counter indicating the number of callbacks generated
   private static int callbackCounter = 1;

   // remoting server connector
   private Connector connector = null;

   // String to be returned from invocation handler upon client invocation calls.
   private static final String RESPONSE_VALUE = "This is the return to SampleInvocationHandler invocation";

   /**
    * Sets up target remoting server.
    *
    * @param locatorURI
    * @throws Exception
    */
   public void setupServer(String locatorURI) throws Exception
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();

      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);

      connector.start();
   }

   /**
    * Shuts down the server
    */
   public void shutdownServer()
   {
      connector.stop();
      connector.destroy();
   }

   /**
    * Can pass transport and port to be used as parameters.
    */
   public static void main(String[] args)
   {
      if(args != null && args.length == 2)
      {
         transport = args[0];
         port = Integer.parseInt(args[1]);
      }
      String locatorURI = transport + "://" + host + ":" + port;
      CallbackServer server = new CallbackServer();
      try
      {
         server.setupServer(locatorURI);

         // wait forever, let the user kill us at any point (at which point, the client will detect we went down)
         while(true)
         {
            Thread.sleep(1000);
         }

      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

   /**
    * Simple invocation handler implementation.  When callback clients are registered, will
    * generate callbacks periodically.
    */
   public static class SampleInvocationHandler
   implements ServerInvocationHandler, Runnable, CallbackListener
   {
      // list of callback listeners registered
      private List listeners = new ArrayList();

      // flag to indicate when should generate callback messages
      private boolean shouldGenerateCallbacks = false;

      public SampleInvocationHandler()
      {
         // will start a new thread for generating callbacks.
         Thread callbackThread = new Thread(this);
         callbackThread.setDaemon(true);
         callbackThread.start();
      }

      /**
       * Called by the remoting server to handle the invocation from client.
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return RESPONSE_VALUE;
      }

      /**
       * Adds a callback handler that will listen for callbacks from
       * the server invoker handler.
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         System.out.println("Adding callback listener.");
         listeners.add(callbackHandler);
         shouldGenerateCallbacks = true;
      }

      /**
       * Removes the callback handler that was listening for callbacks
       * from the server invoker handler.
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         System.out.println("Removing callback listener.");
         listeners.remove(callbackHandler);
         if(listeners.size() == 0)
         {
            shouldGenerateCallbacks = false;
         }
      }

      /**
       * Will generate callback messages every second while shouldGenerateCallbacks
       * flag is true.
       */
      public void run()
      {
         // keep looping while waiting to fire callbacks.
         while(true)
         {
            while(shouldGenerateCallbacks)
            {
               // create new callback message
               Callback callback = new Callback("Callback " + callbackCounter + ": This is the payload of callback invocation.");
               System.out.println("sending callback: " + callbackCounter);
               
               // Register as listener, pass callback id, and ask Remoting to handle acknowledgements.
               HashMap returnPayload = new HashMap();
               returnPayload.put(ServerInvokerCallbackHandler.CALLBACK_LISTENER, this);
               returnPayload.put(ServerInvokerCallbackHandler.CALLBACK_ID, new Integer(callbackCounter++));
               returnPayload.put(ServerInvokerCallbackHandler.REMOTING_ACKNOWLEDGES_PUSH_CALLBACKS, "true");
               callback.setReturnPayload(returnPayload);
               
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
               // sleep for a second before firing next callback message
               try
               {
                  Thread.sleep(1000);
               }
               catch(InterruptedException e)
               {
               }

            }
            // sleep for a second before while waiting for flag to be set
            try
            {
               Thread.sleep(1000);
            }
            catch(InterruptedException e)
            {
            }
         }
      }

      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
      
      public void acknowledgeCallback(InvokerCallbackHandler callbackHandler, Object callbackId, Object response)
      {
         System.out.println("received acknowledgement for callback: " + callbackId);
      }

   }
}