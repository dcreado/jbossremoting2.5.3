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

import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ServerSocketFactory;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.multiplex.utility.AddressPair;
import org.jboss.remoting.transport.socket.SocketServerInvoker;
import org.jboss.remoting.util.socket.HandshakeRepeater;


/**
 * <code>MultiplexServerInvoker</code> is the server side of the Multiplex transport.
 * For more information, see Remoting documentation on labs.jboss.org.
 * 
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class MultiplexServerInvoker extends SocketServerInvoker
implements Runnable, VirtualSocket.DisconnectListener
{
   protected static final Logger log = Logger.getLogger(MultiplexServerInvoker.class);
   private static boolean trace = log.isTraceEnabled();
   
   private static Map socketGroupMap = new HashMap();
   private static Map addressPairMap = new HashMap();
   private static HandshakeCompletedEvent handshakeCompletedEvent;

   private boolean isVirtual = false;
   private Map virtualServerInvokers;
   private Socket connectPrimingSocket;
   private SocketGroupInfo socketGroupInfo;
   private AddressPair addressPair;
   private String bindHost;
   private int bindPort;
   private int originalBindPort;
   private InetAddress bindAddress;
   private InetSocketAddress connectSocketAddress;
   private boolean readyToStart = true; 
   private boolean needsSocketGroupConfiguration = true;
   private boolean cleanedUp;
   private boolean hasMaster;
   private int errorCount;
   
   private ServerSocket serverSocket;
   
   /////////////////////////////////////////////////////////////////////////////////////
   //                    configurable Multiplex parameters                            //
   /////////////////////////////////////////////////////////////////////////////////////
   /*
    * The following parameters may be set in any of four ways:
    * 
    * 1. They may be appended to an <code>InvokerLocator</code> passed to a
    *    <code>Connector</code> constructor.
    * 2. They may be included in a configuration <code>Map</code> passed to a
    *    <code>Connector</code> constructor.
    * 3. They may be described in a <config> XML element passed to
    *    <code>Connector.setConfiguration()</code>.
    * 4. In some cases, a <code>MultiplexServerInvoker</code> setter methods may be invoked.
    * 
    * Of those of the following parameters destined for <code>MultiplexServerInvoker</code>,
    * there are two categories.
    * 
    * 1. <code>serverMultiplexId</code>, <code>multiplexConnectHost</code>, and
    *    <code>multiplexConnectPort</code> are used to
    *    match up <code>MultiplexClientInvoker</code>s and virtual
    *    <code>MultiplexServerInvokers</code> so that
    *    they share an underlying socket connection.  Depending on the way in which
    *    the information is provided (see Remoting documentation), the connection may
    *    be created any time during or after the call to <code>Connector.create()</code>. Note,
    *    however, that if a callback <code>MultiplexServerInvoker</code> is created with just a 
    *    <code>serverMultiplexId</code> parameter (server rule 3 in the Remoting documentation),
    *    then calling <code>setMultiplexConnectHost()</code> and
    *    <code>setMultiplexConnectPort()</code> will
    *    not trigger the creation of a connection.  Moreover, when a <code>Client</code> comes
    *    along, the connect information supplied by the <code>Client</code> will be used, so there
    *    is no point to having setter methods for these parameters.
    *    
    * 2. <code>maxAcceptErrors</code> is used in the
    *    <code>MultiplexServerInvoker</code> <code>accept()</code> loop, and it
    *    may be changed at any time by calling <code>setMaxAcceptErrors()</code>. 
    * 
    * Those of the following parameters which are destined for the <code>MultiplexingManager</code>,
    * <code>InputMultiplexor</code>, and <code>OutputMultiplexor</code> classes are
    * passed to them by way of a configuration <code>Map</code> passed to
    * <code>VirtualSocket</code> and <code>VirtualServerSocket</code> constructors.
    * 
    * A <code>VirtualServerSocket</code> is created when a server side master
    * <code>MultiplexServerInvoker</code>
    * accepts a connection request generated by the creation of a priming socket on a client.  
    * Since this can happen any time after <code>Connector.start()</code> is created, the values of
    * these parameters can be changed by calling their respective setter methods any
    * time before <code>Connector.start()</code> is called.
    * 
    * A <code>VirtualSocket</code> is created when a client side
    * <code>MultiplexClientInvoker</code> or callback
    * <code>MultiplexServerInvoker</code> opens a priming socket, and this happens when
    * <code>Connector.create()</code> is called.  Therefore, the values of these parameters can be 
    * changed by calling their respective setter methods any time before
    * <code>Connector.create()</code> is called.
    */
   // MultiplexingManager:
   private int staticThreadsMonitorPeriod;
   private int shutdownRequestTimeout;
   private int shutdownRefusalsMaximum;
   private int shutdownMonitorPeriod;
   
   // InputMultiplexor:
   private int inputBufferSize;
   private int inputMaxErrors;

   // OutputMultiplexor: 
   private int outputMessagePoolSize;
   private int outputMessageSize;
   private int outputMaxChunkSize;
   private int outputMaxTimeSlice;
   private int outputMaxDataSlice;

   // MultiplexServerInvoker
   private int     maxAcceptErrors;
   private String  serverMultiplexId;
   private String  multiplexConnectHost;
   private int     multiplexConnectPort;
   private boolean multiplexConnectPortIsSet;  // to check for missing configuration information

   public static Map getAddressPairMap()
   {
      return addressPairMap;
   }
   
   public static Map getSocketGroupMap()
   {
      return socketGroupMap;
   }
   
   
/**
 * 
 * Create a new <code>MultiplexServerInvoker</code>.
 * 
 * @param locator
 */
   public MultiplexServerInvoker(InvokerLocator locator)
   {
      super(locator);
//      virtualServerInvokers = Collections.synchronizedMap(new HashMap());
      virtualServerInvokers = new HashMap();
   }

   
/**
 * Create a new <code>MultiplexServerInvoker</code>.
 */
   public MultiplexServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
//      virtualServerInvokers = Collections.synchronizedMap(new HashMap());
      virtualServerInvokers = new HashMap();
   }


   /**
    * Create a new <code>MultiplexServerInvoker</code>.
    */
   protected MultiplexServerInvoker(InvokerLocator locator, Map configuration,
                                    List serverSockets, Socket socket,
                                    Map virtualServerInvokers)
   {
      super(locator, configuration);
      this.serverSockets = serverSockets;
      serverSocket = (ServerSocket) serverSockets.get(0);
      connectPrimingSocket = socket;
      this.virtualServerInvokers = virtualServerInvokers;
      isVirtual = true;
      needsSocketGroupConfiguration = false;
      ((VirtualSocket) connectPrimingSocket).addDisconnectListener(this);
      
      try
      {
         getParameters();
      }
      catch (Exception e)
      {
         log.error(e);
      }
   }
   

   /**
    * Each implementation of the remote client invoker should have
    * a default data type that is uses in the case it is not specified
    * in the invoker locator uri.
    */
   protected String getDefaultDataType()
   {
      return SerializableMarshaller.DATATYPE;
   }

   //TODO: -TME Need to check on synchronization after initial hook up
   public void start() throws IOException
   { 
      if (readyToStart)
         finishStart();
   }
   
   
   public void run()
   {
      if(trace)
      {
         log.trace("Started execution of method run");
      }
      ServerSocketRefresh thread=new ServerSocketRefresh();
      thread.setDaemon(true);
      thread.start();
      
      try
      {
         while(running)
         {
            try
            {
               if(trace)
               {
                  log.trace("Socket is going to be accepted");
               }
               thread.release(); //goes on if serversocket refresh is completed
               Socket socket = serverSocket.accept();
               if(trace)
               {
                  log.trace("Accepted: " + socket);
               }
               processInvocation(socket);
            }
            catch (SocketException e)
            {
               if ("Socket is closed".equals(e.getMessage())
                     || "Socket closed".equals(e.getMessage()))
               {
                  log.info("socket is closed: stopping thread");
                  // If this invoker was started by a Connector, let the Connector stop it.
                  if (hasMaster) 
                     stop();
                  return;
               }
               else if (++errorCount > maxAcceptErrors)
               {
                  log.error("maximum accept errors exceeded: stopping thread");
                  // If this invoker was started by a Connector, let the Connector stop it.
                  if (hasMaster)
                     stop();
                  return;
               }
               else
               {
                  log.info(e);
               }
            }
            catch (SocketTimeoutException e)
            {
               if(running)
               {
                  // If remote MultiplexClientInvoker and optional callback MultiplexServerInvoker
                  // have shutdown, it's safe to stop.
                  if (connectPrimingSocket != null && ((VirtualSocket)connectPrimingSocket).hasReceivedDisconnectMessage())
                  {
                     log.info("Client has closed: stopping thread");
                     // If this invoker was started by a Connector, let the Connector stop it.
                     if (hasMaster)
                        stop();
                     return;
                  }
               }
            }
            catch (javax.net.ssl.SSLHandshakeException e)
            {
               log.info("SSLHandshakeException", e);
            }
            catch(Throwable ex)
            {
               if(running)
               {
                  log.error("Failed to accept socket connection", ex);
                  if (++errorCount > maxAcceptErrors)
                  {
                     log.error("maximum accept errors exceeded: stopping");
                     // If this invoker was started by a Connector, let the Connector stop it.
                     if (hasMaster)
                        stop();
                     return;
                  }
               }
               else
               {
                  log.info(ex);
               }
            }
         }
      }
      finally
      {
         thread.interrupt();
      }
   }
   
   
   public boolean isSafeToShutdown()
   {
      return (connectPrimingSocket == null || ((VirtualSocket) connectPrimingSocket).hasReceivedDisconnectMessage());
   }
   
   
   public void notifyDisconnected(VirtualSocket virtualSocket)
   {
      if (virtualSocket != connectPrimingSocket)
      {
         log.error("notified about disconnection of unrecognized virtual socket");
         return;
      }
      
      log.debug("remote peer socket has closed: stopping");
      stop();
   }
   
   
   public void stop()
   {
      // If running == false, super.stop() will not call cleanup().
      // However, MultiplexServerInvoker could have stuff to clean up
      // (socket group information) even if it didn't start.
      if (!running)
         cleanup();
    
      super.stop();
   }
   
   
   public String toString()
   {
      if (isVirtual)
      {
         VirtualServerSocket vss = (VirtualServerSocket) serverSocket;
         if (vss != null)
            return "MultiplexServerInvoker[virtual:"
               + vss.getInetAddress() + ":" + vss.getLocalPort()
               + "->"
               + vss.getRemoteAddress() + ":" + vss.getRemotePort()
               + "]";
         else
            return "MultiplexServerInvoker[virtual]";
      }
      else
         if (serverSocket != null)
            return "MultiplexServerInvoker[master:"
               + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort()
               + "]";
         else
            return "MultiplexServerInvoker[master]";
   }
   
   
   protected void setup() throws Exception
   {
      originalBindPort = this.getLocator().getPort();
      super.setup();
      getParameters();
      setBindingInfo();
      
//      socketFactory = createSocketFactory(configuration);
//      if (socketFactory != null)
//         configuration.put(Multiplex.SOCKET_FACTORY, socketFactory);
      
      if (!configuration.isEmpty())
      {
         if (needsSocketGroupConfiguration)
         {
            try
            {
               configureSocketGroupParameters(configuration);
            }
            catch (IOException e)
            {
               log.error("error configuring socket group parameters", e);
               cleanup();
               throw e;
            }
         }
      }
   }
   
   
   /**
    * Finishes start up process when suitable bind and connect information is available.
    * For more information, see the Multiplex subsystem documentation at labs.jboss.org.
    */
   protected void finishStart() throws IOException
   {
      log.debug("entering finishStart()");
      
      if (isStarted())
         return;
      
      if (socketGroupInfo != null && connectSocketAddress == null)
      {
         InetAddress connectAddress = socketGroupInfo.getConnectAddress();
         int connectPort = socketGroupInfo.getConnectPort();
         connectSocketAddress = new InetSocketAddress(connectAddress, connectPort);
      }
      
      if (socketGroupInfo != null && addressPair == null)
      {
         String connectHost = socketGroupInfo.getConnectAddress().getHostName();
         int connectPort = socketGroupInfo.getConnectPort();
         addressPair = new AddressPair(connectHost, connectPort, bindHost, bindPort);
      }
      
      try
      {
         super.start();
      }
      catch(IOException e)
      {
         log.error("Error starting MultiplexServerInvoker.", e);
         cleanup();
      }
      
      if (running)
         log.debug("MultiplexServerInvoker started.");
     }
   
   
   
   /**
    * Called by MultiplexClientInvoker.createSocket() when it finds connection is
    * broken and binds virtual socket group to new bind port.
    * <p>
    * @param bindPort
    */
   protected void resetLocator(int bindPort)
   {
      this.bindPort = bindPort;
      final InvokerLocator newLocator = new InvokerLocator(locator.getProtocol(),
                                                           locator.getHost(),
                                                           bindPort,
                                                           locator.getPath(),
                                                           locator.getParameters());
      
      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            InvokerRegistry.updateServerInvokerLocator(locator, newLocator);
            return null;
         }
      });
      
      locator = newLocator;
   }
   
   
   protected void configureSocketGroupParameters(Map parameters) throws IOException
   {
      log.debug("entering configureSocketGroupParameters()");
      log.debug(locator);

      synchronized (SocketGroupInfo.class)
      {
         if (serverMultiplexId != null)
         {
            socketGroupInfo = (SocketGroupInfo) getSocketGroupMap().get(serverMultiplexId);
            if (socketGroupInfo != null)
            {
               rule1();
               return;
            }
         }
         
         if (multiplexConnectHost != null && !this.multiplexConnectPortIsSet)
            throw new IOException("multiplexConnectHost != null and multiplexConnectPort is not set");
         
         if (multiplexConnectHost == null && this.multiplexConnectPortIsSet)
            throw new IOException("multiplexConnectHost == null and multiplexConnectPort is set");
         
         // server rule 2.
         if (multiplexConnectHost != null)
         {
            rule2(multiplexConnectHost, multiplexConnectPort);
            return;
         }
         
         // server rule 3.
         if (serverMultiplexId != null)
         {
            rule3();
            return;
         }
         
         // server rule 4.
         rule4();
      }
   }
   
   
   protected static void createPrimingSocket(SocketGroupInfo socketGroupInfo,
                                             String connectHost, int connectPort,
                                             Map configuration, int timeout)
   throws IOException
   {
      createPrimingSocket(socketGroupInfo, connectHost, connectPort, null, -1, configuration, timeout);
   }
   
   
   protected static void createPrimingSocket(SocketGroupInfo socketGroupInfo,
                                             String connectHost, int connectPort,
                                             InetAddress bindAddress, int bindPort,
                                             Map configuration, int timeout)
   throws IOException
   {  
      log.debug("entering createPrimingSocket()");
      
      boolean needed = true;
      InetSocketAddress csa = new InetSocketAddress(connectHost, connectPort);
      InetSocketAddress bsa = null;
      
      if (bindAddress != null)
      {
         bsa = new InetSocketAddress(bindAddress, bindPort);
         needed = !MultiplexingManager.checkForShareableManagerByAddressPair(bsa, csa);
      }
      else
      {
         needed = !MultiplexingManager.checkForShareableManager(csa);
      }
      
      if (socketGroupInfo != null)
         socketGroupInfo.setPrimingSocketNeeded(needed);
      
      if (!needed)
      {
         log.debug("priming socket is not necessary");
         return;
      }
      
      // If the configuration Map has an SSL HandshakeCompletedListener, we register to
      // receive the HandshakeCompletedEvent with a HandshakeRepeater and, if the event
      // arrives within 60 seconds, we pass it on to the configured listener.  Otherwise,
      // HandshakeRepeater.waitForHandshake() will throw an SSLException. 
      Object obj = configuration.get(Client.HANDSHAKE_COMPLETED_LISTENER);
      HandshakeCompletedListener externalListener = null;
      HandshakeRepeater internalListener = null;
      if (obj != null && obj instanceof HandshakeCompletedListener)
      {
         externalListener = (HandshakeCompletedListener) obj;
         internalListener = new HandshakeRepeater(new InternalHandshakeListener());
         configuration.put(Multiplex.SSL_HANDSHAKE_LISTENER, internalListener);
      }
      
      VirtualSocket socket = new VirtualSocket(configuration);
      
      if (bindAddress != null)
         socket.connect(csa, bsa, timeout);
      else
         socket.connect(csa, timeout);
      
      MultiplexingManager manager = socket.getManager();
      
      if (externalListener != null)
      {
         if (manager.getHandshakeCompletedEvent() != null)
         {
            externalListener.handshakeCompleted(manager.getHandshakeCompletedEvent());
         }
         else
         {
            internalListener.waitForHandshake();
            externalListener.handshakeCompleted(handshakeCompletedEvent);
         }
      }
      
      if (!manager.waitForRemoteServerSocketRegistered())
         throw new IOException("error waiting for remote server socket to be registered");
      
      if (socketGroupInfo != null)
         socketGroupInfo.setPrimingSocket(socket);
      
      log.debug("created priming socket: " + socket.getLocalSocketId());
   }
   

   protected String getThreadName(int i)
   {
      String virtualTag = isVirtual ? "v" : "m";
      return "MultiplexServerInvoker#" + i + virtualTag + "-" + serverSocket.toString();
   }


   protected void processInvocation(Socket socket) throws Exception
   {
      if (isVirtual)
         super.processInvocation(socket);
      else
      {
         log.debug("creating VSS");
         ServerSocket ss = new VirtualServerSocket((VirtualSocket) socket, configuration);
         ss.setSoTimeout(getTimeout());
         List serverSockets = new ArrayList();
         serverSockets.add(ss);
         MultiplexServerInvoker si = new MultiplexServerInvoker(locator, configuration, serverSockets, socket, virtualServerInvokers);
         si.hasMaster = true;
         si.clientCallbackListener = clientCallbackListener;
         si.handlers = handlers;
         si.setMBeanServer(this.getMBeanServer());
         si.setServerSocketFactory(this.getServerSocketFactory());
         si.setSocketFactory(this.socketFactory);
         synchronized (virtualServerInvokers)
         {
            virtualServerInvokers.put(socket.getRemoteSocketAddress(), si);
         }
         si.connectionNotifier = connectionNotifier;
         si.create();
         si.start();
         log.debug("created virtual MultiplexServerInvoker: " + si);
      }
   }

  
   protected void cleanup()
   {  
      // If running == false, SocketServerInvoker doesn't want to call cleanup().
      if (running)
      {
         super.cleanup();
      }
      
      // If the Finalizer thread gets here after clean up has occurred, return.
      if (cleanedUp)
         return;
      
      cleanedUp = true;
      
      if (isVirtual)
      {
         if (connectPrimingSocket != null)
         {
            log.debug("connect priming != null");
            // If !virtualServerInvokers.containsKey(connectPrimingSocket.getRemoteSocketAddress()),
            // the master MultiplexServerInvoker might be iterating through virtualServerInvokers
            // and shutting them down.  This test avoids a NullPointerException.
            Object key = connectPrimingSocket.getRemoteSocketAddress();
            synchronized (virtualServerInvokers)
            {
               if (virtualServerInvokers.containsKey(key))
                  virtualServerInvokers.remove(key);
            }
            
            try
            {
               log.debug("MultiplexServerInvoker: closing connect priming socket");
               connectPrimingSocket.close();
            }
            catch (IOException e)
            {
               log.error("Error closing connect priming socket during cleanup upon stopping", e);
            }
         }
         else
         {
            log.debug("connect priming socket == null");
         }
         
         // Remove all callback handlers (if any ServerInvocationHandlers are registered).
         Iterator it = handlers.values().iterator();
         
         if (it.hasNext())
         {
            log.debug("removing callback handlers");
            ServerInvocationHandler defaultHandler = (ServerInvocationHandler) it.next();
            ServerInvocationHandler handler = null;
            ServerInvokerCallbackHandler callbackHandler = null;
            it = callbackHandlers.values().iterator();
            
            while (it.hasNext())
            {
               callbackHandler = (ServerInvokerCallbackHandler) it.next();
               String subsystem = callbackHandler.getSubsystem();
               
               if (subsystem == null)
                  handler = defaultHandler;
               else
                  handler = (ServerInvocationHandler) handlers.get(subsystem.toUpperCase());
               
               handler.removeListener(callbackHandler);
            }
         }
      }
      else
      {
//         Iterator it = virtualServerInvokers.values().iterator();
         Iterator it = null;
         synchronized (virtualServerInvokers)
         {
            it = new HashMap(virtualServerInvokers).values().iterator();
         }
         
         while (it.hasNext())
         {
            ServerInvoker serverInvoker = ((ServerInvoker) it.next());
            it.remove();
            serverInvoker.stop();
         }
      }
      
      if (socketGroupInfo != null)
      {
         synchronized (MultiplexServerInvoker.SocketGroupInfo.class)
         {
            socketGroupInfo.removeServerInvoker(this);
            VirtualSocket ps = null;
            
            if (socketGroupInfo.getClientInvokers().isEmpty())
            {
               log.debug("invoker group shutting down: " + socketGroupInfo.getSocketGroupId());
               
               if ((ps = socketGroupInfo.getPrimingSocket()) != null)
               {
                  // When the remote virtual MultiplexServerInvoker learns that the
                  // priming socket has closed, it will close its VirtualServerSocket,
                  // rendering unshareable the MultiplexingManager that underlies this
                  // socket group.  We mark it as unshareable immediately so that it will
                  // not be reused by any other socket group.
                  ps.getManager().unregisterShareable();
                  
                  log.debug("MultiplexServerInvoker: closing bind priming socket");
                  try
                  {
                     ps.close();
                  }
                  catch (IOException e)
                  {
                     log.error("Error closing bind priming socket during cleanup upon stopping", e);
                  }
               }
               
               serverMultiplexId = socketGroupInfo.getSocketGroupId();
               log.debug("serverMultiplexId: " + serverMultiplexId);
               if (serverMultiplexId != null)
               {
                  getSocketGroupMap().remove(serverMultiplexId);
                  log.debug("removed serverMultiplexId: " + serverMultiplexId);
                  log.debug("socketGroupInfo: " + getSocketGroupMap().get(serverMultiplexId));
               }
               
               // addressPair is set in finishStart().
               if (addressPair != null)
               {
                  getAddressPairMap().remove(addressPair);
               }
            }
         }
      }
   }


/**
 * In creating the server socket, <code>createServerSocket()</code> determines whether multiplexing
 * will be supported by this <code>ServerInvoker</code>. The determination is made according to the
 * presence or absence of certain parameters in the <code>ServerInvoker</code>'s locator.  In particular,
 * a <code>VirtualServerSocket</code>, which supports multiplexing, needs to connect to a
 * remote <code>MasterServerSocket</code> before it can begin to accept connection requests.
 * In order to know which <code>MasterServerSocket</code> to connect to,
 * it looks for parameters "connectHost" and "connectPort" in the locator.  The presence of these parameters
 * indicates that a <code>VirtualServerSocket</code> should be created, and their absence indicates that a
 * <code>MasterServerSocket</code>, which does not support multiplexing, should be created.
 *
 * @param bindPort
 * @param backlog
 * @param bindAddress
 * @return
 * @throws IOException
 */
   protected ServerSocket createServerSocket(int bindPort, int backlog, InetAddress bindAddress) throws IOException
//   private ServerSocket createServerSocket() throws IOException
   {
      // The following commented code represents an attempt to make an automatic determination as to whether
      // a VirtualServerSocket should be created.  The idea is to see if a ClientInvoker already
      // exists on the local port to which the new server socket wants to bind.  The existence of such a
      // ClientInvoker would indicate that multiplexing is desired.  However, it appears that a ClientInvoker
      // has no control over which local port(s) it uses.

      //    if (InvokerRegistry.isClientInvokerRegistered(getLocator()))
      //    {
      //       try
      //       {
      //          Invoker clientInvoker = InvokerRegistry.createClientInvoker(getLocator());
      //          InvokerLocator connectLocator = clientInvoker.getLocator();
      //          InetSocketAddress connectSocketAddress = new InetSocketAddress(connectLocator.getHost(), connectLocator.getPort());
      //          InetSocketAddress bindSocketAddress = new InetSocketAddress(bindAddress, serverBindPort);
      //          svrSocket = new VirtualServerSocket(connectSocketAddress, bindSocketAddress);
      //       }
      //       catch (Exception e)
      //       {
      //          throw new IOException(e.getMessage());
      //       }
      //    }
      
      // If this is a virtual MultiplexServerInvoker created by a master MultiplexServerInvoker,
      // then the server socket has already been created.
      if (serverSocket != null)
         return serverSocket;
      
      ServerSocket svrSocket = null;

      if (isVirtual)
      {
         InetSocketAddress bindSocketAddress = new InetSocketAddress(bindAddress, this.bindPort);
         svrSocket = new VirtualServerSocket(connectSocketAddress, bindSocketAddress, getTimeout(), configuration);
         svrSocket.setSoTimeout(getTimeout());
         
         if (socketGroupInfo != null)
            socketGroupInfo.setPrimingSocketNeeded(false);
      }
      else
      {
//         svrSocket = new MasterServerSocket(getServerSocketFactory(), bindPort, backlog, bindAddress);
         ServerSocketFactory ssf = getServerSocketFactory();
         if (ssf != null && !ssf.getClass().equals(ServerSocketFactory.getDefault().getClass()))
         {
            configuration.put(Multiplex.SERVER_SOCKET_FACTORY, ssf);
         }
         svrSocket = new MasterServerSocket(bindPort, backlog, bindAddress, configuration);
         svrSocket.setSoTimeout(getTimeout());
      }

      log.debug("Created " + svrSocket.getClass() + ": " + svrSocket);
      return svrSocket;
   }


   protected void rule1() throws IOException
   {
      log.debug("server rule 1");
      
      // If we get here, it's because a MultiplexClientInvoker created a SocketGroupInfo with matching
      // group id.  We want to make sure that it didn't get a bind address or bind port different
      // than the ones passed in through the parameters map.
      InetAddress socketGroupBindAddress = socketGroupInfo.getBindAddress();
      int socketGroupBindPort = socketGroupInfo.getBindPort();
      
      if (socketGroupBindAddress != null && !socketGroupBindAddress.equals(bindAddress))
      {
         String message = "socket group bind address (" + socketGroupBindAddress + 
                          ") does not match bind address (" + bindAddress + ")";
         log.error(message);
         socketGroupInfo = null;  // We don't belong to this group.
         throw new IOException(message);
      }
      
      if (socketGroupBindPort > 0 && originalBindPort > 0 && socketGroupBindPort != bindPort)
      {
         String message = "socket group bind port (" + socketGroupBindPort + 
                          ") does not match bind port (" + bindPort + ")";
         log.error(message);
         socketGroupInfo = null;  // We don't belong to this group.
         throw new IOException(message);
      }
      
      if (originalBindPort <= 0)
      {
         if (socketGroupBindPort > 0)
            bindPort = socketGroupBindPort;
         else
         {
            bindPort = PortUtil.findFreePort(bindHost);
            socketGroupBindPort = bindPort;
         }
         
         // re-write locator since the port is different
         final InvokerLocator newLocator = new InvokerLocator(locator.getProtocol(), locator.getHost(), bindPort, locator.getPath(), locator.getParameters());

         // need to update the locator key used in the invoker registry
         AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               InvokerRegistry.updateServerInvokerLocator(locator, newLocator);
               return null;
            }
         });
         
         this.locator = newLocator;
      }
      
      isVirtual = true;
      InetAddress connectAddress = socketGroupInfo.getConnectAddress();
      int connectPort = socketGroupInfo.getConnectPort();
      connectSocketAddress = new InetSocketAddress(connectAddress, connectPort);
      socketGroupInfo.setBindAddress(bindAddress);
      socketGroupInfo.setBindPort(bindPort);
      socketGroupInfo.setServerInvoker(this);
      
      Iterator it = socketGroupInfo.getClientInvokers().iterator();
      while (it.hasNext())
      {
         ((MultiplexClientInvoker) it.next()).finishStart();
      }
      
      readyToStart = true;

      if (socketGroupInfo.getPrimingSocket() == null)
      {
         socketFactory = createSocketFactory(configuration);
         if (socketFactory != null)
            configuration.put(Multiplex.SOCKET_FACTORY, socketFactory);
         
         createPrimingSocket(socketGroupInfo, connectAddress.getHostName(), connectPort,
                             bindAddress, bindPort, configuration, getTimeout());
      }

      // We got socketGroupInfo by socketGroupId.  Make sure it is also stored by AddressPair.
      String connectHost = connectAddress.getHostName();
      addressPair = new AddressPair(connectHost, connectPort, bindHost, bindPort);
      addressPairMap.put(addressPair, socketGroupInfo);
   }
   
   
   protected void rule2(String connectHost, int connectPort)
   throws IOException
   {
      log.debug("server rule 2");
      isVirtual = true;

      connectSocketAddress = new InetSocketAddress(connectHost, connectPort);    
      addressPair = new AddressPair(connectHost, connectPort, bindHost, bindPort);
      socketGroupInfo = (SocketGroupInfo) addressPairMap.get(addressPair);
      
      // If socketGroupInfo exists, it's because it was created, along with a priming socket (if necessary),
      // by a MultiplexClientInvoker.
      if (socketGroupInfo != null)
      {
         // We got socketGroupInfo by AddressPair.  Make sure it is stored by socketGroupId, if we have one.
         if (serverMultiplexId != null)
         {
            String socketGroupSocketGroupId = socketGroupInfo.getSocketGroupId();
            
            if (socketGroupSocketGroupId != null && socketGroupSocketGroupId != serverMultiplexId)
            {
               String message = "socket group multiplexId (" + socketGroupSocketGroupId + 
                                ") does not match multiplexId (" + serverMultiplexId + ")";
               log.error(message);
               socketGroupInfo = null; // Assume we weren't meant to join this group.
               throw new IOException(message);
            }
               
            if (socketGroupSocketGroupId == null)
            {
               socketGroupInfo.setSocketGroupId(serverMultiplexId);
               getSocketGroupMap().put(serverMultiplexId, socketGroupInfo);
            }
         }
         
         socketGroupInfo.setBindAddress(bindAddress);
         socketGroupInfo.setBindPort(bindPort);
         socketGroupInfo.setServerInvoker(this);
         readyToStart = true;
         return;
      }
      
      socketGroupInfo = new SocketGroupInfo();
      socketGroupInfo.setBindAddress(bindAddress);
      socketGroupInfo.setBindPort(bindPort);
      socketGroupInfo.setServerInvoker(this);
      
      // Set connectAddress and connectPort to be able to test for inconsistencies with connect address
      // and connect port determined by companion MultiplexClientInvoker.
      InetAddress connectAddress = InetAddress.getByName(connectHost);
      socketGroupInfo.setConnectAddress(connectAddress);
      socketGroupInfo.setConnectPort(connectPort);
      
      socketFactory = createSocketFactory(configuration);
      if (socketFactory != null)
         configuration.put(Multiplex.SOCKET_FACTORY, socketFactory);
      
      createPrimingSocket(socketGroupInfo, connectHost, connectPort,
                          bindAddress, bindPort, configuration, getTimeout());
      addressPairMap.put(addressPair, socketGroupInfo);
      
      if (serverMultiplexId != null)
      {
         socketGroupInfo.setSocketGroupId(serverMultiplexId);
         socketGroupMap.put(serverMultiplexId, socketGroupInfo);
      }
      
      readyToStart = true;
   }
   
   
   protected void rule3() throws IOException
   {
      log.debug("server rule 3");
      socketGroupInfo = new SocketGroupInfo();
      socketGroupInfo.setSocketGroupId(serverMultiplexId);
      socketGroupInfo.setServerInvoker(this);
      socketGroupInfo.setBindAddress(bindAddress);
      socketGroupInfo.setBindPort(bindPort);
      socketGroupMap.put(serverMultiplexId, socketGroupInfo);
      isVirtual = true;
      readyToStart = false;
   }
   
   
   protected void rule4()
   {
      log.debug("server rule 4");
      isVirtual = false;
      readyToStart = true;
   }
   
   
   protected void refreshServerSocket() throws IOException
   {
      super.refreshServerSocket();
   }
   
   /**
    * Returns <code>ServerSocket</code> used to accept invocation requests.
    * It is added to facilitate unit tests.
    *
    * @return <code>ServerSocket</code> used to accept invocation requests.
    */
   public ServerSocket getServerSocket()
   {
      return serverSocket;
   }
   
   
/**
 * Provides access to a virtual <code>MultiplexServerInvoker</code> in a master
 * <code>MultiplexServerInvoker</code>'s invoker farm.
 */
   public MultiplexServerInvoker getServerInvoker(InetSocketAddress address)
   {
      synchronized (virtualServerInvokers)
      {
         return (MultiplexServerInvoker) virtualServerInvokers.get(address);
      }
   }
   
   
/**
 * Provides access to all virtual <code>MultiplexServerInvoker</code>s in a master
 * <code>MultiplexServerInvoker</code>'s invoker farm.
 */
   public Collection getServerInvokers()
   {
      synchronized (virtualServerInvokers)
      {
         return virtualServerInvokers.values();
      }
   }
   
   protected void setBindingInfo() throws IOException
   {
      String originalUri = getLocator().getOriginalURI();
      String pastProtocol = originalUri.substring(originalUri.indexOf("://") + 3);
      int colon = pastProtocol.indexOf(":");
      int slash = pastProtocol.indexOf("/");
      String originalHost = null;
      int originalPort = 0;

      if(colon != -1)
      {
         originalHost = pastProtocol.substring(0, colon).trim();
         
         if(slash > -1)
         {
            originalPort = Integer.parseInt(pastProtocol.substring(colon + 1, slash));
         }
         else
         {
            originalPort = Integer.parseInt(pastProtocol.substring(colon + 1));
         }
      }
      else
      {
         if(slash > -1)
         {
            originalHost = pastProtocol.substring(0, slash).trim();
         }
         else
         {
            originalHost = pastProtocol.substring(0).trim();
         }
      }

      bindHost = getServerBindAddress();
      bindPort = getServerBindPort();
      bindAddress = InetAddress.getByName(bindHost);   
   }
   
   
   protected void getParameters() throws Exception
   {
      if (configuration != null)
         maxAcceptErrors
            = Multiplex.getOneParameter(configuration,
                                        "maxAcceptErrors",
                                        Multiplex.MAX_ACCEPT_ERRORS,
                                        Multiplex.MAX_ACCEPT_ERRORS_DEFAULT);
      
      if (configuration != null)
         serverMultiplexId = (String) configuration.get(Multiplex.SERVER_MULTIPLEX_ID);
      
      if (configuration != null)
         multiplexConnectHost = (String) configuration.get(Multiplex.MULTIPLEX_CONNECT_HOST);
      
      Object value = configuration.get(Multiplex.MULTIPLEX_CONNECT_PORT);
      if (value != null)
      {
         if (value instanceof String)
         {     
            try
            {
               multiplexConnectPort = Integer.parseInt((String) value);
               multiplexConnectPortIsSet = true;
            }
            catch (NumberFormatException e)
            {
               String errorMessage = "number format error for multiplexConnectPort: " + (String) value;
               log.error(errorMessage);
               throw new IOException(errorMessage);
            }
         }
         else if (value instanceof Integer)
         {
            multiplexConnectPort = ((Integer) configuration.get(Multiplex.MULTIPLEX_CONNECT_PORT)).intValue();
            multiplexConnectPortIsSet = true;
         }
         else
         {
            String errorMessage = "invalid object passed for multiplexConnectPort: " + value;
            log.error(errorMessage);
            throw new IOException(errorMessage);
         }
      }
   }
   
   
   /////////////////////////////////////////////////////////////////////////////////////
   //                accessors for configurable Multiplex parameters                  //
   /////////////////////////////////////////////////////////////////////////////////////
   public int getInputBufferSize()
   {
      return inputBufferSize;
   }


   public void setInputBufferSize(int inputBufferSize)
   {
      this.inputBufferSize = inputBufferSize;
      if (configuration != null)
         configuration.put(Multiplex.INPUT_BUFFER_SIZE, new Integer(inputBufferSize));
   }


   public int getInputMaxErrors()
   {
      return inputMaxErrors;
   }


   public void setInputMaxErrors(int inputMaxErrors)
   {
      this.inputMaxErrors = inputMaxErrors;
      if (configuration != null)
         configuration.put(Multiplex.INPUT_MAX_ERRORS, new Integer(inputMaxErrors));
   }


   public int getMaxAcceptErrors()
   {
      return maxAcceptErrors;
   }


   public void setMaxAcceptErrors(int maxAcceptErrors)
   {
      this.maxAcceptErrors = maxAcceptErrors;
      if (configuration != null)
         configuration.put(Multiplex.MAX_ACCEPT_ERRORS, new Integer(maxAcceptErrors));
   }


   public String getMultiplexConnectHost()
   {
      return multiplexConnectHost;
      
   }


// This method is useless.  See notes about paramters, above.
//   public void setMultiplexConnectHost(String multiplexConnectHost)
//   {
//      this.multiplexConnectHost = multiplexConnectHost;
//      if (configuration != null)
//         configuration.put(Multiplex.MULTIPLEX_CONNECT_HOST, multiplexConnectHost);
//   }


   public int getMultiplexConnectPort()
   {
      return multiplexConnectPort;
   }


// This method is useless.  See notes about paramters, above.
//   public void setMultiplexConnectPort(int multiplexConnectPort)
//   {
//      this.multiplexConnectPort = multiplexConnectPort;
//      if (configuration != null)
//         configuration.put(Multiplex.MULTIPLEX_CONNECT_PORT, new Integer(multiplexConnectPort));
//   }
   
   
   public int getOutputMaxChunkSize()
   {
      return outputMaxChunkSize;
   }


   public void setOutputMaxChunkSize(int outputMaxChunkSize)
   {
      this.outputMaxChunkSize = outputMaxChunkSize;
      if (configuration != null)
         configuration.put(Multiplex.OUTPUT_MAX_CHUNK_SIZE, new Integer(outputMaxChunkSize));  
   }


   public int getOutputMaxDataSlice()
   {
      return outputMaxDataSlice;
   }


   public void setOutputMaxDataSlice(int outputMaxDataSlice)
   {
      this.outputMaxDataSlice = outputMaxDataSlice;
      if (configuration != null)
         configuration.put(Multiplex.OUTPUT_MAX_DATA_SLICE, new Integer(outputMaxDataSlice));
   }


   public int getOutputMaxTimeSlice()
   {
      return outputMaxTimeSlice;
   }


   public void setOutputMaxTimeSlice(int outputMaxTimeSlice)
   {
      this.outputMaxTimeSlice = outputMaxTimeSlice;
      if (configuration != null)
         configuration.put(Multiplex.OUTPUT_MAX_TIME_SLICE, new Integer(outputMaxTimeSlice));
   }


   public int getOutputMessagePoolSize()
   {
      return outputMessagePoolSize;
   }


   public void setOutputMessagePoolSize(int outputMessagePoolSize)
   {
      this.outputMessagePoolSize = outputMessagePoolSize;
      if (configuration != null)
         configuration.put(Multiplex.OUTPUT_MESSAGE_POOL_SIZE, new Integer(outputMessagePoolSize));
   }


   public int getOutputMessageSize()
   {
      return outputMessageSize;
   }


   public void setOutputMessageSize(int outputMessageSize)
   {
      this.outputMessageSize = outputMessageSize;
      if (configuration != null)
         configuration.put(Multiplex.OUTPUT_MESSAGE_SIZE, new Integer(outputMessageSize));
   }


   public String getServerMultiplexId()
   {
      return serverMultiplexId;
   }


//   This method is useless.  See notes about paramters, above.
//   public void setServerMultiplexId(String serverMultiplexId)
//   {
//      this.serverMultiplexId = serverMultiplexId;
//      if (configuration != null)
//         configuration.put(Multiplex.SERVER_MULTIPLEX_ID, serverMultiplexId);
//   }
   
   
   public int getShutdownMonitorPeriod()
   {
      return shutdownMonitorPeriod;
   }


   public void setShutdownMonitorPeriod(int shutdownMonitorPeriod)
   {
      this.shutdownMonitorPeriod = shutdownMonitorPeriod;
      if (configuration != null)
         configuration.put(Multiplex.SHUTDOWN_MONITOR_PERIOD, new Integer(shutdownMonitorPeriod));
   }


   public int getShutdownRefusalsMaximum()
   {
      return shutdownRefusalsMaximum;
   }


   public void setShutdownRefusalsMaximum(int shutdownRefusalsMaximum)
   {
      this.shutdownRefusalsMaximum = shutdownRefusalsMaximum;
      if (configuration != null)
         configuration.put(Multiplex.SHUTDOWN_REFUSALS_MAXIMUM, new Integer(shutdownRefusalsMaximum));
   }


   public int getShutdownRequestTimeout()
   {
      return shutdownRequestTimeout;
   }


   public void setShutdownRequestTimeout(int shutdownRequestTimeout)
   {
      this.shutdownRequestTimeout = shutdownRequestTimeout;
      if (configuration != null)
         configuration.put(Multiplex.SHUTDOWN_REQUEST_TIMEOUT, new Integer(shutdownRequestTimeout));
   }


   public int getStaticThreadsMonitorPeriod()
   {
      return staticThreadsMonitorPeriod;
   }


   public void setStaticThreadsMonitorPeriod(int staticThreadsMonitorPeriod)
   {
      this.staticThreadsMonitorPeriod = staticThreadsMonitorPeriod;
      if (configuration != null)
         configuration.put(Multiplex.STATIC_THREADS_MONITOR_PERIOD, new Integer(staticThreadsMonitorPeriod));
   }


   /**
    * <code>SocketGroupInfo</code> holds all of the information for a single virtual socket group.
    */
   public static class SocketGroupInfo
   {
      private String                   socketGroupId;
      private Set                      clientInvokers = new HashSet();
      private MultiplexServerInvoker   serverInvoker;
      private boolean                  primingSocketNeeded;
      private VirtualSocket            primingSocket;
      private InetAddress              connectAddress;
      private int                      connectPort;
      private InetAddress              bindAddress;
      private int                      bindPort;
      
      public InetAddress getBindAddress()
      {
         return bindAddress;
      }

      public void setBindAddress(InetAddress bindAddress)
      {
         this.bindAddress = bindAddress;
      }

      public int getBindPort()
      {
         return bindPort;
      }

      public void setBindPort(int bindPort)
      {
         this.bindPort = bindPort;
      }
      
      public Set getClientInvokers()
      {
         return clientInvokers;
      }
      
      public void addClientInvoker(MultiplexClientInvoker clientInvoker)
      {
         clientInvokers.add(clientInvoker);
      }
      
      public void removeClientInvoker(MultiplexClientInvoker clientInvoker)
      {  
         clientInvokers.remove(clientInvoker);
      }
      
      public InetAddress getConnectAddress()
      {
         return connectAddress;
      }
  
      public void setConnectAddress(InetAddress connectAddress)
      {
         this.connectAddress = connectAddress;
      }
  
      public int getConnectPort()
      {
         return connectPort;
      }

      public void setConnectPort(int connectPort)
      {
         this.connectPort = connectPort;
      }
      
      public boolean getPrimingSocketNeeded()
      {
         return primingSocketNeeded;
      }
      
      public void setPrimingSocketNeeded(boolean primingSocketNeeded)
      {
         this.primingSocketNeeded = primingSocketNeeded;
      }
      
      public VirtualSocket getPrimingSocket()
      {
         return primingSocket;
      }
      
      public void setPrimingSocket(VirtualSocket primingSocket)
      {
         this.primingSocket = primingSocket;
      }

      public String getSocketGroupId()
      {
         return socketGroupId;
      }
      
      public void setSocketGroupId(String socketGroupId)
      {
         this.socketGroupId = socketGroupId;
      }
      
      public MultiplexServerInvoker getServerInvoker()
      {
         return serverInvoker;
      }
      
      public void removeServerInvoker(MultiplexServerInvoker serverInvoker)
      {
         if (this.serverInvoker != serverInvoker)
         {
            String message = "Attempt to remove unknown MultiplexServerInvoker: " +
            "(" + bindAddress + "," + bindPort + ")->(" + 
                  connectAddress + "," + connectPort + ")";
            log.error(message);
         }
         
         this.serverInvoker = null;
      }

      public void setServerInvoker(MultiplexServerInvoker serverInvoker) throws IOException
      {
         if (this.serverInvoker != null && serverInvoker != null)
         {
            String message = "Second MultiplexServerInvoker attempting to join invoker group: " +
                             "(" + bindAddress + "," + bindPort + ")->(" + 
                                   connectAddress + "," + connectPort + ")";
            log.error(message);
            throw new IOException(message);
         }
         
         this.serverInvoker = serverInvoker;
      }
   }

   
   protected static class InternalHandshakeListener implements HandshakeCompletedListener
   {
      public void handshakeCompleted(HandshakeCompletedEvent event)
      {
         handshakeCompletedEvent = event;
      }  
   }
}