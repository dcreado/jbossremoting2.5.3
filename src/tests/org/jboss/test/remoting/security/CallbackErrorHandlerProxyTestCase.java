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
package org.jboss.test.remoting.security;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit test for JBREM-977.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 4, 2008
 * </p>
 */
public class CallbackErrorHandlerProxyTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(CallbackErrorHandlerProxyTestCase.class);
   
   private static boolean firstTime = true;
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestCallbackErrorHandler errorHandler;
   protected MBeanServer server;
   protected ObjectName errorHandlerObjectName;

   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }
   }

   
   public void tearDown()
   {
   }
   
   
   public void testCallbackErrorHandlerProxy() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      log.info("ServerInvocationHandler: " + errorHandler);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Check connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Verify callbacks work.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, null, null, true);
      assertEquals(1, callbackHandler.counter);
      
      // Verify CallbackErrorHandler proxy gets used.
      client.addListener(callbackHandler, null, null, true);
      assertEquals(1, callbackHandler.counter);
      assertEquals(1, errorHandler.counter);
      int counter = ((Integer) server.getAttribute(errorHandlerObjectName, "Counter")).intValue();
      assertEquals(errorHandler.counter, counter);

      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
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
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      
      // Create CallbackErrorHandler.
      server = MBeanServerFactory.createMBeanServer();
      errorHandler = new TestCallbackErrorHandler();
      String objectNameString = "test:type=TestCallbackErrorHandler";
      errorHandlerObjectName = new ObjectName(objectNameString);
      server.registerMBean(errorHandler, errorHandlerObjectName);
      config.put(ServerInvokerCallbackHandler.CALLBACK_ERROR_HANDLER_KEY, objectNameString);
      
      // Create Connector..
      connector = new Connector(serverLocator, config);
      server.registerMBean(connector, new ObjectName("test:type=Connector"));
      connector.create();
      connector.addInvocationHandler("test", new TestServerInvocationHandler());
      connector.start();
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
   
   
   public static class TestServerInvocationHandler implements ServerInvocationHandler
   {
      static Logger log = Logger.getLogger(TestServerInvocationHandler.class);
      private int counter;
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         try
         {
            if (counter++ == 0)
            {
               // First time, send callback.
               callbackHandler.handleCallback(new Callback("callback"));
            }
            else
            {
               // Next, generate callback exception.
               callbackHandler.handleCallback(new Callback(new NotSerializable()));
            }
         }
         catch (HandleCallbackException e)
         {
            if (counter == 0)
               log.error("Unexpected exception", e);
            else
               log.info("Expected exception: " + e.getMessage());
         }
      }

      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
      
      public int getCounter()
      {
         return counter;
      }
   }
   
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      int counter;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         counter++;
         log.info("received callback");
      }  
   }
   
   static class NotSerializable {}
}