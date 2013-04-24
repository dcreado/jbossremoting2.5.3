package org.jboss.test.remoting.transporter.proxy;


/**
 * Interface to be implemented for classes that process
 * a customer object (which means basically setting customer
 * number).
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public interface CustomerProcessor extends CompanyProcessor
{
   /**
    * Process a customer object.  Implementors
    * should ensure that the customer object
    * passed as parameter should have its internal
    * state changed somehow and returned.
    *
    * @param customer
    * @return
    */
   public ICustomer processCustomer(Customer customer);
}
