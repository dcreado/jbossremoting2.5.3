package org.jboss.remoting.transport.sslbisocket;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.ServerFactory;

import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class TransportServerFactory implements ServerFactory
{
   public ServerInvoker createServerInvoker(InvokerLocator locator, Map config)
   {
      return new SSLBisocketServerInvoker(locator, config);
   }

   public boolean supportsSSL()
   {
      return true;
   }
}
