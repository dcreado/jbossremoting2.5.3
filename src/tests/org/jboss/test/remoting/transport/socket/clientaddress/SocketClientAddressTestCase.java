package org.jboss.test.remoting.transport.socket.clientaddress;

import org.jboss.test.remoting.clientaddress.ClientAddressTestParent;

public class SocketClientAddressTestCase extends ClientAddressTestParent
{
   protected String getTransport()
   {
      return "socket";
   }
}
