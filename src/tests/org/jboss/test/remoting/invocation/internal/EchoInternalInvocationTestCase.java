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
package org.jboss.test.remoting.invocation.internal;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.InternalInvocation;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Tests ECHO internal invocation facility.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2515 $
 * <p>
 * Copyright Jun 25, 2007
 * </p>
 */
public class EchoInternalInvocationTestCase extends TestCase
{
   public static int port;
   
   private static Logger log = Logger.getLogger(EchoInternalInvocationTestCase.class);
   private static boolean firstTime = true;
   
   // remoting server connector
   private Connector connector;
   private InvokerLocator serverLocator;
   private TestInvocationHandler invocationHandler;

   
   /**
    * Sets up target remoting server.
    */
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
   }

   
   public void tearDown()
   {
   }
   
   
   /**
    * Sends nothing to echo.
    */
   public void testEchoNull() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      InternalInvocation invocation = new InternalInvocation(InternalInvocation.ECHO, null);
      Object response = client.invoke(invocation);
      assertNull(response);
      
      client.disconnect();
      connector.stop();
   }
   
   
   /**
    * Sends an array of one Object to echo.
    */
   public void testEchoNonNull() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      Object[] params = new Object[1];
      params[0] = "abc";
      InternalInvocation invocation = new InternalInvocation(InternalInvocation.ECHO, params);
      Object response = client.invoke(invocation);
      assertEquals("abc", response);
      
      client.disconnect();
      connector.stop();
   }
   
   
   /**
    * Sends an array of two Objects, the first of which should be echoed.
    */
   public void testEchoMultipleNonNull() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      Object[] params = new Object[2];
      params[0] = "def";
      params[1] = "xyz";
      InternalInvocation invocation = new InternalInvocation(InternalInvocation.ECHO, params);
      Object response = client.invoke(invocation);
      assertEquals("def", response);
      
      client.disconnect();
      connector.stop();
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable {return null;}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   static class TestInvokerCallbackHandler implements InvokerCallbackHandler
   {
      public void handleCallback(Callback callback) throws HandleCallbackException {}
   }
}