package org.jboss.test.remoting.transport.bisocket.timeout.idle;

import org.jboss.test.remoting.transport.socket.timeout.idle.InactiveIdleTimeoutClientTest;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class BisocketInactiveIdleTimeoutClientTest extends InactiveIdleTimeoutClientTest
{
   protected String getTransport()
   {
      return "bisocket";
   }
}
