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

package org.jboss.test.remoting.transport.local;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Just a simple example of how to setup remoting to make an invocation to local target,
 * so are not actually going out of process, thus not really using any transport protocol.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class LocalInvocationTestCase extends TestCase
{
   private static final String RESPONSE_VALUE = "This is the return to SampleInvocationHandler invocation";

   public LocalInvocationTestCase(String name)
   {
      super(name);
   }

   public static void setupConfiguration(InvokerLocator locator, ServerInvocationHandler invocationHandler) throws Exception
   {
      Connector connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.start();
      connector.addInvocationHandler("mock", invocationHandler);
   }

   public void testInvocation() throws Throwable
   {
      InvokerLocator locator = new InvokerLocator("socket://localhost:6789");
      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();

      // set up
      LocalInvocationTestCase.setupConfiguration(locator, invocationHandler);

      // This could have been new Client(locator), but want to show that subsystem param is null
      Client remotingClient = new Client(locator);
      remotingClient.connect();
      Object response = remotingClient.invoke("Do something", null);

      System.out.println("Invocation response: " + response);
      assertEquals(response, RESPONSE_VALUE);
   }

   public static class SampleInvocationHandler implements ServerInvocationHandler
   {
      private List listeners = new ArrayList();

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

      /**
       * called to handle a specific invocation
       *
       * @param invocation
       * @return
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         // Just going to return static string as this is just simple example code.

         // Will also fire callback to listeners if they were to exist using
         // simple invocation request.
         Callback callbackInvocationRequest = new Callback("This is the payload of callback invocation");
         Iterator itr = listeners.iterator();
         while(itr.hasNext())
         {
            InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler) itr.next();
            callbackHandler.handleCallback(callbackInvocationRequest);
         }

         return RESPONSE_VALUE;

      }

      /**
       * Adds a callback handler that will listen for callbacks from
       * the server invoker handler.
       *
       * @param callbackHandler
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         listeners.add(callbackHandler);
      }

      /**
       * Removes the callback handler that was listening for callbacks
       * from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         listeners.remove(callbackHandler);
      }
   }
}