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
package org.jboss.test.remoting.callback.push;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit tests for JBREM-727.
 *  
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2312 $
 * <p>
 * Copyright Mar 15, 2007
 * </p>
 */
public class ImplicitCallbackConnectorTestCase extends TestCase
{  
   private static final String CALLBACK_TEST =  "callbackTest";
   private static Logger log = Logger.getLogger(ImplicitCallbackConnectorTestCase.class);
   private static boolean firstTime = true;
   
   // remoting server connector
   private Connector connector;
   private SampleInvocationHandler invocationHandler;
   private Client client;

   
   /**
    * Sets up target remoting server and client.
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
     
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(serverConfig);
      connector = new Connector(serverLocator, serverConfig);
      connector.create();
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      log.info("Started remoting server with locator uri of: " + locatorURI);
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(clientConfig);
      client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("Client is connected to " + serverLocator);
   }

   
   /**
    * Shuts down the server and client
    */
   public void tearDown()
   {
      if (client != null)
      {
         client.disconnect();
      }
      if (connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }
   
   
   public void testMultipleCallbackConnectors() throws Throwable
   {
      log.info("entering " + getName());
      
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      int COUNT = 5;
      
      for (int i = 0; i < COUNT; i++)
      {
         client.addListener(callbackHandler, new HashMap(), null, true);
      }
      log.info("client added callback handlers");
      
      Set callbackConnectors = client.getCallbackConnectors(callbackHandler);
      assertNotNull(callbackConnectors);
      assertEquals(5, callbackConnectors.size());
      
      Class[] classes = ServerInvoker.class.getDeclaredClasses();
      Class callbackContainerClass = null;
      for (int i = 0; i < classes.length; i++)
      {
         log.info(classes[i]);
         String fqn = classes[i].getName();
         String className = fqn.substring(fqn.lastIndexOf('.') + 1);
         log.info(className);
         if ("ServerInvoker$CallbackContainer".equals(className))
         {
            callbackContainerClass = classes[i];
            break;
         }
      }
      assertTrue(callbackContainerClass != null);
      Method method = callbackContainerClass.getDeclaredMethod("getCallbackHandler", new Class[] {});
      method.setAccessible(true);
      Field field = ServerInvoker.class.getDeclaredField("clientCallbackListener");
      field.setAccessible(true);
      
      Iterator it = callbackConnectors.iterator();
      while (it.hasNext())
      {
         Connector callbackConnector = (Connector) it.next();
         ServerInvoker serverInvoker = callbackConnector.getServerInvoker();
         Map clientCallbackListener = (Map) field.get(serverInvoker);
         assertEquals(1, clientCallbackListener.size());
         Object callbackContainer = clientCallbackListener.values().iterator().next();
         assertEquals(callbackHandler, method.invoke(callbackContainer, new Object[]{}));
      }
      
      client.invoke(CALLBACK_TEST);
      assertEquals(COUNT, callbackHandler.callbackCounter);
      
      client.removeListener(callbackHandler);
      callbackConnectors = client.getCallbackConnectors(callbackHandler);
      assertNull(callbackConnectors);
   }
   
   
   public void testReusedCallbackConnectorsSameHandler() throws Throwable
   {
      log.info("entering " + getName());
      
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler();
      client.addListener(callbackHandler, new HashMap(), null, true);
      log.info("client added callback handler first time");
      
      Set callbackConnectors = client.getCallbackConnectors(callbackHandler);
      assertNotNull(callbackConnectors);
      assertEquals(1, callbackConnectors.size());
      Connector callbackConnector = (Connector) callbackConnectors.iterator().next();
      client.addListener(callbackHandler, callbackConnector.getLocator());
      log.info("client added callback handler second time");
      callbackConnectors = client.getCallbackConnectors(callbackHandler);
      assertNotNull(callbackConnectors);
      assertEquals(1, callbackConnectors.size());
      
      Class[] classes = ServerInvoker.class.getDeclaredClasses();
      Class callbackContainerClass = null;
      for (int i = 0; i < classes.length; i++)
      {
         log.info(classes[i]);
         String fqn = classes[i].getName();
         String className = fqn.substring(fqn.lastIndexOf('.') + 1);
         log.info(className);
         if ("ServerInvoker$CallbackContainer".equals(className))
         {
            callbackContainerClass = classes[i];
            break;
         }
      }
      assertTrue(callbackContainerClass != null);
      Method method = callbackContainerClass.getDeclaredMethod("getCallbackHandler", new Class[] {});
      method.setAccessible(true);
      Field field = ServerInvoker.class.getDeclaredField("clientCallbackListener");
      field.setAccessible(true);

      ServerInvoker serverInvoker = callbackConnector.getServerInvoker();
      Map clientCallbackListener = (Map) field.get(serverInvoker);
      assertEquals(1, clientCallbackListener.size());
      Object callbackContainer = clientCallbackListener.values().iterator().next();
      assertEquals(callbackHandler, method.invoke(callbackContainer, new Object[]{}));
      
      client.invoke(CALLBACK_TEST);
      assertEquals(1, callbackHandler.callbackCounter);
      
      client.removeListener(callbackHandler);
      callbackConnectors = client.getCallbackConnectors(callbackHandler);
      assertNull(callbackConnectors);
   }
   
   
   public void testReusedCallbackConnectorsDifferentHandlers() throws Throwable
   {
      log.info("entering " + getName());
      
      SimpleCallbackHandler callbackHandler1 = new SimpleCallbackHandler();
      client.addListener(callbackHandler1, new HashMap(), null, true);
      log.info("client added first callback handler");
      
      Set callbackConnectors = client.getCallbackConnectors(callbackHandler1);
      assertNotNull(callbackConnectors);
      assertEquals(1, callbackConnectors.size());
      Connector callbackConnector = (Connector) callbackConnectors.iterator().next();
      SimpleCallbackHandler callbackHandler2 = new SimpleCallbackHandler();
      client.addListener(callbackHandler2, callbackConnector.getLocator());
      callbackConnectors = client.getCallbackConnectors(callbackHandler1);
      assertNotNull(callbackConnectors);
      assertEquals(1, callbackConnectors.size());
      assertNull(client.getCallbackConnectors(callbackHandler2));
      
      Class[] classes = ServerInvoker.class.getDeclaredClasses();
      Class callbackContainerClass = null;
      for (int i = 0; i < classes.length; i++)
      {
         log.info(classes[i]);
         String fqn = classes[i].getName();
         String className = fqn.substring(fqn.lastIndexOf('.') + 1);
         log.info(className);
         if ("ServerInvoker$CallbackContainer".equals(className))
         {
            callbackContainerClass = classes[i];
            break;
         }
      }
      assertTrue(callbackContainerClass != null);
      Method method = callbackContainerClass.getDeclaredMethod("getCallbackHandler", new Class[] {});
      method.setAccessible(true);
      Field field = ServerInvoker.class.getDeclaredField("clientCallbackListener");
      field.setAccessible(true);

      ServerInvoker serverInvoker = callbackConnector.getServerInvoker();
      Map clientCallbackListener = (Map) field.get(serverInvoker);
      assertEquals(2, clientCallbackListener.size());
      Iterator it = clientCallbackListener.values().iterator();
      Object callbackContainer1 = it.next();
      Object callbackContainer2 = it.next();
      assertTrue(
            callbackHandler1.equals(method.invoke(callbackContainer1, new Object[]{})) &&
            callbackHandler2.equals(method.invoke(callbackContainer2, new Object[]{}))
         ||
            callbackHandler1.equals(method.invoke(callbackContainer2, new Object[]{})) &&
            callbackHandler2.equals(method.invoke(callbackContainer1, new Object[]{})));
      
      
      client.invoke(CALLBACK_TEST);
      assertEquals(1, callbackHandler1.callbackCounter);
      assertEquals(1, callbackHandler2.callbackCounter);
      
      client.removeListener(callbackHandler1);
      callbackConnectors = client.getCallbackConnectors(callbackHandler1);
      assertNull(callbackConnectors);
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config)
   {
   }
   
   
   protected void addExtraServerConfig(Map config)
   {
   }
   

   /**
    * Simple invocation handler implementation.  When callback client's are registered, will
    * generate callbacks periodically.
    */
   static class SampleInvocationHandler implements ServerInvocationHandler
   {
      public Set callbackHandlers = new HashSet();
      private int counter = 0;
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         System.out.println("Adding callback listener.");
         callbackHandlers.add(callbackHandler);
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         Object payload = invocation.getParameter();
         if (CALLBACK_TEST.equals(payload))
         {
            try
            {
               Iterator it = callbackHandlers.iterator();
               while (it.hasNext())
               {
                  InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler) it.next();
                  log.info("sending callback: " + ++counter);
                  callbackHandler.handleCallback(new Callback("callback"));
               }
               log.info("sent callback");
            }
            catch (HandleCallbackException e)
            {
               log.error("Unable to send callback");
            }
            return null;
         }
         else
         {
            throw new Exception("unrecognized invocation: " + payload);
         }
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class SimpleCallbackHandler implements InvokerCallbackHandler
   {
      public int callbackCounter;
      private Object lock = new Object();
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         log.debug("received callback: " + callback.getParameter());
         synchronized (lock)
         {
            callbackCounter++;
         }
      }
   }
}