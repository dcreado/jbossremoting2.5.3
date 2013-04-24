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

/*
 * Created on Oct 4, 2005
 */
package org.jboss.remoting.samples.multiplex.invoker;
/*
 * JBoss, the OpenSource J2EE webOS
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;


/**
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 */
public class Server2Client2
{
   private Client client;
   private SampleCallbackHandler handler;
   private Connector connector;
   private InvokerLocator locator;


   /**
    *  This will create the Client.
    */
   public void init()
   {
      try
      {
         String locatorURI = "multiplex://localhost:9090/?multiplexBindHost=localhost&multiplexBindPort=8080";
         InvokerLocator locator = new InvokerLocator(locatorURI);
         client = new Client(locator, "sample");
         client.connect();
         System.out.println("Connected client to server at: " + client.getInvoker().getLocator().getLocatorURI());
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

  
   /**
    * This will be used to create callback server
    *
    * @param port
    * @return
    * @throws Exception
    */
   private InvokerLocator initServer() throws Exception
   {
      String locatorURI = "multiplex://localhost:8080/?multiplexConnectHost=localhost&multiplexConnectPort=9090";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      connector = new Connector(locator.getLocatorURI());
      connector.create();
      connector.start();
      System.out.println("Started callback server at:    " + connector.getInvokerLocator());
      return connector.getLocator();
   }


   public void setUp() throws Exception
   {
      locator = initServer();
      init();
   }

   public void tearDown() throws Throwable
   {
      while (!handler.gotCallbacks)
         Thread.sleep(1000);
      
      client.removeListener(handler);
      
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
         connector = null;
      }
      locator = null;
      if(client != null)
      {
         client.disconnect();
         client = null;
      }
   }


   public void makeClientCall() throws Throwable
   {
      handler = new SampleCallbackHandler();

      // Need to add callback listener to get callback
      client.addListener(handler, locator, client.getSessionId());

      // make invocation
      Object answer = client.invoke(new Integer(17));
      
      System.out.println("invocation returns: " + ((Integer) answer).intValue());
   }


   public static void main(String[] args)
   {
      Server2Client2 test = new Server2Client2();
      
      try
      {
         test.setUp();
         test.makeClientCall();
         test.tearDown();
      }
      catch(Throwable throwable)
      {
         throwable.printStackTrace();
      }
   }

   
   public static class SampleCallbackHandler implements InvokerCallbackHandler
   {
      int callbackCounter;
      boolean gotCallbacks;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         Object ret = callback.getCallbackObject();
         System.out.println("callback value: " + ret);

         if (++callbackCounter == 2)
            gotCallbacks = true;
      }
      
      public boolean gotCallbacks()
      {
         return gotCallbacks;
      }
   }
}


