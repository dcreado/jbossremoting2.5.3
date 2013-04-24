/*
* JBoss, Home of Professional Open Source
* Copyright 2008, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.remoting.socketfactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.net.SocketFactory;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.AbstractInvoker;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit test for JBREM-1014.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Jul 18, 2008
 * </p>
 */
public abstract class SocketFactoryClassNameTestRoot extends TestCase
{
   private static Logger log = Logger.getLogger(SocketFactoryClassNameTestRoot.class);
   
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
      }
   }

   
   public void tearDown()
   {
   }
   
   
   public void testSocketFactoryClassNameInLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false);
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&" + Remoting.SOCKET_FACTORY_CLASS_NAME + "=" + getSocketFactoryClass().getName();
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected to " + clientLocatorURI);
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Verify client invoker is using configured SocketFactory.
      AbstractInvoker invoker = (AbstractInvoker) client.getInvoker();
      SocketFactory socketFactory = invoker.getSocketFactory();
      log.info("SocketFactory: " + socketFactory);
      assertTrue(getSocketFactoryClass().isInstance(socketFactory));

      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testSocketFactoryClassNameInConfigMap() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Remoting.SOCKET_FACTORY_CLASS_NAME, getSocketFactoryClass().getName());
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Verify client invoker is using configured SocketFactory.
      AbstractInvoker invoker = (AbstractInvoker) client.getInvoker();
      SocketFactory socketFactory = invoker.getSocketFactory();
      log.info("SocketFactory: " + socketFactory);
      assertTrue(getSocketFactoryClass().isInstance(socketFactory));
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testSocketFactoryClassNameInLocatorWithUseAllParams() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true);
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "&" + Remoting.SOCKET_FACTORY_CLASS_NAME + "=" + getSocketFactoryClass().getName();
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected to " + clientLocatorURI);
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Verify client invoker is using configured SocketFactory.
      AbstractInvoker invoker = (AbstractInvoker) client.getInvoker();
      SocketFactory socketFactory = invoker.getSocketFactory();
      log.info("SocketFactory: " + socketFactory);
      assertTrue(getSocketFactoryClass().isInstance(socketFactory));

      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testSocketFactoryClassNameInConfigMapWithUseAllParams() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Remoting.SOCKET_FACTORY_CLASS_NAME, getSocketFactoryClass().getName());
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Verify client invoker is using configured SocketFactory.
      AbstractInvoker invoker = (AbstractInvoker) client.getInvoker();
      SocketFactory socketFactory = invoker.getSocketFactory();
      log.info("SocketFactory: " + socketFactory);
      assertTrue(getSocketFactoryClass().isInstance(socketFactory));
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected abstract String getTransport();


   protected Class getSocketFactoryClass()
   {
      return TestSocketFactory.class;
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer(boolean useAllParams) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port + "/?x=x";
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "&" + metadata;
      }
      if (useAllParams)
      {
         locatorURI += "&" + Remoting.USE_ALL_SOCKET_FACTORY_PARAMS + "=true";
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

   
   public static class TestSocketFactory extends SocketFactory
   {
      SocketFactory sf = SocketFactory.getDefault();
      
      public TestSocketFactory()
      {
      }

      public Socket createSocket() throws IOException, UnknownHostException
      {
         return sf.createSocket();
      }
      
      public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException
      {
         return sf.createSocket(arg0, arg1);
      }

      public Socket createSocket(InetAddress arg0, int arg1) throws IOException
      {
         return sf.createSocket(arg0, arg1);
      }

      public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException,
            UnknownHostException
      {
         return sf.createSocket(arg0, arg1, arg2, arg3);
      }

      public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException
      {
         return sf.createSocket(arg0, arg1, arg2, arg3);
      } 
   }
}