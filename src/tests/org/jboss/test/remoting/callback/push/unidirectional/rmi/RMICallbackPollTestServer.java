package org.jboss.test.remoting.callback.push.unidirectional.rmi;

import org.jboss.test.remoting.callback.push.unidirectional.CallbackPollTestServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RMICallbackPollTestServer extends CallbackPollTestServer
{
   public String getTransport()
   {
      return "rmi";
   }

   public static void main(String[] args)
   {
      RMICallbackPollTestServer server = new RMICallbackPollTestServer();
      try
      {
         server.setUp();

         Thread.sleep(600000);
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }


}
