package org.jboss.test.remoting.transport.http.compression;

import org.jboss.test.remoting.transport.InvokerTestDriver;

/**
 * This should be used as the main test case for the invoker client/server.
 * It will start one instance of the client and one of the server and will
 * gather the test results and report them in standard JUnit format.  When
 * wanting to run JUnit test for invoker, this is the class to use.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class CompressedHTTPInvokerTestCase extends InvokerTestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(CompressedHTTPInvokerTestClient.class.getName(),
                     1,
                     CompressedHTTPInvokerTestServer.class.getName());
   }
}
