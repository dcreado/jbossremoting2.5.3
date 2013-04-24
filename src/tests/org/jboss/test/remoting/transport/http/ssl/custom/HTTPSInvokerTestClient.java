package org.jboss.test.remoting.transport.http.ssl.custom;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.web.WebUtil;
import org.jboss.test.remoting.transport.http.HTTPInvokerTestServer;
import org.jboss.test.remoting.transport.http.ssl.SSLInvokerConstants;
import org.jboss.test.remoting.transport.http.ssl.basic.HTTPSInvokerTestServer;
import org.jboss.test.remoting.transport.web.ComplexObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPSInvokerTestClient extends TestCase implements SSLInvokerConstants
{
   public void testInvocation() throws Exception
   {
      Client remotingClient = null;
      try
      {
         Map config = new HashMap();
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_TYPE, "JKS");
         String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_FILE_PATH, trustStoreFilePath);
         config.put(SSLSocketBuilder.REMOTING_TRUST_STORE_PASSWORD, "unit-tests-client");

         String locatorURI = transport + "://" + host + ":" + port;
         InvokerLocator locator = new InvokerLocator(locatorURI);
         System.out.println("Calling remoting server with locator uri of: " + locatorURI);

         // This could have been new Client(locator), but want to show that subsystem param is null
         // Could have also been new Client(locator, "sample");
         remotingClient = new Client(locator, config);
         remotingClient.connect();

         Map metadata = new HashMap();
         metadata.put(Client.RAW, Boolean.TRUE);
         metadata.put("TYPE", "POST");

         Properties headerProps = new Properties();
         headerProps.put("SOAPAction", "http://www.example.com/fibonacci");
         headerProps.put("Content-type", "application/soap+xml");

         metadata.put("HEADER", headerProps);

         Object response = null;

         // test with null return expected
         response = remotingClient.invoke(HTTPInvokerTestServer.NULL_RETURN_PARAM, metadata);

         assertNull(response);

         response = remotingClient.invoke("Do something", metadata);

         assertEquals(HTTPInvokerTestServer.RESPONSE_VALUE, response);

         // test with small object
         headerProps.put("Content-type", WebUtil.BINARY);
         response = remotingClient.invoke(new ComplexObject(2, "foo", true), metadata);

         assertEquals(org.jboss.test.remoting.transport.http.ssl.basic.HTTPSInvokerTestServer.OBJECT_RESPONSE_VALUE, response);

         // test with large object
         response = remotingClient.invoke(new ComplexObject(2, "foo", true, 8000), metadata);

         assertEquals(HTTPSInvokerTestServer.LARGE_OBJECT_RESPONSE_VALUE, response);

      }
      catch(Throwable throwable)
      {
         throw new Exception(throwable);
      }
      finally
      {
         if(remotingClient != null)
         {
            remotingClient.disconnect();
         }
      }


   }


}
