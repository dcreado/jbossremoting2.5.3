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

package org.jboss.test.remoting.lease;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.w3c.dom.Document;

import javax.management.MBeanServer;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public abstract class LeaseTestServer extends ServerTestCase implements ConnectionListener
{
   // Default locator values
   private static String host = "localhost";
   private static int port = 5400;

   private boolean error = false;
   private boolean disconnect = false;

   private Connector connector = null;

   public boolean isRunning = true;

   // String to be returned from invocation handler upon client invocation calls.
   private static final String RESPONSE_VALUE = "This is the return to SampleInvocationHandler invocation";

   protected abstract String getTransport();

   public void setupServer() throws Exception
   {
      connector = new Connector();
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("<invoker transport=\"" + getTransport() + "\">");
      buf.append("<attribute name=\"clientLeasePeriod\">3000</attribute>");
      buf.append("<attribute name=\"serverBindAddress\">" + host + "</attribute>");
      buf.append("<attribute name=\"serverBindPort\">" + port + "</attribute>");
      buf.append("</invoker>");
      buf.append("<handlers>");
      buf.append("  <handler subsystem=\"mock\">" + SampleInvocationHandler.class.getName() + "</handler>\n");
      buf.append("</handlers>");
      buf.append("</config>");
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(buf.toString().getBytes()));
      //connector.setInvokerLocator(locator.getLocatorURI());
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      connector.addConnectionListener(this);
      connector.start();

   }

   public void testForError()
   {
      while(isRunning)
      {
         assertTrue("Received connection failure from client that was not due to disconnect.", !error);
         try
         {
            Thread.currentThread().sleep(1000);
         }
         catch(InterruptedException e)
         {
            e.printStackTrace();
         }
      }
   }

   public void setUp() throws Exception
   {
      setupServer();
   }

   public void tearDown() throws Exception
   {
      isRunning = false;
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
      if(!disconnect)
      {
         throw new RuntimeException("Never got disconnect notification from any of the clients.");
      }
   }

   public void handleConnectionException(Throwable throwable, Client client)
   {
      System.out.println("received connection exception from " + client + " (session id = " + client.getSessionId() + ") " +
                         "with exception of " + throwable + " and configuration of " + client.getConfiguration());
      if(throwable == null)
      {
         // since there was not an exception (will be one if was from a disconnect),
         // need to indicate error.
         error = true;
      }
      else
      {
         disconnect = true;
      }
   }

   /**
    * Simple invocation handler implementation.
    */
   public static class SampleInvocationHandler implements ServerInvocationHandler
   {
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
         return RESPONSE_VALUE;
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