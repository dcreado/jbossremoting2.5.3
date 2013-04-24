/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.remoting.configuration.client.remote;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.ConnectionValidator;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 1941 $</tt>
 * $Id: ClientPingCountTestClient.java 1941 2007-01-21 01:24:52Z ovidiu $
 */
public class ClientPingCountTestClient extends TestCase
{
   // Constants ------------------------------------------------------------------------------------

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   // Constructors ---------------------------------------------------------------------------------

   // Public ---------------------------------------------------------------------------------------

   public void testGetPingPeriodOnDisconnectedClient() throws Throwable
   {
      Client client = new Client(new InvokerLocator(ClientPingCountTestServer.locatorURI));

      assertEquals(-1, client.getPingPeriod());
   }

   public void testGetPingPeriodOnConnectedClient() throws Throwable
   {
      Client client = new Client(new InvokerLocator(ClientPingCountTestServer.locatorURI));

      client.connect();

      assertEquals(-1, client.getPingPeriod());
   }

   public void testGetPingPeriodOnClientWithListener() throws Throwable
   {
      Client client = new Client(new InvokerLocator(ClientPingCountTestServer.locatorURI));

      client.connect();

      client.addConnectionListener(new ConnectionListener()
      {
         public void handleConnectionException(Throwable throwable, Client client) {}
      });

      assertEquals(ConnectionValidator.DEFAULT_PING_PERIOD, client.getPingPeriod());
   }

   public void testGetPingPeriodOnClientWithListener2() throws Throwable
   {
      Client client = new Client(new InvokerLocator(ClientPingCountTestServer.locatorURI));

      client.connect();

      client.addConnectionListener(new ConnectionListener()
      {
         public void handleConnectionException(Throwable throwable, Client client) {}
      }, 7865);

      assertEquals(7865, client.getPingPeriod());
   }


   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------
}
