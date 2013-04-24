/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.test.remoting.transport.socket.timeout;

import java.io.IOException;
import java.lang.reflect.Method;
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
import org.jboss.logging.XLevel;
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
import org.jboss.remoting.transport.socket.ServerAddress;
import org.jboss.remoting.transport.socket.SocketClientInvoker;
import org.jboss.remoting.transport.socket.SocketServerInvoker;


/**
 * Unit tests for JBREM-1188.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Feb 16, 2010
 */
public class SocketDefaultTimeoutTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(SocketDefaultTimeoutTestCase.class);
   
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
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
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
            InvokerRegistry.registerInvokerFactories(getTransport(), getClientFactoryClass(), TestServerFactory.class);
            return null;
         }
      });
   }

   
   public void tearDown()
   {
   }
   
   
   public void testDefaultTimeout() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      Method getServerAddress = getClientInvokerClass().getMethod("getServerAddress", new Class[]{});
      getServerAddress.setAccessible(true);
      ClientInvoker clientInvoker = client.getInvoker();
      ServerAddress address = (ServerAddress) getServerAddress.invoke(clientInvoker, new Object[]{});
      log.info("timeout in use: " + address.timeout);
      assertEquals(SocketClientInvoker.SO_TIMEOUT_DEFAULT, address.timeout);
      
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
   
   
   protected Class getClientFactoryClass()
   {
      return TestSocketClientFactory.class;
   }
   
   
   protected Class getClientInvokerClass()
   {
      return TestSocketClientInvoker.class;
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
   
   
   static public class TestSocketClientInvoker extends SocketClientInvoker
   {
      public TestSocketClientInvoker(InvokerLocator locator, Map configuration) throws IOException
      {
         super(locator, configuration);
      }
      public TestSocketClientInvoker(InvokerLocator locator)
      {
         super(locator);
      }
      public ServerAddress getServerAddress()
      {
         return address;
      }
      public String toString()
      {
         return "TestSocketClientInvoker";
      }
   }
   
   
   public static class TestSocketClientFactory implements ClientFactory
   {
      public ClientInvoker createClientInvoker(InvokerLocator locator, Map config) throws IOException
      {
         ClientInvoker clientInvoker = new TestSocketClientInvoker(locator, config);
         log.info("TestClientFaotory.createClientInvoker() returning " + clientInvoker);
         return clientInvoker;
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
         return new SocketServerInvoker(locator, config);
      }
      public boolean supportsSSL()
      {
         return false;
      }
   }
}