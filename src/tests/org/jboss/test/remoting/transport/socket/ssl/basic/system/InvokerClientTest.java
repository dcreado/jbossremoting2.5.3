package org.jboss.test.remoting.transport.socket.ssl.basic.system;

import junit.framework.TestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.test.remoting.transport.socket.ssl.SSLInvokerConstants;

/**
 * This is the actual concrete test for the invoker client.  Uses socket transport by default.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class InvokerClientTest extends TestCase implements SSLInvokerConstants
{
   private Client client;
   private static final Logger log = Logger.getLogger(InvokerClientTest.class);

   public void init()
   {
      try
      {
         // since doing basic (using default ssl server socket factory)
         // need to set the system properties to the truststore
         String trustStoreFilePath = this.getClass().getResource("../../.truststore").getFile();
         System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);

         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + host + ":" + port);
         client = new Client(locator, "mock");
         client.connect();
      }
      catch (Exception e)
      {
         log.error(e.getMessage(), e);
         e.printStackTrace();
      }
   }

   public void testRemoteCall() throws Throwable
   {
      InvokerClientTest.log.debug("running testRemoteCall()");

//      InvokerClientTest.log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());

      // simple invoke, should return bar
      Object ret = makeInvocation("foo", "bar");
      assertTrue("Result of testRemoteCall() invocation of foo.", "bar".equals(ret));
      if ("bar".equals(ret))
      {
         InvokerClientTest.log.debug("PASS");
      }
      else
      {
         InvokerClientTest.log.debug("FAILED");
      }
      assertEquals("bar", ret);

   }

   protected String getTransport()
   {
      return transport;
   }

   private Object makeInvocation(String method, String param) throws Throwable
   {
      Object ret = client.invoke(new NameBasedInvocation(method,
                                                         new Object[]{param},
                                                         new String[]{String.class.getName()}),
                                 null);

      return ret;
   }

   public void setUp() throws Exception
   {
      init();
   }

   public void tearDown() throws Exception
   {
      if (client != null)
      {
         client.disconnect();
         client = null;
      }
   }

}
