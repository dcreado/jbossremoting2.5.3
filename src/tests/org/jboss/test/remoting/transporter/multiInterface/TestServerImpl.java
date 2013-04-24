package org.jboss.test.remoting.transporter.multiInterface;

import org.jboss.jrunit.extensions.ServerTestCase;
import org.jboss.remoting.transporter.TransporterServer;

import java.io.IOException;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TestServerImpl extends ServerTestCase implements TestServer, TestServer2
{
   private String locatorURI = "socket://localhost:5400";
   private TransporterServer server = null;

   public String processTestMessage(String msg)
   {
      return msg + " - has been processed";
   }

   public void throwException() throws IOException
   {
      throw new IOException("This is an expected exception thrown by impl on purpose.");
   }

   public void setUp() throws Exception
   {
      server = TransporterServer.createTransporterServer(locatorURI, new TestServerImpl());
   }

   public void tearDown()
   {
      if(server != null)
      {
         server.stop();
      }
   }

   /**
    * Just here so can run from a few line within a main
    *
    * @param args
    */
   public static void main(String[] args)
   {
      String locatorURI = "socket://localhost:5400";

      try
      {
         TestServerImpl testServer = new TestServerImpl();
         testServer.setUp();

         Thread.currentThread().sleep(60000);

         testServer.tearDown();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }

   }

   public String doIt(String msg)
   {
      return msg;
   }
}
