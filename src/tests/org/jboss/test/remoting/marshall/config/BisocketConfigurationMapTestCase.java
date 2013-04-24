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

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.marshal.MarshalFactory;

/**
 * Unit tests for JBREM-1102.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Mar 21, 2009
 * </p>
 */
public class BisocketConfigurationMapTestCase extends ConfigurationMapTestParent
{
   private static Logger log = Logger.getLogger(BisocketConfigurationMapTestCase.class);
   
   protected String getTransport()
   {
      return "bisocket";
   }
   
   public void testDatatypeConfigWithCallbacksDefault() throws Throwable
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
      
      // Configure callbacks.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      
      // Do tests.
      assertTrue(ConfigTestMarshaller.ok(false, 4));
      assertTrue(ConfigTestUnmarshaller.ok(false, 0));
      assertTrue(LocatorTestMarshaller.ok());
      assertTrue(LocatorTestUnmarshaller.ok());
      assertEquals(1, callbackHandler.counter);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testDatatypeConfigWithCallbacksPassConfigMapFalse() throws Throwable
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
      
      // Configure callbacks.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      
      // Do tests.
      assertTrue(ConfigTestMarshaller.ok(false, 4));
      assertTrue(ConfigTestUnmarshaller.ok(false, 0));
      assertTrue(LocatorTestMarshaller.ok());
      assertTrue(LocatorTestUnmarshaller.ok());
      assertEquals(1, callbackHandler.counter);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testDatatypeConfigWithCallbacksPassConfigMapTrue() throws Throwable
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
      
      // Configure callbacks.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      
      // Do tests.
      assertTrue(ConfigTestMarshaller.ok(true, 12));
      assertTrue(ConfigTestUnmarshaller.ok(true, 8));
      assertTrue(LocatorTestMarshaller.ok());
      assertTrue(LocatorTestUnmarshaller.ok());
      assertEquals(1, callbackHandler.counter);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testFQNConfigWithCallbacksDefault() throws Throwable
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
      
      // Do callback.
      // Configure callbacks.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      
      // Do tests.
      assertTrue(ConfigTestMarshaller.ok(false, 2));
      assertTrue(ConfigTestUnmarshaller.ok(false, 0));
      assertTrue(LocatorTestMarshaller.ok());
      assertTrue(LocatorTestUnmarshaller.ok());
      assertEquals(1, callbackHandler.counter);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testFQNConfigWithCallbacksPassConfigMapFalse() throws Throwable
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
      
      // Do callback.
      // Configure callbacks.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      
      // Do tests.
      assertTrue(ConfigTestMarshaller.ok(false, 2));
      assertTrue(ConfigTestUnmarshaller.ok(false, 0));
      assertTrue(LocatorTestMarshaller.ok());
      assertTrue(LocatorTestUnmarshaller.ok());
      assertEquals(1, callbackHandler.counter);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testFQNConfigWithCallbacksPassConfigMapTrue() throws Throwable
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
      
      // Do callback.
      // Configure callbacks.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      
      // Do tests.
      assertTrue(ConfigTestMarshaller.ok(true, 6));
      assertTrue(ConfigTestUnmarshaller.ok(true, 4));
      assertTrue(LocatorTestMarshaller.ok());
      assertTrue(LocatorTestUnmarshaller.ok());
      assertEquals(1, callbackHandler.counter);
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected TestInvocationHandler getInvocationHandler()
   {
      return new BisocketTestInvocationHandler();
   }
   
   
   static class BisocketTestInvocationHandler extends TestInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         try
         {
            callbackHandler.handleCallback(new Callback("callback"));
         }
         catch (HandleCallbackException e)
         {
            log.error("Error sending callback", e);
         }
      }
   }
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      public int counter;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         counter++;
         log.info("received callback");
      }  
   }
}

