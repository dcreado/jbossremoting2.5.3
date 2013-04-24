package org.jboss.test.remoting.lease.socket.multiple;

import org.apache.log4j.Level;
import org.jboss.jrunit.harness.TestDriver;
import org.jboss.logging.XLevel;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketLeaseTestCase extends TestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(SocketLeaseTestClient.class.getName(),
                     1,
                     SocketLeaseTestServer.class.getName());
   }
   
   /**
    * How long to wait for test results to be returned from the client(s).  If goes longer than the
    * specified limit, will throw an exception and kill the running test cases.  Default value is
    * RESULTS_TIMEOUT.
    *
    * @return
    */
   protected long getResultsTimeout()
   {
      return 360000;
   }

   /**
    * How long for the server test case to wait for tear down message.  If exceeds timeout,
    * will throw exception.  The default value is TEARDOWN_TIMEOUT.
    *
    * @return
    */
   protected long getTearDownTimeout()
   {
      return 360000;
   }

   /**
    * How long to allow each of the test cases to run their tests.  If exceeds this timeout
    * will throw exception and kill tests.  The default value is RUN_TEST_TIMEOUT.
    *
    * @return
    */
   protected long getRunTestTimeout()
   {
      return 360000;
   }

   protected Level getTestLogLevel()
   {
      return XLevel.INFO;
   }
   
}
