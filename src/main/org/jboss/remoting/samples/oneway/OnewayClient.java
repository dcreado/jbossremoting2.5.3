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

package org.jboss.remoting.samples.oneway;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

/**
 * Simple test client to make oneway invocations on remoting server.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class OnewayClient
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;

   public void makeInvocation(String locatorURI) throws Throwable
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Calling remoting server with locator uri of: " + locatorURI);

      Client remotingClient = new Client(locator);
      remotingClient.connect();

      /**
       * Make oneway invocation.  Since this is a oneway invocation,
       * the return is void.
       *
       * With this invokeOneway signature, it uses the current thread to execute
       * the invocation on the server and will not return until the invocation request
       * has been placed into a worker thread pool on the server.  This ensures that
       * the call at least made it to the server.
       */
      String payload1 = "Oneway call 1.";
      System.out.println("Making oneway invocation with payload of '" + payload1 + "'");
      remotingClient.invokeOneway(payload1);

      /**
       * This call is the same as the one above, except the last parameter will
       * place the invocation request into a worker thread pool on the client and
       * return immediately.  The worker thread will then make the invocation
       * on the remoting server (which means caller will not be aware if was a
       * problem making the actual invocation on the server).
       */
      String payload2 = "Oneway call 2.";
      System.out.println("Making oneway invocation with payload of '" + payload2 + "'");
      remotingClient.invokeOneway(payload2, null, true);
   }

   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      if(args != null && args.length == 2)
      {
         transport = args[0];
         port = Integer.parseInt(args[1]);
      }
      String locatorURI = transport + "://" + host + ":" + port;
      OnewayClient client = new OnewayClient();
      try
      {
         client.makeInvocation(locatorURI);
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }


}