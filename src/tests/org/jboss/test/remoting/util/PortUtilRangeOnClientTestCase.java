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
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
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
public class PortUtilRangeOnClientTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(PortUtilRangeOnClientTestCase.class);

   
   public void testUpdateRangeOnClient() throws Exception
   {
      log.info("entering " + getName());
      
      // Test initial values.
      assertEquals(1024, PortUtil.getMinPort());
      assertEquals(65535, PortUtil.getMaxPort());
      
      // Set new values using configuration map.
      Map clientConfig = new HashMap();
      clientConfig.put(PortUtil.MIN_PORT, "2000");
      clientConfig.put(PortUtil.MAX_PORT, "60000");
      String host = InetAddress.getLocalHost().getHostAddress();
      InvokerLocator locator = new InvokerLocator("socket://" + host);
      Client client = new Client(locator, clientConfig);
      assertEquals(2000, PortUtil.getMinPort());
      assertEquals(60000, PortUtil.getMaxPort());
      
      // Set more restrictive values using configuration map.
      clientConfig.put(PortUtil.MIN_PORT, "3000");
      clientConfig.put(PortUtil.MAX_PORT, "50000");
      client = new Client(locator, clientConfig);
      assertEquals(3000, PortUtil.getMinPort());
      assertEquals(50000, PortUtil.getMaxPort());
      
      // Set more restrictive values with InvokerLocator overriding configuration map.
      clientConfig.put(PortUtil.MIN_PORT, "3500");
      clientConfig.put(PortUtil.MAX_PORT, "45000");
      locator = new InvokerLocator("socket://" + host + "/?" +  PortUtil.MIN_PORT + "=4000&" + PortUtil.MAX_PORT + "=40000");
      client = new Client(locator, clientConfig);
      assertEquals(4000, PortUtil.getMinPort());
      assertEquals(40000, PortUtil.getMaxPort());
      
      // Try to set less restrictive values - should have no effect.
      clientConfig.put(PortUtil.MIN_PORT, "2000");
      clientConfig.put(PortUtil.MAX_PORT, "60000");
      locator = new InvokerLocator("socket://" + host);
      client = new Client(locator, clientConfig);
      assertEquals(4000, PortUtil.getMinPort());
      assertEquals(40000, PortUtil.getMaxPort());
      
      // Try to set invalid minPort - should have no effect.
      clientConfig.put(PortUtil.MIN_PORT, "60000");
      clientConfig.remove(PortUtil.MAX_PORT);
      try
      {
         log.info("=====================================");
         log.info("EXPECT ILLEGAL_STATE_EXCEPTION");
         client = new Client(locator, clientConfig);
      }
      catch (Exception e)
      {
         log.info(e.getMessage());
         if (e instanceof IllegalStateException
               && e.getMessage() != null
               && e.getMessage().startsWith("trying to set minPort"))
         {
            log.info("GOT EXPECTED ILLEGAL_STATE_EXCEPTION");
            log.info("=====================================");
         }
         else
         {
            fail("expected IllegalStateException");
         }
      }
      assertEquals(4000, PortUtil.getMinPort());
      assertEquals(40000, PortUtil.getMaxPort());
      
      // Try to set invalid maxPort - should have no effect.
      clientConfig.remove(PortUtil.MIN_PORT);
      clientConfig.put(PortUtil.MAX_PORT, "2000");
      try
      {
         log.info("=====================================");
         log.info("EXPECT ILLEGAL_STATE_EXCEPTION");
         client = new Client(locator, clientConfig);
      }
      catch (Exception e)
      {
         log.info(e.getMessage());
         if (e instanceof IllegalStateException
               && e.getMessage() != null
               && e.getMessage().startsWith("trying to set maxPort"))
         {
            log.info("GOT EXPECTED ILLEGAL_STATE_EXCEPTION");
            log.info("=====================================");
         }
         else
         {
            fail("expected IllegalStateException");
         }
      }
      assertEquals(4000, PortUtil.getMinPort());
      assertEquals(40000, PortUtil.getMaxPort());
      
      log.info(getName()+ " PASSES");
   }

}