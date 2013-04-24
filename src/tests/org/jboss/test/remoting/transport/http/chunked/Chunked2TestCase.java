package org.jboss.test.remoting.transport.http.chunked;

import org.jboss.test.remoting.transport.InvokerTestDriver;
import org.jboss.test.remoting.performance.synchronous.PerformanceTestCase;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Chunked2TestCase extends InvokerTestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(ChunkedClient2.class.getName(),
                     1,
                     ChunkedServer.class.getName());
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
      long defaultTimeout = 600000; // default to 10 minutes

      String timeout = System.getProperty(PerformanceTestCase.RESULT_TIMEOUT);
      if(timeout != null && timeout.length() > 0)
      {
         try
         {
            defaultTimeout = Long.parseLong(timeout);
         }
         catch(NumberFormatException e)
         {
            System.out.println("Can not use " + timeout + " as timeout value as is not a number");
         }
      }
      return defaultTimeout;
   }

   /**
    * How long for the server test case to wait for tear down message.  If exceeds timeout,
    * will throw exception.  The default value is TEARDOWN_TIMEOUT.
    *
    * @return
    */
   protected long getTearDownTimeout()
   {
      long defaultTimeout = 600000; // default to 10 minutes

      String timeout = System.getProperty(PerformanceTestCase.TEAR_DOWN_TIMEOUT);
      if(timeout != null && timeout.length() > 0)
      {
         try
         {
            defaultTimeout = Long.parseLong(timeout);
         }
         catch(NumberFormatException e)
         {
            System.out.println("Can not use " + timeout + " as timeout value as is not a number");
         }
      }
      return defaultTimeout;
   }

   /**
    * How long to allow each of the test cases to run their tests.  If exceeds this timeout
    * will throw exception and kill tests.  The default value is RUN_TEST_TIMEOUT.
    *
    * @return
    */
   protected long getRunTestTimeout()
   {
      long defaultTimeout = 600000; // default to 10 minutes

      String timeout = System.getProperty(PerformanceTestCase.RUN_TEST_TIMEOUT);
      if(timeout != null && timeout.length() > 0)
      {
         try
         {
            defaultTimeout = Long.parseLong(timeout);
         }
         catch(NumberFormatException e)
         {
            System.out.println("Can not use " + timeout + " as timeout value as is not a number");
         }
      }
      return defaultTimeout;
   }

}
