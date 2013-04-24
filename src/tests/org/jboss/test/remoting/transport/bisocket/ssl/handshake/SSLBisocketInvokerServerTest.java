package org.jboss.test.remoting.transport.bisocket.ssl.handshake;

import org.jboss.test.remoting.transport.socket.ssl.handshake.InvokerServerTest;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 */
public class SSLBisocketInvokerServerTest extends InvokerServerTest
{
   protected String getTransport()
   {
      return "sslbisocket";
   }
}
