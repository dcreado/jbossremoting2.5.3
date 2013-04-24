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
package org.jboss.test.remoting.util;


import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;

/** 
 * Unit test for JBREM-1139.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2320 $
 * <p>
 * Copyright July 13, 2009.
 * </p>
 */
public class PortUtilRangeOnServerTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(PortUtilRangeOnServerTestCase.class);

   
   
   public void testUpdateRangeOnServer() throws Exception
   {
      log.info("entering " + getName());
      
      // Test initial values.
      assertEquals(1024, PortUtil.getMinPort());
      assertEquals(65535, PortUtil.getMaxPort());
      
      // Set new values using configuration mapr.
      Map serverConfig = new HashMap();
      serverConfig.put(PortUtil.MIN_PORT, "2000");
      serverConfig.put(PortUtil.MAX_PORT, "60000");
      String host = InetAddress.getLocalHost().getHostAddress();
      InvokerLocator locator = new InvokerLocator("socket://" + host);
      Connector connector = new Connector(locator, serverConfig);
      connector.start();
      log.info("InvokerLocator: " + connector.getInvokerLocator());
      assertEquals(2000, PortUtil.getMinPort());
      assertEquals(60000, PortUtil.getMaxPort());
      connector.stop();
      
      // Set more restrictive values using configuration map.
      serverConfig.put(PortUtil.MIN_PORT, "3000");
      serverConfig.put(PortUtil.MAX_PORT, "50000");
      connector = new Connector(locator, serverConfig);
      connector.start();
      log.info("InvokerLocator: " + connector.getInvokerLocator());
      assertEquals(3000, PortUtil.getMinPort());
      assertEquals(50000, PortUtil.getMaxPort());
      connector.stop();
      
      // Set more restrictive values with InvokerLocator overriding configuration map.
      serverConfig.put(PortUtil.MIN_PORT, "3500");
      serverConfig.put(PortUtil.MAX_PORT, "45000");
      locator = new InvokerLocator("socket://" + host + "/?" + PortUtil.MIN_PORT + "=4000&" + PortUtil.MAX_PORT + "=40000");
      connector = new Connector(locator, serverConfig);
      connector.start();
      log.info("InvokerLocator: " + connector.getInvokerLocator());
      assertEquals(4000, PortUtil.getMinPort());
      assertEquals(40000, PortUtil.getMaxPort());
      connector.stop();
      
      // Try to set less restrictive values - should have no effect.
      serverConfig.put(PortUtil.MIN_PORT, "2000");
      serverConfig.put(PortUtil.MAX_PORT, "60000");
      locator = new InvokerLocator("socket://" + host);
      connector = new Connector(locator, serverConfig);
      connector.start();
      log.info("InvokerLocator: " + connector.getInvokerLocator());
      assertEquals(4000, PortUtil.getMinPort());
      assertEquals(40000, PortUtil.getMaxPort());
      connector.stop();
      
      // Try to set invalid minPort - should have no effect.
      serverConfig.put(PortUtil.MIN_PORT, "60000");
      serverConfig.remove(PortUtil.MAX_PORT);
      connector = new Connector(locator, serverConfig);
      try
      {
         log.info("=====================================");
         log.info("EXPECT ILLEGAL_STATE_EXCEPTION");
         connector.start();
      }
      catch (Exception e)
      {
         log.info(e.getCause());
         if (e.getCause() instanceof IllegalStateException
               && e.getCause().getMessage() != null
               && e.getCause().getMessage().startsWith("trying to set minPort"))
         {
            log.info("GOT EXPECTED ILLEGAL_STATE_EXCEPTION");
            log.info("=====================================");
         }
         else
         {
            fail("expected IllegalStateException");
         }
      }
      log.info("InvokerLocator: " + connector.getInvokerLocator());
      assertEquals(4000, PortUtil.getMinPort());
      assertEquals(40000, PortUtil.getMaxPort());
      connector.stop();
      
      // Try to set invalid maxPort - should have no effect.
      serverConfig.remove(PortUtil.MIN_PORT);
      serverConfig.put(PortUtil.MAX_PORT, "2000");
      connector = new Connector(locator, serverConfig);
      try
      {
         log.info("=====================================");
         log.info("EXPECT ILLEGAL_STATE_EXCEPTION");
         connector.start();
      }
      catch (Exception e)
      {
         log.info(e.getCause());
         if (e.getCause() instanceof IllegalStateException
               && e.getCause().getMessage() != null
               && e.getCause().getMessage().startsWith("trying to set maxPort"))
         {
            log.info("GOT EXPECTED ILLEGAL_STATE_EXCEPTION");
            log.info("=====================================");
         }
         else
         {
            fail("expected IllegalStateException");
         }
      }
      log.info("InvokerLocator: " + connector.getInvokerLocator());
      assertEquals(4000, PortUtil.getMinPort());
      assertEquals(40000, PortUtil.getMaxPort());
      connector.stop();
      
      log.info(getName()+ " PASSES");
   }
}