package org.jboss.test.remoting.callback.push.unidirectional.rmi;

import org.jboss.test.remoting.callback.push.unidirectional.CallbackPushTestServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class RMICallbackPushTestServer extends CallbackPushTestServer
{
   public String getTransport()
   {
      return "rmi";
   }

   public static void main(String[] args)
   {
      RMICallbackPushTestServer server = new RMICallbackPushTestServer();
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
