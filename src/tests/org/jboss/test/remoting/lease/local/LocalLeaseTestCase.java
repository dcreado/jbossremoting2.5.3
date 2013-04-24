package org.jboss.test.remoting.lease.local;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
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
public class LocalLeaseTestCase extends TestCase implements ConnectionListener
{
   private Client client = null;
   private Connector connector = null;

   private boolean connectionFailure = false;

   public void setUp() throws Exception
   {
      String locatorUri = "socket://localhost:8888" + "/?" + InvokerLocator.CLIENT_LEASE + "=" + "true";

      connector = new Connector();
      connector.setInvokerLocator(locatorUri);
      connector.create();
      connector.addInvocationHandler("test", new TestInvocationHandler());
      connector.start();
      connector.addConnectionListener(this);

      client = new Client(new InvokerLocator(locatorUri));
   }

   public void testConnection() throws Throwable
   {
      client.connect();
      client.invoke("foobar");
      Thread.sleep(5000);

      client.disconnect();
      Thread.sleep(20000);

      assertFalse(connectionFailure);

      connector.removeConnectionListener(this);
   }

   public void tearDown()
   {
      if(client != null)
      {
         client.disconnect();
      }
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public void handleConnectionException(Throwable throwable, Client client)
   {
      System.out.println("got connection exception.");
      connectionFailure = true;
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
