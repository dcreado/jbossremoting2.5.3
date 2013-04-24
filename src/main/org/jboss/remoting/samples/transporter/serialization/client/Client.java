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

package org.jboss.remoting.samples.transporter.serialization.client;

import org.jboss.remoting.samples.transporter.basic.Address;
import org.jboss.remoting.samples.transporter.basic.Customer;
import org.jboss.remoting.samples.transporter.serialization.Order;
import org.jboss.remoting.samples.transporter.serialization.OrderProcessor;
import org.jboss.remoting.transporter.TransporterClient;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Client
{
   private String locatorURI = "socket://localhost:5400/?serializationtype=jboss";

   public void makeClientCall() throws Exception
   {
      Order order = createOrder();

      OrderProcessor orderProcessor = (OrderProcessor) TransporterClient.createTransporterClient(locatorURI, OrderProcessor.class);

      System.out.println("Order to be processed: " + order);
      Order changedOrder = orderProcessor.processOrder(order);
      System.out.println("Order now processed " + changedOrder);

      TransporterClient.destroyTransporterClient(orderProcessor);

   }

   private Order createOrder()
   {
      Order order = new Order();
      Customer customer = createCustomer();
      order.setCustomer(customer);

      List items = new ArrayList();
      items.add("Xbox 360");
      items.add("Wireless controller");
      items.add("Ghost Recon 3");

      order.setItems(items);

      return order;
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