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
package org.jboss.test.remoting.shutdown;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.CallbackPoller;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3011 $
 * <p>
 * Copyright Jan 19, 2007
 * </p>
 */
public abstract class AbstractClient extends TestCase
{    
   private static Logger log = Logger.getLogger(AbstractClient.class);
   private String transport;
   private Map extraConfig;
   
   
   public AbstractClient(String transport, Map config)
   {
      this.transport = transport;
      this.extraConfig = new HashMap(config);
      log.info("client transport: " + transport);
      log.info("log4j.configuration: " + System.getProperty("log4j.configuration"));
      Runtime.getRuntime().traceMethodCalls(true);
   }
   
   
   /**
    * This test is used to verify that a JVM with a client connected to a server will shut
    * down.  To exercise as many threads as possible, it enables leasing, registers a
    * connection listener, and registers a callback handler for blocking polled callbacks
    * and another callback handler for nonblocking polled callbacks.
    * 
    * At the end of the method, it creates a Thread which runs longer that this test is
    * supposed to last.  According to the value returned by the overridden abstract
    * method daemon(), it the Thread will be a daemon or non-daemon thread.
    */
   public void testShutdown() throws Throwable
   {
      try
      {
         String host = InetAddress.getLocalHost().getHostAddress();
         String portString = System.getProperty("port");
         int port = Integer.parseInt(portString);
         String locatorURI = transport + "://" + host + ":" + port;
         InvokerLocator locator = new InvokerLocator(locatorURI);
         HashMap clientConfig = new HashMap(extraConfig);
         clientConfig.put(Client.ENABLE_LEASE, "true");
         clientConfig.put(InvokerLocator.CLIENT_LEASE_PERIOD, "1000");
         Client client = new Client(locator, clientConfig);
         client.connect();
         log.info("client connected");
         log.info("READY");
         ConnectionListener listener = new ShutdownTestServer.TestListener();
         client.addConnectionListener(listener, 1000);
         Integer i = (Integer) client.invoke(new Integer(17));
         if (18 != i.intValue())
            throw new Exception("invocation failed");
         log.info("invocation successful");
         TestCallbackHandler callbackHandler1 = new TestCallbackHandler();
         HashMap metadata = new HashMap();
         metadata.put(ServerInvoker.BLOCKING_TIMEOUT, "2000");
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.BLOCKING);
         addCallbackArgs(metadata);
         log.info("metadata: " + metadata);
         client.addListener(callbackHandler1, metadata, null, false);
         log.info("added blocking listener 1");
         TestCallbackHandler callbackHandler2 = new TestCallbackHandler();
         metadata.clear();
         metadata.put(CallbackPoller.CALLBACK_POLL_PERIOD, "500");
         metadata.put(ServerInvoker.BLOCKING_MODE, ServerInvoker.NONBLOCKING);
         addCallbackArgs(metadata);
         log.info("metadata: " + metadata);
         client.addListener(callbackHandler2, metadata, null, false);
         log.info("added nonblocking listener 2");
         Thread.sleep(4000);
         if (!callbackHandler1.receivedCallback)
         {
            log.info("callback 1 failed");
            throw new Exception("callback 1 failed");
         }
         if (!callbackHandler2.receivedCallback)
         {
            log.info("callback 2 failed");
            throw new Exception("callback 2 failed");
         }
         log.info("callback successful");
         client.removeConnectionListener(listener);
         log.info("calling removeListener(): 1");
         client.removeListener(callbackHandler1);
         log.info("calling removeListener(): 2");
         client.removeListener(callbackHandler2);
         log.info("calling disconnect()");
         client.disconnect();
         Thread t = new Thread()
         {
            public void run()
            {
               try
               {
                  Thread.sleep(20000);
               }
               catch (InterruptedException e)
               {
                  log.info("interrupted");
               }
            }
         };
         t.setDaemon(daemon());
         t.start();
         log.info("client disconnected");
      }
      catch (Exception e)
      {
         log.info("exception in client: " + e);
         System.exit(1);
      }
   }
   
   
   abstract protected boolean daemon();
   
   
   protected void addCallbackArgs(Map map)
   {
      return;
   }
   
   
   protected static void getConfig(Map config, String configs)
   {
      int start = 0;
      int ampersand = configs.indexOf('&');
      while (ampersand > 0)
      {
         String s = configs.substring(start, ampersand);
         int equals = s.indexOf('=');
         String param = s.substring(0, equals);
         String value = s.substring(equals + 1);
         config.put(param, value);
         start = ampersand + 1;
         ampersand = configs.indexOf('&', start);
      }
   }
   
   
   public class TestCallbackHandler implements InvokerCallbackHandler
   {  
      public boolean receivedCallback;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         receivedCallback = true;
         log.info("received callback: " + callback);
      }  
   }
}
