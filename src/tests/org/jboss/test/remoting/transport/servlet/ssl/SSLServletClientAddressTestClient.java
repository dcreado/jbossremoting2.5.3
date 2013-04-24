package org.jboss.test.remoting.transport.servlet.ssl;

import java.net.InetAddress;
import java.util.Map;

import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.test.remoting.clientaddress.ClientAddressTestParent;

public class SSLServletClientAddressTestClient extends ClientAddressTestParent
{
   protected String getTransport()
   {
      return "sslservlet";
   }
   
   protected String getCallbackTransport()
   {
      return "socket";
   }
   
   protected void addExtraServerConfig(Map config)
   {
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStoreFilePath = this.getClass().getResource("keystore").getFile();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
   }
   
   protected void addExtraClientConfig(Map config)
   {
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStoreFilePath = this.getClass().getResource("truststore").getFile();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
   }
   
   protected void setupServer()
   {
      locatorURI =  "sslservlet://localhost:8443/servlet-invoker/ServerInvokerServlet";
      port = 8443;
   }
   
   protected String reconstructLocator(InetAddress address)
   {
      return "sslservlet://" + address.getHostAddress() + ":8443/servlet-invoker/ServerInvokerServlet";
   }
   
   protected void shutdownServer()
   {
   }
}
