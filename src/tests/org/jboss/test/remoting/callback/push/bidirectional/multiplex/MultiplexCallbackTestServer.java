package org.jboss.test.remoting.callback.push.bidirectional.multiplex;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class MultiplexCallbackTestServer extends ServerTestCase
{
   private String locatorUri = "multiplex://localhost:8999";
   private Connector connector = null;

   public void setUp() throws Exception
   {
      connector = new Connector(locatorUri);
      connector.create();
      connector.addInvocationHandler("test", new MultiplexCallbackTestServer.TestInvocationHandler());
      connector.start();
   }

   public void tearDown() throws Exception
   {
      if (connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      MultiplexCallbackTestServer server = new MultiplexCallbackTestServer();
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

   public class TestInvocationHandler implements ServerInvocationHandler
   {

      private List listeners = new ArrayList();

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
         for (int x = 0; x < listeners.size(); x++)
         {
            InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler) listeners.get(x);
            callbackHandler.handleCallback(new Callback("This is callback payload"));
         }
         return "barfoo";
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         listeners.add(callbackHandler);
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         listeners.remove(callbackHandler);
      }
   }

}
