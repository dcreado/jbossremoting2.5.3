package org.jboss.test.remoting.transport.socket.timeout;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.socket.LRUPool;
import org.jboss.remoting.transport.socket.MicroSocketClientInvoker;
import org.jboss.remoting.transport.socket.ServerThread;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import org.jboss.remoting.transport.socket.SocketWrapper;
import org.jboss.test.remoting.timeout.PerInvocationTimeoutTestRoot;


/**
 * See javadoc for PerInvocationTimeoutTestRoot.
 *   
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 2939 $
 * <p>
 * Copyright Feb 6, 2007
 * </p>
 */
public class SocketPerInvocationTimeoutTestCase extends PerInvocationTimeoutTestRoot
{
   
   /**
    * This test verifies that the timeout for a socket wrapper gets reset after
    * an invocation with an invocation specific timeout has been executed.
    */
   public void testTimeoutReset() throws Throwable
   {
      log.info("entering " + getName());
      ClientInvoker invoker = client.getInvoker();
      assertTrue(invoker instanceof MicroSocketClientInvoker);
      Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
      field.setAccessible(true);
      List pool = (List) field.get(invoker);
      assertEquals(0, pool.size());
      
      Object response = client.invoke(NO_WAIT);
      assertEquals(NO_WAIT, response);
      assertEquals(1, pool.size());
      SocketWrapper socket = (SocketWrapper) pool.get(0);
      assertEquals(CONFIGURED_TIMEOUT, socket.getTimeout());
      
      HashMap metadata = new HashMap();
      metadata.put(ServerInvoker.TIMEOUT, "1000");
      response = client.invoke(NO_WAIT, metadata);
      assertEquals(NO_WAIT, response);
      assertEquals(1, pool.size());
      socket = (SocketWrapper) pool.get(0);
      assertEquals(CONFIGURED_TIMEOUT, socket.getTimeout());
   }
   
   
   // This test verifies that a temporary timeout value gets passed when a new
   // socket wrapper is created.  It is important that the socket wrapper gets
   // the temporary timeout value because creating object streams entails i/o
   // on the socket.
   public void testNewSocketTimeout() throws Throwable
   {
      log.info("entering " + getName());
      String host = InetAddress.getLocalHost().getHostAddress();
      int port = PortUtil.findFreePort(host);
      String locatorURI = getTransport() + "://" + host + ":" + port;
      InvokerLocator locator = new InvokerLocator(locatorURI);
      HashMap serverConfig = new HashMap();
      addServerConfig(serverConfig);
      final Connector connector = new Connector(locator, serverConfig);
      connector.create();
      connector.addInvocationHandler("test", new TestHandler());
      connector.start();
      
      // Disable the server so that the client will need to create a new socket, and
      // so that the creation of the socket wrapper will fail.
      new Thread()
      {
         public void run()
         {
            try
            {
               // Give the client a chance to connect to the server, then 
               // disable the server.
               Thread.sleep(2000);
               ServerInvoker si = connector.getServerInvoker();
               assertTrue(si instanceof SocketServerInvoker);
               SocketServerInvoker ssi = (SocketServerInvoker) si;
               Field field = SocketServerInvoker.class.getDeclaredField("clientpool");
               field.setAccessible(true);
               LRUPool clientpool = (LRUPool) field.get(ssi);
               Set threads = clientpool.getContents();
               Iterator it = threads.iterator();
               while (it.hasNext())
               {
                  ServerThread t = (ServerThread) it.next();
                  t.shutdown();
               }

               ssi.setMaxPoolSize(0);
               log.info("server is disabled");
            }
            catch (Exception e)
            {
               log.info(e);
            }
         }
      }.start();

      HashMap clientConfig = new HashMap();
      clientConfig.put(InvokerLocator.FORCE_REMOTE, "true");
      clientConfig.put(ServerInvoker.TIMEOUT, "20000");
      clientConfig.put(Client.ENABLE_LEASE, "true");
      addClientConfig(clientConfig);
      final Client client = new Client(locator, clientConfig);
      client.connect();
      Object response = client.invoke("test 1");
      assertEquals("test 1", response);
      
      class BooleanHolder {public boolean value;}
      final BooleanHolder timedOut = new BooleanHolder();
      timedOut.value = false;
      
      new Thread()
      {
         public void run()
         {
            try
            {
               // Wait for the server to be disabled.
               Thread.sleep(4000);
               
               // This invocation will require the creation of a new socket, which
               // should promptly time out.
               HashMap metadata = new HashMap();
               metadata.put(ServerInvoker.TIMEOUT, "1000");
               client.invoke("test 3", metadata);
               fail("failed to time out");
            }
            catch (Throwable e)
            {
               timedOut.value = true;
               log.info("time out", e);
            }
         }
      }.start();
      
      // It should take the Client a little while for LeasePinger's attempts to contact
      // the server to time out.  Wait for 4 seconds after the call to Client.invoke()
      // and then verify that the Client has timed out according to the temporary timeout
      // value 1000 instead of the configureed value 20000.
      Thread.sleep(8000);
      log.info("testing timeout");
      assertTrue(timedOut.value);
      
      client.disconnect();
      connector.stop();
      log.info(getName() + " PASSES");
   }
   
   
   protected String getTransport()
   {
      return "socket";
   }
   
   protected List getPool(ClientInvoker clientInvoker) throws Exception
   {
      List pool = null;
      if (clientInvoker instanceof MicroSocketClientInvoker)
      {
         Field field = MicroSocketClientInvoker.class.getDeclaredField("pool");
         field.setAccessible(true);
         pool = (List) field.get(clientInvoker);
      }
      return pool;
   }
   
   
   public class TestHandler implements ServerInvocationHandler
   {
      public void setMBeanServer(MBeanServer server) {}
      public void setInvoker(ServerInvoker invoker) {}

      public Object invoke(InvocationRequest invocation) throws Throwable
      {
         return invocation.getParameter();
      }

      public void addListener(InvokerCallbackHandler callbackHandler) {}
      public void removeListener(InvokerCallbackHandler callbackHandler) {}
   }
}
