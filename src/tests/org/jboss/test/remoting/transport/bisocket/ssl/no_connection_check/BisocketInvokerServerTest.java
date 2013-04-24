package org.jboss.test.remoting.transport.bisocket.ssl.no_connection_check;

import org.jboss.test.remoting.transport.socket.ssl.no_connection_check.InvokerServerTest;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 */
public class BisocketInvokerServerTest extends InvokerServerTest
{
   protected String getTransport()
   {
      return "sslbisocket";
   }
}
