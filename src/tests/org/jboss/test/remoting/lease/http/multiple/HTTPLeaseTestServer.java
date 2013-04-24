package org.jboss.test.remoting.lease.http.multiple;

import org.apache.log4j.Level;
import org.jboss.logging.XLevel;
import org.jboss.test.remoting.lease.LeaseTestServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPLeaseTestServer extends LeaseTestServer
{
   private static String transport = "http";

   protected String getTransport()
   {
      return transport;
   }

   public static void main(String[] args)
   {
      org.apache.log4j.BasicConfigurator.configure();
      org.apache.log4j.Category.getRoot().setLevel(Level.INFO);
      org.apache.log4j.Category.getInstance("org.jboss.remoting").setLevel(XLevel.TRACE);
      org.apache.log4j.Category.getInstance("org.jgroups").setLevel(Level.FATAL);

      final LeaseTestServer server = new HTTPLeaseTestServer();
      try
      {
         server.setupServer();
         try
         {
            new Thread(new Runnable() {
               public void run()
               {
                  server.testForError();
               }
            }).start();
            Thread.currentThread().sleep(300000);
            server.isRunning = false;
         }
         catch(InterruptedException e)
         {
            e.printStackTrace();
         }
         server.tearDown();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
}
