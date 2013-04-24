package org.jboss.remoting.samples.transporter.custom.server;

import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.samples.transporter.basic.CustomerProcessor;
import org.jboss.remoting.samples.transporter.basic.CustomerProcessorImpl;
import org.jboss.remoting.transporter.InternalTransporterServices;
import org.jboss.remoting.transporter.TransporterServer;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.net.InetAddress;


/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RMIServer
{
   private String localLocatorURI = "rmi://localhost:5500";

   private TransporterServer server = null;

   public void start() throws Exception
   {
      initTransporterServices();

      server = TransporterServer.createTransporterServer(localLocatorURI, new CustomerProcessorImpl(),
                                                         CustomerProcessor.class.getName(), true);
   }

   public void stop()
   {
      if (server != null)
      {
         server.stop();
      }
   }

   private void initTransporterServices() throws Exception
   {
      // create MBeanServer
      MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer();

      NetworkRegistry registry = NetworkRegistry.getInstance();

      String host = InetAddress.getLocalHost().getHostAddress();
      JNDIDetector jndiDetector = new JNDIDetector();
      jndiDetector.setPort(JNDIServer.JNDI_PORT);
      jndiDetector.setHost(host);
      jndiDetector.setContextFactory("org.jnp.interfaces.NamingContextFactory");
      jndiDetector.setURLPackage("org.jboss.naming:org.jnp.interfaces");


      InternalTransporterServices transporterService = InternalTransporterServices.getInstance();

      transporterService.setup(mbeanServer,
                               jndiDetector, new ObjectName("remoting:type=Detector,transport=jndi"),
                               registry, new ObjectName("remoting:type=NetworkRegistry"),
                               true, true);

      //TODO: -TME Have to start the detector after setup() call?
      jndiDetector.start();

   }

   public static void main(String[] args)
   {
      RMIServer server = new RMIServer();
      try
      {
         server.start();

         Thread.currentThread().sleep(60000);

      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      finally
      {
         server.stop();
      }
   }
}
