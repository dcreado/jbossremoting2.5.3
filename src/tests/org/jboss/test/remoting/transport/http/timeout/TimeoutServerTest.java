package org.jboss.test.remoting.transport.http.timeout;

import javax.management.MBeanServer;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.callback.InvokerCallbackHandler;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TimeoutServerTest extends ServerTestCase
{
   private String locatorURI = "http://localhost:8899/?timeout=3000";
   private Connector connector = null;

   public void setUp() throws Exception
   {
      connector = new Connector();
      InvokerLocator locator = new InvokerLocator(locatorURI);
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();
      connector.addInvocationHandler("test", new org.jboss.test.remoting.transport.http.timeout.TimeoutServerTest.TimeoutHandler());
      connector.start();
   }

   protected void tearDown() throws Exception
   {
      if(connector != null)
      {
         connector.stop();
         connector.destroy();
      }
   }

   public static void main(String[] args)
   {
      org.jboss.test.remoting.transport.http.timeout.TimeoutServerTest server = new org.jboss.test.remoting.transport.http.timeout.TimeoutServerTest();
      try
      {
         server.setUp();

         Thread.currentThread().sleep(30000);
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

   private class TimeoutHandler implements ServerInvocationHandler
   {

      /**
       * set the mbean server that the handler can reference
       *
       * @param server
       */
      public void setMBeanServer(MBeanServer server)
      {
         //TODO: -TME Implement
      }

      /**
       * set the invoker that owns this handler
       *
       * @param invoker
       */
      public void setInvoker(ServerInvoker invoker)
      {
         //TODO: -TME Implement
      }

      /**
       * called to handle a specific invocation.  Please take care to make sure
       * implementations are thread safe and can, and often will, receive concurrent
       * calls on this method.
       *
       * @param invocation
       * @return
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         Object obj = invocation.getParameter();
         if(obj instanceof String && "timeout".equals(obj))
         {
            System.out.println("server got 'timeout' invocation... sleeping 30 seconds.");
            Thread.currentThread().sleep(30000);
         }
         return null;  //TODO: -TME Implement
      }

      /**
       * Adds a callback handler that will listen for callbacks from
       * the server invoker handler.
       *
       * @param callbackHandler
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         //TODO: -TME Implement
      }

      /**
       * Removes the callback handler that was listening for callbacks
       * from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         //TODO: -TME Implement
      }
   }
}
