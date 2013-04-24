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
import java.util.Timer;
import java.util.TimerTask;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.bisocket.Bisocket;


public abstract class ProducerConsumerSoakTestParent extends TestCase
{
   private static Logger log = Logger.getLogger(ProducerConsumerSoakTestParent.class);
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;

   
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
   }

   
   public void tearDown()
   {
   }
   
   
   public void testSoak() throws Throwable
   {
      log.info("entering " + getName());
      System.gc();
      log.info("free space: " + Runtime.getRuntime().freeMemory());
      
      // Start server.
      setupServer();
      
      // Create clients.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client clients[] = new Client[SoakConstants.SENDERS];
      TestCallbackHandler callbackHandler = new TestCallbackHandler();
      Sender senders[] = new Sender[SoakConstants.SENDERS];
      
      for (int i = 0; i < SoakConstants.SENDERS; i++)
      {
         clients[i] = new Client(clientLocator, clientConfig);
         clients[i].connect();
         log.info("client " + i + " is connected");
         HashMap callbackMetadata = new HashMap();
         
         for (int j = 0; j < SoakConstants.CALLBACK_LISTENERS; j++)
         {
            callbackMetadata.clear();
            addExtraCallbackConfig(callbackMetadata);
            clients[i].addListener(callbackHandler, callbackMetadata, null, true);
         }
         log.info("callback handlers installed for client " + i);
         
         senders[i] = new Sender(i, clients[i]);
      }

      Timer timer = new Timer(true);
      timer.schedule(new IntervalTimerTask(), 60000, 60000);
      
      for (int i = 0; i < SoakConstants.SENDERS; i++)
      {
         senders[i].start();
      }
      log.info("senders atarted");
      
      int invocations = 0;
      for (int i = 0; i < SoakConstants.SENDERS; i++)
      {
         senders[i].join();
         invocations += senders[i].counter;
      }
      
      log.info("senders done");
      log.info("invocations made: " + invocations);
      log.info("invocations received: " + invocationHandler.counter);
      log.info("callbacks received: " + callbackHandler.counter);
      
      assertEquals(invocations, invocationHandler.counter);
      assertEquals(invocations * SoakConstants.SENDERS * SoakConstants.CALLBACK_LISTENERS,
                   callbackHandler.counter);
      
      for (int i = 0; i < SoakConstants.SENDERS; i++)
      {
         clients[i].removeListener(callbackHandler);
         clients[i].disconnect();
      }         

      shutdownServer();
      System.gc();
      log.info("free space: " + Runtime.getRuntime().freeMemory());
      log.info(getName() + " PASSES");
   }
   
   
   protected abstract String getTransport();
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   protected void addExtraCallbackConfig(Map config) {}
   

   protected void setupServer() throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port; 
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraServerConfig(config);
      connector = new Connector(serverLocator, config);
      connector.create();
      invocationHandler = new TestInvocationHandler();
      connector.addInvocationHandler("test", invocationHandler);
      connector.start();
   }
   
   
   protected void shutdownServer() throws Exception
   {
      if (connector != null)
         connector.stop();
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      HashSet listeners = new HashSet();
      int counter;
      Object lock = new Object();
      
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         listeners.add(callbackHandler);
      }
      
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         synchronized (lock)
         {
            counter++;
            if ((counter + 1) % SoakConstants.INTERVAL == 0)
               log.info("invocations received: " + (counter + 1));
         }
         
         Object o = invocation.getParameter();
         Callback c = new Callback(o);
         Iterator it = listeners.iterator();
         while(it.hasNext())
         {
            ServerInvokerCallbackHandler handler = (ServerInvokerCallbackHandler) it.next();
            handler.handleCallbackOneway(c);
            handler.handleCallback(c);
         }
         
         return o;
      }
      
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   
   static class TestCallbackHandler implements InvokerCallbackHandler
   {
      int counter;
      Object lock = new Object();
      
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         synchronized (lock)
         {
            counter++;
            if ((counter + 1) % SoakConstants.INTERVAL == 0)
               log.info("callbacks received: " + (counter + 1));
         }
      }  
   }
   
   static class Sender extends Thread
   {
      String name;
      Client client;
      int counter;
      Object lock = new Object();
      long start;
      
      public Sender(int id, Client client)
      {
         name = "sender:" + id;
         this.client = client;
      }
      
      public void run()
      {
         try
         {
            start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start <= SoakConstants.DURATION)
            {
               counter++;
               if ((counter + 1) % SoakConstants.INTERVAL == 0)
                  log.info(name + " invcations made: : " + (counter + 1));
               client.invoke(name + ":" + counter);
            }
         }
         catch (Throwable t)
         {
            log.error(this, t);
         }
      }
   }
   
   
   static class IntervalTimerTask extends TimerTask
   {
      int counter;
      
      public void run()
      {
         log.info("MINUTES ELAPSED: " + ++counter);
      }
   }
}