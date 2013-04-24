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
package org.jboss.test.remoting.transport.socket.raw;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class RawTestServer extends ServerTestCase
{
   public static final String RESPONSE = "This is response";

   private Connector connector;

   public void setUp() throws Exception
   {
      connector = new Connector("socket://localhost:6700");
      connector.create();
      connector.addInvocationHandler("test", new TestInvocationHandler());
      connector.start();
   }

   public void tearDown()
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      RawTestServer server = new RawTestServer();
      try
      {
         server.setUp();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   public class TestInvocationHandler implements ServerInvocationHandler
   {

      public void setMBeanServer(MBeanServer server)
      {
         //To change body of implemented methods use File | Settings | File Templates.
      }

      public void setInvoker(ServerInvoker invoker)
      {
         //To change body of implemented methods use File | Settings | File Templates.
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return RESPONSE;
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         //To change body of implemented methods use File | Settings | File Templates.
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         //To change body of implemented methods use File | Settings | File Templates.
      }
   }
}