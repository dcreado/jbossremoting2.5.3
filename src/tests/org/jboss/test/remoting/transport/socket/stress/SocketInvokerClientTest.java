package org.jboss.test.remoting.transport.socket.stress;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketInvokerClientTest
{
   private int numOfThreads = 250;
//   private int numOfCalls = 1000;
   private int numOfCalls = 1000000;

//   private int maxClientPoolSize = 50;
   private int maxClientPoolSize = 250;

   private boolean[] finishedThreads = new boolean[numOfThreads];

   public String getTransport()
   {
      return "socket";
   }

   public void testClientCalls() throws Exception
   {
      Thread currentThread = null;

      for (int x = 0; x < numOfThreads; x++)
      {
         currentThread = new Thread(new Runnable()
         {
            public void run()
            {

               Client client = null;
               try
               {
                  InvokerLocator locator = new InvokerLocator(getTransport() + "://" + "localhost" + ":" + 6700 + "/?" +
                                                              "clientMaxPoolSize=" + maxClientPoolSize);
                  client = new Client(locator);
                  client.connect();
                  String threadName = Thread.currentThread().getName();
                  for (int i = 0; i < numOfCalls; i++)
                  {
                     Object ret = client.invoke(threadName);
                     if(!ret.equals(threadName))
                     {
                        System.out.println("ERROR - Should have returned " + threadName + " but returned " + ret);
                        System.exit(1);
                     }
                     if(i % 500 == 0 && i > 0)
                     {
                        System.out.println("Thread " + threadName + " has made " + i + " invocations.");
                     }
                  }
                  System.out.println(threadName + " -- done.");
                  int threadNum = Integer.parseInt(threadName);
                  finishedThreads[threadNum] = true;
               }
               catch (Throwable e)
               {
                  e.printStackTrace();
               }
               finally
               {
                  client.disconnect();
               }
            }
         }, "" + x);
         currentThread.start();

      }

//      Thread.currentThread().sleep(300000);
      Thread.currentThread().sleep(7200000);

      for (int r = 0; r < finishedThreads.length; r++)
      {
         if (!finishedThreads[r])
         {
            System.out.println("Failed - thread " + r + " never finished.");
         }
      }
   }

   public static void main(String[] args)
   {
      SocketInvokerClientTest client = new SocketInvokerClientTest();
      try
      {
         client.testClientCalls();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
}
