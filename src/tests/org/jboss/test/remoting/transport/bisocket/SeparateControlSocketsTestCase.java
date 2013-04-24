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

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.bisocket.BisocketClientInvoker;


/**
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2735 $
 * <p>
 * Copyright May 31, 2007
 * </p>
 */
public class SeparateControlSocketsTestCase extends TestCase
{
   public static int port;
   
   private static Logger log = Logger.getLogger(SeparateControlSocketsTestCase.class);
   
   private static final String INVOCATION_TEST =     "invocationTest";
   private static final String CALLBACK_TEST =       "callbackTest";
   
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
    * Tests blocking and nonblocking direct calls to Client.getCallbacks().
    */
   public void testSeparateMapForControlSockets() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
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
      log.info("client is connected");
      
      // Test connection.
      assertEquals(INVOCATION_TEST, client.invoke(INVOCATION_TEST));
      
      // Add callback handler and test callback BisocketClientInvoker socket maps.
      TestInvokerCallbackHandler callbackHandler = new TestInvokerCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      assertEquals(1, invocationHandler.listeners.size());
      Iterator it = invocationHandler.listeners.iterator();
      ServerInvokerCallbackHandler serverInvokerCallbackHandler;
      serverInvokerCallbackHandler = (ServerInvokerCallbackHandler) it.next();
      Client callbackClient = serverInvokerCallbackHandler.getCallbackClient();
      assertNotNull(callbackClient);
      assertTrue(callbackClient.getInvoker() instanceof BisocketClientInvoker);
      BisocketClientInvoker callbackClientInvoker;
      callbackClientInvoker = (BisocketClientInvoker) callbackClient.getInvoker();
      Field field = BisocketClientInvoker.class.getDeclaredField("listenerIdToControlSocketsMap");
      field.setAccessible(true);
      Map listenerIdToControlSocketsMap = (Map) field.get(callbackClientInvoker);
      field = BisocketClientInvoker.class.getDeclaredField("listenerIdToSocketsMap");
      field.setAccessible(true);
      Map listenerIdToSocketsMap = (Map) field.get(callbackClientInvoker);
      // A set should have been created to hold control sockets.
      assertEquals(1, listenerIdToControlSocketsMap.size());
      Set controlSockets = (Set) listenerIdToControlSocketsMap.values().iterator().next();
      assertEquals(0, controlSockets.size());
      assertEquals(0, listenerIdToSocketsMap.size());
      
      // Do callback and test socket maps.
      client.invoke(CALLBACK_TEST);
      assertEquals(1, listenerIdToControlSocketsMap.size());
      assertEquals(0, controlSockets.size());
      // A set should have been created to hold ordinary sockets.
      assertEquals(1, listenerIdToSocketsMap.size());
      Set sockets = (Set) listenerIdToSocketsMap.values().iterator().next();
      assertEquals(0, sockets.size());
      
      client.removeListener(callbackHandler);
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
      public Set listeners = new HashSet();
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         listeners.add(callbackHandler);
      }
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         if (CALLBACK_TEST.equals(invocation.getParameter()))
         {
            Iterator it = listeners.iterator();
            while (it.hasNext())
            {
               InvokerCallbackHandler handler = (InvokerCallbackHandler) it.next();
               handler.handleCallback(new Callback("test"));
            }
         }
            
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   static class TestInvokerCallbackHandler implements InvokerCallbackHandler
   {
      public void handleCallback(Callback callback) throws HandleCallbackException {}
   }
}