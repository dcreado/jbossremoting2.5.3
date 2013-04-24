package org.jboss.test.remoting.transport.socket.connection;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.socket.SocketServerInvoker;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketConnectionCheckTestClient extends TestCase
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;

   public void testInvocations() throws Throwable
   {
      Client remotingClient = null;

      try
      {


         String locatorURI = getTransport() + "://" + host + ":" + getPort() + "/?" + SocketServerInvoker.CHECK_CONNECTION_KEY + "=" + Boolean.TRUE;

         // create InvokerLocator with the url type string
         // indicating the target remoting server to call upon.
         InvokerLocator locator = new InvokerLocator(locatorURI);
         System.out.println("Calling remoting server with locator uri of: " + locatorURI);

         remotingClient = new Client(locator);
         remotingClient.connect();
         String request = "Do something";
         System.out.println("Invoking server with request of '" + request + "'");

         long startTime = System.currentTimeMillis();

         int numOfCalls = 10000;
         Object response = null;
         for(int x = 0; x < numOfCalls; x++)
         {
            response = remotingClient.invoke(request);
//            System.out.println("Invocation response: " + response);
         }

         long endTime = System.currentTimeMillis();
         System.out.println("Time to make " + numOfCalls + " was " + (endTime -startTime) + " milliseconds.");

         int callValue = 0;
         if(response instanceof Integer)
         {
            callValue = ((Integer) response).intValue();
         }
         assertEquals(numOfCalls, callValue);

      }
      finally
      {
         if(remotingClient != null)
         {
            remotingClient.disconnect();
         }
      }

   }
   
   protected String getTransport()
   {
      return transport;
   }
   
   protected int getPort()
   {
      return port;
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
      SocketConnectionCheckTestClient client = new SocketConnectionCheckTestClient();
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
