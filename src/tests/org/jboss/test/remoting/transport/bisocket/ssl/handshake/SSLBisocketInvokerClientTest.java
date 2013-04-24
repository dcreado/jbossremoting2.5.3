package org.jboss.test.remoting.transport.bisocket.ssl.handshake;

import org.jboss.test.remoting.transport.socket.ssl.handshake.InvokerClientTest;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 */
public class SSLBisocketInvokerClientTest extends InvokerClientTest
{
   protected String getTransport()
   {
      return "sslbisocket";
   }
}
