package org.jboss.remoting.samples.detection.jndi.ssl;

import java.util.HashMap;
import java.util.Map;

import org.jboss.remoting.samples.detection.jndi.SimpleDetectorClient;
import org.jboss.remoting.security.SSLSocketBuilder;

/**
 * A SimpleDetectorSSLClient.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1480 $
 */
public class SimpleSSLDetectorClient extends SimpleDetectorClient
{

   public static void main(String[] args)
   {
      println("Starting JBoss/Remoting client... to stop this client, kill it manually via Control-C");
      SimpleDetectorClient client = new SimpleSSLDetectorClient();
      try
      {
         client.setupDetector();

         // let this client run forever - welcoming new servers when then come online
         while(true)
         {
            Thread.sleep(1000);
         }
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }

      println("Stopping JBoss/Remoting client");
   }

   
   protected Map getConfiguration()
   {
      Map config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
      String trustStoreFilePath = getClass().getResource("truststore").getFile();
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
      config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");
      return config;
   }
}
