package org.jboss.test.remoting.transporter.proxy;

import java.io.Serializable;

/**
 * Simple data object to represent customer data, to include
 * Address object.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class Customer implements Serializable, ICustomer
{
   private String firstName = null;
   private String lastName = null;
   private Address addr = null;
   private int customerId = -1;

   public String getFirstName()
   {
      return firstName;
   }

   public void setFirstName(String firstName)
   {
      this.firstName = firstName;
   }

   public String getLastName()
   {
      return lastName;
   }

   public void setLastName(String lastName)
   {
      this.lastName = lastName;
   }

   public Address getAddr()
   {
      return addr;
   }

   public void setAddr(Address addr)
   {
      this.addr = addr;
   }

   public int getCustomerId()
   {
      return customerId;
   }

   public void setCustomerId(int customerId)
   {
      this.customerId = customerId;
   }

   public String toString()
   {
      System.out.println("Customer.toString() being called.");
      StringBuffer buffer = new StringBuffer();
      buffer.append("\nCustomer:\n");
      buffer.append("customer id: " + customerId + "\n");
      buffer.append("first name: " + firstName + "\n");
      buffer.append("last name: " + lastName + "\n");
      buffer.append("street: " + addr.getStreet() + "\n");
      buffer.append("city: " + addr.getCity() + "\n");
      buffer.append("state: " + addr.getState() + "\n");
      buffer.append("zip: " + addr.getZip() + "\n");

      return buffer.toString();
   }


}
