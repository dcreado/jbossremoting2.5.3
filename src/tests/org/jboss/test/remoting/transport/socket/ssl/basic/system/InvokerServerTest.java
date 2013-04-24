package org.jboss.test.remoting.transport.socket.ssl.basic.system;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.TestUtil;
import org.jboss.test.remoting.transport.mock.MockServerInvocationHandler;
import org.jboss.test.remoting.transport.socket.ssl.SSLInvokerConstants;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This is the concrete test for invoker server.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class InvokerServerTest extends ServerTestCase implements SSLInvokerConstants
{
   private int serverPort = port;
   private Connector connector = null;

   private static final Logger log = Logger.getLogger(InvokerServerTest.class);

   public void init(Map metatdata) throws Exception
   {
      if (serverPort < 0)
      {
         serverPort = TestUtil.getRandomPort();
      }
      InvokerServerTest.log.debug("port = " + serverPort);

      // since doing basic (using default ssl server socket factory)
      // need to set the system properties to the keystore and password
      String keyStoreFilePath = this.getClass().getResource("../../.keystore").getFile();
      System.setProperty("javax.net.ssl.keyStore", keyStoreFilePath);
      System.setProperty("javax.net.ssl.keyStorePassword", "unit-tests-server");

      connector = new Connector();
      InvokerLocator locator = new InvokerLocator(buildLocatorURI(metatdata));
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();

      connector.addInvocationHandler(getSubsystem(), getServerInvocationHandler());
      connector.start();
   }

   protected String getTransport()
   {
      return transport;
   }

   private String buildLocatorURI(Map metadata)
   {
      if (metadata == null || metadata.size() == 0)
      {
         return getTransport() + "://" + host + ":" + serverPort;
      }
      else
      {
         StringBuffer uriBuffer = new StringBuffer(getTransport() + "://localhost:" + serverPort);

         Set keys = metadata.keySet();
         if (keys.size() > 0)
         {
            uriBuffer.append("/?");
         }

         Iterator itr = keys.iterator();
         while (itr.hasNext())
         {
            String key = (String) itr.next();
            String value = (String) metadata.get(key);
            uriBuffer.append(key + "=" + value + "&");
         }
         return uriBuffer.substring(0, uriBuffer.length() - 1);
      }
   }

   protected String getSubsystem()
   {
      return "mock";
   }

   protected ServerInvocationHandler getServerInvocationHandler()
   {
      return new MockServerInvocationHandler();
   }

   protected void setUp() throws Exception
   {
      init(null);
   }

   protected void tearDown() throws Exception
   {
      if (connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      InvokerServerTest server = new InvokerServerTest();
      try
      {
         server.setUp();

         Thread.sleep(600000);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

}
