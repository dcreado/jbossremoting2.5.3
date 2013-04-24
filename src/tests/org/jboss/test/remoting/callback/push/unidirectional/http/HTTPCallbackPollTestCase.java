package org.jboss.test.remoting.callback.push.unidirectional.http;

import org.jboss.jrunit.harness.TestDriver;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPCallbackPollTestCase extends TestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(HTTPCallbackPollTestClient.class.getName(),
                     1,
                     HTTPCallbackPollTestServer.class.getName());
   }
}
