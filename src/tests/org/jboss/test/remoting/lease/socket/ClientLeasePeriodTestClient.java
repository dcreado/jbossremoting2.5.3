/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.remoting.lease.socket;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.LeasePinger;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 2193 $</tt>
 * $Id: ClientLeasePeriodTestClient.java 2193 2007-02-22 01:16:20Z rsigal $
 */
public class ClientLeasePeriodTestClient extends TestCase
{
   // Constants ------------------------------------------------------------------------------------

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   // Constructors ---------------------------------------------------------------------------------

   // Public ---------------------------------------------------------------------------------------

   public void testLeasePeriodNoLease() throws Throwable
   {
      Client client = new Client(new InvokerLocator(ClientLeasePeriodTestServer.locatorURINoLease));

      assertEquals(-1, client.getLeasePeriod());
   }

   public void testLeasePeriodDefaultLease() throws Throwable
   {
      Map conf = new HashMap();
      conf.put(Client.ENABLE_LEASE, Boolean.TRUE);

      Client client =
         new Client(new InvokerLocator(ClientLeasePeriodTestServer.locatorURILease), conf);

      client.connect();

      assertEquals(LeasePinger.DEFAULT_LEASE_PERIOD, client.getLeasePeriod());
   }

   public void testLeasePeriodCustomLease() throws Throwable
   {
      // the custom lease value should be smaller than the default lease, because the smalles
      // lease interval takes precedence
      long customLeasePeriod = LeasePinger.DEFAULT_LEASE_PERIOD - 7;

      Map conf = new HashMap();
      conf.put(Client.ENABLE_LEASE, Boolean.TRUE);
      conf.put(InvokerLocator.CLIENT_LEASE_PERIOD, Long.toString(customLeasePeriod));

      Client client =
         new Client(new InvokerLocator(ClientLeasePeriodTestServer.locatorURILease), conf);

      client.connect();

      assertEquals(customLeasePeriod, client.getLeasePeriod());

   }

   public void testLeasePeriodTwoClients() throws Throwable
   {
      // the custom lease value should be smaller than the default lease, because the smalles
      // lease interval takes precedence
      long customLeasePeriod = LeasePinger.DEFAULT_LEASE_PERIOD - 9;
      long customLeasePeriod2 = LeasePinger.DEFAULT_LEASE_PERIOD - 10;

      Map conf = new HashMap();
      conf.put(Client.ENABLE_LEASE, Boolean.TRUE);
      conf.put(InvokerLocator.CLIENT_LEASE_PERIOD, Long.toString(customLeasePeriod));

      Client client =
         new Client(new InvokerLocator(ClientLeasePeriodTestServer.locatorURILease), conf);

      client.connect();

      assertEquals(customLeasePeriod, client.getLeasePeriod());

      conf.put(InvokerLocator.CLIENT_LEASE_PERIOD, Long.toString(customLeasePeriod2));
      Client client2 =
         new Client(new InvokerLocator(ClientLeasePeriodTestServer.locatorURILease), conf);

      // this client will get a brand new invoker (because the configuration is different and
      // InvokerRegistry looks at that), so we'll have different LeasePinger instances, so will
      // have different lease periods

      client2.connect();

      assertEquals(customLeasePeriod, client.getLeasePeriod());
      assertEquals(customLeasePeriod2, client2.getLeasePeriod());

   }

   public void testLeasePeriodMultipleClientsSameInvoker() throws Throwable
   {
      Map conf = new HashMap();
      conf.put(Client.ENABLE_LEASE, Boolean.TRUE);

      Client client =
         new Client(new InvokerLocator(ClientLeasePeriodTestServer.locatorURILease), conf);

      client.connect();

      assertEquals(LeasePinger.DEFAULT_LEASE_PERIOD, client.getLeasePeriod());

      Client client2 =
         new Client(new InvokerLocator(ClientLeasePeriodTestServer.locatorURILease), conf);

      client2.connect();

      assertEquals(LeasePinger.DEFAULT_LEASE_PERIOD, client2.getLeasePeriod());

      // test terminating lease

      client2.getInvoker().terminateLease(client2.getSessionId(), -1);
      assertEquals(-1, client2.getLeasePeriod());
      assertEquals(LeasePinger.DEFAULT_LEASE_PERIOD, client.getLeasePeriod());


      // make sure that invoking terminateLease() again on a client with no lease is a noop
      client2.getInvoker().terminateLease(client2.getSessionId(), -1);


      client.getInvoker().terminateLease(client.getSessionId(), -1);
      assertEquals(-1, client2.getLeasePeriod());
      assertEquals(-1, client.getLeasePeriod());

   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------
}
