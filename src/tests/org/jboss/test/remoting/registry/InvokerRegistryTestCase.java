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

package org.jboss.test.remoting.registry;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.local.LocalClientInvoker;

import javax.management.MBeanServer;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 1036 $</tt>
 *          <p/>
 *          $Id: InvokerRegistryTestCase.java 1036 2006-05-21 04:47:32Z telrod $
 */
public class InvokerRegistryTestCase extends TestCase
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   // Constructors --------------------------------------------------

   public InvokerRegistryTestCase(String name)
   {
      super(name);
   }

   // Public --------------------------------------------------------

   public void setUp() throws Exception
   {
      super.setUp();
   }

   public void tearDown() throws Exception
   {
      super.tearDown();
   }

   public void testEmptyInvokerRegistry() throws Throwable
   {
      ClientInvoker[] clientInvokers = InvokerRegistry.getClientInvokers();
      assertEquals(0, clientInvokers.length);
   }

   public void testInvokerRegistry() throws Throwable
   {
      String serverlocatorURI = "socket://127.0.0.1:5555";

      Connector server = new Connector();
      server.setInvokerLocator(serverlocatorURI);
      server.start();
      server.addInvocationHandler("TEST", new ServerInvocationHandlerImpl());

      new Client(new InvokerLocator(serverlocatorURI), "TEST").connect();

      ClientInvoker[] clientInvokers = InvokerRegistry.getClientInvokers();
      assertEquals(1, clientInvokers.length);

      LocalClientInvoker clientInvoker = (LocalClientInvoker) clientInvokers[0];
      InvokerLocator locator = clientInvoker.getLocator();

      assertEquals("socket", locator.getProtocol());
      assertEquals("127.0.0.1", locator.getHost());
      assertEquals(5555, locator.getPort());

   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

   private class ServerInvocationHandlerImpl implements ServerInvocationHandler
   {
      public void setMBeanServer(MBeanServer server)
      {
      }

      public void setInvoker(ServerInvoker invoker)
      {
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return null;
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
      }
   }
}
