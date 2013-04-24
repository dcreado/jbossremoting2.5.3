/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.test.remoting.connection.deadlock;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.ConnectionValidator;
import org.jboss.remoting.InvokerLocator;

/**
 * Unit test for JBREM-1070.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version 
 * <p>
 * Copyright Nov 29, 2008
 * </p>
 */
public class DeadlockTestClient extends TestCase
{
   private static Logger log = Logger.getLogger(DeadlockTestClient.class);
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port = 7777;
   protected String locatorURI;
   protected InvokerLocator serverLocator;

   
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
   
   
   public void testForDeadlock() throws Throwable
   {
      log.info("entering " + getName());
      for (int i = 0; i < 10; i++)
      {
         assertTrue("failed execution: " + i, doTest());
         log.info("execution " + i + " PASSES\n");
      }
      log.info(getName() + " PASSES");
   }
   
   
   public boolean doTest() throws Throwable
   {
      // Create client.
      host = InetAddress.getLocalHost().getHostAddress();
      locatorURI = getTransport() + "://" + host + ":" + port;
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "/?" + metadata;
      }
      InvokerLocator clientLocator = new InvokerLocator(locatorURI);
      HashMap clientConfig = new HashMap();
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_TIMEOUT, "5000");
      clientConfig.put(ConnectionValidator.VALIDATOR_PING_PERIOD, "1000");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connections.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Add ConnectionListener.
      TestConnectionListener listener = new TestConnectionListener();
      client.addConnectionListener(listener);
      
      // Wait for notification.
      for (int i = 0; i < 20; i++)
      {
         if (listener.ok)
         {
            break;
         }
         Thread.sleep(1000);
      }
      
      client.disconnect();
      return listener.ok;
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}

   
   static class TestConnectionListener implements ConnectionListener
   {
      public boolean ok;
      
      public void handleConnectionException(Throwable throwable, Client client)
      {
         ok = true;
         log.info("handleConnectionException() called");
      }
   }
}