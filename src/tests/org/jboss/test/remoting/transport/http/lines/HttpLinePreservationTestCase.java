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
package org.jboss.test.remoting.transport.http.lines;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.marshal.http.HTTPUnMarshaller;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;
import org.jboss.remoting.transport.web.WebUtil;


/**
 * 
 * Unit tests for JBREM-809.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Feb 8, 2008
 * </p>
 */
public class HttpLinePreservationTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(HttpLinePreservationTestCase.class);
   
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
   
   
   public void testLinePreservationClientConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, "true");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(HTTPUnMarshaller.PRESERVE_LINES, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      Properties headerProps = new Properties();
      headerProps.put(HTTPMetadataConstants.CONTENTTYPE, WebUtil.PLAIN);
      HashMap metadata = new HashMap();
      metadata.put("HEADER", headerProps);
      metadata.put(Client.RAW, "true");
      
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < 1000; i++)
         sb.append("0123456789").append("\r\n");
      String message = sb.toString();
      assertEquals(message, client.invoke(message, metadata));
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testLinePreservationClientMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, "true");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      Properties headerProps = new Properties();
      headerProps.put(HTTPMetadataConstants.CONTENTTYPE, WebUtil.PLAIN);
      HashMap metadata = new HashMap();
      metadata.put("HEADER", headerProps);
      metadata.put(Client.RAW, "true");
      metadata.put(HTTPUnMarshaller.PRESERVE_LINES, "true");
      
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < 1000; i++)
         sb.append("0123456789");
      String message = sb.toString();
      assertEquals(message, client.invoke(message, metadata));
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNoLinePreservationClientConfigServerConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, "false");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(HTTPUnMarshaller.PRESERVE_LINES, "false");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      Properties headerProps = new Properties();
      headerProps.put(HTTPMetadataConstants.CONTENTTYPE, WebUtil.PLAIN);
      HashMap metadata = new HashMap();
      metadata.put("HEADER", headerProps);
      metadata.put(Client.RAW, "true");
      
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < 1000; i++)
         sb.append("0123456789").append("\r\n");
      String sent = sb.toString();
      sb = new StringBuffer();
      for (int i = 0; i < 1000; i++)
         sb.append("0123456789");
      String received = sb.toString();
      assertEquals(received, client.invoke(sent, metadata));
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNoLinePreservationClientMetadataServerConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, "false");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      Properties headerProps = new Properties();
      headerProps.put(HTTPMetadataConstants.CONTENTTYPE, WebUtil.PLAIN);
      HashMap metadata = new HashMap();
      metadata.put("HEADER", headerProps);
      metadata.put(Client.RAW, "true");
      metadata.put(HTTPUnMarshaller.PRESERVE_LINES, "false");
      
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < 1000; i++)
         sb.append("0123456789").append("\r\n");
      String sent = sb.toString();
      sb = new StringBuffer();
      for (int i = 0; i < 1000; i++)
         sb.append("0123456789");
      String received = sb.toString();
      assertEquals(received, client.invoke(sent, metadata));
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNoLinePreservationClientConfigServerDefault() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false, null);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(HTTPUnMarshaller.PRESERVE_LINES, "false");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      Properties headerProps = new Properties();
      headerProps.put(HTTPMetadataConstants.CONTENTTYPE, WebUtil.PLAIN);
      HashMap metadata = new HashMap();
      metadata.put("HEADER", headerProps);
      metadata.put(Client.RAW, "true");
      
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < 1000; i++)
         sb.append("0123456789").append("\r\n");
      String sent = sb.toString();
      sb = new StringBuffer();
      for (int i = 0; i < 1000; i++)
         sb.append("0123456789");
      String received = sb.toString();
      assertEquals(received, client.invoke(sent, metadata));
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNoLinePreservationClientMetadataServerDefault() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false, null);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      Properties headerProps = new Properties();
      headerProps.put(HTTPMetadataConstants.CONTENTTYPE, WebUtil.PLAIN);
      HashMap metadata = new HashMap();
      metadata.put("HEADER", headerProps);
      metadata.put(Client.RAW, "true");
      metadata.put(HTTPUnMarshaller.PRESERVE_LINES, "false");
      
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < 1000; i++)
         sb.append("0123456789").append("\r\n");
      String sent = sb.toString();
      sb = new StringBuffer();
      for (int i = 0; i < 1000; i++)
         sb.append("0123456789");
      String received = sb.toString();
      assertEquals(received, client.invoke(sent, metadata));
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testNoLinePreservationClientServerDefault() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false, null);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      Properties headerProps = new Properties();
      headerProps.put(HTTPMetadataConstants.CONTENTTYPE, WebUtil.PLAIN);
      HashMap metadata = new HashMap();
      metadata.put("HEADER", headerProps);
      metadata.put(Client.RAW, "true");
      
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < 1000; i++)
         sb.append("0123456789").append("\r\n");
      String sent = sb.toString();
      sb = new StringBuffer();
      for (int i = 0; i < 1000; i++)
         sb.append("0123456789");
      String received = sb.toString();
      assertEquals(received, client.invoke(sent, metadata));
      
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
   

   protected void setupServer(boolean addConfig, String preserveLines) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      
      if (addConfig)
      {
         config.put(HTTPUnMarshaller.PRESERVE_LINES, preserveLines);
      }
      
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
}