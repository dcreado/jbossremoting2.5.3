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

package org.jboss.remoting.samples.transporter.proxy;

import org.jboss.remoting.transporter.TransporterClient;
import org.jboss.remoting.transporter.TransporterServer;

import java.util.Random;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class CustomerProcessorImpl implements CustomerProcessor
{
   private String locatorURI = "socket://localhost:5401";

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

      System.out.println("processed customer with new id of " + customerProxy.getCustomerId());
      return customerProxy;
   }

}