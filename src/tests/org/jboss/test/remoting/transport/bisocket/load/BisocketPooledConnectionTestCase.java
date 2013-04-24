package org.jboss.test.remoting.transport.bisocket.load;

import org.jboss.test.remoting.transport.socket.load.PooledConnectionTestCase;

public class BisocketPooledConnectionTestCase extends PooledConnectionTestCase
{
   protected String getTransport()
   {
      return "bisocket";
   }
}
