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
package org.jboss.test.remoting.transport.http.retry;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.http.HTTPClientInvoker;


/**
 * Unit test for JBREM-979.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 10, 2008
 * </p>
 */
public class ConnectionRetryTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ConnectionRetryTestCase.class);
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;

   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.DEBUG);
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
   
   
   public void testDefaultRetries() throws Throwable
   {
      log.info("entering " + getName());
      
      if (System.getProperty("java.version").indexOf("1.4") >= 0)
      {
         log.info("retries not supported for jdk 1.4");
         return;
      }
      
      // Start server.
      TestInvocationHandler invocationHandler = new TestInvocationHandler(2, 2);
      setupServer(invocationHandler);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(ServerInvoker.TIMEOUT, "4000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // This call should time out.
      boolean success = false;
      try
      {
         client.invoke("xyz");
      }
      catch (CannotConnectException e)
      {
         log.info("got expected exception");
         success = true;
      }
      
      assertTrue("invocation should have thrown exception", success);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testOneRetry() throws Throwable
   {
      log.info("entering " + getName());
      
      if (System.getProperty("java.version").indexOf("1.4") >= 0)
      {
         log.info("retries not supported for jdk 1.4");
         return;
      }
      
      // Start server.
      TestInvocationHandler invocationHandler = new TestInvocationHandler(2, 2);
      setupServer(invocationHandler);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(HTTPClientInvoker.NUMBER_OF_CALL_ATTEMPTS, "1");
      clientConfig.put(ServerInvoker.TIMEOUT, "4000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // This call should time out.
      boolean success = false;
      try
      {
         client.invoke("xyz");
      }
      catch (CannotConnectException e)
      {
         log.info("got expected exception");
         success = true;
      }
      assertTrue("invocation should have thrown exception", success);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testTwoRetries() throws Throwable
   {
      log.info("entering " + getName());
      
      if (System.getProperty("java.version").indexOf("1.4") >= 0)
      {
         log.info("retries not supported for jdk 1.4");
         return;
      }
      
      // Start server.
      TestInvocationHandler invocationHandler = new TestInvocationHandler(2, 2);
      setupServer(invocationHandler);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(HTTPClientInvoker.NUMBER_OF_CALL_ATTEMPTS, "2");
      clientConfig.put(ServerInvoker.TIMEOUT, "4000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // This call should succeed on the second attempt.
      log.info("making second invocation");
      assertEquals("xyz", client.invoke("xyz"));
      log.info("second invocation succeeded as expected");

      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }

   
   public void testFiveRetriesAndFail() throws Throwable
   {
      log.info("entering " + getName());
      
      if (System.getProperty("java.version").indexOf("1.4") >= 0)
      {
         log.info("retries not supported for jdk 1.4");
         return;
      }
      
      // Start server.
      TestInvocationHandler invocationHandler = new TestInvocationHandler(2, 6);
      setupServer(invocationHandler);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(HTTPClientInvoker.NUMBER_OF_CALL_ATTEMPTS, "5");
      clientConfig.put(ServerInvoker.TIMEOUT, "4000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // This call should time out once and succeed on the second attempt.
      log.info("making second invocation");
      boolean success = false;
      try
      {
         client.invoke("xyz");
      }
      catch (CannotConnectException e)
      {
         log.info("got expected exception");
         success = true;
      }
      assertTrue("invocation should have thrown exception", success);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testFiveRetriesThenSucceed() throws Throwable
   {
      log.info("entering " + getName());
      
      if (System.getProperty("java.version").indexOf("1.4") >= 0)
      {
         log.info("retries not supported for jdk 1.4");
         return;
      }
      
      // Start server.
      TestInvocationHandler invocationHandler = new TestInvocationHandler(2, 5);
      setupServer(invocationHandler);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(HTTPClientInvoker.NUMBER_OF_CALL_ATTEMPTS, "5");
      clientConfig.put(ServerInvoker.TIMEOUT, "4000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // This call should time out once and succeed on the second attempt.
      log.info("making second invocation");
      long start = System.currentTimeMillis();
      assertEquals("xyz", client.invoke("xyz"));
      log.info("second invocation succeeded");
      assertTrue(System.currentTimeMillis() - start >= 4 * 4000);

      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "http";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer(ServerInvocationHandler invocationHandler) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      int minDelayAttempt;
      int maxDelayAttempt;
      int counter;
      
      public TestInvocationHandler(int minDelayAttempt, int maxDelayAttempt)
      {
         this.minDelayAttempt = minDelayAttempt;
         this.maxDelayAttempt = maxDelayAttempt;
      }
      
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         counter++;
         if (minDelayAttempt <= counter && counter <= maxDelayAttempt)
         {
            log.info("going to sleep, counter = " + counter);
            Thread.sleep(8000);
            log.info("waking up");
         }
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("received callback");
      }  
   }
}