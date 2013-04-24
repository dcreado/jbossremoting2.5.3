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

package org.jboss.test.remoting.byvalue;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.transport.mock.MockServerInvocationHandler;

/**
 * Just a simple example of how to setup remoting to make an invocation to local target,
 * so are not actually going out of process, thus not really using any transport protocol.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class ByValueInvocationTestCase extends TestCase
{
   private Connector connector = null;

   public void setupConfiguration(InvokerLocator locator, ServerInvocationHandler invocationHandler) throws Exception
   {
      connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.start();
      connector.addInvocationHandler("mock", invocationHandler);
   }

   public void testJavaInvocation() throws Throwable
   {
      InvokerLocator locator = new InvokerLocator("rmi://localhost:5400/?" +
                                                  InvokerLocator.BYVALUE + "=" + Boolean.TRUE.toString());
      ServerInvocationHandler invocationHandler = new MockServerInvocationHandler();

      try
      {
         // set up
         setupConfiguration(locator, invocationHandler);

         Thread.sleep(3000);

         // This could have been new Client(locator), but want to show that subsystem param is null
         Client remotingClient = new Client(locator);
         remotingClient.connect();
         ByValuePayload byValuePayload = new ByValuePayload();
         Object response = remotingClient.invoke(new NameBasedInvocation("testByValue",
                                                                         new Object[]{byValuePayload},
                                                                         new String[]{byValuePayload.getClass().getName()}),
                                                 null);

         System.out.println("Invocation response: " + response);
         if (response instanceof Boolean)
         {
            assertTrue("Result of testByValue is false.", ((Boolean) response).booleanValue());
         }
         else
         {
            assertTrue("Result of testByValue was not even of type Boolean.", false);
         }
         remotingClient.disconnect();
      }
      finally
      {
         if (connector != null)
         {
            connector.stop();
            connector.destroy();
            connector = null;
         }
      }

   }

   public void testJBossInvocation() throws Throwable
   {
      InvokerLocator locator = new InvokerLocator("rmi://localhost:5401/?" +
                                                  InvokerLocator.BYVALUE + "=" + Boolean.TRUE.toString() + "&" +
                                                  InvokerLocator.SERIALIZATIONTYPE + "=" + "jboss");
      ServerInvocationHandler invocationHandler = new MockServerInvocationHandler();

      try
      {
         // set up
         setupConfiguration(locator, invocationHandler);

         Thread.sleep(3000);

         // This could have been new Client(locator), but want to show that subsystem param is null
         Client remotingClient = new Client(locator);
         remotingClient.connect();
         ByValuePayload byValuePayload = new ByValuePayload();
         Object response = remotingClient.invoke(new NameBasedInvocation("testByValue",
                                                                         new Object[]{byValuePayload},
                                                                         new String[]{byValuePayload.getClass().getName()}),
                                                 null);

         System.out.println("Invocation response: " + response);
         if (response instanceof Boolean)
         {
            assertTrue("Result of testByValue is false.", ((Boolean) response).booleanValue());
         }
         else
         {
            assertTrue("Result of testByValue was not even of type Boolean.", false);
         }
         remotingClient.disconnect();
      }
      finally
      {
         if (connector != null)
         {
            connector.stop();
            connector.destroy();
            connector = null;
         }
      }

   }

}