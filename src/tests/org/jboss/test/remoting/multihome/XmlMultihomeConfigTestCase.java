/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and individual contributors as indicated
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

package org.jboss.test.remoting.multihome;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.jboss.logging.Logger;
import org.jboss.remoting.transport.Connector;
import org.w3c.dom.Document;

/**
 * Unit tests for JBREM-983.
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3873 $
 * <p>
 * Copyright (c) Jun 13, 2006
 * </p>
 */
public class XmlMultihomeConfigTestCase extends TestCase
{
   protected static Logger log = Logger.getLogger(XmlMultihomeConfigTestCase.class);
   protected static boolean firstTime = true;
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   
   public void setUp()
   {
   }
   

   public void testXmlConfigOneHome() throws Throwable
   {
      Connector connector = new Connector();

      // Create and set xml configuration document.
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("   <invoker transport=\"socket\">");
      buf.append("      <attribute name=\"homes\">");
      buf.append("         <home>host2:2222</home>");
      buf.append("      </attribute>");
      buf.append("      <attribute name=\"path\">a/b</attribute>");
      buf.append("      <attribute name=\"timeout\" isParam=\"true\">1000</attribute>");
      buf.append("   </invoker>");
      buf.append("</config>");
      ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      
      String createdURI = connector.getInvokerLocator();
      log.info("created InvokerLocator:  " + createdURI);
      String expectedURI = "socket://multihome/a/b/?";
      expectedURI       += "homes=host2:2222&";
      expectedURI       += "timeout=1000";
      log.info("expected InvokerLocator: " + expectedURI);
      assertEquals(expectedURI, createdURI);
      
      log.info(getName() + " PASSES");
   }
   
   
   public void testXmlConfigThreeHomes() throws Throwable
   {
      Connector connector = new Connector();

      // Create and set xml configuration document.
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("   <invoker transport=\"socket\">");
      buf.append("      <attribute name=\"homes\">");
      buf.append("         <home>host2:2222</home>");
      buf.append("         <home>host3:3333</home>");
      buf.append("         <home>host4:4444</home>");
      buf.append("      </attribute>");
      buf.append("      <attribute name=\"path\">a/b</attribute>");
      buf.append("      <attribute name=\"timeout\" isParam=\"true\">1000</attribute>");
      buf.append("   </invoker>");
      buf.append("</config>");
      ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      
      String createdURI = connector.getInvokerLocator();
      log.info("created InvokerLocator:  " + createdURI);
      String expectedURI = "socket://multihome/a/b/?";
      expectedURI       += "homes=host2:2222!host3:3333!host4:4444&";
      expectedURI       += "timeout=1000";
      log.info("expected InvokerLocator: " + expectedURI);
      assertEquals(expectedURI, createdURI);
      
      log.info(getName() + " PASSES");
   }
   
   
   public void testXmlConfigWithConnectHomes() throws Throwable
   {
      Connector connector = new Connector();

      // Create and set xml configuration document.
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("   <invoker transport=\"socket\">");
      buf.append("      <attribute name=\"homes\">");
      buf.append("         <home>host2:2222</home>");
      buf.append("         <home>host3:3333</home>");
      buf.append("      </attribute>");
      buf.append("      <attribute name=\"connecthomes\">");
      buf.append("         <connecthome>host4:4444</connecthome>");
      buf.append("         <connecthome>host5:5555</connecthome>");
      buf.append("      </attribute>");
      buf.append("      <attribute name=\"path\">a/b</attribute>");
      buf.append("      <attribute name=\"timeout\" isParam=\"true\">1000</attribute>");
      buf.append("   </invoker>");
      buf.append("</config>");
      ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      
      String createdURI = connector.getInvokerLocator();
      log.info("created InvokerLocator:  " + createdURI);
      String expectedURI = "socket://multihome/a/b/?";
      expectedURI       += "connecthomes=host4:4444!host5:5555&";
      expectedURI       += "homes=host2:2222!host3:3333&";
      expectedURI       += "timeout=1000";
      log.info("expected InvokerLocator: " + expectedURI);
      assertEquals(expectedURI, createdURI);
      
      log.info(getName() + " PASSES");
   }
   
   
   public void testXmlConfigWithServerBindPort() throws Throwable
   {
      Connector connector = new Connector();

      // Create and set xml configuration document.
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("   <invoker transport=\"socket\">");
      buf.append("      <attribute name=\"serverBindAddress\">host1</attribute>");
      buf.append("      <attribute name=\"serverBindPort\">1111</attribute>");
      buf.append("      <attribute name=\"homes\">");
      buf.append("         <home>host2:2222</home>");
      buf.append("         <home>host3</home>");
      buf.append("      </attribute>");
      buf.append("      <attribute name=\"connecthomes\">");
      buf.append("         <connecthome>host4</connecthome>");
      buf.append("         <connecthome>host5:5555</connecthome>");
      buf.append("      </attribute>");
      buf.append("      <attribute name=\"path\">a/b</attribute>");
      buf.append("      <attribute name=\"timeout\" isParam=\"true\">1000</attribute>");
      buf.append("   </invoker>");
      buf.append("</config>");
      ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      
      String createdURI = connector.getInvokerLocator();
      log.info("created InvokerLocator:  " + createdURI);
      String expectedURI = "socket://multihome:1111/a/b/?";
      expectedURI       += "connecthomes=host4:1111!host5:5555&";
      expectedURI       += "homes=host2:2222!host3:1111&";
      expectedURI       += "timeout=1000";
      log.info("expected InvokerLocator: " + expectedURI);
      assertEquals(expectedURI, createdURI);
      
      log.info(getName() + " PASSES");
   }
   
   
   public void testXmlConfigWithClientConnectPort() throws Throwable
   {
      Connector connector = new Connector();

      // Create and set xml configuration document.
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("   <invoker transport=\"socket\">");
      buf.append("      <attribute name=\"serverBindAddress\">host1</attribute>");
      buf.append("      <attribute name=\"serverBindPort\">1111</attribute>");
      buf.append("      <attribute name=\"clientConnectAddress\">host2</attribute>");
      buf.append("      <attribute name=\"clientConnectPort\">2222</attribute>");
      buf.append("      <attribute name=\"homes\">");
      buf.append("         <home>host3:3333</home>");
      buf.append("         <home>host4</home>");
      buf.append("      </attribute>");
      buf.append("      <attribute name=\"connecthomes\">");
      buf.append("         <connecthome>host5</connecthome>");
      buf.append("         <connecthome>host6:6666</connecthome>");
      buf.append("      </attribute>");
      buf.append("      <attribute name=\"path\">a/b</attribute>");
      buf.append("      <attribute name=\"timeout\" isParam=\"true\">1000</attribute>");
      buf.append("   </invoker>");
      buf.append("</config>");
      ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      connector.setConfiguration(xml.getDocumentElement());
      connector.create();
      
      String createdURI = connector.getInvokerLocator();
      log.info("created InvokerLocator:  " + createdURI);
      String expectedURI = "socket://multihome:2222/a/b/?";
      expectedURI       += "connecthomes=host5:2222!host6:6666&";
      expectedURI       += "homes=host3:3333!host4:2222&";
      expectedURI       += "timeout=1000";
      log.info("expected InvokerLocator: " + expectedURI);
      assertEquals(expectedURI, createdURI);
      
      log.info(getName() + " PASSES");
   }
   
   
   public void testXmlConfigWithConnectHomesAndNoHomes() throws Throwable
   {
      Connector connector = new Connector();

      // Create and set xml configuration document.
      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("   <invoker transport=\"socket\">");
      buf.append("      <attribute name=\"connecthomes\">");
      buf.append("         <connecthome>host5</connecthome>");
      buf.append("         <connecthome>host6:6666</connecthome>");
      buf.append("      </attribute>");
      buf.append("      <attribute name=\"path\">a/b</attribute>");
      buf.append("      <attribute name=\"timeout\" isParam=\"true\">1000</attribute>");
      buf.append("   </invoker>");
      buf.append("</config>");
      ByteArrayInputStream bais = new ByteArrayInputStream(buf.toString().getBytes());
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bais);
      connector.setConfiguration(xml.getDocumentElement());

      try
      {
         log.info("=================  EXCEPTION EXPECTED ================");
         connector.create();
         fail("Should have gotten IllegalStateException");
      }
      catch (IllegalStateException e)
      {
         String msg = "Error configuring invoker for connector.  Can not continue without invoker.";
         assertEquals("unexpected message", msg, e.getMessage());
         log.info("got expected IllegalStateException");
         log.info("======================================================");
      }
      
      log.info(getName() + " PASSES");
   }
}