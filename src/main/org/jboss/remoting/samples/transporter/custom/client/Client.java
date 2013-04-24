package org.jboss.remoting.samples.transporter.custom.client;

import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.samples.transporter.basic.Address;
import org.jboss.remoting.samples.transporter.basic.Customer;
import org.jboss.remoting.samples.transporter.basic.CustomerProcessor;
import org.jboss.remoting.samples.transporter.custom.server.JNDIServer;
import org.jboss.remoting.samples.transporter.custom.server.SocketServer;
import org.jboss.remoting.transporter.InternalTransporterServices;
import org.jboss.remoting.transporter.TransporterClient;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.net.InetAddress;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Client
{
   private String locatorURI = SocketServer.locatorURI;

   private CustomerProcessor customerProcessor = null;

   public void makeClientCall() throws Exception
   {
      Customer customer = createCustomer();

      System.out.println("Customer to be processed: " + customer);
      Customer processedCustomer = customerProcessor.processCustomer(customer);
      System.out.println("Customer is now: " + processedCustomer);

      //TransporterClient.destroyTransporterClient(customerProcessor);
   }

   public void getCustomerProcessor() throws Exception
   {
      initTransporterServices();

      customerProcessor = (CustomerProcessor) TransporterClient.createTransporterClient(locatorURI, CustomerProcessor.class, true);
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


   private Customer createCustomer()
   {
      Customer cust = new Customer();
      cust.setFirstName("Bob");
      cust.setLastName("Smith");
      Address addr = new Address();
      addr.setStreet("101 Oak Stree");
      addr.setCity("Atlanta");
      addr.setZip(30249);
      cust.setAddr(addr);

      return cust;
   }

   public static void main(String[] args)
   {
      org.jboss.remoting.samples.transporter.custom.client.Client client = new org.jboss.remoting.samples.transporter.custom.client.Client();
      try
      {
         client.getCustomerProcessor();
         while (true)
         {
            try
            {
               client.makeClientCall();
               Thread.currentThread().sleep(5000);
            }
            catch (Exception e)
            {
               e.printStackTrace();
            }
         }
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }


}
