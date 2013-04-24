package org.jboss.test.remoting.transport.socket.connection.retry;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.socket.SocketClientInvoker;
import org.jboss.remoting.transport.socket.SocketServerInvoker;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketRetryClient extends TestCase
{
   private static int numOfThread = 30;

   public static Throwable throwable = null;

   public void testConnection() throws Throwable
   {
      for (int i = 0; i < numOfThread; i++)
      {
         final int x = i;
         new Thread(new Runnable()
         {
            public void run()
            {
               try
               {
                  new Caller().makeCalls(x, 1);
               }
               catch (Throwable throwable)
               {
                  throwable.printStackTrace();
               }
            }
         }).start();
      }
      Thread.sleep(5000);

      new Thread(new Runnable()
      {
         public void run()
         {
            try
            {
               new Caller().makeCalls(5, 100);
            }
            catch (Throwable throwable)
            {
               throwable.printStackTrace();
            }
         }
      }).start();

      Thread.sleep(10000);

      if (throwable != null)
      {
         throw throwable;
      }
   }


   public class Caller
   {

      public void makeCalls(int sleepTime, int numOfCalls) throws Throwable
      {
         InvokerLocator locator = new InvokerLocator(getTransport() + "://localhost:" + getPort() + "/?" + SocketClientInvoker.SO_TIMEOUT_FLAG + "=" + 1000 + "&" +
                                                     SocketServerInvoker.CHECK_CONNECTION_KEY + "=" + Boolean.FALSE);
         Client client = new Client(locator);
         client.connect();

         String payload = "foobar";

         long startTime = System.currentTimeMillis();

         try
         {
            for (int x = 0; x < numOfCalls; x++)
            {
               Object resp = client.invoke(payload);
               System.out.println("resp = " + resp);
               assertEquals("barfoo", resp);

               Thread.sleep(sleepTime);
            }

            long endTime = System.currentTimeMillis();

            System.out.println("Total time for " + numOfCalls + ": " + (endTime - startTime));
         }
         catch (Throwable thr)
         {
            throwable = thr;
         }
         finally
         {
            client.disconnect();
         }


      }
   }

   public static void main(String[] args)
   {
      SocketRetryClient client = new SocketRetryClient();
      try
      {
         client.testConnection();
      }
      catch (Throwable throwable)
      {
         throwable.printStackTrace();
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
}
