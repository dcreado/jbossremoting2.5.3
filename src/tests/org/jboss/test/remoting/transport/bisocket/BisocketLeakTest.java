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
import org.jboss.remoting.transport.bisocket.Bisocket;

/** 
 * This class can be profiled for memory leaks.  See JBREM-721.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2316 $
 * <p>
 * Copyright March 15, 2007
 * </p>
 */
public class BisocketLeakTest extends TestCase
{
   private static Logger log = Logger.getLogger(BisocketLeakTest.class);
   private static String transport = "bisocket";
   private static int LOOPS = 1000;
   
   
   public void testConnectionCreation() throws Throwable
   {
      Logger.getLogger("org.jboss.remoting").setLevel(Level.INFO);
      Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
      String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
      PatternLayout layout = new PatternLayout(pattern);
      ConsoleAppender consoleAppender = new ConsoleAppender(layout);
      Logger.getRootLogger().addAppender(consoleAppender); 
      
      String host = InetAddress.getLocalHost().getHostName();
      int port = PortUtil.findFreePort(host);
      String locatorURI = transport + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap serverConfig = new HashMap();
      serverConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      serverConfig.put(Bisocket.IS_CALLBACK_SERVER, "false");
      Connector connector = new Connector(serverLocator, serverConfig);
      connector.create();
      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
      log.info("Started remoting server with locator uri of: " + locatorURI);
      
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(Bisocket.IS_CALLBACK_SERVER, "true");
      log.info("client onnecting to: " + serverLocator);
      Client client = new Client(serverLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      SampleCallbackHandler callbackHandler = new SampleCallbackHandler();
      
      long start = System.currentTimeMillis();
      for (int i = 0; i < LOOPS; i++)
      {
         client.invoke("test");
         client.addListener(callbackHandler, new HashMap(), null, true);
         client.removeListener(callbackHandler);
         assertEquals(i+1, callbackHandler.counter);
         if ((i+1) % 100 == 0)
            log.info("connections: " + (i+1));
      }
      long finish = System.currentTimeMillis();
      log.info("time to create " + LOOPS + " connections: " + (finish - start));
      
      client.disconnect();
      connector.stop();
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
            LOOPS = Integer.parseInt(args[1]);
      }
      BisocketLeakTest testCase = new BisocketLeakTest();
      try
      {
         testCase.setUp();
         testCase.testConnectionCreation();
         testCase.tearDown();
         log.info("done");
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
         log.debug("Adding callback listener.");
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
