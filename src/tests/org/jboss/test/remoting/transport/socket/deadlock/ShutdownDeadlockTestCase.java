/***************************************
 *                                     *
 *  JBoss: The OpenSource J2EE WebOS   *
 *                                     *
 *  Distributable under LGPL license.  *
 *  See terms of license at gnu.org.   *
 *                                     *
 ***************************************/
package org.jboss.test.remoting.transport.socket.deadlock;

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
 * This test case is for JBREM-576.
 * Test trys to catch deadlock in shutdown where socket client
 * invoker being use my multiple Clients.  Need one client making
 * invocations (which will cause sync on pool) and another to
 * shutdown, which will cause disconnect on socket client invoker,
 * thus causing it to sync on pool for clearing out the pool.
 * Since this is an issue of multithreading, is certainly possible
 * this test will pass even though the deadlock issue still exists.
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 */
public class ShutdownDeadlockTestCase extends TestCase
{
   private String invokerLocatorUrl = getTransport() + "://localhost:" + getPort() + "/?" + InvokerLocator.FORCE_REMOTE + "=true";

   private Client shutdownClient;
   private Client invocationClient;
   private Connector connector;

   public void testDeadlock() throws Exception
   {
      xsetUP();
      xtestDeadlock();
      xtearDown();
   }


   public void xsetUP() throws Exception
   {
      connector = new Connector(invokerLocatorUrl);
      connector.create();
      connector.addInvocationHandler("test", new TestInvocationHandler());
      connector.start();


      shutdownClient = new Client(new InvokerLocator(invokerLocatorUrl));
      shutdownClient.connect();

      invocationClient = new Client(new InvokerLocator(invokerLocatorUrl));
      invocationClient.connect();
   }

   public void xtestDeadlock() throws Exception
   {
      final Client aClient = invocationClient;
      final StringBuffer aBuf = new StringBuffer();
      final Client bClient = shutdownClient;
      final StringBuffer bBuf = new StringBuffer();
      Thread a = new Thread(new Runnable() {
         public void run()
         {
            for(int x = 0; x < 10000; x++)
            {
               try
               {
                  aClient.invoke("foo");
               }
               catch (Throwable throwable)
               {
                  throwable.printStackTrace();
               }
            }
            aBuf.append("done");
            System.out.println("invocation client done");
         }
      });

      Thread b = new Thread(new Runnable() {
         public void run()
         {
            for(int i = 0; i < 100; i++)
            {
               try
               {
                  bClient.invoke("bla");
               }
               catch (Throwable throwable)
               {
                  throwable.printStackTrace();
               }
            }
            bClient.disconnect();
            bBuf.append("done");
            System.out.println("shutdown client done");
         }
      });

      a.start();
      b.start();

      Thread.currentThread().sleep(10000);

      System.out.println("aBuf done = " + ("done".equals(aBuf.toString())));
      System.out.println("bBuf done = " + ("done".equals(bBuf.toString())));

   }

   public void xtearDown()
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      ShutdownDeadlockTestCase test = new ShutdownDeadlockTestCase();
      try
      {
         test.testDeadlock();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   protected String getTransport()
   {
      return "socket";
   }
   
   protected int getPort()
   {
      return 7798;
   }
   
   public class TestInvocationHandler implements ServerInvocationHandler
   {

      public void setMBeanServer(MBeanServer server)
      {
         //To change body of implemented methods use File | Settings | File Templates.
      }

      public void setInvoker(ServerInvoker invoker)
      {
         //To change body of implemented methods use File | Settings | File Templates.
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return "bar";
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         //To change body of implemented methods use File | Settings | File Templates.
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         //To change body of implemented methods use File | Settings | File Templates.
      }
   }
}