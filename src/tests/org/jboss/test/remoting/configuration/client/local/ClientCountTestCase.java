package org.jboss.test.remoting.configuration.client.local;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ClientCountTestCase extends TestCase
{
   private String locatorUri = "socket://localhost:9999";
   private Connector connector = null;

   public void setUp() throws Exception
   {
      connector = new Connector(locatorUri);
      connector.create();
      connector.addInvocationHandler("test", new TestInvocationHandler());
      connector.start();
   }

   public void tearDown()
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public void testClientConnection() throws Throwable
   {
      InvokerLocator locator = new InvokerLocator(locatorUri);

      Client client1 = new Client(locator);
      Client client2 = new Client(locator);
      Client client3 = new Client(locator);

      client1.connect();
      client1.invoke("foobar");

      try
      {
         client2.invoke("foobar");
         assertTrue("Should have thrown exception instead of reaching this line.", false);
      }
      catch (Throwable throwable)
      {
         assertTrue(true);
      }

      client2.connect();
      client2.invoke("foobar");

      client1.disconnect();

      try
      {
         client1.invoke("foobar");
         assertTrue("Should have thrown exception instead of reaching this line.", false);
      }
      catch (Throwable throwable)
      {
         assertTrue(true);
      }

      client2.invoke("foobar");

      client3.connect();
      client3.invoke("foobar");
      client3.disconnect();

      try
      {
         client3.invoke("foobar");
         assertTrue("Should have thrown exception instead of reaching this line.", false);
      }
      catch (Throwable throwable)
      {
         assertTrue(true);
      }

      client2.invoke("foobar");
      client2.disconnect();

      try
      {
         client2.invoke("foobar");
         assertTrue("Should have thrown exception instead of reaching this line.", false);
      }
      catch (Throwable throwable)
      {
         assertTrue(true);
      }
   }

   public class TestInvocationHandler implements ServerInvocationHandler
   {

      public void setMBeanServer(MBeanServer server)
      {
         //TODO: -TME Implement
      }

      public void setInvoker(ServerInvoker invoker)
      {
         //TODO: -TME Implement
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return null;  //TODO: -TME Implement
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         //TODO: -TME Implement
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         //TODO: -TME Implement
      }
   }

}
