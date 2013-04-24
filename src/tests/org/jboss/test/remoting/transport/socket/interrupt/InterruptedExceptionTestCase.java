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
package org.jboss.test.remoting.transport.socket.interrupt;

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
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;


/**
 * Unit test for JBREM-955.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Apr 25, 2008
 * </p>
 */
public class InterruptedExceptionTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(InterruptedExceptionTestCase.class);
   
   private static boolean firstTime = true;
   private static String FAST = "fast";
   
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
   
   
   public void testInterruptedException() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(MicroSocketClientInvoker.MAX_POOL_SIZE_FLAG, "1");
      clientConfig.put("numberOfCallRetries", "1");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals(FAST, client.invoke(FAST));
      log.info("connection is good");
      
      InvokerThread t1 = new InvokerThread(client, "abc");
      InvokerThread t2 = new InvokerThread(client, "xyz");
      
      // Start first invocation.
      t1.start();
      log.info("started first invocation");
      
      // Give first invocation time to start.
      Thread.sleep(5000);
      
      // Start second invocation.
      t2.start();
      log.info("started second invocation");
      
      // Give second invocation time to start.
      Thread.sleep(5000);
      
      // Interrupt second invocation as it waits for a semaphore.
      t2.interrupt();
      log.info("interrupted second invocation");

      // Wait until second invocation throws an exception.
      synchronized (t2)
      {
         t2.wait(10000);
      }
      
      // Verify exception is an InterruptedException wrapped in a RuntimeExceptio.
      Throwable t = t2.throwable;
      log.info("throwable: " + t);
      assertTrue(t instanceof RuntimeException);
      assertFalse(t instanceof CannotConnectException);
      assertTrue(t.getCause() instanceof InterruptedException);
      
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
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
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
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         Object o = invocation.getParameter();
         if (!FAST.equals(o))
         {
            Thread.sleep(20000);
         }
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class InvokerThread extends Thread
   {
      static int counter;
      Client client;
      String message;
      Throwable throwable;
      
      public InvokerThread(Client client, String message)
      {
         this.client = client;
         this.message = message;
         setName("InvokerThread:" + counter++);
      }
      
      public void run()
      {
         try
         {
            log.info("invocation succeeded: " + client.invoke(message));
         }
         catch (Throwable t)
         {
            throwable = t;
            log.error("invocation error: " + t.getMessage());
         }
         
         synchronized (this)
         {
            notify();
         }
      }
   }
}