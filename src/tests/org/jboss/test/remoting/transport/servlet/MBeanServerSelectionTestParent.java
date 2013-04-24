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
package org.jboss.test.remoting.transport.servlet;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.logging.XLevel;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.test.remoting.transport.web.WebInvocationHandler;


/**
 * 
 * Used to test JBREM-746.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Nov 29, 2007
 * </p>
 */
public abstract class MBeanServerSelectionTestParent extends TestCase
{
   private static Logger log = Logger.getLogger(MBeanServerSelectionTestParent.class);
   
   private static boolean firstTime = true;
   
   protected String locatorURI;

   
   public void setUp() throws Exception
   {
      if (firstTime)
      {
         firstTime = false;
         Logger.getLogger("org.jboss.remoting").setLevel(XLevel.TRACE);
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
   
   
   public void testMBeanServerSelection() throws Throwable
   {
      log.info("entering " + getName());
      
      // Start server.
      setupServer();
      
      // Create client.
      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      addExtraClientConfig(clientConfig);
      Client client = new Client(new InvokerLocator(locatorURI), clientConfig);
      client.connect();
      log.info("client is connected: " + locatorURI);
      
      // Test connections.
      assertEquals("abc", client.invoke("copy:abc"));
      log.info("connection is good");
      
      // Verify ServletServerInvoker is using the platform MBeanServer.
      HashMap metadata = new HashMap();
      metadata.put(WebInvocationHandler.DEFAULT_DOMAIN, getDefaultDomain());
      Object o = client.invoke(WebInvocationHandler.CHECK_MBEAN_SERVER, metadata);
      Boolean response = (Boolean) o;
      assertTrue(response.booleanValue());
      
      client.disconnect();
      shutdownServer();
      log.info(getName() + " PASSES");
   }
   
   protected abstract String getDefaultDomain();
   protected void addExtraClientConfig(Map config) {}
   protected void addExtraServerConfig(Map config) {}
   

   protected void setupServer() throws Exception
   {
      locatorURI = "servlet://localhost:8080/servlet-invoker/ServerInvokerServlet";
   }
   
   
   protected void shutdownServer() throws Exception
   {
   }
}