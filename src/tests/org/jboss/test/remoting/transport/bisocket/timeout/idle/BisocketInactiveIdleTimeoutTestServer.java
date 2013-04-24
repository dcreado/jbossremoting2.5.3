package org.jboss.test.remoting.transport.bisocket.timeout.idle;

import org.jboss.test.remoting.transport.socket.timeout.idle.InactiveIdleTimeoutTestServer;

/**
 * This is just like IdleTimeoutTestServer except instead of
 * looking for server threads that are still in read mode waiting for
 * data from client, want to test for idle server threads that finished with
 * client/server connection and are just sitting in the thread pool waiting to
 * be re-used.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class BisocketInactiveIdleTimeoutTestServer extends InactiveIdleTimeoutTestServer
{
   protected String getTransport()
   {
      return "bisocket";
   }
   
   public static void main(String[] args) throws Throwable
   {
      BisocketInactiveIdleTimeoutTestServer rt = new BisocketInactiveIdleTimeoutTestServer();
      rt.setUp();
      Thread.sleep(45000);
      rt.tearDown();
   }
}
