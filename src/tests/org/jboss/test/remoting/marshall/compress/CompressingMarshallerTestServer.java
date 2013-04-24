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

package org.jboss.test.remoting.marshall.compress;

import javax.management.MBeanServer;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.compress.CompressingMarshaller;
import org.jboss.remoting.marshal.compress.CompressingUnMarshaller;
import org.jboss.remoting.transport.Connector;


/**
 * A CompressingMarshallerTestServer.
 *
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 566 $
 *          <p/>
 *          Copyright (c) 2005
 *          </p>
 */

public class CompressingMarshallerTestServer extends ServerTestCase
{
   private static final Object lock = new Object();

   public void setUp()
   {
      new Thread()
      {
         public void run()
         {
            try
            {
               MarshalFactory.addMarshaller("compress", new CompressingMarshaller(), new CompressingUnMarshaller());
               String locatorURI = "socket://localhost:5400/?datatype=compress";
               InvokerLocator locator = new InvokerLocator(locatorURI);
               System.out.println("Starting remoting server with locator uri of: " + locatorURI);
               Connector connector = new Connector();
               connector.setInvokerLocator(locator.getLocatorURI());
               connector.create();
               SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
               connector.addInvocationHandler("sample", invocationHandler);
               connector.start();

               synchronized(lock)
               {
                  lock.wait();
               }

               connector.stop();
               connector.destroy();
            }
            catch(Exception e)
            {
               e.printStackTrace();
            }
         }
      }.start();
   }


   public static void main(String[] args)
   {
      new CompressingMarshallerTestServer().setUp();
   }


   /**
    * Simple invocation handler implementation.
    * This is the code that will be called with the invocation payload from the client.
    */
   public static class SampleInvocationHandler implements ServerInvocationHandler
   {
      int counter;

      /**
       * called to handle a specific invocation
       *
       * @param invocation
       * @return
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         if(++counter == 2)
         {
            synchronized(lock)
            {
               lock.notify();
            }
         }

         return invocation.getParameter();
      }

      /**
       * Adds a callback handler that will listen for callbacks from
       * the server invoker handler.
       *
       * @param callbackHandler
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as do not handling callback listeners in this example
      }

      /**
       * Removes the callback handler that was listening for callbacks
       * from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as do not handling callback listeners in this example
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
}

