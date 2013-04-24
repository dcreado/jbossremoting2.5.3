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
package org.jboss.test.remoting.multihome;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Enumeration;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.Home;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.AddressUtil;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


public abstract class MultihomeTestParent extends TestCase
{
   protected static Logger log = Logger.getLogger(MultihomeTestParent.class);
   
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
   
   
   public void testConnectToEachHome() throws Throwable
   {
      log.info("entering " + getName());

      // Start server.
      setupServer();

      Iterator it = serverLocator.getConnectHomeList().iterator();
      while (it.hasNext())
      {
         // Create client.
         Home h = (Home) it.next();
         String path = serverLocator.getPath();
         Map parameters = serverLocator.getParameters();
         InvokerLocator clientLocator = new InvokerLocator(getTransport(), h.host, h.port, path, parameters);
         log.info("locator: " + clientLocator);
         HashMap clientConfig = new HashMap();
         clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
         addExtraClientConfig(clientConfig);
         Client client = new Client(clientLocator, clientConfig);
         client.connect();
         log.info("client is connected to " + clientLocator);

         // Test invocations.
         assertEquals("abc", client.invoke("abc"));
         log.info("invocation successful");
         
         // Test callbacks.
         Map metadata = new HashMap();
         addExtraCallbackConfig(metadata);
         TestCallbackHandler callbackHandler = new TestCallbackHandler();
         client.addListener(callbackHandler, metadata, null, true);
         assertTrue(callbackHandler.ok);
         log.info("callback successful");
         client.removeListener(callbackHandler);
         client.disconnect();
      }

      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testConnectToAnyHome() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected to " + clientLocator);
      
      // Test ivocations.
      assertEquals("abc", client.invoke("abc"));
      log.info("invocation successful");
      
      // Test callbacks.
      Map metadata = new HashMap();
      addExtraCallbackConfig(metadata);
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, metadata, null, true);
      assertTrue(callbackHandler.ok);
      log.info("callback successful");
      
      client.removeListener(callbackHandler);
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   public void testInvocationHandlersAreShared() throws Throwable
   {
      log.info("entering " + getName());

      // Start server.
      setupServer();
      
      List homes = serverLocator.getConnectHomeList();
      for (int i = 0; i < homes.size(); i++)
      {
         // Create client.
         Home h = (Home) homes.get(i);
         String path = serverLocator.getPath();
         Map parameters = serverLocator.getParameters();
         InvokerLocator clientLocator = new InvokerLocator(getTransport(), h.host, h.port, path, parameters);
         log.info("locator: " + clientLocator);
         HashMap clientConfig = new HashMap();
         clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
         addExtraClientConfig(clientConfig);
         Client client = new Client(clientLocator, clientConfig);
         client.connect();
         log.info("client is connected to " + clientLocator);
         
         if (i == 0)
            client.invoke("reset");
         
         // Test invocations.
         assertEquals("count", client.invoke("count"));
         log.info("invocation successful");
         
         // Test callbacks.
         Map metadata = new HashMap();
         addExtraCallbackConfig(metadata);
         TestCallbackHandler callbackHandler = new TestCallbackHandler();
         client.addListener(callbackHandler, metadata, null, true);
         assertTrue(callbackHandler.ok);
         log.info("callback successful");
         
         // Verify that a single ServerInvocationHandler handles invocation
         // for all interfaces.
         Integer counter = (Integer) client.invoke("getCounter");
         assertEquals(i + 1, counter.intValue());
         
         client.removeListener(callbackHandler);
         client.disconnect();
      }
      
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected abstract String getTransport();

   
   protected String getPath() {return "";}
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   protected void addExtraCallbackConfig(Map config) {}
   

   protected void setupServer() throws Exception
   {
      StringBuffer sb = new StringBuffer();
      Enumeration e1 = NetworkInterface.getNetworkInterfaces();
      boolean first = true;
      int counter = 0;
      
      loop: while (e1.hasMoreElements())
      {
         NetworkInterface iface = (NetworkInterface) e1.nextElement();
         Enumeration e2 = iface.getInetAddresses();
         while (e2.hasMoreElements())
         {
            InetAddress address = (InetAddress) e2.nextElement();
            String host = address.getHostAddress();
            if (host.indexOf(':') != host.lastIndexOf(':'))
               host = "[" + host + "]";
            if (AddressUtil.checkAddress(host))
            {
               log.info("host is functional: " + host);
               int port = PortUtil.findFreePort(host);
               if (first)
                  first = false;
               else
                  sb.append('!');
               sb.append(host).append(':').append(port);
               if (++counter > 10) break loop;
            }
            else
            {
               log.info("skipping host: " + host);
            }
         }
      }
      
      locatorURI = getTransport() + "://" + InvokerLocator.MULTIHOME + getPath() + "/?";
      locatorURI += InvokerLocator.HOMES_KEY + "=" + sb.toString() + "&";
      locatorURI += InvokerLocator.CONNECT_HOMES_KEY + "=" + sb.toString();
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
   
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      boolean ok;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.info("received callback");
         ok = true;
      }  
   }
}