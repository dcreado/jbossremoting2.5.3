package org.jboss.test.remoting.transport.socket.ssl.connection_check;

import org.apache.log4j.Level;
import org.jboss.jrunit.harness.TestDriver;

/**
 * This should be used as the main test case for the invoker client/server.
 * It will start one instance of the client and one of the server and will
 * gather the test results and report them in standard JUnit format.  When
 * wanting to run JUnit test for invoker, this is the class to use.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class InvokerTestCase extends TestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(InvokerClientTest.class.getName(),
                     1,
                     InvokerServerTest.class.getName());
   }

   protected Level getTestLogLevel()
   {
      return Level.DEBUG;
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
      return 600000;
   }

   /**
    * How long for the server test case to wait for tear down message.  If exceeds timeout,
    * will throw exception.  The default value is TEARDOWN_TIMEOUT.
    *
    * @return
    */
   protected long getTearDownTimeout()
   {
      return 600000;
   }

   /**
    * How long to allow each of the test cases to run their tests.  If exceeds this timeout
    * will throw exception and kill tests.  The default value is RUN_TEST_TIMEOUT.
    *
    * @return
    */
   protected long getRunTestTimeout()
   {
      return 600000;
   }

}
