package org.jboss.test.remoting.callback.push.unidirectional.http;

import org.jboss.test.remoting.callback.push.unidirectional.CallbackPushTestServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPCallbackPushTestServer extends CallbackPushTestServer
{
   public String getTransport()
   {
      return "http";
   }

   public static void main(String[] args)
   {
      HTTPCallbackPushTestServer server = new HTTPCallbackPushTestServer();
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
