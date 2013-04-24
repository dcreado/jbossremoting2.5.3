/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.test.remoting.connection.deadlock;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.socket.SocketServerInvoker;


/**
 * Unit test for JBREM-1070.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Nov 29, 2008
 * </p>
 */
public class DeadlockTestServer extends ServerTestCase
{
   private static Logger log = Logger.getLogger(DeadlockTestServer.class);
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port = 7777;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected TestServerInvoker serverInvoker;
   protected TestInvocationHandler invocationHandler;
   
   
   public static void main(String[] args)
   {
      try
      {
         DeadlockTestServer server = new DeadlockTestServer();
         server.setUp();
         Thread.sleep(600000);
         server.shutdownServer();
      }
      catch (Throwable t)
      {
         log.error("error", t);
      }
   }

   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(Level.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }
      
      setupServer();
   }

   
   public void tearDown() throws Exception
   {
      shutdownServer();
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer() throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      locatorURI = getTransport() + "://" + host + ":" + port;
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "/?" + metadata;
      }
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      
      serverInvoker = new TestServerInvoker(serverLocator, config);
      serverInvoker.create();
      invocationHandler = new TestInvocationHandler();
      serverInvoker.addInvocationHandler("test", invocationHandler);
      serverInvoker.start();
      log.info("TestServerInvoker(" + locatorURI + ") started");
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (serverInvoker != null)
         serverInvoker.stop();
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   static class TestServerInvoker extends SocketServerInvoker
   {
      public TestServerInvoker(InvokerLocator locator, Map config)
      {
         super(locator, config);
         log.info("TestServerInvoker: " + locator);
      }
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         if ("$PING$".equals(invocation.getParameter()))
         {
            log.info("TestServerInvoker received $PING$");
            Thread.sleep(10000);
            throw new Exception("got $PING$");
         }
         else
         {
            return super.invoke(invocation);
         }
      }
   }

   static class TestConnectionListener implements ConnectionListener
   {
      public boolean ok;
      
      public void handleConnectionException(Throwable throwable, Client client)
      {
         ok = true;
         log.info("handleConnectionException() called");
      }
   }
}