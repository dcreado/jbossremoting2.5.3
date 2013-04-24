/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.remoting.transport.socket.serverlockup;

import org.jboss.jrunit.harness.TestDriver;
import org.jboss.logging.XLevel;
import org.apache.log4j.Level;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 */
public class ServerLockupTestDriver extends TestDriver
{
   // Constants ------------------------------------------------------------------------------------

   public static final String REMOTING_METADATA = "remoting.metadata";
   public static final String JVM_MAX_HEAP_SIZE = "jvm.mx";

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   // Constructors ---------------------------------------------------------------------------------

   // TestDriver overrides -------------------------------------------------------------------------

   public void declareTestClasses()
   {
      addTestClasses(ServerLockupClientTest.class.getName(), 1,
                     ServerLockupServerTest.class.getName());
   }

   protected Level getTestLogLevel()
   {
      return XLevel.TRACE;
   }

   protected Level getTestHarnessLogLevel()
   {
      return Level.INFO;
   }

   /**
    * How long to wait for test results to be returned from the client(s).  If goes longer than the
    * specified limit, will throw an exception and kill the running test cases.  Default value is
    * RESULTS_TIMEOUT.
    */
   protected long getResultsTimeout()
   {
      return 600000;
   }

   /**
    * How long for the server test case to wait for tear down message.  If exceeds timeout,
    * will throw exception. The default value is TEARDOWN_TIMEOUT.
    */
   protected long getTearDownTimeout()
   {
      return 600000;
   }

   /**
    * How long to allow each of the test cases to run their tests.  If exceeds this timeout
    * will throw exception and kill tests. The default value is RUN_TEST_TIMEOUT.
    */
   protected long getRunTestTimeout()
   {
      return 600000;
   }

   protected String getClientJVMArguments()
   {
      String clientJVMArguments = "";

      if (Boolean.getBoolean("clientdebug"))
      {
         clientJVMArguments +=
            "-Xdebug -Xnoagent -Djava.compiler=NONE " +
            "-Xrunjdwp:transport=dt_shmem,server=y,suspend=y,address=client";
      }

      return clientJVMArguments;
   }

   /**
    * Returns the VM arguments to be passed to the VM when creating the server test cases (actually
    * their harness). The default value is null.
    */
   protected String getServerJVMArguments()
   {
      String serverJVMArguments = "";

      if (Boolean.getBoolean("serverdebug"))
      {
         serverJVMArguments +=
            "-Xdebug -Xnoagent -Djava.compiler=NONE " +
            "-Xrunjdwp:transport=dt_shmem,server=y,suspend=y,address=server";
      }

      return serverJVMArguments;
   }


   // Public ---------------------------------------------------------------------------------------

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------

}


