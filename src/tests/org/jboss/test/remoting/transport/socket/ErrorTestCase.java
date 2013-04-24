/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.test.remoting.transport.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.LinkedList;
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
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.ClientFactory;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.ServerFactory;
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.ServerSocketWrapper;
import org.jboss.remoting.transport.socket.ServerThread;
import org.jboss.remoting.transport.socket.SocketClientInvoker;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import org.jboss.remoting.transport.socket.SocketWrapper;


/**
 * Unit tests for JBREM-1183.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Feb 16, 2010
 */
public class ErrorTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ErrorTestCase.class);
   
   private static boolean firstTime = true;
   private static int whenToFail;
   
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
         Logger.getLogger("org.jboss.remoting").setLevel(Level.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }

      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            InvokerRegistry.registerInvokerFactories(getTransport(), TestClientFactory.class, TestServerFactory.class);
            return null;
         }
      });
      
      TestServerThread.threadCounter = 0;
   }

   
   public void tearDown()
   {
   }
   
   
   public void testErrorFirstTime() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      whenToFail = 0;
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("timeout", "2000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test invocations.
      assertEquals("abc", client.invoke("abc"));
      log.info("first invocation succeeded");
      assertEquals("lmn", client.invoke("lmn"));
      log.info("second invocation succeeded");
      assertEquals("xyz", client.invoke("xyz"));
      log.info("third invocation succeeded");
      assertEquals(1, TestServerThread.threadCounter);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testErrorSecondTime() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      whenToFail = 1;
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("timeout", "2000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test invocations.
      assertEquals("abc", client.invoke("abc"));
      log.info("first invocation succeeded");
      assertEquals("lmn", client.invoke("lmn"));
      log.info("second invocation succeeded");
      assertEquals("xyz", client.invoke("xyz"));
      log.info("third invocation succeeded");
      assertEquals(1, TestServerThread.threadCounter);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   

   public void testErrorThirdTime() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      whenToFail = 2;
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put("timeout", "2000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test invocations.
      assertEquals("abc", client.invoke("abc"));
      log.info("first invocation succeeded");
      assertEquals("lmn", client.invoke("lmn"));
      log.info("second invocation succeeded");
      assertEquals("xyz", client.invoke("xyz"));
      log.info("third invocation succeeded");
      assertEquals(1, TestServerThread.threadCounter);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "test";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer() throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
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
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class TestServerInvoker extends SocketServerInvoker
   {
      int counter;
      
      public TestServerInvoker(InvokerLocator locator, Map configuration)
      {
         super(locator, configuration);
      }
      public TestServerInvoker(InvokerLocator locator)
      {
         super(locator);
      }
      protected void processInvocation(Socket socket) throws Exception
      {
         clientpool = new LRUPool(2, maxPoolSize);
         clientpool.create();
         threadpool = new LinkedList();
         ServerThread worker = new TestServerThread(socket, this, clientpool, threadpool, 0, 0, ServerSocketWrapper.class.getName());
         worker.start();
      }
   }
   
   
   public static class TestServerThread extends ServerThread
   {
      static public int threadCounter;
      private int counter;
      
      public TestServerThread(Socket socket, SocketServerInvoker invoker, LRUPool clientpool, LinkedList threadpool,
                              int timeout, int writeTimeout, String serverSocketClassName) throws Exception
      {
         super(socket, invoker, clientpool, threadpool, timeout, writeTimeout, serverSocketClassName);
         threadCounter++;
      }
      protected void processInvocation(SocketWrapper socketWrapper, InputStream inputStream, OutputStream outputStream) throws Exception
      {  
         super.processInvocation(socketWrapper, inputStream, outputStream);
         if (counter++ == whenToFail)
            throw new Error("TestServerThread");
      }
   }
   
   
   public static class TestClientFactory implements ClientFactory
   {
      public ClientInvoker createClientInvoker(InvokerLocator locator, Map config) throws IOException
      {
         log.info("TestClientFaotory.createClientInvoker() called");
         return new SocketClientInvoker(locator, config);
      }
      public boolean supportsSSL()
      {
         return false;
      }  
   }
   
   
   public static class TestServerFactory implements ServerFactory
   {
      public ServerInvoker createServerInvoker(InvokerLocator locator, Map config) throws IOException
      {
         log.info("TestServerFactory.createServerInvoker() called");
         return new TestServerInvoker(locator, config);
      }
      public boolean supportsSSL()
      {
         return false;
      }
   }
}