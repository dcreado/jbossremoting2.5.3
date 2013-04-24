package org.jboss.test.remoting.transport.socket.stress;

import org.apache.log4j.Level;
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
public class SocketInvokerServerTest
{
   private Connector connector = null;

   private int maxPoolSize = 300;
//   private int maxPoolSize = 1000;

   public String getTransport()
   {
      return "socket";
   }

   public void setUp() throws Exception
   {
      InvokerLocator locator = new InvokerLocator(getTransport() + "://" + "localhost" + ":" + 6700 + "/?" +
                                                  "MaxPoolSize=" + maxPoolSize);
      connector = new Connector(locator);
      connector.create();

      connector.addInvocationHandler("test", new TestInvocationHandler());

      connector.start();
   }

   public void tearDown()
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);

      SocketInvokerServerTest server = new SocketInvokerServerTest();
      try
      {
         server.setUp();
//         Thread.currentThread().sleep(300000);
         Thread.currentThread().sleep(7200000);
         server.tearDown();
         System.out.println("Have torn down test.");
      }
      catch(Exception e)
      {
         e.printStackTrace();
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
