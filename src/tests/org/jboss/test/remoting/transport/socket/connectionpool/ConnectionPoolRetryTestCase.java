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
package org.jboss.test.remoting.transport.socket.connectionpool;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

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
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * Unit test for JBREM-786, JBREM-890.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Dec 21, 2007
 * </p>
 */
public class ConnectionPoolRetryTestCase extends TestCase
{
   protected static Logger log = Logger.getLogger(ConnectionPoolRetryTestCase.class);
   protected static String DELAY = "delay";
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
   
   
   /**
    * Verifies a new connection is created on the last attempt to make
    * an invocation.  Tries to create a situation in which server threads are
    * frequently evicted and reused with a new socket, leaving the matching
    * connection in the client side connection pool unusable.  If the probability of
    * getting an unusable connection is high, it is likely that the number
    * of retries would be exhausted and an exception thrown.  The new facility
    * for always creating a new connection on the last attempt should make it
    * possible to avoid running out of retries.
    */
   public void testNewConnectionOnLastRetry() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server - no connection checking.
      setupServer();
      
      // Create first client.
      InvokerLocator clientLocator1 = new InvokerLocator(locatorURI);
      HashMap clientConfig1 = new HashMap();
      clientConfig1.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig1.put("numberOfCallRetries", "2");
      addExtraClientConfig(clientConfig1);
      Client client1 = new Client(clientLocator1, clientConfig1);
      client1.connect();
      
      int BANGERS = 50;
      int INVOCATIONS = 5000;
      BangerThread[] bangers = new BangerThread[BANGERS];
      for (int i = 0; i < BANGERS; i++)
      {
         bangers[i] = new BangerThread(client1, INVOCATIONS, "banger:" + i);
      }
      
      String newLocatorURI = locatorURI + "/?timeout=100000";
      InvokerLocator clientLocator2 = new InvokerLocator(newLocatorURI);
      HashMap clientConfig2 = new HashMap();
      clientConfig2.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig1.put("numberOfCallRetries", "4");
      addExtraClientConfig(clientConfig2);
      Client client2 = new Client(clientLocator2, clientConfig2);
      client2.connect();

      for (int i = 0; i < BANGERS; i++)
      {
         bangers[i].start();
      }
      
      for (int i = 0; i < BANGERS; i++)
      {
         bangers[i].join();
      }
      
      for (int i = 0; i < BANGERS; i++)
      {
         log.info("banger " + i + " done: " + bangers[i].done);
         log.info("banger " + i + " ok:   " + bangers[i].ok);
      }
         
      for (int i = 0; i < BANGERS; i++)
      {
         assertTrue("banger " + i + " not done", bangers[i].done);
         assertTrue("banger " + i + " experienced error", bangers[i].ok);
      }

      client1.disconnect();
      client2.disconnect();
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
      serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put("maxPoolSize", "2");
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
      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public Object invoke(final InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}
   }

   
   static class BangerThread extends Thread
   {
      public boolean done;
      public boolean ok;
      private Client client;
      private int count;
      private String name;
      private boolean error;
      
      public BangerThread(Client client, int count, String name)
      {
         this.client = client;
         this.count = count;
         this.name = name;
         setName(name);
      }
      public void run()
      {
         log.info(name + ": started");
         for (int i = 0; i < count; i++)
         {
            try
            {
               long start = System.currentTimeMillis();
               client.invoke(name + ":" + i);
               long duration = System.currentTimeMillis() - start;
               if (duration > 2000)
               {
                  log.info(this + " invocation(" + i + ") took " + duration + " ms");
               }
               
               if ((i + 1) % 1000 == 0)
                  log.info(name + " got response: " + (i+1));
            }
            catch (Throwable e)
            {
               log.error("error in thread: " + name + ", invocation: " + i, e);
               error = true;
            }
         }
         
         ok = !error;
         done = true;
      }
   }
}