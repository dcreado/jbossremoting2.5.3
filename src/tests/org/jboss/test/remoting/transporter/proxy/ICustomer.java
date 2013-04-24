package org.jboss.test.remoting.transporter.proxy;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public interface ICustomer
{
   String getFirstName();

   void setFirstName(String firstName);

   String getLastName();

   void setLastName(String lastName);

   Address getAddr();

   void setAddr(Address addr);

   int getCustomerId();

   void setCustomerId(int customerId);
}
