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
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;
import java.util.Random;

/**
 * Tests a push callback in the situation when the client, target server and callback server are in
 * the same VM. No need for DistributedTestCase.
 *
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 1036 $</tt>
 */
public class InVMPushCallbackTestCase extends TestCase
{
   // Constants -----------------------------------------------------

   // Static --------------------------------------------------------

   // Attributes ----------------------------------------------------

   protected InvokerLocator targetServerLocator;
   protected InvokerLocator callbackServerLocator;

   protected Connector targetServerConnector;
   protected Connector callbackServerConnector;

   protected ServerInvocationHandlerImpl targetServerInvocationHandler;

   protected Client client;
   protected InvokerCallbackHandlerImpl callbackHandler;

   // Constructors --------------------------------------------------

   public InVMPushCallbackTestCase(String name)
   {
      super(name);
   }

   // TestCase override ---------------------------------------------

   public void setUp() throws Exception
   {
      super.setUp();

      targetServerLocator = new InvokerLocator("socket://localhost:2323");
      callbackServerLocator = new InvokerLocator("socket://localhost:3434");

      targetServerConnector = new Connector();
      targetServerConnector.setInvokerLocator(targetServerLocator.getLocatorURI());
      targetServerConnector.start();
      targetServerInvocationHandler = new ServerInvocationHandlerImpl();
      targetServerConnector.addInvocationHandler("TARGET", targetServerInvocationHandler);

      callbackServerConnector = new Connector();
      callbackServerConnector.setInvokerLocator(callbackServerLocator.getLocatorURI());
      callbackServerConnector.start();
      callbackServerConnector.addInvocationHandler("IRRELEVANT", new ServerInvocationHandlerImpl());

      client = new Client(targetServerLocator);
      client.connect();
      callbackHandler = new InvokerCallbackHandlerImpl();
      try
      {
         client.addListener(callbackHandler, callbackServerLocator);
      }
      catch(Throwable t)
      {
         throw new Exception(t);
      }
      client.connect();
   }

   public void tearDown() throws Exception
   {
      callbackServerConnector.stop();
      targetServerConnector.stop();
      client.disconnect();

      super.tearDown();
   }

   // Public --------------------------------------------------------


   public void testPushCallback() throws Exception
   {
      // send callback, the callback handler must receive it
      Long arg = new Long(new Random().nextLong());
      targetServerInvocationHandler.sendCallback(arg);

      assertEquals(arg, callbackHandler.getReceivedArgument());
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

   private class ServerInvocationHandlerImpl implements ServerInvocationHandler
   {

      private MBeanServer server;
      private ServerInvoker invoker;
      private InvokerCallbackHandler theOnlyHandler;

      // ServerInocationHandler implementation ---------------------

      public void setMBeanServer(MBeanServer server)
      {
         this.server = server;
      }

      public void setInvoker(ServerInvoker invoker)
      {
         this.invoker = invoker;
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return null;
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         theOnlyHandler = callbackHandler;
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         // noop
      }

      // Public ---------------------------------------------------

      public void sendCallback(Object arg) throws Exception
      {
         Callback callback = new Callback(arg);
         theOnlyHandler.handleCallback(callback);
      }

   }

   private class InvokerCallbackHandlerImpl implements InvokerCallbackHandler
   {

      Object receivedArg;

      // InvokerCallbackHandler implementation ---------------------

      public synchronized void handleCallback(Callback callback)
            throws HandleCallbackException
      {
         receivedArg = callback.getParameter();
      }

      // Public ----------------------------------------------------

      public synchronized Object getReceivedArgument()
      {
         return receivedArg;
      }
   }

}
