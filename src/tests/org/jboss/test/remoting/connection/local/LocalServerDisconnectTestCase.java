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
package org.jboss.test.remoting.connection.local;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class LocalServerDisconnectTestCase extends TestCase
{
   private Client client = null;
   private Connector connector = null;

   public void setUp() throws Exception
   {
      String locatorUri = "socket://localhost:8888";

      connector = new Connector();
      connector.setInvokerLocator(locatorUri);
      connector.create();
      connector.addInvocationHandler("test", new TestInvocationHandler());

      connector.start();

      client = new Client(new InvokerLocator(locatorUri));
      client.connect();
   }

   public void testConnection() throws Throwable
   {
      Object r = client.invoke("foobar");
      assertEquals("tadpole", r);

      connector.stop();
      connector.destroy();
      connector = null;

      // should now get an exception thrown due to connector being gone
      try
      {
         client.invoke("barfoo");
      }
      catch (Throwable throwable)
      {
         System.out.println("Got exception as expected. " + throwable.getMessage());
         assertTrue(true);
         return;
      }

      assertTrue("Should have caught exception and returned.", false);

   }

   public void tearDown()
   {
      if (client != null)
      {
         client.disconnect();
      }
      if (connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public class TestInvocationHandler implements ServerInvocationHandler
   {

      public void setMBeanServer(MBeanServer server)
      {
         //TODO: -TME Implement
      }

      public void setInvoker(ServerInvoker invoker)
      {
         //TODO: -TME Implement
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return "tadpole";
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         //TODO: -TME Implement
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         //TODO: -TME Implement
      }
   }


}