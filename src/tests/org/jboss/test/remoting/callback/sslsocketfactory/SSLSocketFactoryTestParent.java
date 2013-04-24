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
package org.jboss.test.remoting.callback.sslsocketfactory;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

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
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


abstract public class SSLSocketFactoryTestParent extends TestCase
{
   private static Logger log = Logger.getLogger(SSLSocketFactoryTestParent.class);
   
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
   }

   
   public void tearDown()
   {
   }
   
   
   public void testSSLServerSocketFactory() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStoreFilePath = this.getClass().getResource("truststore").getFile();
      clientConfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
      clientConfig.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Add callback listener.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, null, null, true);
      
      // Test callback SSLServerSocketFactory.
      Set callbackConnectors = client.getCallbackConnectors(callbackHandler);
      assertEquals(1, callbackConnectors.size());
      Connector callbackConnector = (Connector) callbackConnectors.iterator().next();
      ServerInvoker callbackServerInvoker = callbackConnector.getServerInvoker();
      ServerSocketFactory serverSocketFactory = callbackServerInvoker.getServerSocketFactory();
      assertTrue(serverSocketFactory instanceof SSLServerSocketFactory);
      InetAddress address = InetAddress.getLocalHost();
      int port = PortUtil.findFreePort(address.getHostAddress());
      ServerSocket serverSocket =  serverSocketFactory.createServerSocket(port, 100, address);
      SSLServerSocket sslServerSocket = (SSLServerSocket) serverSocket;
      assertTrue(sslServerSocket.getUseClientMode());
      
      client.removeListener(callbackHandler);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   abstract protected String getTransport();
   
   
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
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStoreFilePath = this.getClass().getResource("keystore").getFile();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
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
   
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("received callback");
      }  
   }
}