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

package org.jboss.test.remoting.registry;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.Connector;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class InvokerRegistryUpdateTestCase extends TestCase
{
   private Connector connector;
   private String locatorURI = "socket://localhost";

   public void setUp() throws Exception
   {
      connector = new Connector();
      connector.setInvokerLocator(locatorURI);
      connector.create();
      connector.stop();
   }

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public void testRegistryUpdate()
   {
      ServerInvoker[] serverInvokers = InvokerRegistry.getServerInvokers();
      ServerInvoker serverInvoker = serverInvokers[0];
      InvokerLocator locator = serverInvoker.getLocator();
      String locatorURI = locator.getLocatorURI();
      assertTrue("Invoker locator uri should have changed.", !this.locatorURI.equals(locatorURI));
   }
}