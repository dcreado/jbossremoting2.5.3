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


package org.jboss.remoting.transport.socket;

import org.jboss.logging.Logger;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvocationResponse;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.Version;
import org.jboss.remoting.Client;
import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.remoting.invocation.OnewayInvocation;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.VersionedMarshaller;
import org.jboss.remoting.marshal.VersionedUnMarshaller;
import org.jboss.serial.io.JBossObjectInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * This Thread object hold a single Socket connection to a client
 * and is kept alive until a timeout happens, or it is aged out of the
 * SocketServerInvoker's LRU cache.
 * <p/>
 * There is also a separate thread pool that is used if the client disconnects.
 * This thread/object is re-used in that scenario and that scenario only.
 * <p/>
 * This is a customization of the same ServerThread class used witht the PookedInvoker.
 * The custimization was made to allow for remoting marshaller/unmarshaller.
 *
 * @author <a href="mailto:bill@jboss.org">Bill Burke</a>
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 * @author <a href="mailto:ovidiu@jboss.org">Ovidiu Feodorov</a>
 *
 * @version $Revision: 5712 $
 */
public class ServerThread extends Thread
{
   // Constants ------------------------------------------------------------------------------------

   /** Used to configure period during which ServerThread is not evictable on first
    *  invocation even when in evictable state. */
   public static final String EVICTABILITY_TIMEOUT = "evictabilityTimeout";
   public static final int EVICTABILITY_TIMEOUT_DEFAULT = 10000;
   
   /** Key used to determine if thread should return to threadpool after
    * SocketTimeoutException */
   public static final String CONTINUE_AFTER_TIMEOUT = "continueAfterTimeout";
   
   final static private Logger log = Logger.getLogger(ServerThread.class);

   // Static ---------------------------------------------------------------------------------------

   private static boolean trace = log.isTraceEnabled();

   private static int idGenerator = 0;

   public static synchronized int nextID()
   {
      return idGenerator++;
   }
   
   private static ClassLoader classLoader = getClassLoader(ServerThread.class);

   // Attributes -----------------------------------------------------------------------------------

   protected volatile boolean running;
   protected volatile boolean shutdown;
   
   protected boolean evictable;
   protected long enteredEvictable;
   protected Object evictionLock = new Object();

   protected LRUPool clientpool;
   protected LinkedList threadpool;

   protected String serverSocketClassName;
   protected Class serverSocketClass;

   private Socket socket;
   private int timeout;
   private int writeTimeout;
   protected SocketServerInvoker invoker;
   private Constructor serverSocketConstructor;
   protected SocketWrapper socketWrapper;

   protected Marshaller marshaller;
   protected UnMarshaller unmarshaller;
   
   protected int version;
   protected boolean performVersioning;

   // the unique identity of the thread, which won't change during the life of the thread. The
   // thread may get associated with different IP addresses though.
   private int id = Integer.MIN_VALUE;

   // Indicates if will check the socket connection when getting from pool by sending byte over the
   // connection to validate is still good.
    private boolean shouldCheckConnection;

   // Will indicate when the last request has been processed (used in determining idle
   // connection/thread timeout)
   private long lastRequestHandledTimestamp = System.currentTimeMillis();
   
   // Used to prevent eviction before first invocation has been completed.
   private int invocationCount;
   
   // Period during which ServerThread is not evictable on first
   // invocation even when in evictable state. */
   private int evictabilityTimeout = EVICTABILITY_TIMEOUT_DEFAULT;
   
   private boolean reuseAfterTimeout;

   private boolean useOnewayConnectionTimeout = true;

   // Constructors ---------------------------------------------------------------------------------

   public ServerThread(Socket socket, SocketServerInvoker invoker, LRUPool clientpool,
                       LinkedList threadpool, int timeout, int writeTimeout, String serverSocketClassName)
      throws Exception
   {
      super();

      running = true;
      setName(getWorkerThreadName(socket));

      this.socket = socket;
      this.timeout = timeout;
      this.writeTimeout = writeTimeout;
      this.serverSocketClassName = serverSocketClassName;
      this.invoker = invoker;
      this.clientpool = clientpool;
      this.threadpool = threadpool;

      if (invoker != null)
      {
         version = invoker.getVersion();
         performVersioning = Version.performVersioning(version);
         
         Map configMap = invoker.getConfiguration();
         String checkValue = (String)configMap.get(SocketServerInvoker.CHECK_CONNECTION_KEY);
         if (checkValue != null && checkValue.length() > 0)
         {
            shouldCheckConnection = Boolean.valueOf(checkValue).booleanValue();
         }
         else if (invoker.getVersion() == Version.VERSION_1)
         {
            shouldCheckConnection = true;
         }
         
         Object o = configMap.get(EVICTABILITY_TIMEOUT);
         if (o != null)
         {
            try
            {
               evictabilityTimeout = Integer.valueOf((String)o).intValue();
               log.debug(this + " setting evictabilityTimeout to " + evictabilityTimeout);
            }
            catch (Exception e)
            {
               log.warn(this + " could not convert " + EVICTABILITY_TIMEOUT + 
                        " value of " + o + " to a int value");
            }
         }
         
         o = configMap.get(MicroSocketClientInvoker.USE_ONEWAY_CONNECTION_TIMEOUT);
         if (o != null)
         {
            try
            {
               useOnewayConnectionTimeout = Boolean.valueOf((String)o).booleanValue();
               log.debug(this + " setting useOnewayConnectionTimeout to " + useOnewayConnectionTimeout);
            }
            catch (Exception e)
            {
               log.warn(this + " could not convert " + MicroSocketClientInvoker.USE_ONEWAY_CONNECTION_TIMEOUT +
                        " value of " + o + " to a boolean value");
            }
         }
      }
   }

   // Thread overrides -----------------------------------------------------------------------------

   public void run()
   {
      try
      {
         processNewSocket();
         
         while (true)
         {
            dorun();

            // The following code has been changed to eliminate a race condition with
            // SocketServerInvoker.cleanup().
            //
            // A ServerThread can shutdown for two reasons:
            // 1. the client shuts down, and
            // 2. the server shuts down.
            //
            // If both occur around the same time, a problem arises.  If a ServerThread starts to
            // shut down because the client shut down, it will test shutdown, and if it gets to the
            // test before SocketServerInvoker.cleanup() calls ServerThread.shutdown() to set shutdown
            // to true, it will return itself to threadpool.  If it moves from clientpool to
            // threadpool at just the right time, SocketServerInvoker could miss it in both places
            // and never call shutdown(), leaving it alive, resulting in a memory leak.  The solution is
            // to synchronize parts of ServerThread.run() and SocketServerInvoker.cleanup() so that
            // they interact atomically.

            synchronized (clientpool)
            {
               if(trace) { log.trace(this + " removing itself from clientpool"); }
               clientpool.remove(this);
               
               if (shutdown)
               {
                  if (trace) log.trace(this + " exiting");
                  invoker = null;
                  return; // exit thread
               }
               else
               {
                  if(trace) { log.trace(this + " returning itself to threadpool"); }
                  threadpool.add(this);
                  clientpool.notifyAll();
                  Thread.interrupted(); // clear any interruption so that we can be pooled.
               }
            }

            synchronized (this)
            {
               // If running == true, then SocketServerInvoker has already removed this
               // ServerThread from threadpool and called wakeup(), in which case run()
               // should continue immediately.
               if (running)
                  continue;
               
               while (true)
               {
                  try
                  {
                     if(trace) { log.trace(this + " begins to wait"); }

                     wait();

                     if(trace) { log.trace(this + " woke up after wait"); }

                     if (shutdown)
                     {
                        invoker = null;
                        if (trace) log.trace(this + " exiting");
                        return; // exit thread
                     }

                     break;
                  }
                  catch (InterruptedException e)
                  {
                  }
               }
            }
         }
      }
      catch (Exception e)
      {
         log.debug(this + " exiting run on exception, definitively thrown out of the threadpool", e);
      }
   }

   // Public ---------------------------------------------------------------------------------------

   public synchronized void wakeup(Socket socket, int timeout, SocketServerInvoker invoker)
      throws Exception
   {
      // rename the worker thread to reflect the new socket it is handling
      if (trace) log.trace(this + " restarting with " + socket);
      setName(getWorkerThreadName(socket));

      this.socket = socket;
      this.timeout = timeout;
      this.invoker = invoker;

      invocationCount = 0;
      running = true;
      
      synchronized (evictionLock)
      {
         evictable = false;
      }

      notify();
      if(trace) { log.trace(this + " has notified on mutex"); }
   }

   public long getLastRequestTimestamp()
   {
      return lastRequestHandledTimestamp;
   }

   public synchronized void shutdown()
   {
      if (trace) log.trace("attempting to shut down " + this);
      shutdown = true;

      try
      {
         if (socketWrapper != null)
         {
            String desc = socketWrapper.toString();
            socketWrapper.close();
            if (trace) log.trace(this + " closing socketWrapper: " + desc);
         }
      }
      catch (Exception ex)
      {
         log.debug("failed to close socket wrapper", ex);
      }

      if (trace) log.trace(this + " shutting down");
      notifyAll();
   }
   
   public void shutdownImmediately()
   {
      if (trace) log.trace("attempting to shut down immediately " + this);
      shutdown = true;

      try
      {
         if (socketWrapper != null)
         {
            String desc = socketWrapper.toString();
            socketWrapper.close();
            if (trace) log.trace(this + " closing socketWrapper: " + desc);
         }
      }
      catch (Exception ex)
      {
         log.debug("failed to close socket wrapper", ex);
      }

      if (trace) log.trace(this + " shutting down");
   }

   /**
    * Sets if server thread should check connection before continue to process on next invocation
    * request.  If is set to true, will send an ACK to client to verify client is still connected
    * on same socket.
    */
   public void shouldCheckConnection(boolean checkConnection)
   {
      this.shouldCheckConnection = checkConnection;
   }

   /**
    * Indicates if server will check with client (via an ACK) to see if is still there.
    */
   public boolean getCheckingConnection()
   {
      return this.shouldCheckConnection;
   }

   /**
    * If this ServerThread is in acknowledge() or readVersion(), evict() will close the
    * socket so that thread returns itself to threadpool.
    * 
    * @return true  if eviction is possible
    * @return false if eviction is not possible
    */
   public boolean evict()
   {  
      if (trace) log.trace(this + " eviction attempted");
      synchronized (evictionLock)
      {
         if (!evictable || 
               (invocationCount == 0 && 
                System.currentTimeMillis() - enteredEvictable < evictabilityTimeout))
         {
            if (trace) log.trace(this + " is not evictable: invocationCount = " + invocationCount);
            return false;
         }
         else if (!running)
         {
            if (trace) log.trace(this + " is not running - may have been evicted already");
            return false;
         }

         running = false;

         try
         {
            if (socketWrapper != null)
            {
               String desc = socketWrapper.toString();
               socketWrapper.close();
               if (trace) log.trace(this + " evict() closed socketWrapper: " + desc);
            }
         }
         catch (Exception ex)
         {
            log.debug("failed to close socket wrapper", ex);
         }
      }

//      notifyAll();
      return true;
   }

      // This is a race and there is a chance that a invocation is going on at the time of the
      // interrupt.  But I see no way right now to protect for this.

      // NOTE ALSO!: Shutdown should never be synchronized. We don't want to hold up accept()
      // thread! (via LRUpool)

//      try
//      {
//         if (socketWrapper != null)
//         {
//            log.debug(this + " closing socketWrapper: " + socketWrapper);
//            socketWrapper.close();
//         }
//      }
//      catch (Exception ex)
//      {
//         log.debug("failed to close socket wrapper", ex);
//      }
//      socketWrapper = null;
//
//      if (trace) log.trace(this + " shutting down");
//      notifyAll();
//   }

   /**
    * This method is intended to be used when need to unblock I/O read, which the thread will
    * automatically loop back to do after processing a request.
    */
//   public synchronized void unblock()
//   {
//      try
//      {
//         socketWrapper.close();
//      }
//      catch (IOException e)
//      {
//         log.warn("Error closing socket when attempting to unblock I/O", e);
//      }
//   }

   public String toString()
   {
      return getName();
   }

   // Package protected ----------------------------------------------------------------------------

   // Protected ------------------------------------------------------------------------------------

   /**
    * This is needed because Object*Streams leak
    */
   protected void dorun()
   {
      running = true;
      InputStream inputStream = null;
      OutputStream outputStream = null;

      // lazy initialize the socketWrapper on the worker thread itself. We do this to avoid to have
      // it done on the acceptor thread (prone to lockup)
      try
      {
         if(trace) { log.trace(this + " creating the socket wrapper"); }

         socketWrapper =
            createServerSocketWrapper(socket, timeout, invoker.getLocator().getParameters());
         inputStream = socketWrapper.getInputStream();
         outputStream = socketWrapper.getOutputStream();
         
         boolean valueSet = false;
         Map configMap = invoker.getConfiguration();
         Object o = configMap.get(CONTINUE_AFTER_TIMEOUT);
         if (o != null)
         {
            try
            {
               reuseAfterTimeout = Boolean.valueOf((String)o).booleanValue();
               valueSet = true;
               log.debug(this + " setting reuseAfterTimeout to " + reuseAfterTimeout);
            }
            catch (Exception e)
            {
               log.warn(this + " could not convert " + CONTINUE_AFTER_TIMEOUT + 
                        " value of " + o + " to a boolean value");
            }
         }
         
         if (!valueSet)
         {
            if (socketWrapper.getInputStream() instanceof JBossObjectInputStream)
            {
               reuseAfterTimeout = true;
            }
         }
         
         // Always do first one without an ACK because its not needed
         if(trace) { log.trace("processing first invocation without acknowledging"); }
         processInvocation(socketWrapper, inputStream, outputStream);
      }
      catch (Exception ex)
      {
         if (running)
         {
            log.error(this + " exception occurred during first invocation", ex);
            running = false;
         }
         else
         {
            log.debug(this + " exception occurred during first invocation", ex);
         }
      }
      catch (Error e)
      {
         if (!shutdown)
         {
            log.error("error", e);
         }
         else
         {
            log.debug("error", e);
         }
      }

      // Re-use loop
      while (running)
      { 
         try
         {
            if (clientpool.getEvictionNeeded())
            {
               if (trace) log.trace(this + " found eviction needed");
               throw new EvictionException();
            }
            
            acknowledge(socketWrapper);
            processInvocation(socketWrapper, inputStream, outputStream);
         }
         catch (EvictionException e)
         {
            if (trace) log.trace(this + " has been evicted");
            running = false;
         }
         catch (AcknowledgeFailure e)
         {
            if (!shutdown && trace)
            {
               log.trace("keep alive acknowledge failed!");
            }
            running = false;
         }
         catch(SocketTimeoutException ste)
         {
            if(!shutdown)
            {
               if(trace)
               {
                  log.trace(this + " timed out", ste);
               }
            }
            
            if (!reuseAfterTimeout)
               running = false;
         }
         catch (InterruptedIOException e)
         {
            if (!shutdown)
            {
               log.error(this + " Socket IO interrupted", e);
            }
            running = false;

         }
         catch (InterruptedException e)
         {
            if(trace)
            {
               log.trace(e);
            }
            if (!shutdown)
            {
               log.error(this +  " interrupted", e);
            }
         }
         catch (EOFException eof)
         {
            if (!shutdown && true)
            {
               if (trace) log.trace(this + " EOFException received. This is likely due to client finishing communication.", eof);
            }
            running = false;
         }
         catch (SocketException sex)
         {
            if (!shutdown && trace)
            {
               if (trace) log.trace(this + " SocketException received. This is likely due to client disconnecting and resetting connection.", sex);
            }
            running = false;
         }
         catch (Exception ex)
         {
            if (!shutdown)
            {
               log.error(this + " failed", ex);
            }
            running = false;
         }
         catch (Error e)
         {
            if (!shutdown)
            {
               log.error("error", e);
            }
            else
            {
               log.debug("error", e);
            }
         }
         
         // clear any interruption so that thread can be pooled.
         Thread.interrupted();
      }

      // Ok, we've been shutdown.  Do appropriate cleanups.
      // The stream close code has been moved to SocketWrapper.close().
//      try
//      {
//         if (socketWrapper != null)
//         {
//            InputStream in = socketWrapper.getInputStream();
//            if (in != null)
//            {
//               in.close();
//            }
//            OutputStream out = socketWrapper.getOutputStream();
//            if (out != null)
//            {
//               out.close();
//            }
//         }
//      }
//      catch (Exception ex)
//      {
//    log.debug("failed to close in/out", ex);
//    }

      try
      {
         if (socketWrapper != null)
         {
            String desc = socketWrapper.toString();
            socketWrapper.close();
            log.debug(this + " closed socketWrapper: " + desc);
         }
      }
      catch (Exception ex)
      {
         log.error(this + " failed to close socket wrapper", ex);
      }
   }

   protected void processInvocation(SocketWrapper socketWrapper, InputStream inputStream, OutputStream outputStream) throws Exception
   {
      if(trace) { log.trace("preparing to process next invocation"); }

      // Ok, now read invocation and invoke

      if (performVersioning)
      {
         version = readVersion(inputStream);

         //TODO: -TME Should I be checking for -1?

         // This is a best attempt to determine if is old version.  Typically, the first byte will
         // be -1, so if is, will reset stream and process as though is older version.

         // Originally this code (now uncommented) and the other commented code was to try to make
         // so could automatically detect older version that would not be sending a byte for the
         // version.  However, due to the way the serialization stream manager handles the stream,
         // resetting it does not work, so will probably have to throw away that idea. However, for
         // now, am uncommenting this section because if are using the flag to turn off connection
         // checking (ack back to client), then will get a -1 when the client closes connection.
         // Then when stream passed onto the versionedRead, will get EOFException thrown and will
         // process normally (as though came from the acknowledge, as would have happened if
         // connection checking was turned on).  Am hoping this is not a mistake...

         if(version == -1)
         {
//            version = Version.VERSION_1;
            throw new EOFException();
         }
      }
      
      completeInvocation(socketWrapper, inputStream, outputStream, performVersioning, version);
   }
   
   protected synchronized void completeInvocation(
                                  SocketWrapper socketWrapper,
                                  InputStream inputStream,
                                  OutputStream outputStream,
                                  boolean performVersioning, int version)
   throws Exception
   {  
      Object obj = versionedRead(inputStream, invoker, classLoader, version);

      // setting timestamp since about to start processing
      lastRequestHandledTimestamp = System.currentTimeMillis();

      InvocationRequest req = null;
      boolean createdInvocationRequest = false;
      boolean isError = false;

      if(obj instanceof InvocationRequest)
      {
         req = (InvocationRequest)obj;
      }
      else
      {
         req = createInvocationRequest(obj, socketWrapper);
         createdInvocationRequest = true;
         performVersioning = false;
      }

      boolean isServerSideOnewayRequest = isServerSideOneway(req);
      InetAddress clientAddress = socketWrapper.getSocket().getInetAddress();
      Object resp = null;

      try
      {
         // Make absolutely sure thread interrupted is cleared.
         Thread.interrupted();

         if(trace) { log.trace("about to call " + invoker + ".invoke()"); }

         if (req.getRequestPayload() == null)
            req.setRequestPayload(new HashMap());

         req.getRequestPayload().put(Remoting.CLIENT_ADDRESS, clientAddress);

         // call transport on the subclass, get the result to handback
         resp = invoker.invoke(req);

         if(trace) { log.trace(invoker + ".invoke() returned " + resp); }
      }
      catch (Throwable ex)
      {
         resp = ex;
         isError = true;
         if (trace) log.trace(invoker + ".invoke() call failed", ex);
      }

      Thread.interrupted(); // clear interrupted state so we don't fail on socket writes

      if(isServerSideOnewayRequest)
      {
         if(trace) { log.trace("oneway request, writing no reply on the wire"); }
      }
      else if (isOneway(req))
      {
         if (useOnewayConnectionTimeout && performVersioning)
         {
            writeVersion(outputStream, version);
            outputStream.flush();
         }
      }
      else
      {
         if(!createdInvocationRequest)
         {
            // need to return invocation response
            if(trace) { log.trace("creating response instance"); }
            resp = new InvocationResponse(req.getSessionId(), resp, isError, req.getReturnPayload());
         }

         if (performVersioning)
         {
            writeVersion(outputStream, version);
         }
    
         versionedWrite(outputStream, invoker, classLoader, resp, version);
      }

      // set the timestamp for last successful processed request
      lastRequestHandledTimestamp = System.currentTimeMillis();
      invocationCount++;
   }

   protected void acknowledge(SocketWrapper socketWrapper) throws Exception
   {
      if (shouldCheckConnection)
      {
         // HERE IS THE RACE between ACK received and handlingResponse = true. We can't synchronize
         // because readByte blocks and client is expecting a response and we don't want to hang
         // client. See shutdown and evict for more details. There may not be a problem because
         // interrupt only effects threads blocking on IO. and this thread will just continue.

         try
         {
            if(trace) { log.trace("checking connection"); }
            
            synchronized (evictionLock)
            {
               evictable = true;
               enteredEvictable = System.currentTimeMillis();
            }
            
            socketWrapper.checkConnection();
         }
         catch (EOFException e)
         {
            throw new AcknowledgeFailure();
         }
         catch (SocketException se)
         {
            throw new AcknowledgeFailure();
         }
         catch (IOException ioe)
         {
            throw new AcknowledgeFailure();
         }
         finally
         {
            synchronized (evictionLock)
            {
               evictable = false;
               
               if (!running) // In case evict() ran after SocketWrapper i/o.
                  throw new EvictionException();
            }
         }
      }
   }

   protected Object versionedRead(InputStream inputStream, ServerInvoker invoker,
                                  ClassLoader classLoader, int version)
      throws IOException, ClassNotFoundException
   {
      //TODO: -TME - Should I even botther to check for version here?  Only one way to do processing
      //             at this point, regardless of version.
      switch (version)
      {
         case Version.VERSION_1:
         case Version.VERSION_2:
         case Version.VERSION_2_2:
         {
            if(trace) { log.trace("blocking to read invocation from unmarshaller"); }

            Object o = null;
            if (unmarshaller instanceof VersionedUnMarshaller)
               o = ((VersionedUnMarshaller)unmarshaller).read(inputStream, null, version);
            else
               o = unmarshaller.read(inputStream, null);

            if(trace) { log.trace("read " + o + " from unmarshaller"); }

            return o;
         }
         default:
         {
            throw new IOException("Can not read data for version " + version +
               ".  Supported versions: " + Version.VERSION_1 + "," + Version.VERSION_2 + "," + Version.VERSION_2_2);
         }
      }
   }

   // Private --------------------------------------------------------------------------------------

   private SocketWrapper createServerSocketWrapper(Socket socket, int timeout, Map metadata)
      throws Exception
   {
      if (serverSocketConstructor == null)
      {
         if(serverSocketClass == null)
         {
            serverSocketClass = ClassLoaderUtility.loadClass(serverSocketClassName, getClass());
         }

         try
         {
            serverSocketConstructor = serverSocketClass.
               getConstructor(new Class[]{Socket.class, Map.class, Integer.class});
         }
         catch (NoSuchMethodException e)
         {
            serverSocketConstructor = serverSocketClass.getConstructor(new Class[]{Socket.class});
         }

      }

      SocketWrapper serverSocketWrapper = null;

      if (serverSocketConstructor.getParameterTypes().length == 3)
      {
         Map localMetadata = null;
         if (metadata == null)
         {
            localMetadata = new HashMap(2);
         }
         else
         {
            localMetadata = new HashMap(metadata);
         }
         localMetadata.put(SocketWrapper.MARSHALLER, marshaller);
         localMetadata.put(SocketWrapper.UNMARSHALLER, unmarshaller);
         if (writeTimeout > 0)
         {
            localMetadata.put(SocketWrapper.WRITE_TIMEOUT, new Integer(writeTimeout));
         }
         
         serverSocketWrapper = (SocketWrapper)serverSocketConstructor.
            newInstance(new Object[]{socket, localMetadata, new Integer(timeout)});
      }
      else
      {
         serverSocketWrapper =
            (SocketWrapper)serverSocketConstructor.newInstance(new Object[]{socket});

         serverSocketWrapper.setTimeout(timeout);
      }
      return serverSocketWrapper;
   }

   private boolean isServerSideOneway(InvocationRequest invocationRequest)
   {
         return invocationRequest.getParameter() instanceof OnewayInvocation;
   }
      
   private boolean isOneway(InvocationRequest invocationRequest)
   {
      boolean isOneway = false;
      Map metadata = invocationRequest.getRequestPayload();

      if (metadata != null)
      {
         Object val = metadata.get(Client.ONEWAY_FLAG);
         if (val != null && val instanceof String && Boolean.valueOf((String) val).booleanValue())
         {
            isOneway = true;
         }
      }
      return isOneway;
   }

   private InvocationRequest createInvocationRequest(Object obj, SocketWrapper socketWrapper)
   {
      if(obj instanceof InvocationRequest)
      {
         return (InvocationRequest)obj;
      }
      else
      {
         // need to wrap request with invocation request
         SocketAddress remoteAddress = socketWrapper.getSocket().getRemoteSocketAddress();

         return new InvocationRequest(remoteAddress.toString(),
                                      invoker.getSupportedSubsystems()[0],
                                      obj, new HashMap(), null, null);
      }
   }

   private void processNewSocket()
   {
      InvokerLocator locator = invoker.getLocator();
      String dataType = invoker.getDataType();
      String serializationType = invoker.getSerializationType();

      //TODO: -TME Need better way to get the unmarshaller (via config)

      Map configMap = null;
      if (invoker != null)
      {
         configMap = invoker.getConfiguration();
      }
      
      boolean passConfigMapToMarshalFactory = false;
      if (configMap != null)
      {
         Object o = configMap.get(Remoting.PASS_CONFIG_MAP_TO_MARSHAL_FACTORY);
         if (o instanceof String)
         {
            passConfigMapToMarshalFactory = Boolean.valueOf((String) o).booleanValue();
         }
         else if (o != null)
         {
            log.warn("Value of " + Remoting.PASS_CONFIG_MAP_TO_MARSHAL_FACTORY + " should be of type String: " + o);
         }
      }
      
      Map map = passConfigMapToMarshalFactory ? configMap : null;
      if (unmarshaller == null)
      {
         unmarshaller = MarshalFactory.getUnMarshaller(locator, classLoader, map);
      }
      if (unmarshaller == null)
      {
         unmarshaller = MarshalFactory.getUnMarshaller(dataType, serializationType);
      }

      if (marshaller == null)
      {
         marshaller = MarshalFactory.getMarshaller(locator, classLoader, map);
      }
      if (marshaller == null)
      {
         marshaller = MarshalFactory.getMarshaller(dataType, serializationType);
      }


   }

   private void versionedWrite(OutputStream outputStream, SocketServerInvoker invoker,
                               ClassLoader classLoader, Object resp, int version) throws IOException
   {
      //TODO: -TME - Should I ever worry about checking version here?  Only one way to send data at this point.
      switch (version)
      {
         case Version.VERSION_1:
         case Version.VERSION_2:
         case Version.VERSION_2_2:
         {
            if (marshaller instanceof VersionedMarshaller)
               ((VersionedMarshaller) marshaller).write(resp, outputStream, version);
            else
               marshaller.write(resp, outputStream);
            if (trace) { log.trace("wrote response to the output stream"); }
            return;
         }
         default:
         {
            throw new IOException("Can not write data for version " + version +
               ".  Supported version: " + Version.VERSION_1 + ", " + Version.VERSION_2 + ", " + Version.VERSION_2_2);
         }
      }
   }

   
   private int readVersion(InputStream inputStream) throws Exception
   {
      long start = -1;
      if(trace) 
      { 
         log.trace(this + " blocking to read version from input stream");
         start = System.currentTimeMillis();
      }

      synchronized (evictionLock)
      {
         evictable = true;
         enteredEvictable = System.currentTimeMillis();
      }

      try
      {
         int version = inputStream.read();
         if(trace) { log.trace(this + " read version " + version + " from input stream"); }
         return version;
      }
      finally
      {
         synchronized (evictionLock)
         {
            evictable = false;
            
            if (!running) // In case evict() ran after InputStream.read().
            {
               if (trace)
               {
                  long d = System.currentTimeMillis() - start;
                  log.trace(this + " socketWrapper: " + socketWrapper + ", waited: " + d);
               }
               throw new EvictionException();
            }
         }
      }
   }

   private void writeVersion(OutputStream outputStream, int version) throws IOException
   {
      outputStream.write(version);
   }

   private String getWorkerThreadName(Socket currentSocket)
   {
      if (id == Integer.MIN_VALUE)
      {
         id = nextID();
      }

      StringBuffer sb = new StringBuffer("WorkerThread#");
      sb.append(id).append('[');
      sb.append(currentSocket.getInetAddress().getHostAddress());
      sb.append(':');
      sb.append(currentSocket.getPort());
      sb.append(']');

      return sb.toString();
   }

   // Inner classes --------------------------------------------------------------------------------

   public static class AcknowledgeFailure extends Exception
   {
   }
   
   public static class EvictionException extends Exception
   {
   }
   
   static private ClassLoader getClassLoader(final Class c)
   {
      if (SecurityUtility.skipAccessControl())
      {
         return c.getClassLoader();
      }

      return (ClassLoader)AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return c.getClassLoader();
         }
      });
   }
}
