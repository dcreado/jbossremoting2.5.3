package org.jboss.test.remoting.transport.http.clientaddress;

import org.jboss.test.remoting.clientaddress.ClientAddressTestParent;

public class HttpClientAddressTestCase extends ClientAddressTestParent
{
   protected String getTransport()
   {
      return "http";
   }
}
