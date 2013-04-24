package org.jboss.remoting;

/**
 * Can expect this exception to be included as parameter to
 * handleConnectionException() method of org.jboss.remoting.ConnectionListener
 * when client disconnects from the server.  Notice, this is not due to the 
 * client failing, but due to normal client termination.
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @see org.jboss.remoting.ConnectionListener
 */
public class ClientDisconnectedException extends Exception
{
   private static final long serialVersionUID = 1665349772039533352L;
}
