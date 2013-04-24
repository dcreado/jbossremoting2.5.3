/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.test.remoting.versioning.lease;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;


/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 08, 2009
 * </p>
 */
public class LeaseVersionTestServer extends ServerTestCase
{
   public static final String REMOTING_METADATA = "remoting.metadata";
   public static final String JVM_MAX_HEAP_SIZE = "jvm.mx";
   public static final String GET_LISTENER_INFO = "getListenerCount";
   public static final String LISTENER_COUNT = "listenerCount";
   public static final String THROWABLE = "throwable";
   public static final String PORT = "9091";
   
   private static Logger log = Logger.getLogger(LeaseVersionTestServer.class);
   
   protected static long LEASE_PERIOD = 2000;
   protected static String LEASE_PERIOD_STRING = "2000";
   
   protected Connector connector;
   protected TestConnectionListener listener;
   
   
   public static void main(String[] args)
   {
      try
      {
         LeaseVersionTestServer p = new LeaseVersionTestServer();
         p.setUp();
         Thread.sleep(3000000);
         p.tearDown();
      }
      catch (Exception e)
      {
         log.error("Error", e);
      }
   }
   
   
   public void setUp() throws Exception
   {
      Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
      Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
      String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
      PatternLayout layout = new PatternLayout(pattern);
      ConsoleAppender consoleAppender = new ConsoleAppender(layout);
      Logger.getRootLogger().addAppender(consoleAppender);  
      setupServer();
   }
   
   
   public void tearDown()
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraServerConfig(Map config) {}
   
   
   protected void setupServer() throws Exception
   {
      String locatorURI = createLocatorURI();
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "/?" + metadata;
      }
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put("leasePeriod", LEASE_PERIOD_STRING);
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      ServerInvocationHandler invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
      listener = new TestConnectionListener();
      connector.addConnectionListener(listener);
   }
   
   
   protected String createLocatorURI() throws UnknownHostException
   {
      String locatorURI = getTransport() + "://" + InetAddress.getLocalHost().getHostAddress() + ":" + PORT;
      locatorURI += "/?useClientConnectionIdentity=true";
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "&" + metadata;
      }
      return locatorURI;
   }
   
   
   class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         Object o = invocation.getParameter();
         if (GET_LISTENER_INFO.equals(o))
         {
            HashMap map = new HashMap();
            map.put(LISTENER_COUNT, new Integer(listener.counter));
            map.put(THROWABLE, listener.throwable);
            listener.counter = 0;
            listener.throwable = null;
            return map;
         }
         return o;
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   

   static class TestConnectionListener implements ConnectionListener
   {
      public int counter;
      public Throwable throwable;
      
      public void handleConnectionException(Throwable throwable, Client client)
      {
         counter++;
         this.throwable = throwable;
         log.info("called: throwable = " + throwable);
      }  
   }
}