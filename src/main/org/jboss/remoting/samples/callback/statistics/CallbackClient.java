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

import java.util.HashMap;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.CallbackPoller;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;

/**
 * Sample client that demonstrates CallbackPoller tuning statistics.
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class CallbackClient
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;


   /**
    * Registers callback listener and asks CallbackPoller to print statistics.
    */
   public void getPolledCallbacks(String locatorURI) throws Throwable
   {
      System.out.println("Calling remoting server with locator uri of: " + locatorURI);
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Client client = new Client(locator);
      client.connect();
      
      // Registter callback handler and tell CallbackPoller to report statistics.
      CallbackHandler callbackHandler = new CallbackHandler();
      HashMap metadata = new HashMap();
      metadata.put(CallbackPoller.REPORT_STATISTICS, "true");
      client.addListener(callbackHandler, metadata);

      // Wait for callbacks to received, processed, and acknowledged.
      Thread.sleep(20000);

      // remove callback handler from server
      client.removeListener(callbackHandler);
      client.disconnect();
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
      CallbackClient callbackClient = new CallbackClient();
      try
      {
         callbackClient.getPolledCallbacks(locatorURI);
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }

   /**
    * Our implementation of the InvokerCallbackHandler.  Simply
    * prints out the callback object's message upon receiving it.
    */
   public class CallbackHandler implements InvokerCallbackHandler
   {
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         // Slow down callback handling so callbacks build up in CallbackPoller.
         try
         {
            Thread.sleep(1000);
         }
         catch (InterruptedException e) {}

         System.out.println("Received push callback.");
         System.out.println("Received callback value of: " + callback.getCallbackObject());
         System.out.println("Received callback server invoker of: " + callback.getServerLocator());
      }
   }

}