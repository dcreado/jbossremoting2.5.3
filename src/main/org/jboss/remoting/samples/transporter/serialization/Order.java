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
import org.jboss.remoting.samples.transporter.basic.Customer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Order
{
   private int orderId = -1;
   private boolean isProcessed = false;
   private Customer customer = null;
   private List items = null;


   public int getOrderId()
   {
      return orderId;
   }

   public void setOrderId(int orderId)
   {
      this.orderId = orderId;
   }

   public boolean isProcessed()
   {
      return isProcessed;
   }

   public void setProcessed(boolean processed)
   {
      isProcessed = processed;
   }

   public Customer getCustomer()
   {
      return customer;
   }

   public void setCustomer(Customer customer)
   {
      this.customer = customer;
   }

   public List getItems()
   {
      return items;
   }

   public void setItems(List items)
   {
      this.items = items;
   }

   public String toString()
   {
      StringBuffer buffer = new StringBuffer();
      buffer.append("\nOrder:\n");
      buffer.append("\nIs processed: " + isProcessed);
      buffer.append("\nOrder id: " + orderId);
      buffer.append(customer.toString());

      buffer.append("\nItems ordered:");
      Iterator itr = items.iterator();
      while(itr.hasNext())
      {
         buffer.append("\n" + itr.next().toString());
      }

      return buffer.toString();

   }


}