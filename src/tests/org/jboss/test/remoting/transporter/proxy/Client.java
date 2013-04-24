package org.jboss.test.remoting.transporter.proxy;

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.jboss.remoting.transporter.TransporterClient;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Client extends TestCase
{
   private String locatorURI = "socket://localhost:5400";

   public void testClientCall() throws Exception
   {
      Customer customer = createCustomer();

      CustomerProcessor customerProcessor = (CustomerProcessor) TransporterClient.createTransporterClient(locatorURI, CustomerProcessor.class);

      System.out.println("Customer to be processed: " + customer);
      ICustomer processedCustomer = customerProcessor.processCustomer(customer);

      assertNotNull(processedCustomer);

      // processedCustomer returned is actually a proxy to the Customer instnace
      // that lives on the server.  So when print it out below, will actually
      // be calling back to the server to get the string (vi toString() call).
      // Notice the output of 'Customer.toString() being called.' on the server side.
      System.out.println("Customer is now: " + processedCustomer);


      TransporterClient.destroyTransporterClient(customerProcessor);


      CompanyProcessor companyProcessor = (CompanyProcessor) TransporterClient.createTransporterClient(locatorURI, CompanyProcessor.class);

      String companyName = companyProcessor.getCompanyName();
      System.out.println("company name = " + companyName);
      assertEquals(CustomerProcessorImpl.COMPANYNAME, companyName);

      TransporterClient.destroyTransporterClient(companyProcessor);


      Auditor audit = (Auditor) TransporterClient.createTransporterClient(locatorURI, Auditor.class);

      int numOfCalls = audit.getNumberOfCalls();
      System.out.println("number of calls = " + numOfCalls);
      assertEquals(1, numOfCalls);

      TransporterClient.destroyTransporterClient(audit);


   }

   private Customer createCustomer()
   {
      Customer cust = new Customer();
      cust.setFirstName("Bob");
      cust.setLastName("Smith");
      Address addr = new Address();
      addr.setStreet("101 Oak Street");
      addr.setCity("Atlanta");
      addr.setState("GA");
      addr.setZip(30249);
      cust.setAddr(addr);

      return cust;
   }

   public static void main(String[] args)
   {

      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("test").setLevel(Level.DEBUG);

      Client client = new Client();
      try
      {
         client.testClientCall();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }


}
