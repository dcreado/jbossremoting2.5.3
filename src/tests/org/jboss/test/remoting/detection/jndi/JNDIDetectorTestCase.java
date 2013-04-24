/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/

package org.jboss.test.remoting.detection.jndi;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.apache.log4j.Level;
import org.jboss.jrunit.harness.TestDriver;
import org.jboss.remoting.samples.detection.jndi.SimpleJNDIServer;
import org.jboss.remoting.util.SecurityUtility;
import org.jnp.server.Main;

/**
 * This should be used as the main test case for JNDI detector.
 * It will start two JNDIDetectors in seperate instances.  The first
 * will detect the second and then the second will shutdown and the first
 * will detect that the second is no longer present.  This also requires
 * this class to start an instance of the JNP
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class JNDIDetectorTestCase extends TestDriver
{

   /**
    * This method should call the addTestClasses() method with the client class to run, number of clients to run
    * and the server class to run.
    */
   public void declareTestClasses()
   {
      try
      {
         // start JNDI server
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
         
         int port = 1099;
         //String host = "localhost";
         String host = InetAddress.getLocalHost().getHostName();
         Main JNDIServer = new Main();
         if (namingBean != null)
         {
            Class namingBeanClass = Class.forName("org.jnp.server.NamingBean");
            Method setNamingInfoMethod = JNDIServer.getClass().getMethod("setNamingInfo", new Class[] {namingBeanClass});
            setNamingInfoMethod.invoke(JNDIServer, new Object[] {namingBean});
         }
         JNDIServer.setPort(port);
         JNDIServer.setBindAddress(host);
         JNDIServer.start();
         System.out.println("Started JNDI server on " + host + ":" + port);

         addTestClasses(JNDIDetectorTest1.class.getName(),
                        1,
                        JNDIDetectorTest2.class.getName());
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