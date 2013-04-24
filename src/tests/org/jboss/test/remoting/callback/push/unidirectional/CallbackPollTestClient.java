package org.jboss.test.remoting.callback.push.unidirectional;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.CallbackPoller;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public abstract class CallbackPollTestClient extends TestCase
{
   private boolean gotCallback = false;

   public void testCallback() throws Throwable
   {
      Client client = new Client(new InvokerLocator(getLocatorUri()));
      client.connect();
      InvokerCallbackHandler testCallbackHandler = new TestCallbackHandler();
      Map metadata = new HashMap();
      metadata.put(CallbackPoller.CALLBACK_POLL_PERIOD, "1000");
      client.addListener(testCallbackHandler, metadata, "foobar");
      client.invoke("foobar");

      Thread.sleep(4000);

      //Thread.sleep(600000);

      client.removeListener(testCallbackHandler);
      client.disconnect();

      assertTrue(gotCallback);
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

   public class TestCallbackHandler implements InvokerCallbackHandler
   {

      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         System.out.println("callback = " + callback);
         Object o = callback.getCallbackHandleObject();
         if ("foobar".equals(o))
         {
            gotCallback = true;
         }
         else
         {
            System.out.println("CallbackHandleObject was " + o + " and not 'foobar'");
         }
      }
   }
}
