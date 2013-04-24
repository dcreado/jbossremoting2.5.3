package org.jboss.test.remoting.transport.bisocket.ssl.builder;

import org.jboss.test.remoting.transport.bisocket.ssl.SSLBisocketInvokerConstants;
import org.jboss.test.remoting.transport.socket.ssl.builder.SSLSocketInvokerTestServer;

/**
 * This is the server for the test case to verify can have push callbacks using ssl where
 * the client mode is used for the client calling back on the callback server living within the
 * client test instance.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class SSLBisocketInvokerTestServer
   extends SSLSocketInvokerTestServer
   implements SSLBisocketInvokerConstants
{

   public static void main(String[] args)
   {
      SSLBisocketInvokerTestServer server = new SSLBisocketInvokerTestServer();
      try
      {
         server.setUp();

         Thread.sleep(600000);
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
      return bisocketPort + 7;
   }
}
