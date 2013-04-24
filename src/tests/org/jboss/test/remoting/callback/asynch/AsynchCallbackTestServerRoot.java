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
package org.jboss.test.remoting.callback.asynch;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.AsynchInvokerCallbackHandler;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.util.threadpool.BasicThreadPool;

/** 
 * AsynchCallbackTestClientRoot and AsynchCallbackTestServerRoot are the parent classes
 * for a set of transport specific tests of the asynchronous callback facility.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2103 $
 * <p>
 * Copyright Nov 25, 2006
 * </p>
 */
public abstract class AsynchCallbackTestServerRoot extends ServerTestCase
{
   public static int port = 5413;
   
   public static String SYNCHRONOUS_TEST = "synchronousTest";
   public static String ASYNCHRONOUS_SERVER_SIDE_TEST = "asynchronousServerSideTest";
   public static String ASYNCHRONOUS_CLIENT_SIDE_TEST = "asynchronousClientSideTest";
   public static String GET_STATUS = "getStatus";
   public static String RESET = "reset";
   public static String GET_THREAD_COUNT = "getThreadCount";
   public static String GET_QUEUE_SIZE = "getQueueSize";
   public static String THREAD_COUNT = "17";
   public static String QUEUE_SIZE = "19";
   
   private static Logger log = Logger.getLogger(AsynchCallbackTestServerRoot.class);
   
   // remoting server connector
   static private Connector connector;
   String serverLocatorURI;

   
   /**
    * Sets up target remoting server.
    */
   public void setUp() throws Exception
   {
      log.info("entering setUp()");
      String locatorURI =  getTransport() + "://" + InetAddress.getLocalHost().getHostAddress() + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      Map config = new HashMap();
      config.put(Client.MAX_NUM_ONEWAY_THREADS, THREAD_COUNT);
      config.put(Client.MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE, QUEUE_SIZE);
      addTransportSpecificConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
   }

   
   /**
    * Shuts down the server
    */
   public void tearDown()
   {
      connector.stop();
      connector.destroy();
   }
   
   
   protected abstract String getTransport();
   
   
   protected void addTransportSpecificConfig(Map config)
   {
   }

   /**
    * Simple invocation handler implementation.  When callback client's are registered, will
    * generate callbacks periodically.
    */
   static class SampleInvocationHandler implements ServerInvocationHandler
   {
      boolean done;
      AsynchInvokerCallbackHandler callbackHandler;
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         System.out.println("Adding callback listener.");
         assertTrue(callbackHandler instanceof AsynchInvokerCallbackHandler);
         this.callbackHandler = (AsynchInvokerCallbackHandler) callbackHandler;
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         try
         {
            String test = (String) invocation.getParameter();
            if (test.equals(SYNCHRONOUS_TEST))
            {
               log.info("making synchronous callback");
               callbackHandler.handleCallback(new Callback(test));
               log.info("made synchronous callback");
            }
            else if (test.equals(ASYNCHRONOUS_SERVER_SIDE_TEST))
            {
               log.info("making asynchronous callback - server side");
               callbackHandler.handleCallbackOneway(new Callback("callback"), true);
            }
            else if (test.equals(ASYNCHRONOUS_CLIENT_SIDE_TEST))
            {
               log.info("making asynchronous callback - client side");
               callbackHandler.handleCallbackOneway(new Callback("callback"));
            }
            else if (test.equals(GET_STATUS))
            {
               synchronized (this)
               {
                  log.info("returning status: " + done);
                  return new Boolean(done);
               }
            }
            else if (test.equals(RESET))
            {
               synchronized (this)
               {
                  done = false;
               } 
            }
            else if (test.equals(GET_THREAD_COUNT))
            {
               ServerInvokerCallbackHandler sich = (ServerInvokerCallbackHandler) callbackHandler;
               Client callbackClient = sich.getCallbackClient();
               Field field = Client.class.getDeclaredField("onewayThreadPool");
               field.setAccessible(true);
               BasicThreadPool threadPool = (BasicThreadPool) field.get(callbackClient);
               int size = threadPool.getMaximumPoolSize();
               return Integer.toString(size);
            }
            else if (test.equals(GET_QUEUE_SIZE))
            {
               ServerInvokerCallbackHandler sich = (ServerInvokerCallbackHandler) callbackHandler;
               Client callbackClient = sich.getCallbackClient();
               Field field = Client.class.getDeclaredField("onewayThreadPool");
               field.setAccessible(true);
               BasicThreadPool threadPool = (BasicThreadPool) field.get(callbackClient);
               int size = threadPool.getMaximumQueueSize();
               return Integer.toString(size);
            }
            else
            {
               log.error("unrecognized test: " + test);
            }
         }
         catch (HandleCallbackException e)
         {
            log.error("Unable to send callback");
         }
         synchronized (this)
         {
            done = true;
            log.info("done");
         }
         return null;
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
}
