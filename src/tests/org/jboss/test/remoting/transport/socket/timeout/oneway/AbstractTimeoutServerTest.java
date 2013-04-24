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
package org.jboss.test.remoting.transport.socket.timeout.oneway;

import org.jboss.remoting.transport.Connector;
import org.jboss.logging.XLevel;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.ConsoleAppender;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public abstract class AbstractTimeoutServerTest extends ServerTestCase
{
   private Connector connector = null;

   protected String timeout = "5000";

   public void setUp() throws Exception
   {
      connector = new Connector(getLocator());
      connector.create();
      connector.addInvocationHandler("timeout_test", new TimeoutTestHandler());
      connector.start();
   }

   protected abstract String getLocator();

   public void tearDown()
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

}