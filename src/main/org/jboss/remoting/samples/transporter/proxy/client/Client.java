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

package org.jboss.remoting.samples.transporter.proxy.client;

import org.jboss.remoting.samples.transporter.proxy.Address;
import org.jboss.remoting.samples.transporter.proxy.Customer;
import org.jboss.remoting.samples.transporter.proxy.CustomerProcessor;
import org.jboss.remoting.samples.transporter.proxy.ICustomer;
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
      ICustomer processedCustomer = customerProcessor.processCustomer(customer);
      // processedCustomer returned is actually a proxy to the Customer instnace
      // that lives on the server.  So when print it out below, will actually
      // be calling back to the server to get the string (vi toString() call).
      // Notice the output of 'Customer.toString() being called.' on the server side.
      System.out.println("Customer is now: " + processedCustomer);

      TransporterClient.destroyTransporterClient(customerProcessor);


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
      Client client = new Client();
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