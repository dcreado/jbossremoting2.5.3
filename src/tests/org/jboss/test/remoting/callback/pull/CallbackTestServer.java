package org.jboss.test.remoting.callback.pull;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.Connector;

/**
 * Simple remoting server.  Uses inner class SampleInvocationHandler
 * as the invocation target handler class.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class CallbackTestServer extends ServerTestCase
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5411;

   private String locatorURI = transport + "://" + host + ":" + port;

   private Connector connector;

   // String to be returned from invocation handler upon client invocation calls.
   private static final String RESPONSE_VALUE = "This is the return to SampleInvocationHandler invocation";


   public void setupServer() throws Exception
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      connector = new Connector();
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();

      CallbackInvocationHandler invocationHandler = new CallbackInvocationHandler();
      // first parameter is sub-system name.  can be any String value.
      connector.addInvocationHandler("sample", invocationHandler);

      connector.start();

   }

   public void setUp() throws Exception
   {
      setupServer();
   }

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      CallbackTestServer server = new CallbackTestServer();
      try
      {
         server.setUp();
         Thread.sleep(6000000);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }

   }

}
