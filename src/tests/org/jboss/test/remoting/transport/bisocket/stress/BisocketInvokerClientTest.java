package org.jboss.test.remoting.transport.bisocket.stress;

import org.jboss.test.remoting.transport.socket.stress.SocketInvokerClientTest;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class BisocketInvokerClientTest extends SocketInvokerClientTest
{

   public String getTransport()
   {
      return "bisocket";
   }

   public static void main(String[] args)
   {
      BisocketInvokerClientTest client = new BisocketInvokerClientTest();
      try
      {
         client.testClientCalls();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}
