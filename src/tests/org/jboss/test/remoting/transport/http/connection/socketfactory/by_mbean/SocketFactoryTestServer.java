package org.jboss.test.remoting.transport.http.connection.socketfactory.by_mbean;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.security.ServerSocketFactoryMBean;
import org.jboss.remoting.security.SocketFactoryMBean;
import org.jboss.remoting.transport.Connector;
import org.w3c.dom.Document;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SocketFactoryTestServer extends ServerTestCase
{
   // Default locator values
   private static String transport = "http";
   private static String host = "localhost";
   private static int port = 5400;

   private static int callCounter = 0;

   private Connector connector = null;

   public void setupServerWithMBeanServer() throws Exception
   {
      String serverSocketFactoryValue = "jboss:type=serversocketfactory";

      //setup mbean server socket factory
      MBeanServer mbeanServer = MBeanServerFactory.createMBeanServer();

      ServerSocketFactoryMock service = new SocketFactoryTestServer.ServerSocketFactoryMock();
      ObjectName objName = new ObjectName(serverSocketFactoryValue);
      mbeanServer.registerMBean(service, objName);

      connector = new Connector();

      mbeanServer.registerMBean(connector, new ObjectName("jboss:type=connector"));

      StringBuffer buf = new StringBuffer();
      buf.append("<?xml version=\"1.0\"?>\n");
      buf.append("<config>");
      buf.append("<invoker transport=\"" + transport + "\">");
      buf.append("<attribute name=\"serverSocketFactory\">" + serverSocketFactoryValue + "</attribute>");
      buf.append("<attribute name=\"serverBindAddress\">" + host + "</attribute>");
      buf.append("<attribute name=\"serverBindPort\">" + port + "</attribute>");
      buf.append("</invoker>");
      buf.append("</config>");
      Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(buf.toString().getBytes()));
      //connector.setInvokerLocator(locator.getLocatorURI());
      connector.setConfiguration(xml.getDocumentElement());
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
      setupServerWithMBeanServer();
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
      SocketFactoryTestServer server = new SocketFactoryTestServer();
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
         System.out.println("Invocation request is: " + invocation.getParameter());
         Integer resp = new Integer(++callCounter);
         System.out.println("Returning response of: " + resp);
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

   public static class ServerSocketFactoryMock extends ServerSocketFactory implements ServerSocketFactoryMockMBean
   {
      private ServerSocketFactory factory = null;

      public ServerSocketFactoryMock()
      {
         factory = ServerSocketFactory.getDefault();
      }

      public ServerSocket createServerSocket() throws IOException
      {
         return factory.createServerSocket();
      }

      public ServerSocket createServerSocket(int i) throws IOException
      {
         return factory.createServerSocket(i);
      }

      public ServerSocket createServerSocket(int i, int i1) throws IOException
      {
         return factory.createServerSocket(i, i1);
      }

      public ServerSocket createServerSocket(int i, int i1, InetAddress inetAddress) throws IOException
      {
         return factory.createServerSocket(i, i1, inetAddress);
      }
   }

   public interface ServerSocketFactoryMockMBean extends ServerSocketFactoryMBean
   {

   }

   public static class SocketFactoryMock extends SocketFactory implements SocketFactoryMockMBean, Serializable
   {
      private SocketFactory factory = null;

      public SocketFactoryMock()
      {
      }

      private void init()
      {
         System.out.println("SocketFactoryMock - init() called");
         this.factory = SocketFactory.getDefault();
      }

      public Socket createSocket(String host, int port) throws IOException, UnknownHostException
      {
         if(factory == null)
         {
            init();
         }
         return factory.createSocket(host, port);
      }

      public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException
      {
         if(factory == null)
         {
            init();
         }
         return factory.createSocket(host, port, localHost, localPort);
      }

      public Socket createSocket(InetAddress host, int port) throws IOException
      {
         if(factory == null)
         {
            init();
         }
         return factory.createSocket(host, port);
      }

      public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException
      {
         if(factory == null)
         {
            init();
         }
         return factory.createSocket(address, port, localAddress, localPort);
      }
   }

   public interface SocketFactoryMockMBean extends SocketFactoryMBean
   {

   }

}
