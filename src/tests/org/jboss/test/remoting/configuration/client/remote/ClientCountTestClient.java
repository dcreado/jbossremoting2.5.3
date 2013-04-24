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
package org.jboss.test.remoting.configuration.client.remote;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ClientCountTestClient extends TestCase
{
   private String locatorUri = "socket://localhost:9999";

   public void testClientConnection() throws Throwable
   {
      InvokerLocator locator = new InvokerLocator(locatorUri);

      Client client1 = new Client(locator);
      Client client2 = new Client(locator);
      Client client3 = new Client(locator);

      client1.connect();
      client1.invoke("foobar");

      try
      {
         client2.invoke("foobar");
         assertTrue("Should have thrown exception instead of reaching this line.", false);
      }
      catch (Throwable throwable)
      {
         assertTrue(true);
      }

      client2.connect();
      client2.invoke("foobar");

      client1.disconnect();

      try
      {
         client1.invoke("foobar");
         assertTrue("Should have thrown exception instead of reaching this line.", false);
      }
      catch (Throwable throwable)
      {
         assertTrue(true);
      }

      client2.invoke("foobar");

      client3.connect();
      client3.invoke("foobar");
      client3.disconnect();

      try
      {
         client3.invoke("foobar");
         assertTrue("Should have thrown exception instead of reaching this line.", false);
      }
      catch (Throwable throwable)
      {
         assertTrue(true);
      }

      client2.invoke("foobar");
      client2.disconnect();

      try
      {
         client2.invoke("foobar");
         assertTrue("Should have thrown exception instead of reaching this line.", false);
      }
      catch (Throwable throwable)
      {
         assertTrue(true);
      }


   }
}