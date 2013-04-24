/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.remoting.lease.socket;

import org.jboss.jrunit.harness.TestDriver;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 1946 $</tt>
 * $Id: ClientLeasePeriodTestCase.java 1946 2007-01-21 08:38:33Z ovidiu $
 */
public class ClientLeasePeriodTestCase extends TestDriver
{
   // Constants ------------------------------------------------------------------------------------

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   // Constructors ---------------------------------------------------------------------------------

   // TestDriver overrides -------------------------------------------------------------------------

   public void declareTestClasses()
   {
      addTestClasses(ClientLeasePeriodTestClient.class.getName(), 1,
                     ClientLeasePeriodTestServer.class.getName());
   }

   // Public ---------------------------------------------------------------------------------------

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected String getClientJVMArguments()
   {
      if (Boolean.getBoolean("clientdebug"))
      {
         return "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_shmem,server=n,suspend=n,address=jrunit_client";
      }

      return "";
   }

   protected long getResultsTimeout()
   {
      if (Boolean.getBoolean("clientdebug"))
      {
         return 3600000L;
      }

      return super.getResultsTimeout();
   }

   protected long getTearDownTimeout()
   {
      if (Boolean.getBoolean("clientdebug"))
      {
         return 3600000L;
      }

      return super.getTearDownTimeout();
   }

   protected long getRunTestTimeout()
   {
      if (Boolean.getBoolean("clientdebug"))
      {
         return 3600000L;
      }

      return super.getRunTestTimeout();
   }

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------
}
