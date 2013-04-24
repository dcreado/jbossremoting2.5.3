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
package org.jboss.test.remoting.classloader.parentfirst;

import java.io.Serializable;
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
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit test for JBREM-1019.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Aug 3, 2008
 * </p>
 */
public class ParentFirstClassloaderTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ParentFirstClassloaderTestCase.class);
   
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
   

   /**
    * Verify that by default the context classloader is not called first.
    */
   public void testDefaultConfig() throws Throwable
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
      
      TestClassLoader tcl = new TestClassLoader();
      Thread.currentThread().setContextClassLoader(tcl);
      
      // Test connection.
      client.invoke("abc");
      log.info("connection is good");
      
      // Verify that TestClassLoader has not been queried.
      assertFalse(tcl.visited);
      log.info("context classloader has not been queried");
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Verify that by the context classloader is called first if 
    * org.jboss.remoting.Remoting.CLASSLOADING_PARENT_FIRST_DELEGATION is set to "true"
    * in the InvokerLocator.
    */
   public void testByInvokerLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      String clientLocatorURI = locatorURI;
      clientLocatorURI += "/?" + Remoting.CLASSLOADING_PARENT_FIRST_DELEGATION + "=false";
      InvokerLocator clientLocator = new InvokerLocator(clientLocatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      TestClassLoader tcl = new TestClassLoader();
      setContextClassLoader(tcl);
      
      // Test connection.
      client.invoke("abc");
      log.info("connection is good");
      
      // Verify that TestClassLoader has been queried.
      assertTrue(tcl.visited);
      log.info("context classloader has been queried");
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Verify that by the context classloader is called first if 
    * org.jboss.remoting.Remoting.CLASSLOADING_PARENT_FIRST_DELEGATION is set to "true"
    * in the config map.
    */
   public void testByConfigMap() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Remoting.CLASSLOADING_PARENT_FIRST_DELEGATION, "false");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      TestClassLoader tcl = new TestClassLoader();
      setContextClassLoader(tcl);
      
      // Test connection.
      client.invoke("abc");
      log.info("connection is good");
      
      // Verify that TestClassLoader has been queried.
      assertTrue(tcl.visited);
      log.info("context classloader has been queried");
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   /**
    * Verify that by the context classloader is called first if the system property
    * org.jboss.remoting.Remoting.CLASSLOADING_PARENT_FIRST_DELEGATION_PROP is set to "true"
    * in the config map.
    */
   public void testBySystemProperty() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      System.setProperty(Remoting.CLASSLOADING_PARENT_FIRST_DELEGATION_PROP, "false");
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      TestClassLoader tcl = new TestClassLoader();
      setContextClassLoader(tcl);
      
      // Test connection.
      client.invoke("abc");
      log.info("connection is good");
      
      // Verify that TestClassLoader has been queried.
      assertTrue(tcl.visited);
      log.info("context classloader has been queried");
      
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
   
   
   protected void setContextClassLoader(final ClassLoader classLoader)
   {
      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            Thread.currentThread().setContextClassLoader(classLoader);
            return null;
         }
      });
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return new TestClass();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }

   
   static class TestClassLoader extends ClassLoader
   {
      boolean visited;
      
      public Class loadClass(String name) throws ClassNotFoundException
      {
         visited = true;
         log.info("TestClassLoader.loadClass(): " + name);
         throw new ClassNotFoundException(name);
      }
   }
   
   
   static class TestClass implements Serializable
   {
      /** The serialVersionUID */
      private static final long serialVersionUID = 1L;
   }
}