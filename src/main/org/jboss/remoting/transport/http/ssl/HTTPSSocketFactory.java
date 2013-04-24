/*
* JBoss, Home of Professional Open Source
* Copyright 2005, JBoss Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.remoting.transport.http.ssl;

import org.jboss.remoting.util.socket.HandshakeRepeater;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * This is a wrapper for the real SSLSocketFactory passed to it so can have it create
 * the real SSLSocket and then add HandshakeCompletedListener to it.  This is needed
 * because no direct API to the HttpsURLConnection class to do this, so am having to
 * do it this way.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class HTTPSSocketFactory extends SSLSocketFactory
{
   private SSLSocketFactory targetFactory = null;
   private HandshakeCompletedListener targetListener = null;

   public HTTPSSocketFactory(SSLSocketFactory socketFactory, HandshakeCompletedListener listener)
   {
      this.targetFactory = socketFactory;
      this.targetListener = listener;
      if (targetFactory == null)
      {
         throw new RuntimeException("Can not create HTTPSSocketFactory with target SSLSocketFactory being null.");
      }
      if (targetListener == null)
      {
         throw new RuntimeException("Can not create HTTPSSocketFactory with target HandshakeCompletedListener being null.");
      }
   }

   public String[] getDefaultCipherSuites()
   {
      return targetFactory.getDefaultCipherSuites();
   }

   public String[] getSupportedCipherSuites()
   {
      return targetFactory.getSupportedCipherSuites();
   }

   public Socket createSocket(Socket socket, String string, int i, boolean b) throws IOException
   {
      Socket retSocket = targetFactory.createSocket(socket, string, i, b);

      if (retSocket instanceof SSLSocket)
      {
         establishHandshake((SSLSocket) retSocket, targetListener);
      }

      return retSocket;
   }

   public Socket createSocket(String string, int i) throws IOException, UnknownHostException
   {
      Socket retSocket = targetFactory.createSocket(string, i);

      if (retSocket instanceof SSLSocket)
      {
         establishHandshake((SSLSocket) retSocket, targetListener);
      }

      return retSocket;
   }

   public Socket createSocket(InetAddress inetAddress, int i) throws IOException
   {
      Socket retSocket = targetFactory.createSocket(inetAddress, i);

      if (retSocket instanceof SSLSocket)
      {
         establishHandshake((SSLSocket) retSocket, targetListener);
      }

      return retSocket;
   }

   public Socket createSocket(String string, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException
   {
      Socket retSocket = targetFactory.createSocket(string, i, inetAddress, i1);

      if (retSocket instanceof SSLSocket)
      {
         establishHandshake((SSLSocket) retSocket, targetListener);
      }

      return retSocket;
   }

   public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException
   {
      Socket retSocket = targetFactory.createSocket(inetAddress, i, inetAddress1, i1);

      if (retSocket instanceof SSLSocket)
      {
         establishHandshake((SSLSocket) retSocket, targetListener);
      }

      return retSocket;
   }

   private void establishHandshake(SSLSocket sslSocket, HandshakeCompletedListener listener)
         throws IOException
   {
      HandshakeRepeater repeater = new HandshakeRepeater(listener);
      sslSocket.addHandshakeCompletedListener(repeater);
      sslSocket.getSession();
      repeater.waitForHandshake();
   }

}