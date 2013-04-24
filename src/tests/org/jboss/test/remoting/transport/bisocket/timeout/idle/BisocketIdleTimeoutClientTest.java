package org.jboss.test.remoting.transport.bisocket.timeout.idle;

import org.jboss.test.remoting.transport.socket.timeout.idle.IdleTimeoutClientTest;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class BisocketIdleTimeoutClientTest extends IdleTimeoutClientTest
{
   protected String getTransport()
   {
      return "bisocket";
   }
}
