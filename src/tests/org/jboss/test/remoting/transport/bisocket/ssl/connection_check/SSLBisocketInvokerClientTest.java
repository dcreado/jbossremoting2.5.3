package org.jboss.test.remoting.transport.bisocket.ssl.connection_check;

import org.jboss.test.remoting.transport.bisocket.ssl.SSLBisocketInvokerConstants;
import org.jboss.test.remoting.transport.socket.ssl.connection_check.InvokerClientTest;

/**
 * This is the actual concrete test for the invoker client.  Uses socket transport by default.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class SSLBisocketInvokerClientTest
   extends InvokerClientTest implements SSLBisocketInvokerConstants
{

   protected String getTransport()
   {
      return "sslbisocket";
   }
   
   protected int getPort()
   {
      return bisocketPort + 11;
   }

}
