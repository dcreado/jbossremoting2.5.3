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
package org.jboss.test.remoting.transport.config;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ServerSocketFactory;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Home;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerConfiguration;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.Connector;



/**
 * 
 * POJOConfigurationTestCase verifies that Remoting org.jboss.remoting.transport.Connector's
 * are properly configured when using the new org.jboss.remoting.ServerConfiguration object.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3375 $
 * <p>
 * Copyright Oct 16, 2007
 * </p>
 */
public class POJOConfigurationTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(POJOConfigurationTestCase.class);
   
   private static boolean firstTime = true;
   private static int MAX_HOMES = 5;
   
   private Connector connector;
   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.TRACE);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }
   }

   
   public void tearDown()
   {
      if (connector != null)
      {
         connector.destroy();
      }
   }
   
   
   public void testConfiguration() throws Throwable
   {  
      log.info("entering " + getName());
      
      // Create ServerConfiguration
      ServerConfiguration config = new ServerConfiguration("socket");
      Map locatorConfig = new HashMap();
      
      // Add invokerLocatorParameters
      String address = InetAddress.getLocalHost().getHostAddress();
      locatorConfig.put("serverBindAddress", address);
      locatorConfig.put("serverBindPort", "4446");
      locatorConfig.put("datatype", "test");
      locatorConfig.put("timeout", "12345");
      config.setInvokerLocatorParameters(locatorConfig);
      
      // Add serverParameters
      Map serverParameters = new HashMap();
      serverParameters.put("clientLeasePeriod", "2345");
      serverParameters.put("timeout", "54321");
      ServerSocketFactory factory = new TestServerSocketFactory();
      serverParameters.put("customServerSocketFactory", factory);
      config.setServerParameters(serverParameters);
      
      // Add invocation handlers
      Map handlers = new HashMap();
      handlers.put("system1", "org.jboss.test.remoting.transport.config.TestServerInvocationHandler1");
      ServerInvocationHandler handler2 = new TestServerInvocationHandler2();
      handlers.put("system2,system3", handler2); 
      config.setInvocationHandlers(handlers);
      
      log.info("invokerLocatorParameters: " + config.getInvokerLocatorParameters());
      log.info("serverParameters: " + config.getServerParameters());
      log.info("invocationHandlers: " + config.getInvocationHandlers());
      
      // Create Connector and get ServerInvoker
      // Get ServerInvoker.
      connector = new Connector();
      connector.setServerConfiguration(config);
      connector.create();
      connector.start();
      ServerInvoker invoker = connector.getServerInvoker();
      
      // Check InvokerLocator.
      InvokerLocator locator = invoker.getLocator();
      log.info("constructed InvokerLocator: " + locator);
      assertEquals("socket://" + address + ":4446/?datatype=test&timeout=12345", locator.getLocatorURI());
      
      // Test parameter that appears only in invokerLocatorParameters.
      assertEquals("test", invoker.getDataType());
      
      // Test parameter that appears only in serverParameters.
      assertEquals(2345, invoker.getLeasePeriod());
      
      // Test parameter that appears in both invokerLocatorParameters and
      // serverParameters.  Verify that value in invokerLocatorParameters overrides.
      assertEquals(12345, invoker.getTimeout());
      
      // Test object injected into serverParameters.
      assertTrue(invoker.getServerSocketFactory() instanceof TestServerSocketFactory);
    
      // Test invocation handlers.
      assertEquals(3, invoker.getInvocationHandlers().length);
      assertEquals(3, invoker.getSupportedSubsystems().length);
      
      ServerInvocationHandler sih1 = invoker.getInvocationHandler("system1");
      ServerInvocationHandler sih2 = invoker.getInvocationHandler("system2");
      ServerInvocationHandler sih3 = invoker.getInvocationHandler("system3");
      assertTrue(sih1 instanceof TestServerInvocationHandler1);
      assertTrue(sih2 instanceof TestServerInvocationHandler2);
      assertTrue(sih3 instanceof TestServerInvocationHandler2);
      
      AbstractInvocationHandler aih1 = (AbstractInvocationHandler) sih1;
      AbstractInvocationHandler aih2 = (AbstractInvocationHandler) sih2;
      AbstractInvocationHandler aih3 = (AbstractInvocationHandler) sih3;
      assertEquals(invoker, aih1.getServerInvoker());
      assertEquals(invoker, aih2.getServerInvoker());
      assertEquals(invoker, aih3.getServerInvoker());
      log.info(getName() + " PASSES");
   }
   
   
   public void testClientConnectParameters() throws Throwable
   {  
      log.info("entering " + getName());
      
      // Create ServerConfiguration
      ServerConfiguration config = new ServerConfiguration("socket");
      Map locatorConfig = new HashMap();
      
      // Add invokerLocatorParameters
      String serverBindAddress = "localhost";
      String serverBindPort = "4446";
      String clientConnectAddress = InetAddress.getLocalHost().getHostAddress();
      String clientConnectPort = "4447";
      locatorConfig.put("serverBindAddress", serverBindAddress);
      locatorConfig.put("serverBindPort", serverBindPort);
      locatorConfig.put("clientConnectAddress", clientConnectAddress);
      locatorConfig.put("clientConnectPort", clientConnectPort);
      config.setInvokerLocatorParameters(locatorConfig);
      
      log.info("invokerLocatorParameters: " + config.getInvokerLocatorParameters());
      log.info("serverParameters: " + config.getServerParameters());
      log.info("invocationHandlers: " + config.getInvocationHandlers());
      
      // Create Connector and get ServerInvoker
      // Get ServerInvoker.
      connector = new Connector();
      connector.setServerConfiguration(config);
      connector.create();
      
      // Check InvokerLocator.
      InvokerLocator locator = connector.getLocator();
      log.info("constructed InvokerLocator: " + locator);
      assertEquals("socket://" + clientConnectAddress + ":" + clientConnectPort + "/",
                   locator.getLocatorURI());
      
      log.info(getName() + " PASSES");
   }
   
   
   public void testSingleHomeParametersInBothParametersMaps() throws Throwable
   {  
      log.info("entering " + getName());
      
      // Create ServerConfiguration
      ServerConfiguration config = new ServerConfiguration("socket");
      Map locatorConfig = new HashMap();
      Map serverConfig = new HashMap();
      
      // Add invokerLocatorParameters
      String serverBindAddress1 = InetAddress.getLocalHost().getHostName();
      String serverBindPort1 = "4446";
      String clientConnectAddress1 = InetAddress.getLocalHost().getHostAddress();
      String clientConnectPort1 = "4447";
      locatorConfig.put("serverBindAddress", serverBindAddress1);
      locatorConfig.put("serverBindPort", serverBindPort1);
      locatorConfig.put("clientConnectAddress", clientConnectAddress1);
      locatorConfig.put("clientConnectPort", clientConnectPort1);
      config.setInvokerLocatorParameters(locatorConfig);
      
      // Add serverParameters
      String serverBindAddress2 = InetAddress.getLocalHost().getHostAddress();
      String serverBindPort2 = "4448";
      String clientConnectAddress2 = InetAddress.getLocalHost().getHostName();
      String clientConnectPort2 = "4447";
      serverConfig.put("serverBindAddress", serverBindAddress2);
      serverConfig.put("serverBindPort", serverBindPort2);
      serverConfig.put("clientConnectAddress", clientConnectAddress2);
      serverConfig.put("clientConnectPort", clientConnectPort2);
      config.setServerParameters(serverConfig);
      
      log.info("invokerLocatorParameters: " + config.getInvokerLocatorParameters());
      log.info("serverParameters: " + config.getServerParameters());
      log.info("invocationHandlers: " + config.getInvocationHandlers());
      
      // Create Connector and get ServerInvoker
      // Get ServerInvoker.
      connector = new Connector();
      connector.setServerConfiguration(config);
      connector.create();
      
      // Check InvokerLocator.
      InvokerLocator locator = connector.getLocator();
      log.info("constructed InvokerLocator: " + locator);
      assertEquals("socket://" + clientConnectAddress1 + ":" + clientConnectPort1 + "/",
                   locator.getLocatorURI());
      
      // Check Connector's configuration map.
      Field field = Connector.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map configuration = (Map) field.get(connector);
      assertEquals(4, configuration.size());
      assertEquals(serverBindAddress2, configuration.get("serverBindAddress"));
      assertEquals(serverBindPort2, configuration.get("serverBindPort"));
      assertEquals(clientConnectAddress1, configuration.get("clientConnectAddress"));
      assertEquals(clientConnectPort1, configuration.get("clientConnectPort"));
      log.info(getName() + " PASSES");
   }
   
   
   public void testMultihomeWithPortsNoDefault() throws Throwable
   {  
      log.info("entering " + getName());
      
      // Create ServerConfiguration
      ServerConfiguration config = new ServerConfiguration("socket");
      Map locatorConfig = new HashMap();
      
      // Add non-multihome invokerLocatorParameters - show they don't interfere.
      String serverBindAddress = "localhost";
      String serverBindPort = "4446";
      String clientConnectAddress = InetAddress.getLocalHost().getHostAddress();
      String clientConnectPort = "4447";
      locatorConfig.put("serverBindAddress", serverBindAddress);
      locatorConfig.put("serverBindPort", serverBindPort);
      locatorConfig.put("clientConnectAddress", clientConnectAddress);
      locatorConfig.put("clientConnectPort", clientConnectPort);
      
      // Add multihome invokerLocatorParameters.
      ArrayList homes = new ArrayList();
      Enumeration e1 = NetworkInterface.getNetworkInterfaces();
      loop: while (e1.hasMoreElements())
      {
         NetworkInterface ni = (NetworkInterface) e1.nextElement();
         Enumeration e2 = ni.getInetAddresses();
         while (e2.hasMoreElements())
         {
            InetAddress ia = (InetAddress) e2.nextElement();
            Home h = new Home(ia.getHostAddress(), 3333);
            homes.add(h);
            if (homes.size() >= MAX_HOMES)
               break loop;
         }
      }
      
      Home h = (Home) homes.get(0);
      StringBuffer sb = new StringBuffer(h.host).append(':').append(h.port);
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb.append('!').append(h.host).append(':').append(h.port);
      }

      locatorConfig.put(InvokerLocator.HOMES_KEY, sb.toString());
      locatorConfig.put(InvokerLocator.CONNECT_HOMES_KEY, sb.toString());
      config.setInvokerLocatorParameters(locatorConfig);
      
      log.info("invokerLocatorParameters: " + config.getInvokerLocatorParameters());
      log.info("serverParameters: " + config.getServerParameters());
      log.info("invocationHandlers: " + config.getInvocationHandlers());
      
      // Create Connector.
      connector = new Connector();
      connector.setServerConfiguration(config);
      connector.create();
      
      // Check InvokerLocator.
      InvokerLocator locator = connector.getLocator();
      log.info("constructed InvokerLocator: " + locator);
      String newLocatorURI = "socket://multihome/?";
      newLocatorURI += InvokerLocator.CONNECT_HOMES_KEY + "=" + sb.toString();
      newLocatorURI += "&" + InvokerLocator.HOMES_KEY + "=" + sb.toString();
      assertEquals(newLocatorURI, locator.getLocatorURI());
      
      log.info(getName() + " PASSES");
   }
   
   
   public void testMultihomeNoPortsWithDefaultPort() throws Throwable
   {  
      log.info("entering " + getName());
      
      // Create ServerConfiguration
      ServerConfiguration config = new ServerConfiguration("socket");
      Map locatorConfig = new HashMap();
      
      // Add non-multihome invokerLocatorParameters - show they don't interfere.
      String serverBindAddress = "localhost";
      String serverBindPort = "4446";
      String clientConnectAddress = InetAddress.getLocalHost().getHostAddress();
      String clientConnectPort = "4447";
      locatorConfig.put("serverBindAddress", serverBindAddress);
      locatorConfig.put("serverBindPort", serverBindPort);
      locatorConfig.put("clientConnectAddress", clientConnectAddress);
      locatorConfig.put("clientConnectPort", clientConnectPort);
      
      // Add multihome invokerLocatorParameters.
      ArrayList homes = new ArrayList();
      Enumeration e1 = NetworkInterface.getNetworkInterfaces();
      loop: while (e1.hasMoreElements())
      {
         NetworkInterface ni = (NetworkInterface) e1.nextElement();
         Enumeration e2 = ni.getInetAddresses();
         while (e2.hasMoreElements())
         {
            InetAddress ia = (InetAddress) e2.nextElement();
            String host = ia.getHostAddress();
            boolean isIPv6 = host.indexOf('[') >= 0 || host.indexOf(':') >= 0;
            if (isIPv6) host = '[' + host + ']';
            Home h = new Home(host, -1);
            homes.add(h);
            if (homes.size() >= MAX_HOMES)
               break loop;
         }
      }
      
      Home h = (Home) homes.get(0);
      StringBuffer sb = new StringBuffer(h.host);
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb.append('!').append(h.host);
      }

      locatorConfig.put(InvokerLocator.CONNECT_HOMES_KEY, sb.toString());
      locatorConfig.put(InvokerLocator.HOMES_KEY, sb.toString());
      locatorConfig.put(InvokerLocator.DEFAULT_CONNECT_PORT, "3333");
      locatorConfig.put(InvokerLocator.DEFAULT_PORT, "4444");
      config.setInvokerLocatorParameters(locatorConfig);
      
      log.info("invokerLocatorParameters: " + config.getInvokerLocatorParameters());
      log.info("serverParameters: " + config.getServerParameters());
      log.info("invocationHandlers: " + config.getInvocationHandlers());
      
      // Create Connector.
      connector = new Connector();
      connector.setServerConfiguration(config);
      connector.create();
      
      // Check InvokerLocator.
      h = (Home) homes.get(0);
      StringBuffer sb_connectHomes = new StringBuffer(h.host).append(':').append("3333");
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb_connectHomes.append('!').append(h.host).append(':').append("3333");
      }
      
      h = (Home) homes.get(0);
      StringBuffer sb_homes = new StringBuffer(h.host).append(':').append("4444");
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb_homes.append('!').append(h.host).append(':').append("4444");
      }
      
      InvokerLocator locator = connector.getLocator();
      log.info("constructed InvokerLocator: " + locator);
      String expectedURI = "socket://multihome/?";
      expectedURI += InvokerLocator.CONNECT_HOMES_KEY + "=" + sb_connectHomes.toString() + "&";
      expectedURI += InvokerLocator.HOMES_KEY + "=" + sb_homes.toString() + "&";
      expectedURI += InvokerLocator.DEFAULT_CONNECT_PORT + "=3333" + "&";
      expectedURI += InvokerLocator.DEFAULT_PORT + "=4444";
      log.info("actual locator: " + locator);
      log.info("expected locator: " + expectedURI);
      assertEquals(new InvokerLocator(expectedURI), locator);
      
      log.info(getName() + " PASSES");
   }
   
   
   public void testMultihomeDuplicateElements() throws Throwable
   {  
      log.info("entering " + getName());
      
      // Create ServerConfiguration
      ServerConfiguration config = new ServerConfiguration("socket");
      Map locatorConfig = new HashMap();
      Map serverConfig = new HashMap();
      
      // Add multihome invokerLocatorParameters.
      ArrayList homes = new ArrayList();
      Enumeration e1 = NetworkInterface.getNetworkInterfaces();
      loop: while (e1.hasMoreElements())
      {
         NetworkInterface ni = (NetworkInterface) e1.nextElement();
         Enumeration e2 = ni.getInetAddresses();
         while (e2.hasMoreElements())
         {
            InetAddress ia = (InetAddress) e2.nextElement();
            Home h = new Home(ia.getHostAddress(), -1);
            homes.add(h);
            if (homes.size() >= MAX_HOMES)
               break loop;
         }
      }
      
      Home h = (Home) homes.get(0);
      StringBuffer sb1 = new StringBuffer(h.host).append(':').append("1111");
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb1.append('!').append(h.host).append(":").append("1111");
      }
      locatorConfig.put(InvokerLocator.CONNECT_HOMES_KEY, sb1.toString());
      
      h = (Home) homes.get(0);
      StringBuffer sb2 = new StringBuffer(h.host).append(':').append("2222");
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb2.append('!').append(h.host).append(":").append("2222");
      }
      serverConfig.put(InvokerLocator.CONNECT_HOMES_KEY, sb2.toString());
      
      h = (Home) homes.get(0);
      StringBuffer sb3 = new StringBuffer(h.host).append(':').append("3333");
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb3.append('!').append(h.host).append(":").append("3333");
      }
      locatorConfig.put(InvokerLocator.HOMES_KEY, sb3.toString());
      
      h = (Home) homes.get(0);
      StringBuffer sb4 = new StringBuffer(h.host).append(':').append("4444");
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb4.append('!').append(h.host).append(":").append("4444");
      }
      serverConfig.put(InvokerLocator.HOMES_KEY, sb4.toString());
      
      config.setInvokerLocatorParameters(locatorConfig);
      config.setServerParameters(serverConfig);
      
      log.info("invokerLocatorParameters: " + config.getInvokerLocatorParameters());
      log.info("serverParameters: " + config.getServerParameters());
      log.info("invocationHandlers: " + config.getInvocationHandlers());
      
      // Create Connector.
      connector = new Connector();
      connector.setServerConfiguration(config);
      connector.create();
      
      // Check InvokerLocator.
      InvokerLocator locator = connector.getLocator();
      log.info("constructed InvokerLocator: " + locator);
      String expectedURI = "socket://multihome/?";
      expectedURI += InvokerLocator.CONNECT_HOMES_KEY + "=" + sb1.toString() + "&";
      expectedURI += InvokerLocator.HOMES_KEY + "=" + sb3.toString();
      assertEquals(expectedURI, locator.getLocatorURI());
      
      log.info(getName() + " PASSES");
   }
   
   
   public void testMultihomeHomesInServerConfig() throws Throwable
   {  
      log.info("entering " + getName());
      
      // Create ServerConfiguration
      ServerConfiguration config = new ServerConfiguration("socket");
      Map locatorConfig = new HashMap();
      Map serverConfig = new HashMap();
      
      // Add multihome invokerLocatorParameters.
      ArrayList homes = new ArrayList();
      Enumeration e1 = NetworkInterface.getNetworkInterfaces();
      loop: while (e1.hasMoreElements())
      {
         NetworkInterface ni = (NetworkInterface) e1.nextElement();
         Enumeration e2 = ni.getInetAddresses();
         while (e2.hasMoreElements())
         {
            InetAddress ia = (InetAddress) e2.nextElement();
            Home h = new Home(ia.getHostAddress(), -1);
            homes.add(h);
            if (homes.size() >= MAX_HOMES)
               break loop;
         }
      }
      
      Home h = (Home) homes.get(0);
      StringBuffer sb4 = new StringBuffer(h.host).append(':').append("4444");
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb4.append('!').append(h.host).append(":").append("4444");
      }
      serverConfig.put(InvokerLocator.HOMES_KEY, sb4.toString());
      
      config.setInvokerLocatorParameters(locatorConfig);
      config.setServerParameters(serverConfig);
      
      log.info("invokerLocatorParameters: " + config.getInvokerLocatorParameters());
      log.info("serverParameters: " + config.getServerParameters());
      log.info("invocationHandlers: " + config.getInvocationHandlers());
      
      // Create Connector.
      connector = new Connector();
      connector.setServerConfiguration(config);
      connector.create();
      
      // Check InvokerLocator.
      InvokerLocator locator = connector.getLocator();
      log.info("constructed InvokerLocator: " + locator);
      String expectedURI = "socket://multihome/?";
      expectedURI += InvokerLocator.HOMES_KEY + "=" + sb4.toString();
      assertEquals(expectedURI, locator.getLocatorURI());
      
      log.info(getName() + " PASSES");
   }
   
   
   public void testMultihomeMissingHomes() throws Throwable
   {  
      log.info("entering " + getName());
      
      // Create ServerConfiguration
      ServerConfiguration config = new ServerConfiguration("socket");
      Map locatorConfig = new HashMap();
      Map serverConfig = new HashMap();
      
      // Add multihome invokerLocatorParameters.
      ArrayList homes = new ArrayList();
      Enumeration e1 = NetworkInterface.getNetworkInterfaces();
      loop: while (e1.hasMoreElements())
      {
         NetworkInterface ni = (NetworkInterface) e1.nextElement();
         Enumeration e2 = ni.getInetAddresses();
         while (e2.hasMoreElements())
         {
            InetAddress ia = (InetAddress) e2.nextElement();
            Home h = new Home(ia.getHostAddress(), -1);
            homes.add(h);
            if (homes.size() >= MAX_HOMES)
               break loop;
         }
      }
      
      Home h = (Home) homes.get(0);
      StringBuffer sb1 = new StringBuffer(h.host).append(':').append("1111");
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb1.append('!').append(h.host).append(":").append("1111");
      }
      locatorConfig.put(InvokerLocator.CONNECT_HOMES_KEY, sb1.toString());
      
      config.setInvokerLocatorParameters(locatorConfig);
      config.setServerParameters(serverConfig);
      
      log.info("invokerLocatorParameters: " + config.getInvokerLocatorParameters());
      log.info("serverParameters: " + config.getServerParameters());
      log.info("invocationHandlers: " + config.getInvocationHandlers());
      
      // Create Connector.
      connector = new Connector();
      connector.setServerConfiguration(config);
      
      try
      {
         log.info("=================  EXCEPTION EXPECTED ================");
         connector.create();
         fail("Should have gotten IllegalStateException");
      }
      catch (IllegalStateException e)
      {
         String msg = "Error configuring invoker from configuration POJO.  Can not continue without invoker.";
         assertEquals(msg, e.getMessage());
         log.info("got expected IllegalStateException");
         log.info("======================================================");
      }
      
      log.info(getName() + " PASSES");
   }
   
   
   public void testMultihomeAnonymousPorts() throws Throwable
   {  
      log.info("entering " + getName());
      
      // Create ServerConfiguration
      ServerConfiguration config = new ServerConfiguration("socket");
      Map locatorConfig = new HashMap();
      
      // Add non-multihome invokerLocatorParameters - show they don't interfere.
      String serverBindAddress = "localhost";
      String serverBindPort = "4446";
      String clientConnectAddress = InetAddress.getLocalHost().getHostAddress();
      String clientConnectPort = "4447";
      locatorConfig.put("serverBindAddress", serverBindAddress);
      locatorConfig.put("serverBindPort", serverBindPort);
      locatorConfig.put("clientConnectAddress", clientConnectAddress);
      locatorConfig.put("clientConnectPort", clientConnectPort);
      
      // Add multihome invokerLocatorParameters.
      ArrayList homes = new ArrayList();
      Enumeration e1 = NetworkInterface.getNetworkInterfaces();
      loop: while (e1.hasMoreElements())
      {
         NetworkInterface ni = (NetworkInterface) e1.nextElement();
         Enumeration e2 = ni.getInetAddresses();
         log.info("interface: " + ni.getDisplayName());
         while (e2.hasMoreElements())
         {
            InetAddress ia = (InetAddress) e2.nextElement();
            String host = ia.getHostAddress();
            boolean isIPv6 = host.indexOf('[') >= 0 || host.indexOf(':') >= 0;
            if (isIPv6) host = '[' + host + ']';
            log.info("host: " + host);
            Home h = new Home(host, -1);
            homes.add(h);
            if (homes.size() >= MAX_HOMES)
               break loop;
         }
      }
      
      Home h = (Home) homes.get(0);
      StringBuffer sb = new StringBuffer(h.host);
      for (int i = 1; i < homes.size(); i++)
      {
         h = (Home) homes.get(i);
         sb.append('!').append(h.host);
      }

      locatorConfig.put(InvokerLocator.CONNECT_HOMES_KEY, sb.toString());
      locatorConfig.put(InvokerLocator.HOMES_KEY, sb.toString());
      config.setInvokerLocatorParameters(locatorConfig);
      
      log.info("invokerLocatorParameters: " + config.getInvokerLocatorParameters());
      log.info("serverParameters: " + config.getServerParameters());
      log.info("invocationHandlers: " + config.getInvocationHandlers());
      
      // Create Connector.
      connector = new Connector();
      connector.setServerConfiguration(config);
      connector.create();
      
      // Check InvokerLocator.
      ServerInvoker invoker = connector.getServerInvoker();
      InvokerLocator locator = invoker.getLocator();
      List homesList = locator.getHomeList();
      List connectHomesList = locator.getConnectHomeList();
      assertEquals(connectHomesList, homesList);

      log.info("constructed InvokerLocator: " + locator);
      String homesString = InvokerLocator.convertHomesListToString(homesList);
      String connectHomesString = InvokerLocator.convertHomesListToString(connectHomesList);
      String expectedURI = "socket://multihome/?";
      expectedURI += InvokerLocator.CONNECT_HOMES_KEY + "=" + connectHomesString + "&";
      expectedURI += InvokerLocator.HOMES_KEY + "=" + homesString;
      assertEquals(new InvokerLocator(expectedURI), locator);
      
      log.info(getName() + " PASSES");
   }
}