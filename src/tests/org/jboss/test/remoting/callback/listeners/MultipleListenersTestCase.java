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

package org.jboss.test.remoting.callback.listeners;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.management.MBeanServer;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import junit.framework.TestCase;

/**
 * This is a test to make sure multiple callback listeners can be added
 * from the same client and the each gets their callbacks, as well
 * as making sure they are removed correctly.
 * <p/>
 * This test really only works the way it is coded because know that
 * everything will be run locally, meaning that I can check the
 * callback values after making in invoke call because the same
 * thread will make the callbacks before the thread returns from
 * invoke method.
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @author <a href="mailto:tom@jboss.org">Ton Elrod</a>
 * @version <tt>$Revision: 1610 $</tt>
 */
public class MultipleListenersTestCase extends TestCase
{
   private static List callbacks = new ArrayList();

   private static String value1 = "FOO";
   private static String value2 = "BAR";
   private static String value3 = "FOOBAR";

   public void testCallbacks() throws Throwable
   {

      // Start the callback server
      InvokerLocator callbackServerLocator = new InvokerLocator("socket://localhost:2222");
      Connector callbackConnector = new Connector();
      callbackConnector.setInvokerLocator(callbackServerLocator.getLocatorURI());
      callbackConnector.start();

      // Start the target server
      InvokerLocator targetServerLocator = new InvokerLocator("socket://localhost:3456");
      Connector connector = new Connector();
      connector.setInvokerLocator(targetServerLocator.getLocatorURI());
      connector.create();
      connector.addInvocationHandler("MySubsystem", new ServerInvocationHandlerImpl());
      connector.start();


      Client client = new Client(targetServerLocator);
      client.connect();

      InvokerCallbackHandler callbackHandler1 = new InvokerCallbackHandlerImpl("ONE");
      InvokerCallbackHandler callbackHandler2 = new InvokerCallbackHandlerImpl("TWO");

      client.addListener(callbackHandler1, callbackServerLocator);

      client.invoke("call back " + value1);

      assertEquals(1, callbacks.size());
      callbacks.clear();

      client.addListener(callbackHandler2, callbackServerLocator);

      client.invoke("call back " + value2);

      assertEquals(2, callbacks.size());
      callbacks.clear();

      client.removeListener(callbackHandler1);

      client.invoke("call back " + value3);

      assertEquals(1, callbacks.size());
      callbacks.clear();

      client.removeListener(callbackHandler2);

      connector.stop();
      callbackConnector.stop();

      connector.destroy();
      callbackConnector.destroy();
   }


   private static class InvokerCallbackHandlerImpl implements InvokerCallbackHandler
   {
      private String id;

      public InvokerCallbackHandlerImpl(String id)
      {
         this.id = id;
      }

      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         System.out.println("Handler " + id + " got callback: " + callback.getParameter());
         callbacks.add(callback.getParameter());
         //throw new CustomListenerException("This is the custom listener exception that I want to know about.");
      }
   }

   private static class ServerInvocationHandlerImpl implements ServerInvocationHandler
   {

      protected ServerInvoker serverInvoker;
      protected ArrayList callbackHandlers = new ArrayList();

      public void setMBeanServer(MBeanServer server)
      {
      }

      public void setInvoker(ServerInvoker invoker)
      {
         serverInvoker = invoker;
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         String param = (String) invocation.getParameter();
         System.out.println(param);

         if(param.startsWith("call back"))
         {
            String arg = param.substring("call back".length()).trim();
            for(Iterator i = callbackHandlers.iterator(); i.hasNext();)
            {
               InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler) i.next();
               System.out.println("sending callback to " + callbackHandler);
               callbackHandler.handleCallback(new Callback(arg));
            }
         }

         return null;
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         System.out.println("adding listener " + callbackHandler);
         callbackHandlers.add(callbackHandler);
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         System.out.println("removing listener " + callbackHandler);
         callbackHandlers.remove(callbackHandler);
      }
   }

   private static class CustomListenerException extends RuntimeException
   {
      public CustomListenerException(String e)
      {
         super(e);
      }
   }

}
