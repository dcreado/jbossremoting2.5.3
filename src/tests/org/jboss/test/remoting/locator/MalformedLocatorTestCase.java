/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.test.remoting.locator;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.util.SecurityUtility;


/**
 * Unit tests for JBREM-1180.
 * 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 * <p>
 * Copyright Feb 17, 2010
 */
public class MalformedLocatorTestCase extends TestCase
{
   private ByteArrayOutputStream baos;
   private PrintStream originalPrintStream;
   private Logger log;
   
   public void setUp() throws Exception
   {
      originalPrintStream = System.out;
      baos = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(baos);
      setOut(ps);
      
      Logger.getLogger("org.jboss.remoting").setLevel(Level.INFO);
      Logger.getLogger("org.jboss.test.remoting").setLevel(Level.INFO);
      String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
      PatternLayout layout = new PatternLayout(pattern);
      ConsoleAppender consoleAppender = new ConsoleAppender(layout);
      Logger.getRootLogger().addAppender(consoleAppender);  
      log = Logger.getLogger(MalformedLocatorTestCase.class);
   }

   
   public void tearDown()
   {
   }
   
   
   public void testMalformedLocatorDefaultLogging() throws Throwable
   {
      log.info("entering " + getName());
      String locatorURI = "bisocket://UNDER_SCORE:4457//?JBM_clientMaxPoolSize=200";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println(locatorURI + " -> " + locator);
      setOut(originalPrintStream);
      String s = new String(baos.toByteArray());
      System.out.println(s);
      assertTrue(s.indexOf("WARN") >= 0);
      assertTrue(s.indexOf("Host resolves to null") >= 0);
      System.out.println(getName() + " PASSES");
   }
   
   
   public void testMalformedLocatorLoggingTurnedOff() throws Throwable
   {
      log.info("entering " + getName());
      setSystemProperty(InvokerLocator.SUPPRESS_HOST_WARNING, "true");
      String locatorURI = "bisocket://UNDER_SCORE:4457//?JBM_clientMaxPoolSize=200";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println(locatorURI + " -> " + locator);
      setOut(originalPrintStream);
      String s = new String(baos.toByteArray());
      System.out.println(s);
      assertTrue(s.indexOf("WARN") == -1);
      assertTrue(s.indexOf("Host resolves to null") == -1);
      System.out.println(getName() + " PASSES");
   }
   
   
   public void testMalformedLocatorLoggingTurnedOn() throws Throwable
   {
      log.info("entering " + getName());
      setSystemProperty(InvokerLocator.SUPPRESS_HOST_WARNING, "false");
      String locatorURI = "bisocket://UNDER_SCORE:4457//?JBM_clientMaxPoolSize=200";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println(locatorURI + " -> " + locator);
      setOut(originalPrintStream);
      String s = new String(baos.toByteArray());
      System.out.println(s);
      assertTrue(s.indexOf("WARN") >= 0);
      assertTrue(s.indexOf("Host resolves to null") >= 0);
      System.out.println(getName() + " PASSES");
   }
   
   
   public void testWellformedLocator() throws Throwable
   {
      log.info("entering " + getName());
      String locatorURI = "bisocket://UNDERSCORE:4457//?JBM_clientMaxPoolSize=200";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println(locatorURI + " -> " + locator);
      setOut(originalPrintStream);
      String s = new String(baos.toByteArray());
      System.out.println(s);
      assertTrue(s.indexOf("WARN") == -1);
      assertTrue(s.indexOf("Host resolves to null") == -1);
      System.out.println(getName() + " PASSES");
   }
   
   
   static private void setSystemProperty(final String name, final String value)
   {
      if (SecurityUtility.skipAccessControl())
      {
         System.setProperty(name, value);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.setProperty(name, value);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
   }
   
   
   static private void setOut(final PrintStream ps)
   {
      if (SecurityUtility.skipAccessControl())
      {
         System.setOut(ps);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               System.setOut(ps);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
   }
}