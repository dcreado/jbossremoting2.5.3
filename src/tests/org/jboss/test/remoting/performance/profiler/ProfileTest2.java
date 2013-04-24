package org.jboss.test.remoting.performance.profiler;

import org.jboss.test.remoting.performance.synchronous.PerformanceClientTest;
import org.jboss.test.remoting.performance.synchronous.PerformanceServerTest;
import org.jboss.test.remoting.performance.synchronous.PerformanceTestCase;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ProfileTest2
{
   private static int numOfClients = 10;
   private static int numOfLoops = 100;

   private String numOfCalls = "1000";
//   private String numOfCalls = "1000000";

   private PerformanceServerTest server = null;


   public void profilerTest() throws Throwable
   {
      setupServer();

      System.setProperty(PerformanceTestCase.NUMBER_OF_CALLS, numOfCalls);
      System.setProperty(PerformanceTestCase.REMOTING_METADATA, "foo=bar");

      for(int x = 0; x < numOfClients; x++)
      {
         new Thread(new Runnable()
         {
            public void run()
            {
               try
               {
                  for(int i = 0; i < numOfLoops;i++)
                  {
                     runClientTest();
                  }
               }
               catch(Throwable throwable)
               {
                  throwable.printStackTrace();
               }
            }
         }).start();
      }

      while(numOfClients > 0)
      {
         Thread.currentThread().sleep(5000);
      }

      server.tearDown();

   }


   public void setupServer() throws Exception
   {
      server = new PerformanceServerTest();
      server.setUp();
      System.out.println("Server setup");
   }


   public void runClientTest() throws Throwable
   {

      PerformanceClientTest client = new PerformanceClientTest();
      client.setUp();
      System.out.println("Client setup");

      client.testClientCalls();
      System.out.println("Done with testing client calls");


      client.tearDown();

      numOfClients--;

      System.out.println("Number of clients = " + numOfClients);
   }

   public static void main(String[] args)
   {
      ProfileTest2 test = new ProfileTest2();
      try
      {
         test.profilerTest();
      }
      catch(Throwable throwable)
      {
         throwable.printStackTrace();
      }
   }
}
