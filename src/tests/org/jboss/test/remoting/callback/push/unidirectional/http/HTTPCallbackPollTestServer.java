package org.jboss.test.remoting.callback.push.unidirectional.http;

import org.jboss.test.remoting.callback.push.unidirectional.CallbackPollTestServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPCallbackPollTestServer extends CallbackPollTestServer
{
   public String getTransport()
   {
      return "http";
   }

   public static void main(String[] args)
   {
      HTTPCallbackPollTestServer server = new HTTPCallbackPollTestServer();
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
