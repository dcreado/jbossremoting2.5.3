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
package org.jboss.remoting.samples.bisocket;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;

import javax.management.MBeanServer;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;


/**
 * This class and org.jboss.remoting.samples.bisocket.BisocketSampleClient
 * demonstrate how to how to make an invocation and how to set up push callbacks
 * over the bisocket transport.
 * 
 * The reason for the existance of the bisocket transport, which is derived from the
 * socket transport, is to make it possible to do push callbacks to a client which 
 * is unable to create a ServerSocket, either due to security restrictions or due
 * to the fact that it is behind a firewall. 
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 1, 2008
 * </p>
 */
public class BisocketSampleServer
{
   public static int port = 4567;
   
   private static Logger log = Logger.getLogger(BisocketSampleServer.class);

   protected Connector connector;

   
   protected void setupServer(String locatorURI) throws Exception
   {
      // Create the InvokerLocator based on url string format
      // to indicate the transport, host, and port to use for the
      // server invoker.
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      
      connector = new Connector(serverLocator);
      
      // Creates all the connector's needed resources, such as the server invoker.
      connector.create();
      
      // Create the handler to receive the invocation request from the client for processing.
      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      
      // Start server.
      connector.start();
      log.info("Started remoting server with locator uri of: " + locatorURI);
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
      {
         connector.stop();
         log.info("shut down server");
      }
   }
   
   
   public static void main(String[] args)
   {
      try
      {
         // Configure logging.
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
         
         // Create and start server.
         String host = InetAddress.getLocalHost().getHostAddress();
         String locatorURI = "bisocket://" + host + ":" + port;
         BisocketSampleServer server = new BisocketSampleServer();
         server.setupServer(locatorURI);

         // Wait until user types a character on command line.
         System.in.read();
         
         // Shut down server.
         server.shutdownServer();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

   
   /**
    * Invocations are handled by the ServerInvocationHandler.
    */
   static class SampleInvocationHandler implements ServerInvocationHandler
   {
      private HashSet callbackHandlers = new HashSet();
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         callbackHandlers.add(callbackHandler);
      }
      
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         Object param =  invocation.getParameter();
         if ("CALLBACK".equals(param))
         {
            Iterator it = callbackHandlers.iterator();
            while (it.hasNext())
            {
               InvokerCallbackHandler handler = (InvokerCallbackHandler) it.next();
               handler.handleCallback(new Callback("callback"));
            }
         }
         return param;
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         callbackHandlers.remove(callbackHandler);
      }
      
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
}