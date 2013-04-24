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
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1702 $
 * <p>
 * Copyright Nov 25, 2006
 * </p>
 */
public class BisocketPerformanceTestClient extends TestCase
{
   private static Logger log = Logger.getLogger(BisocketPerformanceTestClient.class);
   private static String transport = "bisocket";
   private static int invocations = 1000;
   
   
   public void testBisocket() throws Throwable
   {
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = BisocketPerformanceTestServer.port;
      String locatorURI = transport + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      System.out.println("Connecting to: " + serverLocator);
      Client client = new Client(serverLocator);
      client.connect();
      System.out.println("client is connected");
      InvokerCallbackHandler callbackHandler = new SampleCallbackHandler();
      client.addListener(callbackHandler, new HashMap(), null, true);
      System.out.println("client added callback handler");
      
      long start = System.currentTimeMillis();
      for (int i = 0; i < invocations; i++)
      {
         client.invoke("test");
      }
      long finish = System.currentTimeMillis();
      System.out.println("time to make " + invocations + " invocations: " + (finish - start));
      client.removeListener(callbackHandler);
      client.disconnect();
   }
   
   
   /**
    * Can pass transport and port to be used as parameters.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      if(args != null && args.length > 0)
      {
         transport = args[0];
         if (args.length > 1)
            invocations = Integer.parseInt(args[1]);
      }
      BisocketPerformanceTestClient testCase = new BisocketPerformanceTestClient();
      try
      {
         testCase.setUp();
         testCase.testBisocket();
         Thread.sleep(10000);
         testCase.tearDown();
         System.out.println("done");
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
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         System.out.println("Adding callback listener.");
         try
         {
            callbackHandler.handleCallback(new Callback("callback"));
         }
         catch (HandleCallbackException e)
         {
            log.error("Unable to send callback");
         }
      }

      public Object invoke(InvocationRequest invocation) throws Throwable {return null;}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class SampleCallbackHandler implements InvokerCallbackHandler
   {
      int counter;
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         counter++;
         if ((counter + 1) % 1000 == 0)
         {
            System.out.println("received callback " + (counter + 1));
         }
      }
   }
}
