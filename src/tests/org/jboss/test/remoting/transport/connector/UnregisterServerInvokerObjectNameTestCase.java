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
package org.jboss.test.remoting.transport.connector;

import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;


/**
 * This unit test verifies:
 * 
 * 1. When a Connector creates a ServerInvoker and it has a reference to an MBeanServer, it will
 *    (a) register the ServerInvoker with the MBeanServer, and
 *    (b) it will pass the MBeanServer reference to the ServerInvoker
 *    
 * 2. When a Connector stops and it destroys a ServerInvoker, it will unregister the ServerInvoker
 *    with the MBeanServer.
 *    
 * See JIRA issue JBREM-747
 *    
 *    
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3876 $
 * <p>
 * Copyright May 18, 2007
 * </p>
 */
public class UnregisterServerInvokerObjectNameTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(UnregisterServerInvokerObjectNameTestCase.class);  
   private static boolean firstTime = true;

   
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
   
   
   public void testMBeanServerRegistration() throws Throwable
   {
      log.info("entering " + getName());
      MBeanServer server = null;
      
      try
      {
         server = (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return MBeanServerFactory.createMBeanServer();
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (Exception) e.getCause();
      }
      
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port; 
      InvokerLocator serverLocator = new InvokerLocator(locatorURI);
      log.info("Starting remoting server with locator uri of: " + locatorURI);
      HashMap config = new HashMap();
      config.put(InvokerLocator.FORCE_REMOTE, "true");
      config.put("bluemonkey", "diamond");
      Connector connector = new Connector(serverLocator, config);
      connector.preRegister(server, null);
      connector.create();
      connector.start();
      
      // Verify that the ServerInvoker has a reference to the MBeanServer
      ServerInvoker invoker = connector.getServerInvoker();
      assertNotNull(invoker);
      assertEquals(server, invoker.getMBeanServer());
      
      // Verify that the ServerInvoker is registered with the MBeanServer
      ObjectName objectName = new ObjectName(invoker.getMBeanObjectName());
      assertTrue(server.isRegistered(objectName));
      assertTrue(server.isInstanceOf(objectName, "org.jboss.remoting.ServerInvoker"));
      Object o = server.getAttribute(objectName, "Configuration");;
      assertTrue(o instanceof Map);
      assertEquals("diamond", ((Map) o).get("bluemonkey"));
      
      // Verify that the ServerInvoker is unregistered when the Connector is shut down.
      connector.stop();
      assertFalse(server.isRegistered(objectName));
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
}