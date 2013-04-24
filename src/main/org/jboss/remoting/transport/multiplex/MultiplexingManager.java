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
import org.jboss.remoting.transport.multiplex.InputMultiplexor.MultiGroupInputThread;
import org.jboss.remoting.transport.multiplex.utility.GrowablePipedOutputStream;
import org.jboss.remoting.transport.multiplex.utility.StoppableThread;
import org.jboss.remoting.transport.multiplex.utility.VirtualSelector;

import javax.net.SocketFactory;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


/**
 * <code>MultiplexingManager</code> is the heart of the Multiplex system.  It is the implementation
 * of the virtual socket group concept.  See the Multiplex documentation on the
 * labs.jboss.org website for more information about virtual socket groups.
 * <p>
 * <code>MultiplexingManager</code> wraps a single real <code>java.net.Socket</code>.
 * It creates the socket when it is running on the client side, and it is passed a
 * socket by <code>MasterServerSocket</code> when it is running on the server side.
 * <p>
 * <code>MultiplexingManager</code> creates the infrastructure
 * which supports multiplexing, including an <code>OutputMultiplexor</code> output thread and one
 * or more <code>InputMultiplexor</code> input threads.  When the last member leaves the socket
 * group, a <code>MultiplexingManager</code> is responsible for negotiating with its remote peer
 * for permission to shut down, and for tearing down the multiplexing infrastructure when
 * the negotiations succeed.
 * <p>
 * <code>MultiplexingManager</code> also provides the mechanism by which a virtual socket joins
 * a virtual socket group, identifying the new socket to the <code>InputMultiplexor</code> input
 * thread.
 *
 * <p>
 * Copyright (c) 2005
 * <p>
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class MultiplexingManager
implements OutputMultiplexor.OutputMultiplexorClient, HandshakeCompletedListener
{
   private static final Logger log = Logger.getLogger(MultiplexingManager.class);


   /** Determines how often to check that no MultiplexingManagers have been running,
    *  in which case the static threads can be shut down. */
   private static int staticThreadsMonitorPeriod;

   /** True if and only if the MultiplexingManager static threads are running */
   private static boolean staticThreadsRunning;

   /** This object is used to synchronized operations on the managersByRemoteAddress map. */
   private static Object shareableMapLock = new Object();

   /** A HashMap<InetSocketAddress, HasnSet<MultiplexingManager>> of sets of MultiplexingManager's
    *  indexed by remote address.  Holds all MultiplexingManagers whose peer has an attached
    *  VirtualServerSocket. */
   private static Map shareableManagers = new HashMap();

   /** This object is used to synchronize operations on the managersByLocalAddress map. */
   private static Object localAddressMapLock = new Object();

   /** A HashMap<InetSocketAddress, HasnSet<MultiplexingManager>> of sets of MultiplexingManager's
    *  indexed by local address */
   private static Map managersByLocalAddress = new HashMap();

   /** This object is used to synchronized operations on the managersByRemoteAddress map. */
   private static Object remoteAddressMapLock = new Object();

   /** A HashMap<InetSocketAddress, HasnSet<MultiplexingManager>> of sets of MultiplexingManager's
    *  indexed by remote address */
//   private static Map managersByRemoteAddress = Collections.synchronizedMap(new HashMap());
   private static Map managersByRemoteAddress = new HashMap();

   /** Set of all MultiplexingManagers */
   private static Set allManagers = Collections.synchronizedSet(new HashSet());

   /** InputMultiplexor in this JVM */
   private static InputMultiplexor inputMultiplexor;

   /** OutputMultiplexor in this JVM */
   private static OutputMultiplexor outputMultiplexor;

   /** Thread for writing to socket's OutputStream */
   private static OutputMultiplexor.OutputThread outputThread;

   /** MultiGroupInputThread reading from socket's InputStream and distributing to virtual sockets
    *  Processes all NIO sockets */
   private static MultiGroupInputThread multiGroupInputThread;

   /** MultiplexingInputStreams register with virtualSelector when they have bytes to read */
   private static VirtualSelector virtualSelector;

   /** Thread for getting asynchronous messages from remote Protocol */
   private static Protocol.BackChannelThread backChannelThread;

   /** Holds PendingActions waiting to be performed */
   private static List pendingActions = new ArrayList();

   /** Removes virtual sockets from closingSockets and closes them. */
   private static PendingActionThread pendingActionThread;

   /** Thread for stashing potentially long activities, as well as periodic activities */
   private static Timer timer;

   /** Used to determine when to shut down static threads */
   private static boolean hasBeenIdle;

   /** Used to distinguish the static threads in this jvm */
   private final static short time = (short) System.currentTimeMillis();

   /** If shutdown request is not answered within this time period, assume a problem
    *  snd shut down. */
   private int shutdownRequestTimeout;

   /** Determines how often ShutdownMonitorTimerTask should check for response
    *  to ShutdownRequestThread. */
   private int shutdownMonitorPeriod;

   /** If the peer MultiplexingManager has refused to shutdown this many times,
    *  shut down anyway. */
   private int shutdownRefusalsMaximum;

   /** Holds configuration parameters */
   private static Map configuration = new HashMap();

   /** A HashMap<SocketId, VirtualSocket> of VirtualSocket's indexed by local SocketId */
   private Map socketMap = Collections.synchronizedMap(new HashMap());

   /** A HashSet of SocketId's registered on this MultiplexingManager */
   private Set registeredSockets = Collections.synchronizedSet(new HashSet());

   /** A HashMap<Long, OutputStream> of piped OutputStreams associated with InputStreams  */
   private Map outputStreamMap = Collections.synchronizedMap(new HashMap());

   /** A HashMap<Long, InputStream> of InputStreams associated with virtual sockets  */
   private Map inputStreamMap = Collections.synchronizedMap(new HashMap());

   /** Holds OutputStreams associated with virtual sockets */
   private Set outputStreamSet = Collections.synchronizedSet(new HashSet());

   /** Protocol back channel OutputStream */
   private OutputStream backchannelOutputStream;

   /** Threads waiting to be notified of registration of remote server socket */
   private Set threadsWaitingForRemoteServerSocket = new HashSet();

   /** Protocol object for handling connection/disconnection communications */
   private Protocol protocol;

   /** Actual local socket upon which this family of virtual sockets is based */
   private Socket socket;

   /** Returned by toString() and used in log messages */
   String description;

   /** bound state of actual local socket */
   private boolean bound = false;

   /** connected state of actual local socket */
   private boolean connected = false;

   /** SocketAddress of remote actual socket to which this Manager's socket is connected */
   private InetSocketAddress remoteSocketAddress;

   /** SocketAddress to which this manager's actual socket is bound */
   private InetSocketAddress localSocketAddress;

   /** Represents local port on which this manager's actual socket is bound, with any local address */
   private InetSocketAddress localWildCardAddress;

   /** InputStream of real socket */
   private InputStream inputStream;

   /** OutputStream of real socket */
   private OutputStream outputStream;

   /** Currently registered server socket */
   private ServerSocket serverSocket;

   /** Indicates if remote server socket has been registered */
   private boolean remoteServerSocketRegistered = false;

   /** True if and only if this MultiplexingManager was originally created by a
    *  call to MasterServerSocket.acceptServerSocketConnection(). */
   private boolean createdForRemoteServerSocket;

   /** SingleGroupInputThread reading from socket's InputStream and distributing to virtual sockets */
   private InputMultiplexor.SingleGroupInputThread inputThread;

   /** OutputStream for unknown virtual sockets */
   private OutputStream deadLetterOutputStream = new ByteArrayOutputStream();

   /** Manages the shutdown handshaking protocol between peer MultiplexingManagers */
   private ShutdownManager shutdownManager = new ShutdownManager();

   /** Thread that carries out shut down process */
   private ShutdownThread shutdownThread;

   /** true if and only MultiplexingManager is completely shut down */
   private boolean shutdown = false;

   /** Indicates if log trace level is enabled */
   private boolean trace;

   /** Indicates if log debug level is enabled */
   private boolean debug;

   /** Indicates if log info level is enabled */
   private boolean info;

   private long id;

   /** SocketFactory to use for creating sockets */
   private SocketFactory socketFactory;

   /** Saves HandshakeCompletedEvent for SSLSocket */
   private HandshakeCompletedEvent handshakeCompletedEvent;

   /** Holds IOException thrown by real InputStream */
   private IOException readException;

   /** Holds IOException thrown by real OutputStream */
   private IOException writeException;


   protected synchronized static void init(Map configuration) throws IOException
   {
      try
      {
         if (staticThreadsRunning)
            return;

         log.debug("starting static threads");

         // Start output thread.
         outputMultiplexor = new OutputMultiplexor(configuration);
         outputThread = outputMultiplexor.getAnOutputThread();
         outputThread.setName("output:" + time);
         outputThread.setDaemon(true);
         outputThread.start();
         log.debug("started output thread");

         // Start input thread.
         inputMultiplexor = new InputMultiplexor(configuration);
         multiGroupInputThread = inputMultiplexor.getaMultiGroupInputThread();
         multiGroupInputThread.setName("input:" + time);
         multiGroupInputThread.setDaemon(true);
         multiGroupInputThread.start();
         log.debug("started input thread");

         // Start back channel thread.
         virtualSelector = new VirtualSelector();
         backChannelThread = Protocol.getBackChannelThread(virtualSelector);
         backChannelThread.setName("backchannel:" + time);
         backChannelThread.setDaemon(true);
         backChannelThread.start();
         log.debug("started backchannel thread");

         // Start timer.
         timer = new Timer(true);
         TimerTask shutdownMonitorTask = new TimerTask()
         {
            public void run()
            {
               log.trace("allManagers.isEmpty(): " + allManagers.isEmpty());
               log.trace("hasBeenIdle: " + hasBeenIdle);
               if (allManagers.isEmpty())
               {
                  if (hasBeenIdle)
                  {
                     MultiplexingManager.shutdownThreads();
                     this.cancel();
                  }
                  else
                     hasBeenIdle = true;
               }
               else
               {
                  hasBeenIdle = false;
               }
            }

         };
         timer.scheduleAtFixedRate(shutdownMonitorTask, staticThreadsMonitorPeriod, staticThreadsMonitorPeriod);

         // Start pending actions thread.
         pendingActionThread = new PendingActionThread();
         pendingActionThread.setName("pending actions:" + time);
         pendingActionThread.setDaemon(true);
         pendingActionThread.start();
         log.debug("started pendingAction thread");

         staticThreadsRunning = true;
      }
      catch (IOException e)
      {
         log.error(e);
         throw e;
      }
   }


   protected MultiplexingManager(Map configuration) throws IOException
   {
      if (configuration != null)
         MultiplexingManager.configuration.putAll(configuration);
      socketFactory = (SocketFactory) configuration.get(Multiplex.SOCKET_FACTORY);
      id = new Date().getTime();
      socket = createSocket();
      allManagers.add(this);
      if (debug) log.debug("new MultiplexingManager(" + id + "): " + description);
   }


/**
 *
 * @param socket
 * @param configuration
 * @throws IOException
 */
   protected MultiplexingManager(Socket socket, Map configuration) throws IOException
   {
      this.socket = socket;
      if (configuration != null)
         MultiplexingManager.configuration.putAll(configuration);
      id = new Date().getTime();
      setup();
      allManagers.add(this);
      if (debug) log.debug("new MultiplexingManager(" + id + "): " + description);
   }


/**
 *
 * @param address
 * @param timeout
 * @param configuration
 * @throws IOException
 */
   protected MultiplexingManager(InetSocketAddress address, int timeout, Map configuration)
   throws IOException
   {
      if (configuration != null)
         MultiplexingManager.configuration.putAll(configuration);
      socketFactory = (SocketFactory) configuration.get(Multiplex.SOCKET_FACTORY);
      id = new Date().getTime();
      socket = createSocket(address, timeout);
      setup();
      allManagers.add(this);
      if (debug) log.debug("new MultiplexingManager(" + id + "): " + description);
   }


/**
 *
 */
   protected synchronized void setup() throws IOException
   {
      description = socket.toString();
      trace = log.isTraceEnabled();
      debug = log.isDebugEnabled();
      info = log.isInfoEnabled();

      // Initialize MultiplexingManager parameters.
      initParameters(configuration);

      // Make sure static threads are running.
      synchronized (MultiplexingManager.class)
      {
         if (!staticThreadsRunning)
            init(configuration);
      }

      // Get InputStream and OutputStream
      if (socket.getChannel() == null)
      {
//         inputStream = new BufferedInputStream(socket.getInputStream());
//         outputStream = new BufferedOutputStream(socket.getOutputStream());
         inputStream = socket.getInputStream();
         outputStream = socket.getOutputStream();
      }
      else
      {
         inputStream = Channels.newInputStream(socket.getChannel());
         outputStream = Channels.newOutputStream(socket.getChannel());
         socket.setTcpNoDelay(false);
      }

      // register dead letter output stream (for unrecognized destinations)
      outputStreamMap.put(SocketId.DEADLETTER_SOCKET_ID, deadLetterOutputStream);

      // TODO: what was this for???
      registeredSockets.add(SocketId.PROTOCOL_SOCKET_ID);
      registeredSockets.add(SocketId.SERVER_SOCKET_ID);
      registeredSockets.add(SocketId.SERVER_SOCKET_CONNECT_ID);
      registeredSockets.add(SocketId.SERVER_SOCKET_VERIFY_ID);
      registeredSockets.add(SocketId.BACKCHANNEL_SOCKET_ID);

      // set up standard piped streams
      getAnInputStream(SocketId.PROTOCOL_SOCKET_ID, null);
      getAnInputStream(SocketId.SERVER_SOCKET_ID, null);
      getAnInputStream(SocketId.SERVER_SOCKET_CONNECT_ID, null);
      getAnInputStream(SocketId.SERVER_SOCKET_VERIFY_ID, null);

      // Create protocol backchannel streams
      protocol = new Protocol(this);
      MultiplexingInputStream bcis = getAnInputStream(SocketId.BACKCHANNEL_SOCKET_ID, null);
      bcis.register(virtualSelector, this);
      if (debug) log.debug("registered backchannel input stream");
      backchannelOutputStream = new MultiplexingOutputStream(this, SocketId.PROTOCOL_SOCKET_ID);

      // Register with OutputMultiplexor
      outputMultiplexor.register(this);

      // Create or register with input thread
      if (socket.getChannel() == null)
      {
         // start input thread
         log.debug("creating single group input thread");

         if (inputMultiplexor == null)
            inputMultiplexor = new InputMultiplexor(configuration);

         inputThread = inputMultiplexor.getaSingleGroupInputThread(this, socket, deadLetterOutputStream);
         inputThread.setName(inputThread.getName() + ":input(" + description + ")");
         inputThread.start();
      }
      else
      {
         socket.getChannel().configureBlocking(false);
         multiGroupInputThread.registerSocketGroup(this);
         log.debug("registered socket group");
      }

      registerByLocalAddress(new InetSocketAddress(socket.getLocalAddress(), socket.getLocalPort()));
      registerByRemoteAddress(new InetSocketAddress(socket.getInetAddress(), socket.getPort()));
      bound = true;
      connected = true;

      if (socket instanceof SSLSocket)
      {
//         Object o = configuration.get(Multiplex.SSL_HANDSHAKE_LISTENER);
//         if (o != null)
//         {
//            HandshakeCompletedListener hcl = (HandshakeCompletedListener) o;
//            ((SSLSocket) socket).addHandshakeCompletedListener(hcl);
//         }
         ((SSLSocket) socket).addHandshakeCompletedListener(this);
      }
   }


   protected void initParameters(Map configuration)
   {
      this.configuration = configuration;

      staticThreadsMonitorPeriod
         = Multiplex.getOneParameter(configuration,
                                     "staticThreadsMonitorPeriod",
                                      Multiplex.STATIC_THREADS_MONITOR_PERIOD,
                                      Multiplex.STATIC_THREADS_MONITOR_PERIOD_DEFAULT);

      shutdownRequestTimeout
         = Multiplex.getOneParameter(configuration,
                                     "shutdownRequestTimeout",
                                     Multiplex.SHUTDOWN_REQUEST_TIMEOUT,
                                     Multiplex.SHUTDOWN_REQUEST_TIMEOUT_DEFAULT);

      shutdownRefusalsMaximum
         = Multiplex.getOneParameter(configuration,
                                     "shutdownRefusalsMaximum",
                                     Multiplex.SHUTDOWN_REFUSALS_MAXIMUM,
                                     Multiplex.SHUTDOWN_REFUSALS_MAXIMUM_DEFAULT);

      shutdownMonitorPeriod
         = Multiplex.getOneParameter(configuration,
                                     "shutdownMonitorPeriod",
                                     Multiplex.SHUTDOWN_MONITOR_PERIOD,
                                     Multiplex.SHUTDOWN_MONITOR_PERIOD_DEFAULT);
   }


/**
 *
 * @param socket
 * @param configuration
 * @return
 * @throws IOException
 *
 * TODO: what if multiplexor already exists?
 */
   public static MultiplexingManager getaManager(Socket socket, Map configuration) throws IOException
   {
      log.debug("entering getaManager(Socket socket)");
      return new MultiplexingManager(socket, configuration);
   }


/**
 * @param address
 * @return
 * @throws IOException
 */
   public static synchronized MultiplexingManager
   getaManagerByLocalAddress(InetSocketAddress address) throws IOException
   {
      return getaManagerByLocalAddress(address, null);
   }


/**
 *
 * @param address
 * @param conf
 * @return
 * @throws IOException
 */
   public static synchronized MultiplexingManager
   getaManagerByLocalAddress(InetSocketAddress address, Map conf) throws IOException
   {
      log.debug("entering getaManagerByLocalAddress(InetSocketAddress address)");
      MultiplexingManager m = null;

      synchronized (localAddressMapLock)
      {
         HashSet managers = (HashSet) managersByLocalAddress.get(address);

         if (managers != null)
         {
            Iterator it = managers.iterator();
            while (it.hasNext())
            {
               m = (MultiplexingManager) it.next();
               try
               {
                  m.shutdownManager.incrementReferences();
                  return m;
               }
               catch (IOException e)
               {
               }
            }
         }
      }

      log.debug("There is no joinable MultiplexingManager. Creating new one.");
      m = new MultiplexingManager(conf);
      m.bind(address);
      return m;
   }


/**
 *
 * @param address
 * @param timeout
 * @return
 * @throws IOException
 */
   public static synchronized MultiplexingManager
   getaManagerByRemoteAddress(InetSocketAddress address, int timeout) throws IOException
   {
      return getaManagerByRemoteAddress(address, timeout, null);
   }


/**
 *
 * @param address
 * @param timeout
 * @param configuration
 * @return
 * @throws IOException
 */
   public static synchronized MultiplexingManager
   getaManagerByRemoteAddress(InetSocketAddress address, int timeout, Map conf)
   throws IOException
   {
      log.debug("entering getaManagerByRemoteAddress(InetSocketAddress address)");

//      if (isOnServer())
//      {

      // Check each of the MultiplexingManagers connected to the target remote address, looking
      // for one which is not shutting down.
      synchronized(remoteAddressMapLock)
      {
         HashSet managers = (HashSet) managersByRemoteAddress.get(address);

         if (managers != null && !managers.isEmpty())
         {
            Iterator it = managers.iterator();
            while (it.hasNext())
            {
               MultiplexingManager m = (MultiplexingManager) it.next();
               try
               {
                  m.shutdownManager.incrementReferences();
                  return m;
               }
               catch (Exception e)
               {
                  log.debug("manager shutting down: " + m);
               }
            }
         }
      }

      return new MultiplexingManager(address, timeout, conf);
   }



/**
 * @param remoteAddress
 * @param localAddress
 * @param timeout
 * @return
 * @throws IOException
 */
   public static synchronized MultiplexingManager
   getaManagerByAddressPair(InetSocketAddress remoteAddress, InetSocketAddress localAddress, int timeout)
   throws IOException
   {
      return getaManagerByAddressPair(remoteAddress, localAddress, timeout, null);
   }


/**
 * @param remoteAddress
 * @param localAddress
 * @param timeout
 * @param configuration
 * @return
 * @throws IOException
 */
   public static synchronized MultiplexingManager
   getaManagerByAddressPair(InetSocketAddress remoteAddress, InetSocketAddress localAddress,
                            int timeout, Map conf)
   throws IOException
   {
      log.debug("entering getaManagerByRemoteAddress(InetSocketAddress address)");
      MultiplexingManager m;

      // Check each of the MultiplexingManagers connected to the target remote address, looking
      // for one which is not shutting down.
      synchronized(remoteAddressMapLock)
      {
         HashSet managers = (HashSet) managersByRemoteAddress.get(remoteAddress);

         if (managers != null && !managers.isEmpty())
         {
            Iterator it = managers.iterator();

            while (it.hasNext())
            {
               m = (MultiplexingManager) it.next();
               if (m.getSocket().getLocalAddress().equals(localAddress.getAddress()) &&
                   m.getSocket().getLocalPort() == localAddress.getPort())
               {
                  try
                  {
                     m.shutdownManager.incrementReferences();
                     return m;
                  }
                  catch (Exception e)
                  {
                     log.debug("manager shutting down: " + m);
                  }
               }
            }
         }
      }

      log.debug("There is no joinable MultiplexingManager. Creating new one.");
      m = new MultiplexingManager(conf);
      m.bind(localAddress);
      return m;
   }


/**
 * @param address
 * @param timeout
 * @return
 * @throws IOException
 */
   public static synchronized MultiplexingManager
   getaShareableManager(InetSocketAddress address, int timeout) throws IOException
   {
      return getaShareableManager(address, timeout, null);
   }


/**
 * @param address
 * @param timeout
 * @param conf
 * @return
 * @throws IOException
 */
   public static synchronized MultiplexingManager
   getaShareableManager(InetSocketAddress address, int timeout, Map conf) throws IOException
   {
      log.debug("entering getaShareableManager(InetSocketAddress address)");

      // Check each of the shareable MultiplexingManagers connected to the target remote address, looking
      // for one which is not shutting down.
      synchronized(shareableMapLock)
      {
         HashSet managers = (HashSet) shareableManagers.get(address);

         if (managers != null && !managers.isEmpty())
         {
            Iterator it = managers.iterator();
            while (it.hasNext())
            {
               MultiplexingManager m = (MultiplexingManager) it.next();
               try
               {
                  m.shutdownManager.incrementReferences();
                  return m;
               }
               catch (Exception e)
               {
                  log.debug("manager shutting down: " + m);
               }
            }
         }
      }

      return new MultiplexingManager(address, timeout, conf);
   }


/**
 * @param address
 * @param timeout
 * @param conf
 * @return
 * @throws IOException
 */
   public static  MultiplexingManager
   getAnExistingShareableManager(InetSocketAddress address, Map conf)
   throws IOException
   {
      log.debug("entering getAnExistingShareableManager()");

      // Check each of the shareable MultiplexingManagers connected to the target remote address, looking
      // for one which is not shutting down.
      synchronized(shareableMapLock)
      {
         HashSet managers = (HashSet) shareableManagers.get(address);

         if (managers != null && !managers.isEmpty())
         {
            Iterator it = managers.iterator();

            while (it.hasNext())
            {
               MultiplexingManager m = (MultiplexingManager) it.next();
               try
               {
                  m.shutdownManager.incrementReferences();
                  return m;
               }
               catch (Exception e)
               {
                  log.debug("manager shutting down: " + m);
               }
            }
         }
      }

      return null;
   }


/**
 * @param remoteAddress
 * @param localAddress
 * @param timeout
 * @return
 * @throws IOException
 */
   public static synchronized MultiplexingManager
   getaShareableManagerByAddressPair(InetSocketAddress remoteAddress, InetSocketAddress localAddress, int timeout)
   throws IOException
   {
      return getaShareableManagerByAddressPair(remoteAddress, localAddress, timeout, null);
   }


/**
 * @param remoteAddress
 * @param localAddress
 * @param timeout
 * @param conf
 * @return
 * @throws IOException
 */
   public static synchronized MultiplexingManager
   getaShareableManagerByAddressPair(InetSocketAddress remoteAddress, InetSocketAddress localAddress,
                                     int timeout, Map conf)
   throws IOException
   {
      MultiplexingManager m;

      // Check each of the shareable MultiplexingManagers connected to the target remote address, looking
      // for one which is not shutting down.
      synchronized(shareableMapLock)
      {
         HashSet managers = (HashSet) shareableManagers.get(remoteAddress);
         if (managers != null && !managers.isEmpty())
         {
            Iterator it = managers.iterator();

            while (it.hasNext())
            {
               m = (MultiplexingManager) it.next();
               if (m.getSocket().getLocalAddress().equals(localAddress.getAddress()) &&
                   m.getSocket().getLocalPort() == localAddress.getPort())
               {
                  try
                  {
                     m.shutdownManager.incrementReferences();
                     return m;
                  }
                  catch (Exception e)
                  {
                     log.debug("manager shutting down: " + m);
                  }
               }
            }
         }
      }

      log.debug("There is no joinable MultiplexingManager. Creating new one.");
      m = new MultiplexingManager(conf);
      m.bind(localAddress);
      return m;
   }


/**
 * @param remoteAddress
 * @param localAddress
 * @param conf
 * @return
 * @throws IOException
 */
   public static MultiplexingManager
   getAnExistingShareableManagerByAddressPair(InetSocketAddress remoteAddress,
                                              InetSocketAddress localAddress,
                                              Map conf)
   throws IOException
   {
      log.debug("entering getaShareableManager(InetSocketAddress address)");
      MultiplexingManager m;

      // Check each of the shareable MultiplexingManagers connected to the target remote address, looking
      // for one which is not shutting down.
      synchronized(shareableMapLock)
      {
         HashSet managers = (HashSet) shareableManagers.get(remoteAddress);

         if (managers != null && !managers.isEmpty())
         {
            Iterator it = managers.iterator();

            while (it.hasNext())
            {
               m = (MultiplexingManager) it.next();
               if (m.getSocket().getLocalAddress().equals(localAddress.getAddress()) &&
                   m.getSocket().getLocalPort() == localAddress.getPort())
               {
                  try
                  {
                     m.shutdownManager.incrementReferences();
                     return m;
                  }
                  catch (Exception e)
                  {
                     log.debug("manager shutting down: " + m);
                  }
               }
            }
         }
      }

      return null;
   }


/**
 * @param address
 * @return
 * @throws IOException
 */
   public static boolean checkForShareableManager(InetSocketAddress address)
   throws IOException
   {
      log.debug("entering checkForShareableManager(InetSocketAddress address)");

      // Check each if there is at least one shareable MultiplexingManagers connected to the target remote address.
      synchronized (shareableMapLock)
      {
         HashSet managers = (HashSet) shareableManagers.get(address);

         if (managers != null && !managers.isEmpty())
            return true;

         return false;
      }
   }


/**
 * @param localAddress
 * @param remoteAddress
 * @return
 */
   public static boolean checkForManagerByAddressPair(InetSocketAddress localAddress,
                                                      InetSocketAddress remoteAddress)
   {
      log.debug("entering checkForManagerByAddressPair()");

      // Check each of the MultiplexingManagers connected to the target remote address, looking
      // for one bound to the local address.
      synchronized(remoteAddressMapLock)
      {
         HashSet managers = (HashSet) managersByRemoteAddress.get(remoteAddress);

         if (managers != null && !managers.isEmpty())
         {
            Iterator it = managers.iterator();

            while (it.hasNext())
            {
               MultiplexingManager m = (MultiplexingManager) it.next();

               if (m.localSocketAddress.equals(localAddress))
                  return true;
            }
         }
      }

      return false;
   }


/**
 * @param localAddress
 * @param remoteAddress
 * @return
 */
   public static boolean checkForShareableManagerByAddressPair(InetSocketAddress localAddress,
                                                               InetSocketAddress remoteAddress)
   {
      log.debug("entering checkForShareableManagerByAddressPair()");

      // Check each of the shareable MultiplexingManagers connected to the target remote address, looking
      // for one bound to the local address.
      synchronized (shareableMapLock)
      {
         HashSet managers = (HashSet) shareableManagers.get(remoteAddress);

         if (managers != null && !managers.isEmpty())
         {
            Iterator it = managers.iterator();

            while (it.hasNext())
            {
               MultiplexingManager m = (MultiplexingManager) it.next();

               if (m.localSocketAddress.equals(localAddress))
                  return true;
            }
         }
      }

      return false;
   }


/**
 * @return
 */
   public static int getStaticThreadMonitorPeriod()
   {
      return staticThreadsMonitorPeriod;
   }


/**
 * @param period
 */
   public static void setStaticThreadsMonitorPeriod(int period)
   {
      staticThreadsMonitorPeriod = period;
   }


/**
 *
 */
   protected synchronized static void shutdownThreads()
   {
      log.debug("entering shutdownThreads");
      if (outputThread != null)
         outputThread.shutdown();

      if (multiGroupInputThread != null)
         multiGroupInputThread.shutdown();

      if (backChannelThread != null)
         backChannelThread.shutdown();

      if (pendingActionThread != null)
         pendingActionThread.shutdown();

      log.debug("cancelling timer");
      if (timer != null)
         timer.cancel();

      while (true)
      {
         try
         {
            if (outputThread != null)
               outputThread.join();

            if (multiGroupInputThread != null)
               multiGroupInputThread.join();

            if (backChannelThread != null)
               backChannelThread.join();

            if (pendingActionThread != null)
               pendingActionThread.join();

            break;
         }
         catch (InterruptedException ignored) {}
      }

      staticThreadsRunning = false;
      log.debug("static threads shut down");
   }


/**
 * Adds a <code>PendingAction</code> to the list of actions waiting to be executed.
 * @param pendingAction
 */
   protected static void addToPendingActions(PendingAction pendingAction)
   {
      synchronized (pendingActions)
      {
         pendingActions.add(pendingAction);
         pendingActions.notifyAll();
      }
   }


/**
 * Binds the wrapped socket.
 * @param address
 * @throws IOException
 */
   public synchronized void bind(InetSocketAddress address) throws IOException
   {
      if (bound)
         throw new IOException("socket is already bound");

      if (socket == null)
         socket = createSocket();

      if (socket == null)
         localSocketAddress = address;
      else
         socket.bind(address);

      bound = true;
   }

/**
 * Connects the wrapped socket.
 * @param address
 * @param timeout
 * @throws IOException
 */
   public synchronized void connect(InetSocketAddress address, int timeout) throws IOException
   {
      if (connected)
      {
         if (socket.getRemoteSocketAddress().equals(address))
            return;
         else
            throw new IOException("socket is already connected");
      }

      if (debug) log.debug("connecting to: " + address);

      if (socket == null)
         socket = createSocket(address, timeout);
      else
         socket.connect(address, timeout);

      connected = true;
      setup();
   }


/**
 * Identifies a <code>VirtualServerSocket</code> as the one associated with this virtual socket group.
 * @return a <code>MultiplexingInputStream</code> for use by serverSocket.
 */
   public synchronized MultiplexingInputStream registerServerSocket(ServerSocket serverSocket) throws IOException
   {
      if (this.serverSocket != null && this.serverSocket != serverSocket)
      {
         log.error("[" + id + "]: " + "attempt to register a second server socket");
         log.error("current server socket: " + this.serverSocket.toString());
         log.error("new server socket:     " + serverSocket.toString());
         throw new IOException("attempt to register a second server socket");
      }

      if (debug) log.debug(serverSocket.toString());
      this.serverSocket = serverSocket;
      return getAnInputStream(SocketId.SERVER_SOCKET_ID, null);
   }


/**
 * Indicates a <code>VirtualServerSocket</code> is leaving this virtual socket group.
 * @param serverSocket
 * @throws IOException
 */
   public synchronized void unRegisterServerSocket(ServerSocket serverSocket) throws IOException
   {
      if (this.serverSocket != serverSocket)
      {
         log.error("server socket attempting unregister but is not registered");
         throw new IOException("server socket is not registered");
      }

      log.debug("server socket unregistering");
      removeAnInputStream(SocketId.SERVER_SOCKET_ID);
      this.serverSocket = null;
      shutdownManager.decrementReferences();
   }


/**
 * Adds a <code>VirtualSocket</code> to this virtual socket group.
 * @param socket
 * @return a <code>MultiplexingInputStream</code> for use by socket
 * @throws IOException
 */
   public synchronized MultiplexingInputStream registerSocket(VirtualSocket socket) throws IOException
   {
      SocketId localSocketId = socket.getLocalSocketId();
      VirtualSocket currentSocket = (VirtualSocket) socketMap.put(localSocketId, socket);

      if (currentSocket != null)
      {
         String errorMessage = "attempting to register socket on currently used port:"
                              + currentSocket.getLocalVirtualPort();
         log.error(errorMessage);
         throw new IOException(errorMessage);
      }

      if (debug) log.debug("registering virtual socket on port: " + localSocketId.getPort());
      registeredSockets.add(socket.getLocalSocketId());
      return getAnInputStream(localSocketId, socket);
   }


/**
 * Indicates that a <code>VirtualSocket</code> is leaving a virtual socket group.
 * @param socket
 * @throws IOException
 */
   public synchronized void unRegisterSocket(VirtualSocket socket) throws IOException
   {
      try
      {
         if (debug) log.debug(this + ": entering unRegisterSocket()");
         shutdownManager.decrementReferences();

         SocketId localSocketId = socket.getLocalSocketId();
         if (localSocketId == null)
            return;

         VirtualSocket currentSocket = (VirtualSocket) socketMap.remove(localSocketId);
         if (currentSocket == null)
         {
            String errorMessage = "attempting to unregister unrecognized socket: " + socket.getLocalSocketId().getPort();
            log.error(errorMessage);
            throw new IOException(errorMessage);
         }

         if (debug) log.debug("unregistering virtual socket on port: " + localSocketId.getPort());
         registeredSockets.remove(localSocketId);
         removeAnInputStream(localSocketId);
         if (debug) log.debug(this + ": leaving unRegisterSocket()");
      }
      finally
      {
         socket.close();
      }
   }


 /**
  * Indicates that a <code>VirtualServerSocket</code> belongs the virtual socket group at the
  * remote end of the connection.  This virtual socket groupD becomes "joinable", in the
  * sense that a new <code>VirtualSocket</code> <it>v</it> can join this virtual group because there is a
  * <code>VirtualServerSocket</code> at the other end of the connection to create a remote peer for
  * <code>v</code>.
  */
    public synchronized void registerRemoteServerSocket() throws IOException
    {
       log.debug("registerRemoteServerSocket()");
       if (remoteServerSocketRegistered)
       {
          log.error("duplicate remote server socket registration");
          throw new IOException("duplicate remote server socket registration");
       }
       else
       {
          remoteServerSocketRegistered = true;

          // Now that remote MultiplexingManager has a VirtualServerSocket, we
          // add it to set of managers eligible to accept new clients.
          registerShareable(remoteSocketAddress);

          synchronized (threadsWaitingForRemoteServerSocket)
          {
             threadsWaitingForRemoteServerSocket.notifyAll();
          }

          if (!createdForRemoteServerSocket)
             incrementReferences();
       }
    }


/**
 * Indicates there will no longer be a <code>VirtualServeSocket</code> at the remote end of
 * this connection.  This virtual socket group will no longer be "joinable".
 * (See registerRemoteServerSocket().)
 */
   public synchronized void unRegisterRemoteServerSocket()
   {
      if (!remoteServerSocketRegistered)
         log.error("no remote server socket is registered");
      else
      {
         if (debug) log.debug(this + ": remote VSS unregistering");
         remoteServerSocketRegistered = false;
         unregisterShareable();

         MultiplexingManager.addToPendingActions
         (
               new PendingAction()
               {
                  void doAction()
                  {
                     try
                     {
                        decrementReferences();
                     }
                     catch (IOException e)
                     {
                        log.error(e);
                     }
                  }
               }
         );
      }
   }


   public void setCreatedForRemoteServerSocket()
   {
      createdForRemoteServerSocket = true;
   }


   public synchronized boolean isRemoteServerSocketRegistered()
   {
      return remoteServerSocketRegistered;
   }


   public boolean waitForRemoteServerSocketRegistered()
   {
      if (remoteServerSocketRegistered)
         return true;

      synchronized (threadsWaitingForRemoteServerSocket)
      {
         threadsWaitingForRemoteServerSocket.add(Thread.currentThread());

         while (!remoteServerSocketRegistered)
         {
            try
            {
               threadsWaitingForRemoteServerSocket.wait();
            }
            catch (InterruptedException e)
            {
               log.info("interrupted waiting for registration of remote server socket");

               if (shutdown)
               {
                  threadsWaitingForRemoteServerSocket.remove(Thread.currentThread());
                  return false;
               }
            }
         }
      }

      threadsWaitingForRemoteServerSocket.remove(Thread.currentThread());
      return true;
   }

/**
 * Increment reference counter for this <code>MultiplexingManager</code>.
 * @throws IOException
 */
   public void incrementReferences() throws IOException
   {
      shutdownManager.incrementReferences();
   }


/**
 * Decrement reference counter for this <code>MultiplexingManager</code>.
 * @throws IOException
 */
   public void decrementReferences() throws IOException
   {
      shutdownManager.decrementReferences();
   }


/**
 *
 * @return
 */
   public Collection getAllOutputStreams()
   {
      return outputStreamMap.values();
   }


/**
 * Returns<code> OutputStream</code> to use when corrupted InputStream gives no known "mailbox"
 * @return OutputStream to use when corrupted InputStream gives no known "mailbox"
 */
   public OutputStream getDeadLetterOutputStream()
   {
      return deadLetterOutputStream;
   }


/**
 * Returns <code>InputStream</code> of real socket
 * @return InputStream of real socket
 */
   public InputStream getInputStream()
   {
      return inputStream;
   }


/**
 * Returns <code>OutputStream</code> of real socket
 * @return OutputStream of real socket
 */
   public OutputStream getOutputStream()
   {
      return outputStream;
   }

/**
 * Get an <code>InputStream</code> for a <code>VirtualSocket</code>.
 * @param socketId
 * @param socket
 * @return
 * @throws IOException
 */
   public MultiplexingInputStream getAnInputStream(SocketId socketId, VirtualSocket socket) throws IOException
   {
      if (debug) log.debug("getAnInputStream(): " + socketId.getPort());
      MultiplexingInputStream mis = (MultiplexingInputStream) inputStreamMap.get(socketId);

      if (mis != null)
      {
         if (mis.getSocket() == null)
            mis.setSocket(socket);
         return mis;
      }

      GrowablePipedOutputStream pos = (GrowablePipedOutputStream) outputStreamMap.get(socketId);
      if (pos == null)
      {
         pos = new GrowablePipedOutputStream();
         outputStreamMap.put(socketId, pos);
      }

      mis = new MultiplexingInputStream(pos, this, socket);
      inputStreamMap.put(socketId, mis);
      if (readException != null)
         mis.setReadException(readException);

      return mis;
   }

/**
 * Get an <code>OutputStrem</code> for a <code>VirtualSocket</code>.
 * @param socketId
 * @return
 */
   public GrowablePipedOutputStream getAnOutputStream(SocketId socketId)
   {
      if (debug) log.debug("getAnOutputStream(): " + socketId.getPort());

      GrowablePipedOutputStream pos = (GrowablePipedOutputStream) outputStreamMap.get(socketId);
      if (pos == null)
      {
         pos = new GrowablePipedOutputStream();
         outputStreamMap.put(socketId, pos);
      }

      return pos;
   }


/**
 * @param socketId
 * @return
 */
   public MultiplexingOutputStream getAnOutputStream(VirtualSocket socket, SocketId socketId)
   {
      MultiplexingOutputStream mos = new MultiplexingOutputStream(this, socket, socketId);
      outputStreamSet.add(mos);
      if (writeException != null)
         mos.setWriteException(writeException);
      return mos;
   }


/**
 * Returns a <code>GrowablePipedOutputStream</code> that is connected to a
 * <code>MultiplexingInputStream</code>.
 * It will create the <code>MultiplexingInputStream</code> if necessary.  This method exists to
 * allow <code>InputMultiplexor</code> to get a place to put bytes directed to an unrecognized
 * SocketId, which might be necessary if the remote socket starts writing before this
 * end of the connection is ready.  Originally, we had a three step handshake, in which
 * (1) the client socket sent a "connect" message, (2) the server socket sent a "connected"
 * message, and (3) the client socket sent a "connect verify" message.  In the interests
 * of performance we eliminated the final step.
 *
 * @param socketId
 * @return
 * @throws IOException
 */
   public GrowablePipedOutputStream getConnectedOutputStream(SocketId socketId) throws IOException
   {
      if (debug) log.debug("getConnectedOutputStream(): " + socketId.getPort());

      MultiplexingInputStream mis = (MultiplexingInputStream) inputStreamMap.get(socketId);
      if (mis != null)
      {
         GrowablePipedOutputStream pos = (GrowablePipedOutputStream) outputStreamMap.get(socketId);
         if (pos == null)
         {
            StringBuffer message = new StringBuffer();
            message.append("MultiplexingInputStream exists ")
                   .append("without matching GrowablePipedOutputStream: ")
                   .append("socketId = ").append(socketId);
            throw new IOException(message.toString());
         }
         return pos;
      }

      GrowablePipedOutputStream pos = (GrowablePipedOutputStream) outputStreamMap.get(socketId);
      if (pos == null)
      {
         pos = new GrowablePipedOutputStream();
         outputStreamMap.put(socketId, pos);
      }

      mis = new MultiplexingInputStream(pos, this);
      inputStreamMap.put(socketId, mis);
      return pos;
   }


/**
 * @return
 */
   public OutputStream getBackchannelOutputStream()
   {
      return backchannelOutputStream;
   }


/**
 * @return handshakeCompletedEvent
 */
   public HandshakeCompletedEvent getHandshakeCompletedEvent()
   {
      return handshakeCompletedEvent;
   }
/**
 *
 * @return
 */
   public OutputMultiplexor getOutputMultiplexor()
   {
      return outputMultiplexor;
   }


/**
 *
 * @param socketId
 * @return
 */
   public OutputStream getOutputStreamByLocalSocket(SocketId socketId)
   {
      return (OutputStream) outputStreamMap.get(socketId);
   }


/**
 *
 * @return
 */
   public Protocol getProtocol()
   {
      return protocol;
   }


/**
 * @return
 */
   public synchronized ServerSocket getServerSocket()
   {
      return serverSocket;
   }


/**
 *
 * @return
 */
   public Socket getSocket()
   {
      return socket;
   }


/**
 *
 * @param socketId
 * @return
 */
   public VirtualSocket getSocketByLocalPort(SocketId socketId)
   {
      return (VirtualSocket) socketMap.get(socketId);
   }


/**
 * @return
 */
   public SocketFactory getSocketFactory()
   {
      return socketFactory;
   }


/**
 * To implement <code>HandshakeCompletedListener</code> interface.
 */
   public void handshakeCompleted(HandshakeCompletedEvent event)
   {
      description = socket.toString();

      handshakeCompletedEvent = event;
      Object obj = configuration.get(Multiplex.SSL_HANDSHAKE_LISTENER);
      if (obj != null)
      {
         HandshakeCompletedListener listener = (HandshakeCompletedListener) obj;
         listener.handshakeCompleted(event);
      }
   }


/**
 * @return
 */
   public boolean isBound()
   {
      return bound;
   }


/**
 * @return
 */
   public boolean isConnected()
   {
      return connected;
   }


/**
 * @return
 */
   public synchronized boolean isServerSocketRegistered()
   {
      return serverSocket != null;
   }


/**
 *
 */
   public boolean isShutdown()
   {
      return shutdown;
   }


/**
 *
 * @param socketId
 * @return
 * TODO: isn't used
 */
   public synchronized boolean isSocketRegistered(SocketId socketId)
   {
      return registeredSockets.contains(socketId);
   }


/**
 * @return
 */
   public boolean respondToShutdownRequest()
   {
      return shutdownManager.respondToShutdownRequest();
   }


/**
 * @param socketFactory
 */
   public void setSocketFactory(SocketFactory socketFactory)
   {
      this.socketFactory = socketFactory;
   }


/**
 * @return
 */
   public int getShutdownMonitorPeriod()
   {
      return shutdownMonitorPeriod;
   }


/**
 * @return
 */
   public int getShutdownRefusalsMaximum()
   {
      return shutdownRefusalsMaximum;
   }


/**
 * @return
 */
   public int getShutdownRequestTimeout()
   {
      return shutdownRequestTimeout;
   }


/**
 * @param timeout
 */
   public void setShutdownRequestTimeout(int timeout)
   {
      shutdownRequestTimeout = timeout;
   }


/**
 * @param maximum
 */
   public void setShutdownRefusalsMaximum(int maximum)
   {
      shutdownRefusalsMaximum = maximum;
   }


/**
 * @param period
 */
   public void setShutdownMonitorPeriod(int period)
   {
      shutdownMonitorPeriod = period;
   }


/**
 * Needed to implement <code>OutputMultiplexor.OutputMultiplexorClient</code>
 */
   public synchronized void outputFlushed()
   {
      if (shutdownThread != null)
         shutdownThread.setSafeToShutdown(true);
      notifyAll();
   }


   public String toString()
   {
      if (description != null)
         return description;
      return super.toString();
   }

/********************************************************************************************
 *                     	protected methods and classes
 ********************************************************************************************/


   protected Socket createSocket(InetSocketAddress endpoint, int timeout) throws IOException
   {
      Socket socket = null;

      if (localSocketAddress == null)
      {
         if (socketFactory != null)
            socket = socketFactory.createSocket(endpoint.getAddress(), endpoint.getPort());
         else
            socket = SocketChannel.open(endpoint).socket();
      }
      else
      {
         // It's possible that bind() was called, but a socket hasn't been created yet, since
         // SSLSocketFactory will not create an unconnected socket.  In that case, bind() just
         // saved localSocketAddress for later use.
         if (socketFactory != null)
            socket = socketFactory.createSocket(endpoint.getAddress(),
                                                endpoint.getPort(),
                                                localSocketAddress.getAddress(),
                                                localSocketAddress.getPort());
         else
         {
            socket = SocketChannel.open().socket();
            socket.bind(localSocketAddress);
            socket.connect(endpoint);
         }
      }

      if (socket instanceof SSLSocket)
      {
//         Object o = configuration.get(Multiplex.SSL_HANDSHAKE_LISTENER);
//         if (o != null)
//         {
//            HandshakeCompletedListener hcl = (HandshakeCompletedListener) o;
//            ((SSLSocket) socket).addHandshakeCompletedListener(hcl);
//         }
         ((SSLSocket) socket).addHandshakeCompletedListener(this);
      }

      socket.setSoTimeout(timeout);
      return socket;
   }

   protected Socket createSocket() throws IOException
   {
      Socket socket = null;

      try
      {
         if (socketFactory != null)
            socket = socketFactory.createSocket();
         else
            socket = SocketChannel.open().socket();

         if (socket instanceof SSLSocket)
         {
//            Object o = configuration.get(Multiplex.SSL_HANDSHAKE_LISTENER);
//            if (o != null)
//            {
//               HandshakeCompletedListener hcl = (HandshakeCompletedListener) o;
//               ((SSLSocket) socket).addHandshakeCompletedListener(hcl);
//            }
            ((SSLSocket) socket).addHandshakeCompletedListener(this);
         }
      }
      catch (IOException e)
      {
         if ("Unconnected sockets not implemented".equals(e.getMessage()))
            return null;
         throw e;
      }

      return socket;
   }


/**
 * @param address
 */
   protected void registerByLocalAddress(InetSocketAddress address)
   {
      synchronized (localAddressMapLock)
      {
         localSocketAddress = address;
         HashSet managers = (HashSet) managersByLocalAddress.get(address);

         if (managers == null)
         {
            managers = new HashSet();
            managersByLocalAddress.put(address, managers);
         }

         managers.add(this);

         // allow searching on any local address
         localWildCardAddress = new InetSocketAddress(address.getPort());
         managers = (HashSet) managersByLocalAddress.get(localWildCardAddress);

         if (managers == null)
         {
            managers = new HashSet();
            managersByLocalAddress.put(localWildCardAddress, managers);
         }

         managers.add(this);
      }
   }


/**
 *
 */
   protected void unregisterByLocalAddress()
   {
      synchronized (localAddressMapLock)
      {
         HashSet managers = null;

         if (localSocketAddress != null)
         {
            managers = (HashSet) managersByLocalAddress.get(localSocketAddress);

            if (managers != null)
            {
               managers.remove(this);

               if (managers.isEmpty())
                  managersByLocalAddress.remove(localSocketAddress);
            }
         }

         if (localWildCardAddress != null)
         {
            managers = (HashSet) managersByLocalAddress.get(localWildCardAddress);

            if (managers != null)
            {
               managers.remove(this);

               if (managers.isEmpty())
                  managersByLocalAddress.remove(localWildCardAddress);
            }
         }
      }
   }


/**
 *
 * @param address
 */
   protected void registerByRemoteAddress(InetSocketAddress address)
   {
      remoteSocketAddress = address;

      synchronized (remoteAddressMapLock)
      {
         HashSet managers = (HashSet) managersByRemoteAddress.get(address);

         if (managers == null)
         {
            managers = new HashSet();
            managers.add(this);
            managersByRemoteAddress.put(address, managers);
         }
         else
            managers.add(this);
      }
   }


/**
 *
 */
   protected void unregisterByRemoteAddress()
   {
      if (remoteSocketAddress != null)
      {
         synchronized (remoteAddressMapLock)
         {
            HashSet managers = (HashSet) managersByRemoteAddress.get(remoteSocketAddress);

            if (managers != null)
            {
               managers.remove(this);

               if (managers.isEmpty())
                  managersByRemoteAddress.remove(remoteSocketAddress);
            }
         }
      }
   }


/**
 *
 * @param address
 */
   protected void registerShareable(InetSocketAddress address)
   {
      if (debug) log.debug("registering as shareable: " + this + ": " +  address.toString());
      synchronized (shareableMapLock)
      {
         HashSet managers = (HashSet) shareableManagers.get(address);

         if (managers == null)
         {
            managers = new HashSet();
            managers.add(this);
            shareableManagers.put(address, managers);
         }
         else
            managers.add(this);
      }
   }


/**
 *
 */
   protected void unregisterShareable()
   {
      if (debug) log.debug("unregistering remote: " + this + ": " + description);
      if (remoteSocketAddress != null)
      {
         synchronized (shareableMapLock)
         {
            HashSet managers = (HashSet) shareableManagers.get(remoteSocketAddress);

            if (managers != null)
            {
               managers.remove(this);

               if (managers.isEmpty())
                  shareableManagers.remove(remoteSocketAddress);
            }
         }
      }
   }


/*
 *
 */
   protected void unregisterAllMaps()
   {
      unregisterByLocalAddress();
      unregisterByRemoteAddress();
      unregisterShareable();
   }

/**
 * @param socketId
 */
   protected void removeAnInputStream(SocketId socketId)
   {
      if (debug) log.debug("entering removeAnInputStream(): " + socketId.getPort());
      InputStream is = (InputStream) inputStreamMap.remove(socketId);
      OutputStream os = (OutputStream) outputStreamMap.remove(socketId);

      if (is != null)
      {
         try
         {
            is.close();
         }
         catch (Exception ignored)
         {
            log.error("error closing PipedInputStream (" + socket.getPort() + ")", ignored);
         }
      }

      if (os != null)
      {
         try
         {
            os.close();
         }
         catch (Exception ignored)
         {
            log.error("error closing PipedOutputStream (" + socket.getPort() + ")", ignored);
         }
      }
   }


   protected void setReadException(IOException e)
   {
      // Note.  It looks like there could be a race between setReadException() and
      // getAnInputStream().  However, suppose getAnInputStream() gets to its test
      // (readException != null) before readException is set here. Then if it created a
      // new InputStream "is", setReadException() will see "is" in inputStreamMap and will
      // set its read exception.  Suppose getAnInputStream gets to its test
      // (readException != null) after setReadException() sets readException.  Then
      // if it created a new InputStream "is", it will set the read exception for "is".

      // Remove from shareable map (if it's in map).
      unregisterAllMaps();
      notifySocketsOfException();

      // Unregister with input thread.
      if (multiGroupInputThread != null)
         multiGroupInputThread.unregisterSocketGroup(this);

      readException = e;

      HashSet tempSet;
      synchronized (inputStreamMap)
      {
         tempSet = new HashSet(inputStreamMap.values());
      }

      Iterator it = tempSet.iterator();
      while (it.hasNext())
      {
         MultiplexingInputStream is = (MultiplexingInputStream) it.next();
         is.setReadException(e);
      }
   }


   protected void setWriteException(IOException e)
   {
      // Note.  See Note in setReadException().
      // If this connection is unusable, take out of shareable map (if it's in shareable map).
      unregisterAllMaps();
      notifySocketsOfException();

      // Unregister with output thread.
      outputMultiplexor.unregister(this);

      writeException = e;

      HashSet tempSet;
      synchronized (outputStreamMap)
      {
         tempSet = new HashSet(outputStreamSet);
      }

      Iterator it = tempSet.iterator();
      while (it.hasNext())
      {
         MultiplexingOutputStream os = (MultiplexingOutputStream) it.next();
         os.setWriteException(e);
      }
   }

   
   protected void notifySocketsOfException()
   {
      synchronized (socketMap)
      {
         Iterator it = socketMap.values().iterator();
         while (it.hasNext())
            ((VirtualSocket) it.next()).notifyOfException();
      }
   }


   protected void setEOF()
   {
      // Note.  See Note in setReadException().
      log.debug("setEOF()");
      HashSet tempSet;
      synchronized (inputStreamMap)
      {
         tempSet = new HashSet(inputStreamMap.values());
      }

      Iterator it = tempSet.iterator();
      while (it.hasNext())
      {
         MultiplexingInputStream is = (MultiplexingInputStream) it.next();
         try
         {
            is.handleRemoteShutdown();
         }
         catch (IOException e)
         {
            log.error(e);
         }
      }
   }


/**
 *
 */
   protected synchronized void shutdown()
   {
      if (debug) log.debug(description + ": entering shutdown()");
      shutdownThread = new ShutdownThread();
      shutdownThread.setName(shutdownThread.getName() + ":shutdown");
      shutdownThread.start();
   }


/**
 * The motivation behind this class is to prevent the following problem.  Suppose MultiplexingManager A
 * is connected to MultiplexingManager B, and A decides to shut down.  Suppose A shuts down before B knows A
 * is shutting down, and suppose a VirtualSocket C starts up, finds B, and attaches itself to B.  Then C
 * will have "died a-borning," in the words of Tom Paxton.  We need a handshake protocol to ensure a
 * proper shutdown process.  In the following, let A be the local MultiplexingManager and let B be its
 * remote peer.
 *
 * There are two forms of synchronization in ShutdownManager.  incrementReferences() and decrementReferences()
 * maintain the reference count to its MultiplexingManager, and of course the changes to variable
 * referenceCount have to be synchronized.  Parallel to incrementReferences() and decrementReferences() are
 * the pair of methods reseverManager() and unreserveManager(), which are similar but intended for holding a
 * MultiplexingManger for more temporary applications.  See, for example, getaManagerByRemoteAddress().
 *
 * There is also a need for distributed synchronization. When decrementReferences() on A decrements the
 * referenceCount to 0, it indicates to B the desire of A to shut down. Since all of the virtual sockets
 * on A are connected to virtual sockets on B, normally the request would be honored.  However, if a new virtual
 * socket attaches itself to B, then it would have > 0 clients, and it would refuse the request to shut down.
 * In any case, the request is made through Protocol.requestManagerShutdown(), which results in a call to
 * ShutdownManager.respondToShutdownRequest() on B, which is synchronized since it reads the
 * readyToShutdown variable, which is modified by decrementReferences().  Here lies the danger of
 * distributed deadlock.  If decrementReferences() on A and B both start executing at about the same time, they
 * would both be waiting for a response from respondToShutdownRequest().  But each respondToShutdownRequest()
 * would be locked out because each decrementReferences() is blocked on i/o.
 *
 * The solution is to put the i/o operation in a separate thread, ShutdownRequestThread, and have decrementReferences()
 * enter a wait state, allowing respondToShutdownRequest() to execute.  So, on each end respondToShutdownRequest()
 * will return an answer ("go ahead and shut down", in particular), and each ShutdownRequestThread.run() will wake up
 * the waiting decrementReferences(), which will then run to completion.
 *
 * Note also that while decrementReferences() is waiting, incrementReferences() can run.  However, before it waits,
 * decrementReferences() sets the shutdownRequestInProgress flag, and if incrementReferences() finds the flag set, it
 * will also enter a wait state and will take no action until the outstanding shutdown request is accepted or
 * rejected.
 *
 * Another issue is what to do if MultiplexingManager B responds negatively to A's request to shut down, not
 * because it has a new client but simply because some of its virtual sockets just haven't gotten around to
 * closing yet.  When B's referenceCount finally goes to 0, it will send a shutdown request to A, and if A's
 * referenceCount is still 0, B will shut down.  But what about B?  If decrementReferences() gets a negative
 * response, it will start up a ShutdownMonitorThread, which, as long as readyToShutdown remains true, will
 * periodically check to see if remoteShutdown has been set to true.  If it has, ShutdownMonitorThread
 * initiates the shut down of its enclosing MultiplexingManager.
 *
 * reserveManager() interacts with decrementReferences() by preventing decrementReferences() from committing to
 * shutting down.  If reserveManager() runs first, it sets the flag reserved to true, which causes
 * decrementReferences() to return without checking for referenceCount == 0.  If decrementReferences() runs
 * first and finds referenceCount == 0 and gets a positve response from the remote manager, then reserveManager()
 * will throw an IOException.  But if decrementReferences() gets a negative response, it will start up a
 * ShutdownMonitorThread, which runs as long as the flag readyToShutdown is true.  But reserveManager() will
 * set readyToShutdown to false, ending the ShutdownMonitorThread.  When unreserveManager() eventually runs
 * and sees referenceCount == 0, it will increment referenceCount and call decrementReferences(), allowing the
 * shutdown attempt to proceed anew.  Note that when incrementReferences() runs successfully, it sets the flag
 * reserved to false, since incrementing referenceCount will also keep the MultiplexingManager alive.
 */
   protected class ShutdownManager
   {
      /** referenceCount keeps track of the number of clients of ShutdownManager's enclosing
       *  MultiplexingManager. */
      private int referenceCount = 1;

      /** reserved is set to true to prevent the manager from shutting down without incrementing
       *  the reference count. */
      private boolean reserved = false;

      /** shutdownRequestInProgress remains true while a remote shutdown request is pending */
      private boolean shutdownRequestInProgress = false;

      /** readyToShutdown is set to true as long as long as referenceCount == 0.  It is interpreted
       *  by respondToShutdownRequest() as the local manager's willingness to shut down. */
      private boolean readyToShutdown = false;

      /** shutdownMonitorThread holds a reference to the most recently created ShutdownMonitorThread. */
//      ShutdownMonitorThread shutdownMonitorThread;
      ShutdownMonitorTimerTask shutdownMonitorTimerTask;

      /** shutdown is set to true when the irrevocable decision has been made to shut down.  Once it
       *  is set to true, it is never set to false. */
      private boolean shutdown = false;

      /** remoteShutdown is set to true when the remote manager makes a shutdown request and
       *  respondToShutdownRequest(), its agent for the request, discovers that the local manager
       *  is also willing to shut down.  It represents the fact that the remote manager will respond
       *  by deciding to shut down. */
      private boolean remoteShutdown = false;

      /** shutdownHandled is set to true when ShutdownMonitorTimerTask begins the
       *  shut down process */
      private boolean shutdownHandled;

      /** requestShutdownFailed is true if and only if ShutdownRequestThread's attempt
       *  to get a response to a shut down request failed */
      private boolean requestShutdownFailed;


      private class ShutdownRequestThread extends Thread
      {
         public ShutdownRequestThread()
         {
            shutdownRequestInProgress = true;
         }

         public void run()
         {
            try
            {
               // Note. The timeout for Protocol's input stream should be longer than the
               // time spent in decrementReferences() or by ShutdownMonitorTimerTask waiting
               // for a response.
               shutdown = protocol.requestManagerShutdown(shutdownRequestTimeout * 2);
               if (debug) log.debug("shutdown: " + shutdown);
            }
            catch (SocketTimeoutException e)
            {
               requestShutdownFailed = true;
               log.debug("socket timeout exception in manager shutdown request");
            }
            catch (Exception e)
            {
               requestShutdownFailed = true;
               log.debug("i/o exception in manager shutdown request", e);
            }

            if (debug) log.debug("ShutdownRequestThread.run() done: " + shutdown);
            shutdownRequestInProgress = false;

            synchronized(ShutdownManager.this)
            {
               ShutdownManager.this.notifyAll();
            }
         }
      }


/**
 * It is possible,due to a race condition, for there to be multiple ShutdownMonitorThreads running.
 * In particular, suppose
 * <p>
 * <ol>
 *   <li> decrementReferences() starts a ShutdownMonitorThread T1,
 *   <li> reserverManager() runs and calls T1.terminate(),
 *   <li> unreserveManager() runs, sees referenceCount == 0, and calls decrementReferences(),
 *   <li> decrementReferences() creates a new ShutdownMonitorThread T2,
 *   <li> T1 leaves sleep(1000) finds remoteShutdown == true and calls shutdown(), and
 *   <li> T2 finds remoteShutdown == true.
 * </ol>
 * <p>
 * For this reason, we turn on shutdown before calling shutdown() in T1, so that T2 will
 * not call shutdown().
 *
 */
//      private class ShutdownMonitorThread extends Thread
//      {
//         boolean running = true;
//
//         public void terminate()
//         {
//            running = false;
//         }
//
//         public void run()
//         {
//            log.debug(socket.toString() + ": entering ShutdownMonitorThread");
//
//            while (running)
//            {
//               try
//               {
//                  sleep(1000);
//               }
//               catch (InterruptedException ignored) {}
//
//               synchronized (ShutdownManager.this)
//               {
//                  if (readyToShutdown && remoteShutdown && !shutdown)
//                  {
//                     log.debug("ShutdownMonitorThread: found remoteShutdown == true");
//                     shutdown = true;
//                     shutdown();
//                     ShutdownManager.this.notifyAll();
//                     return;
//                  }
//               }
//            }
//         }
//      }


      private class ShutdownMonitorTimerTask extends TimerTask
      {
         int count;
         boolean cancelled;

         public boolean cancel()
         {
            log.debug("cancelling ShutdownMonitorTimerTask");
            cancelled = true;
            return super.cancel();
         }

         public void run()
         {
            if (debug) log.debug(description + ": entering ShutdownMonitorTimerTask");
            count++;

            synchronized (ShutdownManager.this)
            {
               // Another ShutdownMonitorTimerTask got here first.
               if (shutdownHandled)
               {
                  if (debug) log.debug(description + ": shutdownHandled == true");
                  cancel();
               }

               // ShutdownRequestThread got a positive response.
               else if (shutdown)
               {
                  if (debug) log.debug(description + ": shutdown is true");
                  shutdownHandled = true;
                  shutdown();
                  cancel();
               }

               // Peer MultiplexingManager requested shutdown consent.
               else if (readyToShutdown && remoteShutdown)
               {
                  if (debug) log.debug(description + ": ShutdownMonitorTimerTask: found remoteShutdown == true");
                  shutdown = true;
                  shutdownHandled = true;
                  shutdown();
                  ShutdownManager.this.notifyAll();
                  cancel();
               }

               // Timeout (or other error) in ShutdownRequestThread
               else if (requestShutdownFailed)
               {
                  if (debug) log.debug(description + ": ShutdownMonitorTimerTask: found requestShutdownFailed == true");
                  shutdown = true;
                  shutdownHandled = true;
                  shutdown();
                  ShutdownManager.this.notifyAll();
                  cancel();
               }

               // Count of peer MultiplexingManager refusals has reached maximum.
               // Assume peer is hung up somehow and shut down.
               else if (count > shutdownRefusalsMaximum)
               {
                  if (debug)
                     log.debug(description + ": ShutdownMonitorTimerTask: " +
                              "shutdown refusal count exceeded maximut: " + shutdownRefusalsMaximum);

                  shutdown = true;
                  shutdownHandled = true;
                  shutdown();
                  ShutdownManager.this.notifyAll();
                  cancel();
               }

               // ShutdownRequestThread is still running.
               else if (shutdownRequestInProgress)
               {
                  if (debug) log.debug(description + ": shutdownRequestInProgress == true");
                  return;
               }

               // ShutdownRequestThread got a negative response.  If we haven't been cancelled
               // yet, we still are trying to shut down.  Ask again.
               else
               {
                  // Note. The timeout for Protocol's input stream should be longer than the
                  // time spent waiting for a response.
                  ShutdownRequestThread shutdownRequestThread = new ShutdownRequestThread();
                  shutdownRequestThread.setName(shutdownRequestThread.getName() + ":shutdownRequest:" + time);
                  shutdownRequestThread.setDaemon(true);
                  if (debug) log.debug(description + ": starting ShutdownRequestThread: " + shutdownRequestThread.toString());
                  shutdownRequestThread.start();
               }
            }
         }

         public String toString()
         {
            return "shutdownRequest:" + time;
         }
      }

/**
 *
 * @throws IOException
 */
      public synchronized void reserveManager() throws IOException
      {
         if (debug) log.debug(description + referenceCount);

         // If decrementReferences() grabbed the lock first, set referenceCount to 0, and initiated a
         // remote shutdown request, wait until the answer comes back.
         while (shutdownRequestInProgress)
         {
            try
            {
               wait();
            }
            catch (InterruptedException e)
            {
               // shouldn't happen
               log.error("interruption in ShutdownRequestThread");
            }
         }

         // 1. shutdown is true if and only if (a) ShutdownRequestThread returned an indication that the
         //    remote manager is shutting down, or (b) the wait() in decrementReferences() for the return
         //    of ShutdownRequestThread timed out and decrementReferences() set shutdown to true;
         // 2. remoteShutdown is true if and only if the remote manager initiated a shutdown request
         //    which found that the local manager was ready to shut down. In this case, the remote
         //    manager would go ahead and shut down, and the local manager will inevitably shutdown as well.

         if (shutdown || remoteShutdown)
            throw new IOException("manager shutting down");

         readyToShutdown = false;
         reserved = true;

//         if (shutdownMonitorThread != null)
//            shutdownMonitorThread.terminate();

         if (shutdownMonitorTimerTask != null)
            shutdownMonitorTimerTask.cancel();

         // wake up decrementReferences() if it is waiting
         notifyAll();
      }


/**
 *
 */
      public synchronized void unreserveManager()
      {
         if (debug) log.debug(description + referenceCount);

         if (!reserved)
         {
            log.error("attempting to unreserve a MultiplexingManager that was not reserved: " + description);
            return;
         }

         reserved = false;

         // If referenceCount == 0, it is because it was decremented by decrementReferences(). But if
         // reserveManager() was able to run, it was because decrementReferences() did not succeed in
         // negotiating a shutdown.  Either it found reserved == true and gave up, or it received a
         // negative reply from the remote manager and started up a ShutdownMonitorThread, which would
         // have terminated when reserveManager() set readyToShutdown to false.  It is therefore
         // appropriate to give decrementReferences() another opportunity to shut down.
         if (referenceCount == 0)
         {
            referenceCount++;
            decrementReferences();
         }
      }
/**
 *
 */
      public synchronized void incrementReferences() throws IOException
      {
         if (debug) log.debug(description + referenceCount);

         // If decrementReferences() grabbed the lock first, set referenceCount to 0, and initiated a
         // remote shutdown request, wait until the answer comes back.
         while (shutdownRequestInProgress)
         {
            try
            {
               wait();
            }
            catch (InterruptedException e)
            {
               // shouldn't happen
               log.error("interruption in ShutdownRequestThread");
            }
         }

         // 1. shutdown is true if and only if (a) ShutdownRequestThread returned an indication that the
         //    remote manager is shutting down, or (b) the wait() in decrementReferences() for the return
         //    of ShutdownRequestThread timed out and decrementReferences() set shutdown to true;
         // 2. remoteShutdown is true if and only if the remote manager initiated a shutdown request
         //    which found that the local manager was ready to shut down. In this case, the remote
         //    manager would go ahead and shut down, and the local manager will inevitably shutdown as well.

         if (shutdown || remoteShutdown)
            throw new IOException("not accepting new clients");

         readyToShutdown = false;
         reserved = false;
         referenceCount++;

         if (debug) log.debug(description + referenceCount);

//         if (shutdownMonitorThread != null)
//            shutdownMonitorThread.terminate();

         if (shutdownMonitorTimerTask != null)
            shutdownMonitorTimerTask.cancel();

         // wake up decrementReferences() if it is waiting
         notifyAll();
      }


/**
 *
 */
      public synchronized void decrementReferences()
      {
         referenceCount--;
         if (debug) log.debug(description + referenceCount);

         if (reserved)
         {
            if (debug) log.debug(description + ": reserved == true");
            return;
         }

         if (referenceCount == 0)
         {
            readyToShutdown = true;

            if (isConnected())
            {
               ShutdownRequestThread shutdownRequestThread = new ShutdownRequestThread();
               shutdownRequestThread.setName(shutdownRequestThread.getName() + ":shutdownRequest:" + time);
               shutdownRequestThread.setDaemon(true);
               if (debug) log.debug(description + "starting ShutdownRequestThread: " + shutdownRequestThread.toString());
               shutdownRequestThread.start();

               try
               {
                  // Note. The timeout for Protocol's input stream should be longer than the
                  // time spent waiting for a response.
                  wait(shutdownRequestTimeout);
               }
               catch (InterruptedException e)
               {
                  // shouldn't happen
                  log.error("interrupt in ShutdownRequestThread");
               }

               if (log.isDebugEnabled())
               {
                  log.debug(description + shutdown);
                  log.debug(description + shutdownRequestThread.isAlive());
               }

               // If shutdownRequestInProgress is still true, we assume that the peer MultiplexingManager
               // has shut down or is inaccessible, and we shut down.
               if (shutdownRequestInProgress)
               {
                  shutdown = true;

                  // turn off shutdownRequestInProgress in case incrementReferences() or reserveManager()
                  // are waiting
                  shutdownRequestInProgress = false;
               }
            }
            else // !isConnected()
               shutdown = true;


            if (shutdown)
            {
               shutdown();

               // wake up incrementReferences() if it is waiting
               notifyAll();
            }
            else
            {
//               shutdownMonitorThread = new ShutdownMonitorThread();
//               shutdownMonitorThread.setName(shutdownMonitorThread.getName() + ":shutdownMonitor");
//               shutdownMonitorThread.start();

               shutdownMonitorTimerTask = new ShutdownMonitorTimerTask();
               if (debug) log.debug(description + ": scheduling ShutdownMonitorTask: " + shutdownMonitorTimerTask);
               timer.schedule(shutdownMonitorTimerTask, shutdownMonitorPeriod, shutdownMonitorPeriod);
            }
         }
      }


/**
 * @return
 */
      protected synchronized boolean respondToShutdownRequest()
      {
         if (debug)
         {
            log.debug(description + readyToShutdown);
            log.debug(description + shutdown);
         }

         if (readyToShutdown)
         {
            remoteShutdown = true;
            if (debug) log.debug(description + ": respondToShutdownRequest(): set remoteShutdown to true");
         }

         return readyToShutdown;
      }


/**
 * @return
 */
      protected boolean isShutdown()
      {
         return shutdown;
      }
   }


/**
 *
 */
   protected class ShutdownThread extends Thread
   {
      private boolean safeToShutDown;

      public void run()
      {
         String message = null;
         if (debug) log.debug(description + ": manager shutting down");

         // Unregister this MultiplexingManager by local address(es)
         unregisterByLocalAddress();

         // Unregister this MultiplexingManager by remote address
         unregisterByRemoteAddress();

         // Remove this MultiplexingManager from Map of shareable managers
         unregisterShareable();

         if (socket != null)
         {
            try
            {
               if (outputMultiplexor != null)
               {
                  outputMultiplexor.unregister(MultiplexingManager.this);

                  // Don't close socket until all output has been written.
                  synchronized (MultiplexingManager.this)
                  {
                     while (!safeToShutDown)
                     {
                        if (debug) log.debug("waiting for safe to shut down");
                        try
                        {
                           MultiplexingManager.this.wait();
                        }
                        catch (InterruptedException ignored)
                        {
                        }
                     }
                  }
               }

               if (socket.getChannel() == null)
                  socket.close();
               else
               {
//                  socket.getChannel().close();
                  message = description;

                  if (multiGroupInputThread != null)
                     multiGroupInputThread.unregisterSocketGroup(MultiplexingManager.this);

                  socket.close();
                  if (debug) log.debug("closed socket: " + description);
//                  multiGroupInputThread.unregisterSocketGroup(MultiplexingManager.this);
//                  socket.close();
               }

               log.debug("manager: closed socket");
            }
            catch (Exception e)
            {
               log.error("manager: unable to close socket", e);
            }
         }

         if (inputThread != null)
         {
            inputThread.shutdown();

            try
            {
               inputThread.join();
               log.debug("manager: joined input thread");
            }
            catch (InterruptedException ignored)
            {
               log.debug("manager: interrupted exception waiting for read thread");
            }
         }

         removeAnInputStream(SocketId.PROTOCOL_SOCKET_ID);
         removeAnInputStream(SocketId.SERVER_SOCKET_ID);
         removeAnInputStream(SocketId.SERVER_SOCKET_CONNECT_ID);
         removeAnInputStream(SocketId.SERVER_SOCKET_VERIFY_ID);
         removeAnInputStream(SocketId.BACKCHANNEL_SOCKET_ID);

         shutdown = true;

         // Remove this MultiplexManager from set of all managers
         if (debug) log.debug("removing from allManagers: " + description + "(" + id + ")");
         allManagers.remove(MultiplexingManager.this);

         if (debug) log.debug("manager shut down (: " + id + "): "  + message);
         if (debug) log.debug("managers left: " + allManagers.size());
      }

      public void setSafeToShutdown(boolean safe)
      {
         if (debug) log.debug("output flushed");
         safeToShutDown = safe;
      }
   }


   protected static class PendingActionThread extends StoppableThread
   {
      private List pendingActionsTemp = new ArrayList();

      protected void doInit()
      {
         log.debug("PendingActionThread starting");
      }

      protected void doRun()
      {
         synchronized (pendingActions)
         {
            while (pendingActions.isEmpty())
            {
               try
               {
                  pendingActions.wait();
               }
               catch (InterruptedException ignored)
               {
                  if (!isRunning())
                     return;
               }
            }

            pendingActionsTemp.addAll(pendingActions);
            pendingActions.clear();
         }

         Iterator it = pendingActionsTemp.iterator();

         while (it.hasNext())
         {
            Object o = it.next();
            if (o instanceof PendingAction)
               ((PendingAction) o).doAction();
            else
               log.error("object in closePendingSockets has invalid type: " + o.getClass());
         }

         pendingActionsTemp.clear();
      }

      public void shutdown()
      {
         log.debug("pending action thread beginning shut down");
         super.shutdown();
         interrupt();
      }

      protected void doShutDown()
      {
         log.debug("PendingActionThread shutting down");
      }
   }
}