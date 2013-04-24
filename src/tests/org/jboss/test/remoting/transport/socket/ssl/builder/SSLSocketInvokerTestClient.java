package org.jboss.test.remoting.transport.socket.ssl.builder;

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.transport.socket.ssl.SSLInvokerConstants;

import javax.management.MBeanServer;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the client for the test to make regular ssl based invocation to the server and
 * have a ssl based callback server.  The special test in this case is want to have the callback
 * client that lives on the server to be in client mode so that this client test only has to have
 * a truststore locally, yet still use ssl.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class SSLSocketInvokerTestClient extends TestCase implements SSLInvokerConstants
{
   private Client client;
   private Connector callbackConnector;
   private static final Logger log = Logger.getLogger(SSLSocketInvokerTestClient.class);
   private boolean gotCallback = false;

   public void init() throws Exception
   {
      // since doing basic (using default ssl server socket factory)
      // need to set the system properties to the truststore
      String trustStoreFilePath = this.getClass().getResource("../.truststore").getFile();
      System.setProperty("javax.net.ssl.trustStore", trustStoreFilePath);
      System.setProperty("javax.net.ssl.trustStorePassword", "unit-tests-client");
//         System.setProperty("javax.net.ssl.trustStorePassword", "secureexample");

      InvokerLocator locator = new InvokerLocator(getTransport() + "://" + host + ":" + getPort());

      // create callback connector
      Map config = new HashMap();
      config.put(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE, "true");
      callbackConnector = new Connector(getTransport() + "://" + host + ":" + (getPort() + 5), config);
      callbackConnector.create();
      callbackConnector.addInvocationHandler("callback", new CallbackInvokerHandler());
      callbackConnector.start();

      client = new Client(locator, "mock");
      client.connect();

      try
      {
         client.addListener(new TestCallbackHandler(), callbackConnector.getLocator());
      }
      catch (Throwable throwable)
      {
         Exception e = new Exception(throwable.getMessage());
         e.initCause(throwable);
         e.printStackTrace();
         throw e;
      }
   }

   public void testRemoteCall() throws Throwable
   {
      log.debug("running testRemoteCall()");

      // simple invoke, should return bar
      Object ret = makeInvocation("foo", "bar");
      assertTrue("Result of testRemoteCall() invocation of foo.", "bar".equals(ret));
      assertEquals("bar", ret);

      // this will cause the server to start sending callbacks
      ret = makeInvocation("test", "foobar");

      System.out.println("ret = " + ret);

      Thread.sleep(5000);

      //DEBUG
//      Thread.sleep(600000);

      assertTrue(gotCallback);


   }

   protected String getTransport()
   {
      return transport;
   }
   
   protected int getPort()
   {
      return port;
   }

   private Object makeInvocation(String method, String param) throws Throwable
   {
      Object ret = client.invoke(new NameBasedInvocation(method,
                                                         new Object[]{param},
                                                         new String[]{String.class.getName()}),
                                 null);

      return ret;
   }

   public void setUp() throws Exception
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("test").setLevel(Level.DEBUG);

      init();
   }

   public void tearDown() throws Exception
   {
      if (client != null)
      {
         client.disconnect();
         client = null;
      }
   }

   public class TestCallbackHandler implements InvokerCallbackHandler
   {

      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         System.out.println("TestCallbackHandler.handleCallback() - " + callback);
         gotCallback = true;
      }
   }

   public class CallbackInvokerHandler implements ServerInvocationHandler
   {
      private InvokerCallbackHandler listener;

      public void setMBeanServer(MBeanServer server)
      {
      }

      public void setInvoker(ServerInvoker invoker)
      {
      }

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         System.out.println("CallbackInvokerHandler.invoke() called with " + invocation.getParameter());
         return null;
      }

      public void addListener(InvokerCallbackHandler callbackHandler)
      {
         listener = callbackHandler;
      }

      public void removeListener(InvokerCallbackHandler callbackHandler)
      {
         listener = null;
      }
   }
}
