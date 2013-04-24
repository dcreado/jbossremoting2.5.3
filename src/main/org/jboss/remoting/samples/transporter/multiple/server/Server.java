package org.jboss.remoting.samples.transporter.multiple.server;

import org.jboss.remoting.samples.transporter.multiple.CustomerProcessorImpl;
import org.jboss.remoting.samples.transporter.multiple.AccountProcessorImpl;
import org.jboss.remoting.samples.transporter.multiple.CustomerProcessor;
import org.jboss.remoting.samples.transporter.multiple.AccountProcessor;
import org.jboss.remoting.transporter.TransporterServer;


/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class Server
{
   private String locatorURI = "socket://localhost:5400";
   private TransporterServer server = null;

   public void start() throws Exception
   {
      server = TransporterServer.createTransporterServer(locatorURI, new CustomerProcessorImpl(), CustomerProcessor.class.getName());
      server.addHandler(new AccountProcessorImpl(), AccountProcessor.class.getName());
   }

   public void stop()
   {
      if(server != null)
      {
         server.stop();
      }
   }

   public static void main(String[] args)
   {
      Server server = new Server();
      try
      {
         server.start();

         Thread.currentThread().sleep(60000);

      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
      finally
      {
         server.stop();
      }
   }
}
