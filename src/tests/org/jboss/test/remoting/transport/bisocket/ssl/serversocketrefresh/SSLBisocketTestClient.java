package org.jboss.test.remoting.transport.bisocket.ssl.serversocketrefresh;

import org.jboss.test.remoting.transport.socket.ssl.serversocketrefresh.TestClient;

/**
 * @author Michael Voss
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 *
 */
public class SSLBisocketTestClient extends TestClient
{
	protected String getTransport()
	{
	   return "sslbisocket";
	}
	
}
