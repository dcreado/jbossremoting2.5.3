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

package org.jboss.test.remoting.deploy;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvalidConfigurationException;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class DualDeploymentTestCase extends TestCase
{
   private static int idCounter = 1;

   public DualDeploymentTestCase(String name)
   {
      super(name);
   }

   /**
    * This test checks to see if use the exact same locator uri, will get the
    * exact same server invoker (even though connectors are different).  If add handler to
    * each of the different connectors (with different subsystems), will really be two handlers in the same server invoker
    *
    * @throws Throwable
    */
   public void testSameLocator() throws Throwable
   {
      MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer();

      String locator1 = "socket://localhost:5701";
      String locator2 = "socket://localhost:5701";

      int id1 = 1;
      int id2 = 2;

      Connector connector1 = setupServer(locator1, mbeanServer);
      SampleInvocationHandler invocationHandler1 = new SampleInvocationHandler(id1);
      connector1.addInvocationHandler("sub1", invocationHandler1);

      Connector connector2 = null;
      try
      {
         connector2 = setupServer(locator2, mbeanServer);
         SampleInvocationHandler invocationHandler2 = new SampleInvocationHandler(id2);
         connector2.addInvocationHandler("sub2", invocationHandler2);
      }
      catch(InvalidConfigurationException e)
      {
         assertTrue("Got InvalidConfigurationException as expected.", true);
         return;
      }
      finally
      {
         connector1.stop();
         connector1.destroy();
         if(connector2 != null)
         {
            connector2.stop();
            connector2.destroy();
         }
      }
      assertTrue("Did not get InvalidConfigurationException as expected.", false);

   }

   /**
    * This checks to makes sure if you add a handler with the same subsystem value, it will return the existing
    * one.
    *
    * @throws Throwable
    */
   public void testSameSubsystem() throws Throwable
   {
      MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer();

      String locator1 = "socket://localhost:5703";

      int id1 = 1;
      int id2 = 2;

      Connector connector1 = setupServer(locator1, mbeanServer);
      SampleInvocationHandler invocationHandler1 = new SampleInvocationHandler(id1);
      ServerInvocationHandler previous = connector1.addInvocationHandler("sub1", invocationHandler1);

      assertNull(previous);

      SampleInvocationHandler invocationHandler2 = new SampleInvocationHandler(id2);
      previous = connector1.addInvocationHandler("sub1", invocationHandler2);

      assertEquals(invocationHandler1, previous);

      ServerInvocationHandler[] handlers1 = connector1.getInvocationHandlers();

      assertEquals(1, handlers1.length);

      connector1.stop();
      connector1.destroy();

   }

   /**
    * Disabling this test.  
    * 
    * org.jboss.remoting.ServerInvoker is not written to support
    * the assertion "should always use the last handler added" made below.  
    * 
    * There are two choices:
    * 
    * 1. Implement the feature, or
    * 2. Eliminate the test.
    * 
    * Since there is no provision in the Remoting Guide that supports this feature, and
    * it doesn't seem particularly useful, we'll leave the ServerInvoker code alone
    * and eliminate the test.  - Ron Sigal, 4/29/08
    * 
    * If multiple handlers added to connector (thus server invoker) and subsystem is NOT
    * specified in client, then will be processed by last handler added.
    *
    * @throws Throwable
    */
   public void xtestNoSubsystem() throws Throwable
   {
      MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer();

      String locator1 = "socket://localhost:5704";
      String locator2 = "socket://localhost:5705";

      int id1 = 1;
      int id2 = 2;
      int id3 = 3;
      int id4 = 4;

      Connector connector1 = setupServer(locator1, mbeanServer);
      SampleInvocationHandler invocationHandler1 = new SampleInvocationHandler(id1);
      connector1.addInvocationHandler("sub1", invocationHandler1);

      SampleInvocationHandler invocationHandler2 = new SampleInvocationHandler(id2);
      connector1.addInvocationHandler("sub2", invocationHandler2);
      SampleInvocationHandler invocationHandler3 = new SampleInvocationHandler(id3);
      connector1.addInvocationHandler("sub3", invocationHandler3);

      Connector connector2 = setupServer(locator2, mbeanServer);
      SampleInvocationHandler invocationHandler4 = new SampleInvocationHandler(id4);
      connector2.addInvocationHandler("sub4", invocationHandler4);

      Client client = new Client(new InvokerLocator(locator1));
      Client client2 = new Client(new InvokerLocator(locator2));
      client.connect();
      client2.connect();

      Object ret1 = client.invoke("Do something");
      Object ret2 = client2.invoke("Do something");

      // should always use the last handler added
      assertEquals("" + id3, ret1);
      assertEquals("" + id4, ret2);

      connector1.stop();
      connector1.destroy();
      connector2.stop();
      connector2.destroy();

   }

   public Connector setupServer(String locatorURI, MBeanServer mbeanServer) throws Exception
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      Connector connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      int randomInt = idCounter++;
      ObjectName obj = new ObjectName("jboss.remoting:type=Connector,transport=" + locator.getProtocol() + ",id=" + randomInt);
      mbeanServer.registerMBean(connector, obj);
      connector.start();

      return connector;
   }

   /**
    * Simple invocation handler implementation.
    */
   public static class SampleInvocationHandler implements ServerInvocationHandler
   {
      private int id = 0;

      public SampleInvocationHandler(int id)
      {
         this.id = id;
      }

      public int getId()
      {
         return id;
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
         // Print out the invocation request
         System.out.println("Invocation request is: " + invocation.getParameter());

         // Just going to return static string as this is just simple example code.
         return "" + id;
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