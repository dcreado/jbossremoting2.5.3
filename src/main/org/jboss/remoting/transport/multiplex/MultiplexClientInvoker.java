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

package org.jboss.remoting.transport.multiplex;

import org.jboss.logging.Logger;
import org.jboss.remoting.ConnectionFailedException;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.remoting.transport.BidirectionalClientInvoker;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.multiplex.MultiplexServerInvoker.SocketGroupInfo;
import org.jboss.remoting.transport.multiplex.utility.AddressPair;
import org.jboss.remoting.transport.socket.ClientSocketWrapper;
import org.jboss.remoting.transport.socket.SocketClientInvoker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

/**
 * <code>MultiplexClientInvoker</code> is the client side of the Multiplex transport.
 * For more information, see Remoting documentation on labs.jboss.org.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class MultiplexClientInvoker extends SocketClientInvoker implements BidirectionalClientInvoker
{
   private static final Logger log = Logger.getLogger(MultiplexClientInvoker.class);
   private static final boolean isTraceEnabled = log.isTraceEnabled();
   
   private InetAddress connectAddress;
   private String connectHost;
   private int connectPort;
   private InetSocketAddress connectSocketAddress;
   private InetSocketAddress bindSocketAddress;
   private String socketGroupId;
   private MultiplexServerInvoker.SocketGroupInfo socketGroupInfo;
   private AddressPair addressPair;
   private boolean readyToRun;

   protected String clientSocketClassName = ClientSocketWrapper.class.getName();


/**
 * Create a new <code>MultiplexClientInvoker</code>.
 *
 * @param locator
 */
   public MultiplexClientInvoker(InvokerLocator locator) throws IOException
   {
      super(locator);
   }


/**
 * Create a new <code>MultiplexClientInvoker</code>.
 *
 * @param locator
 */
   public MultiplexClientInvoker(InvokerLocator locator, Map configuration) throws IOException
   {
      super(locator, configuration);
   }


   protected void handleConnect() throws ConnectionFailedException
   {
      try
      {
      log.debug("configuring MultiplexClientInvoker for: " + locator);
      super.handleConnect();

      this.connectAddress = InetAddress.getByName(locator.getHost());
      this.connectHost = connectAddress.getHostName();
      this.connectPort = locator.getPort();
      this.connectSocketAddress = new InetSocketAddress(connectAddress, connectPort);

      if (getSocketFactory() != null)
         configuration.put(Multiplex.SOCKET_FACTORY, getSocketFactory());

      Map parameters = configuration;

      if (parameters != null)
      {
         configureSocketGroupParameters(parameters);
      }
      }
      catch (Exception e)
      {
         throw new ConnectionFailedException(e.getMessage());
      }
   }


/**
 * Finishes the start up process, once adequate bind and connect information is
 * made available.  For more information, see the Multiplex subsystem documentation
 * at labs.jboss.org.
 */
   public void finishStart() throws IOException
   {
      if (socketGroupInfo != null &&
          socketGroupInfo.getBindAddress() != null &&
          bindSocketAddress == null)
      {
         InetAddress bindAddress = socketGroupInfo.getBindAddress();
         int bindPort = socketGroupInfo.getBindPort();
         bindSocketAddress = new InetSocketAddress(bindAddress, bindPort);
      }

      if (socketGroupInfo != null &&
          socketGroupInfo.getBindAddress() != null &&
          addressPair == null)
      {
         String bindHost = socketGroupInfo.getBindAddress().getHostName();
         int bindPort = socketGroupInfo.getBindPort();
         addressPair = new AddressPair(connectHost, connectPort, bindHost, bindPort);
      }

      readyToRun = true;
   }

   public InvokerLocator getCallbackLocator(Map metadata)
   {
      Socket socket = socketGroupInfo.getPrimingSocket();
      metadata.put(Multiplex.MULTIPLEX_CONNECT_HOST,
                   socket.getInetAddress().getHostAddress());
      metadata.put(Multiplex.MULTIPLEX_CONNECT_PORT,
                   Integer.toString(socket.getPort()));
      metadata.put(Multiplex.MULTIPLEX_BIND_HOST,
            socket.getInetAddress().getHostAddress());
      metadata.put(Multiplex.MULTIPLEX_BIND_PORT,
            Integer.toString(socket.getPort()));
      InvokerLocator locator = new InvokerLocator("multiplex",
                                                  socket.getLocalAddress().getHostAddress(),
                                                  socket.getLocalPort(),
                                                  null,
                                                  metadata);
      return locator;
   }

/**
 * @param parameters
 * @throws IOException
 */
   protected void configureSocketGroupParameters(Map parameters) throws IOException
   {
      String bindHost;
      String bindPortString;
      int bindPort = -1;
      InetAddress bindAddress = null;
      socketGroupId = (String) parameters.get(Multiplex.CLIENT_MULTIPLEX_ID);
      log.debug("socketGroupId: " + socketGroupId);

      synchronized (MultiplexServerInvoker.SocketGroupInfo.class)
      {
         if (socketGroupId != null)
            socketGroupInfo = (SocketGroupInfo) MultiplexServerInvoker.getSocketGroupMap().get(socketGroupId);

         if (socketGroupInfo != null && socketGroupInfo.getServerInvoker() != null)
         {
            log.debug("client rule 1");

            // If we get here, it's because a MultiplexServerInvoker created a SocketGroupInfo with matching
            // group id.  We want to make sure that it didn't get a connect address or connect port different
            // than the ones passed in through the parameters map.
            InetAddress socketGroupConnectAddress = socketGroupInfo.getConnectAddress();
            int socketGroupConnectPort = socketGroupInfo.getConnectPort();

            if (socketGroupConnectAddress != null && !socketGroupConnectAddress.equals(connectAddress))
            {
               String message = "socket group connect address (" + socketGroupConnectAddress +
                                ") does not match connect address (" + connectAddress + ")";
               log.error(message);
               throw new IOException(message);
            }

            if (socketGroupConnectPort > 0 && socketGroupConnectPort != connectPort)
            {
               String message = "socket group connect port (" + socketGroupConnectPort +
                                ") does not match connect port (" + connectPort + ")";
               log.error(message);
               throw new IOException(message);
            }

            bindAddress = socketGroupInfo.getBindAddress();
            bindPort = socketGroupInfo.getBindPort();
            bindSocketAddress = new InetSocketAddress(bindAddress, bindPort);

            if (socketGroupInfo.getPrimingSocket() == null)
            {
               String connectHost = connectAddress.getHostName();
               MultiplexServerInvoker.createPrimingSocket(socketGroupInfo, connectHost, connectPort,
                                                          bindAddress, bindPort,
                                                          configuration, timeout);
            }

            socketGroupInfo.setConnectAddress(connectAddress);
            socketGroupInfo.setConnectPort(connectPort);
            socketGroupInfo.addClientInvoker(this);

            // We got socketGroupInfo by socketGroupId.  Make sure it is also stored by AddressPair.
            bindHost = bindAddress.getHostName();
            addressPair = new AddressPair(connectHost, connectPort, bindHost, bindPort);
            MultiplexServerInvoker.getAddressPairMap().put(addressPair, socketGroupInfo);

            MultiplexServerInvoker serverInvoker = socketGroupInfo.getServerInvoker();
            if (serverInvoker != null)
               serverInvoker.finishStart();

            finishStart();
            return;
         }

         bindHost = (String) parameters.get(Multiplex.MULTIPLEX_BIND_HOST);
         bindPortString = (String) parameters.get(Multiplex.MULTIPLEX_BIND_PORT);

         if (bindHost != null && bindPortString == null)
         {
            bindPortString = "0";
         }

         if (bindHost == null && bindPortString != null)
         {
            bindHost = "localhost";
         }

         if (bindHost != null)
         {
            log.debug("client rule 2");

            try
            {
               bindPort = Integer.parseInt(bindPortString);
            }
            catch (NumberFormatException e)
            {
               throw new IOException("number format error for bindPort: " + bindPortString);
            }

            if (bindPort != 0)
            {
               bindSocketAddress = new InetSocketAddress(bindHost, bindPort);
               addressPair = new AddressPair(connectHost, connectPort, bindHost, bindPort);
               socketGroupInfo = (SocketGroupInfo) MultiplexServerInvoker.getAddressPairMap().get(addressPair);
            }

            // If socketGroupInfo exists, it's because it was created, along with a priming socket, by a
            // MultiplexServerInvoker.  Note that we don't look for a match if the bind port was
            // anonymous.
            if (socketGroupInfo != null)
            {
               socketGroupInfo.setConnectAddress(connectAddress);
               socketGroupInfo.setConnectPort(connectPort);
               socketGroupInfo.addClientInvoker(this);

               // We got socketGroupInfo by AddressPair.  Make sure it is stored by socketGroupId, if we have one.
               if (socketGroupId != null)
               {
                  String socketGroupSocketGroupId = socketGroupInfo.getSocketGroupId();

                  if (socketGroupSocketGroupId != null && !socketGroupSocketGroupId.equals(socketGroupId))
                  {
                     String message = "socket group multiplexId (" + socketGroupSocketGroupId +
                                      ") does not match multiplexId (" + socketGroupId + ")";
                     log.error(message);
                     throw new IOException(message);
                  }

                  if (socketGroupSocketGroupId == null)
                  {
                     socketGroupInfo.setSocketGroupId(socketGroupId);
                     MultiplexServerInvoker.getSocketGroupMap().put(socketGroupId, socketGroupInfo);
                  }
               }

               finishStart();
               return;
            }

            if (bindPort == 0 && socketGroupId == null)
            {
               String message = "Can never be found by any MultiplexServerInvoker: " +
                                "bind port == 0 and socketGroupId == null";
               log.warn(message);
            }

            // Anonymous bind port
            if (bindPort == 0)
               bindPort = PortUtil.findFreePort(bindHost);

            socketGroupInfo = new SocketGroupInfo();
            socketGroupInfo.setConnectAddress(connectAddress);
            socketGroupInfo.setConnectPort(connectPort);
            socketGroupInfo.addClientInvoker(this);

            // Set bindAddress and bindPort to be able to test for inconsistencies with bind address
            // and bind port determined by companion MultiplexServerInvoker.
            bindAddress = InetAddress.getByName(bindHost);
            socketGroupInfo.setBindAddress(bindAddress);
            socketGroupInfo.setBindPort(bindPort);

            String connectHost = connectAddress.getHostName();
            MultiplexServerInvoker.createPrimingSocket(socketGroupInfo, connectHost, connectPort,
                                                       bindAddress, bindPort, configuration, timeout);
            MultiplexServerInvoker.getAddressPairMap().put(addressPair, socketGroupInfo);

            if (socketGroupId != null)
            {
               socketGroupInfo.setSocketGroupId(socketGroupId);
               MultiplexServerInvoker.getSocketGroupMap().put(socketGroupId, socketGroupInfo);
            }

            finishStart();
            return;
         }

         if (socketGroupId != null)
         {
            log.debug("client rule 3");

            if (socketGroupInfo == null)
            {
               socketGroupInfo = new SocketGroupInfo();
               socketGroupInfo.setSocketGroupId(socketGroupId);
               socketGroupInfo.setConnectAddress(connectAddress);
               socketGroupInfo.setConnectPort(connectPort);
               MultiplexServerInvoker.getSocketGroupMap().put(socketGroupId, socketGroupInfo);
            }

            socketGroupInfo.addClientInvoker(this);
            return;
         }

         log.debug("client rule 4");
         String connectHost = connectAddress.getHostName();
         socketGroupInfo = new SocketGroupInfo();
         MultiplexServerInvoker.createPrimingSocket(socketGroupInfo, connectHost, connectPort,
                                                    configuration, timeout);
         finishStart();
      }
   }

   /**
    * @param sessionId
    * @param invocation
    * @param marshaller
    * @return
    * @throws java.io.IOException
    * @throws org.jboss.remoting.ConnectionFailedException
    *
    */
   protected Object transport(String sessionId, Object invocation, Map metadata, Marshaller marshaller, UnMarshaller unmarshaller)
   throws IOException, ConnectionFailedException, ClassNotFoundException
   {
      log.debug("entering transport()");
      if (!readyToRun)
         throw new IOException("connection to server has not been made");

      return super.transport(sessionId, invocation, metadata, marshaller, unmarshaller);
   }


   /**
    * subclasses must implement this method to provide a hook to disconnect from the remote server, if this applies
    * to the specific transport. However, in some transport implementations, this may not make must difference since
    * the connection is not persistent among invocations, such as SOAP.  In these cases, the method should
    * silently return without any processing.
    */
   protected void handleDisconnect()
   {
      //TODO: -TME Should this be a no op or need to pool?

      log.debug("entering handleDisconnect()");
      super.handleDisconnect();

      synchronized (MultiplexServerInvoker.SocketGroupInfo.class)
      {
         if (socketGroupInfo != null)
         {
            socketGroupInfo.removeClientInvoker(this);

            if (socketGroupInfo.getClientInvokers().isEmpty() && socketGroupInfo.getServerInvoker() == null)
            {
               log.debug("invoker group shutting down: " + socketGroupInfo.getSocketGroupId());

               if (socketGroupInfo.getPrimingSocket() != null)
               {
                  log.debug("MultiplexClientInvoker: closing bind priming socket");

                  VirtualSocket ps = socketGroupInfo.getPrimingSocket();
                  if (ps != null)
                  {
                     try
                     {
                        // When the remote virtual MultiplexServerInvoker learns that the
                        // priming socket has closed, it will close its VirtualServerSocket,
                        // rendering unshareable the MultiplexingManager that underlies this
                        // socket group.  We mark it as unshareable immediately so that it will
                        // not be reused by any other socket group.
                        ps.getManager().unregisterShareable();
                        ps.close();
                     }
                     catch (IOException e)
                     {
                        log.error("Error closing bind priming socket during cleanup upon stopping", e);
                     }
                  }
               }

               socketGroupId = socketGroupInfo.getSocketGroupId();

               if (socketGroupId != null)
               {
                  MultiplexServerInvoker.getSocketGroupMap().remove(socketGroupId);
               }

               // addressPair is set in finishStart().
               if (addressPair != null)
               {
                  MultiplexServerInvoker.getAddressPairMap().remove(addressPair);
               }
            }

            socketGroupInfo = null;  // Prevent from occurring a second time in Finalizer thread.
         }
      }
   }


/**
 * @return
 */
   protected InetSocketAddress getBindSocketAddress()
   {
      return bindSocketAddress;
   }


/**
 * @return
 */
   protected InetSocketAddress getConnectSocketAddress()
   {
      return connectSocketAddress;
   }


   /**
    * Each implementation of the remote client invoker should have
    * a default data type that is uses in the case it is not specified
    * in the invoker locator uri.
    *
    * @return
    */
   protected String getDefaultDataType()
   {
      return SerializableMarshaller.DATATYPE;
   }


   protected Socket createSocket(String address, int port, int timeout) throws IOException
   {
      log.debug("MultiplexClientInvoker.createSocket()");
      
      if (timeout < 0)
      {
         timeout = getTimeout();
         if (timeout < 0)
            timeout = 0;
      }

      // If connection has been broken, try to connect to new server.
      if (socketGroupInfo != null && socketGroupInfo.getPrimingSocket() != null)
      {
         VirtualSocket primingSocket = socketGroupInfo.getPrimingSocket();
         if (!primingSocket.isFunctional() || primingSocket.hasReceivedDisconnectMessage())
         {
            log.info("Current server is inaccessible.  Will try to connect to new server");
            primingSocket.close();

            // Get new priming socket.
            if (bindSocketAddress != null)
            {
               InetAddress bindAddress = bindSocketAddress.getAddress();
               int bindPort = PortUtil.findFreePort(bindSocketAddress.getHostName());
               socketGroupInfo.setBindPort(bindPort);
               bindSocketAddress = new InetSocketAddress(bindAddress, bindPort);
               MultiplexServerInvoker.createPrimingSocket(socketGroupInfo,
                                                          connectHost, connectPort,
                                                          bindAddress, bindPort,
                                                          configuration, timeout);
            }
            else
               MultiplexServerInvoker.createPrimingSocket(socketGroupInfo,
                                                          connectHost, connectPort,
                                                          configuration, port);

            MultiplexServerInvoker serverInvoker = socketGroupInfo.getServerInvoker();
            if (serverInvoker != null)
            {
               try
               {
                  // Restart callback server invoker with new server socket.
                  serverInvoker.stop();
                  socketGroupInfo.setServerInvoker(null);
                  serverInvoker.resetLocator(bindSocketAddress.getPort());
                  serverInvoker.refreshServerSocket();
                  serverInvoker.setup();
                  serverInvoker.start();
               }
               catch (Exception e)
               {
                  log.error(e.getMessage(), e);
               }
            }

            VirtualSocket socket = new VirtualSocket(configuration);
            for (int i = 0; i < 3; i++)
            {
               try
               {
                  socket.connect(connectSocketAddress, bindSocketAddress, timeout);
                  return socket;
               }
               catch (Exception e)
               {
                  try
                  {
                     Thread.sleep(500);
                  }
                  catch (InterruptedException e1)
                  {
                  }
               }
            }
         }
      }

      VirtualSocket socket = new VirtualSocket(configuration);
      socket.connect(connectSocketAddress, bindSocketAddress, timeout);
      return socket;
   }
}