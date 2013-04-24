package org.jboss.test.remoting.transport.bisocket.ssl.builder;

import org.jboss.test.remoting.transport.bisocket.ssl.SSLBisocketInvokerConstants;
import org.jboss.test.remoting.transport.socket.ssl.builder.SSLSocketInvokerTestCase;

/**
 * This is the client for the test to make regular ssl based invocation to the server and
 * have a ssl based callback server.  The special test in this case is want to have the callback
 * client that lives on the server to be in client mode so that this client test only has to have
 * a truststore locally, yet still use ssl.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class SSLBisocketInvokerTestClient
   extends SSLSocketInvokerTestCase
   implements SSLBisocketInvokerConstants
{
   protected String getTransport()
   {
      return "sslbisocket";
   }
   
   protected int getPort()
   {
      return bisocketPort + 7;
   }
}
