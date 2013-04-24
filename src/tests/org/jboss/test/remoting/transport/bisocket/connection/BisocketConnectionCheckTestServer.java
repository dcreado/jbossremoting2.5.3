package org.jboss.test.remoting.transport.bisocket.connection;

import org.jboss.test.remoting.transport.socket.connection.SocketConnectionCheckTestServer;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 */
public class BisocketConnectionCheckTestServer extends SocketConnectionCheckTestServer
{
   protected String getTransport()
   {
      return "bisocket";
   }
   
   protected int getPort()
   {
      return super.getPort() + 17;
   }
}
