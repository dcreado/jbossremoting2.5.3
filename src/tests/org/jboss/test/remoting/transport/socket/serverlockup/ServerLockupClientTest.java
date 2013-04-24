/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.remoting.transport.socket.serverlockup;

import junit.framework.TestCase;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.Client;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.transport.Connector;
import org.jboss.test.remoting.TestUtil;
import org.jboss.test.remoting.transport.mock.MockServerInvocationHandler;
import org.jboss.logging.Logger;

import java.net.BindException;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 */
public class ServerLockupClientTest extends TestCase
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(ServerLockupClientTest.class);

   // Static ---------------------------------------------------------------------------------------

   public static void main(String[] args)
   {
      ServerLockupClientTest client = new ServerLockupClientTest();

      try
      {
         client.setUp();
         client.testSimplePing();
         client.testRogueClient();
         client.tearDown();
      }
      catch (Throwable throwable)
      {
         throwable.printStackTrace();
      }
   }

   // Attributes -----------------------------------------------------------------------------------

   private int port = 9091;
   private int callbackPort = -1;

   private Client client;

   // Constructors ---------------------------------------------------------------------------------

   // Public ---------------------------------------------------------------------------------------

   public void testSimplePing() throws Throwable
   {
      log.debug("test simple ping");
      Object ret = client.invoke(new NameBasedInvocation("ping",
                                                         new Object[] {"hello"},
                                                         new String[] {"java.lang.String"}));
      assertEquals("pong.hello", ret);
   }

   /**
    * Invoking the server with a specially crafted client that is trying to lock up the server.
    */
   public void testRogueClient() throws Throwable
   {
      log.debug("testRogueClient()");

      RogueClientInvoker rogueInvoker =
         new RogueClientInvoker(new InvokerLocator(getTransport() + "://localhost:" + port));

      rogueInvoker.connect();
      rogueInvoker.openConnectionButDontSendAnything();

   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected String getTransport()
   {
      return "socket";
   }

   protected String getSubsystem()
   {
      return "mock";
   }

   protected ServerInvocationHandler getServerInvocationHandler()
   {
      return new MockServerInvocationHandler();
   }

   protected void setUp() throws Exception
   {
      // This is a retry hack because in some cases, can get duplicate callback server ports when
      // trying to find a free one.

      int retryLimit = 3;
      for(int x = 0; x < retryLimit; x++)
      {
         try
         {
            initServer(callbackPort);
         }
         catch(BindException e)
         {
            if(x + 1 == retryLimit)
            {
               throw e;
            }
            else
            {
               continue;
            }
         }
         break;
      }

      initClient();
   }

   protected void tearDown() throws Exception
   {
   }

   // Private --------------------------------------------------------------------------------------

   private InvokerLocator initServer(int port) throws Exception
   {
      if(port < 0)
      {
         port = TestUtil.getRandomPort();
      }

      log.debug("server port " + port);

      Connector connector = new Connector();

      String locatorString =
         getTransport() + "://localhost:" + port + "/?serializationType=jboss";

      InvokerLocator locator = new InvokerLocator(locatorString);

      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();
      connector.addInvocationHandler(getSubsystem(), getServerInvocationHandler());
      connector.start();

      return locator;
   }

   private void initClient()
   {
      try
      {
         String locatorURI = getTransport() + "://localhost:" + port + "/?serializationType=jboss";
         InvokerLocator locator = new InvokerLocator(locatorURI);

         client = new Client(locator, "mock");
         client.connect();
      }
      catch(Exception e)
      {
         log.error(e.getMessage(), e);
      }
   }

   // Inner classes --------------------------------------------------------------------------------

}
