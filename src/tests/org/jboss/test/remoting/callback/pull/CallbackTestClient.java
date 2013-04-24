package org.jboss.test.remoting.callback.pull;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;

import java.util.List;

/**
 * Tests that two separate clients with separate callback listeners
 * can be distinguished on the server and given different callback messages.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class CallbackTestClient extends TestCase
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5411;

   private String locatorURI = transport + "://" + host + ":" + port;

   private Client remotingClient;
   private CallbackHandler pullCallbackHandler;

   private Client remotingClient2;
   private CallbackHandler pullCallbackHandler2;

   public void createRemotingClient() throws Exception
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Calling remoting server with locator uri of: " + locatorURI);

      remotingClient = new Client(locator);
      remotingClient.connect();

      remotingClient2 = new Client(locator);
      remotingClient2.connect();

   }

   public void setUp() throws Exception
   {
      createRemotingClient();
   }

   public void tearDown() throws Exception
   {
      if(remotingClient != null)
      {
         if(pullCallbackHandler != null)
         {
            try
            {
               remotingClient.removeListener(pullCallbackHandler);
            }
            catch(Throwable throwable)
            {
               throw new Exception(throwable);
            }
         }
         remotingClient.disconnect();
      }
      if(remotingClient2 != null)
      {
         if(pullCallbackHandler2 != null)
         {
            try
            {
               remotingClient2.removeListener(pullCallbackHandler2);
            }
            catch(Throwable throwable)
            {
               throw new Exception(throwable);
            }
         }
         remotingClient2.disconnect();
      }
   }

   public void testPullCallback() throws Throwable
   {
      pullCallbackHandler = new CallbackHandler();
      pullCallbackHandler2 = new CallbackHandler();
      // by passing only the callback handler, will indicate pull callbacks
      remotingClient.addListener(pullCallbackHandler);
      remotingClient2.addListener(pullCallbackHandler2);

      // now make invocation on server, which should cause a callback to happen
      remotingClient.invoke("Do something");
      remotingClient2.invoke("Do something");

      Thread.sleep(5000);

      List callbacks = remotingClient.getCallbacks(pullCallbackHandler);
      List callbacks2 = remotingClient2.getCallbacks(pullCallbackHandler2);

      boolean callbackWorked = false;

      if(callbacks != null && callbacks.size() > 0)
      {
         for(int x = 0; x < callbacks.size(); x++)
         {
            Callback c = (Callback)callbacks.get(x);
            callbackWorked = c.getCallbackObject().equals(remotingClient.getSessionId());
            if(!callbackWorked)
            {
               break;
            }
         }
      }

      assertTrue(callbackWorked);

      boolean callbackWorked2 = false;

      if(callbacks2 != null && callbacks2.size() > 0)
      {
         for(int x = 0; x < callbacks2.size(); x++)
         {
            Callback c = (Callback)callbacks2.get(x);
            callbackWorked2 = c.getCallbackObject().equals(remotingClient2.getSessionId());
            if(!callbackWorked2)
            {
               break;
            }
         }
      }

      assertTrue(callbackWorked2);

   }

   public class CallbackHandler implements InvokerCallbackHandler
   {
      public void handleCallback(Callback callback) throws HandleCallbackException
      {
      }

   }

}
