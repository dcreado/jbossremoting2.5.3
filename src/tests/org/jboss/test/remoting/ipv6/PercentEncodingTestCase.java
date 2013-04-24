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
package org.jboss.test.remoting.ipv6;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.InvokerLocator;


/**
 * 
 * Unit test for 
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Jan 23, 2008
 * </p>
 */
public class PercentEncodingTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(PercentEncodingTestCase.class);
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
         log.info("java.version: " + System.getProperty("java.version"));
      }
   }

   
   public void tearDown()
   {
   }
   
   
   public void testPercentEncoding() throws Throwable
   {
      log.info("entering " + getName());

      String version = System.getProperty("java.version");
      if (version.startsWith("1.4"))
      {
         log.info("java version is " + version + ". Skipping test");
      }
      else
      {
         String locatorURI = "socket://[fe80::205:9aff:fe3c:7800%7]:7777/";
         InvokerLocator locator = new InvokerLocator(locatorURI);
         assertEquals(locatorURI, locator.getLocatorURI());

         locatorURI = "socket://multihome/?homes=[fe80::205:9aff:fe3c:7800%7]:7777![fe80::214:22ff:feef:68bb%4]:8888";
         locator = new InvokerLocator(locatorURI);
         assertEquals(locatorURI, locator.getLocatorURI());
      }
      log.info(getName() + " PASSES");
      
   }
}