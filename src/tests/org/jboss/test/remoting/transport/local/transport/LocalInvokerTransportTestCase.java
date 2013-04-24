/*
* JBoss, a division of Red Hat
* Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
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
package org.jboss.test.remoting.transport.local.transport;

import junit.framework.TestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.transport.Connector;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.management.MBeanServer;
import java.io.ByteArrayInputStream;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class LocalInvokerTransportTestCase extends TestCase
{
   private Client client;
   private String transport = "local";
   private InvokerLocator locator;
   private static final Logger log = Logger.getLogger(LocalInvokerTransportTestCase.class);

//   public LocalInvokerTransportTestCase()
//   {
//   }
//
//   public LocalInvokerTransportTestCase(String transport)
//   {
//      this.transport = transport;
//   }


   public void testLocalInvocation() throws Throwable
   {
      InvokerLocator locator = new InvokerLocator("local://localhost");
//      Connector connector = new Connector("local://localhost");
      Connector connector = new Connector(locator);
      connector.create();
      connector.addInvocationHandler("mock", new TestServerInvocationHandler());
      connector.start();

//      Client client = new Client(new InvokerLocator("local://localhost"));
      Client client = new Client(locator);
      client.connect();
      client.invoke("barfoo");

   }

   public class TestServerInvocationHandler implements ServerInvocationHandler
   {

      public void setMBeanServer(MBeanServer server)
      {
      }

      public void setInvoker(ServerInvoker invoker)
      {
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         System.out.println("called with " + invocation.getParameter());
         return "foobar";
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
      }
   }

   public static void main(String[] args)
   {
      LocalInvokerTransportTestCase test = new LocalInvokerTransportTestCase();
      try
      {
         test.testLocalInvocation();
      }
      catch (Throwable throwable)
      {
         throwable.printStackTrace();
      }

   }
}
