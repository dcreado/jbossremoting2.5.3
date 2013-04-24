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
package org.jboss.test.remoting.transport.bisocket.listenerID;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
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
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.bisocket.BisocketClientInvoker;


/**
 * Unit test for JBREM-785.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2738 $
 * <p>
 * Copyright Aug 2, 2007
 * </p>
 */
public class ListenerIDonStackTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ListenerIDonStackTestCase.class);
   
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
   public void testStabilityOfListenerId() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
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
      InvokerLocator clientLocator1 = new InvokerLocator(locatorURI + "/?timeout=1000");
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator1, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Get listenerId from BisocketClientInvoker and verify it is null.
      assertTrue(client.getInvoker() instanceof BisocketClientInvoker);
      BisocketClientInvoker invoker = (BisocketClientInvoker) client.getInvoker();
      Field field = BisocketClientInvoker.class.getDeclaredField("listenerId");
      field.setAccessible(true);
      String listenerId = (String) field.get(invoker);
      log.info("listenerId before adding callback handler (should be null): " + listenerId);
      assertNull(listenerId);
      
      // Add callback handler.
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      client.addListener(callbackHandler, new HashMap());
      assertEquals(1, callbackHandler.counter);
      log.info("added callback handler");
      
      // Verify listenerId is still null.
      listenerId = (String) field.get(invoker);
      assertNull(listenerId);
      log.info("listenerId after adding callback handler (should be null): " + listenerId);
      
      client.removeListener(callbackHandler);
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "bisocket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         try
         {
            callbackHandler.handleCallback(new Callback("test"));
         }
         catch (HandleCallbackException e)
         {
            e.printStackTrace();
         }
      }
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
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