package org.jboss.test.remoting.transport.bisocket.connection.retry;

import org.jboss.test.remoting.transport.socket.connection.retry.SocketRetryClient;

/**
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ron.sigal@jboss.org">Ron Sigal</a>
 */
public class BisocketRetryClient extends SocketRetryClient
{
   protected String getTransport()
   {
      return "bisocket";
   }
   
   protected int getPort()
   {
      return super.getPort() + 13;
   }
}
