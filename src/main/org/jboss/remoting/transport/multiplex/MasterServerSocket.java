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
 * Created on Jul 24, 2005
 */

package org.jboss.remoting.transport.multiplex;

import org.jboss.logging.Logger;

import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.channels.ServerSocketChannel;
import java.util.HashMap;
import java.util.Map;


/**
 * <code>MasterServerSocket</code> is one of the two implementations of a server socket in the
 * Multiplex project.  For each socket created in the <code>accept()</code> method, it builds
 * a virtual socket group.  For more details, see the Multiplex documentation on
 * the labs.jboss.org web site.
 * 
 * <p>
 * Most of the methods in <code>MasterServerSocket</code> override those in its parent class,
 * <code>java.net.ServerSocket</code>.  For method descriptions, see the <code>ServerSocket</code> javadoc.
 * 
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class MasterServerSocket extends ServerSocket implements Serializable
{
   private static final Logger log = Logger.getLogger(MasterServerSocket.class);
   private Map configuration = new HashMap();
   private ServerSocket ss;
   private ServerSocketChannel ssc;
   private static final long serialVersionUID = 402293949935889044L;

   /**
 * @throws java.io.IOException
 */
   public MasterServerSocket() throws IOException
   {
      this(true);
   }
   
/**
 * @param port
 * @throws java.io.IOException
 */
   public MasterServerSocket(int port) throws IOException
   {
      this(true, port);
   }
   
/**
 * @param port
 * @param backlog
 * @throws java.io.IOException
 */
   public MasterServerSocket(int port, int backlog) throws IOException
   {
      this(true, port, backlog);
   }
   
/**
 * @param port
 * @param backlog
 * @param bindAddr
 * @throws java.io.IOException
 */
   public MasterServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException
   {
      this(true, port, backlog, bindAddr);
   }
   
/**
 * @throws java.io.IOException
 */
   public MasterServerSocket(boolean nio) throws IOException
   {
      if (nio)
      {
         ssc = ServerSocketChannel.open();
         ss = ssc.socket();
      }
      else
      {
         ss = new ServerSocket();
      }
   }
   
/**
 * @param port
 * @throws java.io.IOException
 */
   public MasterServerSocket(boolean nio, int port) throws IOException
   {
      if (nio)
      {
         ssc = ServerSocketChannel.open();
         ss = ssc.socket();
         ss.bind(new InetSocketAddress(port));
      }
      else
      {
         ss = new ServerSocket(port);
      }
   }
   
/**
 * @param port
 * @param backlog
 * @throws java.io.IOException
 */
   public MasterServerSocket(boolean nio, int port, int backlog) throws IOException
   {
      if (nio)
      {
         ssc = ServerSocketChannel.open();
         ss = ssc.socket();
         ss.bind(new InetSocketAddress(port), backlog);
      }
      else
      {
         ss = new ServerSocket(port, backlog);
      }
   }
   
/**
 * @param port
 * @param backlog
 * @param bindAddr
 * @throws java.io.IOException
 */
   public MasterServerSocket(boolean nio, int port, int backlog, InetAddress bindAddr) throws IOException
   {
      if (nio)
      {
         ssc = ServerSocketChannel.open();
         ss = ssc.socket();
         ss.bind(new InetSocketAddress(bindAddr, port), backlog);
      }
      else
      {
         ss = new ServerSocket(port, backlog, bindAddr);
      }
   }
   
   
/**
 * 
 * Create a new <code>MasterServerSocket</code>. 
 * If <code>Map</code> conf contains key <code>Multiplex.SERVER_SOCKET_FACTORY</code>, will use it.
 * Otherwise, will create an NIO <code>ServerSocket</code>.
 * 
 * @param port
 * @param backlog
 * @param bindAddr
 * @param conf
 * @throws IOException
 */
   public MasterServerSocket(int port, int backlog, InetAddress bindAddr, Map conf) throws IOException
   {
      if (conf != null)
         configuration.putAll((conf));
      
      if (conf == null || conf.get(Multiplex.SERVER_SOCKET_FACTORY) == null)
      {
         ssc = ServerSocketChannel.open();
         ss = ssc.socket();
         ss.bind(new InetSocketAddress(bindAddr, port), backlog);
      }
      else
      {
         Object obj = conf.get(Multiplex.SERVER_SOCKET_FACTORY);
         if (!(obj instanceof ServerSocketFactory))
         {
            String msg = "configuration map contains invalid entry for Multiplex.SERVER_SOCKET_FACTORY: " + obj;
            log.error(msg);
            throw new IOException(msg);
         }
         ServerSocketFactory ssf = (ServerSocketFactory) obj;
         ss = ssf.createServerSocket(port, backlog, bindAddr);
      }
   }
   
   
/**
 * @param ssf
 * @throws IOException
 */
   public MasterServerSocket(ServerSocketFactory ssf) throws IOException
   {
      ss = ssf.createServerSocket();
   }
   
   
/**
 * @param ssf
 * @param port
 * @throws IOException
 */
   public MasterServerSocket(ServerSocketFactory ssf, int port) throws IOException
   {
      ss = ssf.createServerSocket(port);
   }
   
   
/**
 * @param ssf
 * @param port
 * @param backlog
 * @throws IOException
 */
   public MasterServerSocket(ServerSocketFactory ssf, int port, int backlog) throws IOException
   {
      ss = ssf.createServerSocket(port, backlog);
   }
   
   
/**
 * @param ssf
 * @param port
 * @param backlog
 * @param bindAddr
 * @throws IOException
 */
   public MasterServerSocket(ServerSocketFactory ssf, int port, int backlog, InetAddress bindAddr)
   throws IOException
   {
      ss = ssf.createServerSocket(port, backlog, bindAddr);
   }
   
   
   //////////////////////////////////////////////////////////////////////////////////////////////////
   ///                  The following methods are required of any ServerSocket                   '///
   //////////////////////////////////////////////////////////////////////////////////////////////////
   
   
   /*
    ok: public Socket              accept() throws IOException;
    ok: public void                bind(SocketAddress endpoint) throws IOException;
    ok: public void                bind(SocketAddress endpoint, int backlog) throws IOException;
    ok: public void                close() throws IOException;
    ok: public ServerSocketChannel getChannel();
    ok: public InetAddress         getInetAddress();
    ok: public int                 getLocalPort();
    ok: public SocketAddress       getLocalSocketAddress();
    ok: public int                 getReceiveBufferSize() throws SocketException;
    ok: public boolean             getReuseAddress() throws SocketException;
    ok: public int                 getSoTimeout() throws IOException;
    ok: public boolean             isBound();
    ok: public boolean             isClosed();
    ok: public void                setReceiveBufferSize(int size) throws SocketException;
    ok: public void                setReuseAddress(boolean on) throws SocketException;
    ok: public void                setSoTimeout(int timeout) throws SocketException;
    ok: public String              toString();
    */

   
/**
 * See <code>java.net.ServerSocket</code> javadoc.
 */
   public Socket accept() throws IOException
   {
      long start = System.currentTimeMillis();
      int timeout = getSoTimeout();
      int savedTimeout = timeout;
      int timeLeft = 0;
      Socket socket = null;
      SocketTimeoutException savedException = null;
      
      while (true)
      {
         if (timeout > 0)
            if ((timeLeft = timeout - (int) (System.currentTimeMillis() - start)) <= 0)
               throw new SocketTimeoutException("Accept timed out");
         
         setSoTimeout(timeLeft);
         
         try
         {
            socket = ss.accept();
         }
         catch (SocketTimeoutException e)
         {
            // NIO ServerSocket doesn't set message.
            savedException = new SocketTimeoutException("Accept timed out");
            throw savedException;
         }
         finally
         {
            try
            {
               setSoTimeout(savedTimeout);
            }
            catch (Exception e) {}
            
            if (savedException != null)
               throw savedException;
         }
         
         MultiplexingManager manager = MultiplexingManager.getaManager(socket, configuration);
         MultiplexingInputStream is = null;
         Protocol protocol = null;
         SocketId clientPort = null;
         
         try
         {
            is = manager.getAnInputStream(SocketId.SERVER_SOCKET_ID, null);
            protocol = manager.getProtocol();
            
            if (timeout > 0)
               if ((timeLeft = timeout - (int) (System.currentTimeMillis() - start)) <= 0)
                  throw new SocketTimeoutException("Accept timed out");
            
            clientPort = protocol.acceptConnect(is, timeLeft);
         }
         catch (SocketTimeoutException e)
         {
            log.debug("i/o exception in MasterServerSocket.accept()");
            manager.decrementReferences();
            throw e;
         }
         catch (IOException e)
         {
            log.error("i/o exception in MasterServerSocket.accept()", e);
            manager.decrementReferences();
            throw e;
         }
         
         if (log.isDebugEnabled())
            log.debug("accept(): clientPort:  " + clientPort.getPort());
         
         // connection from independent VirtualServerSocket
         if (clientPort.getPort() < 0)
         {
            MultiplexingOutputStream os = new MultiplexingOutputStream(manager, SocketId.SERVER_SOCKET_CONNECT_ID);
            
            try
            {
               protocol.answerConnect(os, SocketId.SERVER_SOCKET_CONNECT_PORT);
            }
            catch (IOException e)
            {
               // If this connect timed out at the other end, there may not be an OutputStream to write to.
               log.error("unable to respond to connect request");
               manager.decrementReferences();
               
               if (e instanceof SocketTimeoutException)
                  throw new SocketTimeoutException("Accept timed out");
               
               throw e;
            }
            
            // Keept trying to accept a request for a VirtualSocket connection.
            continue;
         }
         
         VirtualSocket virtualSocket = null;
         
         try
         {
            virtualSocket = new VirtualSocket(manager, clientPort, configuration);
         }
         catch (IOException e)
         {
            manager.decrementReferences();
            throw e;
         }
         
         int localPort = virtualSocket.getLocalVirtualPort();
         
         try
         {
            protocol.answerConnect((MultiplexingOutputStream)virtualSocket.getOutputStream(), localPort);
         }
         catch (IOException e)
         {
            // If this connect timed out at the other end, there may not be an OutputStream to write to.
            log.error("unable to respond to connect request");
            virtualSocket.close();
            throw e;
         }

         return virtualSocket;
      }
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc.
    */
   public void bind(SocketAddress endpoint) throws IOException
   {
      ss.bind(endpoint);
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc. 
    */
   public void bind(SocketAddress endpoint, int backlog) throws IOException
   {
      ss.bind(endpoint, backlog);
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc. 
    */
   public void close() throws IOException
   {
      log.debug("MasterServerSocket: closing");
      ss.close();
   }
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc.
    */
   public ServerSocketChannel getChannel()
   {
      return ss.getChannel();
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc. 
    */
   public InetAddress getInetAddress()
   {
      return ss.getInetAddress();
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc.
    */
   public int getLocalPort()
   {
      return ss.getLocalPort();
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc.
    */
   public SocketAddress getLocalSocketAddress()
   {
      return ss.getLocalSocketAddress();
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc.
    */
   public int getReceiveBufferSize() throws SocketException
   {
      return ss.getReceiveBufferSize();
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc.
    */
   public boolean getReuseAddress() throws SocketException
   {
      return ss.getReuseAddress();
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc.
    */
   public int getSoTimeout() throws IOException
   {
      return ss.getSoTimeout();
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc.
    */
   public boolean isBound()
   {
      return ss.isBound();
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc.
    */
   public boolean isClosed()
   {
      return ss.isClosed();
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc.
    */
   public void setReceiveBufferSize(int size) throws SocketException
   {
      ss.setReceiveBufferSize(size);
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc.
    */
   public void setReuseAddress(boolean on) throws SocketException
   {
      ss.setReuseAddress(on);
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc.
    */
   public void setSoTimeout(int timeout) throws SocketException
   {
      ss.setSoTimeout(timeout);
   }
   
   
   /**
    * See <code>java.net.ServerSocket</code> javadoc.
    */
   public String toString()
   {
      if (!isBound())
         return "MasterServerSocket[unbound]";
      
      return "MasterServerSocket[" + ss.toString()  + "]";
   }
   
   
//////////////////////////////////////////////////////////////////////////////////////////////////
// /              The following methods are specific to MasterServerSocket           '///
//////////////////////////////////////////////////////////////////////////////////////////////////
   
/**
 * Accepts a connection from a remote <code>VirtualServerSocket</code>.
 */
   public int acceptServerSocketConnection() throws IOException
   {     
      long start = System.currentTimeMillis();
      int timeout = getSoTimeout();
      int savedTimeout = timeout;
      int timeLeft = 0;
      
//    Socket socket = ss.accept();
      Socket socket = null;
      
      if (ssc == null)
         socket = ss.accept();
      else
         socket = ssc.accept().socket();
      
      MultiplexingManager manager = MultiplexingManager.getaManager(socket, configuration);
      manager.setCreatedForRemoteServerSocket();
      MultiplexingInputStream is = null;
      Protocol protocol = null;
      SocketId clientPort = null;
      
      try
      {
         is = manager.getAnInputStream(SocketId.SERVER_SOCKET_ID, null);
         protocol = manager.getProtocol();
         
         if (timeout > 0)
            if ((timeLeft = timeout - (int) (System.currentTimeMillis() - start)) <= 0)
               throw new SocketTimeoutException("Accept timed out");
         
         clientPort = protocol.acceptConnect(is, timeLeft);
      }
      catch (IOException e)
      {
         log.error("i/o exception in MasterServerSocket.acceptServerSocketConnection()", e);
         manager.decrementReferences();
         
         if (e instanceof SocketTimeoutException)
            throw new SocketTimeoutException("Accept timed out");
         
         throw e;
      }
      
      if (clientPort.getPort() != SocketId.SERVER_SOCKET_PORT)
      {
         manager.decrementReferences();
         String message = "received connect request not from a VirtualServerSocket";
         log.error(message);
         throw new IOException(message);
      }
      
      MultiplexingOutputStream os = new MultiplexingOutputStream(manager, SocketId.SERVER_SOCKET_CONNECT_ID);
      
      try
      {
         protocol.answerConnect(os, SocketId.SERVER_SOCKET_CONNECT_PORT);
      }
      catch (IOException e)
      {
         // If this connect timed out at the other end, there may not be an OutputStream to write to.
         log.error("unable to respond to connect request");
         manager.decrementReferences();
         throw e;
      }
      
      return manager.getSocket().getLocalPort();
   }
   
   
   public void setConfiguration(Map configuration)
   {
      this.configuration.putAll(configuration);
   }
}