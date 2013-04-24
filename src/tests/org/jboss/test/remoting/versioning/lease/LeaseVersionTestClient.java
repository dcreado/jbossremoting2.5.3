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

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.ClientDisconnectedException;
import org.jboss.remoting.InvokerLocator;


/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright May 08, 2009
 * </p>
 */
public class LeaseVersionTestClient extends TestCase
{
   private static Logger log = Logger.getLogger(LeaseVersionTestClient.class);
   
   protected static long LEASE_PERIOD = 2000;
   protected static String LEASE_PERIOD_STRING = "2000";
   
   private static boolean firstTime = true;

   
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
   
   
   public void testLease() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      InvokerLocator clientLocator = new InvokerLocator(createLocatorURI());
      HashMap clientConfig = new HashMap();
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(clientLocator, clientConfig);
      client.connect();
      log.info("client is connected");
      
      // Test connection.
      assertEquals("abc", client.invoke("abc"));
      log.info("connection is good");
      
      // Test lease behavior.
      client.disconnect();
      client.connect();
      Map info = (Map) client.invoke(LeaseVersionTestServer.GET_LISTENER_INFO);
      log.info("listener info: " + info);
      assertEquals(1, ((Integer)info.get(LeaseVersionTestServer.LISTENER_COUNT)).intValue());
      assertTrue(info.get(LeaseVersionTestServer.THROWABLE) instanceof ClientDisconnectedException);
      client.disconnect();
      
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   protected void addExtraClientConfig(Map config) {}
   
   
   protected String createLocatorURI() throws UnknownHostException
   {
      String locatorURI = getTransport() + "://" + InetAddress.getLocalHost().getHostAddress() + ":" + LeaseVersionTestServer.PORT;
      locatorURI += "/?useClientConnectionIdentity=true";
      String metadata = System.getProperty("remoting.metadata");
      if (metadata != null)
      {
         locatorURI += "&" + metadata;
      }
      return locatorURI;
   }
}