package org.jboss.test.remoting.callback.push.bidirectional.multiplex;

import junit.framework.TestCase;

import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.HandleCallbackException;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.multiplex.MultiplexClientInvoker;
import org.jboss.remoting.transport.multiplex.MultiplexServerInvoker;
import org.jboss.remoting.transport.multiplex.MultiplexingManager;
import org.jboss.remoting.transport.multiplex.VirtualServerSocket;
import org.jboss.remoting.transport.multiplex.VirtualSocket;
import org.jboss.remoting.transport.multiplex.MultiplexServerInvoker.SocketGroupInfo;
import org.jboss.remoting.transport.socket.SocketServerInvoker;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class MultiplexCallbackTestClient extends TestCase
{
   private String locatorUri = "multiplex://localhost:8999";
   private boolean gotCallback = false;

   public void testCallback() throws Throwable
   {
      Client client = new Client(new InvokerLocator(locatorUri));
      client.connect();
      InvokerCallbackHandler testCallbackHandler = new MultiplexCallbackTestClient.TestCallbackHandler();
      client.addListener(testCallbackHandler, new HashMap(), "foobar");
      client.invoke("foobar");

      Thread.sleep(5000);

      client.removeListener(testCallbackHandler);
      client.disconnect();

      assertTrue(gotCallback);
   }
   

   
   /**
    * This test verifies that when Client creates an anonymous callback Connector,
    * the callback connection from server to client uses the same underlying
    * socket as the client to server connection.
    * 
    * @throws Throwable
    */
   public void testSharedCallbackConnection() throws Throwable
   {
      // Wait for any existing MultiplexingManagers to close.
      Thread.sleep(5000);
      
      // There should be 0 MultiplexingManagers before Client is connected.
      Field field = MultiplexingManager.class.getDeclaredField("allManagers");
      field.setAccessible(true);
      Set allManagers = (Set) field.get(null);
      assertNotNull(allManagers);
      assertEquals(0, allManagers.size());
      
      // Create and connect Client.
      Client client = new Client(new InvokerLocator(locatorUri));
      client.connect();
 
      // There should be 1 MultiplexingManager now that Client is connected.
      assertEquals(1, allManagers.size());
      
      // Add a push CallbackHandler.
      InvokerCallbackHandler testCallbackHandler = new TestCallbackHandler();
      client.addListener(testCallbackHandler, new HashMap(), "foobar");
      
      // There should still be 1 MultiplexingManager, since client invoker and
      // server invoker should be using the same connection.
      assertEquals(1, allManagers.size());
      
      // Show connection works.
      client.invoke("foobar");
      Thread.sleep(5000);
      assertTrue(gotCallback);
      
      // Get client invoker's MultiplexingManager.
      MultiplexClientInvoker clientInvoker = (MultiplexClientInvoker) client.getInvoker();
      field = MultiplexClientInvoker.class.getDeclaredField("socketGroupInfo");
      field.setAccessible(true);
      SocketGroupInfo sgi = (SocketGroupInfo) field.get(clientInvoker);
      VirtualSocket socket = sgi.getPrimingSocket();
      field = VirtualSocket.class.getDeclaredField("manager");
      field.setAccessible(true);
      MultiplexingManager clientManager = (MultiplexingManager) field.get(socket);
      assertNotNull(clientManager);
      
      // Get server invoker's MultiplexingManager.
      field = Client.class.getDeclaredField("callbackConnectors");
      field.setAccessible(true);
      Map callbackConnectors = (Map) field.get(client);
      assertEquals(1, callbackConnectors.size());
      Set callbackConnectorSet = (Set) callbackConnectors.values().iterator().next();
      assertEquals(1, callbackConnectorSet.size());
      Connector connector = (Connector) callbackConnectorSet.iterator().next();
      MultiplexServerInvoker serverInvoker = (MultiplexServerInvoker) connector.getServerInvoker();
      field = SocketServerInvoker.class.getDeclaredField("serverSocket");
      field.setAccessible(true);
      VirtualServerSocket serverSocket = (VirtualServerSocket) field.get(serverInvoker);
      field = VirtualServerSocket.class.getDeclaredField("manager");
      field.setAccessible(true);
      MultiplexingManager serverManager = (MultiplexingManager) field.get(serverSocket);
      
      // Show client and server invokers are using the same MultiplexingManager.
      assertEquals(clientManager, serverManager);
      
      client.removeListener(testCallbackHandler);
      client.disconnect();
      
      Thread.sleep(5000);
      
      // The connection should be closed and there should be 0 MultiplexingManagers.
      assertEquals(0, allManagers.size());
   }

   public class TestCallbackHandler implements InvokerCallbackHandler
   {

      public void handleCallback(Callback callback) throws HandleCallbackException
      {
         System.out.println("callback = " + callback);
         Object handle = callback.getCallbackHandleObject();
         if ("foobar".equals(handle))
         {
            gotCallback = true;
         }
      }
   }
}
