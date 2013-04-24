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
package org.jboss.test.remoting.connection.local;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.Connector;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class LocalConnectionValidationTestCase extends TestCase implements ConnectionListener
{
   private Client client = null;
   private Connector connector = null;

   private boolean connectionFailure = false;

   public void setUp() throws Exception
   {
      String locatorUri = "socket://localhost:8888";

      connector = new Connector();
      connector.setInvokerLocator(locatorUri);
      connector.start();

      client = new Client(new InvokerLocator(locatorUri));
      client.connect();
      client.addConnectionListener(this);
   }

   public void testConnection() throws Exception
   {
      Thread.sleep(5000);

      connector.stop();
      connector.destroy();
      connector = null;

      Thread.sleep(10000);

      assertFalse(connectionFailure);

      client.removeConnectionListener(this);
   }

   public void tearDown()
   {
      if(client != null)
      {
         client.disconnect();
      }
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public void handleConnectionException(Throwable throwable, Client client)
   {
      System.out.println("got connection exception.");
      connectionFailure = true;
   }
}