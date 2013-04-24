package org.jboss.remoting.transport.bisocket;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.transport.ServerFactory;

import java.util.Map;

/**
 *  
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1681 $
 * <p>
 * Copyright Nov 25, 2006
 * </p>
 */
public class TransportServerFactory implements ServerFactory
{
   public ServerInvoker createServerInvoker(InvokerLocator locator, Map config)
   {
      return new BisocketServerInvoker(locator, config);
   }

   public boolean supportsSSL()
   {
      return false;
   }
}
