package org.jboss.test.remoting.transporter.proxy;

import org.jboss.remoting.transporter.TransporterClient;
import org.jboss.remoting.transporter.TransporterServer;

import java.util.Random;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class CustomerProcessorImpl implements CustomerProcessor, Auditor
{
   private String locatorURI = "socket://localhost:5401";

   private int callCounter = 0;
   public static final String COMPANYNAME = "acme co.";

   /**
    * Takes the customer passed, and if not null and customer id
    * is less than 0, will create a new random id and set it.
    * The customer object returned will be the modified customer
    * object passed.
    *
    * @param customer
    * @return
    */
   public ICustomer processCustomer(Customer customer)
   {
      if (customer != null && customer.getCustomerId() < 0)
      {
         customer.setCustomerId(new Random().nextInt(1000));
      }

      ICustomer customerProxy = null;
      try
      {
         TransporterServer server = TransporterServer.createTransporterServer(locatorURI, customer, ICustomer.class.getName());
         customerProxy = (ICustomer) TransporterClient.createTransporterClient(locatorURI, ICustomer.class);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

      callCounter++;

      System.out.println("processed customer with new id of " + customerProxy.getCustomerId());
      return customerProxy;
   }

   public String getCompanyName()
   {
      return COMPANYNAME;
   }

   public int getNumberOfCalls()
   {
      return callCounter;
   }
}
