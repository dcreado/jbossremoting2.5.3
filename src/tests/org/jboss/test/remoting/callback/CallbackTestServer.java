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

package org.jboss.test.remoting.callback;

import org.apache.log4j.Level;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Simple remoting server.  Uses inner class SampleInvocationHandler
 * as the invocation target handler class.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class CallbackTestServer extends ServerTestCase
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5500;

   protected String locatorURI = transport + "://" + host + ":" + port;
   protected Connector connector;

   // String to be returned from invocation handler upon client invocation calls.
   public static final String RESPONSE_VALUE = "This is the return to SampleInvocationHandler invocation";
   public static final String CALLBACK_VALUE = "This is the payload of callback invocation.";


   public void setupServer() throws Exception
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.start();

      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      // first parameter is sub-system name.  can be any String value.
      connector.addInvocationHandler("sample", invocationHandler);
   }

   protected void setUp() throws Exception
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.FATAL);

      setupServer();
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
      CallbackTestServer server = new CallbackTestServer();
      try
      {
         server.setUp();

         Thread.sleep(600000);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   /**
    * Simple invocation handler implementation.
    */
   public static class SampleInvocationHandler implements ServerInvocationHandler
   {

      protected List listeners = new ArrayList();


      /**
       * called to handle a specific invocation
       *
       * @param invocation
       * @return
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         // Just going to return static string as this is just simple example code.

         // Will also fire callback to listeners if they were to exist using
         // simple invocation request.
         Callback callback = new Callback(CALLBACK_VALUE);
         Iterator itr = listeners.iterator();
         while(itr.hasNext())
         {
            InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler) itr.next();
            callbackHandler.handleCallback(callback);
         }

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
         listeners.add(callbackHandler);
      }

      /**
       * Removes the callback handler that was listening for callbacks
       * from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         listeners.remove(callbackHandler);
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