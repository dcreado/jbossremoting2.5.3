/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.remoting.configuration.client.remote;

import org.jboss.jrunit.harness.TestDriver;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 1941 $</tt>
 * $Id: ClientPingCountTestCase.java 1941 2007-01-21 01:24:52Z ovidiu $
 */
public class ClientPingCountTestCase extends TestDriver
{
   // Constants ------------------------------------------------------------------------------------

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   // Constructors ---------------------------------------------------------------------------------

   // TestDriver overrides -------------------------------------------------------------------------

   public void declareTestClasses()
   {
      addTestClasses(ClientPingCountTestClient.class.getName(), 1,
                     ClientPingCountTestServer.class.getName());
   }

   // Public ---------------------------------------------------------------------------------------

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------
}
