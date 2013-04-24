package org.jboss.test.remoting.transport.rmi.timeout;

import org.jboss.jrunit.harness.TestDriver;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TimeoutTestCase extends TestDriver
{
   public void declareTestClasses()
   {
      addTestClasses(TimeoutClientTest.class.getName(),
                     1,
                     TimeoutServerTest.class.getName());
   }
}
