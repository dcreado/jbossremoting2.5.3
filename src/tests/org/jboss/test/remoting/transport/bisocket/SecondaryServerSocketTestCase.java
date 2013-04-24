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
package org.jboss.test.remoting.transport.bisocket;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.xml.parsers.DocumentBuilderFactory;

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
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.remoting.transport.bisocket.BisocketServerInvoker;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import org.w3c.dom.Document;


/**
 * Unit tests for JBREM-755.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3283 $
 * <p>
 * Copyright Aug 22, 2007
 * </p>
 */
public class SecondaryServerSocketTestCase extends TestCase
{
   public static int port;
   
   private static Logger log = Logger.getLogger(SecondaryServerSocketTestCase.class);
   private static boolean firstTime = true;
   
   // remoting server connector
   private Connector connector;
   private InvokerLocator serverLocator;
   private TestInvocationHandler invocationHandler;

   
   /**
    * Sets up target remoting server.
    */
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
   
   
   /**
    * Verify that secondary port is correctly set when specifed in InvokerLocator.
    */
   public void testSetSecondaryPortByInvokerLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      int PORT = PortUtil.findFreePort(host);
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      locatorURI += "/?" + Bisocket.SECONDARY_BIND_PORT + "=" + PORT;
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      assertEquals("abc", client.invoke("abc"));
      log.info("client is connected");
      
      // Verify secondary ServerSocket port is set correctly.
      ServerInvoker serverInvoker = connector.getServerInvoker();
      assertTrue(serverInvoker instanceof BisocketServerInvoker);
      Field field = BisocketServerInvoker.class.getDeclaredField("secondaryBindPorts");
      field.setAccessible(true);
      List secondaryBindPorts = (List) field.get(serverInvoker);
      Integer secondaryBindPort = (Integer) secondaryBindPorts.get(0);
      assertEquals(PORT, secondaryBindPort.intValue());
      field = BisocketServerInvoker.class.getDeclaredField("secondaryServerSockets");
      field.setAccessible(true);
      Set secondaryServerSockets = (Set) field.get(serverInvoker);
      ServerSocket serverSocket = (ServerSocket) secondaryServerSockets.iterator().next();
      assertEquals(PORT, serverSocket.getLocalPort());

      client.disconnect();
      connector.stop();
   }
   
   
   /**
    * Verify that secondary bind port is correctly set when specifed in config map.
    */
   public void testSetSecondaryBindPortByConfigMap() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      int PORT = PortUtil.findFreePort(host);
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.SECONDARY_BIND_PORT, Integer.toString(PORT));
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      assertEquals("abc", client.invoke("abc"));
      log.info("client is connected");
      
      // Verify secondary ServerSocket port is set correctly.
      ServerInvoker serverInvoker = connector.getServerInvoker();
      assertTrue(serverInvoker instanceof BisocketServerInvoker);
      Field field = BisocketServerInvoker.class.getDeclaredField("secondaryBindPorts");
      field.setAccessible(true);
      List secondaryBindPorts = (List) field.get(serverInvoker);
      Integer secondaryBindPort = (Integer) secondaryBindPorts.get(0);
      assertEquals(PORT, secondaryBindPort.intValue());
      field = BisocketServerInvoker.class.getDeclaredField("secondaryServerSockets");
      field.setAccessible(true);
      Set secondaryServerSockets = (Set) field.get(serverInvoker);
      ServerSocket serverSocket = (ServerSocket) secondaryServerSockets.iterator().next();
      assertEquals(PORT, serverSocket.getLocalPort());

      client.disconnect();
      connector.stop();
   }
   
   
   /**
    * Verify that secondary bind port is correctly set by
    * BisocketServerInvoker.setSecondaryBindPort().
    */
   public void testSetSecondaryBindPortBySetter() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      int PORT = PortUtil.findFreePort(host);
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      assertTrue(connector.getServerInvoker() instanceof BisocketServerInvoker);
      BisocketServerInvoker serverInvoker = (BisocketServerInvoker) connector.getServerInvoker();
      serverInvoker.setSecondaryBindPort(PORT);
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      assertEquals("abc", client.invoke("abc"));
      log.info("client is connected");
      
      // Verify secondary ServerSocket port is set correctly.
      Field field = BisocketServerInvoker.class.getDeclaredField("secondaryBindPorts");
      field.setAccessible(true);
      List secondaryBindPorts = (List) field.get(serverInvoker);
      log.info("secondaryBindPorts.size(): " + secondaryBindPorts.size());
      Integer secondaryBindPort = (Integer) secondaryBindPorts.get(0);
      assertEquals(PORT, secondaryBindPort.intValue());
      field = BisocketServerInvoker.class.getDeclaredField("secondaryServerSockets");
      field.setAccessible(true);
      Set secondaryServerSockets = (Set) field.get(serverInvoker);
      ServerSocket serverSocket = (ServerSocket) secondaryServerSockets.iterator().next();
      assertEquals(PORT, serverSocket.getLocalPort());

      client.disconnect();
      connector.stop();
   }
   
   
   /**
    * Verify that secondary bind port is correctly set by when specified
    * in XML document.
    */
   public void testSetSecondaryBindPortByXMLConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      int PORT = PortUtil.findFreePort(host);
      port = PortUtil.findFreePort(host);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(config);
      
      // Create and set xml configuration document.
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("   <invoker transport=\"" + getTransport() + "\">");
      buf.append("      <attribute name=\"serverBindAddress\">" + host + "</attribute>");
      buf.append("      <attribute name=\"serverBindPort\">" + port + "</attribute>");
      buf.append("      <attribute name=\"secondaryBindPort\">" + PORT + "</attribute>");
      buf.append("   </invoker>");
      buf.append("</config>");
      ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      String locatorURI = connector.getInvokerLocator();
      log.info("Started remoting server with locator uri of: " + locatorURI);
      serverLocator = new InvokerLocator(locatorURI);
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      assertEquals("abc", client.invoke("abc"));
      log.info("client is connected");
      
      // Verify secondary ServerSocket port is set correctly.
      ServerInvoker serverInvoker = connector.getServerInvoker();
      assertTrue(serverInvoker instanceof BisocketServerInvoker);
      Field field = BisocketServerInvoker.class.getDeclaredField("secondaryBindPorts");
      field.setAccessible(true);
      List secondaryBindPorts = (List) field.get(serverInvoker);
      Integer secondaryBindPort = (Integer) secondaryBindPorts.get(0);
      assertEquals(PORT, secondaryBindPort.intValue());
      field = BisocketServerInvoker.class.getDeclaredField("secondaryServerSockets");
      field.setAccessible(true);
      Set secondaryServerSockets = (Set) field.get(serverInvoker);
      ServerSocket serverSocket = (ServerSocket) secondaryServerSockets.iterator().next();
      assertEquals(PORT, serverSocket.getLocalPort());

      client.disconnect();
      connector.stop();
   }
   
   
   /**
    * Verify that secondary connect port is correctly set when specifed in config map.
    */
   public void testSetSecondaryConnectPortByConfigMap() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      int PORT = PortUtil.findFreePort(host);
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(Bisocket.SECONDARY_CONNECT_PORT, Integer.toString(PORT));
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      assertEquals("abc", client.invoke("abc"));
      log.info("client is connected");
      
      // Verify secondaryLocator is set correctly.
      ServerInvoker serverInvoker = connector.getServerInvoker();
      assertTrue(serverInvoker instanceof BisocketServerInvoker);
      Field field = BisocketServerInvoker.class.getDeclaredField("secondaryLocator");
      field.setAccessible(true);
      InvokerLocator secondaryLocator = (InvokerLocator) field.get(serverInvoker);
      log.info("secondaryLocator: " + secondaryLocator);
      assertEquals(PORT, secondaryLocator.getPort());

      client.disconnect();
      connector.stop();
   }
   
   
   /**
    * Verifies that host in secondary InvokerLocator can be set by
    * clientConnectAddress parameter.
    */
   public void testClientConnectAddressFromXML() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String HOST = InetAddress.getLocalHost().getHostName();
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(config);
      
      // Create and set xml configuration document.
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("   <invoker transport=\"" + getTransport() + "\">");
      buf.append("      <attribute name=\"serverBindAddress\">" + host + "</attribute>");
      buf.append("      <attribute name=\"serverBindPort\">" + port + "</attribute>");
      buf.append("      <attribute name=\"clientConnectAddress\">" + HOST + "</attribute>");
      buf.append("   </invoker>");
      buf.append("</config>");
      ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      String locatorURI = connector.getInvokerLocator();
      log.info("Started remoting server with locator uri of: " + locatorURI);
      serverLocator = new InvokerLocator(locatorURI);
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      assertEquals("abc", client.invoke("abc"));
      log.info("client is connected");
      
      // Verify secondaryLocator is set correctly.
      ServerInvoker serverInvoker = connector.getServerInvoker();
      assertTrue(serverInvoker instanceof BisocketServerInvoker);
      Field field = BisocketServerInvoker.class.getDeclaredField("secondaryLocator");
      field.setAccessible(true);
      InvokerLocator secondaryLocator = (InvokerLocator) field.get(serverInvoker);
      log.info("secondaryLocator: " + secondaryLocator);
      assertEquals(HOST, secondaryLocator.getHost());

      client.disconnect();
      connector.stop();
   }
   
   
   /**
    * Verifies that host in secondary InvokerLocator can be set by InvokerLocator.
    */
   public void testClientConnectAddressFromInvokerLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String HOST = InetAddress.getLocalHost().getHostName();
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + HOST + ":" + port;
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      assertEquals("abc", client.invoke("abc"));
      log.info("client is connected");
      
      // Verify secondaryLocator is set correctly.
      ServerInvoker serverInvoker = connector.getServerInvoker();
      assertTrue(serverInvoker instanceof BisocketServerInvoker);
      Field field = BisocketServerInvoker.class.getDeclaredField("secondaryLocator");
      field.setAccessible(true);
      InvokerLocator secondaryLocator = (InvokerLocator) field.get(serverInvoker);
      log.info("secondaryLocator: " + secondaryLocator);
      assertEquals(HOST, secondaryLocator.getHost());
      client.disconnect();
      connector.stop();
   }
   
   
   /**
    * Verifies that secondary ServerSocket is bound to the same address as the
    * primary ServerSocket.
    */
   public void testServerBindAddress() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String HOST = InetAddress.getLocalHost().getHostName();
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put(ServerInvoker.SERVER_BIND_ADDRESS_KEY, HOST);
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      assertEquals("abc", client.invoke("abc"));
      log.info("client is connected");
      
      // Verify secondary ServerSocket port is set correctly.
      ServerInvoker serverInvoker = connector.getServerInvoker();
      assertTrue(serverInvoker instanceof BisocketServerInvoker);
      Field field = SocketServerInvoker.class.getDeclaredField("serverSockets");
      field.setAccessible(true);
      List serverSockets = (List) field.get(serverInvoker);
      ServerSocket primaryServerSocket = (ServerSocket) serverSockets.get(0);
      field = BisocketServerInvoker.class.getDeclaredField("secondaryServerSockets");
      field.setAccessible(true);
      Set secondaryServerSockets = (Set) field.get(serverInvoker);
      ServerSocket secondaryServerSocket = (ServerSocket) secondaryServerSockets.iterator().next();
      assertEquals(primaryServerSocket.getInetAddress(), secondaryServerSocket.getInetAddress());

      client.disconnect();
      connector.stop();
   }
   
   
   protected String getTransport()
   {
      return "bisocket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

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
}