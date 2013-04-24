package org.jboss.test.remoting.transport.bisocket.timeout;

import org.jboss.test.remoting.transport.socket.timeout.SocketPerInvocationTimeoutTestCase;


/**
 * See javadoc for PerInvocationTimeoutTestRoot.
 *   
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2340 $
 * <p>
 * Copyright Feb 6, 2007
 * </p>
 */
public class BisocketPerInvocationTimeoutTestCase extends SocketPerInvocationTimeoutTestCase
{  
   protected String getTransport()
   {
      return "bisocket";
   }
}
