package org.jboss.test.remoting.transport.bisocket.stress;

import org.jboss.test.remoting.transport.socket.stress.SocketInvokerServerTest;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class BisocketInvokerServerTest extends SocketInvokerServerTest
{
   public String getTransport()
   {
      return "bisocket";
   }

   public static void main(String[] args)
   {
      BisocketInvokerServerTest server = new BisocketInvokerServerTest();
      try
      {
         server.setUp();
//         Thread.currentThread().sleep(300000);
         Thread.sleep(7200000);
         server.tearDown();
         System.out.println("Have torn down test.");
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
}
