package org.jboss.test.remoting.transport.socket.load;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.Connector;

public class PooledConnectionTestCase extends TestCase
{
   private static int numOfRunnerThreads = 10;
//   private static int numOfRunnerThreads = 3;
   private static SynchronizedInt responseCount = new SynchronizedInt(0);
   private Connector connector;

//   static
//   {
//      BasicConfigurator.configure();
//      Logger.getRootLogger().setLevel(Level.INFO);
//      Logger.getInstance("org.jboss.remoting.transport.socket").setLevel(Level.ALL);
//   }

   private Logger logger = Logger.getRootLogger();

   protected String getTransport()
   {
      return "socket";
   }
   
   public static void main(String[] args) throws Throwable
   {
      PooledConnectionTestCase rt = new PooledConnectionTestCase();
      rt.startServer();
//      rt.runMultipleClients(Integer.parseInt(args[1]));
      rt.runMultipleClients(PooledConnectionTestCase.numOfRunnerThreads);
   }

   public void setUp() throws Exception
   {
      startServer();
   }

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public void startServer() throws Exception
   {
      String locatorURI = getTransport() + "://localhost:54000/?maxPoolSize=10&timeout=10000";
      InvokerLocator locator = new InvokerLocator(locatorURI);

      connector = new Connector();

      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();

      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
   }

   public void testRunClients() throws Throwable
   {
      runMultipleClients(PooledConnectionTestCase.numOfRunnerThreads);
      // waiting 8 seconds as the server handler will have waited client calls
      // 5 seconds, but should only be able to make client invocations 2 at a time.
      Thread.currentThread().sleep(8000);
      // should not have all invocations as should be blocking within client invoker (as only
      // allows 2 concurrent client connections).
      assertFalse(10 == PooledConnectionTestCase.responseCount.get());

      Thread.currentThread().sleep(120000);
      System.out.println("Response count = " + PooledConnectionTestCase.responseCount + ".  Expected 10.");
      assertEquals(10, PooledConnectionTestCase.responseCount.get());
   }

   public void runClient(String clientId) throws Throwable
   {
      String locatorURI = getTransport() + "://localhost:54000/?clientMaxPoolSize=2";
      InvokerLocator locator = new InvokerLocator(locatorURI);
      Client client = new Client(locator);
      client.connect();
      String req = clientId;
      Object resp = client.invoke(req);
      PooledConnectionTestCase.responseCount.increment();
      System.out.println("Received response of: " + resp + ".  Response count = " + PooledConnectionTestCase.responseCount);
      System.in.read();
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
