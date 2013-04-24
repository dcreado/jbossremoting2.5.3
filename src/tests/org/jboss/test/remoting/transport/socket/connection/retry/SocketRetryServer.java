package org.jboss.test.remoting.transport.socket.connection.retry;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.socket.SocketClientInvoker;
import org.jboss.remoting.transport.socket.SocketServerInvoker;

import javax.management.MBeanServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketRetryServer extends ServerTestCase
{
   private Connector connector = null;


   public void setUp() throws Exception
   {
      String locatorUri = getTransport() + "://localhost:" + getPort() + "/?" + SocketClientInvoker.SO_TIMEOUT_FLAG + "=" + 1000 + "&" +
                          SocketServerInvoker.CHECK_CONNECTION_KEY + "=" + Boolean.FALSE;

      connector = new Connector(locatorUri);
      connector.create();
      connector.addInvocationHandler("test", new TestInvocationHandler());
      connector.start();

   }

   public void tearDown()
   {
      if (connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      SocketRetryServer server = new SocketRetryServer();
      try
      {
         server.setUp();

         Thread.sleep(600000);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      finally
      {
         server.tearDown();
      }
   }

   protected String getTransport()
   {
      return "socket";
   }
   
   protected int getPort()
   {
      return 8888;
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
         return "barfoo";
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
