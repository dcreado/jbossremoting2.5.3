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
package org.jboss.test.remoting.locator;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.InvokerLocator;


public class ExtraneousEqualsTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(ExtraneousEqualsTestCase.class);
   
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
   
   
   public void testExtraneousEquals() throws Throwable
   {
      log.info("entering " + getName());
      
      // Create client.
      String locatorURI = "socket://localhost:8080/abc?xyz";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      log.info(locator.getLocatorURI());
      assertEquals(locator.getLocatorURI(), locatorURI);
      
      locatorURI = "socket://localhost:8080/abc?pqr=123&xyz";
      locator = new InvokerLocator(locatorURI);
      log.info(locator.getLocatorURI());
      assertEquals(locator.getLocatorURI(), locatorURI);

      locatorURI = "socket://localhost:8080/abc?pqr&xyz=123";
      locator = new InvokerLocator(locatorURI);
      log.info(locator.getLocatorURI());
      assertEquals(locator.getLocatorURI(), locatorURI);      

      log.info(getName() + " PASSES");
   }
}