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
package org.jboss.test.remoting.lease.synchronization;

import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2913 $
 * <p>
 * Copyright Jul 31, 2007
 * </p>
 */
public class MultipleClientSynchronizationTestCase extends TestCase
{
   protected static Logger log = Logger.getLogger(MultipleClientSynchronizationTestCase.class);
   protected static boolean firstTime = true;
   protected static BooleanHolder go1 = new BooleanHolder();
   protected static BooleanHolder go2 = new BooleanHolder();
   protected static BooleanHolder stop1 = new BooleanHolder();
   protected static BooleanHolder stop2 = new BooleanHolder();
   protected static InvokerLocator locator;
   protected static int counter;
   protected static Object lock = new Object();
   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.INFO);
         Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
         String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
         PatternLayout layout = new PatternLayout(pattern);
         ConsoleAppender consoleAppender = new ConsoleAppender(layout);
         Logger.getRootLogger().addAppender(consoleAppender);  
      }
      
      counter = 0;
   }
   
   
   public void testMultipleClientsStoppingAndStartingSimultaneously() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "socket://" + host + ":" + port;
      locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      connector.setLeasePeriod(1000);
      connector.addConnectionListener(new TestListener());
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      int INVOCATIONS = 1000;
      ClientThread[] threads = new ClientThread[INVOCATIONS];
      
      // Start clients 0..499.
      for (int i = 0; i < INVOCATIONS / 2; i++)
      {
         threads[i] = new ClientThread(i, go1, stop1);
         threads[i].start();
      }
      
      synchronized (go1)
      {
         go1.value = true;
         go1.notifyAll();
      }
      Thread.sleep(5000);
      
      // Stop clients 0..499 and start clients 500..999.
      for (int i = INVOCATIONS / 2; i < INVOCATIONS; i++)
      {
         threads[i] = new ClientThread(i, go2, stop2);
         threads[i].start();
      }
      
      synchronized (stop1)
      {
         stop1.value = true;
         stop1.notifyAll();
      }
      synchronized (go2)
      {
         go2.value = true;
         go2.notifyAll();
      }
      
      Thread.sleep(5000);
      
      // Stop clients 500..999.
      synchronized (stop2)
      {
         stop2.value = true;
         stop2.notifyAll();
      }
      
      // Wait for clients to disconnect.
      for (int i = 0; i < 60; i++)
      {
         synchronized (lock)
         {
            log.info("counter: " + counter);
            if (counter == INVOCATIONS)
               break;
         }
         
         try
         {
            Thread.sleep(2000);
         }
         catch (Exception e)
         {
         }
      }
      
      // Verify that all clients started and stopped successfully.
      for (int i = 0; i < INVOCATIONS; i++)
      {
         assertTrue(new Date() + ": failure in thread: " + i, threads[i].ok);
      }
      
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   public void testMultipleClientsStartingStoppingStarting() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = "socket://" + host + ":" + port;
      locator = new InvokerLocator(locatorURI);
      Connector connector = new Connector(locator);
      connector.create();
      connector.setLeasePeriod(1000);
      connector.addConnectionListener(new TestListener());
      TestHandler handler = new TestHandler();
      connector.addInvocationHandler("test", handler);
      connector.start();
      
      int INVOCATIONS = 1000;
      ClientThread[] threads = new ClientThread[INVOCATIONS];
      
      // Start clients 0..499.
      for (int i = 0; i < INVOCATIONS / 2; i++)
      {
         threads[i] = new ClientThread(i, go1, stop1);
         threads[i].start();
      }
      
      synchronized (go1)
      {
         go1.value = true;
         go1.notifyAll();
      }
      Thread.sleep(5000);
      
      // Stop clients 0..499.
      synchronized (stop1)
      {
         stop1.value = true;
         stop1.notifyAll();
      }
      
      // Start clients 500..999.
      for (int i = INVOCATIONS / 2; i < INVOCATIONS; i++)
      {
         threads[i] = new ClientThread(i, go2, stop2);
         threads[i].start();
      }
      
      synchronized (go2)
      {
         go2.value = true;
         go2.notifyAll();
      }
      
      Thread.sleep(5000);
      
      // Stop clients 500..999.
      synchronized (stop2)
      {
         stop2.value = true;
         stop2.notifyAll();
      }
      
      // Wait for clients to disconnect.
      for (int i = 0; i < 60; i++)
      {
         synchronized (lock)
         {
            log.info("counter: " + counter);
            if (counter == INVOCATIONS)
               break;
         }
         
         try
         {
            Thread.sleep(2000);
         }
         catch (Exception e)
         {
         }
      }

      // Verify that all clients started and stopped successfully.
      for (int i = 0; i < INVOCATIONS; i++)
      {
         assertTrue(new Date() + ": failure in thread: " + threads[i], threads[i].ok);
      }
      
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   public class ClientThread extends Thread
   {
      boolean ok;
      int id;
      BooleanHolder startFlag;
      BooleanHolder stopFlag;
      Client client;
      
      public ClientThread(int id, BooleanHolder startFlag, BooleanHolder stopFlag) throws Exception
      {
         this.id = id;
         this.startFlag = startFlag;
         this.stopFlag = stopFlag;
         
         HashMap config = new HashMap();
         config.put(InvokerLocator.FORCE_REMOTE, "true");
         config.put(Client.ENABLE_LEASE, "true");
         config.put(InvokerLocator.CLIENT_LEASE_PERIOD, "1000");
         client = new Client(locator, config);
         setName("ClientThread-" + id);
         log.debug("client created (" + id + "): " + client.getSessionId());
      }
      
      public void run()
      {
         try
         {
            synchronized (startFlag)
            {
               while (!startFlag.value)
               {
                  try {startFlag.wait();} catch (InterruptedException e) {}
               }
            }
            log.debug("client got start flag (" + id + "): " + client.getSessionId());
            client.connect();
            log.debug("client connected (" + id + "): " + client.getSessionId());
         }
         catch (Throwable e)
         {
            e.printStackTrace();
         }
         
         try
         {
            log.debug("client waiting for stop flag (" + id + "): " + client.getSessionId());
           
            synchronized (stopFlag)
            {
               while (!stopFlag.value)
               {
                  try {stopFlag.wait();} catch (InterruptedException e) {}
               }
            }

            log.debug("client got stop flag (" + id + "): " + client.getSessionId());
            client.disconnect();
//            log.info("client disconnected (" + id + "): " + client.getSessionId());
            ok = true;
            log.debug("client ok (" + id + "):" + client.getSessionId());
            
            synchronized (lock)
            {
               counter++;
            }
         }
         catch (Throwable e)
         {
            e.printStackTrace();
         }
      }
   }
   
   
   static class TestHandler implements ServerInvocationHandler
   {
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
      public Object invoke(InvocationRequest invocation) throws Throwable {return null;}
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
   }
   
   
   public static class TestListener implements ConnectionListener
   {
      public void handleConnectionException(Throwable throwable, Client client)
      {
         log.debug("got connection exception: " + throwable);
      }
   }
   
   
   static class BooleanHolder
   {
      public boolean value;
   }
}