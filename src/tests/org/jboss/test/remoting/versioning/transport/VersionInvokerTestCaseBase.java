package org.jboss.test.remoting.versioning.transport;

import org.apache.log4j.Level;
import org.jboss.test.remoting.transport.InvokerTestDriver;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public abstract class VersionInvokerTestCaseBase extends InvokerTestDriver
{
   /**
    * Returns the classpath to be added to the classpath used to start the client tests.
    * Default return is null, which means no extra classpath will be added.
    *
    * @return
    */
   protected String getExtendedServerClasspath()
   {
      return System.getProperty("server.path");
   }

   /**
    * Returns the classpath to be added to the classpath used to start the client tests.
    * Default return is null, which means no extra classpath will be added.
    *
    * @return
    */
   protected String getExtendedClientClasspath()
   {
      return System.getProperty("client.path");
   }
   
   protected String getClientJVMArguments()
   {
      String prop = System.getProperty("client.pre_2_0_compatible");
      String args = "";
      if (prop != null && !"".equals(prop))
      {
         args = "-Djboss.remoting.pre_2_0_compatible=" + prop;
      }
      else
      {
         prop = System.getProperty("client.version");
         if (prop != null && !"".equals(prop))
            args = "-Djboss.remoting.version=" + prop;
      }
      prop = System.getProperty("client.check_connection");
      if (prop != null && !"".equals(prop))
      {
         args += " -Dremoting.metadata=socket.check_connection=" + prop;
      }
      System.out.println("client arg: " + args);
      return args;
   }


   protected String getServerJVMArguments()
   {
      String prop = System.getProperty("server.pre_2_0_compatible");
      String args = "";
      if (prop != null && !"".equals(prop))
      {
         args = "-Djboss.remoting.pre_2_0_compatible=" + prop;
      }
      else
      {
         prop = System.getProperty("server.version");
         if (prop != null && !"".equals(prop))
            args = "-Djboss.remoting.version=" + prop;
      }
      prop = System.getProperty("server.check_connection");
      if (prop != null && !"".equals(prop))
      {
         args += " -Dremoting.metadata=socket.check_connection=" + prop;
      }
      System.out.println("server arg: " + args);
      return args;
   }
   

   protected Level getTestHarnessLogLevel()
   {
      return Level.DEBUG;
   }

   protected Level getTestLogLevel()
   {
      return Level.DEBUG;
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
      return 60000;
   }

   /**
    * How long for the server test case to wait for tear down message.  If exceeds timeout,
    * will throw exception.  The default value is TEARDOWN_TIMEOUT.
    *
    * @return
    */
   protected long getTearDownTimeout()
   {
      return 60000;
   }

   /**
    * How long to allow each of the test cases to run their tests.  If exceeds this timeout
    * will throw exception and kill tests.  The default value is RUN_TEST_TIMEOUT.
    *
    * @return
    */
   protected long getRunTestTimeout()
   {
      return 60000;
   }


}
