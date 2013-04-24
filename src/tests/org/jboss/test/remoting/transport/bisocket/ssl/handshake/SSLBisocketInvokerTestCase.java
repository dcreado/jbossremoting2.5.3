package org.jboss.test.remoting.transport.bisocket.ssl.handshake;

import org.apache.log4j.Level;
import org.jboss.jrunit.harness.TestDriver;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 */
public class SSLBisocketInvokerTestCase extends TestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(SSLBisocketInvokerClientTest.class.getName(),
                     1,
                     SSLBisocketInvokerServerTest.class.getName());
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
