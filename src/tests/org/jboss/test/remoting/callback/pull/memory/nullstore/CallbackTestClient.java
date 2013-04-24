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

package org.jboss.test.remoting.callback.pull.memory.nullstore;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;

import java.util.List;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class CallbackTestClient extends TestCase
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5411;

   private String locatorURI = transport + "://" + host + ":" + port;

   private Client remotingClient;
   private CallbackHandler pullCallbackHandler;

   private boolean isCallbackDone = false;

   private int numberOfCallbacks = 520;

   public void createRemotingClient() throws Exception
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Calling remoting server with locator uri of: " + locatorURI);

      // This could have been new Client(locator), but want to show that subsystem param is null
      // Could have also been new Client(locator, "sample");
      remotingClient = new Client(locator);
      remotingClient.connect();

   }

   public void makeInvocation(String param) throws Throwable
   {
      Object response = remotingClient.invoke(param, null);
      System.out.println("Invocation response: " + response);
   }

   public void setUp() throws Exception
   {
      createRemotingClient();
   }

   public void tearDown() throws Exception
   {
      if(remotingClient != null)
      {
         if(pullCallbackHandler != null)
         {
            try
            {
               remotingClient.removeListener(pullCallbackHandler);
            }
            catch(Throwable throwable)
            {
               throw new Exception(throwable);
            }
         }
         remotingClient.disconnect();
      }
   }

   public void testPullCallback() throws Throwable
   {
      numberOfCallbacks = calculateNumberOfCallbacks();
      System.out.println("Number of callbacks need to activate persitence: " + numberOfCallbacks);
      pullCallbackHandler = new CallbackHandler();
      // by passing only the callback handler, will indicate pull callbacks
      remotingClient.addListener(pullCallbackHandler);

      // need to tell server handler how many
      makeInvocation("" + numberOfCallbacks);

      // now make invocation on server, which should cause a callback to happen
      makeInvocation("Do something");

      boolean didItWork = checkForCallback();

      System.out.println("Did id work = " + didItWork);

      int totalCallbacks = 0;
      if(didItWork)
      {
         boolean gotExpectedException = false;

         try
         {
            // now need to go get the callbacks until none left.
            int callbacksReceived = getAllCallbacks(pullCallbackHandler);

            System.out.println("callbacks received = " + callbacksReceived);
            totalCallbacks = totalCallbacks + callbacksReceived;

         }
         catch(RuntimeException re)
         {
            System.out.println("Got exception as expected - " + re);
            gotExpectedException = true;
         }
         catch(Throwable thr)
         {
            System.out.println("Got unexpected exception - " + thr);
         }

         if(gotExpectedException)
         {
            System.out.println("PASSED");
         }
         else
         {
            System.out.println("FAILED");
         }
         assertTrue(gotExpectedException);
      }

      System.out.println("total callbacks received: " + totalCallbacks);
      System.out.println("total callbacks expected: " + numberOfCallbacks);

   }

   /**
    * calculate how many 102400 byte callback messages it will take to consume 30%
    * of the vm's memory.  The CallbackInvocationHandler will take care of consuming 70%
    * so need to make sure we have enough callbacks to trigger persistence.
    */
   private int calculateNumberOfCallbacks()
   {
      long max = Runtime.getRuntime().maxMemory();
      int targetMem = (int) (max * 0.1);
      int num = targetMem / 102400;
      return num;
   }

   private int getAllCallbacks(CallbackHandler pullCallbackHandler) throws Throwable
   {
      int counter = 0;
      List callbacks = null;

      callbacks = remotingClient.getCallbacks(pullCallbackHandler);
      while(callbacks.size() > 0)
      {
         System.out.println("callbacks.size() = " + callbacks.size());
         counter = counter + callbacks.size();
         for(int i = 0; i < callbacks.size(); i++)
         {
            ((Callback) callbacks.get(i)).getCallbackObject();
         }

         // need to give time for server to clean up mem
         Thread.currentThread().sleep(2000);
         callbacks = remotingClient.getCallbacks(pullCallbackHandler);
      }
      return counter;
   }

   private boolean checkForCallback() throws Throwable
   {
      boolean isComplete = false;

      int waitPeriod = 1000;
      int numOfWaits = 360; // means will wait 30 seconds
      for(int x = 0; x < numOfWaits; x++)
      {
         //isComplete = pushCallbackHandler.isComplete();
         isComplete = ((Boolean) remotingClient.invoke("getdone")).booleanValue();
         if(!isComplete)
         {
            try
            {
               Thread.currentThread().sleep(waitPeriod);
            }
            catch(InterruptedException e)
            {
               e.printStackTrace();
            }
         }
         else
         {
            break;
         }
      }
      return isComplete;
   }

   public class PushCallbackHandler extends CallbackHandler
   {

   }

   public class CallbackHandler implements InvokerCallbackHandler
   {
      boolean isComplete = false;

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
         System.out.println("Received callback value of: " + callback.getCallbackObject());
         System.out.println("Received callback handle object of: " + callback.getCallbackHandleObject());
         System.out.println("Received callback server invoker of: " + callback.getServerLocator());
         isComplete = true;
      }

      public boolean isComplete()
      {
         return isComplete;
      }
   }

}