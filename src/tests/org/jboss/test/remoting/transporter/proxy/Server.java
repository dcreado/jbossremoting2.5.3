package org.jboss.test.remoting.transporter.proxy;

import org.apache.log4j.Level;
import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.transporter.TransporterServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Server extends ServerTestCase
{
   private String locatorURI = "socket://localhost:5400";
   private TransporterServer server = null;

   public void setUp() throws Exception
   {
      server = TransporterServer.createTransporterServer(locatorURI, new CustomerProcessorImpl());
   }

   public void tearDown()
   {
      if (server != null)
      {
         server.stop();
      }
   }

   public static void main(String[] args)
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(Level.DEBUG);
      org.apache.log4j.Category.getInstance("test").setLevel(Level.DEBUG);

      Server server = new Server();
      try
      {
         server.setUp();

         Thread.currentThread().sleep(60000);

      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      finally
      {
         server.tearDown();
      }
   }
}
