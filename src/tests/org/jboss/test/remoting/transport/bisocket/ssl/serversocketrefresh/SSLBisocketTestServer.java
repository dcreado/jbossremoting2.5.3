package org.jboss.test.remoting.transport.bisocket.ssl.serversocketrefresh;

import org.jboss.test.remoting.transport.socket.ssl.serversocketrefresh.TestServer;


/**
 * @author Michael Voss
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 *
 */
public class SSLBisocketTestServer extends TestServer
{
   public static void main(String[] args)
   {
      TestServer server = new SSLBisocketTestServer();
      server.setUp();
      try
      {
         server.test();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }
   
   
   protected String getTransport()
   {
      return "sslbisocket";
   }
}
