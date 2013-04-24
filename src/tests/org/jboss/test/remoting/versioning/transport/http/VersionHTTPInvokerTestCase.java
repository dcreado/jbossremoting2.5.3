package org.jboss.test.remoting.versioning.transport.http;

import org.jboss.test.remoting.versioning.transport.VersionInvokerTestCaseBase;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class VersionHTTPInvokerTestCase extends VersionInvokerTestCaseBase
{
   public void declareTestClasses()
   {
      addTestClasses("org.jboss.test.remoting.versioning.transport.http.VersionHTTPInvokerTestClient",
                     1,
                     "org.jboss.test.remoting.transport.http.HTTPInvokerTestServer");
   }
   
   protected String getClientJVMArguments()
   {
      String args = super.getClientJVMArguments();
      String prop = System.getProperty("check_content_type");
      if (prop != null && !"".equals(prop))
      {
         args += " -Dcheck_content_type=" + prop;
      }

      System.out.println("client arg: " + args);
      return args;
   }

}
