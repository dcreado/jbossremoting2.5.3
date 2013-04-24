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

package org.jboss.test.remoting.transport.socket.timeout.keepalive;

import javax.management.MBeanServer;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TimeoutServerTest
{
   //private String locatorURI = "socket://localhost:8899/?socketTimeout=1000&backlog=500&clientMaxPoolSize=100&maxPoolSize=500";
   //private String locatorURI = "socket://localhost:8899/?socketTimeout=1000&backlog=0&clientMaxPoolSize=10&maxPoolSize=3";
   private String locatorURI = "socket://localhost:8899/?socketTimeout=1000&backlog=0&clientMaxPoolSize=10&maxPoolSize=3&serializationtype=jboss";
   private Connector connector = null;

   private static int counter = 0;

   public void setUp() throws Exception
   {
      connector = new Connector();
      InvokerLocator locator = new InvokerLocator(locatorURI);
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();
      connector.addInvocationHandler("test", new TimeoutHandler());
      connector.start();
   }

   protected void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
//      org.apache.log4j.BasicConfigurator.configure();
//      org.apache.log4j.Category.getInstance("org.jboss.remoting.transport.socket.ServerThread").setLevel(XLevel.TRACE);
//      org.apache.log4j.Category.getInstance("org.jboss.remoting.transport.socket.SocketServerInvoker").setLevel(XLevel.TRACE);

      TimeoutServerTest server = new TimeoutServerTest();
      try
      {
         server.setUp();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

   private class TimeoutHandler implements ServerInvocationHandler
   {

      /**
       * set the mbean server that the handler can reference
       *
       * @param server
       */
      public void setMBeanServer(MBeanServer server)
      {
         //TODO: -TME Implement
      }

      /**
       * set the invoker that owns this handler
       *
       * @param invoker
       */
      public void setInvoker(ServerInvoker invoker)
      {
         //TODO: -TME Implement
      }

      /**
       * called to handle a specific invocation.  Please take care to make sure
       * implementations are thread safe and can, and often will, receive concurrent
       * calls on this method.
       *
       * @param invocation
       * @return
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         Object obj = invocation.getParameter();
         if(obj instanceof String && "timeout".equals(obj))
         {
            Thread.currentThread().sleep(30000);
         }
         return new Integer(++counter);  //TODO: -TME Implement
      }

      /**
       * Adds a callback handler that will listen for callbacks from
       * the server invoker handler.
       *
       * @param callbackHandler
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         //TODO: -TME Implement
      }

      /**
       * Removes the callback handler that was listening for callbacks
       * from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         //TODO: -TME Implement
      }
   }
}