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
package org.jboss.test.remoting.transport.socket.connection;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Version;
import org.jboss.remoting.transport.socket.SocketServerInvoker;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketTestClient extends TestCase
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
//   private static String host = "192.168.1.100";
   private static int port = 5400;

   private int numOfCalls = 10000;
//   private int numOfCalls = 2;

   public void testInvocations() throws Throwable
   {
//      System.setProperty(Version.PRE_2_0_COMPATIBLE, "true");

      Client remotingClient = null;

      try
      {


         //String locatorURI = transport + "://" + host + ":" + port + "/?" + SocketServerInvoker.CHECK_CONNECTION_KEY + "=" + Boolean.TRUE;
         //String locatorURI = transport + "://" + host + ":" + port + "/?" + InvokerLocator.SERIALIZATIONTYPE + "=jboss";
         String locatorURI = getTransport() + "://" + host + ":" + getPort();

         // create InvokerLocator with the url type string
         // indicating the target remoting server to call upon.
         InvokerLocator locator = new InvokerLocator(locatorURI);
         System.out.println("Calling remoting server with locator uri of: " + locatorURI);

         remotingClient = new Client(locator);
         remotingClient.connect();
         String request = "Do something";
         System.out.println("Invoking server with request of '" + request + "'");

         long startTime = System.currentTimeMillis();


         Object response = null;
         for(int x = 0; x < numOfCalls; x++)
         {
            response = remotingClient.invoke(request);
//            System.out.println("Invocation response: " + response);
         }

         long endTime = System.currentTimeMillis();
         System.out.println("Time to make " + numOfCalls + " was " + (endTime -startTime) + " milliseconds.");

         int callValue = 0;
         if(response instanceof Integer)
         {
            callValue = ((Integer) response).intValue();
         }
         assertEquals(numOfCalls, callValue);

      }
      finally
      {
         if(remotingClient != null)
         {
            remotingClient.disconnect();
         }
      }

   }

   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      if(args != null && args.length == 3)
      {
         transport = args[0];
         host = args[1];
         port = Integer.parseInt(args[2]);
      }
      SocketTestClient client = new SocketTestClient();
      try
      {
         client.testInvocations();
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }

   
   protected String getTransport()
   {
      return transport;
   }
   
   protected int getPort()
   {
      return port;
   }

}