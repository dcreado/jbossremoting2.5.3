package org.jboss.test.remoting.transport.bisocket.timeout.idle;

import org.jboss.test.remoting.transport.socket.timeout.idle.IdleTimeoutTestServer;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class BisocketIdleTimeoutTestServer extends IdleTimeoutTestServer
{
   protected String getTransport()
   {
      return "bisocket";
   }
   
   public static void main(String[] args) throws Throwable
   {
      BisocketIdleTimeoutTestServer rt = new BisocketIdleTimeoutTestServer();
      rt.startServer();
   }
}

