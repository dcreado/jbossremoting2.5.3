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

package org.jboss.test.remoting.exception.cannotconnect.rmi;

import org.jboss.logging.Logger;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.invocation.NameBasedInvocation;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class CannotConnectTestCase extends TestCase
{
   private static final Logger log = Logger.getLogger(CannotConnectTestCase.class);

   public void testCannotConnect()
   {

      try
      {
         log.debug("running testCannotConnect()");

         InvokerLocator locator = new InvokerLocator("rmi://localhost:8823");
         Client client = new Client(locator, "mock");
         client.connect();

         log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());

         Object ret = client.invoke(new NameBasedInvocation("foo",
                                                            new Object[]{"bar"},
                                                            new String[]{String.class.getName()}),
                                    null);
      }
      catch(CannotConnectException cce)
      {
         log.debug("Got CannotConnectException as expected.");
         assertTrue(true);
      }
      catch(Throwable tr)
      {
         tr.printStackTrace();
         assertTrue("Did not catch CannotConnectException as expected.", false);
      }
   }
}