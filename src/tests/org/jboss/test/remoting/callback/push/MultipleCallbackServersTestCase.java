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

package org.jboss.test.remoting.callback.push;

import junit.framework.TestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 1036 $</tt>
 *          <p/>
 *          $Id: MultipleCallbackServersTestCase.java 1036 2006-05-21 04:47:32Z telrod $
 */
public class MultipleCallbackServersTestCase extends TestCase
{
   // Constants -----------------------------------------------------

   private static final Logger log = Logger.getLogger(MultipleCallbackServersTestCase.class);

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   // Constructors --------------------------------------------------

   public MultipleCallbackServersTestCase(String name)
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

   public void testIfDisconnectClearsInvokerRegistry() throws Throwable
   {

      String serverlocatorURI = "socket://localhost:5555";

      Connector server = new Connector();
      ServerInvocationHandlerImpl serverHandler = new ServerInvocationHandlerImpl();
      server.setInvokerLocator(serverlocatorURI);
      server.start();
      server.addInvocationHandler("TEST", serverHandler);

      String callbackServer1URI = "socket://localhost:1111";
      Connector callbackServer1 = new Connector();
      callbackServer1.setInvokerLocator(callbackServer1URI);
      callbackServer1.start();

      String callbackServer2URI = "socket://localhost:2222";
      Connector callbackServer2 = new Connector();
      callbackServer2.setInvokerLocator(callbackServer2URI);
      callbackServer2.start();

      Client client = new Client(new InvokerLocator(serverlocatorURI), "TEST");
      client.connect();

      // add a listener that uses callbackServer1
      InvokerCallbackHandlerImpl listener1 = new InvokerCallbackHandlerImpl("ONE");
      client.addListener(listener1, new InvokerLocator(callbackServer1URI));

      assertEquals(1, serverHandler.size());

      // add a listener that uses callbackServer2
      InvokerCallbackHandlerImpl listener2 = new InvokerCallbackHandlerImpl("TWO");
      client.addListener(listener2, new InvokerLocator(callbackServer2URI));

      assertEquals(2, serverHandler.size());

      // remove them

      /**
       * Note, if uncomment the following, the test will fail.
       * This is because each Client has its own session id which
       * is used as part of the key for each callback listener regsitered
       * on the server.  Therefore a new Client, means the callback listener
       * key used for the remove will be different than the original one.
       */
      // client = new Client(new InvokerLocator(serverlocatorURI), "TEST");

      client.removeListener(listener1);

      assertEquals(1, serverHandler.size());

      try
      {
         client.removeListener(listener1);
         assertTrue("Expected to get exception for removing same listener twice.", false);
      }
      catch(Throwable thr)
      {
         assertTrue("Expected to get exception for removing same listener twice.", true);
      }

      client.removeListener(listener2);

      assertEquals(0, serverHandler.size());

      try
      {
         client.removeListener(listener2);
         assertTrue("Excpected to get exception from removing second listener twice.", false);
      }
      catch(Throwable thr)
      {
         assertTrue("Expected to get exception from removing second listener twice.", true);
      }

   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

   private class InvokerCallbackHandlerImpl implements InvokerCallbackHandler
   {
      private String name;

      public InvokerCallbackHandlerImpl(String name)
      {
         this.name = name;
      }

      public void handleCallback(Callback c)
      {

      }

      public String getName()
      {
         return name;
      }
   }

   private class ServerInvocationHandlerImpl implements ServerInvocationHandler
   {
      private List listeners = Collections.synchronizedList(new ArrayList());

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
         listeners.add(callbackHandler);
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         log.info("Trying to remove listener: " + callbackHandler);
         boolean removed = listeners.remove(callbackHandler);
         if(removed)
         {
            log.info("Listener removed");
         }
         else
         {
            log.info("Listener NOT removed");
         }

      }

      public int size()
      {
         return listeners.size();
      }
   }

}
