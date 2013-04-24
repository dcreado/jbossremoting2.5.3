/*
* JBoss, Home of Professional Open Source
* Copyright 2009, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.remoting.transport.socket.shutdown;

import java.io.IOException;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.ServerFactory;
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import org.jboss.remoting.transport.socket.TransportClientFactory;


/**
 * Unit test for JBREM-1076.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright Apr 12, 2009
 * </p>
 */
public class ProcessInvocationShutdownTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ProcessInvocationShutdownTestCase.class);
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;
   protected SocketServerInvoker socketServerInvoker;

   
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
   
   
   public void testShutdown() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      assertTrue(connector.getServerInvoker() instanceof TestServerInvoker);
      log.info("using a TestServerInvoker");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      final Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Start SocketServerInvoker.processInvocation().
      new Thread()
      {
         public void run()
         {
            try
            {
               client.invoke("abc");
            }
            catch (Throwable e)
            {
               e.printStackTrace();
            }
         }
      }.start();
      
      Thread.sleep(4000);
      shutdownServer();
      Thread.sleep(4000);
      assertFalse(TestLRUPool.called);
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
      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            InvokerRegistry.registerInvokerFactories(getTransport(), TransportClientFactory.class, TestTransportServerFactory.class);
            return null;
         }
      });
      
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
      socketServerInvoker = (SocketServerInvoker) connector.getServerInvoker();
      socketServerInvoker.setMaxPoolSize(0);
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

   static class TestLRUPool extends LRUPool
   {
      static public boolean called;
      
      public TestLRUPool(int min, int max)
      {
         super(min, max);
      }  
      public void insert(Object key, Object o)
      {
         log.info(this + ".insert() called");
         called = true;
      }
   }
   
   static class TestServerInvoker extends SocketServerInvoker
   {
      public TestServerInvoker(InvokerLocator locator, Map configuration)
      {
         super(locator, configuration);
      }
      public TestServerInvoker(InvokerLocator locator)
      {
         super(locator);
      }
      public synchronized void start() throws IOException
      {
         super.start();
         clientpool = new TestLRUPool(2, maxPoolSize);
         clientpool.create(); 
      }
   }
   
   static public class TestTransportServerFactory implements ServerFactory
   {
      public boolean called;
      
      public ServerInvoker createServerInvoker(InvokerLocator locator, Map config)
      {
         called = true;
         log.info(this + ".createServerInvoker() called");
         return new TestServerInvoker(locator, config);
      }
      public boolean supportsSSL()
      {
         return false;
      }
   }
}