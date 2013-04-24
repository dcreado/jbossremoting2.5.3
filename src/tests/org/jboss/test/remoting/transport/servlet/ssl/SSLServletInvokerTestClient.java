package org.jboss.test.remoting.transport.servlet.ssl;

import org.jboss.test.remoting.transport.web.WebInvokerTestClient;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SSLServletInvokerTestClient extends WebInvokerTestClient
{
   public static final Integer ERROR_RESPONSE_CODE = new Integer(500);

   public String getLocatorURI()
   {
      // since doing basic (using default ssl server socket factory)
      // need to set the system properties to the truststore
      String trustStoreFilePath = this.getClass().getResource("truststore").getFile();
      System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);


      return "sslservlet://localhost:8443/servlet-invoker/ServerInvokerServlet";
      //return "http://localhost:8080/servlet-invoker/ServerInvokerServlet";
   }

   public static void main(String[] args)
   {
      SSLServletInvokerTestClient client = new SSLServletInvokerTestClient();
      try
      {
         client.testPostInvocation();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

}
