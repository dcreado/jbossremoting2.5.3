package org.jboss.test.remoting.stream;

import org.jboss.jrunit.harness.TestDriver;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Streaming2TestCase extends TestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(StreamingConnectorTestClient.class.getName(),
                     1,
                     StreamingTestServer.class.getName());
   }
}
