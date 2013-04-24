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

import javax.management.MBeanServer;

import org.apache.log4j.Logger;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

/** 
 * This is the server half of a unit test designed to verify that a Remoting application
 * will shut down without any stray threads hanging it up.  To exercise as many
 * Remoting threads as possible, the server enables leasing and registers a
 * connection listener.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3011 $
 * <p>
 * Copyright Jan 19, 2007
 * </p>
 */
public class ShutdownTestServer extends ServerTestCase
{
   public static int port = 9876;
   private static Logger log = Logger.getLogger(ShutdownTestServer.class);
   private Connector connector;
   private String transport;
   private Map extraConfig;
   
   public ShutdownTestServer(String transport, Map config)
   {
      this.transport = transport;
      this.extraConfig = config;
   }
   
   
   public void setUp() throws Exception
   {
      String host = InetAddress.getLocalHost().getHostAddress();
      String portString = System.getProperty("port");
      log.info("portString: " + portString);
      int port = Integer.parseInt(portString);
      String locatorURI = transport + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap(extraConfig);
      log.info("serverConfig: " + serverConfig);
      connector = new Connector(locator, serverConfig);
      connector.create();
      connector.setLeasePeriod(2000);
      connector.addInvocationHandler("test", new TestHandler());
      connector.addConnectionListener(new TestListener());
      connector.start();
      log.info("server started at: " + locatorURI);
      log.info("READY");
   }
   
   
   public void tearDown()
   {
      if (connector != null)
      {
         connector.stop();
         log.info("server shut down");
      }
   }
   
   
   public static void main(String[] args)
   {
      if (args.length == 0)
         throw new RuntimeException();
      
      HashMap config = new HashMap();
      System.out.println("server args.length: " + args.length);
      if (args.length > 1)
         getConfig(config, args[1]);
      
      String transport = args[0];
      ShutdownTestServer server = new ShutdownTestServer(transport, config);
      try
      {
         server.setUp();
         log.info("server back from setUp()");
         Thread.sleep(25000);
         log.info("server calling tearDown()");
         server.tearDown();
         log.info("server back from tearDown()");
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
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
      log.info("config: " + config);
      log.info("configs: " + configs);
   }
   
   
   public class TestHandler implements ServerInvocationHandler
   {
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         Integer i = (Integer) invocation.getParameter();
         return new Integer(i.intValue() + 1);
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         try
         {
            log.info("sending callback");
            callbackHandler.handleCallback(new Callback("callback"));
            log.info("sent callback");
         }
         catch (HandleCallbackException e)
         {
            log.info("error handling callback");
            e.printStackTrace();
         }
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
   }
   
   
   public static class TestListener implements ConnectionListener
   {
      public void handleConnectionException(Throwable throwable, Client client)
      {
         log.info("got connection exception: " + throwable);
      }
   }
}
