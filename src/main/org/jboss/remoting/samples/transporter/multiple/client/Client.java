package org.jboss.remoting.samples.transporter.multiple.client;

import org.jboss.remoting.samples.transporter.multiple.Account;
import org.jboss.remoting.samples.transporter.multiple.AccountProcessor;
import org.jboss.remoting.samples.transporter.multiple.Address;
import org.jboss.remoting.samples.transporter.multiple.Customer;
import org.jboss.remoting.samples.transporter.multiple.CustomerProcessor;
import org.jboss.remoting.transporter.TransporterClient;


/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Client
{
   private String locatorURI = "socket://localhost:5400";

   public void makeClientCall() throws Exception
   {
      Customer customer = createCustomer();

      CustomerProcessor customerProcessor = (CustomerProcessor) TransporterClient.createTransporterClient(locatorURI, CustomerProcessor.class);

      System.out.println("Customer to be processed: " + customer);
      Customer processedCustomer = customerProcessor.processCustomer(customer);
      System.out.println("Customer is now: " + processedCustomer);

      AccountProcessor accountProcessor = (AccountProcessor) TransporterClient.createTransporterClient(locatorURI, AccountProcessor.class);

      System.out.println("Asking for a new account to be created for customer.");
      Account account = accountProcessor.createAccount(processedCustomer);
      System.out.println("New account: " + account);

      TransporterClient.destroyTransporterClient(customerProcessor);
      TransporterClient.destroyTransporterClient(accountProcessor);

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
      org.jboss.remoting.samples.transporter.multiple.client.Client client = new org.jboss.remoting.samples.transporter.multiple.client.Client();
      try
      {
         client.makeClientCall();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }


}
