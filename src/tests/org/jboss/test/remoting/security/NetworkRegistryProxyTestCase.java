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
package org.jboss.test.remoting.security;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.w3c.dom.Document;


/**
 * Unit test for JBREM-977.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 4, 2008
 * </p>
 */
public class NetworkRegistryProxyTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(NetworkRegistryProxyTestCase.class);
   
   private static boolean firstTime = true;
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected MBeanServer server;
   protected int numAdded;
   protected int numUpdated;

   
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
   
   
   public void testNetworkRegistryProxy() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Start registry.
      MBeanServer server = MBeanServerFactory.createMBeanServer();
      InvokerLocator locator = new InvokerLocator(locatorURI);
      TestNetworkRegistry networkRegistry = new TestNetworkRegistry(locator);
      ObjectName name = new ObjectName("test:type=TestNetworkRegistry");
      server.registerMBean(networkRegistry, name);
      
      // Create detector and tell it to register local Connectors.
      MulticastDetector detector = new MulticastDetector();
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<local/>");
      ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      detector.setConfiguration(xml.getDocumentElement());
      server.registerMBean(detector, new ObjectName("test:type=MulticastDetector"));
      detector.start();

      // Verify that TestNetworkRegistry proxy gets used.
      detector.forceDetection();
      assertEquals(1, networkRegistry.counter);
      int counter = ((Integer) server.getAttribute(name, "Counter")).intValue();
      assertEquals(networkRegistry.counter, counter);
      
      detector.stop();
      server.unregisterMBean(name);
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
      connector.addInvocationHandler("test", new TestServerInvocationHandler());
      connector.start();
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
 
   
   public static class TestServerInvocationHandler implements ServerInvocationHandler
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
}