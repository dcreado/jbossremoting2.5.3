package org.jboss.test.remoting.detection.jndi;

import org.jboss.jrunit.harness.TestDriver;
import org.jboss.remoting.samples.detection.jndi.SimpleJNDIServer;
import org.jboss.remoting.util.SecurityUtility;
import org.jnp.server.Main;
import org.apache.log4j.Level;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

/**
 * This should be used as the main test case for JNDI detector.
 * It will start two JNDIDetectors in seperate instances.  The first
 * will detect the second and then the second will shutdown and the first
 * will detect that the second is no longer present.  This also requires
 * this class to start an instance of the JNP
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class JNDIDetector2TestCase extends TestDriver
{

   /**
    * This method should call the addTestClasses() method with the client class to run, number of clients to run
    * and the server class to run.
    */
   public void declareTestClasses()
   {
      try
      {
         Object namingBean = null;
         Class namingBeanImplClass = null;
         try
         {
            namingBeanImplClass = Class.forName("org.jnp.server.NamingBeanImpl");
            namingBean = namingBeanImplClass.newInstance();
            Method startMethod = namingBeanImplClass.getMethod("start", new Class[] {});
            setSystemProperty("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
            startMethod.invoke(namingBean, new Object[] {});
         }
         catch (Exception e)
         {
            SimpleJNDIServer.println("Cannot find NamingBeanImpl: must be running jdk 1.4");
         }
         
         String host = InetAddress.getLocalHost().getHostAddress();

         Main jserver = new Main();
         if (namingBean != null)
         {
            Class namingBeanClass = Class.forName("org.jnp.server.NamingBean");
            Method setNamingInfoMethod = jserver.getClass().getMethod("setNamingInfo", new Class[] {namingBeanClass});
            setNamingInfoMethod.invoke(jserver, new Object[] {namingBean});
         }
         int port = 2410;
         jserver.setPort(port);
         jserver.setBindAddress(host);
         jserver.setRmiPort(31000);
         jserver.start();
         System.out.println("Started JNDI server on " + host + ":" + port);

         addTestClasses(Client.class.getName(),
                        1,
                        Server.class.getName());
      }
      catch(Exception e)
      {
         System.out.println("Error starting JNDI server.");
         e.printStackTrace();
      }
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
      return 300000;
   }

   /**
    * How long for the server test case to wait for tear down message.  If exceeds timeout,
    * will throw exception.  The default value is TEARDOWN_TIMEOUT.
    *
    * @return
    */
   protected long getTearDownTimeout()
   {
      return 300000;
   }

   /**
    * How long to allow each of the test cases to run their tests.  If exceeds this timeout
    * will throw exception and kill tests.  The default value is RUN_TEST_TIMEOUT.
    *
    * @return
    */
   protected long getRunTestTimeout()
   {
      return 300000;
   }
   
   static private void setSystemProperty(final String name, final String value)
   {
      if (SecurityUtility.skipAccessControl())
      {
         System.setProperty(name, value);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.setProperty(name, value);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
   }
}
