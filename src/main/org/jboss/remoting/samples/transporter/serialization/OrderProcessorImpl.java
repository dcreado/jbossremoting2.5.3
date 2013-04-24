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

package org.jboss.remoting.samples.transporter.serialization;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.jboss.remoting.samples.transporter.basic.CustomerProcessor;
import org.jboss.remoting.samples.transporter.basic.CustomerProcessorImpl;
import org.jboss.remoting.samples.transporter.serialization.Order;
import org.jboss.remoting.samples.transporter.serialization.OrderProcessor;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class OrderProcessorImpl implements OrderProcessor
{
   private CustomerProcessor customerProcessor = null;

   public OrderProcessorImpl()
   {
      customerProcessor = new CustomerProcessorImpl();
   }

   public Order processOrder(Order order)
   {
      System.out.println("Incoming order to process from customer.\n" + order.getCustomer());

      // has this customer been processed?
      if(order.getCustomer().getCustomerId() < 0)
      {
         order.setCustomer(customerProcessor.processCustomer(order.getCustomer()));
      }

      List items = order.getItems();
      System.out.println("Items ordered:");
      Iterator itr = items.iterator();
      while(itr.hasNext())
      {
         System.out.println(itr.next());
      }

      order.setOrderId(new Random().nextInt(1000));
      order.setProcessed(true);

      System.out.println("Order processed.  Order id now: " + order.getOrderId());
      return order;
   }

}