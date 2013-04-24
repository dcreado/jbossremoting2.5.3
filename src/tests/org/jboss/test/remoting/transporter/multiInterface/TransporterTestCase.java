package org.jboss.test.remoting.transporter.multiInterface;

import org.jboss.jrunit.harness.TestDriver;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TransporterTestCase extends TestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(TestClient.class.getName(),
                     1,
                     TestServerImpl.class.getName());
   }
}
