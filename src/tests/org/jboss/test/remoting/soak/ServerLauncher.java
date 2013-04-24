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
package org.jboss.test.remoting.soak;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;


/**
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Mar 13, 2008
 * </p>
 */
public class ServerLauncher extends SoakConstants
{
   private static Logger log = Logger.getLogger(ServerLauncher.class);
   private static Map locators = new HashMap();
   private static Connector[] connectors = new Connector[4];
   
   public static Map getLocators()
   {
      return locators;
   }
   
   public static void main(String[] args)
   {
      try
      {
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender); 
         
         String host = InetAddress.getLocalHost().getHostAddress();
         
         connectors[0] = setupServer(host, 6666, "bisocket");
         locators.put("bisocket", connectors[0].getLocator().getLocatorURI());
         
         connectors[1] = setupServer(host, 6667, "http");
         locators.put("http", connectors[1].getLocator().getLocatorURI());
         
         connectors[2] = setupServer(host, 6668, "rmi");
         locators.put("rmi", connectors[2].getLocator().getLocatorURI());
         
         connectors[3] = setupServer(host, 6669, "socket");
         locators.put("socket", connectors[3].getLocator().getLocatorURI());
         
         log.info("SERVERS CREATED: " + locators);
         
         System.in.read();
         System.in.read();
         System.in.read();
         
         log.info("SHUTTING DOWN SERVERS");
         for (int i = 0; i < connectors.length; i++)
         {
            connectors[i].stop();
         }
         log.info("SERVERS SHUT DOWN");
         
      }
      catch (Exception e)
      {
         log.error("Error", e);
      }
   }
   
   
   protected static Connector setupServer(String host, int port, String transport) throws Exception
   {
      String locatorURI = transport + "://" + host + ":" + port + "/?timeout=0"; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      Connector connector = new Connector(serverLocator, config);
      connector.create();
      TestInvocationHandler invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
      return connector;
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      Map listeners = new HashMap();
      Object lock = new Object();
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         log.debug("entering addListener()");
         synchronized (lock)
         {
            String id = ((ServerInvokerCallbackHandler)callbackHandler).getClientSessionId();
            listeners.put(id, callbackHandler);
         }
         log.debug("added InvokerCallbackHandler: " + listeners);
      }
      
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         String command = (String) invocation.getParameter();
         log.debug("command: " + command);
         
         if (COPY.equals(command))
         {
            Object o = invocation.getRequestPayload().get(PAYLOAD);
            return o;
         }
         else if (SPIN.equals(command))
         {
            String s = (String) invocation.getRequestPayload().get(SPIN_TIME);
            int spinTime = Integer.parseInt(s);
            SpinThread t = new SpinThread();
            t.start();
            Thread.sleep(spinTime);
            t.setStop();
            return "done";
         }
         else if (CALLBACK.equals(command))
         {
            String id = invocation.getSessionId();
            Map requestPayload = invocation.getRequestPayload();
            String s = (String) requestPayload.get(NUMBER_OF_CALLBACKS);
            int callbacks = Integer.parseInt(s);
            InvokerCallbackHandler callbackHandler = null;

            synchronized (lock)
            {
               callbackHandler = (InvokerCallbackHandler) listeners.get(id);
            }

            if (callbackHandler == null)
            {
               log.debug("sessionId: " + id);
               log.debug("listeners: " + listeners);
            }
            Callback callback = new Callback("callback");
            for (int i = 0; i < callbacks; i++)
            {
               callbackHandler.handleCallback(callback);
            }
         }

         return command;
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         synchronized (lock)
         {
            String id = ((ServerInvokerCallbackHandler) callbackHandler).getClientSessionId();
            listeners.remove(id);
         }
      }
      
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   static class SpinThread extends Thread
   {
      boolean stop;
      static int counter;
      static Object lock = new Object();
      
      public SpinThread()
      {
         synchronized (lock)
         {
            setName("spinThread:" + counter++);
         }
      }
      public void setStop()
      {
         stop = true;
         log.debug(this + " stop = " + stop);
      }
      
      public void run()
      {
         int n = 0;
         while (!stop)
         {
            n++;
            if ((n + 1) % 10000 == 0)
               log.debug(this + "stop = " + stop);
         }
         log.debug("SpinThread done");
      }
   }
}

