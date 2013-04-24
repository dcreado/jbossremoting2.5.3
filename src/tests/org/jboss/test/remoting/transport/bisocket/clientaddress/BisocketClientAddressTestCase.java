package org.jboss.test.remoting.transport.bisocket.clientaddress;

import org.jboss.test.remoting.clientaddress.ClientAddressTestParent;

public class BisocketClientAddressTestCase extends ClientAddressTestParent
{
   protected String getTransport()
   {
      return "bisocket";
   }
}
