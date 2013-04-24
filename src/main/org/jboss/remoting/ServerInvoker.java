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

import org.jboss.remoting.callback.Callback;
import org.jboss.remoting.callback.InvokerCallbackHandler;
import org.jboss.remoting.callback.ServerInvokerCallbackHandler;
import org.jboss.remoting.invocation.InternalInvocation;
import org.jboss.remoting.invocation.OnewayInvocation;
import org.jboss.remoting.loading.ClassBytes;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.security.ServerSocketFactoryMBean;
import org.jboss.remoting.security.ServerSocketFactoryWrapper;
import org.jboss.remoting.socketfactory.CreationListenerServerSocketFactory;
import org.jboss.remoting.socketfactory.SocketCreationListener;
import org.jboss.remoting.stream.StreamHandler;
import org.jboss.remoting.stream.StreamInvocationHandler;
import org.jboss.remoting.transport.PortUtil;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.util.threadpool.BasicThreadPool;
import org.jboss.util.threadpool.BlockingMode;
import org.jboss.util.threadpool.ThreadPool;
import org.jboss.util.threadpool.ThreadPoolMBean;
import org.jboss.logging.Logger;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.net.ServerSocketFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * ServerInvoker is the server-side part of a remote Invoker. The ServerInvoker implementation is
 * responsible for calling transport, depending on how the protocol receives the incoming data.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 *
 * @version $Revision: 5887 $
 */
public abstract class ServerInvoker extends AbstractInvoker implements ServerInvokerMBean
{
   // Constants ------------------------------------------------------------------------------------

   protected  static final Logger log = Logger.getLogger(ServerInvoker.class);

   /**
    * Key for the the maximum number of threads to be used in the thread pool for one way
    * invocations (server side).
    * This property is only used when the default oneway thread pool is used.
    */
   public static final String MAX_NUM_ONEWAY_THREADS_KEY = "maxNumThreadsOneway";

   /**
    * Key for setting the setting the oneway thread pool to use.
    * The value used with this key will first be checked to see if is a JMX ObjectName and if so,
    * try to look up associated mbean for the ObjectName given and cast to type
    * org.jboss.util.threadpool.ThreadPoolMBean
    * (via MBeanServerInvocationHandler.newProxyInstance()). If the value is not a JMX ObjectName,
    * will assume is a fully qualified classname and load the coresponding class and create a new
    * instance of it (which will require it to have a void constructor). The newly created instance
    * will then be cast to type of org.jboss.util.threadpool.ThreadPool.
    */
   public static final String ONEWAY_THREAD_POOL_CLASS_KEY = "onewayThreadPool";

   /**
    * Key for setting the address the server invoker should bind to.
    * The value can be either host name or IP.
    */
   public static final String SERVER_BIND_ADDRESS_KEY = "serverBindAddress";

   /**
    * Key for setting the addres the client invoker should connecto to.
    * This should be used when client will be connecting to server from outside the server's network
    * and the external address is different from that of the internal address the server invoker
    * will bind to (e.g. using NAT to expose different external address). This will mostly be useful
    * when client uses remoting detection to discover remoting servers. The value can be either host
    * name or IP.
    */
   public static final String CLIENT_CONNECT_ADDRESS_KEY = "clientConnectAddress";

   /**
    * Key for setting the port the server invoker should bind to.
    * If the value supplied is less than or equal to zero, the server invoker will randomly choose
    * a free port to use.
    */
   public static final String SERVER_BIND_PORT_KEY = "serverBindPort";

   /**
    * key for setting the port the client invoker should connect to.
    * This should be used when client will be connecting to server from outside the server's network
    * and the external port is different from that of the internal port the server invoker will bind
    * to (e.g. using NAT to expose different port routing). This will be mostly useful when client
    * uses remoting detection to discover remoting servers.
    */
   public static final String CLIENT_CONNECT_PORT_KEY = "clientConnectPort";

   /**
    * Key used for setting the amount of time (in milliseconds) that a client should renew its
    * lease.
    * If this value is not set, the default of five seconds (see DEFAULT_CLIENT_LEASE_PERIOD), will
    * be used. This value will also be what is given to the client when it initially querys server
    * for leasing information.
    */
   public static final String CLIENT_LEASE_PERIOD = "clientLeasePeriod";

   /**
    * Key for setting the timeout value (in milliseconds) for socket connections.
    */
   public static final String TIMEOUT = "timeout";

   /**
    * Key for setting the value for the server socket factory to be used by the server invoker.
    * The value can be either a JMX Object name, in which case will lookup the mbean and create
    * a proxy to it with type of org.jboss.remoting.security.ServerSocketFactoryMBean
    * (via MBeanServerInvocationHandler.newProxyInstance()).  If not a JMX ObjectName, will assume
    * is the fully qualified classname to the implementation to be used and will load the class,
    * create a new instance of it (which requires it to have a void constructor). The instance will
    * then be cast to type javax.net.ServerSocketFactory.
    */
   public static final String SERVER_SOCKET_FACTORY = "serverSocketFactory";

   /**
    * The max number of worker threads to be used in the pool for processing one way calls on the
    * server side. Value is is 100.
    */
   public static final int MAX_NUM_ONEWAY_THREADS = 100;

   /**
    * Key for the configuration map that determines the queue size for waiting asynchronous
    * invocations.
    */
   public static final String MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE = "maxOnewayThreadPoolQueueSize";
   
   /**
    * The default lease period for clients. This is the number of milliseconds that a client will be
    * required to renew their lease with the server. The default value is 5 seconds.
    */
   public static final int DEFAULT_CLIENT_LEASE_PERIOD = 5000;

   /**
    * The default timeout period for socket connections. The default value is 60000 milliseconds.
    */
   public static final int DEFAULT_TIMEOUT_PERIOD = 60000;
   
   /**
    * The key to be used to determine if pull callbacks should be obtained in 
    *  blocking or nonblocking mode
    */
   public static final String BLOCKING_MODE = "blockingMode";
   
   /**
    * The key value to use to specify timeout for getting callbacks in blocking mode
    */ 
   public static final String BLOCKING_TIMEOUT = "blockingTimeout";
   
   /**
    * The value associated with BLOCKING_MODE that indicates that pull callbacks
    * should be obtained in blocking mode;
    */
   public static final String BLOCKING = "blocking";
   
   /**
    * The value associated with BLOCKING_MODE that indicates that pull callbacks
    * should be obtained in nonblocking mode;
    */
   public static final String NONBLOCKING = "nonblocking";
   
   /**
    * Default timeout for getting callbacks in blocking mode.
    * Default is 5000 milliseconds.
    */
   public static final int DEFAULT_BLOCKING_TIMEOUT = 5000;
   
   
   /**
    * The key to use to specify if ServerInvokerCallbackHandlers should be
    * registered as ConnectionListeners.
    */
   public static final String REGISTER_CALLBACK_LISTENER = "registerCallbackListener";
   
   public static final String ECHO = "$ECHO$";
   
   public static final String INVOKER_SESSION_ID = "invokerSessionId";
   
   public static final String CONNECTION_LISTENER = "connectionListener";


   // Static ---------------------------------------------------------------------------------------

   private static boolean trace = log.isTraceEnabled();

   // Attributes -----------------------------------------------------------------------------------

   /**
    * Indicated the max number of threads used within oneway thread pool.
    */
   private int maxNumberThreads = MAX_NUM_ONEWAY_THREADS;
   private int maxOnewayThreadPoolQueueSize = -1;
   private String onewayThreadPoolClass = null;
   private ThreadPool onewayThreadPool;
   private Object onewayThreadPoolLock = new Object();
   private boolean created = false;

   private MBeanServer mbeanServer = null;

   private String dataType;
   private String serverBindAddress = null;
   private int serverBindPort = 0;
   private String clientConnectAddress = null;
   private int clientConnectPort = -1;
   
   protected List connectHomes = new ArrayList();
   protected List homes = new ArrayList();
   
   private int timeout = DEFAULT_TIMEOUT_PERIOD;

   // indicates the lease timeout period for clients
   private long leasePeriod = DEFAULT_CLIENT_LEASE_PERIOD;
   private boolean leaseManagement = false;
   private Map clientLeases = new ConcurrentHashMap();

   protected Map handlers = new HashMap();
   
   // If there is only one handler we store a direct reference to it, as an optimisation
   // to avoid lookup in this common case - TLF
   protected volatile ServerInvocationHandler singleHandler;
   
   // If there is only one callback container we store a direct reference to it, as an optimisation
   // to avoid lookup in this common case - TLF
   protected volatile CallbackContainer singleCallbackContainer;
      
   protected Map callbackHandlers = new HashMap();
   protected Map clientCallbackListener = new HashMap();
   protected boolean started = false;
   protected ConnectionNotifier connectionNotifier = new ConnectionNotifier();
   protected ServerSocketFactory serverSocketFactory = null;
   
   protected boolean registerCallbackListeners = true;
   
   protected boolean useClientConnectionIdentity;

   // Constructors ---------------------------------------------------------------------------------

   public ServerInvoker(InvokerLocator locator)
   {
      super(locator);
      Map params = locator.getParameters();
      if(configuration != null && params != null)
      {
         configuration.putAll(locator.getParameters());
      }
   }

   public ServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   // Public ---------------------------------------------------------------------------------------

   public void setServerSocketFactory(ServerSocketFactory serverSocketFactory)
   {
      this.serverSocketFactory = serverSocketFactory;
   }

   public ServerSocketFactory getServerSocketFactory()
   {
      return serverSocketFactory;
   }

   /**
    * Sets timeout (in millseconds) to be used for the socket connection.
    */
   public void setTimeout(int timeout)
   {
      this.timeout = timeout;
   }

   /**
    * The timeout (in milliseconds) used for the socket connection.
    */
   public int getTimeout()
   {
      return timeout;
   }
   
   public ConnectionNotifier getConnectionNotifier()
   {
      return connectionNotifier;
   }

   public boolean isLeaseActivated()
   {
      return leaseManagement;
   }

   public void addConnectionListener(ConnectionListener listener)
   {
      if(listener != null)
      {
         connectionNotifier.addListener(listener);

         if(leasePeriod > 0)
         {
            leaseManagement = true;
         }
      }
      else
      {
         throw new IllegalArgumentException("Can not add null ConnectionListener.");
      }
   }
   
   public void setConnectionListener(Object listener)
   {
      if (listener == null)
      {
         log.error("ConnectionListener is null");
         return;
      }
      
      if (listener instanceof ConnectionListener)
      {
         addConnectionListener((ConnectionListener) listener);
         return;
      }

      if (!(listener instanceof String))
      {
         log.error("Object supplied as ConnectionListener is neither String nor ConnectionListener");
         return;
      }

      ConnectionListener connectionListener = null;
      try
      {
         MBeanServer server = getMBeanServer();
         ObjectName objName = new ObjectName((String) listener);
         Class c = ConnectionListener.class;
         Object o = MBeanServerInvocationHandler.newProxyInstance(server, objName, c, false);
         connectionListener = (ConnectionListener) o;
      }
      catch (MalformedObjectNameException e)
      {
         log.debug("Object supplied as ConnectionListener is not an object name.");
      }

      if (connectionListener == null)
      {
         try
         {
            Class listenerClass = ClassLoaderUtility.loadClass((String) listener, ServerInvoker.class);
            connectionListener = (ConnectionListener) listenerClass.newInstance();
         }
         catch (Exception e)
         {
            log.error("Unable to instantiate " + listener + ": " + e.getMessage());
            return;
         }
      }

      if (connectionListener == null)
      {
         log.error("Unable to create ConnectionListener from " + listener);
         return;
      }
      
      addConnectionListener(connectionListener);
   }

   public void removeConnectionListener(ConnectionListener listener)
   {
      if(connectionNotifier != null)
      {
         connectionNotifier.removeListener(listener);

         // turn off lease management if no listeners (since no one to tell client died)
         if(connectionNotifier.size() == 0)
         {
            leaseManagement = false;

            // go through any existing leases and terminate them
            Set clientKeys = clientLeases.keySet();
            Iterator itr = clientKeys.iterator();
            while(itr.hasNext())
            {
               String sessionId = (String)itr.next();
               Lease clientLease = (Lease)clientLeases.get(sessionId);
               clientLease.terminateLease(sessionId);
            }
            clientLeases.clear();
         }
      }
   }

   /**
    * Sets the amount of time (in milliseconds) that a client should renew its lease. If this value
    * is not set, the default of five seconds (see DEFAULT_CLIENT_LEASE_PERIOD), will be used. This
    * value will also be what is given to the client when it initially querys server for leasing
    * information. If set after create() method called, this value will override value set by
    * CLIENT_LEASE_PERIOD key.
    */
   public void setLeasePeriod(long leasePeriodValue)
   {
      this.leasePeriod = leasePeriodValue;

      if (leasePeriod <= 0)
      {
    	  this.leaseManagement = false;
      }
      else
      {
         if(connectionNotifier != null && connectionNotifier.size() > 0)
         {
            this.leaseManagement = true;
         }
      }
   }

   public Lease getLease(String sessionId)
   {
      return (Lease) clientLeases.get(sessionId);
   }
   
   /**
    * Gets the amount of time (in milliseconds) that a client should renew its lease.
    */
   public long getLeasePeriod()
   {
      return leasePeriod;
   }

   /**
    * @jmx:managed-attribute
    */
   public String getClientConnectAddress()
   {
      return clientConnectAddress;
   }

   public int getClientConnectPort()
   {
      return clientConnectPort;
   }

   public void setClientConnectPort(int clientConnectPort)
   {
      this.clientConnectPort = clientConnectPort;
   }

   /**
    * This method should only be called by the service controller when this invoker is specified
    * within the Connector configuration of a service xml. Calling this directly will have no
    * effect, as will be used in building the locator uri that is published for detection and this
    * happens when the invoker is first created and started (after that, no one will be aware of a
    * change).
    *
    * @jmx:managed-attribute
    */
   public void setClientConnectAddress(String clientConnectAddress)
   {
      this.clientConnectAddress = clientConnectAddress;
   }

   public String getServerBindAddress()
   {
      return serverBindAddress;
   }

   public int getServerBindPort()
   {
      return serverBindPort;
   }
   
   public List getConnectHomes()
   {
      return new ArrayList(connectHomes);
   }
   
   public void setConnectHomes(List connectHomes)
   {
      this.connectHomes = new ArrayList(connectHomes);
   }
   
   public List getHomes()
   {
      return new ArrayList(homes);
   }
   
   public void setHomes(List homes)
   {
      this.homes = new ArrayList(homes);
   }

   /**
    * Sets the maximum number of thread to be used in the thread pool for one way invocations
    * (server side). This property is only used when the default oneway thread pool is used. If set
    * after create() method called, this value will override value set by MAX_NUM_ONEWAY_THREADS_KEY
    * key.
    */
   public void setMaxNumberOfOnewayThreads(int numOfThreads)
   {
      this.maxNumberThreads = numOfThreads;
   }

   /**
    * Gets the maximum number of thread to be used in the thread pool for one way invocations
    * (server side).
    */
   public int getMaxNumberOfOnewayThreads()
   {
      return this.maxNumberThreads;
   }

   /**
    * Gets the oneway thread pool to use.
    */
   public ThreadPool getOnewayThreadPool()
   {
      synchronized (onewayThreadPoolLock)
      {
         if(onewayThreadPool == null)
         {
            // if no thread pool class set, then use default BasicThreadPool
            if(onewayThreadPoolClass == null || onewayThreadPoolClass.length() == 0)
            {
               BasicThreadPool pool = new BasicThreadPool("JBossRemoting Server Oneway");
               pool.setMaximumPoolSize(maxNumberThreads);
               if (maxOnewayThreadPoolQueueSize > 0)
                  pool.setMaximumQueueSize(maxOnewayThreadPoolQueueSize);
               pool.setBlockingMode(BlockingMode.RUN);
               onewayThreadPool = pool;
               log.debug(this + " created new thread pool");
            }
            else
            {
               //first check to see if this is an ObjectName
               boolean isObjName = false;
               try
               {
                  ObjectName objName = new ObjectName(onewayThreadPoolClass);
                  onewayThreadPool = createThreadPoolProxy(objName);
                  isObjName = true;
               }
               catch(MalformedObjectNameException e)
               {
                  log.debug("Thread pool class supplied is not an object name.");
               }
               
               if(!isObjName)
               {
                  try
                  {
                     onewayThreadPool = (ThreadPool)Class.
                     forName(onewayThreadPoolClass, false, getClassLoader()).newInstance();
                  }
                  catch(Exception e)
                  {
                     throw new RuntimeException("Error loading instance of ThreadPool based " +
                           "on class name " + onewayThreadPoolClass);
                  }
               }
            }
         }
         else
         {
            log.trace("reusing oneway thread pool");
         }
         return onewayThreadPool;
      }
   }

   /**
    * Sets the oneway thread pool to use.
    */
   public void setOnewayThreadPool(ThreadPool pool)
   {
      this.onewayThreadPool = pool;
   }

   public MBeanServer getMBeanServer()
   {
      return mbeanServer;
   }

   public void setMBeanServer(MBeanServer server)
   {
      // This has been added in order to support mbean service configuration. Now supporting
      // classes, such as the ServerInvokerCallbackHandler can find and use resources such as
      // CallbackStore, which can be run as a service mbean (and specified via object name within
      // config). The use of JMX throughout remoting is a problem as now have to tie it in all
      // throughout the code for service configuration as is being done here. When migrate to use
      // under new server model, which does not depend on JMX, can rip out code such as this.
      this.mbeanServer = server;
   }

   public boolean isRegisterCallbackListeners()
   {
      return registerCallbackListeners;
   }

   public void setRegisterCallbackListeners(boolean registerCallbackListeners)
   {
      this.registerCallbackListeners = registerCallbackListeners;
   }

   /**
    * @return true if a server invocation handler has been registered for this subsystem.
    */
   public synchronized boolean hasInvocationHandler(String subsystem)
   {
      return handlers.containsKey(subsystem);
   }

   /**
    * @return an array of keys for each subsystem this invoker can handle.
    */
   public synchronized String[] getSupportedSubsystems()
   {
      String subsystems [] = new String[handlers.size()];
      return (String[]) handlers.keySet().toArray(subsystems);
   }

   /**
    * @return an array of the server invocation handlers.
    */
   public synchronized ServerInvocationHandler[] getInvocationHandlers()
   {
      ServerInvocationHandler ih [] = new ServerInvocationHandler[handlers.size()];
      return (ServerInvocationHandler[]) handlers.values().toArray(ih);
   }

   /**
    * Add a server invocation handler for a particular subsystem. Typically, subsystems are defined
    * in org.jboss.remoting.Subsystem, however, this can be any string that the caller knows about.
    *
    * @return previous ServerInvocationHandler with the same sybsystem value (case insensitive) or
    *         null if a previous one did not exist.
    */
   public synchronized ServerInvocationHandler addInvocationHandler(String subsystem,
                                                                    ServerInvocationHandler handler)
   {
      handler.setInvoker(this);

      ServerInvocationHandler oldHandler =
         (ServerInvocationHandler)handlers.put(subsystem.toUpperCase(), handler);

      log.debug(this + " added " + handler + " for subsystem '" + subsystem + "'" +
         (oldHandler == null ? "" : ", replacing old handler " + oldHandler));
            
      if (handlers.size() == 1)
      {
         singleHandler = handler;
      }
      else
      {
         singleHandler = null;
      }

      return oldHandler;
   }

   /**
    * Remove a subsystem invocation handler.
    */
   public synchronized ServerInvocationHandler removeInvocationHandler(String subsystem)
   {
      ServerInvocationHandler handler =
         (ServerInvocationHandler)handlers.remove(subsystem.toUpperCase());

      log.debug(this + (handler == null ?
         " tried to remove handler for " + subsystem + " but no handler found" :
         " removed handler " + handler + " for subsystem '" + subsystem + "'"));
      
      if (handlers.size() == 1)
      {
         singleHandler = (ServerInvocationHandler)handlers.values().iterator().next();
      }
      else
      {
         singleHandler = null;
      }

      return handler;
   }

   /**
    * Get a ServerInvocationHandler for a given subsystem type.
    */
   public synchronized ServerInvocationHandler getInvocationHandler(String subsystem)
   {
      return (ServerInvocationHandler) handlers.get(subsystem.toUpperCase());
   }

   protected boolean isUseClientConnectionIdentity()
   {
      return useClientConnectionIdentity;
   }

   protected void setUseClientConnectionIdentity(boolean useClientConnectionIdentity)
   {
      this.useClientConnectionIdentity = useClientConnectionIdentity;
   }

   public Object invoke(Object invoke) throws IOException
   {
      InvocationRequest request = null;
      InvocationResponse response = null;

      if(trace) { log.trace("server received invocation " + invoke); }

      if(invoke != null && invoke instanceof InvocationRequest)
      {
         request = (InvocationRequest) invoke;
         try
         {

            Object result = invoke(request);

            response = new InvocationResponse(request.getSessionId(),
                                              result, false, request.getReturnPayload());

         }
         catch(Throwable throwable)
         {
            response = new InvocationResponse(request.getSessionId(),
                                              throwable, true, request.getReturnPayload());
         }
      }
      else
      {
         log.error("server invoker received " + invoke + " as invocation. " +
            "Must not be null and must be of type InvocationRequest.");

         Exception e = new Exception("Error processing invocation request on " + getLocator() +
            ". Either invocation was null or of wrong type.");

         response =
            new InvocationResponse(request.getSessionId(), e, true, request.getReturnPayload());
      }
      return response;
   }

   /**
    * Processes invocation request depending on the invocation type (internal, name based, oneway,
    * etc). Can be called on directly when client and server are local to one another (by-passing
    * serialization).
    */
   public Object invoke(InvocationRequest invocation) throws Throwable
   {
      if (isStarted())
      {
         Object param = invocation.getParameter();
         Object result = null;

         if (trace) { log.trace(this + " received " + param); }

         if (ECHO.equals(param))
         {
            return ECHO;
         }

         if (param instanceof String)
         {
            // check to see if this is a is alive ping
            if ("$PING$".equals(param))
            {
               Map metadata = invocation.getRequestPayload();
               if (metadata != null)
               {
                  String invokerSessionId = (String) metadata.get(INVOKER_SESSION_ID);
                  if (invokerSessionId != null)
                  {
                     // Comes from ConnectionValidator configured to tie validation with lease.
                     boolean response = checkForClientLease(invokerSessionId);
                     if (trace) log.trace(this + " responding " + response + " to $PING$ for invoker sessionId " + invokerSessionId);
                     return new Boolean(response);
                  }
               }

               if (leaseManagement)
               {
                  // Otherwise, it's a normal PING.  NOTE we only update the lease when we
                  // receive a PING, not for all invocations.
                  updateClientLease(invocation);
               }

               // if this is an invocation ping, just pong back
               Map responseMap = new HashMap();
               responseMap.put(CLIENT_LEASE_PERIOD, new Long(leasePeriod));

               InvocationResponse ir = new InvocationResponse(invocation.getSessionId(),
                     new Boolean(leaseManagement),
                     false, responseMap);

               if (trace) { log.trace(this + " returning " + ir); }
               return ir;
            }

            if ("$GET_CLIENT_LOCAL_ADDRESS$".equals(param))
            {
               InetAddress address = null;
               if (invocation.getRequestPayload() != null)
                  address = (InetAddress) invocation.getRequestPayload().get(Remoting.CLIENT_ADDRESS);

               return address;
            }

            if ("$DISCONNECT$".equals(param))
            {
               if (leaseManagement)
               {
                  terminateLease(invocation);
               }

               if (trace) { log.trace(this + " returning null"); }
               return null;
            }
         }

         //TODO: -TME both oneway and internal invocation will be broken since have not
         // deserialized the para yet (removed ClassUtil.deserialize() so would let handler do it).
         if (param instanceof OnewayInvocation)
         {
            // no point in delaying return to client if oneway
            handleOnewayInvocation((OnewayInvocation)param, invocation);
            
            return null;
         }
         else
         {
            String subsystem = invocation.getSubsystem();
            String clientId = invocation.getSessionId();

            //I have optimised this, so that if there is only one handler set (a very common case)
            //then it will just use that without having to do a lookup or HashMap iteration over
            //values
            
            ServerInvocationHandler handler = findInvocationHandler(subsystem);

            if (param instanceof InternalInvocation)
            {
               result = handleInternalInvocation((InternalInvocation)param, invocation, handler);
            }
            else
            {
               if (trace) { log.trace(this + " dispatching " + invocation + " from client " + clientId + " to subsystem '" + subsystem + "'"); }

               if (handler == null)
               {
                  throw new InvalidConfigurationException(
                     "Can not handle invocation request for subsystem '" + subsystem + "' because " +
                     "there are no matching ServerInvocationHandlers registered. Please add via " +
                     "xml configuration or via the Connector's addInvocationHandler() method.");
               }
               result = handler.invoke(invocation);
            }

            if (trace) { log.trace(this + " successfully dispatched invocation, returning " + result + " from subsystem '" + subsystem + "' to client " + clientId); }
         }

         return result;
      }
      else
      {
         log.warn(this + " can not process invocation requests since is not in started state!");
         throw new InvalidStateException(
            "Can not process invocation request since is not in started state.");
      }
   }

   /**
    * Will get the data type for the marshaller factory so know which marshaller to get to marshal
    * the data. Will first check the locator uri for a 'datatype' parameter and take that value if
    * it exists. Otherwise, will use the default datatype for the client invoker, based on
    * transport.
    */
   public String getDataType()
   {
      if(dataType == null)
      {
         dataType = getDataType(getLocator());
         if(dataType == null)
         {
            dataType = getDefaultDataType();
         }
      }
      return dataType;
   }

   public void create()
   {
      if(!created)
      {
         try
         {
            setup();
         }
         catch(Exception e)
         {
            throw new RuntimeException("Error setting up server invoker " + this, e);
         }
         created = true;
      }
   }

   /**
    * Subclasses should override to provide any specific start logic.
    */
   public void start() throws IOException
   {
      started = true;
      log.debug(this + " started for locator " + getLocator());
   }

   /**
    * @return true if the server invoker is started, false if not.
    */
   public boolean isStarted()
   {
      return started;
   }

   /**
    * Subclasses should override to provide any specific stop logic.
    */
   public void stop()
   {
      started = false;

      for(Iterator i = callbackHandlers.values().iterator(); i.hasNext(); )
      {
         ServerInvokerCallbackHandler callbackHandler = (ServerInvokerCallbackHandler)i.next();
         callbackHandler.destroy();
      }

      log.debug(this + " stopped");
   }

   /**
    * Destory the invoker permanently.
    */
   public void destroy()
   {
      if(classbyteloader != null)
      {
         classbyteloader.destroy();
      }
   }

   /**
    * Sets the server invoker's transport specific configuration. Will need to set before calling
    * start() method (or at least stop() and start() again) before configurations will take affect.
    */
   public void setConfiguration(Map configuration)
   {
      this.configuration = configuration;
   }

   /**
    * Gets the server invoker's transport specific configuration.
    */
   public Map getConfiguration()
   {
      return configuration;
   }

   public void removeCallbackListener(String subsystem, InvokerCallbackHandler callbackHandler)
   {
      ServerInvocationHandler handler = null;
      if(subsystem != null)
      {
         handler = (ServerInvocationHandler) handlers.get(subsystem.toUpperCase());
      }
      else
      {
         // subsystem not specified, so will hope for a default one being set
         if(!handlers.isEmpty())
         {
            handler = (ServerInvocationHandler) handlers.values().iterator().next();
         }
      }
      handler.removeListener(callbackHandler);
   }

   /**
    * @return the String for the object name to be used for the invoker.
    */
   public String getMBeanObjectName()
   {
      InvokerLocator locator = getLocator();
      StringBuffer buffer =
         new StringBuffer("jboss.remoting:service=invoker,transport=" + locator.getProtocol());
      String host = locator.getHost();
      boolean isIPv6 = host.indexOf("[") >= 0 | host.indexOf(":") >= 0;
      
      buffer.append(",host=");
      if (isIPv6)
         buffer.append("\"");
      buffer.append(locator.getHost());
      if (isIPv6)
         buffer.append("\"");
      
      buffer.append(",port=").append(locator.getPort());
      Map param = locator.getParameters();
      if(param != null)
      {
         Iterator itr = param.keySet().iterator();
         while(itr.hasNext())
         {
            buffer.append(",");
            String key = (String) itr.next();
            String value = (String) param.get(key);
            buffer.append(key);
            buffer.append("=");
            buffer.append(value);
         }
      }

      return buffer.toString();
   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected abstract String getDefaultDataType();

   protected void setup() throws Exception
   {
      Map config = getConfiguration();
      PortUtil.updateRange(config);
      setupHomes(config);
      
      String maxNumOfThreads = (String)config.get(MAX_NUM_ONEWAY_THREADS_KEY);

      if(maxNumOfThreads != null && maxNumOfThreads.length() > 0)
      {
         try
         {
            maxNumberThreads = Integer.parseInt(maxNumOfThreads);
         }
         catch(NumberFormatException e)
         {
            log.error("Can not convert max number of threads value (" +
                       maxNumOfThreads + ") into a number.");
         }
      }

      String param = (String) configuration.get(MAX_ONEWAY_THREAD_POOL_QUEUE_SIZE);
      
      if (param != null && param.length() > 0)
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
      
      onewayThreadPoolClass = (String)config.get(ONEWAY_THREAD_POOL_CLASS_KEY);

      // get timeout config
      String timeoutPeriod = (String)config.get(TIMEOUT);
      if(timeoutPeriod != null && timeoutPeriod.length() > 0)
      {
         try
         {
            timeout = Integer.parseInt(timeoutPeriod);
         }
         catch(NumberFormatException e)
         {
            throw new InvalidConfigurationException("Can not set timeout because can not " +
               "convert give value (" + timeoutPeriod + ") to a number.");
         }
      }

      // config for client lease period
      String clientLeasePeriod = (String)config.get(CLIENT_LEASE_PERIOD);
      if(clientLeasePeriod != null)
      {
         try
         {
            long leasePeriodValue = Long.parseLong(clientLeasePeriod);
            setLeasePeriod(leasePeriodValue);
         }
         catch(NumberFormatException e)
         {
            throw new InvalidConfigurationException("Can not set client lease period because " +
               "can not convert given value (" + clientLeasePeriod + ") to a number.");
         }
      }
      
      // config for useClientConnectionIdentity
      String useClientConnectionIdentityString = (String)config.get(Remoting.USE_CLIENT_CONNECTION_IDENTITY);
      if(useClientConnectionIdentityString != null)
      {
         useClientConnectionIdentity = Boolean.valueOf(useClientConnectionIdentityString).booleanValue();
      }
      
      // Inject ConnectionListener
      String connectionListener = (String)config.get(CONNECTION_LISTENER);
      if (connectionListener != null)
      {
         setConnectionListener(connectionListener);
      }
      
      String registerCallbackListenersString = (String)config.get(REGISTER_CALLBACK_LISTENER);
      if(registerCallbackListenersString != null)
      {
         registerCallbackListeners = Boolean.valueOf(registerCallbackListenersString).booleanValue();
      }

      createServerSocketFactory();
      
      // need to check invoker locator to see if need to provide binding address (in the case 0.0.0.0 was used)
      final InvokerLocator originalLocator = locator;
      locator = InvokerLocator.validateLocator(locator);
      if (!locator.getLocatorURI().equals(originalLocator.getLocatorURI())) {
         log.debug(this + " original locator: " + originalLocator);
         log.debug(this + " new locator:      " + locator);
      }
      
      // need to update the locator key used in the invoker registry
      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            InvokerRegistry.updateServerInvokerLocator(originalLocator, locator);
            return null;
         }
      });
   }
   
   protected void setupHomes(Map config) throws Exception
   {
      // First try to find  address(es) using the new multihome facility.
      if (locator.isMultihome())
      {
         connectHomes = locator.getConnectHomeList();
         Object o = config.get(InvokerLocator.CONNECT_HOMES_KEY);
         if (o != null)
         {
            if (o instanceof Collection)
               connectHomes.addAll((Collection) o);
            else if (o instanceof String)
               connectHomes.addAll(createHomeCollection((String) o));
            else
               log.warn(this + ": " + InvokerLocator.CONNECT_HOMES_KEY + " must be a collection or String: " + o);
         }

         homes = locator.getHomeList();
         o = config.get(InvokerLocator.HOMES_KEY);
         if (o != null)
         {
            if (o instanceof Collection)
               homes.addAll((Collection) o);
            else if (o instanceof String)
               homes.addAll(createHomeCollection((String) o));
            else
               log.warn(this + ": " + InvokerLocator.HOMES_KEY + " must be a collection or String: " + o);
         }

         if (!homes.isEmpty() && connectHomes.isEmpty())
            connectHomes.addAll(homes);

         assignPorts();
         return;
      }
      
      // If no bind address(es) found, try the old way.
      String locatorhost = locator.getHost();
      InetAddress addr = null;
      if(locatorhost != null)
      {
         addr = getAddressByName(locatorhost);
      }
      else
      {
         addr = getLocalHost();
      }

      int port = locator.getPort();
      if(port <= 0)
      {
         port = assignPort();
      }

      // set the bind address
      serverBindAddress = (String)config.get(SERVER_BIND_ADDRESS_KEY);
      clientConnectAddress = (String)config.get(CLIENT_CONNECT_ADDRESS_KEY);
      if(serverBindAddress == null)
      {
         if(clientConnectAddress != null)
         {
            // can't use uri address, as is for client only
            serverBindAddress = getLocalHost().getHostAddress();
         }
         else
         {
            serverBindAddress = addr.getHostAddress();
         }
      }

      // set the bind port
      String serverBindPortString = (String)config.get(SERVER_BIND_PORT_KEY);
      String clientConnectPortString = (String)config.get(CLIENT_CONNECT_PORT_KEY);
      if(clientConnectPortString != null)
      {
         try
         {
            clientConnectPort = Integer.parseInt(clientConnectPortString);
         }
         catch(NumberFormatException e)
         {
            throw new InvalidConfigurationException("Can not set client bind port because can " +
               "not convert given value (" + clientConnectPortString + ") to a number.");
         }
      }
      if(serverBindPortString != null)
      {
         try
         {
            serverBindPort = Integer.parseInt(serverBindPortString);
            if(serverBindPort <= 0)
            {
               serverBindPort = assignPort();
            }

         }
         catch(NumberFormatException e)
         {
            throw new InvalidConfigurationException("Can not set server bind port because can " +
               "not convert given value (" + serverBindPortString + ") to a number.");
         }
      }
      else
      {
         if(clientConnectPort > 0)
         {
            // can't use uri port, as is for client only
            serverBindPort = PortUtil.findFreePort(locator.getHost());
         }
         else
         {
            serverBindPort = port;
         }
      }
      
      Home h = new Home(serverBindAddress, serverBindPort);
      homes.add(h);
      connectHomes.add(h);
   }
   
   protected Collection createHomeCollection(String s)
   {
      ArrayList homes = new ArrayList();
      StringTokenizer st = new StringTokenizer(s, "!");
      
      while (st.hasMoreTokens())
      {
         String token = st.nextToken();
         int p = token.indexOf(':');
         String host = null;
         String portString = null;
         if (p < 0)
         {
            host = token;
            portString = "-1";
         }
         else
         {
            host = token.substring(0, p);
            portString = token.substring(p + 1);
         }

         int port = -1;
         try
         {
            port = Integer.parseInt(portString);
         }
         catch (Exception e)
         {
            log.warn("invalid port value in Home: " + token + ", using -1");
         }
         Home home = new Home(host, port);
         homes.add(home);
      }
      
      return homes;
   }
   
   protected int assignPort() throws IOException
   {
      int port;
      port = PortUtil.findFreePort(locator.getHost());

      // re-write locator since the port is different
      final InvokerLocator newLocator = new InvokerLocator(locator.getProtocol(), locator.getHost(), port,
                                                     locator.getPath(), locator.getParameters());

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
      return port;
   }
   
   protected void assignPorts() throws IOException
   {  
      boolean changed = false;
      List list = homes;
      for (int i = 0; i < list.size(); i++)
      {
         Home home = (Home) list.get(i);
         if (home.port < 0)
         {
            changed = true;
            try
            {
               home.port = PortUtil.findFreePort(home.host);
            }
            catch (Exception e)
            {
               if (trace) log.trace(this + " unable to find free port for: " + home.host);
            }
         }
      }
      if (changed)
         locator.setHomeList(homes);
      
      if (connectHomes.size() == homes.size())
      {
         changed = false;
         for (int i = 0; i < homes.size(); i++)
         {
            Home home = (Home) connectHomes.get(i);
            if (home.port < 0)
            {
               changed = true;
               home.port = ((Home) homes.get(i)).port;
            }
         }
         if (changed)
            locator.setConnectHomeList(connectHomes);
      }
   }

   protected ServerSocketFactory createServerSocketFactory() throws IOException
   {
      // only want to look at config if server socket factory has not already been set
      if(serverSocketFactory == null)
      {
         Object obj = configuration.get(Remoting.CUSTOM_SERVER_SOCKET_FACTORY);
         if (obj != null)
         {
            if (obj instanceof ServerSocketFactory)
            {
               serverSocketFactory = (ServerSocketFactory)obj;
            }
            else
            {
               throw new RuntimeException("Can not set custom server socket factory (" + obj +
                                          ") as is not of type javax.net.SocketFactory");
            }
         }

         if (serverSocketFactory == null)
         {
            // TODO: -TME This is another big hack because of dependancy on JMX within configuration
            // Have to wait till the mbean server is set before can actually set the server socket
            // factory since it is an mbean (new server's DI will fix all this). Would prefer this
            // to be in the setup() method...
            // Also, I can't cast the mbean proxy directly to ServerSocketFactory because it is not
            // an interface. Therefore, have to require that ServerSocketFactoryMBean is used.

            String serverSocketFactoryString = (String)configuration.get(SERVER_SOCKET_FACTORY);
            if(serverSocketFactoryString != null && serverSocketFactoryString.length() > 0)
            {
               try
               {
                  if(serverSocketFactoryString != null)
                  {
                     MBeanServer server = getMBeanServer();
                     ObjectName serverSocketFactoryObjName =
                        new ObjectName(serverSocketFactoryString);

                     if(server != null)
                     {
                        try
                        {
                           ServerSocketFactoryMBean serverSocketFactoryMBean =
                              (ServerSocketFactoryMBean)MBeanServerInvocationHandler.
                                 newProxyInstance(server, serverSocketFactoryObjName,
                                                  ServerSocketFactoryMBean.class, false);
                           serverSocketFactory =
                              new ServerSocketFactoryWrapper(serverSocketFactoryMBean);
                        }
                        catch(Exception e)
                        {
                           log.debug("Error creating mbean proxy for server socket factory " +
                              "for object name " + serverSocketFactoryObjName + ". " +
                              "Will try by class name.");
                        }
                     }
                     else
                     {
                        log.debug("The 'serverSocketFactory' attribute was set with a value, " +
                           "but the MBeanServer reference is null.");
                     }
                  }
               }
               catch(MalformedObjectNameException e)
               {
                  log.debug("Attibute value (" + serverSocketFactoryString + ") passed is not a " +
                     "valid ObjectName. Can not look up if is a mbean service. Will try by classname.");
               }
               catch(NullPointerException e)
               {
                  log.debug("Could not set up the server socket factory as a mbean service " +
                     "due to null pointer exception.");
               }

               // couldn't create from object name for mbean service, will try by class name
               if(serverSocketFactory == null)
               {
                  try
                  {
                     Class cl = ClassLoaderUtility.loadClass(serverSocketFactoryString, getClass());

                     Constructor serverSocketConstructor = null;
                     serverSocketConstructor = cl.getConstructor(new Class[]{});
                     serverSocketFactory =
                        (ServerSocketFactory)serverSocketConstructor.newInstance(new Object[] {});
                     log.trace("ServerSocketFactory (" + serverSocketFactoryString + ") loaded");
                  }
                  catch(Exception e)
                  {
                     log.debug("Could not create server socket factory by classname (" +
                        serverSocketFactoryString + ").  Error message: " + e.getMessage());
                  }
               }
            }
         }
      }

      if (serverSocketFactory == null && needsCustomSSLConfiguration(configuration))
      {
         try
         {
            SSLSocketBuilder socketBuilder = new SSLSocketBuilder(configuration);
            socketBuilder.setUseSSLServerSocketFactory(false);
            serverSocketFactory = socketBuilder.createSSLServerSocketFactory();
         }
         catch (IOException e)
         {
            throw new RuntimeException("Unable to create customized SSL socket factory", e);
         }
      }

      if(serverSocketFactory == null)
      {
         log.debug(this + " did not find server socket factory configuration as mbean service " +
            "or classname. Creating default server socket factory.");

         serverSocketFactory = getDefaultServerSocketFactory();
      }

      log.debug(this + " created server socket factory " + serverSocketFactory);

      serverSocketFactory = wrapServerSocketFactory(serverSocketFactory, configuration);
      return serverSocketFactory;

   }

   protected boolean justNeedsSSLClientMode(Map configuration)
   {
      if (configuration.size() == 1 &&
         configuration.containsKey(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE))
      {
         String useClientModeString =
            (String)configuration.get(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE);
         return Boolean.valueOf(useClientModeString).booleanValue();
      }

      if (configuration.size() == 1 &&
         configuration.containsKey(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE))
      {
         String useClientModeString =
            (String)configuration.get(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE);
         return Boolean.valueOf(useClientModeString).booleanValue();
      }

      if (configuration.size() == 2
            && configuration.containsKey(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE)
            && configuration.containsKey(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE))
      {
         String useClientModeString =
            (String)configuration.get(SSLSocketBuilder.REMOTING_SERVER_SOCKET_USE_CLIENT_MODE);
         return Boolean.valueOf(useClientModeString).booleanValue();
      }

      return false;
   }

   /**
    * Gets the default server socket factory to use for the server invoker. The intention is this
    * method will be overridden by sub-classes for their specific defaults.
    */
   protected ServerSocketFactory getDefaultServerSocketFactory() throws IOException
   {
      return ServerSocketFactory.getDefault();
   }

   protected ServerSocketFactory wrapServerSocketFactory(ServerSocketFactory ssf, Map config)
   {
      if (config == null)
      {
         return ssf;
      }

      Object o = config.get(Remoting.SOCKET_CREATION_SERVER_LISTENER);

      if (o == null)
      {
         return ssf;
      }

      if (o instanceof SocketCreationListener)
      {
         return new CreationListenerServerSocketFactory(ssf, (SocketCreationListener) o);
      }
      else if (o instanceof String)
      {
         try
         {
            Class c = ClassLoaderUtility.loadClass((String) o, ServerInvoker.class);
            SocketCreationListener listener = (SocketCreationListener)c.newInstance();
            return new CreationListenerServerSocketFactory(ssf, listener);
         }
         catch (Exception e)
         {
            log.warn("unable to instantiate class: " + o, e);
            return ssf;
         }
      }
      else
      {
         log.warn("unrecognized type for socket creation server listener: " + o);
         return ssf;
      }
   }

   /**
    * Handles both internal and external invocations (internal meaning only to be used within
    * remoting and external for ones that go to handlers.
    */
   protected Object handleInternalInvocation(InternalInvocation param,
                                             InvocationRequest invocation,
                                             ServerInvocationHandler handler) throws Throwable
   {
      Object result = null;
      String methodName = param.getMethodName();

      if(trace) { log.trace("handling InternalInvocation where method name = " + methodName); }

      // check if the invocation is for callback handling
      if(InternalInvocation.HANDLECALLBACK.equals(methodName))
      {
         String sessionId = ServerInvokerCallbackHandler.getId(invocation);
         if(trace) { log.trace("ServerInvoker (" + this + ") is being asked to deliver callback on client callback handler with session id of " + sessionId + "."); }

         CallbackContainer callbackContainer = null;
         
         if (singleCallbackContainer != null)
         {
            callbackContainer = singleCallbackContainer;
         }
         else
         {         
            callbackContainer = (CallbackContainer)clientCallbackListener.get(sessionId);
         }

         if(callbackContainer != null && callbackContainer.getCallbackHandler() != null)
         {
            Object[] params = param.getParameters();
            
            Callback callbackRequest = (Callback) params[0];
                                    
            Object obj = callbackContainer.getCallbackHandleObject();
            
            if (obj != null)
            {
               Map callbackHandleObject = callbackRequest.getReturnPayload();
               
               if(callbackHandleObject == null)
               {
                  callbackHandleObject = new HashMap();
               }
                              
               //We only want to add it if it is not null otherwise is a redundant operation
               callbackHandleObject.put(Callback.CALLBACK_HANDLE_OBJECT_KEY,
                                        obj);
               
               callbackRequest.setReturnPayload(callbackHandleObject);
            }
            
            InvokerCallbackHandler callbackHandler = callbackContainer.getCallbackHandler();
            
            callbackHandler.handleCallback(callbackRequest);
         }
         else
         {
            log.error("Could not find callback handler to call upon for handleCallback " +
                      "where session id equals " + sessionId);
         }
      }
      else if(InternalInvocation.ADDLISTENER.equals(methodName))
      {
         if(handler == null)
         {
            throw new InvalidConfigurationException(
               "Can not accept a callback listener since there are no ServerInvocationHandlers " +
               "registered. Please add via xml configuration or via the Connector's " +
               "addInvocationHandler() method.");

         }
         ServerInvokerCallbackHandler callbackHandler = getCallbackHandler(invocation);
         if (registerCallbackListeners)
         {
            connectionNotifier.addListenerFirst(callbackHandler);
            if(leasePeriod > 0)
            {
               leaseManagement = true;
            }
         }
         handler.addListener(callbackHandler);
      }
      else if(InternalInvocation.REMOVELISTENER.equals(methodName))
      {
         ServerInvokerCallbackHandler callbackHandler = removeCallbackHandler(invocation);
         if(callbackHandler != null)
         {
            if (registerCallbackListeners)
            {
//               connectionNotifier.removeListener(callbackHandler);
               removeConnectionListener(callbackHandler);
            }
            
            callbackHandler.destroy();
            
            if(handler == null)
            {
               throw new InvalidConfigurationException(
                  "Can not remove a callback listener since there are no ServerInvocationHandlers " +
                  "registered.  Please add via xml configuration or via the Connector's " +
                  "addInvocationHandler() method.");
            }
            
            handler.removeListener(callbackHandler);

            if(trace) { log.trace("ServerInvoker (" + this + ") removing server callback handler " + callbackHandler + "."); }
         }
         else
         {
            String sessionId = ServerInvokerCallbackHandler.getId(invocation);
            throw new RuntimeException("Can not remove callback listener from target server with " +
               "id of " + sessionId + " as it does not exist as a registered callback listener.");
         }
      }
      else if(InternalInvocation.GETCALLBACKS.equals(methodName))
      {
         ServerInvokerCallbackHandler callbackHandler = getCallbackHandler(invocation);
         if(trace) { log.trace("ServerInvoker (" + this + ") getting callbacks for callback handler " + callbackHandler + "."); }
         result = callbackHandler.getCallbacks(invocation.getRequestPayload());
      }
      else if(InternalInvocation.ACKNOWLEDGECALLBACK.equals(methodName))
      {
         ServerInvokerCallbackHandler callbackHandler = getCallbackHandler(invocation);
         if(trace) { log.trace("ServerInvoker (" + this + ") acknowledge callback on callback handler " + callbackHandler + "."); }
         callbackHandler.acknowledgeCallbacks(param);
      }
      else if(InternalInvocation.ADDCLIENTLISTENER.equals(methodName))
      {
         String sessionId = ServerInvokerCallbackHandler.getId(invocation);
         Object[] params = param.getParameters();

         // the only elements should be the callback handler and possibly the callback handle object
         if(params == null || params.length < 0 || params.length > 3)
         {
            log.debug("Received addClientListener InternalInvocation, but getParameters() " +
                      "returned: " + params);
            throw new RuntimeException(
               "InvokerCallbackHandler and callback handle object (optional) must be supplied as " +
               "the only parameter objects within the InternalInvocation when calling " +
               "addClientListener.");
         }

         InvokerCallbackHandler callbackHandler = (InvokerCallbackHandler)params[0];
         Object callbackHandleObject = params[1];
         CallbackContainer callbackContainer =
            new CallbackContainer(callbackHandler, callbackHandleObject);                          
         
         clientCallbackListener.put(sessionId, callbackContainer);
         
         //If there is only one CallbackContainer we store a direct reference to it to avoid
         //unnecessary lookups - TLF
         if (clientCallbackListener.size() == 1)
         {
            singleCallbackContainer = callbackContainer;
         }
         else
         {
            singleCallbackContainer = null;
         }

         log.debug("ServerInvoker (" + this + ") added client callback handler " + callbackHandler +
            " with session id of " + sessionId + " and callback handle object of " +
            callbackHandleObject + ".");

      }
      else if(InternalInvocation.REMOVECLIENTLISTENER.equals(methodName))
      {
         String sessionId = ServerInvokerCallbackHandler.getId(invocation);

         log.debug("ServerInvoker (" + this + ") removing client callback handler with session " +
            "id of " + sessionId + ".");

         Object cbo = clientCallbackListener.remove(sessionId);
         if(cbo == null)
         {
            throw new RuntimeException(
               "Can not remove callback listener from callback server with id of " + sessionId +
               " as it does not exist as a registered callback listener.");
         }
         //If there is only one CallbackContainer we store a direct reference to it to avoid
         //unnecessary lookups - TLF
         if (clientCallbackListener.size() == 1)
         {
            singleCallbackContainer =
               (CallbackContainer)clientCallbackListener.values().iterator().next();
         }
         else
         {
            singleCallbackContainer = null;
         }

      }
      
      else if(InternalInvocation.ADDSTREAMCALLBACK.equals(methodName))
      {
         StreamHandler streamHandler = getStreamHandler(invocation);
         if(handler instanceof StreamInvocationHandler)
         {
            InternalInvocation inv = (InternalInvocation)invocation.getParameter();
            // second parameter should be the param payload
            result = ((StreamInvocationHandler)handler).
               handleStream(streamHandler, (InvocationRequest)inv.getParameters()[1]);
         }
         else
         {
            log.debug("Client request is an InputStream, but the registered handlers do not " +
                      "implement the StreamInvocationHandler interface, so could not process call.");
            throw new RuntimeException(
               "No handler registered of proper type (StreamInvocationHandler).");
         }
      }
      else if (InternalInvocation.ECHO.equals(methodName))
      {
         Object response = null;
         Object[] objects = param.getParameters();
         if (objects != null && objects.length > 0)
            response = objects[0];
         
         if (trace)
         {
            log.trace(this + " echoing " + response);
         }
         return response;
      }
      else
      {
         log.debug("Error processing InternalInvocation.  Unable to process method " +
                   methodName + ". Please make sure this should be an InternalInvocation.");
         throw new RuntimeException(
            "Error processing InternalInvocation. Unable to process method " + methodName);
      }
      return result;
   }
  
   protected ServerInvocationHandler findInvocationHandler(String subsystem)
   {
      ServerInvocationHandler handler = null;

      if (singleHandler != null)
      {
         handler = singleHandler;
      }
      else
      {               
         if (subsystem != null)
         {
            handler = (ServerInvocationHandler)handlers.get(subsystem.toUpperCase());
         }
         else
         {
            // subsystem not specified, so will hope for a default one being set
            if (!handlers.isEmpty())
            {
               if (trace) { log.trace(this + " handling invocation with no subsystem explicitely specified, using the default handler"); }
               handler = (ServerInvocationHandler)handlers.values().iterator().next();
            }
         }
      }
      return handler;
   }
   
   /**
    * Called prior to an invocation.
    * TODO is sending in the arg appropriate?
    */
   protected void preProcess(String sessionId, ClassBytes arg, Map payload, InvokerLocator locator)
   {
   }

   /**
    * Called after an invocation.
    * TODO is sending in the arg appropriate?
    */
   protected void postProcess(String sessionId, Object param, Map payload, InvokerLocator locator)
   {
   }

   // Private --------------------------------------------------------------------------------------

   private ThreadPool createThreadPoolProxy(ObjectName objName)
   {
      ThreadPool pool;
      MBeanServer server = getMBeanServer();
      if(server != null)
      {
         ThreadPoolMBean poolMBean = (ThreadPoolMBean)MBeanServerInvocationHandler.
            newProxyInstance(server, objName, ThreadPoolMBean.class, false);

         pool = poolMBean.getInstance();
      }
      else
      {
         throw new RuntimeException("Can not register MBean ThreadPool as the ServerInvoker " +
            "has not been registered with a MBeanServer.");
      }
      return pool;
   }

   //TODO: -TME getting of datatype is duplicated in both the RemoteClientInvoker and the ServerInvoker
   private String getDataType(InvokerLocator locator)
   {
      String type = null;

      if(locator != null)
      {
         Map params = locator.getParameters();
         if(params != null)
         {
            type = (String) params.get(InvokerLocator.DATATYPE);
         }
      }
      return type;
   }

   private void terminateLease(InvocationRequest invocation)
   {
      if (invocation != null)
      {
         // clientSessionId == MicroRemoteClientInvoker.invokerSessionID.
         String clientSessionId = invocation.getSessionId();
         Lease clientLease = (Lease)clientLeases.get(clientSessionId);

         if (clientLease != null)
         {
            boolean clientOnlyTerminated = false;
            // now have to determine if is just Client that disconnected
            // or if all Clients disconnected, thus the client invoker
            // is also disconnected as well.
            Map reqMap = invocation.getRequestPayload();
            if (reqMap != null)
            {
               Object holderObj = reqMap.get(ClientHolder.CLIENT_HOLDER_KEY);
               if (holderObj != null && holderObj instanceof ClientHolder)
               {
                  // just a client that disconnected, so only need to terminate lease for
                  // that particular client (by client session id).
            	  if (trace) log.trace("terminating client lease: " + clientSessionId);
                  ClientHolder holder = (ClientHolder) holderObj;
                  clientLease.terminateLease(holder.getSessionId());
                  clientOnlyTerminated = true;
               }
            }

            // now see if client invoker needs to be terminated
            if (!clientOnlyTerminated)
            {
               if (trace) log.trace("terminating invoker lease: " + clientSessionId);
               clientLease.terminateLease(clientSessionId);
               clientLeases.remove(clientSessionId);
            }
         }
         else
         {
             String type = "invoker";
        	    Map reqMap = invocation.getRequestPayload();
             if (reqMap != null)
             {
                Object holderObj = reqMap.get(ClientHolder.CLIENT_HOLDER_KEY);
                if (holderObj != null && holderObj instanceof ClientHolder)
                {
                	type = "client";
                }
             }
             log.debug("Asked to terminate " + type + " lease for invoker session id "
                       + clientSessionId + ", but lease for this id could not be found." +"" +
                       "Probably has been removed due to connection failure.");
         }
      }
   }

   private void updateClientLease(InvocationRequest invocation)
   {
      if(invocation != null)
      {
         String clientSessionId = invocation.getSessionId();
         if (invocation.getRequestPayload() != null)
         {
            // Remove per invocation timeout.
            invocation.getRequestPayload().remove(TIMEOUT);
         }
         if(clientSessionId != null)
         {
            if(trace) { log.trace("Getting lease for invoker session id: " + clientSessionId); }

            Lease clientLease = (Lease)clientLeases.get(clientSessionId);
            if(clientLease == null)
            {
               Lease newClientLease = new Lease(clientSessionId, leasePeriod,
                                                locator.getLocatorURI(),
                                                invocation.getRequestPayload(),
                                                connectionNotifier,
                                                clientLeases);

               clientLeases.put(clientSessionId, newClientLease);
               newClientLease.startLease();
               
               if(trace) { log.trace("No lease established for invoker session id (" + clientSessionId + 
                                    "), so starting a new one:" + newClientLease); }
            }
            else
            {
               if (useClientConnectionIdentity)
               {
                  String leasePingerId = (String) invocation.getRequestPayload().get(LeasePinger.LEASE_PINGER_ID);;
                  if (leasePingerId == null || leasePingerId.equals(clientLease.getLeasePingerId()))
                  {
                     // including request payload from invocation as may contain updated list of clients.
                     if (trace) log.trace(clientLease + " matches: leasePingerId: " + leasePingerId);
                     clientLease.updateLease(leasePeriod, invocation.getRequestPayload());
                     if(trace) { log.trace("Updated lease for invoker session id (" + clientSessionId + ")"); }
                  }
                  else
                  {
                     if (trace) log.trace(clientLease + " does not match: leasePingerId: " + leasePingerId);
                     if (trace) log.trace("terminating invoker lease: " + clientLease);
                     clientLease.terminateLeaseUponFailure(clientSessionId);
                     clientLeases.remove(clientSessionId);

                     Lease newClientLease = new Lease(clientSessionId, leasePeriod,
                                                      locator.getLocatorURI(),
                                                      invocation.getRequestPayload(),
                                                      connectionNotifier,
                                                      clientLeases);

                     clientLeases.put(clientSessionId, newClientLease);
                     newClientLease.startLease();

                     if(trace) { log.trace("starting a new lease:" + newClientLease); }
                  }
               }
               else
               {
                  // including request payload from invocation as may contain updated list of clients.
                  clientLease.updateLease(leasePeriod, invocation.getRequestPayload());

                  if(trace) { log.trace("Updated lease for client session id (" + clientSessionId + ")"); }
               }
            }
         }
      }
   }

   private boolean checkForClientLease(String invokerSessionId)
   {
      if(leaseManagement && invokerSessionId != null)
      {
         if(trace) { log.trace("Checking lease for invoker session id: " + invokerSessionId); }

         Lease clientLease = (Lease)clientLeases.get(invokerSessionId);
         if(clientLease == null)
         {
            if(trace) { log.trace("No lease established for invoker session id (" + invokerSessionId + ")"); }
            return false;
         }
         else
         {
            if(trace) { log.trace("Found lease for invoker session id (" + invokerSessionId + ")"); }
            return true;
         }
      }

      return false;
   }

   /**
    * Takes the real invocation from the client out of the OnewayInvocation and then executes the
    * invoke() with the real invocation on a seperate thread.
    */
   private void handleOnewayInvocation(OnewayInvocation onewayInvocation,
                                       InvocationRequest invocation) throws Throwable
   {
      Object[] objs = onewayInvocation.getParameters();

      // The oneway invocation should contain the real param as it's only param in parameter array
      Object realParam = objs[0];
      invocation.setParameter(realParam);

      final InvocationRequest newInvocation = invocation;

      ThreadPool executor = getOnewayThreadPool();
      Runnable onewayRun = new Runnable()
      {
         public void run()
         {
            try
            {
               invoke(newInvocation);
            }
            catch(Throwable e)
            {
               // throw away exception since can't get it back to original caller
               log.error("Error executing server oneway invocation request: " + newInvocation, e);
            }
         }
      };

      if(trace) { log.trace(this + " placing " + invocation + " in onewayThreadPool"); }
      executor.run(onewayRun);
   }

   private StreamHandler getStreamHandler(InvocationRequest invocation) throws Exception
   {
      InternalInvocation inv = (InternalInvocation)invocation.getParameter();
      String locator = (String)inv.getParameters()[0];
      return new StreamHandler(locator);
   }

   private ServerInvokerCallbackHandler getCallbackHandler(InvocationRequest invocation)
      throws Exception
   {
      ServerInvokerCallbackHandler callbackHandler = null;
      String id = ServerInvokerCallbackHandler.getId(invocation);
      synchronized(callbackHandlers)
      {
         callbackHandler = (ServerInvokerCallbackHandler)callbackHandlers.get(id);

         // if does not exist, create it
         if(callbackHandler == null)
         {
            callbackHandler = new ServerInvokerCallbackHandler(invocation, getLocator(), this);
            callbackHandlers.put(id, callbackHandler);
         }
      }

      callbackHandler.connect();
      if(trace) { log.trace("ServerInvoker (" + this + ") adding server callback handler " + callbackHandler + " with id of " + id + "."); }
      return callbackHandler;
   }

   public ServerInvokerCallbackHandler removeCallbackHandler(InvocationRequest invocation)
   {
      String id = ServerInvokerCallbackHandler.getId(invocation);
      ServerInvokerCallbackHandler callbackHandler = null;

      synchronized(callbackHandlers)
      {
         callbackHandler = (ServerInvokerCallbackHandler) callbackHandlers.remove(id);
      }
      log.debug(this + " removed " + callbackHandler);
      return callbackHandler;
   }
   
   public void shutdownCallbackHandler(ServerInvokerCallbackHandler callbackHandler, InvocationRequest invocation)
   {
      removeCallbackHandler(invocation);
      if (registerCallbackListeners)
      {
         removeConnectionListener(callbackHandler);
      }
      ServerInvocationHandler handler = findInvocationHandler(invocation.getSessionId());
      if (handler != null)
      {
         handler.removeListener(callbackHandler);
         if(trace) { log.trace(this + " removing server callback handler " + callbackHandler + "."); }
      }
      else
      {
         log.debug(this + " cannot remove " + callbackHandler + ": associated ServerInvocationHandler not longer exists");
      }
   }

   // Inner classes --------------------------------------------------------------------------------

   public static class InvalidStateException extends Exception
   {
      public InvalidStateException(String msg)
      {
         super(msg);
      }
   }

   private class CallbackContainer
   {
      private InvokerCallbackHandler handler;
      private Object handleObject;

      public CallbackContainer(InvokerCallbackHandler handler, Object handleObject)
      {
         this.handler = handler;
         this.handleObject = handleObject;
      }

      public InvokerCallbackHandler getCallbackHandler()
      {
         return handler;
      }

      public Object getCallbackHandleObject()
      {
         return handleObject;
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
   
   static private InetAddress getAddressByName(final String host) throws UnknownHostException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return InetAddress.getByName(host);
      }
      
      try
      {
         return (InetAddress)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               return InetAddress.getByName(host);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (UnknownHostException) e.getCause();
      }
   }
}
