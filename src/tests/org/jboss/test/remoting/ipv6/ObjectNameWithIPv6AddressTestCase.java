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
package org.jboss.test.remoting.ipv6;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

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
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


public class ObjectNameWithIPv6AddressTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ObjectNameWithIPv6AddressTestCase.class);
   
   private static boolean firstTime = true;
   private static boolean ipv6IsAvailable = true;
   
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
         log.info("java.version: " + System.getProperty("java.version"));
         
         try
         {
            InetAddress addr = InetAddress.getByName("[::1]");
            new ServerSocket(3333, 200, addr);
         }
         catch (Exception e)
         {
            if ("Protocol family unavailable".equalsIgnoreCase(e.getMessage()) ||
                "Protocol family not supported".equalsIgnoreCase(e.getMessage()))
            {
               ipv6IsAvailable = false;
               log.info("ipV6 is not available");
            }
         }
      }
   }

   
   public void tearDown()
   {
   }
   
   
   public void testIPv6Loopback() throws Throwable
   {
      log.info("entering " + getName());
      doTest("[::1]");
      log.info(getName() + " PASSES");
   }
   
   
   public void testIPv6Any() throws Throwable
   { 
      log.info("entering " + getName());
      doTest("[::]");
      log.info(getName() + " PASSES");
   }   
   
   
   public void testIPv4Mapped() throws Throwable
   {
      log.info("entering " + getName());
      doTest("[::ffff:127.0.0.1]");
      log.info(getName() + " PASSES");
   }
   
   
   protected void doTest(String host) throws Throwable
   {
      if (!ipv6IsAvailable) return;
      
      MBeanServer server = null;
      try
      {
         server = (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return MBeanServerFactory.createMBeanServer();
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (Exception) e.getCause();
      }

      Connector connector = new Connector();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      connector.setInvokerLocator(locatorURI);
      ObjectName obj = new ObjectName("jboss.remoting:type=Connector,transport=" + getTransport());
      server.registerMBean(connector, obj);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
      
      ServerInvoker serverInvoker = connector.getServerInvoker();
      ObjectName objectName = new ObjectName(serverInvoker.getMBeanObjectName());
      assertTrue(objectName.getCanonicalName().indexOf(host) >= 0);
      assertTrue(server.isRegistered(objectName));
      log.info(objectName + " is registered");
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      client.disconnect();
      shutdownServer();
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