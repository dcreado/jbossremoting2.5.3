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
package org.jboss.test.remoting.invoker;

import junit.framework.TestCase;


import org.jboss.remoting.*;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.logging.Logger;

import javax.management.MBeanServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class DisconnectInvokerTestCase extends TestCase
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(DisconnectInvokerTestCase.class.getName());

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   // Constructors --------------------------------------------------

   public DisconnectInvokerTestCase(String name)
   {
      super(name);
   }

   // TestCase overrides -------------------------------------------

   public void setUp() throws Exception
   {
      super.setUp();
   }

   public void tearDown() throws Exception
   {
      super.tearDown();
   }

   public void testInvokerThreadSafety() throws Exception
   {
      Connector serverConnector = new Connector();

      InvokerLocator serverLocator = new InvokerLocator("socket://localhost:9099");

      serverConnector.setInvokerLocator(serverLocator.getLocatorURI());

      serverConnector.create();

      SimpleServerInvocationHandler invocationHandler = new SimpleServerInvocationHandler();

      serverConnector.addInvocationHandler("JMS", invocationHandler);

      serverConnector.start();

      //Create n clients each firing requests in their own thread, using the same locator

      final int NUM_CLIENTS = 3;

      Thread[] threads = new Thread[NUM_CLIENTS];
      Invoker[] invokers = new Invoker[NUM_CLIENTS];

      Object obj = new Object();

      for (int i = 0; i < NUM_CLIENTS; i++)
      {
         invokers[i] = new Invoker(serverLocator, obj);
         threads[i] = new Thread(invokers[i]);
         threads[i].start();
      }

      synchronized (obj)
      {
         obj.wait();
      }

      for (int i = 0; i < NUM_CLIENTS; i++)
      {
         if (invokers[i].failed)
         {
            fail();
            for (int j = 0; j < NUM_CLIENTS; j++)
            {
               threads[j].interrupt();
            }
         }
      }

      for (int i = 0; i < NUM_CLIENTS; i++)
      {
         threads[i].join();
      }

      for (int i = 0; i < NUM_CLIENTS; i++)
      {
         if (invokers[i].failed)
         {
            fail();
         }
      }
   }

   class Invoker implements Runnable
   {
      boolean failed;
      InvokerLocator locator;
      Object o;
      Invoker(InvokerLocator locator, Object o)
      {
         this.locator = locator;
         this.o = o;
      }
      public void run()
      {
         try
         {
            for (int i = 0; i < 10000; i++)
            {
               Client cl = new Client(locator);
               cl.connect();
               cl.invoke("aardvark");
               cl.disconnect();
            }
            synchronized (o)
            {
               o.notify();
            }
         }
         catch (Throwable t)
         {
            failed = true;
            log.error("Caught throwable", t);
            synchronized (o)
            {
               o.notify();
            }
         }
      }
   }

   class SimpleServerInvocationHandler implements ServerInvocationHandler
   {
      InvokerCallbackHandler handler;


      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         this.handler = callbackHandler;

      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         //log.info("Received invocation:" + invocation);

         return "Sausages";
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         // FIXME removeListener

      }

      public void setInvoker(ServerInvoker invoker)
      {
         // FIXME setInvoker

      }

      public void setMBeanServer(MBeanServer server)
      {
         // FIXME setMBeanServer

      }

   }

}

