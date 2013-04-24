package org.jboss.test.remoting.transport.socket.timeout.idle;

import org.jboss.jrunit.harness.TestDriver;

public class IdleTimeoutTestCase extends TestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(IdleTimeoutClientTest.class.getName(),
                     2,
                     IdleTimeoutTestServer.class.getName());
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

}