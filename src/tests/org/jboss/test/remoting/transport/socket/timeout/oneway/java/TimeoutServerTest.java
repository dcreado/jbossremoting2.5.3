/*
* JBoss, a division of Red Hat
* Copyright 2006, Red Hat Middleware, LLC, and individual contributors as indicated
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
package org.jboss.test.remoting.transport.socket.timeout.oneway.java;

import org.jboss.test.remoting.transport.socket.timeout.oneway.AbstractTimeoutServerTest;
import org.jboss.logging.XLevel;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class TimeoutServerTest extends AbstractTimeoutServerTest
{
   protected String getLocator()
   {
      return "socket://localhost:5700/?timeout=" + timeout;
   }

   public static void main(String[] args)
   {

//      Logger.getLogger("org.jboss.remoting").setLevel(Level.DEBUG);
      Logger.getLogger("org.jboss.remoting").setLevel(XLevel.TRACE);
      Logger.getLogger("org.jboss.test.remoting").setLevel(Level.DEBUG);
      String pattern = "[%d{ABSOLUTE}] [%t] %5p (%F:%L) - %m%n";
      PatternLayout layout = new PatternLayout(pattern);
      ConsoleAppender consoleAppender = new ConsoleAppender(layout);
      Logger.getRootLogger().addAppender(consoleAppender);

      AbstractTimeoutServerTest test = new TimeoutServerTest();
      try
      {
         test.setUp();

         Thread.currentThread().sleep(600000);

         test.tearDown();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

}