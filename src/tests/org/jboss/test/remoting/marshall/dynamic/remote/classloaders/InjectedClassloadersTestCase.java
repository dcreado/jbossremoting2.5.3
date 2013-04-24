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
package org.jboss.test.remoting.marshall.dynamic.remote.classloaders;

import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
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
import org.jboss.remoting.ServerConfiguration;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit test for JBREM-1000.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Aug 9, 2008
 * </p>
 */
public class InjectedClassloadersTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(InjectedClassloadersTestCase.class);
   private static String jarFileName;
   private static String targetClassName = "org.jboss.test.remoting.marshall.dynamic.remote.classloaders.ResponseImpl";
   
   private static final int BY_DIRECT_INJECTION = 1;
   private static final int BY_SERVER_CONFIGURATION = 2;
   private static final int BY_CONFIG_MAP = 3;
   
   private static Object RESPONSE_VALUE;
   private static boolean firstTime = true;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestClassLoader1 classLoader1;
   protected TestClassLoader2 classLoader2;
   protected TestInvocationHandler invocationHandler;

   
   public static void main(String[] args)
   {
      InjectedClassloadersTestCase server = new InjectedClassloadersTestCase();
      try
      {
         server.setUp();

         while(true)
         {
            Thread.sleep(5000);
         }
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
   
   
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
         
         jarFileName = getSystemProperty("loader.path");
         ClassLoader cl = new TestClassLoader2(getClass().getClassLoader(), jarFileName, targetClassName);
         Class c = Class.forName(targetClassName, false, cl);
         RESPONSE_VALUE = c.newInstance();
      }
   }
   
   
   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }
   
   
   public void testByDirectInjection() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(BY_DIRECT_INJECTION);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Verify that unmarshaller can get ResponseImpl class.
      Object response = client.invoke("abc");
      log.info("response: " + response);
      assertEquals(targetClassName, response.getClass().getName());
      
      // Verify that MarshallerLoaderHandler tried both configured classLoaders.
      assertTrue(classLoader1.queriedForTarget);
      assertTrue(classLoader2.queriedForTarget);

      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testByServerConfiguration() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(BY_SERVER_CONFIGURATION);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Verify that unmarshaller can get ResponseImpl class.
      Object response = client.invoke("abc");
      log.info("response: " + response);
      assertEquals(targetClassName, response.getClass().getName());
      
      // Verify that MarshallerLoaderHandler tried both configured classLoaders.
      assertTrue(classLoader1.queriedForTarget);
      assertTrue(classLoader2.queriedForTarget);

      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testByConfigMap() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(BY_CONFIG_MAP);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Verify that unmarshaller can get ResponseImpl class.
      Object response = client.invoke("abc");
      log.info("response: " + response);
      assertEquals(targetClassName, response.getClass().getName());
      
      // Verify that MarshallerLoaderHandler tried both configured classLoaders.
      assertTrue(classLoader1.queriedForTarget);
      assertTrue(classLoader2.queriedForTarget);

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
   

   protected void setupServer(int configMethod) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String loaderPort = Integer.toString(PortUtil.findFreePort(host));
      locatorURI = getTransport() + "://" + host + ":" + port;
      locatorURI += "/?" + InvokerLocator.LOADER_PORT + "=" + loaderPort;
      serverLocator = new InvokerLocator(locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      
      ArrayList classLoaders = new ArrayList();
      classLoader1 = new TestClassLoader1(targetClassName);
      classLoader2 = new TestClassLoader2(getClass().getClassLoader(), jarFileName, targetClassName);
      classLoaders.add(classLoader1);
      classLoaders.add(classLoader2);
      
      switch (configMethod)
      {
         case BY_DIRECT_INJECTION:
            log.info("injecting: BY_DIRECT_INJECTION");
            connector = new Connector(serverLocator, config);
            connector.setRemoteClassLoaders(classLoaders);
            break;
            
         case BY_SERVER_CONFIGURATION:
            log.info("injecting: BY_SERVER_CONFIGURATION");
            ServerConfiguration serverConfiguration = new ServerConfiguration(getTransport());
            Map serverParameters = new HashMap();
            serverParameters.put(Remoting.REMOTE_CLASS_LOADERS, classLoaders);
            serverConfiguration.setServerParameters(serverParameters);
            Map invokerLocatorParameters = new HashMap();
            invokerLocatorParameters.put(InvokerLocator.LOADER_PORT, loaderPort);
            serverConfiguration.setInvokerLocatorParameters(invokerLocatorParameters);
            connector = new Connector(config);
            connector.setServerConfiguration(serverConfiguration);
            break;
            
         case BY_CONFIG_MAP:
            log.info("injecting: BY_CONFIG_MAP");
            config.put(Remoting.REMOTE_CLASS_LOADERS, classLoaders);
            connector = new Connector(locatorURI, config);
            break;
            
         default:
            log.error("unrecognized configMethod: " + configMethod);
            return;
      }

      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
      locatorURI = connector.getInvokerLocator();
      log.info("Started remoting server with locator uri of: " + locatorURI);
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
   
   
   private String getSystemProperty(final String name)
   {     
      String value = null;
      try
      {
         value = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.getProperty(name);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
      
      return value;
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return RESPONSE_VALUE;
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
}