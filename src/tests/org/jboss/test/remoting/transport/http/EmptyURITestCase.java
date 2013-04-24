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

package org.jboss.test.remoting.transport.http;

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
import org.jboss.remoting.CannotConnectException;
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
import org.jboss.remoting.transport.coyote.CoyoteInvoker;
import org.jboss.remoting.transport.http.TransportClientFactory;


/**
 * Unit test for JBREM-1168.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright Dec 13, 2009
 * </p>
 */
public class EmptyURITestCase extends TestCase
{
   private static Logger log = Logger.getLogger(EmptyURITestCase.class);
   
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
         Logger.getLogger("org.jboss.remoting").setLevel(Level.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);
         
         AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               InvokerRegistry.registerInvokerFactories("http", TransportClientFactory.class, TestCoyoteInvokerFactory.class);
               return null;
            }
         });
      }
   }

   
   public void tearDown()
   {
   }
   
   
   public void testEmptyURI() throws Throwable
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
      
      // Test connections.
      try
      {
         Object response = client.invoke("abc");
         fail("expected CannotConnectException, got: " + response);
      }
      catch (CannotConnectException e)
      {
         log.info("got expected CannotConnectException");
      }
      catch (Throwable t)
      {
         fail("expected CannotConnectException, got: " + t);         
      }
      
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
   
   static public class TestCoyoteInvokerFactory implements ServerFactory
   {
      public ServerInvoker createServerInvoker(InvokerLocator locator, Map config) throws IOException
      {
         return new TestCoyoteInvoker(locator, config);
      }
      public boolean supportsSSL()
      {
         return false;
      }
   }
   
   
   static class TestCoyoteInvoker extends CoyoteInvoker
   {
      public TestCoyoteInvoker(InvokerLocator locator)
      {
         super(locator);
      }

      public TestCoyoteInvoker(InvokerLocator locator, Map configuration)
      {
         super(locator, configuration);
      }
      
      public void service(org.apache.coyote.Request req, org.apache.coyote.Response res) throws Exception
      {
         byte[] b = new byte[]{};
         req.requestURI().setBytes(b, 0, 0);
         log.info(this + ".service()");
         super.service(req, res);
      }
   }
}