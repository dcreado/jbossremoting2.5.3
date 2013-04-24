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

import java.net.InetAddress;
import java.util.HashMap;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.bisocket.Bisocket;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1717 $
 * <p>
 * Copyright Nov 25, 2006
 * </p>
 */
public class BisocketPerformanceTestServer extends TestCase
{
   public static int port = 5423;
   
   private static Logger log = Logger.getLogger(BisocketPerformanceTestServer.class);
   private static String transport = "bisocket";
   
   // remoting server connector
   private Connector connector;

   
   /**
    * Sets up target remoting server.
    */
   public void setUp() throws Exception
   {
      String locatorURI = transport + "://" + InetAddress.getLocalHost().getHostAddress() + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(Bisocket.IS_CALLBACK_SERVER, "false");
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
      if (connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }
   
   
   public static void main(String[] args)
   {
      if (args.length == 1)
         transport = args[0];
         
      BisocketPerformanceTestServer testCase = new BisocketPerformanceTestServer();
      try
      {
         testCase.setUp();
         Thread.sleep(600000);
         testCase.tearDown();
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }

   /**
    * Simple invocation handler implementation.  When callback client's are registered, will
    * generate callbacks periodically.
    */
   static class SampleInvocationHandler implements ServerInvocationHandler
   {
      InvokerCallbackHandler callbackHandler;
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         System.out.println("Adding callback listener.");
         this.callbackHandler = callbackHandler;
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         try
         {
            callbackHandler.handleCallback(new Callback("callback"));
         }
         catch (HandleCallbackException e)
         {
            log.error("Unable to send callback");
         }
         return null;
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
}
