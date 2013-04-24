package org.jboss.test.remoting.callback.push.unidirectional;

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
public abstract class CallbackPollTestServer extends ServerTestCase
{
   private Connector connector = null;

   public void setUp() throws Exception
   {
      connector = new Connector(getLocatorUri());
      connector.create();
      connector.addInvocationHandler("test", new TestInvocationHandler());
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

   private String getLocatorUri()
   {
      return getTransport() + "://" + getHost() + ":" + getPort();
   }

   public abstract String getTransport();

   public String getHost()
   {
      return "localhost";
   }

   public int getPort()
   {
      return 6999;
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
