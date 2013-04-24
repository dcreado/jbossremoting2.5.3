/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.performance.spring.http.client;

import org.apache.log4j.Level;
import org.jboss.jrunit.harness.BenchmarkTestDriver;
import org.jboss.logging.Logger;

import java.io.IOException;

/**
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class SpringHttpPerformanceTestCase extends BenchmarkTestDriver
{
   protected int numberOfClients = 1;

   public static final String REMOTING_TRANSPORT = "remoting.transport";
   public static final String REMOTING_METADATA = "remoting.metadata";
   public static final String REMOTING_SERIALIZATION = "remoting.serialization";
   public static final String PAYLOAD_SIZE = "remoting.payload.size";
   public static final String NUMBER_OF_CLIENTS = "remoting.number_of_clients";
   public static final String NUMBER_OF_CALLS = "remoting.number_of_calls";
   public static final String JVM_MAX_HEAP_SIZE = "jvm.mx";
   public static final String RESULT_TIMEOUT = "jrunit.result_timeout";
   public static final String TEAR_DOWN_TIMEOUT = "jrunit.tear_down_timeout";
   public static final String RUN_TEST_TIMEOUT = "jrunit.run_test_timeout";
   public static final String REMOTING_HOST = "remoting.host";
   private static Logger log = Logger.getLogger(SpringHttpPerformanceTestCase.class);

   public void declareTestClasses()
   {
      //**************** LOGGING ***********************
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.DEBUG);
      //org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);

      org.apache.log4j.SimpleLayout layout = new org.apache.log4j.SimpleLayout();

      try
      {
         org.apache.log4j.FileAppender fileAppender = new org.apache.log4j.FileAppender(layout, "debug_output.log");
         fileAppender.setThreshold(Level.DEBUG);
         fileAppender.setAppend(false);
         org.apache.log4j.Category.getRoot().addAppender(fileAppender);
      }
      catch(IOException e)
      {
         e.printStackTrace();
      }
      //*************** END LOGGING ***********************


      String numOfClients = System.getProperty(NUMBER_OF_CLIENTS);
      if(numOfClients != null && numOfClients.length() > 0)
      {
         try
         {
            numberOfClients = Integer.parseInt(numOfClients);
         }
         catch(NumberFormatException e)
         {
            e.printStackTrace();
         }
      }

      addTestClasses(getClientTestClass(),
                     numberOfClients,
                     SpringHttpPerformanceDummyServer.class.getName());
   }

   protected String getClientTestClass()
   {
      return SpringHttpPerformanceClient.class.getName();
   }

   protected Level getTestHarnessLogLevel()
   {
      return Level.INFO;
      //return Level.DEBUG;
   }

   /**
    * The log level to run as for the test case.
    *
    * @return
    */
   protected Level getTestLogLevel()
   {
      return Level.INFO;
      //return Level.DEBUG;
   }

   /**
    * Returns the VM arguments to be passed to the vm when creating the client test cases (actually their harness).
    * The default value is null.
    *
    * @return
    */
   protected String getClientJVMArguments()
   {
      return getJVMArguments();
//      String args = "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5000 ";
//      args = args + getJVMArguments();
//      return args;
   }

   /**
    * Returns the VM arguments to be passed to the vm when creating the server test cases (actually their harness).
    * The default value is null.
    *
    * @return
    */
   protected String getServerJVMArguments()
   {
      return getJVMArguments();
   }

   /**
    * Returns the VM arguments to be passed to the vm when creating the client and server test cases (actually their harness).
    * The default value is null.
    *
    * @return
    */
   private String getJVMArguments()
   {
      String vmArgs = "";

      String transport = System.getProperty(REMOTING_TRANSPORT);
      if(transport != null && transport.length() > 0)
      {
         vmArgs = "-D" + REMOTING_TRANSPORT + "=" + transport;
      }
      String host = System.getProperty(REMOTING_HOST);
      if(host != null && host.length() > 0)
      {
         vmArgs = "-D" + REMOTING_HOST + "=" + host;
      }
      String serialization = System.getProperty(REMOTING_SERIALIZATION);
      if(serialization != null && serialization.length() > 0)
      {
         vmArgs = vmArgs + " -D" + REMOTING_SERIALIZATION + "=" + serialization;
      }
      String metadata = System.getProperty(REMOTING_METADATA);
      if(metadata != null && metadata.length() > 0)
      {
         vmArgs = vmArgs + " -D" + REMOTING_METADATA + "=" + metadata;
      }
      String payloadSize = System.getProperty(PAYLOAD_SIZE);
      if(payloadSize != null && payloadSize.length() > 0)
      {
         vmArgs = vmArgs + " -D" + PAYLOAD_SIZE + "=" + payloadSize;
      }
      String numOfCalls = System.getProperty(NUMBER_OF_CALLS);
      if(numOfCalls != null && numOfCalls.length() > 0)
      {
         vmArgs = vmArgs + " -D" + NUMBER_OF_CALLS + "=" + numOfCalls;
      }
      String jvmMx = System.getProperty(JVM_MAX_HEAP_SIZE);
      if(jvmMx != null && jvmMx.length() > 0)
      {
         vmArgs = vmArgs + " -Xmx" + jvmMx + "m";
      }
	  log.info(vmArgs);
      return vmArgs;
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
      long defaultTimeout = 6000000; // default to 100 minutes

      String timeout = System.getProperty(RESULT_TIMEOUT);
      if(timeout != null && timeout.length() > 0)
      {
         try
         {
            defaultTimeout = Long.parseLong(timeout);
         }
         catch(NumberFormatException e)
         {
            System.out.println("Can not use " + timeout + " as timeout value as is not a number");
         }
      }
      return defaultTimeout;
   }

   /**
    * How long for the server test case to wait for tear down message.  If exceeds timeout,
    * will throw exception.  The default value is TEARDOWN_TIMEOUT.
    *
    * @return
    */
   protected long getTearDownTimeout()
   {
      long defaultTimeout = 6000000; // default to 100 minutes

      String timeout = System.getProperty(TEAR_DOWN_TIMEOUT);
      if(timeout != null && timeout.length() > 0)
      {
         try
         {
            defaultTimeout = Long.parseLong(timeout);
         }
         catch(NumberFormatException e)
         {
            System.out.println("Can not use " + timeout + " as timeout value as is not a number");
         }
      }
      return defaultTimeout;
   }

   /**
    * How long to allow each of the test cases to run their tests.  If exceeds this timeout
    * will throw exception and kill tests.  The default value is RUN_TEST_TIMEOUT.
    *
    * @return
    */
   protected long getRunTestTimeout()
   {
      long defaultTimeout = 6000000; // default to 100 minutes

      String timeout = System.getProperty(RUN_TEST_TIMEOUT);
      if(timeout != null && timeout.length() > 0)
      {
         try
         {
            defaultTimeout = Long.parseLong(timeout);
         }
         catch(NumberFormatException e)
         {
            System.out.println("Can not use " + timeout + " as timeout value as is not a number");
         }
      }
      return defaultTimeout;
   }


}