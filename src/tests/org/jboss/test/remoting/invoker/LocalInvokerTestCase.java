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

package org.jboss.test.remoting.invoker;

import java.net.InetAddress;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.local.LocalClientInvoker;

import javax.management.MBeanServer;

/**
 * Provides a unit test to assure that when an <code>org.jboss.remoting.ServerInvoker</code>
 * is deleted from <code>org.jboss.remoting.InvokerRegistry</code>'s tables, any
 * <code>org.jboss.remoting.transport.local.LocalClientInvoker</code> that
 * refers to it is also deleted. (See issue JBREM-153.)
 * <p/>
 * Also provides a unit test to assure that any <code>LocalClientInvoker</code> still accessible
 * after the purging of its <code>ServerInvoker</code> is disconnected and, therefore, unusable.
 *
 * @author ron sigal - mailto:r.sigal@computer.org
 */
public class LocalInvokerTestCase extends TestCase
{
   private static final Logger log = Logger.getLogger(LocalInvokerTestCase.class);
   private static final int MAX_TRIES = 10;


   /**
    * Tests that a newly created <code>org.jboss.remoting.Client</code>
    * does not get an expired <code>org.jboss.remoting.transport.local.LocalClientInvoker</code> from
    * <code>org.jboss.remoting.InvokerRegistry</code>.
    * <p/>
    * It does the following steps:
    * <p/>
    * <ol>
    * <li>creates an <code>org.jboss.remoting.transport.Connector</code> for socket://127.0.0.1:9090;
    * <li>registers an <code>org.jboss.remoting.ServerInvocationHandler</code> that holds secret value 3;
    * <li>creates an <code>org.jboss.remoting.Client</code> for socket://127.0.0.1:9090;
    * <li>verifies that <code>org.jboss.remoting.InvokerRegistry</code> created an
    * <code>org.jboss.remoting.transport.local.LocalClientInvoker</code>  on behalf of the <code>Client</code>;
    * <li>retrieves the secret value from the <code>ServerInvocationHandler</code>;
    * <li>stops the <code>Connector</code>;
    * <li>creates a new <code>Connector</code> for socket://127.0.0.1:9090;
    * <li>registers a new <cod>ServerInvocationHandler</code> that holds a secret value of 7;
    * <li>creates a new <code>Client</code> for socket://127.0.0.1:9090;
    * <li>verifies that <code>org.jboss.remoting.InvokerRegistry</code> created a
    * <code>LocalClientInvoker</code>  on behalf of the new <code>Client</code>;
    * <li>retrieves the secret value from the new <code>ServerInvocationHandler</code> and verifies that it is 7.
    * </ol>
    * <p/>
    * If the second retrieved secret value is 7, then the first <code>LocalClientInvoker</code>, which would have
    * invoked the old <code>ServerInvocationHandler</code>, is no longer accessible and must have been disposed of
    * by <code>InvokerRegistry</code>.
    * <p/>
    * <b>Note.</b> A call to <code>Client.stop()</code> before the creation of the second <code>Client</code>
    * woud assure the purging of the old <code>LocalClientInvoker</code>.  The call is omitted in this test to
    * assure that the purging of the old <code>LocalClientInvoker</code> happens automatically.
    */
   public void testLocalInvokerTwoClients()
   {
      InvokerLocator invokerLocator = null;

      try
      {
         String host = InetAddress.getLocalHost().getHostAddress();
         int port = PortUtil.findFreePort(host);
         invokerLocator = new InvokerLocator("socket://" + host + ":" + port);

         Integer secret = new Integer(3);
         runTestForTwoClients(invokerLocator, secret);

         secret = new Integer(7);
         Integer newSecret = runTestForTwoClients(invokerLocator, secret);
         log.debug("newSecret = " + newSecret);

         assertTrue(secret.equals(newSecret));
      }
      catch(Throwable t)
      {
         log.debug(t);
         t.printStackTrace();
         fail();
      }
   }


   /**
    * Tests that when an <code>org.jboss.remoting.Client</code>
    * with an expired <code>org.jboss.remoting.transport.local.LocalClientInvoker</code> executes
    * <code>invoke()</code>, it gets a new <code>LocalClientInvoker</code> instead of using the old one.
    * <p/>
    * It does the following steps:
    * <p/>
    * <ol>
    * <li>creates an <code>org.jboss.remoting.transport.Connector</code> for socket://127.0.0.1:9090;
    * <li>registers an <code>org.jboss.remoting.ServerInvocationHandler</code> that holds secret value 3;
    * <li>creates an <code>org.jboss.remoting.Client</code> for socket://127.0.0.1:9090;
    * <li>verifies that <code>org.jboss.remoting.InvokerRegistry</code> created an
    * <code>org.jboss.remoting.transport.local.LocalClientInvoker</code>  on behalf of the <code>Client</code>;
    * <li>retrieves the secret value from the <code>ServerInvocationHandler</code>;
    * <li>stops the <code>Connector</code>
    * <li>creates a new <code>Connector</code> for socket://127.0.0.1:9090;
    * <li>registers a new <cod>ServerInvocationHandler</code> that holds a secret value of 7;
    * <li>retrieves the secret value from the new <code>ServerInvocationHandler</code> and verifies that it is 7.
    * </ol>
    * <p/>
    * If the second retrieved secret value is 7, then the old <code>LocalClientInvoker</code>, which would have
    * invoked the old <code>ServerInvocationHandler</code>, must have been replaced with a new one.
    */
   public void testLocalInvokerOneClient()
   {
      InvokerLocator invokerLocator = null;
      Connector connector = null;
      Client client = null;


      try
      {
         String host = InetAddress.getLocalHost().getHostAddress();
         int port = PortUtil.findFreePort(host);
         invokerLocator = new InvokerLocator("socket://" + host + ":" + port);

         Integer secret = new Integer(3);
         connector = makeConnector(invokerLocator, secret);
         client = new Client(invokerLocator);
         client.connect();

         // verify that InvokerRegistry has created a LocalClientInvoker for client
         assertTrue(client.getInvoker() instanceof LocalClientInvoker);

         Integer newSecret = (Integer) client.invoke(null);
         connector.stop();

         secret = new Integer(7);
         connector = makeConnector(invokerLocator, secret);

         newSecret = (Integer) client.invoke(null);
         log.debug("newSecret = " + newSecret);
         assertTrue(secret.equals(newSecret));

         connector.stop();

         try
         {
            client.invoke(null);
            assertTrue("Should have thrown ServerInvoker.InvalidStateException.", false);
         }
         catch (ServerInvoker.InvalidStateException invalidStateEx)
         {
            assertTrue(true);
         }

      }
      catch(Throwable t)
      {
         log.debug(t);
         t.printStackTrace();
         fail();
      }
      finally
      {
         if (client != null) client.disconnect();
         if (connector != null) connector.stop();
      }
   }


   /**
    * Subroutine for <code>testLocalInvokerTwoClients()</code>.
    * <p/>
    * Does the following:
    * <p/>
    * <ol>
    * <li>creates a <code>Connector</code> according to parameter <code>invokerLocator</code>
    * <li>registers a <code>ServerInvocationHandler</code> that holds secret value given by parameter <code>secret</code>;
    * <li>creates a <code>org.jboss.remoting.Client</code> according to parameter <code>invokerLocator</code>
    * <li>verifies that <code>InvokerRegistry</code> created a
    * <code>LocalClientInvoker</code>  on behalf of the <code>Client</code>
    * <li>retrieves the secret value from the <code>ServerInvocationHandler</code>
    * <li>stops the <code>Connector</code>
    * </ol>
    *
    * @param invokerLocator URI for both <code>Client</code> and <code>Connector</code>
    * @param secret         secret value for <code>ServerInvocationHandler</code>
    * @return secret value retrieved from <code>ServerInvocationHandler</code>
    */
   protected Integer runTestForTwoClients(InvokerLocator invokerLocator, Integer secret)
   {
      Connector connector = null;
      Client client = null;
      Integer newSecret = null;

      try
      {
         connector = makeConnector(invokerLocator, secret);
         client = new Client(invokerLocator);
         client.connect();

         // verify that InvokerRegistry has created a LocalClientInvoker for client
         assertTrue(client.getInvoker() instanceof LocalClientInvoker);

         newSecret = (Integer) client.invoke(null);
      }
      catch(Throwable t)
      {
         log.debug(t);
         t.printStackTrace();
         fail();
      }
      finally
      {
         connector.stop();
      }

      return newSecret;
   }


   /**
    * Sets up a <code>Connector</code> with a specified <code>ServerInvocationHandler</code>.
    * <p/>
    * Does the following:
    * <p/>
    * <ol>
    * <li>creates a <code>Connector</code> according to parameter <code>invokerLocator</code>
    * <li>registers a <code>ServerInvocationHandler</code> that holds secret value given by parameter <code>secret</code>.
    * </ol>
    *
    * @param invokerLocator <code>InvokerLocator</code> used by <code>Connector</code>
    * @param secret         value to be held by <code>ServerInvocationHandler</code>
    * @return newly created <code>Connector</code>
    */
   protected Connector makeConnector(InvokerLocator invokerLocator, Integer secret)
   {
      Connector connector = null;

      try
      {
         connector = new Connector();
         connector.setInvokerLocator(invokerLocator.getLocatorURI());

         for(int i = 0; i < MAX_TRIES; i++)
         {
            try
            {
               connector.start();
               break;
            }
            catch(Exception e)
            {
               log.info("error - will try again: " + e.getMessage());
               Thread.sleep(60000);
            }
         }

         SampleInvocationHandler invocationHandler = new SampleInvocationHandler(secret);
         connector.addInvocationHandler("xmlrmi", invocationHandler);
      }
      catch(Throwable t)
      {
         log.debug(t);
         t.printStackTrace();
         fail();
      }

      return connector;
   }


   /**
    * Simple invocation handler implementation.
    *
    * <code>invoke()</code> simply returns its secret value.
    */
   public class SampleInvocationHandler implements ServerInvocationHandler
   {
      private Object secret;

      public SampleInvocationHandler(Object secret)
      {
         this.secret = secret;
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
         log.debug("SampleInvocationHandler.invoke(): " + this.hashCode());
         return secret;
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