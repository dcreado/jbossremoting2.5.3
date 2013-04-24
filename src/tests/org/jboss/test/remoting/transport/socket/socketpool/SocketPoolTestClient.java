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
package org.jboss.test.remoting.transport.socket.socketpool;

import junit.framework.TestCase;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.socket.SocketClientInvoker;

/**
 * See SocketPoolTestCase for description.
 *  
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * 
 * @version $Revision: 3204 $
 * <p>
 * Copyright Nov 2, 2006
 * </p>
 */
public class SocketPoolTestClient extends TestCase
{
   private static Logger log = Logger.getLogger(SocketPoolTestClient.class);
   
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 6413;


   public void testSocketPool() throws Throwable
   {
      Client client = null;

      try
      {
         log.warn("EXCEPTIONS ARE EXPECTED");

         String locatorURI = getTransport() + "://" + host + ":" + getPort() + "/?timeout=1000";
         log.info("Calling remoting server with locator uri of: " + locatorURI);

         // create InvokerLocator with the url type string
         // indicating the target remoting server to call upon.
         InvokerLocator locator = new InvokerLocator(locatorURI);

         client = new Client(locator);
         client.connect();
         SocketClientInvoker invoker = (SocketClientInvoker) client.getInvoker();
        
         // No sockets are in use.
//         assertEquals(0, invoker.usedPooled);
//         log.info("usedPool: " + invoker.usedPooled);
         assertEquals(0, invoker.getNumberOfUsedConnections());
   
         // Make SocketPoolTestServer.NUMBER_OF_CALLS invocations.
         for (int i = 0; i < SocketPoolTestServer.NUMBER_OF_CALLS; i++)
         {
            new Invoker(client, i).start();
         }
         
         Thread.sleep(500);
         
         // SocketPoolTestServer.NUMBER_OF_CALLS sockets are in use.
//         log.info("usedPool: " + invoker.usedPooled);
//         assertEquals(SocketPoolTestServer.NUMBER_OF_CALLS, invoker.usedPooled);
         log.info("usedPool: " + invoker.getNumberOfUsedConnections());
         assertEquals(SocketPoolTestServer.NUMBER_OF_CALLS, invoker.getNumberOfUsedConnections());
         
         
         Thread.sleep(5000);
         
         // All invocations have timed out.  All sockets should be closed and discarded.
//         log.info("usedPool: " + invoker.usedPooled);
//         assertEquals(0, invoker.usedPooled);
         log.info("usedPool: " + invoker.getNumberOfUsedConnections());
         assertEquals(0, invoker.getNumberOfUsedConnections());
      }
      finally
      {
         if(client != null)
         {
            client.disconnect();
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
      if(args != null && args.length == 2)
      {
         host = args[0];
         port = Integer.parseInt(args[1]);
      }
      SocketPoolTestClient client = new SocketPoolTestClient();
      try
      {
         client.testSocketPool();
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
   
   class Invoker extends Thread
   {
      private Client client;
      private int id;
      
      Invoker(Client client, int id)
      {
         this.client = client;
         this.id = id;
      }
      
      public void run()
      {
         try
         {
            client.invoke("payload");
            log.info("invoke succeeded: " + id);
         }
         catch (Throwable e)
         {
            log.info("invoke failed: " + id, e);
         }
      }
   }
}