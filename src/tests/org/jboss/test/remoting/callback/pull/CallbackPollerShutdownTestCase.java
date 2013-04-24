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
package org.jboss.test.remoting.callback.pull;

import java.lang.reflect.Field;
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
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.CallbackListener;
import org.jboss.remoting.callback.CallbackPoller;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.util.id.GUID;


/** 
 * Verifies that CallbackPoller shuts down when max error count is exceeded.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2903 $
 * <p>
 * Copyright June 18, 2007
 * </p>
 */
public class CallbackPollerShutdownTestCase extends TestCase
{
   public static int port;
   
   private static Logger log = Logger.getLogger(CallbackPollerShutdownTestCase.class);
   private static boolean firstTime = true;
   
   // remoting server connector
   private Connector connector;
   private InvokerLocator serverLocator;
   private SampleInvocationHandler invocationHandler;

   
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

   
   /**
    * Shuts down the server
    */
   public void tearDown()
   {
      if (connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }
   
   
   /**
    * Verifies that CallbackPoller in blocking mode shuts down after max error 
    * count has been exceeded.
    */
   public void testBlockingCallbackPoller() throws Throwable
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
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Add callback handler.
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler(client);
      Map metadata = new HashMap();
      metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
      metadata.put(ServerInvoker.BLOCKING_TIMEOUT, "500");
      metadata.put(CallbackPoller.MAX_ERROR_COUNT, "4");
      client.addListener(callbackHandler, metadata);
      log.info("client added callback handler for pull callbacks");
      
      // Test for good connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Give time for CallbackPoller$AcknowledgeThread to start.
      Thread.sleep(2000);

      // Get necessary fields.
      Field field = Client.class.getDeclaredField("callbackPollers");
      field.setAccessible(true);
      Map callbackPollers = (Map) field.get(client);
      assertEquals(1, callbackPollers.size());
      CallbackPoller poller = (CallbackPoller) callbackPollers.values().iterator().next();
      field.setAccessible(true);
      Class[] classes = CallbackPoller.class.getDeclaredClasses();
      Class handleThreadClass = null;
      Class acknowledgeThreadClass = null;
      for (int i = 0; i < classes.length; i++)
      {
         Class c = classes[i];
         log.info(c.getName());
         String fqn = c.getName();
         String name = fqn.substring(fqn.indexOf('$') + 1);
         log.info(name);
         if ("HandleThread".equals(name))
            handleThreadClass = c;
         else if ("AcknowledgeThread".equals(name))
            acknowledgeThreadClass = c;
            
      }
      assertNotNull(handleThreadClass);
      assertNotNull(acknowledgeThreadClass);
      field = CallbackPoller.class.getDeclaredField("handleThread");
      field.setAccessible(true);
      Thread handleThread = (Thread) field.get(poller); 
      log.info("handleThread: " + handleThread);
      field = CallbackPoller.class.getDeclaredField("acknowledgeThread");
      field.setAccessible(true);
      Thread acknowledgeThread = (Thread) field.get(poller); 
      log.info("acknowledgeThread: " + handleThread);
      
      connector.stop();
      
      // Wait for CallbackPoller to shut down.
      Thread.sleep(6000);
      field = handleThreadClass.getDeclaredField("done");
      field.setAccessible(true);
      boolean handleThreadDone = ((Boolean) field.get(handleThread)).booleanValue();
      field = acknowledgeThreadClass.getDeclaredField("done");
      field.setAccessible(true);
      boolean acknowledgeThreadDone = ((Boolean) field.get(acknowledgeThread)).booleanValue();
      field = CallbackPoller.class.getDeclaredField("running");
      field.setAccessible(true);      
      boolean pollerRunning = ((Boolean) field.get(poller)).booleanValue();
      assertTrue(handleThreadDone);
      assertTrue(acknowledgeThreadDone);
      assertFalse(pollerRunning);

      client.setDisconnectTimeout(0);
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   /**
    * Verifies that CallbackPoller in nonblocking mode shuts down after max error 
    * count has been exceeded.
    */
   public void testNonBlockingCallbackPoller() throws Throwable
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
      invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Add callback handler.
      SimpleCallbackHandler callbackHandler = new SimpleCallbackHandler(client);
      Map metadata = new HashMap();
      metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.NONBLOCKING);
      metadata.put(CallbackPoller.CALLBACK_POLL_PERIOD, "500");
      metadata.put(ServerInvoker.BLOCKING_TIMEOUT, "500");
      metadata.put(CallbackPoller.MAX_ERROR_COUNT, "4");
      client.addListener(callbackHandler, metadata);
      log.info("client added callback handler for pull callbacks");
      
      // Test for good connection.
      assertEquals("abc", client.invoke("abc"));
      
      // Give time for CallbackPoller$AcknowledgeThread to start.
      Thread.sleep(2000);

      // Get necessary fields.
      Field field = Client.class.getDeclaredField("callbackPollers");
      field.setAccessible(true);
      Map callbackPollers = (Map) field.get(client);
      assertEquals(1, callbackPollers.size());
      CallbackPoller poller = (CallbackPoller) callbackPollers.values().iterator().next();
      field.setAccessible(true);
      Class[] classes = CallbackPoller.class.getDeclaredClasses();
      Class handleThreadClass = null;
      Class acknowledgeThreadClass = null;
      for (int i = 0; i < classes.length; i++)
      {
         Class c = classes[i];
         log.info(c.getName());
         String fqn = c.getName();
         String name = fqn.substring(fqn.indexOf('$') + 1);
         log.info(name);
         if ("HandleThread".equals(name))
            handleThreadClass = c;
         else if ("AcknowledgeThread".equals(name))
            acknowledgeThreadClass = c;
            
      }
      assertNotNull(handleThreadClass);
      assertNotNull(acknowledgeThreadClass);
      field = CallbackPoller.class.getDeclaredField("handleThread");
      field.setAccessible(true);
      Thread handleThread = (Thread) field.get(poller); 
      log.info("handleThread: " + handleThread);
      field = CallbackPoller.class.getDeclaredField("acknowledgeThread");
      field.setAccessible(true);
      Thread acknowledgeThread = (Thread) field.get(poller); 
      log.info("acknowledgeThread: " + handleThread);
      
      connector.stop();
      
      // Wait for CallbackPoller to shut down.
      Thread.sleep(4000);
      field = handleThreadClass.getDeclaredField("done");
      field.setAccessible(true);
      boolean handleThreadDone = ((Boolean) field.get(handleThread)).booleanValue();
      field = acknowledgeThreadClass.getDeclaredField("done");
      field.setAccessible(true);
      boolean acknowledgeThreadDone = ((Boolean) field.get(acknowledgeThread)).booleanValue();
      field = CallbackPoller.class.getDeclaredField("running");
      field.setAccessible(true);      
      boolean pollerRunning = ((Boolean) field.get(poller)).booleanValue();
      assertTrue(handleThreadDone);
      assertTrue(acknowledgeThreadDone);
      assertFalse(pollerRunning);

      client.setDisconnectTimeout(0);
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   static class SampleInvocationHandler implements ServerInvocationHandler, CallbackListener
   {  
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         log.info("sending callback with request for acknowledgement");
         HashMap returnPayload = new HashMap();
         returnPayload.put(ServerInvokerCallbackHandler.CALLBACK_LISTENER, this);
         returnPayload.put(ServerInvokerCallbackHandler.CALLBACK_ID, new GUID());
         returnPayload.put(ServerInvokerCallbackHandler.REMOTING_ACKNOWLEDGES_PUSH_CALLBACKS, "true");
         Callback callback = new Callback("callback");
         callback.setReturnPayload(returnPayload);
         
         try
         {
            callbackHandler.handleCallback(callback);
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
      public void acknowledgeCallback(InvokerCallbackHandler callbackHandler, Object callbackId, Object response)
      {
         log.info(callbackId + " acknowledged");
      }
   }
   
   
   static class SimpleCallbackHandler implements InvokerCallbackHandler
   {
      List callbacks = new ArrayList();
      Client client;
      
      public SimpleCallbackHandler(Client client)
      {
    	  this.client = client;
      }

      public void handleCallback(Callback callback) throws HandleCallbackException
      {
    	  try
    	  {
    		  callbacks.add(callback);
    		  log.info("received callback");
    	  }
    	  catch (Throwable t)
    	  {
    		  t.printStackTrace();
    	  }
      }
   }
}