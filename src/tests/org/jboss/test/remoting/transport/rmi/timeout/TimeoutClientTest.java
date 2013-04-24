package org.jboss.test.remoting.transport.rmi.timeout;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TimeoutClientTest extends TestCase
{
   private String locatorURI = "rmi://localhost:8899/?timeout=3000";

   public void testTimeout() throws Exception
   {
      Client client = new Client(new InvokerLocator(locatorURI));
      client.connect();

      //test for client timeout
      try
      {
         client.invoke("foo");

         Thread.currentThread().sleep(5000);
         client.invoke("bar");
         System.out.println("Done making all calls after sleeping.");
      }
      catch(Throwable throwable)
      {
         if(throwable instanceof Exception)
         {
            throw (Exception) throwable;
         }
         else
         {
            throw new Exception(throwable);
         }
      }


      long start = System.currentTimeMillis();

      long end = 0;

      try
      {
         client.invoke("timeout");
         end = System.currentTimeMillis();
      }
      catch(Throwable t)
      {
         System.out.println("Caught exception: " + t.getMessage());
         end = System.currentTimeMillis();
      }

      long executionTime = end - start;
      System.out.println("execution time was " + executionTime);
      boolean timedOut = (executionTime < 10000);
      assertTrue("Socket did not timeout within expected time", timedOut);
   }

}
