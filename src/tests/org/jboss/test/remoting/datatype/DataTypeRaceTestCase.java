/*
* JBoss, Home of Professional Open Source
* Copyright 2009, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.remoting.datatype;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.MicroRemoteClientInvoker;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;

import EDU.oswego.cs.dl.util.concurrent.Rendezvous;


/**
 * Unit test for JBREM-1109.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Apr 8, 2009
 * </p>
 */
public class DataTypeRaceTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(DataTypeRaceTestCase.class);
   
   private static boolean firstTime = true;
   protected static String dataType;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;
   protected TestInvocationHandler invocationHandler;
   protected Object lock = new Object();

   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(Level.INFO);
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
   
   
   public void testDataTypeRace() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test datatype race.
      MicroRemoteClientInvoker clientInvoker = (MicroRemoteClientInvoker) client.getInvoker();
      
      int THREADS = 1000;
      TestThread[] threads = new TestThread[THREADS];
      Rendezvous startBarrier = new Rendezvous(THREADS);
      Rendezvous stopBarrier = new Rendezvous(THREADS + 1);
      
      log.info(getName() + " creating " + THREADS + " threads");
      for (int i = 0; i < THREADS; i++)
      {
         threads[i] = new TestThread(clientInvoker, startBarrier, stopBarrier, i);
         threads[i].start();
      }
      
      log.info(getName() + " waiting on stopBarrier");
      rendezvous(stopBarrier);
      log.info(getName() + " checking threads");
      
      for (int i = 0; i < THREADS; i++)
      {
         assertTrue("failure in " + threads[i], threads[i].ok);
      }
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer() throws Exception
   {
      host = InetAddress.getLocalHost().getHostAddress();
      port = PortUtil.findFreePort(host);
      locatorURI = getTransport() + "://" + host + ":" + port;
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "/?" + metadata;
      }
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
   
   
   protected static void rendezvous(Rendezvous barrier)
   {
      while (true)
      {
         try
         {
            barrier.rendezvous(null);
            break;
         }
         catch (InterruptedException e1)
         {

         }
      }
   }
   
   
   static class TestInvocationHandler implements ServerInvocationHandler
   {
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }
   
   static class TestThread extends Thread
   {
      String name;
      ClientInvoker clientInvoker;
      Rendezvous startBarrier;
      Rendezvous stopBarrier;
      InvocationRequest request = new InvocationRequest(null, null, "abc", null, null, null);
      boolean ok;
      
      public TestThread(ClientInvoker clientInvoker, Rendezvous startBarrier, Rendezvous stopBarrier, int number)
      {
         this.clientInvoker = clientInvoker;
         this.startBarrier = startBarrier;
         this.stopBarrier = stopBarrier;
         name = "TestThread[" + number + "]";
      }
      
      public void run()
      {
//         log.debug(this + " waiting on startBarrier");
         rendezvous(startBarrier);
//         log.debug(this + " executing");
         try
         {
               clientInvoker.invoke(request);
//            log.debug(this + " waiting on stopBarrier");
            ok = true;
            rendezvous(stopBarrier);
//            log.debug(this + " done");
         }
         catch (Throwable t)
         {
            t.printStackTrace();
            rendezvous(stopBarrier);
         }
      }
      
      public String toString()
      {
         return name;
      }
   }
}