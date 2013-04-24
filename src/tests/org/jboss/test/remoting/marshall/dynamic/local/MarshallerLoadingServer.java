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

package org.jboss.test.remoting.marshall.dynamic.local;

import java.io.IOException;
import java.util.Enumeration;
import javax.management.MBeanServer;
import org.apache.log4j.Level;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

/**
 * Simple remoting server.  Uses inner class SampleInvocationHandler
 * as the invocation target handler class.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class MarshallerLoadingServer extends ServerTestCase implements MarshallerLoadingConstants
{
   private Connector connector;

   private static final Logger log = Logger.getLogger(MarshallerLoadingServer.class);

   // String to be returned from invocation handler upon client invocation calls.
   private static final String RESPONSE_VALUE = "This is the return to SampleInvocationHandler invocation";


   public void setupServer() throws Exception
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      Connector connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.start();

      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      // first parameter is sub-system name.  can be any String value.
      connector.addInvocationHandler("sample", invocationHandler);
   }

   public void setUp() throws Exception
   {
      setupServer();
   }

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public void setLogging()
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("org.jboss.remoting.marshall.dynamic.local").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("org.jboss.dtf").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.FATAL);

      org.apache.log4j.SimpleLayout layout = new org.apache.log4j.SimpleLayout();
      try
      {
         org.apache.log4j.FileAppender fileAppender = new org.apache.log4j.FileAppender(layout, getClass().getName() + "_output.log");
         fileAppender.setThreshold(Level.DEBUG);
         org.apache.log4j.Category.getRoot().addAppender(fileAppender);
      }
      catch(IOException e)
      {
         e.printStackTrace();
      }

//      org.apache.log4j.ConsoleAppender consoleAppender = new org.apache.log4j.ConsoleAppender();
//      consoleAppender.setThreshold(Level.INFO);
//      org.apache.log4j.Category.getRoot().addAppender(consoleAppender);

      //System.out.println("Root log level = " + org.apache.log4j.Category.getRoot().getLevel());
      Enumeration appenders = org.apache.log4j.Category.getRoot().getAllAppenders();
      while(appenders.hasMoreElements())
      {
         org.apache.log4j.Appender appender = (org.apache.log4j.Appender) appenders.nextElement();
         //System.out.println(appender.getName());
         if(appender instanceof org.apache.log4j.ConsoleAppender)
         {
            ((org.apache.log4j.ConsoleAppender) appender).setThreshold(Level.INFO);
         }
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