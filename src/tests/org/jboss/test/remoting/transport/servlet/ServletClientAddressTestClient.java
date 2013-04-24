package org.jboss.test.remoting.transport.servlet;

import java.net.InetAddress;

import org.jboss.test.remoting.clientaddress.ClientAddressTestParent;

public class ServletClientAddressTestClient extends ClientAddressTestParent
{
   protected String getTransport()
   {
      return "servlet";
   }
   
   protected String getCallbackTransport()
   {
      return "socket";
   }
   
   protected void setupServer()
   {
      locatorURI =  "servlet://localhost:8080/servlet-invoker/ServerInvokerServlet";
      port = 8080;
   }
   
   protected String reconstructLocator(InetAddress address)
   {
      return "servlet://" + address.getHostAddress() + ":8080/servlet-invoker/ServerInvokerServlet";
   }
   
   protected void shutdownServer()
   {
   }
}
