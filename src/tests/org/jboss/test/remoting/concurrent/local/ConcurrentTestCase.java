package org.jboss.test.remoting.concurrent.local;

import junit.framework.TestCase;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;

import javax.management.MBeanServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class ConcurrentTestCase extends TestCase
{
   private String locatorUri = "socket://localhost:8777";
   private boolean failure = false;
   private Client client = null;
   private Connector connector = null;
   private int numOfThreads = 100;
   private int numOfIterations = 100;
   private int[][] results = null;

   public void setUp() throws Exception
   {
      connector = new Connector(locatorUri);
      connector.create();
      connector.addInvocationHandler("test", new TestInvocationHandler());
      connector.start();

      client = new Client(new InvokerLocator(locatorUri));
      client.connect();
      results = new int[numOfThreads][numOfIterations];
   }


   public void testConcurrentInvocations() throws Exception
   {

      for(int x = 0; x < numOfThreads; x++)
      {
         final int num = x;
         new Thread(new Runnable() {
            public void run()
            {
               try
               {
                  runInvocations(num);
               }
               catch (Throwable throwable)
               {
                  throwable.printStackTrace();
               }
            }
         }, "" + x).start();
      }

      Thread.sleep(20000);

      assertFalse(failure);

      assertTrue(validateResults());

   }

   private boolean validateResults()
   {
      boolean failed = true;

      for(int z = 0; z < numOfThreads; z++)
      {
         for(int q = 1; q < numOfIterations; q++)
         {
            int a = results[z][q -1];
            int b = results[z][q];
            //System.out.println("a = " + a + ", b = " + b + ((b -1 != a) ? " - FAILED" : ""));
            if(b - 1 != a)
            {
               failed = false;
            }
         }
      }
      return failed;
   }

   private void runInvocations(int num) throws Throwable
   {
      for(int i = 0; i < numOfIterations; i++)
      {
         String param = num + "-" + i;
         Object result = client.invoke(param);
         //System.out.println(Thread.currentThread() + " - " + result);
         assertEquals(param, result);
         String subResult = ((String)result).substring(String.valueOf(num).length() + 1);
         //System.out.println(Thread.currentThread() + " - " + subResult);
         results[num][i] = Integer.parseInt(subResult);
      }
   }

   public void tearDown()
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }

      if(client != null)
      {
         client.disconnect();
      }
   }

   public class TestInvocationHandler implements ServerInvocationHandler
   {

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
         return invocation.getParameter();
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         //TODO: -TME Implement
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         //TODO: -TME Implement
      }
   }


}
