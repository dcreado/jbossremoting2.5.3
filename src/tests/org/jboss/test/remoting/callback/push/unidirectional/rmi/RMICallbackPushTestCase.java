package org.jboss.test.remoting.callback.push.unidirectional.rmi;

import org.jboss.jrunit.harness.TestDriver;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RMICallbackPushTestCase extends TestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(RMICallbackPushTestClient.class.getName(),
                     1,
                     RMICallbackPushTestServer.class.getName());
   }
}
