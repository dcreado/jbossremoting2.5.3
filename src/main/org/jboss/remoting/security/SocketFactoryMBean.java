package org.jboss.remoting.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public interface SocketFactoryMBean
{
   public Socket createSocket(String host, int port) throws IOException, UnknownHostException;

   public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
         throws IOException, UnknownHostException;

   public Socket createSocket(InetAddress host, int port) throws IOException;

   public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
         throws IOException;


}
