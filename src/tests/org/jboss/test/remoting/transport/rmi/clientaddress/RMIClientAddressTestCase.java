package org.jboss.test.remoting.transport.rmi.clientaddress;

import org.jboss.test.remoting.clientaddress.ClientAddressTestParent;

public class RMIClientAddressTestCase extends ClientAddressTestParent
{
   protected String getTransport()
   {
      return "rmi";
   }
}
