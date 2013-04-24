package org.jboss.test.remoting.transport.socket.connection;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;

import javax.management.MBeanServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketConnectionCheckTestServer extends ServerTestCase
{
   // Default locator values
   private static String transport = "socket";
   private static String host = "localhost";
   private static int port = 5400;

   private static int callCounter = 0;

   private Connector connector = null;

   public void setupServer(String locatorURI) throws Exception
   {
      // create the InvokerLocator based on url string format
      // to indicate the transport, host, and port to use for the
      // server invoker.
      InvokerLocator locator = new InvokerLocator(locatorURI);
      System.out.println("Starting remoting server with locator uri of: " + locatorURI);
      connector = new Connector(locator);
      // creates all the connector's needed resources, such as the server invoker
      connector.create();

      // create the handler to receive the invocation request from the client for processing
      SampleInvocationHandler invocationHandler = new SampleInvocationHandler();
      // first parameter is sub-system name.  can be any String value.
      connector.addInvocationHandler("sample", invocationHandler);

      // start with a new non daemon thread so
      // server will wait for request and not exit
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

   public void setUp() throws Exception
   {
      String locatorURI = getTransport() + "://" + host + ":" + getPort() + "/?" + SocketServerInvoker.CHECK_CONNECTION_KEY + "=" + Boolean.TRUE;
      setupServer(locatorURI);
   }

   /**
    * Can pass transport and port to be used as parameters.
    * Valid transports are 'rmi' and 'socket'.
    *
    * @param args
    */
   public static void main(String[] args)
   {
      if(args != null && args.length == 3)
      {
         transport = args[0];
         host = args[1];
         port = Integer.parseInt(args[2]);
      }
      SocketConnectionCheckTestServer server = new SocketConnectionCheckTestServer();
      try
      {
         server.setUp();

         // wait forever, let the user kill us at any point (at which point, the client will detect we went down)
         while(true)
         {
            Thread.sleep(1000);
         }

      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

   protected String getTransport()
   {
      return transport;
   }
   
   protected int getPort()
   {
      return port;
   }
   
   /**
    * Simple invocation handler implementation.
    * This is the code that will be called with the invocation payload from the client.
    */
   public static class SampleInvocationHandler implements ServerInvocationHandler
   {
      /**
       * called to handle a specific invocation
       *
       * @param invocation
       * @return
       * @throws Throwable
       */
      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         // Print out the invocation request
//         System.out.println("Invocation request is: " + invocation.getParameter());
         Integer resp = new Integer(++SocketConnectionCheckTestServer.callCounter);
//         System.out.println("Returning response of: " + resp);
         // Just going to return static string as this is just simple example code.
         return resp;
      }

      /**
       * Adds a callback handler that will listen for callbacks from
       * the server invoker handler.
       *
       * @param callbackHandler
       */
      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as do not handling callback listeners in this example
      }

      /**
       * Removes the callback handler that was listening for callbacks
       * from the server invoker handler.
       *
       * @param callbackHandler
       */
      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         // NO OP as do not handling callback listeners in this example
      }

      /**
       * set the mbean server that the handler can reference
       *
       * @param server
       */
      public void setMBeanServer(MBeanServer server)
      {
         // NO OP as do not need reference to MBeanServer for this handler
      }

      /**
       * set the invoker that owns this handler
       *
       * @param invoker
       */
      public void setInvoker(ServerInvoker invoker)
      {
         // NO OP as do not need reference back to the server invoker
      }

   }

}
