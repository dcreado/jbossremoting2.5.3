package org.jboss.test.remoting.transport.http.ssl.clientaddress;

import java.util.Map;

import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.http.ssl.HTTPSClientInvoker;
import org.jboss.test.remoting.clientaddress.ClientAddressTestParent;

public class HttpsClientAddressTestCase extends ClientAddressTestParent
{
   protected String getTransport()
   {
      return "https";
   }
   
   protected void addExtraServerConfig(Map config)
   {
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_TYPE, "JKS");
      String keyStoreFilePath = this.getClass().getResource("../.keystore").getFile();
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_FILE_PATH, keyStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_KEY_STORE_PASSWORD, "unit-tests-server");
   }
   
   protected void addExtraClientConfig(Map config)
   {
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
      config.put(HTTPSClientInvoker.IGNORE_HTTPS_HOST, "true");
   }
   
   protected void addExtraCallbackConfig(Map config)
   {
      addExtraClientConfig(config);
      config.put(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE, "true");
   }
}
