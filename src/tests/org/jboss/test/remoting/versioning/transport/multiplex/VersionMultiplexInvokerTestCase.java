package org.jboss.test.remoting.versioning.transport.multiplex;

import org.jboss.test.remoting.versioning.transport.VersionInvokerTestCaseBase;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class VersionMultiplexInvokerTestCase extends VersionInvokerTestCaseBase
{
//   protected String getJVMArguments()
//   {
//      String vmArgs = super.getJVMArguments();
//      vmArgs = vmArgs + " -D" + Version.PRE_2_0_COMPATIBLE + "=" + Boolean.TRUE.toString();
//      return vmArgs;
//   }

   public void declareTestClasses()
   {
      addTestClasses("org.jboss.test.remoting.transport.multiplex.invoker.MultiplexInvokerClientTest",
                     1,
                     "org.jboss.test.remoting.transport.multiplex.invoker.MultiplexInvokerServerTest");
   }

}
