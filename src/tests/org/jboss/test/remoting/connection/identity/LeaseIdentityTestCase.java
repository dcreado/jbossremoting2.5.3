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

package org.jboss.test.remoting.connection.identity;

import java.net.InetAddress;
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
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.LeasePinger;
import org.jboss.remoting.MicroRemoteClientInvoker;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.util.id.GUID;


/**
 * Unit test for JBREM-1133.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 08, 2009
 * </p>
 */
public class LeaseIdentityTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(LeaseIdentityTestCase.class);
   
   protected static long LEASE_PERIOD = 2000;
   protected static String LEASE_PERIOD_STRING = "2000";
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;
   protected TestConnectionListener listener;

   
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
   
   
   public void testDefault() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(false, null);
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Test lease behavior.
      MicroRemoteClientInvoker clientInvoker = (MicroRemoteClientInvoker) client.getInvoker();
      clientInvoker.terminateLease(client.getSessionId(), 0);
      TestLeasePinger leasePinger = new TestLeasePinger(clientInvoker, clientInvoker.getSessionId(), LEASE_PERIOD);
      leasePinger.setLeasePingerId(new GUID().toString());
      leasePinger.addClient(client.getSessionId(), client.getConfiguration(), LEASE_PERIOD);
      leasePinger.startPing();
      Thread.sleep(LEASE_PERIOD * 4);
      assertFalse(listener.called);
      
      leasePinger.stopPing();
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testClientConnectionIdentityFalse() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, "false");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Test lease behavior.
      MicroRemoteClientInvoker clientInvoker = (MicroRemoteClientInvoker) client.getInvoker();
      clientInvoker.terminateLease(client.getSessionId(), 0);
      TestLeasePinger leasePinger = new TestLeasePinger(clientInvoker, clientInvoker.getSessionId(), LEASE_PERIOD);
      leasePinger.setLeasePingerId(new GUID().toString());
      leasePinger.addClient(client.getSessionId(), client.getConfiguration(), LEASE_PERIOD);
      leasePinger.startPing();
      Thread.sleep(LEASE_PERIOD * 4);
      assertFalse(listener.called);
      
      leasePinger.stopPing();
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testClientConnectionIdentityTrue() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer(true, "true");
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Test lease behavior.
      MicroRemoteClientInvoker clientInvoker = (MicroRemoteClientInvoker) client.getInvoker();
      clientInvoker.terminateLease(client.getSessionId(), 0);
      TestLeasePinger leasePinger = new TestLeasePinger(clientInvoker, clientInvoker.getSessionId(), LEASE_PERIOD);
      leasePinger.setLeasePingerId(new GUID().toString());
      leasePinger.addClient(client.getSessionId(), client.getConfiguration(), LEASE_PERIOD);
      leasePinger.startPing();
      Thread.sleep(LEASE_PERIOD * 4);
      assertTrue(listener.called);
      assertNull(listener.throwable);
      
      leasePinger.stopPing();
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
   

   protected void setupServer(boolean setUseClientIdentity, String useClientIdentity) throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port;
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "/?" + metadata;
      }
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put("leasePeriod", LEASE_PERIOD_STRING);
      if (setUseClientIdentity)
      {
         config.put(Remoting.USE_CLIENT_CONNECTION_IDENTITY, useClientIdentity);
      }
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
      listener = new TestConnectionListener();
      connector.addConnectionListener(listener);
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
   

   static class TestConnectionListener implements ConnectionListener
   {
      public boolean called;
      public Throwable throwable;
      
      public void handleConnectionException(Throwable throwable, Client client)
      {
         called = true;
         this.throwable = throwable;
         log.info("called: throwable = " + throwable);
      }  
   }
   
   
   static class TestLeasePinger extends LeasePinger
   {
      public TestLeasePinger(ClientInvoker invoker, String invokerSessionID, long defaultLeasePeriod)
      {
         super(invoker, invokerSessionID, defaultLeasePeriod);
      }

      public void setLeasePingerId(String leasePingerId)
      {
         super.setLeasePingerId(leasePingerId);
      }
   }
}