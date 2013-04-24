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

package org.jboss.remoting.transport.rmi;

import org.jboss.logging.Logger;
import org.jboss.remoting.AbstractInvoker;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.ConnectionFailedException;
import org.jboss.remoting.Home;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.RemoteClientInvoker;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.MarshallerDecorator;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.UnMarshallerDecorator;
import org.jboss.remoting.marshal.VersionedMarshaller;
import org.jboss.remoting.marshal.VersionedUnMarshaller;
import org.jboss.remoting.marshal.rmi.RMIMarshaller;
import org.jboss.remoting.marshal.rmi.RMIUnMarshaller;
import org.jboss.remoting.serialization.SerializationManager;
import org.jboss.remoting.serialization.SerializationStreamFactory;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.serial.io.JBossObjectInputStream;
import org.jboss.util.threadpool.BasicThreadPool;
import org.jboss.util.threadpool.BlockingMode;
import org.jboss.util.threadpool.RunnableTaskWrapper;
import org.jboss.util.threadpool.Task;
import org.jboss.util.threadpool.ThreadPool;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketTimeoutException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * RMIClientInvoker
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:telrod@vocalocity.net">Tom Elrod</a>
 * @version $Revision: 5017 $
 */
public class RMIClientInvoker extends RemoteClientInvoker
{
   /**
    * Key for the configuration map that determines the threadpool size for 
    * simulated timeouts.
    */
   public static final String MAX_NUM_TIMEOUT_THREADS = "maxNumTimeoutThreads";

   /**
    * Key for the configuration map that determines the queue size for simulated
    * timeout threadpool.
    */
   public static final String MAX_TIMEOUT_QUEUE_SIZE = "maxTimeoutQueueSize";
   
   /**
    * Specifies the default number of work threads in the thread pool for 
    * simulating timeouts.
    */
   public static final int MAX_NUM_TIMEOUT_THREADS_DEFAULT = 10;
   
   private static final Logger log = Logger.getLogger(RMIClientInvoker.class);
   private static final boolean isTraceEnabled = log.isTraceEnabled();
   
   private RMIServerInvokerInf server;
   
   private Object timeoutThreadPoolLock = new Object();
   private ThreadPool timeoutThreadPool;
   
   protected boolean rmiOnewayMarshalling;

   /**
    * Need flag to indicate if have been able to lookup registry and set stub.
    * Can't do this in the constructor, as need to throw CannotConnectException so
    * for clustering capability.
    *
    * @param locator
    */
   private boolean connected = false;

   public RMIClientInvoker(InvokerLocator locator)
   {
      super(locator);
      configureParameters();
   }

   public RMIClientInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
      configureParameters();
   }
   
   protected void configureParameters()
   {
      Map params = configuration;

      if (params == null)
      {
         return;
      }

      // look for enableTcpNoDelay param
      Object val = params.get(RMIServerInvoker.RMI_ONEWAY_MARSHALLING);
      if (val instanceof String)
      {
         try
         {
            rmiOnewayMarshalling = Boolean.valueOf((String)val).booleanValue();
            log.debug(this + " setting rmiOnewayMarshalling to " + rmiOnewayMarshalling);
         }
         catch (Exception e)
         {
            log.warn(this + " could not convert " + RMIServerInvoker.RMI_ONEWAY_MARSHALLING +
                     " value of " + val + " to a boolean value.  Defaulting to false");
         }
      }
      else if (val != null)
      {
         log.warn(this + " value of " + RMIServerInvoker.RMI_ONEWAY_MARSHALLING +
                  " (" + val + ") must be a String.  Defaulting to false");
      }
   }


   private int getRegistryPort(InvokerLocator locator)
   {
      int port = RMIServerInvoker.DEFAULT_REGISTRY_PORT;

      // See if locator contains a specific registry port
      Map params = locator.getParameters();
      if(params != null)
      {
         String value = (String) params.get(RMIServerInvoker.REGISTRY_PORT_KEY);
         if(value != null)
         {
            try
            {
               port = Integer.parseInt(value);
               log.debug("Using port " + port + " for rmi registry.");
            }
            catch(NumberFormatException e)
            {
               log.error("Can not set the RMIServerInvoker RMI registry to port " + value + ".  This is not a valid port number.");
            }
         }
      }
      return port;
   }

   /**
    * get the server stub
    *
    * @param server
    */
   public void setServerStub(RMIServerInvokerInf server)
   {
      this.server = server;
      log.trace(this.server);
   }

   /**
    * return the RMI server stub
    *
    * @return
    */
   public RMIServerInvokerInf getServerStub()
   {
      return this.server;
   }

   /**
    * subclasses must implement this method to provide a hook to connect to the remote server, if this applies
    * to the specific transport. However, in some transport implementations, this may not make must difference since
    * the connection is not persistent among invocations, such as SOAP.  In these cases, the method should
    * silently return without any processing.
    *
    * @throws ConnectionFailedException
    */
   protected void handleConnect()
   throws ConnectionFailedException
   {  
      int registryPort = getRegistryPort(locator);
      Home home = null;
      Exception savedException = null;
      Iterator it = getConnectHomes().iterator();
      
      while (it.hasNext())
      {
         //TODO: -TME Need to figure this out a little better as am now dealing with
         // with 2 ports, the rmi server and the registry.
         try
         {
            home = (Home) it.next();
            String host = home.host;
            final int port = home.port;
            locator.setHomeInUse(home);
            storeLocalConfig(configuration);
            log.debug(this + " looking up registry: " + host + "," + port);
            final Registry registry = LocateRegistry.getRegistry(host, registryPort);
            log.debug(this + " trying to connect to: " + home);
            Remote remoteObj = lookup(registry, "remoting/RMIServerInvoker/" + port);
            log.debug("Remote RMI Stub: " + remoteObj);
            setServerStub((RMIServerInvokerInf) remoteObj);
            connected = true;
            break;
         }
         catch(Exception e)
         {
            savedException = e;
            connected = false;
            RemotingRMIClientSocketFactory.removeLocalConfiguration(locator);
            log.trace("Unable to connect RMI invoker client to " + home, e);

         }
      }

      if (server == null)
      {
         String message = this + " unable to connect RMI invoker client";
         log.debug(message);
         throw new CannotConnectException(message, savedException);
      }
   }
   
   protected Home getUsableAddress()
   {
      InvokerLocator savedLocator = locator;
      String protocol = savedLocator.getProtocol();
      String path = savedLocator.getPath();
      Map params = savedLocator.getParameters();
      List homes = locator.getConnectHomeList();
      Home home = null;
      
      Iterator it = homes.iterator();
      while (it.hasNext())
      {
         try
         {
            home = (Home) it.next();
            locator = new InvokerLocator(protocol, home.host, home.port, path, params);
            invoke(new InvocationRequest(null, null, ServerInvoker.ECHO, null, null, null));
            if (log.isTraceEnabled()) log.trace(this + " able to contact server at: " + home);
            return home;
         }
         catch (Throwable e)
         {
            log.debug(this + " unable to contact server at: " + home);
         }
         finally
         {
            locator = savedLocator;
         }
      }
   
      return home;
   }

   /**
    * subclasses must implement this method to provide a hook to disconnect from the remote server, if this applies
    * to the specific transport. However, in some transport implementations, this may not make must difference since
    * the connection is not persistent among invocations, such as SOAP.  In these cases, the method should
    * silently return without any processing.
    */
   protected void handleDisconnect()
   {
      RemotingRMIClientSocketFactory.removeLocalConfiguration(locator);
   }

   protected String getDefaultDataType()
   {
      return RMIMarshaller.DATATYPE;
   }

   protected void storeLocalConfig(Map config)
   {
      HashMap localConfig = new HashMap(config);

      // If a specific SocketFactory was passed in, use it.  If a SocketFactory was
      // generated from SSL parameters, discard it.  It will be recreated later by
      // SerializableSSLClientSocketFactory with any additional parameters sent
      // from server.
      if (socketFactory != null &&
            !socketFactoryCreatedFromSSLParameters &&
            AbstractInvoker.isCompleteSocketFactory(socketFactory))
         localConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, socketFactory);

      // Save configuration for SerializableSSLClientSocketFactory.
      RemotingRMIClientSocketFactory.addLocalConfiguration(locator, localConfig);
   }
   
   protected Object transport(String sessionId, Object invocation, Map metadata, Marshaller marshaller, UnMarshaller unmarshaller)
         throws IOException, ConnectionFailedException
   {
      if(this.server == null)
      {
         log.debug("Server stub has not been set in RMI invoker client.  See previous errors for details.");
         //throw new IOException("Server stub hasn't been set!");
         throw new CannotConnectException("Server stub has not been set.");
      }
      try
      {

         Object payload = invocation;
         if(marshaller != null && !(marshaller instanceof RMIMarshaller))
         {
            if(marshaller instanceof MarshallerDecorator)
            {
               payload = ((MarshallerDecorator) marshaller).addDecoration(payload);
            }
            else
            {
               ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
               if (marshaller instanceof VersionedMarshaller)
                  ((VersionedMarshaller) marshaller).write(payload, byteOut, getVersion());
               else
                  marshaller.write(payload, byteOut);
               
               byteOut.close();
               
               if (rmiOnewayMarshalling)
               {
                  // Legacy treatment, pre 2.4.0.
                  ByteArrayInputStream bais = new ByteArrayInputStream(byteOut.toByteArray());
                  SerializationManager manager = SerializationStreamFactory.getManagerInstance(getSerializationType());
                  ObjectInputStream ois = manager.createInput(bais, getClassLoader());

                  try
                  {
                     byteOut.close();
                     payload = readObject(ois);
                     ois.close();
                  }
                  catch(ClassNotFoundException e)
                  {
                     log.debug("Could not marshall invocation payload object " + payload, e);
                     throw new IOException(e.getMessage());
                  }
               }
               else
               {
                  payload = byteOut.toByteArray();
               }
            }
         }

         int simulatedTimeout = getSimulatedTimeout(configuration, metadata);
         if (simulatedTimeout <= 0)
         {
            Object result = callTransport(server, payload);
            return unmarshal(result, unmarshaller, metadata);
         }
         else
         {
            if (log.isTraceEnabled()) log.trace("using simulated timeout: " + simulatedTimeout);
            
            class Holder {public Object value;}
            final Holder resultHolder = new Holder();
            final Object finalPayload = payload;
            
            Runnable r = new Runnable()
            {
               public void run()
               {
                  try
                  {
                     resultHolder.value = callTransport(server, finalPayload);
                     if (log.isTraceEnabled()) log.trace("result: " + resultHolder.value);
                  }
                  catch (Exception e)
                  {
                     resultHolder.value = e;
                     if (log.isTraceEnabled()) log.trace("exception: " + e); 
                  }
               }
            };
            
            // BasicThreadPool timeout mechanism depends on the interrupted status of
            // the running thread.
            Thread.interrupted();
            
            ThreadPool pool = getTimeoutThreadPool();
            WaitingTaskWrapper wrapper = new WaitingTaskWrapper(r, simulatedTimeout);
            if (log.isTraceEnabled()) log.trace("starting task in thread pool");
            pool.runTaskWrapper(wrapper);
            if (log.isTraceEnabled()) log.trace("task finished in thread pool");
            
            Object result = unmarshal(resultHolder.value, unmarshaller, metadata);
            if (result == null)
            {
               if (log.isTraceEnabled()) log.trace("invocation timed out");
               Exception cause = new SocketTimeoutException("timed out");
               throw new CannotConnectException("Can not connect http client invoker.", cause);
            }
            else if (result instanceof IOException)
            {
               throw (IOException) result;
            }
            else if (result instanceof RuntimeException)
            {
               throw (RuntimeException) result;
            }
            else
            {
               if (log.isTraceEnabled()) log.trace("returning result: " + result);
               return result;
            }
         }
      }
      catch(RemoteException e)
      {
         log.debug("Error making invocation in RMI client invoker.", e);
         throw new CannotConnectException("Error making invocation in RMI client invoker.", e);
      }
   }
   
   private int getSimulatedTimeout(Map configuration, Map metadata)
   {
      int timeout = -1;
      String connectionTimeout = (String) configuration.get("timeout");
      String invocationTimeout = null;
      if (metadata != null) invocationTimeout = (String) metadata.get("timeout");
      
      if (invocationTimeout != null && invocationTimeout.length() > 0)
      {
         try
         {
            timeout = Integer.parseInt(invocationTimeout);
         }
         catch (NumberFormatException e)
         {
            log.warn("Could not set timeout for current invocation because value (" + invocationTimeout + ") is not a number.");
         }
      }
      
      if (timeout < 0 && connectionTimeout != null && connectionTimeout.length() > 0)
      {
         try
         {
            timeout = Integer.parseInt(connectionTimeout);
         }
         catch (NumberFormatException e)
         {
            log.warn("Could not set timeout for http client connection because value (" + connectionTimeout + ") is not a number.");
         }
      }
      
      if (timeout < 0)
         timeout = 0;

      return timeout;
   }
   
   
   protected Object unmarshal(Object o, UnMarshaller unmarshaller, Map metadata) throws IOException
   {
      Object result = o;
      if(unmarshaller != null && !(unmarshaller instanceof RMIUnMarshaller) && !rmiOnewayMarshalling)
      {
         if(unmarshaller instanceof UnMarshallerDecorator)
         {
            result = ((UnMarshallerDecorator) unmarshaller).removeDecoration(o);
         }
         else
         {  
            byte[] byteIn = (byte[]) o;
            ByteArrayInputStream is = new ByteArrayInputStream(byteIn);

            try
            {
               if (unmarshaller instanceof VersionedUnMarshaller)
               {
                  result = ((VersionedUnMarshaller) unmarshaller).read(is, metadata, getVersion());
               }
               else
               {
                  result = unmarshaller.read(is, metadata);
               }
            }
            catch(ClassNotFoundException e)
            {
               log.debug("Could not unmarshall invocation response" + o, e);
               throw new IOException(e.getMessage());
            }
         }
      }
      
      return result;
   }
   
   /**
    * Gets the thread pool being used for simulating timeouts. If one has
    * not been specifically set via configuration or call to set it, will always return
    * instance of org.jboss.util.threadpool.BasicThreadPool.
    */
   public ThreadPool getTimeoutThreadPool()
   {
      synchronized (timeoutThreadPoolLock)
      {
         if (timeoutThreadPool == null)
         {
            int maxNumberThreads = MAX_NUM_TIMEOUT_THREADS_DEFAULT;
            int maxTimeoutQueueSize = -1;
            
            BasicThreadPool pool = new BasicThreadPool("HTTP timeout");
            log.debug(this + " created new simulated timeout thread pool: " + pool);
            Object param = configuration.get(MAX_NUM_TIMEOUT_THREADS);
            if (param instanceof String)
            {
               try
               {
                  maxNumberThreads = Integer.parseInt((String) param);
               }
               catch (NumberFormatException  e)
               {
                  log.warn("maxNumberThreads parameter has invalid format: " + param);
               }
            }
            else if (param != null)
            {
               log.warn("maxNumberThreads parameter must be a string in integer format: " + param);
            }

            param = configuration.get(MAX_TIMEOUT_QUEUE_SIZE);
            if (param instanceof String)
            {
               try
               {
                  maxTimeoutQueueSize = Integer.parseInt((String) param);
               }
               catch (NumberFormatException  e)
               {
                  log.warn("maxTimeoutQueueSize parameter has invalid format: " + param);
               }
            }
            else if (param != null)
            {
               log.warn("maxTimeoutQueueSize parameter must be a string in integer format: " + param);
            }

            pool.setMaximumPoolSize(maxNumberThreads);
            if (maxTimeoutQueueSize > 0)
            {
               pool.setMaximumQueueSize(maxTimeoutQueueSize);
            }
            pool.setBlockingMode(BlockingMode.RUN);
            timeoutThreadPool = pool;
         }
      }
      
      return timeoutThreadPool;
   }
   
   
   /**
    * When a WaitingTaskWrapper is run in a BasicThreadPool, the calling thread
    * will block for the designated timeout period.
    */
   static class WaitingTaskWrapper extends RunnableTaskWrapper
   {
      long completeTimeout;
      
      public WaitingTaskWrapper(Runnable runnable, long completeTimeout)
      {
         super(runnable, 0, completeTimeout);
         this.completeTimeout = completeTimeout;
      }
      public int getTaskWaitType()
      {
         return Task.WAIT_FOR_COMPLETE;
      }
      public String toString()
      {
         return "WaitingTaskWrapper[" + completeTimeout + "]";
      }
   }
   
   static private Object readObject(final ObjectInputStream ois)
   throws IOException, ClassNotFoundException
   {
      if (SecurityUtility.skipAccessControl() || !(ois instanceof JBossObjectInputStream))
      {
         return ois.readObject();
      }

      try
      {
         return AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException, ClassNotFoundException
            {
               return ois.readObject();
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof IOException)
            throw (IOException) cause;
         else if (cause instanceof ClassNotFoundException)
            throw (ClassNotFoundException) cause;
         else
            throw (RuntimeException) cause;
      }
   }
   
   static private Object callTransport(final RMIServerInvokerInf server, final Object payload)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return server.transport(payload);
      }

      try
      {
         return AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               return server.transport(payload);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      } 
   }
   
   static private Remote lookup(final Registry registry, final String name)
   throws RemoteException, NotBoundException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return registry.lookup(name);
      }
      
      try
      {
         return (Remote) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return registry.lookup(name);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof RemoteException)
            throw (RemoteException) cause;
         else
            throw (NotBoundException) cause;
      }
   }
}
