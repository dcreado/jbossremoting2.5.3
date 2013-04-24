package org.jboss.test.remoting.transport.bisocket.ssl.timeout;

import org.jboss.test.remoting.transport.socket.ssl.timeout.SSLSocketPerInvocationTimeoutTestCase;

/**
 * See javadoc for PerInvocationTimeoutTestRoot.
 *  
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2348 $
 * <p>
 * Copyright Feb 6, 2007
 * </p>
 */
public class SSLBisocketPerInvocationTimeoutTestCase extends SSLSocketPerInvocationTimeoutTestCase
{  
   protected String getTransport()
   {
      return "sslbisocket";
   }
}
