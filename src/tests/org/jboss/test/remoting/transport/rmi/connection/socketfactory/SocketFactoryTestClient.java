package org.jboss.test.remoting.transport.rmi.connection.socketfactory;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketFactoryTestClient extends TestCase
{
   // Default locator values
   private static String transport = "rmi";
   private static String host = "localhost";
   private static int port = 5400;

   public void testInvocations() throws Throwable
   {
      Client remotingClient = null;

      try
      {


         String locatorURI = transport + "://" + host + ":" + port;
         //String locatorURI = transport + "://" + host + ":" + port;

         // create InvokerLocator with the url type string
         // indicating the target remoting server to call upon.
         InvokerLocator locator = new InvokerLocator(locatorURI);
         System.out.println("Calling remoting server with locator uri of: " + locatorURI);

         remotingClient = new Client(locator);
         remotingClient.connect();
         String request = "Do something";
         System.out.println("Invoking server with request of '" + request + "'");

         long startTime = System.currentTimeMillis();

         Object response = remotingClient.invoke(request);
         System.out.println("Invocation response: " + response);

      }
      finally
      {
         if(remotingClient != null)
         {
            remotingClient.disconnect();
         }
      }

   }

   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      if(args != null && args.length == 3)
      {
         transport = args[0];
         host = args[1];
         port = Integer.parseInt(args[2]);
      }
      SocketFactoryTestClient client = new SocketFactoryTestClient();
      try
      {
         client.testInvocations();
      }
      catch(Throwable e)
      {
         e.printStackTrace();
      }
   }


}
