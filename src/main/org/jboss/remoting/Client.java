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

package org.jboss.remoting;

import org.jboss.logging.Logger;
import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.CallbackPoller;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.invocation.InternalInvocation;
import org.jboss.remoting.invocation.OnewayInvocation;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.stream.StreamServer;
import org.jboss.remoting.transport.BidirectionalClientInvoker;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.transport.local.LocalClientInvoker;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.util.id.GUID;
import org.jboss.util.threadpool.BasicThreadPool;
import org.jboss.util.threadpool.BlockingMode;
import org.jboss.util.threadpool.ThreadPool;

import javax.net.SocketFactory;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.rmi.MarshalException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Client is a convience class for invoking remote methods for a given subsystem. It is intended to
 * be the main user interface for making remote invocation on the client side.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 *
 * @version $Revision: 5881 $
 */
public class Client implements Externalizable
{
   // Constants ------------------------------------------------------------------------------------

   /**
    * Key to be used to determine if invocation is to be oneway (async).
    */
   public static final String ONEWAY_FLAG = "oneway";

   /**
    * Key to be used when tracking callback listeners.
    */
   public static final String LISTENER_ID_KEY = "listenerId";

   /**
    * Specifies the default number of work threads in the pool for executing one way invocations on
    * the client. Value is 10.
    */
   public static final int MAX_NUM_ONEWAY_THREADS_DEFAULT = 10;

   /**
    * The key to use for the metadata Map passed when making a invoke() call and wish for the
    * invocation payload to be sent as is and not wrapped within a remoting invocation request
    * object. This should be used when want to make direct calls on systems outside of remoting
    * (e.g. making a http POST request to a web service).
    */
   public static final String RAW = "rawPayload";

   /**
    * Key for the configuration map passed to the Client constructor to indicate that client should
    * make initial request to establish lease with server. The value for this should be either a
    * String that java.lang.Boolean can evaluate or a java.lang.Boolean. Client leasing is turned
    * off by default, so would need to use this property to turn client leasing on.
    */
   public static final String ENABLE_LEASE = "enableLease";

   /**
    * Key for the configuration map passed to the Client constructor providing a ssl
    * javax.net.ssl.HandshakeCompletedListener implementation, which will be called on when ssl
    * handshake completed with server.
    */
   public static final String HANDSHAKE_COMPLETED_LISTENER = "handshakeCompletedListener";

   /**
    * Key for the configuration when adding a callback handler and internal callback server
    * connector is created.  The value should be the transport protocol to be used. By default will
    * use the same protocol as being used by this client (e.g. http, socket, rmi, multiplex, etc.)
    */
   public static final String CALLBACK_SERVER_PROTOCOL = "callbackServerProtocol";

   /**
    * Key for the configuration when adding a callback handler and internal callback server
    * connector is created.  The value should be the host name to be used. By default will use the
    * result of calling InetAddress.getLocalHost().getHostAddress().
    */
   public static final String CALLBACK_SERVER_HOST = "callbackServerHost";

   /**
    * Key for the configuration when adding a callback handler and internal callback server
    * connector is created.  The value should be the port to be used.  By default will find a random
    * unused port.
    */
   public static final String CALLBACK_SERVER_PORT = "callbackServerPort";

   /**
    * Key for the configuration map that determines the threadpool size for asynchrouous invocations.
    */
   public static final String MAX_NUM_ONEWAY_THREADS = "maxNumThreadsOneway";

   /**
    * Key for the configuration map that determines the queue size for waiting asynchronous
    * invocations.
    */
   public static final String MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE = "maxOnewayThreadPoolQueueSize";

   /**
    * Default timeout period for network i/o in disconnect() and removeListener().
    * -1 indicates that no special per invocation timeout will be set.
    */
   public static final int DEFAULT_DISCONNECT_TIMEOUT = -1;
   
   /**
    * Key for setting delay before client invoker is destroyed by disconnect().
    */
   public static final String INVOKER_DESTRUCTION_DELAY = "invokerDestructionDelay";
   
   public static final String THROW_CALLBACK_EXCEPTION = "throwCallbackException";
   
   private static Map connectionValidators = new HashMap();
   private static Object connectionValidatorLock = new Object();

   static final String CLIENT = "client";
   static final String CONNECTION_LISTENER = "connectionListener";
   
   /** The key to use to specify that parameters for objects created by Client should be taken,
    *  in addition to the metadata map, from the InvokerLocator and from the Client's configuration map.
    */
   public static final String USE_ALL_PARAMS = "useAllParams";
   
   private static final Logger log = Logger.getLogger(Client.class);
   private static boolean trace = log.isTraceEnabled();

   private static final long serialVersionUID = 5679279425009837934L;
   
   private static Timer invokerDestructionTimer;
   private static Object invokerDestructionTimerLock = new Object();
   
   private static int clientCounter;

   // Static ---------------------------------------------------------------------------------------

   // Attributes -----------------------------------------------------------------------------------

   /**
    * Indicated the max number of threads used within oneway thread pool.
    */
   private int maxNumberThreads = MAX_NUM_ONEWAY_THREADS_DEFAULT;
   private int maxOnewayThreadPoolQueueSize = -1;
   private ClientInvoker invoker;
   private ClassLoader classloader;
   private String subsystem;
   private String sessionId;
   private Object onewayThreadPoolLock = new Object();
   private ThreadPool onewayThreadPool;
   private InvokerLocator locator;

   private ConnectionValidator connectionValidator = null;
   private ConnectionValidatorKey connectionValidatorKey;
   private Map configuration = new HashMap();

   private Map callbackConnectors = new HashMap();
   private Map callbackPollers = new HashMap();

   private Map listeners = new HashMap();

   private SocketFactory socketFactory;

   private int disconnectTimeout = DEFAULT_DISCONNECT_TIMEOUT;

   private boolean connected = false;
   
   private int invokerDestructionDelay = 0;

   private Set connectionListeners = new HashSet();
   
   private boolean useClientConnectionIdentity;
   
   // Constructors ---------------------------------------------------------------------------------

   /**
    * PLEASE DO NOT USE THIS CONSTRUCTOR OR YOUR COMPUTER WILL BURST INTO FLAMES!!!
    * It is only here so can externalize object and will provide a dead object if invoker is not
    * explicitly set. Please use other contructors provided.
    */
   public Client()
   {
   }

   /**
    * Constructs a remoting client with intended target server specified via the locator, without
    * specifing a remote subsystem or including any metadata. Same as calling Client(locator, null,
    * null).
    */
   public Client(InvokerLocator locator) throws Exception
   {
      this(locator, null, null);
   }

   /**
    * Constructs a remoting client with intended target server specified via the locator and
    * configuration metadata.  The metadata supplied will be used when creating client invoker (in
    * the case specific data is required) and also for passing along additional data to connection
    * listeners on the server side in the case that the client fails, will be able to use this extra
    * information when notified.
    */
   public Client(InvokerLocator locator, Map configuration) throws Exception
   {
      this(locator, null, configuration);
   }

   /**
    * Constructs a remoting client with intended target server specified via the locator and
    * intended subsystem on server for invocations to be routed to.
    */
   public Client(InvokerLocator locator, String subsystem) throws Exception
   {
      this(locator, subsystem, null);
   }

   /**
    * Constructs a remoting client with intended target server specified via the locator, intended
    * subsystem on the server for invocations to be routed to, and configuration metadata. The
    * metadata supplied will be used when creating client invoker (in the case specific data is
    * required) and also for passing along additional data to connection listeners on the server
    * side in the case that the client fails, will be able to use this extra information when
    * notified.
    */
   public Client(InvokerLocator locator, String subsystem, Map configuration) throws Exception
   {
      this(null, locator, subsystem, configuration);
   }

   /**
    * Constructs a remoting client with intended target server specified via the locator, intended
    * subsystem on the server for invocations to be routed to, and configuration metadata. The
    * metadata supplied will be used when creating client invoker (in the case specific data is
    * required) and also for passing along additional data to connection listeners on the server
    * side in the case that the client fails, will be able to use this extra information when
    * notified (which will happen when connect() method is called.
    *
    * @param cl - the classloader that should be used by remoting.
    * @deprecated This constructor should not be used any more as will no longer take into account
    *             the classloader specified as a parameter.
    */
   public Client(ClassLoader cl, InvokerLocator locator, String subsystem, Map configuration)
         throws Exception
   {
      if (cl == null)
      {
         this.classloader = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return Thread.currentThread().getContextClassLoader();
            }
         });
      }
      else
      {
         this.classloader = cl;
      }
      this.locator = locator;
      this.subsystem = subsystem == null ? null : subsystem.toUpperCase();
      if (configuration != null)
      {
         this.configuration = new HashMap(configuration);
      }
      this.sessionId = new GUID().toString();
      processParameters();
   }

   /**
    * Constructs a remoting client with intended target server specified via the locator and
    * intended subsystem on server for invocations to be routed to.
    *
    * @deprecated This constructor should not be used any more as will no longer take into account
    *             the classloader specified as a parameter.
    */
   public Client(ClassLoader cl, ClientInvoker invoker, String subsystem) throws Exception
   {
      this.classloader = cl;
      this.subsystem = subsystem == null ? null : subsystem.toUpperCase();
      this.invoker = invoker;
      this.sessionId = new GUID().toString();
   }

   // Externalizable implementation ----------------------------------------------------------------

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      int version = in.readInt();

      switch (version)
      {
         case Version.VERSION_2:
         case Version.VERSION_2_2:
         {
            this.locator = (InvokerLocator) in.readObject();
            this.subsystem = (String) in.readObject();
            this.configuration = (Map) in.readObject();
            boolean wasConnected = in.readBoolean();

            this.classloader = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
            {
               public Object run()
               {
                  return Thread.currentThread().getContextClassLoader();
               }
            });
            
            try
            {
               try
               {
                  this.invoker = (ClientInvoker) AccessController.doPrivileged( new PrivilegedExceptionAction()
                  {
                     public Object run() throws Exception
                     {
                        return InvokerRegistry.createClientInvoker(locator, configuration);
                     }
                  });
               }
               catch (PrivilegedActionException pae)
               {
                  throw pae.getException();
               }
               if(wasConnected)
               {
                  connect();
               }
            }
            catch (Exception e)
            {
               log.debug(e);
               throw new IOException(e.getMessage());
            }

            break;
         }
         default:
            throw new StreamCorruptedException("Unkown version seen: " + version);
      }
   }

   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeInt(Version.getDefaultVersion());
      out.writeObject(invoker != null ? invoker.getLocator() : locator);
      out.writeObject(subsystem);
      out.writeObject(configuration);
      out.writeBoolean(isConnected());
      out.flush();
   }

   // Public ---------------------------------------------------------------------------------------

   /**
    * Adds a connection listener that will be notified if/when the connection to the server fails
    * while the client is idle (no calls being made). The default behavior is to ping for connection
    * every two seconds.
    */
   public void addConnectionListener(ConnectionListener listener)
   {
      HashMap metadata = new HashMap();
      if (configuration.get(ConnectionValidator.VALIDATOR_PING_PERIOD) == null && 
          locator.getParameters().get(ConnectionValidator.VALIDATOR_PING_PERIOD) == null)
      {
         String pingPeriod = Long.toString(ConnectionValidator.DEFAULT_PING_PERIOD);
         metadata.put(ConnectionValidator.VALIDATOR_PING_PERIOD, pingPeriod);
      }
      addConnectionListener(listener, metadata);
   }

   /**
    * Adds a connection listener that will be notified if/when the connection to the server fails
    * while the client is idle (no calls being made). The current behavior is to ping the server
    * periodically.  The time period is defined by the pingPeriod (which should be in milliseconds).
    */
   public void addConnectionListener(ConnectionListener listener, int pingPeriod)
   {
      HashMap metadata = new HashMap();
      metadata.put(ConnectionValidator.VALIDATOR_PING_PERIOD, Integer.toString(pingPeriod));
      addConnectionListener(listener, metadata);
   }
   
   /**
    * Adds a connection listener that will be notified if/when the connection to the server fails
    * while the client is idle (no calls being made). The current behavior is to ping the server
    * periodically.  Various parameters may be specified in metadata.
    * 
    * @see org.jboss.remoting.ConnectionValidator
    */
   public void addConnectionListener(ConnectionListener listener, Map metadata)
   {
      if (invoker == null)
      {
         throw new RuntimeException("Can not add connection listener to remoting client " +
                                    "while client is not connected.");
      }
      else
      {
         // if local, then no point in having connection listener
         if (invoker instanceof LocalClientInvoker)
         {
            return;
         }
      }

      synchronized (connectionValidatorLock)
      {
         if (trace) log.trace(this + " in addConnectionListener()");
         if (connectionValidator == null)
         {
            Map map = new HashMap(configuration);
            map.putAll(metadata);
            connectionValidatorKey = new ConnectionValidatorKey(invoker, map);
            WeakReference ref = (WeakReference) connectionValidators.get(connectionValidatorKey);
            if (ref == null)
            {
               connectionValidator = new ConnectionValidator(this, metadata);
               connectionValidators.put(connectionValidatorKey, new WeakReference(connectionValidator));
               connectionValidator.addConnectionListener(this, listener);
               if (trace) log.trace(this + ": created " + connectionValidator);
            }
            else
            {
               connectionValidator = (ConnectionValidator) ref.get();
               if (connectionValidator.addConnectionListener(this, listener))
               {
                  if (trace) log.trace(this + ": reusing from static table:  " + connectionValidator);                  
               }
               else
               {
                  if (trace) log.trace(this + ": unable to reuse existing ConnectionValidator in static map: " + connectionValidator);
                  connectionValidator = new ConnectionValidator(this, metadata);
                  connectionValidators.put(connectionValidatorKey, new WeakReference(connectionValidator));
                  connectionValidator.addConnectionListener(this, listener);
                  if (trace) log.trace(this + ": current ConnectionValidator is stopped: created " + connectionValidator);
               }
            }
         }
         else
         {
            if (connectionValidator.addConnectionListener(this, listener))
            {
               if (trace) log.trace(this + ": reusing from local reference: " + connectionValidator);                  
            }
            else
            {
               if (trace) log.trace(this + ": unable to reuse ConnectionValidator from local reference: " + connectionValidator);
               connectionValidator = new ConnectionValidator(this, metadata);
               connectionValidators.put(connectionValidatorKey, new WeakReference(connectionValidator));
               connectionValidator.addConnectionListener(this, listener);
               if (trace) log.trace(this + ": current ConnectionValidator is stopped: created " + connectionValidator);
            }
         }
         
         connectionListeners.add(listener);
      }
   }

   /**
    * Removes specified connection listener.  Will return true if it has already been registered,
    * false otherwise.
    */
   public boolean removeConnectionListener(ConnectionListener listener)
   {
      if (trace) log.trace(this + ".removeConnectionListener(" + listener + ")");
      boolean isRemoved = false;
      synchronized (connectionValidatorLock)
      {
         if (connectionValidator == null)
         {
            return false;
         }
         isRemoved = connectionValidator.removeConnectionListener(this, listener);
         if (connectionValidator.isStopped())
         {
            if (connectionValidators.remove(connectionValidatorKey) != null)
            {
               log.debug(this + ".removeConnectionListener() removed from static map: " + connectionValidator);
            }
            connectionValidator = null;
            connectionValidatorKey = null;
         }
         connectionListeners.remove(listener);
         if (connectionListeners.isEmpty())
         {
            connectionValidator = null;
            connectionValidatorKey = null;
         }
         if (connectionValidator == null)
         {
            if (trace) log.trace(this + " set connectionValidator to null");
         }
      }
      return isRemoved;
   }

   /**
    * This will set the session id used when making invocations on server invokers. There is a
    * default unique id automatically generated for each Client instance, so unless you have a good
    * reason to set this, do not set this.
    */
   public void setSessionId(String sessionId)
   {
      this.sessionId = sessionId;
   }

   /**
    * Gets the configuration map passed when constructing this object.
    */
   public Map getConfiguration()
   {
      return configuration;
   }

   /**
    * Gets the session id used when making invocations on server invokers. This is the id that will
    * be used for tracking client connections on the server side, to include client failures that
    * are sent to connection listeners on the server side.
    */
   public String getSessionId()
   {
      return this.sessionId;
   }

   /**
    * Indicates if the underlying transport has been connected to the target server.
    */
   public boolean isConnected()
   {
      return connected;
   }

   /**
    * Will cause the underlying transport to make connection to the target server.  This is
    * important for any stateful transports, like socket or multiplex. This is also when a client
    * lease with the server is started.
    */
   public void connect() throws Exception
   {
       connect(null, null);
   }
   
   /**
    * Will cause the underlying transport to make connection to the target server.  This is
    * important for any stateful transports, like socket or multiplex. This is also when a client
    * lease with the server is started.  If listener is not null, it will be registered to
    * receive a callback if the connection fails.
    */
   public void connect(ConnectionListener listener) throws Exception
   {
       connect(listener, null);
   }
   
   /**
    * Will cause the underlying transport to make connection to the target server.  This is
    * important for any stateful transports, like socket or multiplex. This is also when a client
    * lease with the server is started.  If listener is not null, it will be registered to
    * receive a callback if the connection fails.
    * <p>
    * 
    * If this version of connect() is used, and leasing is enabled, the concept of "connection
    * identity" is enforced.  That is, the ConnectionValidator used by this Client will be
    * tied to the LeasePinger currently used by the MicroRemoteClientInvoker created or reused
    * in this method, and that LeasePinger will be tied to this Client and its ConnectionValidator.
    * If the ConnectionValidator used by any of the Clients associated with the MicroRemoteClientInvoker
    * used by this Client detects a broken connection, it will shut down that LeasePinger.
    * Moreover, each ConnectionValidator associated with that LeasePinger will notify its
    * ConnectionListeners of the broken connection.  At that point, the LeasePinger will be
    * destroyed, and all of the associated Clients will be disconnected. 
    */
   public void connect(ConnectionListener listener, Map metadata) throws Exception
   {
      log.debug(this + ".connect(" + listener + ")");
      if (trace) log.trace(this + ": metadata = " + metadata);
      if (isConnected())
         return;

      if (locator == null)
      {
         throw new IllegalStateException("Cannot connect a client with a null locator");
      }

      if (invoker == null)
      {
         if (socketFactory != null)
         {
            configuration.put(Remoting.CUSTOM_SOCKET_FACTORY, socketFactory);
            this.socketFactory = null;
         }
         try
         {
            invoker = (ClientInvoker) AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  return InvokerRegistry.createClientInvoker(locator, configuration);
               }
            });
         }
         catch (PrivilegedActionException pae)
         {
            throw pae.getException();
         }
         
      }

      connect(invoker, listener, metadata);

      connected = true;
      log.debug(this + " is connected");
   }

   /**
    * Disconnects the underlying transport from the target server. Also notifies the target server
    * to terminate client lease.  Is important that this method is called when no longer using the
    * remoting client.  Otherwise resource will not be cleaned up and if the target server requires
    * a lease, it will be maintained in the background.
    */
   public void disconnect()
   {
      if (trace) log.trace(this + " entering disconnect()");
      
      connected = false;
      
      if (invoker != null)
      {
         // this is a noop if no lease is active
         invoker.terminateLease(sessionId, disconnectTimeout);
         
         // Need to remove myself from registry so will not keep reference to me since I am of no
         // use now. Will have to create a new one.

         if (invokerDestructionDelay > 0)
         {
            synchronized (invokerDestructionTimerLock)
            {
               InvokerDestructionTimerTask task = new InvokerDestructionTimerTask(invoker, configuration);
               if (invokerDestructionTimer == null)
               {
                  invokerDestructionTimer = new Timer(true);
               }

               try
               {
                  invokerDestructionTimer.schedule(task, invokerDestructionDelay);
               }
               catch (IllegalStateException e)
               {
                  log.debug("Unable to schedule InvokerDestructionTimerTask on existing Timer", e);
                  invokerDestructionTimer = new Timer(true);
                  invokerDestructionTimer.schedule(task, invokerDestructionDelay);
               }
               
               if (trace) log.trace(this + " scheduled destruction of " + invoker);
            }
         }
         else
         {
            AccessController.doPrivileged( new PrivilegedAction()
            {
               public Object run()
               {
                  InvokerRegistry.destroyClientInvoker(invoker.getLocator(), configuration);
                  return null;
               }
            });
         }
         
         invoker = null;
      }
      
      synchronized (connectionValidatorLock)
      {
         if (connectionValidator != null)
         {
            Iterator it = connectionListeners.iterator();
            while (it.hasNext())
            {
               ConnectionListener listener = (ConnectionListener) it.next();
               connectionValidator.removeConnectionListener(this, listener);
            }
            if (connectionValidator.isStopped())
            {
               if (connectionValidators.remove(connectionValidatorKey) != null)
               {
                  if (trace) log.trace(this + ".disconnect() removed from static map: " + connectionValidator);
               }
            }
            
            connectionValidator = null;
            connectionValidatorKey = null;
         }
      }
      log.debug(this + " is disconnected");
   }

   /**
    * Get the client invoker (transport implementation).
    */
   public ClientInvoker getInvoker()
   {
      return invoker;
   }

   /**
    * Set the client invoker (transport implementation).
    */
   public void setInvoker(ClientInvoker invoker)
   {
      this.invoker = invoker;
   }

   /**
    * Gets the subsystem being used when routing invocation request on the server side.
    */
   public String getSubsystem()
   {
      return subsystem;
   }

   /**
    * Sets the subsystem being used when routing invocation requests on the server side.  Specifing
    * a subsystem is only needed when server has multiple handlers registered (which will each have
    * their own associated subsystem).
    */
   public void setSubsystem(String subsystem)
   {
      this.subsystem = subsystem;
   }

   /**
    * Invokes the server invoker handler with the payload parameter passed. Same as calling
    * invoke(param, null);
    */
   public Object invoke(Object param) throws Throwable
   {
      return invoke(param, null);
   }

   /**
    * Invoke the method remotely.
    *
    * @param param - payload for the server invoker handler.
    * @param metadata - any extra metadata that may be needed by the transport (i.e. GET or POST if
    *        using http invoker) or if need to pass along extra data to the server invoker handler.
    */
   public Object invoke(Object param, Map metadata) throws Throwable
   {
      return invoke(param, metadata, null);
   }

   /**
    * Will invoke a oneway call to server without a return object. This should be used when not
    * expecting a return value from the server and wish to achieve higher performance, since the
    * client will not wait for a return.
    * <b>
    * This is done one of two ways. The first is to pass true as the clientSide param.  This will
    * cause the execution of the remote call to be executed in a new thread on the client side and
    * will return the calling thread before making call to server side.
    * <p/>
    * The second, is to pass false as the clientSide param. This will allow the current calling
    * thread to make the call to the remote server, at which point, the server side processing of
    * the thread will be executed on the remote server in a new executing thread.
    * <p>
    * NOTE:  The treatment of server side oneway invocations may vary with the transport.  The
    * client side transport is not required to wait for a reply from the server.  In particular,
    * the socket and bisocket transports return immediately after writing the invocation.
    */
   public void invokeOneway(final Object param, final Map sendPayload, boolean clientSide) 
      throws Throwable
   {
      final Map internalSendPayload = sendPayload == null ? new HashMap() : sendPayload;
      internalSendPayload.put(ONEWAY_FLAG, "true");

      if (clientSide)
      {
         ThreadPool threadPool = getOnewayThreadPool();
         Runnable onewayRun = new Runnable()
         {
            public void run()
            {
               try
               {
                  invoke(param, internalSendPayload);
               }
               catch (Throwable e)
               {
                  // throw away exception since can't get it back to original caller
                  log.error("Error executing client oneway invocation request: " + param, e);
               }
            }
         };
         threadPool.run(onewayRun);
      }
      else
      {
         OnewayInvocation invocation = new OnewayInvocation(param);
         invoke(invocation, internalSendPayload);
      }
   }

   /**
    * Returns the callback Connectors with which callbackHandler is registered.
    */
   public Set getCallbackConnectors(InvokerCallbackHandler callbackHandler)
   {
      return (Set) callbackConnectors.get(callbackHandler);
   }

   /**
    * Gets the timeout used for network i/o in disconnect() and removeListener().
    */
   public int getDisconnectTimeout()
   {
      return disconnectTimeout;
   }

   /**
    * Sets the timeout used for network i/o in disconnect() and removeListener().
    */
   public void setDisconnectTimeout(int disconnectTimeout)
   {
      this.disconnectTimeout = disconnectTimeout;
   }

   /**
    * Sets the maximum queue size to use within client pool for one way invocations on the client
    * side (meaning oneway invocation is handled by thread in this pool and user's call returns
    * immediately). Default value is MAX_NUM_ONEWAY_THREADS.
    */
   public void setMaxOnewayThreadPoolQueueSize(int maxOnewayThreadPoolQueueSize)
   {
      this.maxOnewayThreadPoolQueueSize = maxOnewayThreadPoolQueueSize;
   }

   /**
    * Gets the maximum queue size to use within client pool for one way invocations on the client
    * side (meaning oneway invocation is handled by thread in this pool and user's call returns
    * immediately). Default value is MAX_NUM_ONEWAY_THREADS.
    */
   public int getMaxOnewayThreadPoolQueueSize()
   {
      return this.maxOnewayThreadPoolQueueSize;
   }

   /**
    * Sets the maximum number of threads to use within client pool for one way invocations on the
    * client side (meaning oneway invocation is handled by thread in this pool and user's call
    * returns immediately). Default value is MAX_NUM_ONEWAY_THREADS.
    */
   public void setMaxNumberOfThreads(int numOfThreads)
   {
      this.maxNumberThreads = numOfThreads;
   }

   /**
    * Gets the maximum number of threads to use within client pool for one way invocations on the
    * client side (meaning oneway invocation is handled by thread in this pool and user's call
    * returns immediately). Default value is MAX_NUM_ONEWAY_THREADS.
    */
   public int getMaxNumberOfThreads()
   {
      return this.maxNumberThreads;
   }

   /**
    * Gets the thread pool being used for making one way invocations on the client side. If one has
    * not be specifically set via configuration or call to set it, will always return instance of
    * org.jboss.util.threadpool.BasicThreadPool.
    */
   public ThreadPool getOnewayThreadPool()
   {
      synchronized (onewayThreadPoolLock)
      {
         if (onewayThreadPool == null)
         {
            BasicThreadPool pool = new BasicThreadPool("JBossRemoting Client Oneway");
            log.debug("created new thread pool: " + pool);
            Object param = configuration.get(MAX_NUM_ONEWAY_THREADS);
            if (param instanceof String)
            {
               try
               {
                  maxNumberThreads = Integer.parseInt((String) param);
               }
               catch (NumberFormatException  e)
               {
                  log.error("maxNumberThreads parameter has invalid format: " + param);
               }
            }
            else if (param != null)
            {
               log.error("maxNumberThreads parameter must be a string in integer format: " + param);
            }

            param = configuration.get(MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE);

            if (param instanceof String)
            {
               try
               {
                  maxOnewayThreadPoolQueueSize = Integer.parseInt((String) param);
               }
               catch (NumberFormatException  e)
               {
                  log.error("maxOnewayThreadPoolQueueSize parameter has invalid format: " + param);
               }
            }
            else if (param != null)
            {
               log.error("maxOnewayThreadPoolQueueSize parameter must be a string in integer format: " + param);
            }

            pool.setMaximumPoolSize(maxNumberThreads);

            if (maxOnewayThreadPoolQueueSize > 0)
            {
               pool.setMaximumQueueSize(maxOnewayThreadPoolQueueSize);
            }
            pool.setBlockingMode(BlockingMode.RUN);
            onewayThreadPool = pool;
         }
      }
      return onewayThreadPool;
   }

   /**
    * Sets the thread pool to be used for making one way invocations on the client side.
    */
   public void setOnewayThreadPool(ThreadPool pool)
   {
      this.onewayThreadPool = pool;
   }

   /**
    * The socket factory can only be set on the Client before the connect() method has been called.
    * Otherwise, a runtime exception will be thrown.
    */
   public void setSocketFactory(SocketFactory socketFactory)
   {
      if(isConnected())
      {
         throw new RuntimeException("Cannot set socket factory on Client after " +
                                    "the connect() method has been called.");
      }

      if (invoker != null)
      {
         invoker.setSocketFactory(socketFactory);
      }
      else
      {
         this.socketFactory = socketFactory;
      }
   }

   public SocketFactory getSocketFactory()
   {
      if (invoker != null)
      {
         return invoker.getSocketFactory();
      }
      else
      {
         return socketFactory;
      }
   }

   /**
    * Same as calling invokeOneway(Object param, Map sendPayload, boolean clientSide) with
    * clientSide param being false and a null sendPayload.
    */
   public void invokeOneway(Object param) throws Throwable
   {
      invokeOneway(param, null);
   }

   /**
    * Same as calling invokeOneway(Object param, Map sendPayload, boolean clientSide) with
    * clientSide param being false.
    */
   public void invokeOneway(Object param, Map sendPayload) throws Throwable
   {
      invokeOneway(param, sendPayload, false);
   }

   /**
    * Adds the specified handler as a callback listener for push (async) callbacks. If the transport
    * is uni-directional (e.g. http), remoting will automatically poll for callbacks from the server
    * and deliver them to the callback handler. If the transport is bi-directional (e.g. multiplex),
    * remoting will automatically create a callback server internally and receive and deliver to
    * callback handler the callbacks as they are generated on the server. The metadata map passed
    * will control configuration for how the callbacks are processed, such as the polling frequency.
    */
   public void addListener(InvokerCallbackHandler callbackhandler, Map metadata) throws Throwable
   {
      addListener(callbackhandler, metadata, null);
   }

   /**
    * Adds the specified handler as a callback listener for push (async) callbacks. If the transport
    * is uni-directional (e.g. http), remoting will automatically poll for callbacks from the server
    * and deliver them to the callback handler. If the transport is bi-directional (e.g. multiplex),
    * remoting will automatically create a callback server internally and receive and deliver to
    * callback handler the callbacks as they are generated on the server. The metadata map passed
    * will control configuration for how the callbacks are processed, such as the polling frequency.
    *
    * @param callbackHandlerObject - this object will be included in the Callback object instance
    *        passed to the InvokerCallbackHandler specified.
    */
   public void addListener(InvokerCallbackHandler callbackhandler, Map metadata,
                           Object callbackHandlerObject) throws Throwable
   {
      addListener(callbackhandler, metadata, callbackHandlerObject, false);
   }

   /**
    * Adds the specific handler as a callback listener for async callbacks. If the transport
    * supports bi-directional calls (meaning server can call back to client over same connection
    * that was established by the client) or if the serverToClient flag is set to true, a callback
    * server will be created internally and the target server will actually send callbacks to the
    * client's internal server. Otherwise, the client will simulate push callbacks by internally
    * polling for callbacks on the server and then deliver them to the callback handler.
    *
    * @param serverToClient - if true, will allow server to connect to the client directly (which
    *        must be allowed by firewall in front of client unless transport is bi-directional, such
    *        as the multiplex transport). If false (and not bi-directional transport), server will
    *        not create any new connection to the client.
    */
   public void addListener(InvokerCallbackHandler callbackhandler, Map metadata,
                           Object callbackHandlerObject, boolean serverToClient) throws Throwable
   {
      InvokerLocator callbackLocator = null;

      if (isConnected())
      {
         if (callbackhandler != null)
         {
            boolean isBidirectional = invoker instanceof BidirectionalClientInvoker;

            if (isBidirectional || serverToClient)
            {
               // setup callback server
               String transport = null;
               String host = null;
               int port = -1;

               // look for config values
               if (metadata != null)
               {
                  transport = (String) metadata.get(CALLBACK_SERVER_PROTOCOL);
                  host = (String) metadata.get(CALLBACK_SERVER_HOST);
                  String sPort = (String) metadata.get(CALLBACK_SERVER_PORT);
                  if (sPort != null)
                  {
                     try
                     {
                        port = Integer.parseInt(sPort);
                     }
                     catch (NumberFormatException e)
                     {
                        log.warn("Could not set the internal callback server port as " +
                                 "configuration value (" + sPort + ") is not a number.");
                     }
                  }
               }
               else
               {
                  metadata = new HashMap();
               }
               if (transport == null)
               {
                  transport = invoker.getLocator().getProtocol();
                  metadata.put(CALLBACK_SERVER_PROTOCOL, transport);
               }
               if (host == null)
               {
                  host = getLocalHost().getHostAddress();
                  metadata.put(CALLBACK_SERVER_HOST, host);
               }
               if (port == -1)
               {
                  port = PortUtil.findFreePort(host);
                  metadata.put(CALLBACK_SERVER_PORT, String.valueOf(port));
               }

               if(isBidirectional)
               {
                  callbackLocator =
                     ((BidirectionalClientInvoker)invoker).getCallbackLocator(metadata);
               }
               else
               {
                  callbackLocator = new InvokerLocator(transport, host, port, null, metadata);
               }
               log.debug("starting callback Connector: " + callbackLocator);
               Map callbackConfig = new HashMap(configuration);
               
               if (locator.getParameters() != null)
               {
                  callbackConfig.putAll(locator.getParameters());
               }
               
               configureCallbackServerSocketFactory(callbackConfig);
               Connector callbackServerConnector = new Connector(callbackLocator, callbackConfig);
               
               synchronized (callbackConnectors)
               {
                  Set connectors = (Set) callbackConnectors.get(callbackhandler);
                  if (connectors == null)
                  {
                     connectors = new HashSet();
                  }
                  connectors.add(callbackServerConnector);
                  callbackConnectors.put(callbackhandler, connectors);
               }

               callbackServerConnector.start();
               // have to use the locator from the server as can be modified internally
               callbackLocator = callbackServerConnector.getServerInvoker().getLocator();
               addCallbackListener(callbackhandler, metadata, callbackLocator, callbackHandlerObject);
            }
            else
            {
               if (callbackPollers.get(callbackhandler) != null)
               {
                  log.debug(callbackhandler + " already registered");
                  return;
               }
               
               //need to setup poller to get callbacks from the server
               CallbackPoller poller =
                  new CallbackPoller(this, callbackhandler, metadata, callbackHandlerObject);
               callbackPollers.put(callbackhandler, poller);
               addCallbackListener(callbackhandler, metadata, callbackLocator, callbackHandlerObject);
               poller.start();
            }
         }
         else
         {
            throw new NullPointerException("InvokerCallbackHandler to be added as " +
                                           "a listener can not be null.");
         }
      }
      else
      {
         throw new Exception("Can not add callback listener because " +
                             "remoting client is not connected to server.");
      }
   }

   /**
    * Adds the specified handler as a callback listener for pull (sync) callbacks. Using this method
    * will require the programatic getting of callbacks from the server (they will not be pushed to
    * the callback handler automatically).
    */
   public void addListener(InvokerCallbackHandler callbackHandler) throws Throwable
   {
      addListener(callbackHandler, (InvokerLocator) null);
   }

   /**
    * Adds the specified handler as a callback listener for push (async) callbacks. The invoker
    * server will then callback on this handler (via the server invoker specified by the
    * clientLocator) when it gets a callback from the server handler.
    *
    * Note: passing a null clientLocator will cause the client invoker's client locator to be set to
    * null, which basically converts the mode to be pull (sync) where will require call to get
    * callbacks (as will not automatically be pushed to callback handler).
    */
   public void addListener(InvokerCallbackHandler callbackHandler,
                           InvokerLocator clientLocator) throws Throwable
   {
      addListener(callbackHandler, clientLocator, null);
   }

   /**
    * Adds the specified handler as a callback listener for push (async) callbacks. The invoker
    * server will then callback on this handler (via the server invoker specified by the
    * clientLocator) when it gets a callback from the server handler.
    *
    * Note: passing a null clientLocator will cause the client invoker's client locator to be set to
    * null, which basically converts the mode to be pull (sync) where will require call to get
    * callbacks (as will not automatically be pushed to callback handler).
    *
    * @param callbackHandlerObject will be included in the callback object passed upon callback.
    */
   public void addListener(InvokerCallbackHandler callbackHandler,
                           InvokerLocator clientLocator, Object callbackHandlerObject)
      throws Throwable
   {
      if (callbackHandler != null)
      {
         if (isConnected())
         {
            addCallbackListener(callbackHandler, null, clientLocator, callbackHandlerObject);
         }
         else
         {
            throw new Exception("Can not add callback listener as " +
                                "remoting client is not connected to server.");
         }
      }
      else
      {
         throw new NullPointerException("InvokerCallbackHandler to be added as " +
                                        "a listener can not be null.");
      }
   }

   /**
    * Removes callback handler as a callback listener from the server (and client in the case that
    * it was setup to receive async callbacks). See addListener().
    */
   public void removeListener(InvokerCallbackHandler callbackHandler) throws Throwable
   {
      if (isConnected())
      {
         if (callbackHandler != null)
         {
            // first need to see if is push or pull callback (i.e. does have locator associated
            // with it)
            String listenerId = (String)listeners.get(callbackHandler);
            if(listenerId != null)
            {
               // have a pull callback handler
               // If disconnectTimeout == 0, skip network i/o.
               if (disconnectTimeout != 0)
               {
                  Map metadata = new HashMap();
                  metadata.put(LISTENER_ID_KEY, listenerId);
                  
                  if (disconnectTimeout > 0)
                     metadata.put(ServerInvoker.TIMEOUT, Integer.toString(disconnectTimeout));

                  try
                  {
                     invoke(new InternalInvocation(InternalInvocation.REMOVELISTENER, null), metadata);
                  }
                  catch (Exception e)
                  {
                     log.debug("unable to remove remote callback handler", e);
                  }
               }

               // clean up callback poller if one exists
               CallbackPoller callbackPoller = (CallbackPoller) callbackPollers.remove(callbackHandler);
               if (callbackPoller != null)
               {
                  callbackPoller.stop();
               }

               listeners.remove(callbackHandler);
            }
            else
            {
               // have a push callback handler
               List holderList = invoker.getClientLocators(sessionId, callbackHandler);
               if(holderList != null && holderList.size() > 0)
               {
                  for(int x = 0; x < holderList.size(); x++)
                  {
                     AbstractInvoker.CallbackLocatorHolder holder =
                        (AbstractInvoker.CallbackLocatorHolder)holderList.get(x);
                     listenerId = holder.getListenerId();
                     InvokerLocator locator = holder.getLocator();
                     Map metadata = new HashMap();
                     metadata.put(LISTENER_ID_KEY, listenerId);

                     // If disconnectTimeout == 0, skip network i/o.
                     if (disconnectTimeout != 0)
                     {
                        if (disconnectTimeout > 0)
                           metadata.put(ServerInvoker.TIMEOUT, Integer.toString(disconnectTimeout));

                        try
                        {
                           // now call target server to remove listener
                           InternalInvocation ii =
                              new InternalInvocation(InternalInvocation.REMOVELISTENER, null);

                           invoke(ii, metadata);
                        }
                        catch (Exception e)
                        {
                           log.debug("unable to remove remote callback handler", e);
                        }
                     }

                     // call to callback server to remove listener
                     Client client = new Client(locator, subsystem);
                     client.setSessionId(getSessionId());
                     client.connect();
                     InternalInvocation ii =
                        new InternalInvocation(InternalInvocation.REMOVECLIENTLISTENER,
                              new Object[]{callbackHandler});

                     client.invoke(ii, metadata);
                     client.disconnect();
                  }
               }
            }

            // clean up callback server connectors if any exist
            Set connectors = null;
            synchronized (callbackConnectors)
            {
               connectors = (Set) callbackConnectors.remove(callbackHandler);
            }

            if (connectors != null)
            {
               Iterator it = connectors.iterator();
               while (it.hasNext())
               {
                  Connector callbackConnector = (Connector) it.next();
                  callbackConnector.stop();
                  callbackConnector.destroy();
               }
            }
         }
         else
         {
            throw new NullPointerException("Can not remove null InvokerCallbackHandler listener.");
         }
      }
      else
      {
         throw new Exception("Can not remove callback listener as " +
         "remoting client is not connected to server.");
      }
   }

   /**
    * Gets the callbacks for specified callback handler. The handler is required because an id is
    * generated for each handler.  So if have two callback handlers registered with the same server,
    * no other way to know for which handler to get the callbacks for.
    */
   public List getCallbacks(InvokerCallbackHandler callbackHandler) throws Throwable
   {
      return getCallbacks(callbackHandler, null);
   }
   
   /**
    * Gets the callbacks for specified callback handler. The handler is required because an id is
    * generated for each handler.  So if have two callback handlers registered with the same server,
    * no other way to know for which handler to get the callbacks for.
    * 
    * The metadata map can be used to set callback blocking mode and blocking timeout
    * value.
    */
   public List getCallbacks(InvokerCallbackHandler callbackHandler, Map metadata) throws Throwable
   {
      if (callbackHandler != null)
      {
         String listenerId = (String)listeners.get(callbackHandler);

         if(listenerId != null)
         {
            if (metadata == null)
               metadata = new HashMap();
            
            metadata.put(LISTENER_ID_KEY, listenerId);
            InternalInvocation invocation = new InternalInvocation(InternalInvocation.GETCALLBACKS, null);

            try
            {
               List response = (List) invoke(invocation, metadata);
               return response;
            }
            catch (MarshalException e)
            {
               if (e.getCause() != null && e.getCause() instanceof SocketTimeoutException)
               {
                  if (trace) log.trace(this + ": getCallbacks() timed out: returning empty list");
                  return new ArrayList();
               }
               throw e;
            }
            finally
            {
               metadata.remove(LISTENER_ID_KEY);
            }
         }
         else
         {
            String errorMessage = "Could not find listener id for InvokerCallbackHandler (" +
                                  callbackHandler +
                                  "), please verify handler has been registered as listener.";

            String errorMode = (String) metadata.get(THROW_CALLBACK_EXCEPTION);
            boolean throwError = Boolean.valueOf(errorMode).booleanValue();
            
            if (throwError)
            {
               throw new IOException(errorMessage);
            }
            else
            {
               log.error(errorMessage);
               return null;
            }
         }
      }
      else
      {
         throw new NullPointerException("Can not remove null InvokerCallbackHandler listener.");
      }
   }

   public int acknowledgeCallback(InvokerCallbackHandler callbackHandler, Callback callback)
      throws Throwable
   {
      return acknowledgeCallback(callbackHandler, callback, null);
   }

   public int acknowledgeCallback(InvokerCallbackHandler callbackHandler, Callback callback,
                                  Object response) throws Throwable
   {
      ArrayList callbacks = new ArrayList(1);
      callbacks.add(callback);

      ArrayList responses = null;
      if (response != null)
      {
         responses = new ArrayList(1);
         responses.add(response);
      }

      return acknowledgeCallbacks(callbackHandler, callbacks, responses);
   }

   public int acknowledgeCallbacks(InvokerCallbackHandler callbackHandler, List callbacks)
      throws Throwable
   {
      return acknowledgeCallbacks(callbackHandler, callbacks, null);
   }

   public int acknowledgeCallbacks(InvokerCallbackHandler callbackHandler, List callbacks,
                                   List responses) throws Throwable
   {
      if (callbackHandler == null)
      {
         throw new Exception("InvokerCallbackHandler parameter must not be null");
      }

      if (callbacks == null)
      {
         throw new Exception("Callback List parameter must not be null");
      }

      if (responses != null && responses.size() != callbacks.size())
      {
         throw new Exception("Callback response list must be (1) null " +
                             "or (2) the same size as callback list");
      }

      if (callbacks.size() == 0)
      {
         return 0;
      }

      if (isConnected())
      {
         ArrayList callbackIds = new ArrayList(callbacks.size());
         Iterator idsIterator = callbacks.iterator();
         ArrayList responseList = null;
         Iterator responseIterator = null;

         if (responses != null)
         {
            responseList = new ArrayList(responses.size());
            responseIterator = responses.iterator();
         }

         Callback callback = null;
         Object response = null;
         String listenerId = null;

         for (int i = 0; i < callbacks.size(); i++)
         {
            callback = (Callback) idsIterator.next();

            if (responseIterator != null)
            {
               response = responseIterator.next();
            }

            Map returnPayload = callback.getReturnPayload();

            if (returnPayload != null)
            {
               Object callbackId = returnPayload.get(ServerInvokerCallbackHandler.CALLBACK_ID);
               if (callbackId != null)
               {
                  callbackIds.add(callbackId);

                  if (responseIterator != null)
                  {
                     responseList.add(response);
                  }

                  String nextListenerId = (String) returnPayload.get(LISTENER_ID_KEY);

                  if (nextListenerId == null)
                  {
                     throw new Exception("Cannot acknowledge callbacks: " +
                                         "callback " + callbackId + " has null listener id");
                  }

                  if (i == 0)
                  {
                     listenerId = nextListenerId;
                  }
                  else
                  {
                     if (!listenerId.equals(nextListenerId))
                        throw new Exception("Cannot acknowledge callbacks: " +
                                            "all must be from same server side callback handler");
                  }
               }
               else
               {
                  log.error("Cannot acknowledge callback: callback id " +
                            "is missing from return payload");
               }
            }
            else
            {
               log.error("Cannot acknowledge callback: return payload is null");
            }
         }

         if (callbackIds.size() == 0)
         {
            return 0;
         }

         Map metadata = new HashMap();
         if(listenerId != null)
         {
            metadata.put(LISTENER_ID_KEY, listenerId);
         }
         else
         {
            throw new Exception("Could not find listener id for InvokerCallbackHandler (" +
                                callbackHandler + "), please verify handler " +
                                "has been registered as listener.");
         }

         Object[] params = new Object[] {callbackIds, responseList};
         InternalInvocation invocation =
            new InternalInvocation(InternalInvocation.ACKNOWLEDGECALLBACK, params);
         invoke(invocation, metadata);
         return callbackIds.size();
      }
      else
      {
         throw new Exception("Can not acknowledge Callback due to not being connected to server.");
      }
   }

   /**
    * Sets the marshaller implementation that should be used by the client invoker (transport). This
    * overrides the client's default marshaller (or any set within configuration).
    */
   public void setMarshaller(Marshaller marshaller)
   {
      if (isConnected())
      {
         if (marshaller != null)
         {
            invoker.setMarshaller(marshaller);
         }
         else
         {
            throw new NullPointerException("Can not set Marshaller with a null value.");
         }
      }
      else
      {
         throw new RuntimeException("Can not set remoting client Marshaller when not connected.");
      }
   }

   /**
    * Sets the unmarshaller implementation that should be used by the client invoker (transport).
    * This overrides the client's default unmarshaller (or any set within configuration).
    */
   public void setUnMarshaller(UnMarshaller unmarshaller)
   {
      if (isConnected())
      {
         if (unmarshaller != null)
         {

            invoker.setUnMarshaller(unmarshaller);
         }
         else
         {
            throw new NullPointerException("Can not set UnMarshaller to null value.");
         }
      }
      else
      {
         throw new RuntimeException("Can not set remoting client UnMarhshaller when not connected.");
      }
   }

   /**
    * Takes an inputstream and wraps a server around. Then calls the target remoting server and
    * passes a proxy for an inputstream to the server's handler. When the server handler calls on
    * this proxy, it will call back on this server wrapped around this inputstream.
    *
    * @param param - invocation payload.
    *
    * @return the return value from the invocation.
    * @throws Throwable
    */
   public Object invoke(InputStream inputStream, Object param) throws Throwable
   {
      StreamServer streamServer = new StreamServer(inputStream);
      String locator = streamServer.getInvokerLocator();

      // now call on target server and pass locator for stream callbacks
      InvocationRequest invocationRequest =
         new InvocationRequest(sessionId, subsystem, param, null, null, null);
      return invoke(new InternalInvocation(InternalInvocation.ADDSTREAMCALLBACK,
                                           new Object[]{locator, invocationRequest}), null);
   }

   /**
    * Takes an inputstream and wraps a server around. Then calls the target remoting server and
    * passes a proxy for an inputstream to the server's handler. When the server handler calls on
    * this proxy, it will call back on this server wrapped around this inputstream. The Connector
    * passed is expected to have already been started and will have the stream handler added with
    * subsystem of 'stream'. Also note that the Connector passed will not be stopped when/if the
    * server calls to close the input stream.
    *
    * @param param - invocation payload.
    *
    * @return the return value from the invocation
    */
   public Object invoke(InputStream inputStream, Object param, Connector streamConnector)
      throws Throwable
   {
      StreamServer streamServer = new StreamServer(inputStream, streamConnector);
      String locator = streamServer.getInvokerLocator();

      // now call on target server and pass locator for stream callbacks
      InvocationRequest invocationRequest =
         new InvocationRequest(sessionId, subsystem, param, null, null, null);

      return invoke(new InternalInvocation(InternalInvocation.ADDSTREAMCALLBACK,
                                           new Object[]{locator, invocationRequest}), null);
   }

   /**
    * Takes an inputstream and wraps a server around. Then calls the target remoting server and
    * passes proxy for an inputstream to the server's handler. When the server handle calls on this
    * proxy, it will call back on this server wrapped around this inputstream. The InvokerLocator
    * passed is used to create the internal Connector used to receive the calls from the server
    * side.
    */
   public Object invoke(InputStream inputStream, Object param, InvokerLocator streamServerLocator)
      throws Throwable
   {
      StreamServer streamServer = new StreamServer(inputStream, streamServerLocator);
      String locator = streamServer.getInvokerLocator();

      // now call on target server and pass locator for stream callbacks
      InvocationRequest invocationRequest =
         new InvocationRequest(sessionId, subsystem, param, null, null, null);
      return invoke(new InternalInvocation(InternalInvocation.ADDSTREAMCALLBACK,
                                           new Object[]{locator, invocationRequest}), null);
   }

   /**
    * @return the ping period (in ms) this client's connection validator is configured with. If the
    *         client doesn't ping (on account of connection validator not being installed, or
    *         stopped), returns -1.
    */
   public long getPingPeriod()
   {
      if (connectionValidator == null)
      {
         return -1;
      }

      return connectionValidator.getPingPeriod();
   }

   /**
    * @return the lease period (in ms) if the client has an active leasing mechanism with the server
    *         or -1 otherwise.
    */
   public long getLeasePeriod()
   {
      if (invoker == null)
      {
         return -1;
      }

      return invoker.getLeasePeriod(sessionId);
   }
   
   /**
    * Returns an InetAddress for the client machine as seen by the server machine.
    * @return an InetAddress for the client machine as seen by the server machine.
    * @throws Throwable
    */
   public InetAddress getAddressSeenByServer() throws Throwable
   {
      return (InetAddress) invoke("$GET_CLIENT_LOCAL_ADDRESS$");
   }

   public String toString()
   {
      return "Client[" + System.identityHashCode(this) + ":" + sessionId + "]";
   }

   // Package protected ----------------------------------------------------------------------------

   void notifyListeners()
   {
      synchronized (connectionValidatorLock)
      {
         log.debug(this + " entering notifyListeners(): " + connectionValidator);
         if (connectionValidator != null)
         {
            synchronized (connectionValidator)
            {
               if (connectionValidator.isStopped())
               {
                  if (trace) log.trace(this + ": " + connectionValidator + " is stopped");
               }
               else
               {
                  if (trace) log.trace(this + ": " + connectionValidator + " is not stopped");
                  if (trace) log.trace(this + " calling connectionValidator.notifyListeners()");
                  connectionValidator.notifyListeners(new Exception("Could not connect to server!"));
                  Iterator it = connectionListeners.iterator();
                  while (it.hasNext())
                  {
                     ConnectionListener listener = (ConnectionListener) it.next();
                     connectionValidator.removeConnectionListener(this, listener);
                  }
                  if (connectionValidators.remove(connectionValidatorKey) != null)
                  {
                     if (trace) log.trace(this + ".notifyAndDisconnect() removed from static map: " + connectionValidator);
                  }
               }
            }
            connectionValidator = null;
            connectionValidatorKey = null;
         }
         
         log.debug(this + " leaving notifyListeners()");
      }
   }
   
   // Protected ------------------------------------------------------------------------------------

   // Private --------------------------------------------------------------------------------------

   private void connect(ClientInvoker invoker, ConnectionListener listener, Map metadata)
   {
      if (invoker != null)
      {
         invoker.connect();
         try
         {
            setupClientLease(invoker, listener, metadata);
         }
         catch (Throwable throwable)
         {
            RuntimeException e = new CannotConnectException("Error setting up client lease upon performing connect.");
            e.initCause(throwable);
            throw e;
         }
         log.debug(this + " connected to " + locator);
         if (invokerDestructionDelay > 0)
         {
            synchronized (invokerDestructionTimerLock)
            {
               clientCounter++;
               log.debug(this + " clientCounter: " + clientCounter);
            }
         }
      }
      else
      {
         throw new RuntimeException("Client invoker is null (may have used void constructor " +
                                    "for Client, which should only be used for Externalization.");
      }
   }

   private void setupClientLease(ClientInvoker invoker, ConnectionListener listener, Map metadata) throws Throwable
   {
      long leasePeriod = -1;
      boolean enableLease = false;

      // start with checking the locator URL for hint as to if should do initial lease ping
      if (invoker != null)
      {
         if (invoker instanceof LocalClientInvoker)
         {
            // no need to continue as won't do client lease when is local (JBREM-382)
            return;
         }

         InvokerLocator locator = invoker.getLocator();
         Map locatorParams = locator.getParameters();
         if (locatorParams != null)
         {
            String leaseValue = (String)locatorParams.get(InvokerLocator.CLIENT_LEASE);
            if (leaseValue != null && leaseValue.length() > 0)
            {
               enableLease = Boolean.valueOf(leaseValue).booleanValue();
            }

            String leasePeriodValue = (String)locatorParams.get(InvokerLocator.CLIENT_LEASE_PERIOD);
            if (leasePeriodValue != null && leasePeriodValue.length() > 0)
            {
               try
               {
                  leasePeriod = Long.parseLong(leasePeriodValue);
               }
               catch (NumberFormatException e)
               {
                  log.warn("Could not convert client lease period value (" +
                           leasePeriodValue + ") to a number.");
               }
            }
         }
      }
      else
      {
         throw new RuntimeException("Can not set up client lease as client invoker is null.");
      }

      if (configuration != null)
      {
         Object val = configuration.get(ENABLE_LEASE);

         if (val != null)
         {
            if (val instanceof Boolean)
            {
               enableLease = ((Boolean)val).booleanValue();
            }
            else if (val instanceof String)
            {
               enableLease = Boolean.valueOf((String)val).booleanValue();
            }
            else
            {
               log.warn("Can not evaluate " + ENABLE_LEASE + " value (" +
                         val + ") as a boolean type.");
            }
         }

         String leasePeriodValue = (String)configuration.get(InvokerLocator.CLIENT_LEASE_PERIOD);

         if (leasePeriodValue != null && leasePeriodValue.length() > 0)
         {
            try
            {
               leasePeriod = Long.parseLong(leasePeriodValue);
            }
            catch (NumberFormatException e)
            {
               log.warn("Could not convert client lease period value (" +
                         leasePeriodValue + ") to a number.");
            }
         }
      }

      if (trace) log.trace(this + " enableLease: " + enableLease);
      if (enableLease)
      {
         Map temp = new HashMap(configuration);
         if (metadata != null)
         {
             temp.putAll(metadata);
         }
         if (useClientConnectionIdentity)
         {
            temp.put(CLIENT, this);
            temp.put(CONNECTION_LISTENER, listener);
         }
         if (trace) log.trace(this + " calling MicroRemoteClientInvoker.establishLease()");
         invoker.establishLease(sessionId, temp, leasePeriod);
      }
      else if (listener != null)
      {
          addConnectionListener(listener, metadata);
      }
   }

   private Object invoke(Object param, Map metadata, InvokerLocator callbackServerLocator)
         throws Throwable
   {
      if (isConnected())
      {
         return invoker.invoke(new InvocationRequest(sessionId, subsystem, param,
                                                     metadata, null, callbackServerLocator));
      }
      else
      {
         throw new Exception("Can not make remoting client invocation " +
                             "due to not being connected to server.");
      }
   }

   private void addCallbackListener(InvokerCallbackHandler callbackhandler, Map metadata,
                                    InvokerLocator callbackLocator, Object callbackHandlerObject)
         throws Throwable
   {
      // if callback locator is null, then is pull callbacks and need to track callback handler
      // per Client (not by client invoker).
      if (callbackLocator == null)
      {
         String listenerId = generateListenerId(callbackhandler);

         // if listenerId is null, means this Client has already had the callbackhanler reference
         // registered as a listener, so no need to add it again.
         if (listenerId != null)
         {
            Map internalMetadata = new HashMap();
            internalMetadata.put(LISTENER_ID_KEY, listenerId);
            if(metadata != null)
            {
               internalMetadata.putAll(metadata);
            }
            // now call server to add listener
            invoke(new InternalInvocation(InternalInvocation.ADDLISTENER, null),
                   internalMetadata, callbackLocator);
         }
      }
      else
      {
         // is going to be push callbacks which means callback server locator involved.
         // will have to delegate to client invoker.
         String listenerId = invoker.addClientLocator(sessionId, callbackhandler, callbackLocator);

         if (listenerId != null)
         {

            Map internalMetadata = new HashMap();
            internalMetadata.put(LISTENER_ID_KEY, listenerId);
            if(metadata != null)
            {
               internalMetadata.putAll(metadata);
            }

            Client client = new Client(callbackLocator, subsystem);
            client.setSessionId(getSessionId());
            client.connect();

            try
            {
               InternalInvocation i =
                  new InternalInvocation(InternalInvocation.ADDCLIENTLISTENER,
                                         new Object[]{callbackhandler, callbackHandlerObject});

               client.invoke(i, internalMetadata);
            }
            finally
            {
               client.disconnect();
            }

            // now call server to add listener
            invoke(new InternalInvocation(InternalInvocation.ADDLISTENER, null),
                   internalMetadata, callbackLocator);
         }
      }
   }

   private String generateListenerId(InvokerCallbackHandler callbackhandler)
   {
      String listenerId = null;
      Object obj = listeners.get(callbackhandler);
      if(obj == null)
      {
         listenerId = new GUID().toString();
         listeners.put(callbackhandler, listenerId);
      }
      return listenerId;
   }
   
   private void processParameters()
   {
      Map params = new HashMap();
      if (configuration != null)
         params.putAll(configuration);
      if (locator.getParameters() != null)
         params.putAll(locator.getParameters());
      
      Object param = params.get(INVOKER_DESTRUCTION_DELAY);
      if (param instanceof String)
      {
         try
         {
            invokerDestructionDelay = Integer.parseInt((String) param);
            log.debug(this + " setting invokerDestructionDelay to " + invokerDestructionDelay);
         }
         catch (NumberFormatException  e)
         {
            log.error("invokerDestructionDelay parameter has invalid format: " + param);
         }
      }
      else if (param != null)
      {
         log.error("invokerDestructionDelay parameter must be a string in integer format: " + param);
      }
      
      param = configuration.get(Remoting.USE_CLIENT_CONNECTION_IDENTITY);
      if (param instanceof String)
      {
         useClientConnectionIdentity = Boolean.valueOf((String) param).booleanValue();
      }
      else if (param != null)
      {
         log.warn("value of " + Remoting.USE_CLIENT_CONNECTION_IDENTITY + " must be a String: " + param); 
      }
      else
      {
         if (locator.getParameters() != null)
         {
            param = locator.getParameters().get(Remoting.USE_CLIENT_CONNECTION_IDENTITY);
            if (param != null)
            {
               useClientConnectionIdentity = Boolean.valueOf((String) param).booleanValue();
               this.configuration.put(Remoting.USE_CLIENT_CONNECTION_IDENTITY, param);
            }
         }
      }
      
      PortUtil.updateRange(params);
   }

   private void configureCallbackServerSocketFactory(Map map) throws Exception
   {
      Boolean supportsSSL = null;
      try
      {
         supportsSSL = (Boolean) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return new Boolean(InvokerRegistry.isSSLSupported(locator.getProtocol()));
            }
         });
      }
      catch (PrivilegedActionException pae)
      {
         throw pae.getException();
      }
      
      if (supportsSSL.booleanValue() &&
          !map.containsKey(Remoting.CUSTOM_SERVER_SOCKET_FACTORY) &&
          !map.containsKey(ServerInvoker.SERVER_SOCKET_FACTORY) &&
          !map.containsKey(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE))
         map.put(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE, "true");
   }

   // Inner classes --------------------------------------------------------------------------------

   static class InvokerDestructionTimerTask extends TimerTask
   {
      private ClientInvoker invoker;
      private Map config;
      
      public InvokerDestructionTimerTask(ClientInvoker invoker, Map config)
      {
         this.invoker = invoker;
         this.config = config;
      }
      
      public void run()
      {
         log.debug(this + " calling InvokerRegistry.destroyClientInvoker() for " + invoker);
         AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               InvokerRegistry.destroyClientInvoker(invoker.getLocator(), config);
               return null;
            }
         });
         
         synchronized (invokerDestructionTimerLock)
         {
            if (--clientCounter == 0)
            {
               invokerDestructionTimer.cancel();
               invokerDestructionTimer = null;
               log.debug(this + " stopped invokerDestructionTimer");
            }
            log.debug(this + " clientCounter: " + clientCounter);
         }
         log.debug(this + "done");
      }
   }
   
   static class ConnectionValidatorKey
   {
      private ClientInvoker invoker;
      private Map metadata;
      
      ConnectionValidatorKey(ClientInvoker invoker, Map metadata)
      {
         this.invoker = invoker;
         this.metadata = metadata;
      }
      
      public boolean equals(Object o)
      {
         if (o == null)
            return false;
         if (! (o instanceof ConnectionValidatorKey))
            return false;
         ConnectionValidatorKey holder = (ConnectionValidatorKey) o;
         boolean metadataEquals = (metadata == null && holder.metadata == null) || metadata.equals(holder.metadata); 
         return invoker == holder.invoker && metadataEquals;
      }
      
      public int hashCode()
      {
         return invoker.hashCode() * metadata.hashCode();
      }
   }
   
   static private InetAddress getLocalHost() throws UnknownHostException
   {
      if (SecurityUtility.skipAccessControl())
      {
         try
         {
            return InetAddress.getLocalHost();
         }
         catch (IOException e)
         {
            return InetAddress.getByName("127.0.0.1");
         }
      }

      try
      {
         return (InetAddress) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               try
               {
                  return InetAddress.getLocalHost();
               }
               catch (IOException e)
               {
                  return InetAddress.getByName("127.0.0.1");
               }
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (UnknownHostException) e.getCause();
      }
   }
}
