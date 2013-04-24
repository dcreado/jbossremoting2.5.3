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
package org.jboss.test.remoting.transport.http.errors;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerConfiguration;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;
import org.jboss.test.remoting.transport.mock.MockServerInvocationHandler;
import org.w3c.dom.Document;


public class NoThrowOnErrorTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(NoThrowOnErrorTestCase.class);
   
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
   
   
   public void testThrowException() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false, true);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Should throw exception.
      try
      {
         client.invoke("abc");
         fail("should have thrown TestException");
      }
      catch (Exception e)
      {
         assertTrue("should have thrown TestException", e instanceof TestException);
      }
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNoThrowExceptionInMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false, true);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Should return exception.
      HashMap metadata = new HashMap();
      metadata.put(HTTPMetadataConstants.NO_THROW_ON_ERROR, "true");
      assertTrue(client.invoke("abc", metadata) instanceof TestException);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNoThrowExceptionInConfigMap() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false, true);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(HTTPMetadataConstants.NO_THROW_ON_ERROR, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Should return exception.
      assertTrue(client.invoke("abc") instanceof TestException);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNoThrowExceptionInInvokerLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, true);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Should return exception.
      assertTrue(client.invoke("abc") instanceof TestException);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNoThrowExceptionInXMLConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("<invoker transport=\"http\">");
      buf.append("<attribute name=\"serverBindAddress\">" + host + "</attribute>");
      buf.append("<attribute name=\"serverBindPort\">" + port + "</attribute>");
      buf.append("<attribute name=\"" +  HTTPMetadataConstants.NO_THROW_ON_ERROR + "\" isParam=\"true\">true</attribute>");
      buf.append("<attribute name=\"" +  InvokerLocator.FORCE_REMOTE + "\" isParam=\"true\">true</attribute>");
      buf.append("</invoker>");
      buf.append("<handlers>");
      buf.append("  <handler subsystem=\"test\">" + TestInvocationHandler.class.getName() + "</handler>\n");
      buf.append("</handlers>");
      buf.append("</config>");
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(buf.toString().getBytes()));
      connector = new Connector();
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      connector.start();
      
      // Create client.
      locatorURI = connector.getInvokerLocator();
      log.info("created server: " + locatorURI);
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Should return exception.
      assertTrue(client.invoke("abc") instanceof TestException);
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNoThrowExceptionInServerConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      ServerConfiguration config = new ServerConfiguration("http");
      Map locatorConfig = new HashMap();
      locatorConfig.put("serverBindAddress", host);
      locatorConfig.put("serverBindPort", Integer.toString(port));
      locatorConfig.put(HTTPMetadataConstants.NO_THROW_ON_ERROR, "true");
      locatorConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      config.setInvokerLocatorParameters(locatorConfig);
      Map handlers = new HashMap();
      handlers.put("test", new TestInvocationHandler());
      config.setInvocationHandlers(handlers);
      connector = new Connector();
      connector.setServerConfiguration(config);
      connector.create();
      connector.start();
      
      // Create client.
      locatorURI = connector.getInvokerLocator();
      log.info("created server: " + locatorURI);
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Should return exception.
      assertTrue(client.invoke("abc") instanceof TestException);
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   public void testMetadataOverridesConfigMap() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false, true);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(HTTPMetadataConstants.NO_THROW_ON_ERROR, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Should return exception.
      HashMap metadata = new HashMap();
      metadata.put(HTTPMetadataConstants.NO_THROW_ON_ERROR, "false");
      // Should throw exception.
      try
      {
         client.invoke("abc", metadata);
         fail("should have throw  TestException");
      }
      catch (Exception e)
      {
         assertTrue("should thrown received testException", e instanceof TestException);
      }
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testMetadataOverridesInvokerLocator() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, true);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(HTTPMetadataConstants.NO_THROW_ON_ERROR, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Should return exception.
      HashMap metadata = new HashMap();
      metadata.put(HTTPMetadataConstants.NO_THROW_ON_ERROR, "false");
      
      // Should throw exception.
      try
      {
         client.invoke("abc", metadata);
         fail("should have throw  TestException");
      }
      catch (Exception e)
      {
         assertTrue("should thrown received testException", e instanceof TestException);
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
   

   protected void setupServer(boolean addNoThrow, boolean noThrow) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port;
      if (addNoThrow)
      {
         locatorURI += "/?" + HTTPMetadataConstants.NO_THROW_ON_ERROR + "=" + noThrow;
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
   
   
   public static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         throw new TestException();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   static class TestException extends Exception {}
}