package org.jboss.test.remoting.transport.socket.timeout.idle;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class IdleTimeoutClientTest extends TestCase
{

//   private static int numOfRunnerThreads = 10;
   private static int numOfRunnerThreads = 2;
   private static SynchronizedInt responseCount = new SynchronizedInt(0);

   private Logger logger = Logger.getRootLogger();
   
   protected String getTransport()
   {
      return "socket";
   }

   public static void main(String[] args) throws Throwable
   {
      IdleTimeoutClientTest rt = new IdleTimeoutClientTest();
//      rt.runMultipleClients(Integer.parseInt(args[1]));
      rt.runMultipleClients(numOfRunnerThreads);
   }

   public void testRunClients() throws Throwable
   {
      String locatorURI = getTransport() + "://localhost:54000/?clientMaxPoolSize=50&timeout=10000";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Client client = new Client(locator);
      client.connect();
      Object resp = client.invoke(Boolean.TRUE);
      Boolean isFirst = (Boolean)resp;
      System.out.println("client is first = " + isFirst);

      Thread.currentThread().sleep(10000);

      if(isFirst.booleanValue())
      {
          Thread.currentThread().sleep(10000);
      }

      runMultipleClients(numOfRunnerThreads);
      Thread.currentThread().sleep(30000);
      System.out.println("done with first timeout.");
      runMultipleClients(numOfRunnerThreads);
      Thread.currentThread().sleep(12000);

      System.out.println("response count = " + responseCount.get());
      assertEquals(4, responseCount.get());
   }

   public void runClient(String clientId) throws Throwable
   {
      String locatorURI = "socket://localhost:54000/?clientMaxPoolSize=50&timeout=10000";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Client client = new Client(locator);
      client.connect();
      String req = clientId;
      Object resp = client.invoke(req);
      responseCount.increment();
      System.out.println("Received response of: " + resp + ".  Response count = " + responseCount);
   }

      public void runMultipleClients(int cnt) throws Throwable {
      for (int i = 0; i < cnt; i++) {
         Thread t = new Thread(new Runnable() {
            public void run() {
               try {
                  Thread.sleep(1000);
                  runClient(Thread.currentThread().getName());
               } catch (Throwable e) {
                  logger.error(e);
                  e.printStackTrace();
               }
            }
         }, Integer.toString(i));
         t.start();
      }
   }
}
