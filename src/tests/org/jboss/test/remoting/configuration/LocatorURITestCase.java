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

package org.jboss.test.remoting.configuration;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.ClientInvoker;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class LocatorURITestCase extends TestCase
{
   private Client remotingClient1;
   private Client remotingClient2;

   public void testLocatorURI() throws Exception
   {
      String locatorURI1 = "http://satellite:8080/ws4ee-encstyle-rpc";
      String locatorURI2 = "http://satellite:8080/ws4ee-encstyle-doc";

      remotingClient1 = new Client(new InvokerLocator(locatorURI1), "test");
      remotingClient1.connect();

      ClientInvoker clientInvoker1 = remotingClient1.getInvoker();

      remotingClient2 = new Client(new InvokerLocator(locatorURI2), "test");
      remotingClient2.connect();

      ClientInvoker clientInvoker2 = remotingClient2.getInvoker();

      assertNotSame("Client invokers should NOT be the same since have different locator uri.", clientInvoker1, clientInvoker2);

   }

   public void tearDown()
   {
      if(remotingClient1 != null)
      {
         remotingClient1.disconnect();
      }
      if(remotingClient2 != null)
      {
         remotingClient2.disconnect();
      }
   }

}