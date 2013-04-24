/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.test.remoting.transport.socket.serverlockup;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.test.remoting.TestUtil;
import org.jboss.logging.Logger;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.apache.log4j.Level;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Category;

/**
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 */
public class ServerLockupServerTest extends ServerTestCase
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(ServerLockupServerTest.class);

   // Static ---------------------------------------------------------------------------------------

   public static void main(String[] args)
   {
      BasicConfigurator.configure();
      Category.getRoot().setLevel(Level.INFO);
      Category.getInstance("org.jboss.remoting.transport.socket").setLevel(Level.DEBUG);
      Category.getInstance("org.jboss.test.remoting").setLevel(Level.DEBUG);
      Category.getInstance("org.jgroups").setLevel(Level.FATAL);

      ServerLockupServerTest server = new ServerLockupServerTest();

      try
      {
         server.setUp();
         Thread.sleep(300000);
         server.tearDown();
         System.out.println("Have torn down test.");
         Thread.sleep(30000);
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }

   // Attributes -----------------------------------------------------------------------------------

   private int serverPort = 9091;  // default port

   private Connector connector;

   // Constructors ---------------------------------------------------------------------------------

   // Public ---------------------------------------------------------------------------------------

   public String getTransport()
   {
      return "socket";
   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected String getSubsystem()
   {
      return "mock";
   }

   protected ServerInvocationHandler getServerInvocationHandler()
   {
      return new SimpleServerInvocationHandler();
   }

   protected void setUp() throws Exception
   {
      if(serverPort < 0)
      {
         serverPort = TestUtil.getRandomPort();
      }

      log.debug("port = " + serverPort);

      connector = new Connector();
      
      String locatorString =
         getTransport() + "://localhost:" + serverPort + "/?serializationType=jboss";

      InvokerLocator locator = new InvokerLocator(locatorString);
      connector.setInvokerLocator(locator.getLocatorURI());
      connector.create();
      connector.addInvocationHandler(getSubsystem(), getServerInvocationHandler());
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

   // Private --------------------------------------------------------------------------------------

   // Inner classes --------------------------------------------------------------------------------

}







