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

/*
 * Created on Jul 23, 2005
 */

package org.jboss.remoting.transport.multiplex;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.ServerSocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * <code>VirtualServerSocket</code> is one of the two implementations of a server socket in the
 * Multiplex project.  Each <code>VirtualServerSocket</code> belongs to an existing virtual
 * socket group, and each <code>VirtualScoket</code> it creates is added to that virtual socket
 * group. For more details, see the Multiplex documentation on
 * the labs.jboss.org web site.
 *
 * <p>
 * Most of the methods in <code>VirtualServerSocket</code> override those in its parent class,
 * <code>java.net.ServerSocket</code>.  For method descriptions, see the <code>ServerSocket</code>
 * javadoc.
 *
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class VirtualServerSocket extends ServerSocket implements Serializable
{
   private static final Logger log = Logger.getLogger(VirtualServerSocket.class);
   private List acceptingThreads = Collections.synchronizedList(new LinkedList());

   private Map configuration = new HashMap();
   private MultiplexingManager manager;
   private MultiplexingInputStream is;
   private MultiplexingInputStream cis;
   private Protocol protocol;
   private Socket actualSocket;
   private int timeout;
   private boolean bound = false;
   private boolean connected = false;
   private boolean closed = false;
   private Socket dummySocket;
   private static final long serialVersionUID = -5320724929164012313L;

//////////////////////////////////////////////////////////////////////////////////////////////////
///        The following constructors duplicate those of the parent class ServerSocket        '///
//////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * @throws java.io.IOException
 */
   public VirtualServerSocket() throws IOException
   {
   }


/**
 *
 * See superclass javadoc.
 *
 * @param port
 * @throws IOException
 */
   public VirtualServerSocket(int port) throws IOException
   {
      bind(new InetSocketAddress(port));
      log.debug("created VirtualServerSocket: " + toString());
   }


/**
 * See superclass javadoc.
 *
 * @param port
 * @param backlog
 * @throws java.io.IOException
 */
   public VirtualServerSocket(int port, int backlog) throws IOException
   {
      this(port);
      log.warn("backlog parameter is ignored");
      log.debug("created VirtualServerSocket: " + toString());
   }

/**
 * See superclass javadoc.
 *
 * @param port
 * @param backlog
 * @param bindAddr
 * @throws java.io.IOException
 */
   public VirtualServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException
   {
      bind(new InetSocketAddress(bindAddr, port));
      log.warn("backlog parameter is ignored");
      log.debug("created VirtualServerSocket: " + toString());
   }


//////////////////////////////////////////////////////////////////////////////////////////////////
///           The following constructors are particular to VirtualServerSocket        '///
//////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * See superclass javadoc.
 * @param configuration
 */
   public VirtualServerSocket(VirtualSocket socket, Map configuration) throws IOException
   {
      this.actualSocket = socket.getActualSocket();
      if (configuration != null)
         this.configuration.putAll(configuration);
      manager = socket.getManager();
      manager.incrementReferences();
      bind(new InetSocketAddress(socket.getLocalAddress(), socket.getLocalPort()));
      log.debug("created VirtualServerSocket: " + toString());
   }


/**
 *
 * See superclass javadoc.
 *
 * @param timeout
 * @param configuration
 * @param port
 * @throws IOException
 */
   public VirtualServerSocket(InetSocketAddress remoteAddress, InetSocketAddress localAddress, int timeout, Map configuration)
   throws IOException
   {
      if (configuration != null)
         this.configuration.putAll(configuration);

      connect(remoteAddress, localAddress, timeout);
      log.debug("created VirtualServerSocket: " + toString());
   }


//////////////////////////////////////////////////////////////////////////////////////////////////
///                  The following methods are required of any ServerSocket                   '///
//////////////////////////////////////////////////////////////////////////////////////////////////

   /*
     ok: public void bind(SocketAddress endpoint) throws IOException;
     ok: public void bind(SocketAddress endpoint, int backlog) throws IOException;
     ok: public InetAddress getInetAddress();
     ok: public int getLocalPort();
     ok: public SocketAddress getLocalSocketAddress();
     ok: public Socket accept() throws IOException;
     ok: public void close() throws IOException;
     ok: public ServerSocketChannel getChannel();
     ok: public boolean isBound();
     ok: public boolean isClosed();
     ok: public void setSoTimeout(int timeout) throws SocketException;
     ok: public int getSoTimeout() throws IOException;
     ok: public void setReuseAddress(boolean on) throws SocketException;
     ok: public boolean getReuseAddress() throws SocketException;
     ok: public String toString();
     ok: public void setReceiveBufferSize(int size) throws SocketException;
     ok: public int getReceiveBufferSize() throws SocketException;
   */

/**
 * See superclass javadoc.
 */
   public synchronized Socket accept() throws IOException
   {
      log.debug("entering accept()");
      long start = System.currentTimeMillis();
      int timeout = getSoTimeout();
      int timeLeft = 0;

      if (isClosed())
         throw new SocketException("Socket is closed");

      if (!isBound())
         throw new SocketException("Socket is not bound yet");

      SecurityManager security = System.getSecurityManager();

      if (security != null)
      {
         security.checkAccept(actualSocket.getInetAddress().getHostAddress(), actualSocket.getPort());
      }

      Thread currentThread = Thread.currentThread();
      acceptingThreads.add(currentThread);
      VirtualSocket virtualSocket = null;

      try
      {
         if (timeout > 0)
            if ((timeLeft = timeout - (int) (System.currentTimeMillis() - start)) <= 0)
            {
               log.error("timed out");
               throw new SocketTimeoutException("Accept timed out");
            }

         log.debug("timeLeft: " + timeLeft);
         SocketId clientPort = protocol.acceptConnect(is, timeLeft);
         log.debug("clientPort:  " + clientPort.getPort());

         virtualSocket = new VirtualSocket(manager, clientPort, configuration);
         manager.incrementReferences();
         int localPort = virtualSocket.getLocalVirtualPort();
         protocol.answerConnect((MultiplexingOutputStream) virtualSocket.getOutputStream(), localPort);
         return virtualSocket;
      }
      catch (IOException e)
      {
         if (e instanceof InterruptedIOException
             || "Socket closed".equals(e.getMessage())
             || "An existing connection was forcibly closed by the remote host".equals(e.getMessage()))
           log.debug(e);
         else
            log.error(e);

         if (virtualSocket != null)
            virtualSocket.close();

         if (isClosed())
            throw new SocketException("Socket closed");

         if (e instanceof SocketTimeoutException)
            throw new SocketTimeoutException("Accept timed out");

         throw e;
      }
      finally
      {
         acceptingThreads.remove(currentThread);

         if (isClosed())
         {
            if (virtualSocket != null)
               virtualSocket.close();

            throw new SocketException("Socket closed");
         }
      }
   }


/**
 * See superclass javadoc.
 */
   public void bind(SocketAddress socketAddress) throws IOException
   {
      bind(socketAddress, 1);
   }


/**
 * See superclass javadoc.
 */
   public void bind(SocketAddress socketAddress, int backlog) throws IOException
   {
      if (backlog != 1)
      {
         log.warn("backlog != 1: ignored");
      }

      if (isClosed())
         throw new SocketException("Socket is closed");

      if (isBound())
         throw new SocketException("Already bound");

      if (socketAddress == null)
         socketAddress = new InetSocketAddress(0);

      if (!(socketAddress instanceof InetSocketAddress))
         throw new IllegalArgumentException("Unsupported address type");

      InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;

      if (inetSocketAddress.isUnresolved())
         throw new SocketException("Unresolved address");

      SecurityManager security = System.getSecurityManager();

      if (security != null)
         security.checkListen(inetSocketAddress.getPort());

      if (manager == null)
      {
         manager = MultiplexingManager.getaManagerByLocalAddress(inetSocketAddress, configuration);
         actualSocket = manager.getSocket();
      }

      try
      {
         bound = true;
         is = manager.registerServerSocket(this);
         cis = manager.getAnInputStream(SocketId.SERVER_SOCKET_CONNECT_ID, null);
         is.setTimeout(timeout);
         cis.setTimeout(timeout);

         if (manager.isConnected())
         {
            protocol = manager.getProtocol();

            // remote MultiplexingManager might be shutting down and refuse to register ServerSocket
            protocol.registerRemoteServerSocket(getSoTimeout());
            connected = true;
         }
      }
      catch (IOException e)
      {
         bound = false;

         if (manager.isServerSocketRegistered())
            manager.unRegisterServerSocket(this);

         throw e;
      }

      log.debug(toString());
   }


/**
 * See superclass javadoc.
 */
   public void close() throws IOException
   {
      if (isClosed())
         return;

      closed = true;

      // Try once to interrupt accepting threads.
      if (!acceptingThreads.isEmpty())
      {
         // We get a copy of acceptingThreads to avoid a ConcurrentModificationException when a thread
         // removes itself from acceptingThreads.
         LinkedList threads = new LinkedList(acceptingThreads);
         Iterator it = threads.iterator();

         while (it.hasNext())
         {
            Thread t = (Thread) it.next();
            t.interrupt();
            log.debug("interrupting accepting thread: " + t.getName());
         }

         if (!acceptingThreads.isEmpty())
         {
            MultiplexingManager.addToPendingActions(new PendingClose(this));
         }
      }

      // We want the unregister request to go out to the remote MultiplexingManager first.
      // This way, if the local unregister request causes the local MultiplexingManager to
      // shut down, the remote MultiplexingManager will get the unregisterRemoteServerSocket
      // request before it gets the shutdown manager request.

      if (protocol != null)
         protocol.unregisterRemoteServerSocket();

      if (manager != null)
         manager.unRegisterServerSocket(this);
   }


/**
 * See superclass javadoc.
 */
   public InetAddress getInetAddress()
   {
      if (actualSocket == null)
         return null;

      return actualSocket.getInetAddress();
   }


/**
 * See superclass javadoc.
 */
   public int getLocalPort()
   {
      if (actualSocket == null)
         return -1;

      return actualSocket.getLocalPort();
   }


/**
 * See superclass javadoc.
 */
   public ServerSocketChannel getChannel()
   {
      return null;
   }


/**
 * See superclass javadoc.
 */
   public SocketAddress getLocalSocketAddress()
   {
      if (actualSocket == null)
         return null;

      return actualSocket.getLocalSocketAddress();
   }


/**
 * See superclass javadoc.
 */
   public int getReceiveBufferSize() throws SocketException
   {
      if (actualSocket == null)
      {
         if (dummySocket == null)
            dummySocket = new Socket();

         return dummySocket.getReceiveBufferSize();
      }

      return actualSocket.getReceiveBufferSize();
   }


/**
 * See superclass javadoc.
 */
   public boolean getReuseAddress() throws SocketException
   {
      if (actualSocket == null)
         return true;
      else
         return actualSocket.getReuseAddress();
   }


/**
 * See superclass javadoc.
 */
   public int getSoTimeout() throws SocketException
   {
      if (isClosed())
         throw new SocketException("Socket is closed");

      return timeout;
   }


/**
 * See superclass javadoc.
 */
   public boolean isBound()
   {
      return bound;
   }


/**
 * See superclass javadoc.
 */
   public boolean isClosed()
   {
      return closed;
   }


/**
 * See superclass javadoc.
 */
   public void setReceiveBufferSize (int size) throws SocketException
   {
      if (actualSocket != null)
         actualSocket.setReceiveBufferSize(size);
   }


/**
 * See superclass javadoc.
 */
   public void setReuseAddress(boolean on) throws SocketException
   {
      if (actualSocket != null)
         actualSocket.setReuseAddress(on);
   }


/**
 * See superclass javadoc.
 */
   public void setSoTimeout(int timeout) throws SocketException
   {
      if (isClosed())
         throw new SocketException("Socket is closed");

      if (timeout < 0)
         throw new IllegalArgumentException("timeout can't be negative");

      this.timeout = timeout;

      if (is != null)
         is.setTimeout(timeout);

      if (cis != null)
         cis.setTimeout(timeout);
   }


/**
 * See superclass javadoc.
 */
   public String toString()
   {
      StringBuffer answer = new StringBuffer().append("VirtualServerSocket[");

      if (actualSocket == null)
         answer.append("unbound");
      else
         answer.append(actualSocket.toString());

      return answer.append("]").toString();
   }


   //////////////////////////////////////////////////////////////////////////////////////////////////
   ///             The following methods are specific to VirtualServerSocket           '///
   //////////////////////////////////////////////////////////////////////////////////////////////////

   /**
    * See superclass javadoc.
    */
   public void connect(SocketAddress remoteAddress) throws IOException
   {
      connect(remoteAddress, null, timeout);
   }


/**
 * Connects this socket to the server.
 *
 * See superclass javadoc.
 */
   public void connect(SocketAddress remoteAddress, SocketAddress localAddress) throws IOException
   {
      connect(remoteAddress, localAddress, timeout);
   }


/**
 * See superclass javadoc.
 */
   public void connect(SocketAddress remoteAddress, SocketAddress localAddress, int timeout) throws IOException
   {
      long start = System.currentTimeMillis();
      int timeLeft = 0;

      if (remoteAddress == null)
         throw new IllegalArgumentException("connect: The address can't be null");

      if (timeout < 0)
         throw new IllegalArgumentException("connect: timeout can't be negative");

      if (isClosed())
         throw new SocketException("Socket is closed");

      if (!(remoteAddress instanceof InetSocketAddress))
         throw new IllegalArgumentException("Unsupported address type");

      InetSocketAddress remoteInetSocketAddress = (InetSocketAddress) remoteAddress;
      SecurityManager security = System.getSecurityManager();

      if (security != null)
      {
         if (remoteInetSocketAddress.isUnresolved())
            security.checkConnect(remoteInetSocketAddress.getHostName(), remoteInetSocketAddress.getPort());
         else
            security.checkConnect(remoteInetSocketAddress.getAddress().getHostAddress(), remoteInetSocketAddress.getPort());
      }

      // Binding entails getting a MultiplexingManager, which might already be connected,
      // thereby putting the ServerSocket in a connected state.  Therefore, we allow connect() to be
      // called on a connected ServerSocket, as long as the host to which it is to be connected
      // is the same as the host to which it is already connected.
      if (isConnected())
         if (getRemoteAddress().equals(remoteInetSocketAddress.getAddress()))
            return;
         else
            throw new SocketException("already connected");

      if (manager == null)
      {
         if (timeout > 0)
            if ((timeLeft = timeout - (int) (System.currentTimeMillis() - start)) <= 0)
               throw new SocketTimeoutException("connect timed out");

         if (localAddress == null)
            manager = MultiplexingManager.getaManagerByRemoteAddress(remoteInetSocketAddress,
                                                                     timeLeft, configuration);
         else
         {
            InetSocketAddress localInetSocketAddress = (InetSocketAddress) localAddress;
            manager = MultiplexingManager.getaManagerByAddressPair(remoteInetSocketAddress,
                                                                   localInetSocketAddress,
                                                                   timeLeft,
                                                                   configuration);
         }
      }

      actualSocket = manager.getSocket();

      try
      {
         if (!isBound())
         {
            log.debug("calling registerServerSocket()");
            is = manager.registerServerSocket(this);
            cis = manager.getAnInputStream(SocketId.SERVER_SOCKET_CONNECT_ID, null);
            is.setTimeout(this.timeout);
            cis.setTimeout(this.timeout);
            bound = true;
         }

         // If the manager is not connected, we need to connect to the remote MasterServerSocket, which
         // will create a real socket and a MultiplexingManager to wrap it.
         if (!manager.isConnected())
         {
            if (timeout > 0)
               if ((timeLeft = timeout - (int) (System.currentTimeMillis() - start)) <= 0)
                  throw new SocketTimeoutException("connect timed out");

            manager.connect(remoteInetSocketAddress, timeLeft);
            protocol = manager.getProtocol();

            if (timeout > 0)
               if ((timeLeft = timeout - (int) (System.currentTimeMillis() - start)) <= 0)
                  throw new SocketTimeoutException("connect timed out");

            cis.setTimeout(timeout);
            protocol.connect(cis, SocketId.SERVER_SOCKET_ID, timeLeft);

            // Remote MultiplexingManager might be shutting down and refuse to register ServerSocket.
            // If so, registerRemoteServerSocket() will throw an IOException
            if (timeout > 0)
               if ((timeLeft = timeout - (int) (System.currentTimeMillis() - start)) <= 0)
                  throw new SocketTimeoutException("connect timed out");
         }
         else
            protocol = manager.getProtocol();

         if (timeout > 0)
            if ((timeLeft = timeout - (int) (System.currentTimeMillis() - start)) <= 0)
               throw new SocketTimeoutException("connect timed out");

         protocol.registerRemoteServerSocket(timeLeft);
      }
      catch (IOException e)
      {
         log.error("i/o exception in VirtualServerSocket.connect()", e);

         if (manager.isServerSocketRegistered())
            manager.unRegisterServerSocket(this);

         if (e instanceof SocketTimeoutException)
            throw new SocketTimeoutException("connect timed out");

         throw e;
      }
      finally
      {
         if (cis != null)
            cis.setTimeout(this.timeout);
      }

      connected = true;
      log.debug(toString());
   }


/**
 * @return
 */
   public MultiplexingManager getMultiplexingManager()
   {
      return manager;
   }



/**
 * See superclass javadoc.
 */
   public boolean isConnected()
   {
      return connected;
   }


/**
 * See superclass javadoc.
 */
   public InetAddress getRemoteAddress()
   {
      if (actualSocket == null)
         return null;

      return actualSocket.getInetAddress();
   }


/**
 * See superclass javadoc.
 */
   public int getRemotePort()
   {
      if (actualSocket == null)
         return 0;

      return actualSocket.getPort();
   }


/**
 * See superclass javadoc.
 */
   public void setConfiguration(Map configuration)
   {
      this.configuration.putAll(configuration);
   }


/**
 *
 */
   protected void doClose()
   {
      // We get a copy of acceptingThreads to avoid a ConcurrentModificationException when a thread
      // removes itself from acceptingThreads.
      LinkedList threads = new LinkedList(acceptingThreads);
      Iterator it = threads.iterator();

      while (it.hasNext())
      {
         Thread t = (Thread) it.next();
         t.interrupt();
         log.debug("interrupting accepting thread: " + t.getName());

         // Make sure the accepting thread caught the interrupt.
         while (acceptingThreads.contains(t))
         {
            try
            {
               log.debug("waiting for accepting thread to catch interrupt: " + t.getName());
               Thread.sleep(500);
               t.interrupt();
               log.debug("interrupting accepting thread: " + t.getName());
            }
            catch (InterruptedException ignored) {}
         }
      }
   }


   protected class PendingClose extends PendingAction
   {
      public PendingClose(Object o)
      {
         super(o);
      }

      public void doAction()
      {
         ((VirtualServerSocket) o).doClose();
      }
   }
}
