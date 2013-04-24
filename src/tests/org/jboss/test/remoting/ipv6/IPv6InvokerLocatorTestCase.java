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
package org.jboss.test.remoting.ipv6;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.Connector;


/**
 * Unit tests for JBREM-1175.
 * 
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Rev$
 * <p>
 * Copyright Jan 8, 2010
 * </p>
 */
public class IPv6InvokerLocatorTestCase extends TestCase
{
   private static Logger log = Logger.getLogger(IPv6InvokerLocatorTestCase.class);
   
   private static boolean firstTime = true;
   
   protected String host;
   protected int port;
   protected String locatorURI;
   protected InvokerLocator serverLocator;
   protected Connector connector;

   
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
   
   
   public void testHostWithBrackets()
   {
      log.info("entering " + getName());

      Map params = new HashMap();
      params.put("x", "y");
      assertTrue(doTest("socket", "[::]", 1234, "a/b", params, "socket://[::]:1234/a/b?x=y"));
      assertTrue(doTest("socket", "[::1]", 1234, "a/b", params, "socket://[::1]:1234/a/b?x=y"));
      assertTrue(doTest("socket", "[0:0:0:0:0:0:0:1]", 1234, "a/b", params, "socket://[0:0:0:0:0:0:0:1]:1234/a/b?x=y"));
      assertTrue(doTest("socket", "[0:0:0:0:0:0:127.0.0.1]", 1234, "a/b", params, "socket://[0:0:0:0:0:0:127.0.0.1]:1234/a/b?x=y"));
      assertTrue(doTest("socket", "[3ffe:1900:4545:3:200:f8ff:fe21:67cf]", 1234, "a/b", params, "socket://[3ffe:1900:4545:3:200:f8ff:fe21:67cf]:1234/a/b?x=y"));

      String version = System.getProperty("java.version");
      if (version.startsWith("1.4"))
      {
         log.info("java version is " + version + ". Skipping test");
      }
      else
      {
         assertTrue(doTest("socket", "[3ffe:1900:4545:3:200:f8ff:fe21:67cf%5]", 1234, "a/b", params, "socket://[3ffe:1900:4545:3:200:f8ff:fe21:67cf%5]:1234/a/b?x=y"));
      }

      log.info(getName() + " PASSES");
   }
   
   
   public void testHostWithoutBrackets()
   {
      log.info("entering " + getName());

      Map params = new HashMap();
      params.put("x", "y");
      assertTrue(doTest("socket", "::", 1234, "a/b", params, "socket://[::]:1234/a/b?x=y"));
      assertTrue(doTest("socket", "::1", 1234, "a/b", params, "socket://[::1]:1234/a/b?x=y"));
      assertTrue(doTest("socket", "0:0:0:0:0:0:0:1", 1234, "a/b", params, "socket://[0:0:0:0:0:0:0:1]:1234/a/b?x=y"));
      assertTrue(doTest("socket", "0:0:0:0:0:0:127.0.0.1", 1234, "a/b", params, "socket://[0:0:0:0:0:0:127.0.0.1]:1234/a/b?x=y"));
      assertTrue(doTest("socket", "3ffe:1900:4545:3:200:f8ff:fe21:67cf", 1234, "a/b", params, "socket://[3ffe:1900:4545:3:200:f8ff:fe21:67cf]:1234/a/b?x=y"));

      String version = System.getProperty("java.version");
      if (version.startsWith("1.4"))
      {
         log.info("java version is " + version + ". Skipping test");
      }
      else
      {
         assertTrue(doTest("socket", "3ffe:1900:4545:3:200:f8ff:fe21:67cf%5", 1234, "a/b", params, "socket://[3ffe:1900:4545:3:200:f8ff:fe21:67cf%5]:1234/a/b?x=y"));
      }

      log.info(getName() + " PASSES");
   }
   
   
   protected boolean doTest(String protocol, String host, int port, String path, Map params, String expected)
   {

      InvokerLocator locator = new InvokerLocator(protocol, host, port, path, params);
      log.info(host + " -> " + locator.getLocatorURI());
      return expected.equals(locator.getLocatorURI());
   }
}