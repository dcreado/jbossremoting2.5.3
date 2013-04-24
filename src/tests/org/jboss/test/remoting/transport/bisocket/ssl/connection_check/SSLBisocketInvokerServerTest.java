package org.jboss.test.remoting.transport.bisocket.ssl.connection_check;

import org.jboss.test.remoting.transport.bisocket.ssl.SSLBisocketInvokerConstants;
import org.jboss.test.remoting.transport.socket.ssl.connection_check.InvokerServerTest;

/**
 * This is the concrete test for invoker server.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class SSLBisocketInvokerServerTest
   extends InvokerServerTest implements SSLBisocketInvokerConstants
{
   
   public static void main(String[] args)
   {
      SSLBisocketInvokerServerTest server = new SSLBisocketInvokerServerTest();
      try
      {
         server.setUp();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   protected String getTransport()
   {
      return "sslbisocket";
   }
   
   protected int getPort()
   {
      return bisocketPort + 11;
   }
}
