package org.jboss.remoting.transport.sslsocket;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.ServerFactory;

import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TransportServerFactory implements ServerFactory
{
   public ServerInvoker createServerInvoker(InvokerLocator locator, Map config)
   {
      return new SSLSocketServerInvoker(locator, config);
   }

   public boolean supportsSSL()
   {
      return true;
   }
}
