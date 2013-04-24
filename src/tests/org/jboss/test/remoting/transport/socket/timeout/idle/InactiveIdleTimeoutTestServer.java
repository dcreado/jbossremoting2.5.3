package org.jboss.test.remoting.transport.socket.timeout.idle;

import org.apache.log4j.Logger;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.socket.SocketServerInvoker;

/**
 * This is just like IdleTimeoutTestServer except instead of
 * looking for server threads that are still in read mode waiting for
 * data from client, want to test for idle server threads that finished with
 * client/server connection and are just sitting in the thread pool waiting to
 * be re-used.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class InactiveIdleTimeoutTestServer extends ServerTestCase
{
   private Connector connector;

   private Logger logger = Logger.getRootLogger();

   protected String getTransport()
   {
      return "socket";
   }
   
   public static void main(String[] args) throws Throwable
   {
      InactiveIdleTimeoutTestServer rt = new InactiveIdleTimeoutTestServer();
      rt.setUp();
      Thread.currentThread().sleep(45000);
      rt.tearDown();

   }

   public void setUp() throws Exception
   {
      startServer();
   }

   public void tearDown() throws Exception
   {
      if(connector != null)
      {
         try
         {
            // check for empty thread pool
            SocketServerInvoker svrInvoker = (SocketServerInvoker)connector.getServerInvoker();
            int threadPoolSize = svrInvoker.getCurrentThreadPoolSize();
            if(threadPoolSize > 0)
            {
               System.out.println("Error - thread pool should be size of 0, but is " + threadPoolSize);
               throw new RuntimeException("Thread pool was not size of 0, but " + threadPoolSize);
            }
            else
            {
               System.out.println("Pass - thread pool size is 0");
            }

         }
         finally
         {
            connector.stop();
            connector.destroy();
         }
      }
   }

   public void startServer() throws Exception
   {
//      String locatorURI = "socket://localhost:54000/?maxPoolSize=2&timeout=60000&backlog=0";
      String locatorURI = getTransport() + "://localhost:54000/?maxPoolSize=2&backlog=0&timeout=10000&idleTimeout=15";
      InvokerLocator locator = new InvokerLocator(locatorURI);

      connector = new Connector();

      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();

      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      connector.addInvocationHandler("sample", invocationHandler);
      connector.start();
   }

}
