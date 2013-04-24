package org.jboss.test.remoting.callback.push.unidirectional.http;

import org.jboss.jrunit.harness.TestDriver;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPCallbackPushTestCase extends TestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(HTTPCallbackPushTestClient.class.getName(),
                     1,
                     HTTPCallbackPushTestServer.class.getName());
   }
}
