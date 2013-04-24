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

package org.jboss.test.remoting.exception.server;

import javax.management.MBeanServer;
import org.jboss.logging.Logger;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.transport.Connector;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ServerExceptionTestCase extends TestCase
{
   private static final Logger log = Logger.getLogger(ServerExceptionTestCase.class);

   public void setupServer(String locatorURI) throws Exception
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      Connector connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.start();

      TestInvocationHandler invocationHandler = new TestInvocationHandler();
      // first parameter is sub-system name.  can be any String value.
      connector.addInvocationHandler("test", invocationHandler);
   }

   public void testServerException()
   {

      try
      {
         log.debug("running testServerException()");

         InvokerLocator locator = new InvokerLocator("socket://localhost:8823");
         setupServer(locator.getOriginalURI());
         Client client = new Client(locator, "test");
         client.connect();

         log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());

         try
         {
            Object ret = client.invoke(new NameBasedInvocation("throwServerException",
                                                               new Object[]{"nonserialized"},
                                                               new String[]{String.class.getName()}),
                                       null);
         }
         catch(NonSerializeTestException nonEx)
         {
            log.debug("Expected to get NonSerializable exception and got it.", nonEx);
            assertTrue(true);
         }
         try
         {
            Object ret = client.invoke(new NameBasedInvocation("throwServerException",
                                                               new Object[]{"serialized"},
                                                               new String[]{String.class.getName()}),
                                       null);
         }
         catch(SerializedTestException ex)
         {
            log.debug("Expected to get Serializable exception and got it.", ex);
            assertTrue(true);
         }

      }
      catch(CannotConnectException cce)
      {
         log.debug("Got CannotConnectException.", cce);
         assertTrue("Did not expect CannotConnectException.", false);
      }
      catch(Throwable tr)
      {
         tr.printStackTrace();
         assertTrue("Did not catch server exception as expected.", false);
      }
   }

   /**
    * Simple invocation handler implementation.
    */
   public static class TestInvocationHandler implements ServerInvocationHandler
   {
      /**
       * called to handle a specific invocation
       *
       * @param invocation
       * @return
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         String param = (String) ((NameBasedInvocation) invocation.getParameter()).getParameters()[0];

         if(param.equals("serialized"))
         {
            throw new SerializedTestException("This is serialized server test exception");
         }
         else
         {
            throw new NonSerializeTestException("This is a non serialized server test exception");
         }
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