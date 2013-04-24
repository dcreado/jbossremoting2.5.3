/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.remoting.lease.socket;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.Client;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 2171 $</tt>
 * $Id: ClientLeasePeriodTestServer.java 2171 2007-02-16 05:09:03Z rsigal $
 */
public class ClientLeasePeriodTestServer extends ServerTestCase
{
   // Constants ------------------------------------------------------------------------------------

   public static String locatorURINoLease = "socket://localhost:9900";
   public static String locatorURILease = "socket://localhost:9909";
   protected static Logger log = Logger.getLogger(ClientLeasePeriodTestServer.class);

   // Static ---------------------------------------------------------------------------------------

   public static void main(String[] args)
   {
      try
      {
         ClientLeasePeriodTestServer server = new ClientLeasePeriodTestServer();
         server.setUp();
         Thread.sleep(600000);
         server.tearDown();
      }
      catch (Exception e)
      {
         log.error(e);
      }
   }
   
   
   // Attributes -----------------------------------------------------------------------------------

   private Connector connectorNoLease;
   private Connector connectorLease;
   private ConnectionListener connectionListener;

   // Constructors ---------------------------------------------------------------------------------

   // Public ---------------------------------------------------------------------------------------

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected void setUp() throws Exception
   {
      connectorNoLease = new Connector(ClientLeasePeriodTestServer.locatorURINoLease);
      connectorNoLease.create();
      connectorNoLease.start();

      connectorLease = new Connector(ClientLeasePeriodTestServer.locatorURILease);
      connectorLease.create();
      connectorLease.start();

      connectionListener = new ConnectionListener()
      {
         public void handleConnectionException(Throwable throwable, Client client)
         {
         }
      };

      // activate leases
      connectorLease.addConnectionListener(connectionListener);

      super.setUp();
   }

   protected void tearDown() throws Exception
   {
      if(connectorLease != null)
      {
         connectorLease.removeConnectionListener(connectionListener);
         connectorLease.stop();
         connectorLease.destroy();
      }

      if(connectorNoLease != null)
      {
         connectorNoLease.stop();
         connectorNoLease.destroy();
      }

      super.tearDown();
   }

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------
}
