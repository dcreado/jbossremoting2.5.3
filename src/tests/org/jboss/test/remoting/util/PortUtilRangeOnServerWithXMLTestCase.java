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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;

import javax.management.MBeanServer;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/** 
 * Unit test for JBREM-1139.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2320 $
 * <p>
 * Copyright July 13, 2009.
 * </p>
 */
public class PortUtilRangeOnServerWithXMLTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(PortUtilRangeOnServerWithXMLTestCase.class);

   
   
   public void testUpdateRangeOnServer() throws Exception
   {
      log.info("entering " + getName());
      
      // Test initial values.
      assertEquals(1024, PortUtil.getMinPort());
      assertEquals(65535, PortUtil.getMaxPort());
      
      // Set new values.
      Connector connector = new Connector();
      connector.setConfiguration(getXML(2000, 60000));
      connector.start();
      assertEquals(2000, PortUtil.getMinPort());
      assertEquals(60000, PortUtil.getMaxPort());
      connector.stop();
      
      // Set more restrictive values.
      connector = new Connector();
      connector.setConfiguration(getXML(3000, 50000));
      connector.start();
      assertEquals(3000, PortUtil.getMinPort());
      assertEquals(50000, PortUtil.getMaxPort());
      connector.stop();
      
      // Try to set less restrictive values - should have no effect.
      connector = new Connector();
      connector.setConfiguration(getXML(2000, 60000));
      connector.start();
      assertEquals(3000, PortUtil.getMinPort());
      assertEquals(50000, PortUtil.getMaxPort());
      connector.stop();
            
      // Try to set invalid minPort - should have no effect.
      connector = new Connector();
      connector.setConfiguration(getXML(60000, -1));
      try
      {
         log.info("=====================================");
         log.info("EXPECT ILLEGAL_STATE_EXCEPTION");
         connector.start();
      }
      catch (Exception e)
      {
         log.info(e.getMessage());
         if (e instanceof IllegalStateException
               && e.getMessage() != null
               && e.getMessage().startsWith("Error configuring invoker for connector."))
         {
            log.info("GOT EXPECTED ILLEGAL_STATE_EXCEPTION");
            log.info("=====================================");
         }
         else
         {
            fail("expected IllegalStateException");
         }
      }
      assertEquals(3000, PortUtil.getMinPort());
      assertEquals(50000, PortUtil.getMaxPort());
      connector.stop();
    
      // Try to set invalid maxPort - should have no effect.
      connector = new Connector();
      connector.setConfiguration(getXML(-1, 2000));
      try
      {
         log.info("=====================================");
         log.info("EXPECT ILLEGAL_STATE_EXCEPTION");
         connector.start();
      }
      catch (Exception e)
      {
         log.info(e.getMessage());
         if (e instanceof IllegalStateException
               && e.getMessage() != null
               && e.getMessage().startsWith("Error configuring invoker for connector."))
         {
            log.info("GOT EXPECTED ILLEGAL_STATE_EXCEPTION");
            log.info("=====================================");
         }
         else
         {
            fail("expected IllegalStateException");
         }
      }
      assertEquals(3000, PortUtil.getMinPort());
      assertEquals(50000, PortUtil.getMaxPort());
      connector.stop();
      
      log.info(getName()+ " PASSES");
   }
   
   
   Element getXML(int minPort, int maxPort) throws SAXException, IOException, ParserConfigurationException
   {
      String host = InetAddress.getLocalHost().getHostAddress();
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>\n");
      buf.append("   <invoker transport=\"socket\">\n");
      buf.append("      <attribute name=\"serverBindAddress\">" + host + "</attribute>\n");
      if (minPort > -1)
      {
         buf.append("      <attribute name=\"" + PortUtil.MIN_PORT + "\">" + minPort + "</attribute>\n");
      }
      if (maxPort > -1)
      {
         buf.append("      <attribute name=\"" + PortUtil.MAX_PORT + "\">" + maxPort + "</attribute>\n");
      }
      buf.append("   </invoker>\n");
      buf.append("   <handlers>\n");
      buf.append("      <handler subsystem=\"test\">" + TestInvocationHandler.class.getName() + "</handler>\n");
      buf.append("   </handlers>\n");
      buf.append("</config>\n");
      log.info("\n\n" + buf.toString());
      ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      return xml.getDocumentElement();
   }
   
   
   public static class TestInvocationHandler implements ServerInvocationHandler
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
}