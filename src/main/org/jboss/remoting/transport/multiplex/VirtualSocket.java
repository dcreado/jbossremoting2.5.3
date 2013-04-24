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
 * Created on Jul 22, 2005
 */

package org.jboss.remoting.transport.multiplex;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <code>VirtualSocket</code> represents an endpoint on a virtual connection created by the
 * Multiplex system.  It extends <code>java.net.Socket</code> and reimplements nearly all of
 * the methods in <code>java.net.Socket</code>.  For information about method behavior, please
 * see the <code>java.net.Socket</code> javadoc.  For more information about the nature of
 * virtual sockets, please see the Multiplex documentation at the labs.jboss.org
 * web site.
 *
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class VirtualSocket extends Socket
{
   protected static final Logger log = Logger.getLogger(VirtualSocket.class);
   protected static Thread closingThread;

   private Map configuration = new HashMap();
   private MultiplexingManager manager;
   private Protocol protocol;
   private Socket actualSocket;
   private SocketId remoteSocketId;
   private SocketId localSocketId;
   private MultiplexingInputStream inputStream;
   private MultiplexingOutputStream outputStream;
   private Set disconnectListeners = new HashSet();

   private boolean bound = false;
   private boolean closed = false;
   private boolean connected = false;
   private boolean inputShutdown = false;
   private boolean outputShutdown = false;
   private boolean receivedDisconnectMessage = false;

   private int     timeout;
   private Socket  dummySocket;

   private boolean functional = true;

   private boolean trace;
   private boolean debug;
   private boolean info;


   /**
    * A class that implements <code>DisconnectListener</code> can register to
    * be notified if the remote peer of this <code>VirtualSocket</code> has
    * disconnected.
    */
   public interface DisconnectListener
   {
      void notifyDisconnected(VirtualSocket virtualSocket);
   }


   public VirtualSocket(MultiplexingManager manager, SocketId remoteSocketId, Map configuration) throws IOException
   {
      this.manager = manager;
      this.actualSocket = manager.getSocket();
      this.remoteSocketId = remoteSocketId;
      this.configuration.putAll(configuration);
      protocol = manager.getProtocol();
      localSocketId = new SocketId();
      inputStream = manager.registerSocket(this);
//      outputStream = new MultiplexingOutputStream(manager, this, remoteSocketId);
      outputStream = manager.getAnOutputStream(this, remoteSocketId);
      bound = true;
      connected = true;

      trace = log.isTraceEnabled();
      debug = log.isDebugEnabled();
      info = log.isInfoEnabled();
      if (debug) log.debug("created virtual socket on port: " + localSocketId.getPort());
   }


   public VirtualSocket(Map configuration)
   {
      this.configuration.putAll(configuration);
   }


//////////////////////////////////////////////////////////////////////////////////////////////////
///           The following constructors duplicate those of the parent class Socket           '///
//////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * See superclass javadoc.
 */
   public VirtualSocket()
   {
      log.debug("created unbound virtual socket");
   }


/**
 * See superclass javadoc.
 */
   public VirtualSocket(String host, int port) throws UnknownHostException, IOException
   {
      InetSocketAddress address = null;

      if (host == null)
         address = new InetSocketAddress(InetAddress.getByName(null), port);
      else
         address = new InetSocketAddress(host, port);

      connect(address);
      if (debug) log.debug("created virtual socket on port: " + localSocketId.getPort());
   }


/**
 * See superclass javadoc.
 * @param host
 * @param port
 * @param stream
 * @throws java.io.IOException
 */
   public VirtualSocket(String host, int port, boolean stream) throws IOException
   {
      if (!stream)
      {
         throw new SocketException("Deprecated: use DataGramSocket instead of stream = false");
      }

      InetSocketAddress address = null;

      if (host == null)
         address = new InetSocketAddress(InetAddress.getByName(null), port);
      else
         address = new InetSocketAddress(host, port);

      connect(address);
      if (debug) log.debug("created virtual socket on port: " + localSocketId.getPort());
   }


/**
 * See superclass javadoc.
 * @param address
 * @param port
 * @throws java.io.IOException
 */
   public VirtualSocket(InetAddress address, int port) throws IOException
   {
      connect(new InetSocketAddress(address, port));
      if (debug) log.debug("created virtual socket on port: " + localSocketId.getPort());
   }


/**
 * See superclass javadoc.
 * @param host
 * @param port
 * @param stream
 * @throws java.io.IOException
 */
   public VirtualSocket(InetAddress host, int port, boolean stream) throws IOException
   {
      if (!stream)
      {
         throw new SocketException("Deprecated: use DataGramSocket instead of stream = false");
      }

      connect(new InetSocketAddress(host, port));
      if (debug) log.debug("created virtual socket on port: " + localSocketId.getPort());
   }


/**
 * This constuctor is not implemented.
 * <p>
 * @param impl
 * @throws java.net.SocketException in all cases
 */
   public VirtualSocket(SocketImpl impl) throws SocketException
   {
      throw new SocketException("VirtualSocket does not use SocketImpl");
   }


/**
 * See superclass javadoc.
 * @param host
 * @param port
 * @param localAddr
 * @param localPort
 * @throws java.io.IOException
 */
   public VirtualSocket(String host, int port, InetAddress localAddr, int localPort) throws IOException
   {
      this(InetAddress.getByName(host), port, localAddr, localPort);
   }


/**
 * See superclass javadoc.
 * @param address
 * @param port
 * @param localAddr
 * @param localPort
 * @throws java.io.IOException
 */
   public VirtualSocket(InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException
   {
      this();
      connect(new InetSocketAddress(address, port), new InetSocketAddress(localAddr, localPort), 0);
      if (debug) log.debug("created virtual socket on port: " + localSocketId.getPort());
   }



//////////////////////////////////////////////////////////////////////////////////////////////////
///                     The following methods are required of all Socket's                    '///
//////////////////////////////////////////////////////////////////////////////////////////////////


   /*************************************************************************************************
    * public methods in Socket class
    *
      ok: public void connect(SocketAddress endpoint) throws IOException;
      ok: public void connect(SocketAddress endpoint, int timeout) throws IOException;
      ok: public void bind(SocketAddress bindpoint) throws IOException;
      ok: public InetAddress getInetAddress();
      ok: public InetAddress getLocalAddress();
      ok: public int getPort();
      ok: public int getLocalPort();
      ok: public SocketAddress getRemoteSocketAddress();
      ok: public SocketAddress getLocalSocketAddress();
      ok: public SocketChannel getChannel();
      ok: public InputStream getInputStream() throws IOException;
      ok: public OutputStream getOutputStream() throws IOException;
      ok: public void setTcpNoDelay(boolean on) throws SocketException;
      ok: public boolean getTcpNoDelay() throws SocketException;
      ok: public void setSoLinger(boolean on, int linger) throws SocketException;
      ok: public int getSoLinger() throws SocketException;
      ok: public void sendUrgentData(int data) throws IOException;
      ok: public void setOOBInline(boolean on) throws SocketException;
      ok: public boolean getOOBInline() throws SocketException;
      ok: public void setSoTimeout(int timeout) throws SocketException;
      ok: public int getSoTimeout() throws SocketException;
      ok: public void setSendBufferSize(int size) throws SocketException;
      ok: public int getSendBufferSize() throws SocketException;
      ok: public void setReceiveBufferSize(int size) throws SocketException;
      ok: public int getReceiveBufferSize() throws SocketException;
      ok: public void setKeepAlive(boolean on) throws SocketException;
      ok: public boolean getKeepAlive() throws SocketException;
      ok: public void setTrafficClass(int tc) throws SocketException;
      ok: public int getTrafficClass() throws SocketException;
      ok: public void setReuseAddress(boolean on) throws SocketException;
      ok: public boolean getReuseAddress() throws SocketException;
      ok: public void close() throws IOException;
      ok: public void shutdownInput() throws IOException;
      ok: public void shutdownOutput() throws IOException;
      ok: public String toString();
      ok: public boolean isConnected();
      ok: public boolean isBound();
      ok: public boolean isClosed();
      ok: public boolean isInputShutdown();
      ok: public boolean isOutputShutdown();
   *************************************************************************************************/


/**
 * See superclass javadoc.
 */
   public void bind(SocketAddress address) throws IOException
   {
      if (isClosed())
         throw new SocketException("Socket is closed");

      if (isBound())
         throw new SocketException("Already bound");

      if (address != null && (!(address instanceof InetSocketAddress)))
         throw new IllegalArgumentException("Unsupported address type");

      InetSocketAddress inetAddress = (InetSocketAddress) address;

      if (inetAddress != null && inetAddress.isUnresolved())
         throw new SocketException("Unresolved address");

      manager = MultiplexingManager.getaManagerByLocalAddress(inetAddress, configuration);
      actualSocket = manager.getSocket();
      localSocketId = new SocketId();
      if (debug) log.debug("bound virtual socket to port: " + localSocketId.getPort());
      bound = true;
   }


/**
 * See superclass javadoc.
 */
   public void close() throws IOException
   {
      if (closed)
         return;

      log.debug("closing: " + localSocketId);
      closed = true;

      if (connected && !receivedDisconnectMessage)
         protocol.disconnect(remoteSocketId);

      if (inputStream != null)
         inputStream.close();

      if (outputStream != null)
      {
         outputStream.flush();
         outputStream.close();
      }

      if (localSocketId != null)
         localSocketId.releasePort();

      // This VirtualSocket might have been unregistered when connect() failed.
      if (manager.isSocketRegistered(this.localSocketId))
      {
         MultiplexingManager.addToPendingActions(new PendingClose(this));
      }

      if (debug) log.debug("virtual socket closed on port: " + localSocketId.getPort());
   }


   /**
    * See superclass javadoc.
    */
   public void connect(SocketAddress socketAddress) throws IOException
   {
      connect(socketAddress, null, timeout);
   }


/**
 * See superclass javadoc.
 */
   public void connect(SocketAddress socketAddress, int timeout) throws IOException
   {
      connect(socketAddress, null, timeout);
   }


/**
 * See superclass javadoc.
 */
   public SocketChannel getChannel()
   {
      return null;
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
   public InputStream getInputStream() throws IOException
   {
      if (isClosed())
         throw new SocketException("Socket is closed");

      if (actualSocket == null || !connected)
         throw new SocketException("Socket is not connected");

      if (isInputShutdown())
         throw new SocketException("Socket input is shutdown");

      return inputStream;
   }


/**
 * See superclass javadoc.
 */
   public boolean getKeepAlive() throws SocketException
   {
      if (actualSocket == null)
         return false;

      return actualSocket.getKeepAlive();
   }


/**
 * See superclass javadoc.
 * Note. <code>Socket.getLocalAddress()</code> returns "wildcard" address for an unbound socket.
 */
   public InetAddress getLocalAddress()
   {
      if (actualSocket == null)
      {
         if (dummySocket == null)
            dummySocket = new Socket();

         return dummySocket.getLocalAddress();
      }

      // The following is a workaround for a problem in NIO sockets, which sometimes
      // return "0.0.0.0" instead of "127.0.0.1".
      InetAddress address = actualSocket.getLocalAddress();
      try
      {
         if ("0.0.0.0".equals(address.getHostAddress()))
            return InetAddress.getByName("localhost");
      }
      catch (UnknownHostException e)
      {
         return address;
      }

      return address;
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
   public SocketAddress getLocalSocketAddress()
   {
      if (actualSocket == null)
         return null;

      SocketAddress address = actualSocket.getLocalSocketAddress();
      InetSocketAddress socketAddress = null;

      // The following is a workaround for a problem in NIO sockets, which sometimes
      // return "0.0.0.0" instead of "127.0.0.1".
      if (address instanceof InetSocketAddress)
      {
         socketAddress = (InetSocketAddress) address;
         if ("0.0.0.0".equals(socketAddress.getHostName()) ||
             socketAddress.getAddress() == null)
            return new InetSocketAddress("localhost", socketAddress.getPort());
      }

      return address;
   }


/**
 * See superclass javadoc.
 */
   public boolean getOOBInline() throws SocketException
   {
      return false;
   }


/**
 * See superclass javadoc.
 */
   public OutputStream getOutputStream() throws IOException
   {
      if (isClosed())
         throw new SocketException("Socket is closed");

      if (actualSocket == null || !connected)
         throw new SocketException("Socket is not connected");

      if (isOutputShutdown())
         throw new SocketException("Socket output is shutdown");

      // TODO: return distinct output streams? See PlainSocketImpl.
      //return new SocketOutputStream(this);
      return outputStream;
   }


/**
 * See superclass javadoc.
 */
   public int getPort()
   {
      if (actualSocket == null)
         return 0;

      return actualSocket.getPort();
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
   public SocketAddress getRemoteSocketAddress()
   {
      if (actualSocket == null)
         return null;

      return actualSocket.getRemoteSocketAddress();
   }


/**
 * See superclass javadoc.
 */
   public boolean getReuseAddress() throws SocketException
   {
      if (actualSocket == null)
         return false;

      return actualSocket.getReuseAddress();
   }


/**
 * See superclass javadoc.
 */
   public int getSendBufferSize() throws SocketException
   {
      if (actualSocket == null)
      {
         if (dummySocket == null)
            dummySocket = new Socket();

         return dummySocket.getSendBufferSize();
      }

      return actualSocket.getSendBufferSize();
   }


/**
 * See superclass javadoc.
 */
   public int getSoLinger() throws SocketException
   {
      if (actualSocket == null)
         return -1;

      return actualSocket.getSoLinger();
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
   public boolean getTcpNoDelay() throws SocketException
   {
      if (actualSocket == null)
         return false;

      return actualSocket.getTcpNoDelay();
   }


/**
 * See superclass javadoc.
 */
   public int getTrafficClass() throws SocketException
   {
      if (actualSocket == null)
         return 0;

      return actualSocket.getTrafficClass();
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
   public boolean isConnected()
   {
      return connected;
   }


/**
 * See superclass javadoc.
 */
   public boolean isInputShutdown()
   {
      return inputShutdown;
   }


/**
 * See superclass javadoc.
 */
   public boolean isOutputShutdown()
   {
      return outputShutdown;
   }


/**
 * This method is not implemented.
 * <p>
 * See superclass javadoc.
 */
   public void sendUrgentData(int data) throws IOException
   {
      log.warn("sendUrgentData() called: ignored");

      if (isClosed())
         throw new IOException("Socket Closed");
   }


/**
 * See superclass javadoc.
 */
   public void setKeepAlive(boolean on) throws SocketException
   {
      if (actualSocket != null)
         actualSocket.setKeepAlive(on);
   }


/**
 *
 */
   public void setOOBInline(boolean on) throws SocketException
   {
      log.warn("setOOBInLine() called: ignored");

      if (isClosed())
         throw new SocketException("Socket is closed");
   }


/**
 * See superclass javadoc.
 */
   public void setReceiveBufferSize(int size) throws SocketException
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
   public void setSendBufferSize(int size) throws SocketException
   {
      if (actualSocket != null)
         actualSocket.setSendBufferSize(size);
   }


/**
 * See superclass javadoc.
 */
   public void setSoLinger(boolean on, int linger) throws SocketException
   {
      if (actualSocket != null)
         actualSocket.setSoLinger(on, linger);
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

      if (inputStream != null)
         inputStream.setTimeout(timeout);
   }


/**
 * See superclass javadoc.
 */
   public void setTcpNoDelay(boolean on) throws SocketException
   {
      if (actualSocket != null)
         actualSocket.setTcpNoDelay(on);
   }


/**
 * See superclass javadoc.
 */
   public void setTrafficClass(int tc) throws SocketException
   {
      if (actualSocket != null)
         actualSocket.setTrafficClass(tc);
   }


/**
 * See superclass javadoc.
 */
   public void shutdownInput() throws IOException
   {
      if (isClosed())
         throw new SocketException("Socket is closed");

      if (!isConnected())
         throw new SocketException("Socket is not connected");

      if (isInputShutdown())
         throw new SocketException("Socket input is already shutdown");

      inputStream.setEOF();
      inputShutdown = true;
      //         protocol.notifyInputShutdown(localSocketId);
   }


/**
* See superclass javadoc.
*/
   public void shutdownOutput() throws IOException
   {
      if (isClosed())
         throw new SocketException("Socket is closed");

      if (!isConnected())
         throw new SocketException("Socket is not connected");

      if (isOutputShutdown())
         throw new SocketException("Socket output is already shutdown");

      outputStream.shutdown();
      outputShutdown = true;
      protocol.notifyOutputShutdown(remoteSocketId);
   }


/**
 * See superclass javadoc.
 */
   public String toString()
   {
      StringBuffer answer = new StringBuffer().append("VirtualSocket[");

      if (actualSocket == null)
         answer.append("unbound");
      else
         answer.append(actualSocket.toString());

      return answer.append("]").toString();
   }

//////////////////////////////////////////////////////////////////////////////////////////////////
///                     The following methods are specific to VirtualSockets                   ///
//////////////////////////////////////////////////////////////////////////////////////////////////

/**
 *
 */
   public void addDisconnectListener(DisconnectListener listener)
   {
      disconnectListeners.add(listener);
   }


/**
 *
 */
   public void connect(SocketAddress remoteAddress, SocketAddress localAddress, int timeout) throws IOException
   {
      log.debug("entering connect()");
      long start = System.currentTimeMillis();
      int timeLeft = 0;

      if (remoteAddress == null)
         throw new IllegalArgumentException("connect: The address can't be null");

      if (timeout < 0)
         throw new IllegalArgumentException("connect: timeout can't be negative");

      if (isClosed())
         throw new SocketException("Socket is closed");

      if (isConnected())
         throw new SocketException("already connected");

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

      if (timeout > 0)
         if ((timeLeft = timeout - (int) (System.currentTimeMillis() - start)) <= 0)
            throw new SocketTimeoutException("connect timed out");

      if (manager == null)
      {
         if (localAddress == null)
            manager = MultiplexingManager.getaShareableManager(remoteInetSocketAddress, timeLeft, configuration);
         else
         {
            InetSocketAddress localInetSocketAddress = (InetSocketAddress) localAddress;
            manager = MultiplexingManager.getaShareableManagerByAddressPair(remoteInetSocketAddress,
                                                                            localInetSocketAddress,
                                                                            timeLeft,
                                                                            configuration);
         }
      }

      try
      {
         if (timeout > 0)
            if ((timeLeft = timeout - (int) (System.currentTimeMillis() - start)) <= 0)
               throw new SocketTimeoutException("connect timed out");

         manager.connect(remoteInetSocketAddress, timeLeft);
         actualSocket = manager.getSocket();
         protocol = manager.getProtocol();

         if (!bound)
         {
            localSocketId = new SocketId();
            bound = true;
         }

         inputStream = manager.registerSocket(this);
         inputStream.setTimeout(timeout);

         if (timeout > 0)
            if ((timeLeft = timeout - (int) (System.currentTimeMillis() - start)) <= 0)
               throw new SocketTimeoutException("connect timed out");

         remoteSocketId = protocol.connect(inputStream, localSocketId, timeLeft);
         outputStream = new MultiplexingOutputStream(manager, this, remoteSocketId);
      }
      catch (IOException e)
      {
         // Calling unRegisterSocket() will lead to this VirtualSocket being closed.
         try
         {
            manager.unRegisterSocket(this);
         }
         catch (IOException ignored)
         {
            // May not be registered yet.
         }

         if (e instanceof SocketTimeoutException)
            throw new SocketTimeoutException("connect timed out");

         throw e;
      }
      finally
      {
         if (inputStream != null)
            inputStream.setTimeout(this.timeout);
      }

      connected = true;
   }


   public MultiplexingManager getMultiplexingManager()
   {
      return manager;
   }


   public int getVirtualPort()
   {
      return remoteSocketId.getPort();
   }


   public int getLocalVirtualPort()
   {
      return localSocketId.getPort();
   }


/**
 *
 * @return
 */
   public SocketId getLocalSocketId()
   {
      return localSocketId;
   }


/**
 * @return
 */
   public Socket getRealSocket()
   {
      return actualSocket;
   }


/**
 *
 * @return
 */
   public SocketId getRemoteSocketId()
   {
      return localSocketId;
   }


   /**
    * Returns true if and only if has not received notification of error state
    * on actual connection.
    *
    * @return true if and only if has not received notification of error state
    *         on actual connection
    */
   public boolean isFunctional()
   {
      return functional;
   }

/**
 *
 * @param listener
 */
   public void removeDisconnectListener(DisconnectListener listener)
   {
      if (!disconnectListeners.remove(listener))
         log.error("attempt to remove unregistered DisconnectListener: " + listener);
   }


/**
 * @param configuration
 */
   public void setConfiguration(Map configuration)
   {
      this.configuration.putAll(configuration);
   }
//////////////////////////////////////////////////////////////////////////////////////////////////
///                              Protected getters and setters                                '///
//////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * @return Returns the actualSocket.
 */
   protected Socket getActualSocket()
   {
      return actualSocket;
   }

/**
 * @param actualSocket The actualSocket to set.
 */
   protected void setActualSocket(Socket actualSocket)
   {
      this.actualSocket = actualSocket;
   }

// in socket API section
///**
// * @return Returns the bound.
// */
//   public boolean isBound()
//   {
//      return bound;
//   }

/**
 * @param bound The bound to set.
 */
   protected void setBound(boolean bound)
   {
      this.bound = bound;
   }

/**
 * @param closed The closed to set.
 */
   protected void setClosed(boolean closed)
   {
      this.closed = closed;
   }

/**
 * @param connected The connected to set.
 */
   protected void setConnected(boolean connected)
   {
      this.connected = connected;
   }

/**
 * @param inputShutdown The inputShutdown to set.
 */
   protected void setInputShutdown(boolean inputShutdown)
   {
      this.inputShutdown = inputShutdown;
   }

/**
 * @param inputStream The inputStream to set.
 */
   protected void setInputStream(MultiplexingInputStream inputStream)
   {
      this.inputStream = inputStream;
   }

/**
 * @param localSocketId The localSocketId to set.
 */
   protected void setLocalSocketId(SocketId localSocketId)
   {
      this.localSocketId = localSocketId;
   }

/**
 *
 * @return
 */
   protected MultiplexingManager getManager()
   {
      return manager;
   }

/**
 * @param manager The manager to set.
 */
   protected void setManager(MultiplexingManager manager)
   {
      this.manager = manager;
   }

/**
 * @param outputShutdown The outputShutdown to set.
 */
   protected void setOutputShutdown(boolean outputShutdown)
   {
      this.outputShutdown = outputShutdown;
   }

/**
 * @param outputStream The outputStream to set.
 */
   protected void setOutputStream(MultiplexingOutputStream outputStream)
   {
      this.outputStream = outputStream;
   }

/**
 * @return Returns the protocol.
 */
   protected Protocol getProtocol()
   {
      return protocol;
   }

/**
 * @param protocol The protocol to set.
 */
   protected void setProtocol(Protocol protocol)
   {
      this.protocol = protocol;
   }

/**
 *
 * @return
 */
   protected boolean hasReceivedDisconnectMessage()
   {
      return receivedDisconnectMessage;
   }

/**
 *
 * @param receivedDisconnectMessage
 */
   protected void setReceivedDisconnectMessage(boolean receivedDisconnectMessage)
   {
      this.receivedDisconnectMessage = receivedDisconnectMessage;
   }

/**
 * @param remoteSocketId The remoteSocketId to set.
 */
   protected void setRemoteSocketId(SocketId remoteSocketId)
   {
      this.remoteSocketId = remoteSocketId;
   }



//////////////////////////////////////////////////////////////////////////////////////////////////
///                                  Other protected methods                                  '///
//////////////////////////////////////////////////////////////////////////////////////////////////


/**
 *
 */
   protected void doClose()
   {
      if (debug) log.debug("doClose()" + this.localSocketId.getPort());
      try
      {
//         if (connected && !receivedDisconnectMessage)
//            protocol.disconnect(remoteSocketId);

         // This VirtualSocket might have been unregistered when connect() failed.
         if (manager.isSocketRegistered(getLocalSocketId()))
            manager.unRegisterSocket(this);

         if (debug) log.debug("virtual socket closed on port: " + remoteSocketId.getPort());
      }
      catch (Exception e)
      {
         log.error("error closing socket: " + this);
         log.error(e);
      }
   }



/**
 *
 * @throws IOException
 */
   protected void handleRemoteOutputShutDown() throws IOException
   {
      // already closed ?
      try
      {
         inputStream.handleRemoteShutdown();
      }
      catch (NullPointerException ignored)
      {
      }
   }


   /**
    *
    * @throws IOException
    */
   protected void handleRemoteDisconnect() throws IOException
   {
      if (isClosed())
         return;

      if (debug) log.debug("remote virtual socket disconnecting: local port: " + getLocalVirtualPort());
      receivedDisconnectMessage = true;

      // already closed ?
      if (inputStream != null)
         inputStream.handleRemoteShutdown();

      // already closed ?
      if (outputStream != null)
      {
         outputStream.flush();
         outputStream.handleRemoteDisconnect();
      }

      MultiplexingManager.addToPendingActions(new PendingRemoteDisconnect(this));

      log.debug("handleRemoteDisconnect(): done.");
      // TODO
      //     connected = false;
      //     inputShutdown = true;
      //     outputShutdown = true;
   }


   /**
    * Indicate error condition on actual connection.
    */
   protected void notifyOfException()
   {
      functional = false;
   }

   protected class PendingRemoteDisconnect extends PendingAction
   {
      public PendingRemoteDisconnect(Object o)
      {
         super(o);
      }

      void doAction()
      {
         VirtualSocket vs = (VirtualSocket) o;
         Set disconnectListeners = vs.disconnectListeners;

         Iterator it = disconnectListeners.iterator();
         while (it.hasNext())
            ((DisconnectListener) it.next()).notifyDisconnected(vs);
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
         ((VirtualSocket)o).doClose();
      }
   }
}