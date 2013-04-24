package org.jboss.remoting.transport.socket;

import org.jboss.logging.Logger;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.ConnectionFailedException;
import org.jboss.remoting.Home;
import org.jboss.remoting.InvocationFailureException;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.RemoteClientInvoker;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.Version;
import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.remoting.invocation.OnewayInvocation;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.VersionedMarshaller;
import org.jboss.remoting.marshal.VersionedUnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.util.propertyeditor.PropertyEditors;

import java.beans.IntrospectionException;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.MarshalException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import EDU.oswego.cs.dl.util.concurrent.Semaphore;

/**
 * SocketClientInvoker uses Sockets to remotely connect to the a remote ServerInvoker, which must be
 * a SocketServerInvoker.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 *
 * @version $Revision: 5476 $
 */
public class MicroSocketClientInvoker extends RemoteClientInvoker
{
   // Constants ------------------------------------------------------------------------------------

   private static final Logger log = Logger.getLogger(MicroSocketClientInvoker.class);

   /**
    * Can be either true or false and will indicate if client socket should have TCP_NODELAY turned
    * on or off. TCP_NODELAY is for a specific purpose; to disable the Nagle buffering algorithm.
    * It should only be set for applications that send frequent small bursts of information without
    * getting an immediate response; where timely delivery of data is required (the canonical
    * example is mouse movements). The default is false.
    */
   public static final String TCP_NODELAY_FLAG = "enableTcpNoDelay";

   /**
    * The client side maximum number of threads. The default is MAX_POOL_SIZE.
    */
   public static final String MAX_POOL_SIZE_FLAG = "clientMaxPoolSize";

   /**
    * Specifies the fully qualified class name for the custom SocketWrapper implementation to use on
    * the client. Note, will need to make sure this is marked as a client parameter (using the
    * 'isParam' attribute). Making this change will not affect the marshaller/unmarshaller that is
    * used, which may also be a requirement.
    */
   public static final String CLIENT_SOCKET_CLASS_FLAG = "clientSocketClass";
   
   /** Key for setting timeout used by OnewayConnectionTask */
   public static final String ONEWAY_CONNECTION_TIMEOUT = "onewayConnectionTimeout";
   
   /** Key to determine if client side oneway invocations should wait to read version.
    *  See JBREM-706.
    */
   public static final String USE_ONEWAY_CONNECTION_TIMEOUT = "useOnewayConnectionTimeout";

   /** Key for setting time to wait to get permission to get a connection */
   public static final String CONNECTION_WAIT = "connectionWait";
   
   /** Key for setting socket write timeout */
   public static final String WRITE_TIMEOUT = "writeTimeout";
   
   /**
    * Default value for enable TCP nodelay. Value is false.
    */
   public static final boolean TCP_NODELAY_DEFAULT = false;

   /**
    * Default maximum number of times a invocation will be made when it gets a SocketException.
    * Default is 3.
    */
   public static final int MAX_CALL_RETRIES = 3;

   /**
    * Default maximum number of socket connections allowed at any point in time. Default is 50.
    */
   public static final int MAX_POOL_SIZE = 50;

   /** Default timeout value used by OnewayConnectionTask.  Value is 2 seconds. */
   public static final int ONEWAY_CONNECTION_TIMEOUT_DEFAULT = 2000;
   
   /** Default time to wait to get permission to get a connection */
   public static final int CONNECTION_WAIT_DEFAULT = 30000;

   // Static ---------------------------------------------------------------------------------------

   private static boolean trace = log.isTraceEnabled();

   /**
    * Used for debugging (tracing) connections leaks
    */
   static int counter = 0;

   protected static final Map connectionPools = new HashMap();
   
   protected static final Map semaphores = new HashMap();

   // Performance measurements
   public static long getSocketTime = 0;
   public static long readTime = 0;
   public static long writeTime = 0;
   public static long serializeTime = 0;
   public static long deserializeTime = 0;
   
   private static final String patternString = "^.*(?:connection.*reset|connection.*closed|broken.*pipe).*$";
   private static final Pattern RETRIABLE_ERROR_MESSAGE = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
   
   /**
    * Close all sockets in a specific pool.
    */
   public static void clearPool(LinkedList thepool)
   {
      try
      {
         if (thepool == null)
         {
            return;
         }
         synchronized (thepool)
         {
            int size = thepool.size();
            for (int i = 0; i < size; i++)
            {
               SocketWrapper socketWrapper = (SocketWrapper)thepool.removeFirst();
               try
               {
                  socketWrapper.close();
                  socketWrapper = null;
               }
               catch (Exception ignored)
               {
               }
            }
         }
      }
      catch (Exception ex)
      {
         log.debug("Failure", ex);
      }
   }

   /**
    * Close all sockets in all pools.
    */
   public static void clearPools()
   {
      synchronized (connectionPools)
      {
         for(Iterator i = connectionPools.keySet().iterator(); i.hasNext();)
         {
            ServerAddress sa = (ServerAddress) i.next();

            if (trace) { log.trace("clearing pool for " + sa); }
            clearPool((LinkedList) connectionPools.get(sa));
            i.remove();
         }
         semaphores.clear();
      }
   }

   // Attributes -----------------------------------------------------------------------------------

   private Constructor clientSocketConstructor;
   private boolean reuseAddress;

   protected InetAddress addr;
   protected int port;

   // flag being set on true by a disconnect request. If trying to create a connection goes on in a
   // loop and a disconnect request arrives, this flag will be used to sent this information into
   // the connect loop
   // private volatile boolean bailOut;

   /**
    * Indicates if will check the socket connection when getting from pool by sending byte over the
    * connection to validate is still good.
    */
   protected boolean shouldCheckConnection;

   /**
    * If the TcpNoDelay option should be used on the socket.
    */
   protected boolean enableTcpNoDelay;

   protected String clientSocketClassName;
   protected Class clientSocketClass;
   protected int numberOfCallRetries;
   protected int maxPoolSize;
   protected int onewayConnectionTimeout;
   protected boolean useOnewayConnectionTimeout = true;
   protected int connectionWait = CONNECTION_WAIT_DEFAULT;

   /**
    * Pool for this invoker. This is shared between all instances of proxies attached to a specific
    * invoker.
    */
   protected LinkedList pool;
   
   //Semaphore is also shared between all proxies - must 1-1 correspondence between pool and semaphore
   protected Semaphore semaphore;
   

   /**
    * connection information
    */
   protected ServerAddress address;
   protected Home home;
   
   /**
    * Socket configuration parameters.
    */
   protected boolean keepAlive;
   protected boolean keepAliveSet;
   protected boolean oOBInline;
   protected boolean oOBInlineSet;
   protected int receiveBufferSize = - 1;
   protected int sendBufferSize = -1;
   protected boolean soLinger;
   protected boolean soLingerSet;
   protected int soLingerDuration = -1;
   protected int trafficClass = -1;
   
   /**
    * If true, an IOException with message such as "Connection reset by peer: socket write error" will 
    * be treated like a SocketException.
    */
   protected boolean generalizeSocketException;
   
   protected int writeTimeout = -1;

   // Constructors ---------------------------------------------------------------------------------

   public MicroSocketClientInvoker(InvokerLocator locator)
   {
      this(locator, null);
   }

   public MicroSocketClientInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);

      clientSocketConstructor = null;
      reuseAddress = true;
      shouldCheckConnection = false;
      enableTcpNoDelay = TCP_NODELAY_DEFAULT;
      clientSocketClassName = ClientSocketWrapper.class.getName();
      clientSocketClass = null;
      numberOfCallRetries = MAX_CALL_RETRIES;
      pool = null;
      maxPoolSize = MAX_POOL_SIZE;
      onewayConnectionTimeout = ONEWAY_CONNECTION_TIMEOUT_DEFAULT;

      try
      {
         setup();
      }
      catch (Exception ex)
      {
         log.debug("Error setting up " + this, ex);
         throw new RuntimeException(ex.getMessage(), ex);
      }

      log.debug(this + " constructed");
   }

   // Public ---------------------------------------------------------------------------------------

   /**
    * Indicates if will check socket connection when returning from pool by sending byte to the
    * server. Default value will be false.
    */
   public boolean checkingConnection()
   {
      return shouldCheckConnection;
   }

   /**
    * Returns if newly created sockets will have SO_REUSEADDR enabled. Default is for this to be
    * true.
    */
   public boolean getReuseAddress()
   {
      return reuseAddress;
   }

   /**
    * Sets if newly created socket should have SO_REUSEADDR enable. Default is true.
    */
   public void setReuseAddress(boolean reuse)
   {
      reuseAddress = reuse;
   }

   public boolean isKeepAlive()
   {
      return keepAlive;
   }

   public void setKeepAlive(boolean keepAlive)
   {
      this.keepAlive = keepAlive;
      keepAliveSet = true;
   }

   public boolean isOOBInline()
   {
      return oOBInline;
   }

   public void setOOBInline(boolean inline)
   {
      oOBInline = inline;
      oOBInlineSet = true;
   }

   public int getReceiveBufferSize()
   {
      return receiveBufferSize;
   }

   public void setReceiveBufferSize(int receiveBufferSize)
   {
      this.receiveBufferSize = receiveBufferSize;
   }

   public int getSendBufferSize()
   {
      return sendBufferSize;
   }

   public void setSendBufferSize(int sendBufferSize)
   {
      this.sendBufferSize = sendBufferSize;
   }

   public boolean isSoLinger()
   {
      return soLinger;
   }
   
   public int getSoLingerDuration()
   {
      return soLingerDuration;
   }

   public void setSoLinger(boolean soLinger)
   {
      this.soLinger = soLinger;
      soLingerSet = true;
   }

   public void setSoLingerDuration(int soLingerDuration)
   {
      this.soLingerDuration = soLingerDuration;
   }

   public int getTrafficClass()
   {
      return trafficClass;
   }

   public void setTrafficClass(int trafficClass)
   {
      this.trafficClass = trafficClass;
   }

   public int getWriteTimeout()
   {
      return writeTimeout;
   }

   public void setWriteTimeout(int writeTimeout)
   {
      this.writeTimeout = writeTimeout;
   }

   public boolean isGeneralizeSocketException()
   {
      return generalizeSocketException;
   }

   public void setGeneralizeSocketException(boolean generalizeSocketException)
   {
      this.generalizeSocketException = generalizeSocketException;
   }

   public synchronized void disconnect()
   {
      log.debug(this + " disconnecting ...");
//      bailOut = true;
      super.disconnect();
   }

   public void flushConnectionPool()
   {
      synchronized (pool)
      {
         while (pool != null && pool.size() > 0)
         {
            SocketWrapper socketWrapper = (SocketWrapper)pool.removeFirst();
            try
            {
               socketWrapper.close();
            }
            catch (IOException e)
            {
               log.debug("Failed to close socket wrapper", e);
            }
         }
      }
   }
   
   public int getConnectionWait()
   {
      return connectionWait;
   }

   public void setConnectionWait(int connectionWait)
   {
      this.connectionWait = connectionWait;
   }

   public Home getHomeInUse()
   {
      return home;
   }

   /**
    * Sets the number of times an invocation will retry based on getting SocketException.
    */
   public void setNumberOfCallRetries(int numberOfCallRetries)
   {
      if (numberOfCallRetries < 1)
      {
         this.numberOfCallRetries = MAX_CALL_RETRIES;
      }
      else
      {
         this.numberOfCallRetries = numberOfCallRetries;
      }
   }

   public int getNumberOfCallRetries()
   {
      return numberOfCallRetries;
   }

   /**
    * Sets the number of retries to get a socket connection.
    *
    * @param numberOfRetries Must be a number greater than 0.
    */
   public void setNumberOfRetries(int numberOfRetries)
   {
      log.warn("numberOfRetries is no longer used");
   }

   public int getNumberOfRetries()
   {
      log.warn("numberOfRetries is no longer used");
      return -1;
   }

   /**
    * The name of of the server.
    */
   public String getServerHostName() throws Exception
   {
      return address.address;
   }
   
   public int getNumberOfUsedConnections()
   {
      if (semaphore == null)
         return 0;
      
      return maxPoolSize - (int) semaphore.permits();
   }
   
   public int getNumberOfAvailableConnections()
   {
      if (semaphore == null)
         return 0;
      
      return (int) semaphore.permits();
   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   protected void setup() throws Exception
   {
      Properties props = new Properties();
      props.putAll(configuration);
      mapJavaBeanProperties(MicroSocketClientInvoker.this, props, false);
      configureParameters();

      if (!InvokerLocator.MULTIHOME.equals(locator.getHost()))
      {
         addr = getAddressByName(locator.getHost());
         port = locator.getPort();
         address = createServerAddress(addr, port);
      }
      else
      {
         List homes = locator.getConnectHomeList();
         if (homes.size() == 1)
         {
            // Treat as in non MULTIHOME case.
            Home home = (Home) homes.iterator().next();
            addr = getAddressByName(home.host);
            address = createServerAddress(addr, home.port);
         }
      }
   }

   protected void configureParameters()
   {
      Map params = configuration;

      if (params == null)
      {
         return;
      }

      // look for enableTcpNoDelay param
      Object val = params.get(TCP_NODELAY_FLAG);
      if (val != null)
      {
         try
         {
            enableTcpNoDelay = Boolean.valueOf((String)val).booleanValue();
            log.debug(this + " setting enableTcpNoDelay to " + enableTcpNoDelay);
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + TCP_NODELAY_FLAG + " value of " +
                     val + " to a boolean value.");
         }
      }

      // look for maxPoolSize param
      val = params.get(MAX_POOL_SIZE_FLAG);
      if (val != null)
      {
         try
         {
            maxPoolSize = Integer.valueOf((String)val).intValue();
            log.debug(this + " setting maxPoolSize to " + maxPoolSize);
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + MAX_POOL_SIZE_FLAG + " value of " +
                     val + " to a int value");
         }
      }

      // look for client socket class name
      val = params.get(CLIENT_SOCKET_CLASS_FLAG);
      if (val != null)
      {
         String value = (String)val;
         if (value.length() > 0)
         {
            clientSocketClassName = value;
            log.debug(this + " setting client socket wrapper class name to " + clientSocketClassName);
         }
      }

      val = params.get(SocketServerInvoker.CHECK_CONNECTION_KEY);
      if (val != null && ((String)val).length() > 0)
      {
         String value = (String) val;
         shouldCheckConnection = Boolean.valueOf(value).booleanValue();
         log.debug(this + " setting shouldCheckConnection to " + shouldCheckConnection);
      }
      else if (getVersion() == Version.VERSION_1)
      {
         shouldCheckConnection = true;
         log.debug(this + " setting shouldCheckConnection to " + shouldCheckConnection);
      }
      
      // look for onewayConnectionTimeout param
      val = params.get(ONEWAY_CONNECTION_TIMEOUT);
      if (val != null)
      {
         try
         {
            onewayConnectionTimeout = Integer.valueOf((String)val).intValue();
            log.debug(this + " setting onewayConnectionTimeout to " + onewayConnectionTimeout);
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + ONEWAY_CONNECTION_TIMEOUT + " value of " +
                     val + " to an int value");
         }
      }
      
      // look for useOnewayConnectionTimeout param
      val = params.get(USE_ONEWAY_CONNECTION_TIMEOUT);
      if (val != null)
      {
         try
         {
            useOnewayConnectionTimeout = Boolean.valueOf((String)val).booleanValue();
            log.debug(this + " setting useOnewayConnectionTimeout to " + useOnewayConnectionTimeout);
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + USE_ONEWAY_CONNECTION_TIMEOUT + " value of " +
                     val + " to a boolean value");
         }
      }
      
      // look for writeTimeout param
      val = params.get(WRITE_TIMEOUT);
      if (val != null)
      {
         try
         {
            writeTimeout = Integer.valueOf((String)val).intValue();
            log.debug(this + " setting writeTimeout to " + writeTimeout);
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + WRITE_TIMEOUT + " value of " +
                     val + " to an int value");
         }
      }
   }

   protected ServerAddress createServerAddress(InetAddress addr, int port)
   {
      return new ServerAddress(addr.getHostAddress(), port, enableTcpNoDelay, -1, maxPoolSize);
   }

   protected void finalize() throws Throwable
   {
      disconnect();
      super.finalize();
   }

   protected synchronized void handleConnect() throws ConnectionFailedException
   {
      initPool();
      
      if (InvokerLocator.MULTIHOME.equals(locator.getHost()))
      {
         home = getUsableAddress(locator);
         if (home == null)
         {
            throw new ConnectionFailedException(this + " unable to find a usable address for: " + home);
         }
         locator.setHomeInUse(home);
      }
      else
      {
         home = new Home(locator.getHost(), locator.getPort());
      }
   }

   protected Home getUsableAddress(InvokerLocator locator)
   {
      List homes = getConnectHomes();
      Iterator it = homes.iterator();
      Home home = null;
      
      while (it.hasNext())
      {
         try
         {
            home = (Home) it.next();
            addr = getAddressByName(home.host);
            address = createServerAddress(addr, home.port);
            invoke(new InvocationRequest(null, null, ServerInvoker.ECHO, null, null, null));
            if (trace) log.trace(this + " able to contact server at: " + home);
            return home;
         }
         catch (Throwable e)
         {
            log.debug(this + " unable to contact server at: " + home);
         }
      }
   
      return null;
   }

   protected synchronized void handleDisconnect()
   {
      clearPools();
      clearPool(pool);
   }

   /**
    * Each implementation of the remote client invoker should have a default data type that is used
    * in the case it is not specified in the invoker locator URI.
    */
   protected String getDefaultDataType()
   {
      return SerializableMarshaller.DATATYPE;
   }

   protected Object transport(String sessionID, Object invocation, Map metadata,
                              Marshaller marshaller, UnMarshaller unmarshaller)
         throws IOException, ConnectionFailedException, ClassNotFoundException
   {
      SocketWrapper socketWrapper = null;
      Object response = null;
      boolean oneway = false;

      // tempTimeout < 0 will indicate there is no per invocation timeout.
      int tempTimeout = -1;
      int timeLeft = -1;
      int savedTimeout = -1;
      long start = -1;

      if(metadata != null)
      {
         // check to see if is one way invocation and return after writing invocation if is
         Object val = metadata.get(org.jboss.remoting.Client.ONEWAY_FLAG);
         if(val != null && val instanceof String && Boolean.valueOf((String)val).booleanValue())
         {
            oneway = true;
         }

         // look for temporary timeout values
         String tempTimeoutString = (String) metadata.get(ServerInvoker.TIMEOUT);
         {
            if (tempTimeoutString != null)
            {
               try
               {
                  tempTimeout = Integer.valueOf(tempTimeoutString).intValue();
                  log.debug(this + " setting timeout to " + tempTimeout + " for this invocation");
               }
               catch (Exception e)
               {
                  log.warn(this + " could not convert " + ServerInvoker.TIMEOUT + " value of " +
                           tempTimeoutString + " to an integer value.");
               }
            }
         }
      }
      
      if (tempTimeout >= 0)
      {
         start = System.currentTimeMillis();
      }

      boolean serverSideOneway = false;
      if (oneway && invocation instanceof InvocationRequest)
      {
         InvocationRequest ir = (InvocationRequest) invocation;
         if (ir.getParameter() instanceof OnewayInvocation)
            serverSideOneway = true;
      }
      
      int retryCount = 0;
      Exception sockEx = null;

      for (; retryCount < numberOfCallRetries; retryCount++)
      {
         if (trace) log.trace(this + " retryCount: " + retryCount);
         if (0 < tempTimeout)
         {
            // If a per invocation timeout has been set, the time spent retrying
            // should count toward the elapsed time.
            timeLeft = (int) (tempTimeout - (System.currentTimeMillis() - start));
            if (timeLeft <= 0)
               break;
         }

         try
         {
            boolean tryPool = retryCount < (numberOfCallRetries - 1)
                                 || maxPoolSize == 1
                                 || numberOfCallRetries == 1;
            socketWrapper = getConnection(marshaller, unmarshaller, tryPool, timeLeft);
            log.trace(this + " got socketWrapper: " + socketWrapper);
         }
         catch (InterruptedException e)
         {
            semaphore.release();
            if (trace) log.trace(this + " released semaphore: " + semaphore.permits(), e);
            throw new RuntimeException(e);
         }
         catch (Exception e)
         {
//            if (bailOut)
//               return null;
            semaphore.release();
            if (trace) log.trace(this + " released semaphore: " + semaphore.permits(), e);
            sockEx =  new CannotConnectException(
                  "Can not get connection to server. Problem establishing " +
                  "socket connection for " + locator, e);
            continue;
         }

         if (tempTimeout >= 0)
         {
            timeLeft = (int) (tempTimeout - (System.currentTimeMillis() - start));
            if (timeLeft <= 0)
               break;
            savedTimeout = socketWrapper.getTimeout();            
            socketWrapper.setTimeout(timeLeft);
         }

         try
         {
            int version = getVersion();
            boolean performVersioning = Version.performVersioning(version);

            OutputStream outputStream = socketWrapper.getOutputStream();
            log.trace(this + "got outputStream: " + outputStream);
            if (performVersioning)
            {
               log.trace(this + " writing version");
               writeVersion(outputStream, version);
               log.trace(this + " wrote version");
            }

            //TODO: -TME so this is messed up as now ties remoting versioning to using a marshaller type
            versionedWrite(outputStream, marshaller, invocation, version);

            if (serverSideOneway)
            {
               if(trace) { log.trace(this + " sent oneway invocation, so not waiting for response, returning null"); }
            }
            else if (oneway)
            {
               if (performVersioning && useOnewayConnectionTimeout)
               {
                  int onewaySavedTimeout = socketWrapper.getTimeout();
                  socketWrapper.setTimeout(onewayConnectionTimeout);
                  InputStream inputStream = socketWrapper.getInputStream();
                  version = readVersion(inputStream);
                  if (version == -1)
                  {
                     throw new EOFException("end of file");
                  }
                  if (version == SocketWrapper.CLOSING)
                  {
                     log.trace(this + " received version 254: treating as end of file");
                     throw new EOFException("end of file");
                  }

                  // Note that if an exception is thrown, the socket is thrown away,
                  // so there's no need to reset the timeout value.
                  socketWrapper.setTimeout(onewaySavedTimeout);
               }
            }
            else
            {
               InputStream inputStream = socketWrapper.getInputStream();
               if (performVersioning)
               {
                  version = readVersion(inputStream);
                  if (version == -1)
                  {
                     throw new EOFException("end of file");
                  }
                  if (version == SocketWrapper.CLOSING)
                  {
                     log.trace(this + " received version 254: treating as end of file");
                     throw new EOFException("end of file");
                  }
               }

               response = versionedRead(inputStream, unmarshaller, version);
            }

            // Note that resetting the timeout value after closing the socket results
            // in an exception, so the reset is not done in a finally clause.  However,
            // if a catch clause is ever added that does not close the socket, care
            // must be taken to reset the timeout in that case.
            if (tempTimeout >= 0)
            {
               socketWrapper.setTimeout(savedTimeout);
            }
         }
         catch (SocketException sex)
         {
            handleRetriableException(socketWrapper, sex, retryCount);
            sockEx = sex;
            continue;
         }
         catch (EOFException ex)
         {
            handleRetriableException(socketWrapper, ex, retryCount);
            sockEx = ex;
            continue;
         }
         catch (IOException e)
         {
            if (isGeneralizeSocketException() && e.getMessage() != null && RETRIABLE_ERROR_MESSAGE.matcher(e.getMessage()).matches())
            {
               handleRetriableException(socketWrapper, e, retryCount);
               sockEx = new SocketException(e.getMessage());
               continue;
            }
            else
            {
               return handleOtherException(e, semaphore, socketWrapper, oneway);
            }
         }
         catch (Exception ex)
         {
            return handleOtherException(ex, semaphore, socketWrapper, oneway);
         }

         // call worked, so no need to retry
         break;
      }

      // need to check if ran out of retries
      if (retryCount >= numberOfCallRetries)
      {
         handleException(sockEx, socketWrapper);
      }
      
      if (response == null && tempTimeout > 0 && timeLeft <= 0)
      {
         if (sockEx == null)
         {
            sockEx =  new CannotConnectException(
                         "Can not get connection to server. Timed out establishing " +
                         "socket connection for " + locator);
         }
         handleException(sockEx, socketWrapper);
      }

      // Put socket back in pool for reuse
      synchronized (pool)
      {
         if (pool.size() < maxPoolSize)
         {
            pool.add(socketWrapper);
            if (trace) { log.trace(this + " returned " + socketWrapper + " to pool"); }
         }
         else
         {
            if (trace) { log.trace(this + "'s pool is full, will close the connection"); }
            try
            {
               socketWrapper.close();
            }
            catch (Exception ignored)
            {
            }
         }         
         semaphore.release();
         if (trace) log.trace(this + " released semaphore: " + semaphore.permits());
      }

      if (trace && !oneway) { log.trace(this + " received response " + response);  }
      return response;
   }

   protected Object handleException(Exception ex, SocketWrapper socketWrapper)
      throws ClassNotFoundException, InvocationFailureException
   {
      if (ex instanceof ClassNotFoundException)
      {
         //TODO: -TME Add better exception handling for class not found exception
         log.debug("Error loading classes from remote call result.", ex);
         throw (ClassNotFoundException)ex;
      }
      
      if (ex instanceof CannotConnectException)
      {
         log.debug(this, ex);
         throw (CannotConnectException) ex;
      }
      
      if (ex instanceof InterruptedException)
      {
         log.debug(this, ex);
         throw new RuntimeException(ex);
      }

      throw new InvocationFailureException("Unable to perform invocation", ex);
   }
   
   protected void handleRetriableException(SocketWrapper socketWrapper, Exception e, int retryCount)
   {
      if (trace) log.trace(this + "(" + socketWrapper + ") got Exception: " + e);

      try
      {
         semaphore.release();
         if (trace) log.trace(this + " released semaphore: " + semaphore.permits());
         socketWrapper.close();            
      }
      catch (Exception ex)
      {
         if (trace) { log.trace(this + " couldn't successfully close its socketWrapper", ex); }
      }

      /**
       * About to run out of retries and
       * pool may be full of timed out sockets,
       * so want to flush the pool and try with
       * fresh socket as a last effort.
       */
      if (retryCount == (numberOfCallRetries - 2))
      {
         flushConnectionPool();
      }
      
      if (trace)
      {
         if (retryCount < (numberOfCallRetries - 1))
         {
            log.trace(this + " will try again, retries: " + retryCount + " < " + numberOfCallRetries);
         }
         else
         {
            log.trace(this + " retries exhausted");               
         }
      }
   }

   protected Object handleOtherException(Exception ex, Semaphore semaphore, SocketWrapper socketWrapper, boolean oneway)
   throws ClassNotFoundException, InvocationFailureException
   {
      log.debug(this + " got exception: " + socketWrapper, ex);

      try
      {
         semaphore.release();
         if (trace) log.trace(this + " released semaphore: " + semaphore.permits());
         socketWrapper.close();
      }
      catch (Exception ignored)
      {
      }
      
      if (oneway)
         return null;
      else
         return handleException(ex, socketWrapper);
   }
   
   protected void initPool()
   {
      synchronized (connectionPools)
      {
         pool = (LinkedList)connectionPools.get(address);
         semaphore = (Semaphore)semaphores.get(address);
         if (pool == null)
         {
            pool = new LinkedList();
            connectionPools.put(address, pool);
            log.debug("Creating semaphore with size " + maxPoolSize);
            semaphore = new Semaphore(maxPoolSize);
            semaphores.put(address, semaphore);
            
            if (trace)
            {
               synchronized (pool)
               {
                  log.trace(this + " added new pool (" + pool + ") as " + address);
               }
            }
         }
         else
         {
            if (trace)
            {
               synchronized (pool)
               {
                  log.trace(this + " using pool (" + pool + ") already defined for " + address);
               }
            }
         }
      }
   }
   
   protected SocketWrapper getConnection(Marshaller marshaller,
                                         UnMarshaller unmarshaller,
                                         boolean tryPool, int timeAllowed)
      throws Exception
   {
      long start = System.currentTimeMillis();
      long timeToWait = (timeAllowed > 0) ? timeAllowed : connectionWait;
      boolean timedout = !semaphore.attempt(timeToWait);
      if (trace) log.trace(this + " obtained semaphore: " + semaphore.permits());
      
      if (timedout)
      {
         throw new IllegalStateException("Timeout waiting for a free socket");
      }
      
      SocketWrapper pooled = null;

      if (tryPool)
      {
         synchronized (pool)
         {
            // if connection within pool, use it
            if (pool.size() > 0)
            {
               pooled = getPooledConnection();
               if (trace) log.trace(this + " reusing pooled connection: " + pooled);
            }
         }
      }
      else
      {
         if (trace) log.trace(this + " avoiding connection pool, creating new socket");
      }

      if (pooled == null)
      {
         //Need to create a new one  
         Socket socket = null;

         if (trace) { log.trace(this + " creating socket "); }
 
         // timeAllowed < 0 indicates no per invocation timeout has been set.
         int timeRemaining = -1;
         if (0 <= timeAllowed)
         {
            timeRemaining = (int) (timeAllowed - (System.currentTimeMillis() - start));
         }
         
         socket = createSocket(address.address, address.port, timeRemaining);
         if (trace) log.trace(this + " created socket: " + socket);

         socket.setTcpNoDelay(address.enableTcpNoDelay);

         Map metadata = getLocator().getParameters();
         if (metadata == null)
         {
            metadata = new HashMap(2);
         }
         else
         {
            metadata = new HashMap(metadata);
         }
         metadata.put(SocketWrapper.MARSHALLER, marshaller);
         metadata.put(SocketWrapper.UNMARSHALLER, unmarshaller);
         if (writeTimeout > 0)
         {
            metadata.put(SocketWrapper.WRITE_TIMEOUT, new Integer(writeTimeout));
         }
         if (timeAllowed > 0)
         {
            timeRemaining = (int) (timeAllowed - (System.currentTimeMillis() - start));
            
            if (timeRemaining <= 0)
               throw new IllegalStateException("Timeout creating a new socket");
            
            metadata.put(SocketWrapper.TEMP_TIMEOUT, new Integer(timeRemaining));
         }
         
         pooled = createClientSocket(socket, address.timeout, metadata);
      }

      return pooled;
   }

   protected SocketWrapper createClientSocket(Socket socket, int timeout, Map metadata)
      throws Exception
   {
      if (clientSocketConstructor == null)
      {
         if(clientSocketClass == null)
         {
            clientSocketClass = ClassLoaderUtility.loadClass(clientSocketClassName, getClass());
         }

         Class[] args = new Class[]{Socket.class, Map.class, Integer.class};
         clientSocketConstructor = clientSocketClass.getConstructor(args);
      }

      SocketWrapper clientSocketWrapper = null;
      clientSocketWrapper = (SocketWrapper)clientSocketConstructor.
         newInstance(new Object[]{socket, metadata, new Integer(timeout)});

      return clientSocketWrapper;
   }

   protected Socket createSocket(String address, int port, int timeout) throws IOException
   {
      Socket s = new Socket();
      configureSocket(s);
      InetSocketAddress inetAddr = new InetSocketAddress(address, port);
      connect(s, inetAddr);
      return s;
   }
   
   protected void configureSocket(Socket s) throws SocketException
   {
      s.setReuseAddress(getReuseAddress());
      
      if (keepAliveSet)           s.setKeepAlive(keepAlive);
      if (oOBInlineSet)           s.setOOBInline(oOBInline);
      if (receiveBufferSize > -1) s.setReceiveBufferSize(receiveBufferSize);
      if (sendBufferSize > -1)    s.setSendBufferSize(sendBufferSize);
      if (soLingerSet && 
            soLingerDuration > 0) s.setSoLinger(soLinger, soLingerDuration);
      if (trafficClass > -1)      s.setTrafficClass(trafficClass);
   }

   protected SocketWrapper getPooledConnection()
   {
      SocketWrapper socketWrapper = null;
      while (pool.size() > 0)
      {
         socketWrapper = (SocketWrapper)pool.removeFirst();
         try
         {
            if (socketWrapper != null)
            {
               if (socketWrapper instanceof OpenConnectionChecker)
               {
                  ((OpenConnectionChecker) socketWrapper).checkOpenConnection();
               }
               if (shouldCheckConnection)
               {
                  socketWrapper.checkConnection();
                  return socketWrapper;
               }
               else
               {
                  return socketWrapper;
               }
            }
         }
         catch (Exception ex)
         {
            if (trace) { log.trace(this + " couldn't reuse connection from pool"); }
            try
            {
               socketWrapper.close();
            }
            catch (Exception e)
            {
               log.debug("Failed to close socket wrapper", e);
            }
         }
      }
      return null;
   }

   // Private --------------------------------------------------------------------------------------

   private Object versionedRead(InputStream inputStream, UnMarshaller unmarshaller, int version)
      throws IOException, ClassNotFoundException
   {
      //TODO: -TME - is switch required?
      switch (version)
      {
         case Version.VERSION_1:
         case Version.VERSION_2:
         case Version.VERSION_2_2:
         {
            if (trace) { log.trace(this + " reading response from unmarshaller"); }
            if (unmarshaller instanceof VersionedUnMarshaller)
               return((VersionedUnMarshaller)unmarshaller).read(inputStream, null, version);
            else
               return unmarshaller.read(inputStream, null);
         }
         default:
         {
            throw new IOException("Can not read data for version " + version + ". " +
               "Supported versions: " + Version.VERSION_1 + ", " + Version.VERSION_2 + ", " + Version.VERSION_2_2);
         }
      }
   }

   private void versionedWrite(OutputStream outputStream, Marshaller marshaller,
                               Object invocation, int version) throws IOException
   {
      //TODO: -TME Should I worry about checking the version here?  Only one way to do it at this point
      switch (version)
      {
         case Version.VERSION_1:
         case Version.VERSION_2:
         case Version.VERSION_2_2:
         {
            if (trace) { log.trace(this + " writing invocation to marshaller"); }
            if (marshaller instanceof VersionedMarshaller)
               ((VersionedMarshaller) marshaller).write(invocation, outputStream, version);
            else
               marshaller.write(invocation, outputStream);
            if (trace) { log.trace(this + " done writing invocation to marshaller"); }

            return;
         }
         default:
         {
            throw new IOException("Can not write data for version " + version + ".  " +
               "Supported versions: " + Version.VERSION_1 + ", " + Version.VERSION_2 + ", " + Version.VERSION_2_2);
         }
      }
   }

   //TODO: -TME Exact same method in ServerThread
   private int readVersion(InputStream inputStream) throws IOException
   {
      if (trace) { log.trace(this + " reading version from input stream"); }
      int version = inputStream.read();
      if (trace) { log.trace(this + " read version " + version + " from input stream"); }
      return version;
   }

   //TODO: -TME Exact same method in ServerThread
   private void writeVersion(OutputStream outputStream, int version) throws IOException
   {
      if (trace) { log.trace(this + " writing version " + version + " on output stream"); }
      outputStream.write(version);
   }
   
   static private void mapJavaBeanProperties(final Object o, final Properties props, final boolean isStrict)
   throws IntrospectionException
   {
      if (SecurityUtility.skipAccessControl())
      {
         PropertyEditors.mapJavaBeanProperties(o, props, isStrict);
         return;
      }

      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IntrospectionException
            {
               PropertyEditors.mapJavaBeanProperties(o, props, isStrict);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IntrospectionException) e.getCause();
      }
   }
   
   static private void connect(final Socket socket, final InetSocketAddress address)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         socket.connect(address);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               socket.connect(address);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
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
   // Inner classes --------------------------------------------------------------------------------

}
