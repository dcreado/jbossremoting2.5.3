package org.jboss.test.remoting.transport.socket.ssl.no_connection_check;

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
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
         String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
         System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);

         InvokerLocator locator = new InvokerLocator(getTransport() + "://" + host + ":" + port + "/?" + SocketServerInvoker.CHECK_CONNECTION_KEY + "=" + Boolean.FALSE);
         client = new Client(locator, "mock");
         client.connect();
      }
      catch (Exception e)
      {
         InvokerClientTest.log.error(e.getMessage(), e);
      }
   }

   public void testRemoteCall() throws Throwable
   {
      InvokerClientTest.log.debug("running testRemoteCall()");

      InvokerClientTest.log.debug("client.getInvoker().getLocator()" + client.getInvoker().getLocator());

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
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("test").setLevel(Level.DEBUG);

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
