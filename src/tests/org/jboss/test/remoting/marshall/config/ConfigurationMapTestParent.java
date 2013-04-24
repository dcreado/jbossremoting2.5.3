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

package org.jboss.test.remoting.marshall.config;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit test for JBREM-1102.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Mar 20, 2009
 * </p>
 */
public abstract class ConfigurationMapTestParent extends TestCase
{
   private static Logger log = Logger.getLogger(ConfigurationMapTestParent.class);
   
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
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.TRACE);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }
      
      ConfigTestMarshaller.reset();
      ConfigTestUnmarshaller.reset();
      LocatorTestMarshaller.reset();
      LocatorTestUnmarshaller.reset();
   }

   
   public void tearDown()
   {
   }
   
   
   public void testDatatypeConfigDefault() throws Throwable
   {
      log.info("entering " + getName());
      
      // Cache marshaller/unmarshaller.
      MarshalFactory.addMarshaller("config", new ConfigTestMarshaller(), new ConfigTestUnmarshaller());
      
      // Start server.
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.DATATYPE, "config");
      setupServer("x=y", serverConfig);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(InvokerLocator.DATATYPE, "config");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Do tests.
      assertTrue(ConfigTestMarshaller.ok(false, marshallerDatatypeUnused()));
      assertTrue(ConfigTestUnmarshaller.ok(false, 0));
      assertTrue(LocatorTestMarshaller.ok());
      assertTrue(LocatorTestUnmarshaller.ok());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testDatatypePassConfigMapFalse() throws Throwable
   {
      log.info("entering " + getName());
      
      // Cache marshaller/unmarshaller.
      MarshalFactory.addMarshaller("config", new ConfigTestMarshaller(), new ConfigTestUnmarshaller());
      
      // Start server.
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.DATATYPE, "config");
      serverConfig.put(Remoting.PASS_CONFIG_MAP_TO_MARSHAL_FACTORY, "false");
      setupServer("x=y", serverConfig);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(InvokerLocator.DATATYPE, "config");
      clientConfig.put(Remoting.PASS_CONFIG_MAP_TO_MARSHAL_FACTORY, "false");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Do tests.
      assertTrue(ConfigTestMarshaller.ok(false, marshallerDatatypeUnused()));
      assertTrue(ConfigTestUnmarshaller.ok(false, 0));
      assertTrue(LocatorTestMarshaller.ok());
      assertTrue(LocatorTestUnmarshaller.ok());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testDatatypePassConfigMapTrue() throws Throwable
   {
      log.info("entering " + getName());
      
      // Cache marshaller/unmarshaller.
      MarshalFactory.addMarshaller("config", new ConfigTestMarshaller(), new ConfigTestUnmarshaller());
      
      // Start server.
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.DATATYPE, "config");
      serverConfig.put(Remoting.PASS_CONFIG_MAP_TO_MARSHAL_FACTORY, "true");
      setupServer("x=y", serverConfig);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(InvokerLocator.DATATYPE, "config");
      clientConfig.put(Remoting.PASS_CONFIG_MAP_TO_MARSHAL_FACTORY, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Do tests.
      assertTrue(ConfigTestMarshaller.ok(true, marshallerCountDatatype()));
      assertTrue(ConfigTestUnmarshaller.ok(true, unmarshallerCountDatatype()));
      assertTrue(LocatorTestMarshaller.ok());
      assertTrue(LocatorTestUnmarshaller.ok());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testFQNConfigDefault() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.MARSHALLER, ConfigTestMarshaller.class.getName());
      serverConfig.put(InvokerLocator.UNMARSHALLER, ConfigTestUnmarshaller.class.getName());
      setupServer("x=y", serverConfig);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(InvokerLocator.MARSHALLER, ConfigTestMarshaller.class.getName());
      clientConfig.put(InvokerLocator.UNMARSHALLER, ConfigTestUnmarshaller.class.getName());
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Do tests.
      assertTrue(ConfigTestMarshaller.ok(false,marshallerFQNUnused()));
      assertTrue(ConfigTestUnmarshaller.ok(false, 0));
      assertTrue(LocatorTestMarshaller.ok());
      assertTrue(LocatorTestUnmarshaller.ok());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   
   
   public void testFQNPassConfigMapFalse() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.MARSHALLER, ConfigTestMarshaller.class.getName());
      serverConfig.put(InvokerLocator.UNMARSHALLER, ConfigTestUnmarshaller.class.getName());
      serverConfig.put(Remoting.PASS_CONFIG_MAP_TO_MARSHAL_FACTORY, "false");
      setupServer("x=y", serverConfig);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(InvokerLocator.MARSHALLER, ConfigTestMarshaller.class.getName());
      clientConfig.put(InvokerLocator.UNMARSHALLER, ConfigTestUnmarshaller.class.getName());
      clientConfig.put(Remoting.PASS_CONFIG_MAP_TO_MARSHAL_FACTORY, "false");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Do tests.
      assertTrue(ConfigTestMarshaller.ok(false, marshallerFQNUnused()));
      assertTrue(ConfigTestUnmarshaller.ok(false, 0));
      assertTrue(LocatorTestMarshaller.ok());
      assertTrue(LocatorTestUnmarshaller.ok());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testFQNConfigPassConfigMapTrue() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.MARSHALLER, ConfigTestMarshaller.class.getName());
      serverConfig.put(InvokerLocator.UNMARSHALLER, ConfigTestUnmarshaller.class.getName());
      serverConfig.put(Remoting.PASS_CONFIG_MAP_TO_MARSHAL_FACTORY, "true");
      setupServer("x=y", serverConfig);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(InvokerLocator.MARSHALLER, ConfigTestMarshaller.class.getName());
      clientConfig.put(InvokerLocator.UNMARSHALLER, ConfigTestUnmarshaller.class.getName());
      clientConfig.put(Remoting.PASS_CONFIG_MAP_TO_MARSHAL_FACTORY, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Do tests.
      assertTrue(ConfigTestMarshaller.ok(true, marshallerCountFQN()));
      assertTrue(ConfigTestUnmarshaller.ok(true, unmarshallerCountFQN()));
      assertTrue(LocatorTestMarshaller.ok());
      assertTrue(LocatorTestUnmarshaller.ok());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   

   protected int marshallerCountDatatype()
   {
      return 6;
   }
   
   protected int unmarshallerCountDatatype()
   {
      return 4;
   }
   
   protected int marshallerCountFQN()
   {
      return 3;
   }
   
   protected int unmarshallerCountFQN()
   {
      return 2;
   }
   
   protected int marshallerDatatypeUnused()
   {
      return 2;
   }

   protected int marshallerFQNUnused()
   {
      return 1;
   }
   
   protected abstract String getTransport();
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer(String parameter, Map extraConfig) throws Exception
   {
      log.info("parameter: " + parameter);
      log.info("extraConfig: " + extraConfig);
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port + "/?" + parameter;
//      locatorURI += "&serializationtype=jboss";
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "&" + metadata;
      }
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      if (extraConfig != null)
      {
         config.putAll(extraConfig);
      }
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = getInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
   
   
   protected TestInvocationHandler getInvocationHandler()
   {
      return new TestInvocationHandler();
   }
}