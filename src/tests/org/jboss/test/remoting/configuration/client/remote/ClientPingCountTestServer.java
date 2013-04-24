/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.remoting.configuration.client.remote;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.transport.Connector;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 1941 $</tt>
 * $Id: ClientPingCountTestServer.java 1941 2007-01-21 01:24:52Z ovidiu $
 */
public class ClientPingCountTestServer extends ServerTestCase
{
   // Constants ------------------------------------------------------------------------------------

   public static String locatorURI = "socket://localhost:9999";

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   private Connector connector;

   // Constructors ---------------------------------------------------------------------------------

   // Public ---------------------------------------------------------------------------------------

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected void setUp() throws Exception
   {
      connector = new Connector(locatorURI);
      connector.create();
      connector.start();

      super.setUp();
   }

   protected void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }

      super.tearDown();
   }

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------
}
