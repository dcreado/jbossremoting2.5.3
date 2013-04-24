package org.jboss.test.remoting.transport.bisocket.ssl.clientaddress;

import java.util.Map;

import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.bisocket.Bisocket;
import org.jboss.test.remoting.clientaddress.ClientAddressTestParent;

public class SSLBisocketClientAddressTestCase extends ClientAddressTestParent
{
   protected String getTransport()
   {
      return "sslbisocket";
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
   }
   
   protected void addExtraCallbackConfig(Map config)
   {
      addExtraClientConfig(config);
      config.put(Bisocket.IS_CALLBACK_SERVER, "true");
   }
}
