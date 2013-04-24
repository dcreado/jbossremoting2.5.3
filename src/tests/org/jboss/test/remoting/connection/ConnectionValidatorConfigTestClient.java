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
package org.jboss.test.remoting.connection;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.jboss.remoting.AbstractInvoker;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.ConnectionValidator;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;


/**
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 4742 $
 * <p>
 * Copyright Jun 15, 2007
 * </p>
 */
public class ConnectionValidatorConfigTestClient extends TestCase
{
   public static int port;
   
   private static Logger log = Logger.getLogger(ConnectionValidatorConfigTestClient.class);
   private static boolean firstTime = true;
   private static InvokerLocator serverLocator;

   
   /**
    * Sets up target remoting server.
    */
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false; 
         serverLocator = new InvokerLocator(ConnectionValidatorConfigTestServer.serverLocatorURI);
      }
   }

   
   public void tearDown()
   {
   }
   
   
   /**
    * Verifies that the default values are set correctly if a call is made to
    * Client.addConnectionListener(ConnectionListener listener).
    */
   public void testDefaultConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(ConnectionValidator.DEFAULT_PING_PERIOD, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that pingPeriod is set correctly if a call is made to
    * Client.addConnectionListener(ConnectionListener listener, int pingPeriod).
    */
   public void testSetPingPeriod() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, 3456);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(3456, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that pingPeriod is set correctly if it appears in Client
    * configuration map but Client.addConnectionListener(ConnectionListener listener)
    * puts default value in metadata map.
    */
   public void testSetPingPeriodByClientConfigUsingSingleArgMethod() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_PERIOD, "3468");
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(3468, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
  
   
   /**
    * Verifies that pingPeriod is set correctly if it appears in Client
    * configuration map.
    */
   public void testSetPingPeriodByClientConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_PERIOD, "3468");
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(3468, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that pingPeriod is set correctly if a call is made to
    * Client.addConnectionListener(ConnectionListener listener, Map metadata).
    */
   public void testSetPingPeriodByMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      metadata.put(ConnectionValidator.VALIDATOR_PING_PERIOD, "3467");
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(3467, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that pingPeriod is set correctly if it appears in both Client
    * configuration map and metadata map.
    */
   public void testSetPingPeriodByClientConfigAndMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_PERIOD, "3413");
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      metadata.put(ConnectionValidator.VALIDATOR_PING_PERIOD, "3414");
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(3414, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that timeout is set correctly if it is set in Client
    * configuration map.
    */
   public void testSetTimeoutByClientConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_TIMEOUT, "3546");
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(ConnectionValidator.DEFAULT_PING_PERIOD, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals("3546", o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);     
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that "timeout" in Client configuration map and metadata is ignored.
    */
   public void testDefaultTimeoutWithStandardTimeoutInClientConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      clientConfig.put(ServerInvoker.TIMEOUT, ConnectionValidator.DEFAULT_PING_TIMEOUT + "0");
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      metadata.put(ServerInvoker.TIMEOUT, ConnectionValidator.DEFAULT_PING_TIMEOUT + "1");
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(ConnectionValidator.DEFAULT_PING_PERIOD, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);     
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that timeout is set correctly if a call is made to
    * Client.addConnectionListener(ConnectionListener listener, Map metadata).
    */
   public void testSetTimeoutByMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      metadata.put(ConnectionValidator.VALIDATOR_PING_TIMEOUT, "3478");
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(ConnectionValidator.DEFAULT_PING_PERIOD, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals("3478", o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);     
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that timeout is set correctly if "validatorPingTimeout" appears in
    * both Client configuration map and metadata map.
    */
   public void testSetTimeoutByClientConfigAndMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_TIMEOUT, "3167");
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      metadata.put(ConnectionValidator.VALIDATOR_PING_TIMEOUT, "3168");
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(ConnectionValidator.DEFAULT_PING_PERIOD, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals("3168", o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);     
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that number of ping retries is set correctly if it appears in
    * Client configuratin map.
    */
   public void testSetPingRetriesClientConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put("NumberOfCallRetries", "13");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(ConnectionValidator.DEFAULT_PING_PERIOD, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      assertEquals(13, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that number of ping retries is set correctly if a call is made to
    * Client.addConnectionListener(ConnectionListener listener, Map metadata).
    */
   public void testSetPingRetriesByMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      metadata.put("NumberOfCallRetries", "7");
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(ConnectionValidator.DEFAULT_PING_PERIOD, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      assertEquals(7, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that number of ping retries is set correctly if it appears in both
    * Client configuration map and metadata.
    */
   public void testSetPingRetriesByClientConfigAndMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put("NumberOfCallRetries", "17");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      metadata.put("NumberOfCallRetries", "19");
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(ConnectionValidator.DEFAULT_PING_PERIOD, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      assertEquals(19, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that number of connection retries is set correctly if it appears
    * in Client configuration map.
    */
   public void testSetConnectionRetriesByClientConfig() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put("NumberOfRetries", "21");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(ConnectionValidator.DEFAULT_PING_PERIOD, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that number of connection retries is set correctly if a call is made to
    * Client.addConnectionListener(ConnectionListener listener, Map metadata).
    */
   public void testSetConnectionRetriesByMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      metadata.put("NumberOfRetries", "27");
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(ConnectionValidator.DEFAULT_PING_PERIOD, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that number of connection retries is set correctly if it appears
    * in Client configuration map.
    */
   public void testSetConnectionRetriesByClientConfigAndMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put("NumberOfRetries", "31");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      metadata.put("NumberOfRetries", "33");
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(ConnectionValidator.DEFAULT_PING_PERIOD, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());
         
      client.disconnect();
   }
   
   
   /**
    * Verifies that other parameters may be set by passing in metadata map.
    */
   public void testOtherParamsByMetadata() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      HashMap clientConfig = new HashMap();
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Add connection listener.
      Map metadata = new HashMap();
      metadata.put("ReuseAddress", "false");
      ConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener, metadata);
      
      // Test pingPeriod.
      Field field = Client.class.getDeclaredField("connectionValidator");
      field.setAccessible(true);
      ConnectionValidator validator = (ConnectionValidator) field.get(client);
      field = ConnectionValidator.class.getDeclaredField("pingPeriod");
      field.setAccessible(true);
      long pingPeriod = ((Long) field.get(validator)).longValue();
      assertEquals(ConnectionValidator.DEFAULT_PING_PERIOD, pingPeriod);
      
      // Test timeout.
      field = ConnectionValidator.class.getDeclaredField("clientInvoker");
      field.setAccessible(true);
      AbstractInvoker invoker = (AbstractInvoker) field.get(validator);
      field = AbstractInvoker.class.getDeclaredField("configuration");
      field.setAccessible(true);
      Map config = (Map) field.get(invoker);
      Object o = config.get(ServerInvoker.TIMEOUT);
      assertEquals(ConnectionValidator.DEFAULT_PING_TIMEOUT, o);
      
      // Test ping retries.
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      MicroSocketClientInvoker socketInvoker = (MicroSocketClientInvoker) invoker;
      int defaultPingRetries = Integer.parseInt(ConnectionValidator.DEFAULT_NUMBER_OF_PING_RETRIES);
      assertEquals(defaultPingRetries, socketInvoker.getNumberOfCallRetries());

      // Test ReuseAddress.
      assertFalse(socketInvoker.getReuseAddress());
         
      client.disconnect();
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) {return invocation.getParameter();}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }

   static class TestConnectionListener implements ConnectionListener
   {
      public void handleConnectionException(Throwable throwable, Client client) {}
   }
}