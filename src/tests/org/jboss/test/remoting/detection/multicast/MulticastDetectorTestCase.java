package org.jboss.test.remoting.detection.multicast;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import junit.framework.TestCase;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class MulticastDetectorTestCase extends TestCase
{

   private MBeanServer server;

   private ObjectName objectName;

   protected void setUp() throws Exception
   {
      super.setUp();
      
      try
      {
          server = (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
          {
             public Object run() throws Exception
             {
                 return MBeanServerFactory.createMBeanServer();
             }
          });
      }
      catch (PrivilegedActionException e)
      {
          throw (Exception) e.getCause();
      }
      
      objectName = new ObjectName("remoting:type=MulticastDetector");
   }

   protected void tearDown() throws Exception
   {
      super.tearDown();
   }

   public void testStopWithoutStart() throws Exception
   {
      MulticastDetector detector = new MulticastDetector();
      server.registerMBean(detector, objectName);
// don't call detector.start();
      Thread.sleep(1000);

      server.unregisterMBean(objectName);
      detector.stop();
   }

   public void testCallingStopTwice() throws Exception
   {
      MulticastDetector detector = new MulticastDetector();
      server.registerMBean(detector, objectName);
      detector.start();
      Thread.sleep(1000);

      server.unregisterMBean(objectName);
      detector.stop();
      detector.stop();
   }
}
