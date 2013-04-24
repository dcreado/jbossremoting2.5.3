/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.test.remoting.transport.http.marshal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;

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
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.http.HTTPMarshaller;
import org.jboss.remoting.marshal.http.HTTPUnMarshaller;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.http.HTTPMetadataConstants;


/**
 * Unit tests for JBREM-1145.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Aug 17, 2009
 * </p>
 */
public class HttpContentTypeTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(HttpContentTypeTestCase.class);
   
   protected static boolean firstTime = true;
   
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
         Logger.getLogger("org.jboss.remoting").setLevel(Level.TRACE);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }
      
      TestMarshaller.marshallers.clear();
      TestUnMarshaller.unmarshallers.clear();
      MarshalFactory.addMarshaller("test", new TestMarshaller(), new TestUnMarshaller());
   }

   
   public void tearDown()
   {
   }
   
   
   public void testOrdinaryInvocationDefault() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false, false);
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      log.info("connecting to: " + serverLocator);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected to " + serverLocator);
      
      // Do invocation.
      Object o = client.invoke("abc");
      
      // Show that an InvocationResponse was incorrectly returned as a Strin..
      assertTrue(o instanceof String);
      String result = (String) o;
      assertTrue(result.indexOf("org.jboss.remoting.InvocationResponse") >= 0);
      
      // Check remoting content type handling.
      validateOrdinaryInvocation(client);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testOrdinaryInvocationRemotingContentTypeFalse() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, false);
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected to " + serverLocator);
      
      // Do invocation.
      Object o = client.invoke("abc");
      
      // Show that an InvocationResponse was incorrectly returned as a String.
      assertTrue(o instanceof String);
      String result = (String) o;
      assertTrue(result.indexOf("org.jboss.remoting.InvocationResponse") >= 0);
      
      // Check remoting content type handling.
      validateOrdinaryInvocation(client);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testOrdinaryInvocationRemotingContentTypeTrue() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, true);
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected to " + serverLocator);
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Check remoting content type handling.
      validateOrdinaryInvocation(client);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testRawStringMessageDefault() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false, false);
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected to " + serverLocator);
      
      // Test connections.
      Map metadata = new HashMap();
      metadata.put(Client.RAW, "true");
      assertEquals("abc", client.invoke("abc", metadata));
      log.info("connection is good");
      
      // Check remoting content type handling.
      validateRawStringMessage(client);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testRawStringMessageRemotingContentTypeFalse() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, false);
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected to " + serverLocator);
      
      // Test connections.
      Map metadata = new HashMap();
      metadata.put(Client.RAW, "true");
      assertEquals("abc", client.invoke("abc", metadata));
      log.info("connection is good");
      
      // Check remoting content type handling.
      validateRawStringMessage(client);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testRawStringMessageRemotingContentTypeTrue() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, true);
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected to " + serverLocator);
      
      // Test connections.
      Map metadata = new HashMap();
      metadata.put(Client.RAW, "true");
      assertEquals("abc", client.invoke("abc", metadata));
      log.info("connection is good");
      
      // Check remoting content type handling.
      validateRawStringMessage(client);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected void validateOrdinaryInvocation(Client client) throws Throwable
   {
      assertEquals(6, TestMarshaller.marshallers.size());    
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING, ((TestMarshaller)TestMarshaller.marshallers.get(3)).type);
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING, ((TestMarshaller)TestMarshaller.marshallers.get(5)).type);
      assertEquals(4, TestUnMarshaller.unmarshallers.size());
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING, ((TestUnMarshaller)TestUnMarshaller.unmarshallers.get(1)).type);
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING, ((TestUnMarshaller)TestUnMarshaller.unmarshallers.get(3)).type);
   }
   
   
   protected void validateRawStringMessage(Client client) throws Throwable
   {
      assertEquals(6, TestMarshaller.marshallers.size());    
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING, ((TestMarshaller)TestMarshaller.marshallers.get(3)).type);
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING, ((TestMarshaller)TestMarshaller.marshallers.get(5)).type);
      assertEquals(4, TestUnMarshaller.unmarshallers.size());
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING, ((TestUnMarshaller)TestUnMarshaller.unmarshallers.get(1)).type);
      assertEquals(HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING, ((TestUnMarshaller)TestUnMarshaller.unmarshallers.get(3)).type);
   }
   
   
   protected String getTransport()
   {
      return "http";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer(boolean addUseRemotingContentType, boolean useRemotingContentType) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port;
      locatorURI += "/?datatype=test";
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "&" + metadata;
      }
      if (addUseRemotingContentType)
      {
         locatorURI += "&useRemotingContentType=" + useRemotingContentType;
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
         Map responseMap = invocation.getReturnPayload();
         responseMap.put(HTTPMetadataConstants.CONTENTTYPE, "text/html");
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   

   public static class TestMarshaller extends HTTPMarshaller
   {
      public static ArrayList marshallers = new ArrayList();
      private static final long serialVersionUID = -7528137229006015488L;
      public String type;
      
      public void write(Object dataObject, OutputStream output, int version) throws IOException
      {
         log.info(this + " writing " + dataObject);
         type = (dataObject instanceof String) ? HTTPMetadataConstants.REMOTING_CONTENT_TYPE_STRING : HTTPMetadataConstants.REMOTING_CONTENT_TYPE_NON_STRING;
         super.write(dataObject, output, version);
      }
      
      public Marshaller cloneMarshaller() throws CloneNotSupportedException
      {
         TestMarshaller marshaller = new TestMarshaller();
         marshallers.add(marshaller);
         log.info("returning " + marshaller);
         return marshaller;
      }
   }
   
   public static class TestUnMarshaller extends HTTPUnMarshaller
   {
      public static ArrayList unmarshallers = new ArrayList();
      private static final long serialVersionUID = -6422222480047910351L;
      public String type;
      
      public Object read(InputStream inputStream, Map metadata, int version) throws IOException, ClassNotFoundException
      {
         Object o = metadata.get(HTTPMetadataConstants.REMOTING_CONTENT_TYPE);
         if (o instanceof List)
         {
            type = (String) ((List) o).get(0);
         }
         else if (o instanceof String)
         {
            type = (String) o;
         }
         else 
         {
            log.warn(this + " unrecognized remotingContentType: " + o);
         }
         
         o = super.read(inputStream, metadata, version);
         log.info(this + " read " + o);
         return o;
      }
      
      public UnMarshaller cloneUnMarshaller() throws CloneNotSupportedException
      {
         TestUnMarshaller unmarshaller = new TestUnMarshaller();
         unmarshallers.add(unmarshaller);
         unmarshaller.setClassLoader(this.customClassLoader);
         log.info("returning " + unmarshaller);
         return unmarshaller;
      }
   }
}