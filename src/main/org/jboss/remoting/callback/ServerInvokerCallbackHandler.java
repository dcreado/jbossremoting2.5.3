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
package org.jboss.remoting.callback;

import org.jboss.logging.Logger;
import org.jboss.remoting.Client;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.SerializableStore;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.invocation.InternalInvocation;
import org.jboss.remoting.security.SSLServerSocketFactoryServiceMBean;
import org.jboss.remoting.security.SSLSocketBuilder;
import org.jboss.remoting.security.SSLSocketBuilderMBean;
import org.jboss.remoting.security.SSLSocketFactoryService;
import org.jboss.remoting.util.SecurityUtility;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Responsible for all callbacks in remoting at invoker level (on the server side).
 * This is created within the ServerInvoker and passed to the server handler as a
 * proxy for the client's callback handler.
 * <p/>
 * Will determin internally if is using pull or push mechanism for delivering callbacks.
 * If is push, will create a Client to call back on the callback server.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public class ServerInvokerCallbackHandler
implements AsynchInvokerCallbackHandler, ConnectionListener
{

   private static final Logger log = Logger.getLogger(ServerInvokerCallbackHandler.class);

   private static boolean trace = log.isTraceEnabled();

   private InvocationRequest invocation;
   private Client callBackClient;
   private ArrayList callbacks = new ArrayList();
   private String sessionId;
   private String listenerId;
   private String clientSessionId;
   private InvokerLocator serverLocator;
   private int blockingTimeout = ServerInvoker.DEFAULT_BLOCKING_TIMEOUT;
   private boolean shouldPersist;
   

   private SerializableStore callbackStore = null;
   private CallbackErrorHandler callbackErrorHandler = null;
   private ServerInvoker serverInvoker;

   /**
    * The map key to use when looking up any callback store that
    * should be used.  This key should be used when setting up
    * config in the invoker.
    */
   public static final String CALLBACK_STORE_KEY = "callbackStore";

   /**
    * The map key to use when looking up any callback error handler that
    * should be used.  This key should be used when setting up
    * config in the invoker.
    */
   public static final String CALLBACK_ERROR_HANDLER_KEY = "callbackErrorHandler";

   /**
    * The map key to use when looking up the percentage of free memory
    * available before tiggering persistence.
    */
   public static final String CALLBACK_MEM_CEILING = "callbackMemCeiling";

   /**
    * The key used for storing a CallbackListener in the return metadata map of a Callback.
    */
   public static final String CALLBACK_LISTENER = "callbackListener";

   /*
    * The key used to indicate if callback acknowledgement should be handled by Remoting
    * or the application.  If it is present in the callback's return payload and
    * set to true, Remoting will handle the acknowledgement for push callbacks.
    */
   public static final String REMOTING_ACKNOWLEDGES_PUSH_CALLBACKS = "remotingAcknowledgesPushCallbacks";

   /**
    * This key used to identify a Callback to be acknowledged.
    */
   public static final String CALLBACK_ID = "callbackId";
   
   /** This key is used to identify the timeout used by a callback client */
   public static final String CALLBACK_TIMEOUT = "callbackTimeout";
   
   /** The key used to pass to the callback client a reference to ServerInvoker */
   public static final String SERVER_INVOKER = "serverInvoker";
   
   /** The key used to pass to the callback client a reference this ServerInvokerCallbackHandler */
   public static final String SERVER_INVOKER_CALLBACK_HANDLER = "serverInvokerCallbackHandler";

   /**
    * The percentage number of used memory before should persist messages.
    * For example, if 64MB available and only 30MB free mem and memPercentCeiling
    * is 50, then would trigger persisting of messages.
    */
   private double memPercentCeiling = 20; // 20% by default

   /**
    * Maps an ID to a CallbackListener for a Callback waiting to be acknowledged.
    */
   private Map idToListenerMap = Collections.synchronizedMap(new HashMap());


   public ServerInvokerCallbackHandler(InvocationRequest invocation, InvokerLocator serverLocator,
                                       ServerInvoker owner) throws Exception
   {
      if(invocation == null)
      {
         throw new Exception("Can not construct ServerInvokerCallbackHandler with null InvocationRequest.");
      }
      this.invocation = invocation;
      this.serverLocator = serverLocator;
      init(invocation, owner);
   }
   
   public void connect() throws Exception
   {
      if (callBackClient != null)
      {
         if (callBackClient.isConnected())
            return;
         callBackClient.connect();
      }
         
   }

   private void init(InvocationRequest invocation, ServerInvoker owner) throws Exception
   {
      serverInvoker = owner;
      clientSessionId = invocation.getSessionId();
      sessionId = invocation.getSessionId();
      
      Map metadata = null;
      if (owner.getConfiguration() == null)
      {
         metadata = new HashMap();
      }
      else
      {
         metadata = new HashMap(owner.getConfiguration());
      }
      if(invocation.getRequestPayload() != null)
      {
         metadata.putAll(invocation.getRequestPayload());
      }

      listenerId = (String) metadata.get(Client.LISTENER_ID_KEY);
      if(listenerId != null)
      {
         sessionId = sessionId + "+" + listenerId;
      }
      log.debug("Session id for callback handler is " + sessionId);

      if(invocation.getLocator() != null)
      {
         Object val = metadata.get(CALLBACK_TIMEOUT);
         if (val instanceof String)
         {
            try
            {
               Integer.parseInt((String) val);
               metadata.put(ServerInvoker.TIMEOUT, val);
               log.debug(this + " using callbackTimeout value " + val);
            }
            catch (NumberFormatException e)
            {
               log.warn("callbackTimeout value must have valid numeric format: " + val);
            }
         }
         else if (val != null)
         {
            log.warn("callbackTimeout value must be a String: " + val);
         }
         
         // need to configure callback client with ssl config if one exists for server
         configureSocketFactory(metadata, owner);
         
         metadata.put(SERVER_INVOKER, owner);
         metadata.put(SERVER_INVOKER_CALLBACK_HANDLER, this);

         callBackClient = new Client(invocation.getLocator(), invocation.getSubsystem(), metadata);
         callBackClient.setSessionId(sessionId);
         createCallbackErrorHandler(owner, invocation.getSubsystem());
      }
      else
      {
         createCallbackStore(owner, sessionId);
      }
      
      Object val = metadata.get(ServerInvoker.BLOCKING_TIMEOUT);
      if (val != null)
      {
         if (val instanceof String)
         {
            try
            {
               blockingTimeout = Integer.parseInt((String) val);
            }
            catch (NumberFormatException e)
            {
               log.warn("Error converting " + ServerInvoker.BLOCKING_TIMEOUT + " to type long.  " + e.getMessage());
            }
         }
         else
         {
            log.warn("Value for " + ServerInvoker.BLOCKING_TIMEOUT + " configuration must be of type " + String.class.getName() +
                     " and is " + val.getClass().getName());
         }
      }
   }

   /**
    * Will check to see if the server invoker associated with this callback client
    * needs to have associated ssl config
    * @param clientConfig
    * @param serverInvoker
    */
   private void configureSocketFactory(Map clientConfig, ServerInvoker serverInvoker) throws Exception
   {
      // If a SocketFactory already exists, then all we have to do is tell the
      // client invoker to use it.
      if (serverInvoker.getSocketFactory() != null)
      {
         clientConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, serverInvoker.getSocketFactory());
         return;
      }

      if (clientConfig == null)
         clientConfig = new HashMap();

      // If a constructed SocketFactory was passed in through config map, the client invoker
      // will use it.  Also, we store it in server invoker for future use.
      if (clientConfig.containsKey(Remoting.CUSTOM_SOCKET_FACTORY))
      {
         serverInvoker.setSocketFactory((SocketFactory) clientConfig.get(Remoting.CUSTOM_SOCKET_FACTORY));
         return;
      }

      // If a SocketFactory has not been created already, we need to make sure that the client
      // invoker creates a suitable SocketFactory.

      // First, see if we can convert server socket factory into socket factory.
      String serverSocketFactoryString = (String) clientConfig.get(ServerInvoker.SERVER_SOCKET_FACTORY);
      if (serverSocketFactoryString != null && serverSocketFactoryString.length() > 0)
      {
         final MBeanServer server = serverInvoker.getMBeanServer();
         try
         {
            final ObjectName serverSocketFactoryObjName = new ObjectName(serverSocketFactoryString);
            if (server != null)
            {
               String className = SSLServerSocketFactoryServiceMBean.class.getName();
               boolean isCorrectType = isInstanceOf(server, serverSocketFactoryObjName, className);
               if (isCorrectType)
               {
                  Object o = getMBeanAttribute(server, serverSocketFactoryObjName, "SSLSocketBuilder");             
                  SSLSocketBuilderMBean sslSocketBuilder = (SSLSocketBuilderMBean) o;
                  
                  if (sslSocketBuilder != null)
                  {
                     SSLSocketBuilder clonedSSLSocketBuilder = (SSLSocketBuilder) sslSocketBuilder.clone();
                     boolean shouldUseDefault = sslSocketBuilder.getUseSSLServerSocketFactory();
                     clonedSSLSocketBuilder.setUseSSLSocketFactory(shouldUseDefault);
                     boolean useClientMode = sslSocketBuilder.isServerSocketUseClientMode();
                     clonedSSLSocketBuilder.setSocketUseClientMode(useClientMode);
                     SSLSocketFactoryService sslSocketFactoryService = new SSLSocketFactoryService();
                     sslSocketFactoryService.setSSLSocketBuilder(clonedSSLSocketBuilder);
                     sslSocketFactoryService.start();
                     clientConfig.put(Remoting.CUSTOM_SOCKET_FACTORY, sslSocketFactoryService);
                     clientConfig.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "false");
                     // JBREM-536
                     clientConfig.put("hostnameVerifier", "org.jboss.test.remoting.transport.http.ssl.config.SelfIdentifyingHostnameVerifier");
                     serverInvoker.setSocketFactory(sslSocketFactoryService);
                     return;
                  }
               }
            }
         }
         catch (Exception ignored)
         {
            log.debug("error", ignored);
         }
      }

      // Otherwise, if we need an SSLSocketFactory, the client invoker will create it from
      // configuration parameters.  Tell it to use client mode (unless explictly configured
      // otherwise).
      if (serverInvoker.getServerSocketFactory() instanceof SSLServerSocketFactory)
      {
         if (!clientConfig.containsKey(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE))
            clientConfig.put(SSLSocketBuilder.REMOTING_SOCKET_USE_CLIENT_MODE, "false");
      }
   }

   public String getCallbackSessionId()
   {
      return sessionId;
   }

   public String getClientSessionId()
   {
      return clientSessionId;
   }

   public String getSubsystem()
   {
	   return invocation.getSubsystem();
   }

   public void setMemPercentCeiling(Double ceiling)
   {
      if(ceiling != null)
      {
         memPercentCeiling = ceiling.doubleValue();
      }
   }

   public Double getMemPercentCeiling()
   {
      return new Double(memPercentCeiling);
   }

   private void createCallbackStore(ServerInvoker owner, String sessionId) throws Exception
   {
      Map config = owner.getConfiguration();
      if(config != null)
      {
         // should either be a fully qualified class name or a mbean object name
         String storeName = (String) config.get(CALLBACK_STORE_KEY);
         if(storeName != null)
         {
            // will first try as a MBean
            try
            {
               MBeanServer server = owner.getMBeanServer();
               ObjectName storeObjectName = new ObjectName(storeName);
               if(server != null)
               {
                  callbackStore = (SerializableStore)
                        MBeanServerInvocationHandler.newProxyInstance(server,
                                                                      storeObjectName,
                                                                      SerializableStore.class,
                                                                      false);
                  callbackStore = new CallbackStoreWrapper(callbackStore);
               }
            }
            catch(Exception ex)
            {
               log.debug("Could not create callback store from the configration value given (" + storeName + ") as an MBean.");
               if(trace) { log.trace("Error is: " + ex.getMessage(), ex); }

               callbackStore = null;
            }

            // now try by class name
            if(callbackStore == null)
            {
               try
               {
                  Class storeClass = Class.forName(storeName);
                  callbackStore = (SerializableStore) storeClass.newInstance();
               }
               catch(Exception e)
               {
                  log.debug("Could not create callback store from the configuration value given (" + storeName + ") as a fully qualified class name.");
                  if(trace) { log.trace("Error is: " + e.getMessage(), e); }
               }
            }
         }
      }

      // if still null, then just use default
      if(callbackStore == null)
      {
         callbackStore = new NullCallbackStore();
      }
      else
      {
         // need to modify configuration to include session id for the callback client.
         Map storeConfig = new HashMap();
         storeConfig.putAll(owner.getConfiguration());

         String filePath = (String) storeConfig.get(CallbackStore.FILE_PATH_KEY);
         if(filePath == null)
         {
            filePath = getSystemProperty("jboss.server.data.dir", "data");
         }

         String separator = getSystemProperty("file.separator");
         String newFilePath = filePath + separator + "remoting" + separator + sessionId;
         storeConfig.put(CallbackStore.FILE_PATH_KEY, newFilePath);
         callbackStore.setConfig(storeConfig);
      }

      callbackStore.create();
      callbackStore.start();

      configureMemCeiling(owner.getConfiguration());
   }

   private void createCallbackErrorHandler(ServerInvoker owner, String subsystem) throws Exception
   {
      Map config = owner.getConfiguration();
      if(config != null)
      {
         // should either be a fully qualified class name or a mbean object name
         String errorHandlerName = (String) config.get(CALLBACK_ERROR_HANDLER_KEY);
         if(errorHandlerName != null)
         {
            // will first try as a MBean
            try
            {
               MBeanServer server = owner.getMBeanServer();
               ObjectName errorHandlerObjectName = new ObjectName(errorHandlerName);
               if(server != null)
               {
                  callbackErrorHandler = (CallbackErrorHandler)
                        MBeanServerInvocationHandler.newProxyInstance(server,
                                                                      errorHandlerObjectName,
                                                                      CallbackErrorHandler.class,
                                                                      false);
                  callbackErrorHandler = new CallbackErrorHandlerWrapper(callbackErrorHandler);
               }
            }
            catch(Exception ex)
            {
               log.debug("Could not create callback error handler from the configration value " +
                         "given (" + errorHandlerName + ") as an MBean.");
               if(trace) { log.trace("Error is: " + ex.getMessage(), ex); }
               callbackErrorHandler = null;
            }

            // now try by class name
            if(callbackErrorHandler == null)
            {
               try
               {
                  Class errorHandlerClass = Class.forName(errorHandlerName);
                  callbackErrorHandler = (CallbackErrorHandler) errorHandlerClass.newInstance();
               }
               catch(Exception e)
               {
                  log.debug("Could not create callback error handler from the configuration value " +
                            "given (" + errorHandlerName + ") as a fully qualified class name.");
                  if(trace) { log.trace("Error is: " + e.getMessage(), e); }
               }
            }
         }
      }

      // if still null, then just use default
      if(callbackErrorHandler == null)
      {
         callbackErrorHandler = new DefaultCallbackErrorHandler();
      }

      // set configuration for the error handler.
      Map errorHandlerConfig = new HashMap();
      errorHandlerConfig.putAll(owner.getConfiguration());
      errorHandlerConfig.put(CallbackErrorHandler.HANDLER_SUBSYSTEM, subsystem);
      callbackErrorHandler.setConfig(errorHandlerConfig);
      callbackErrorHandler.setServerInvoker(owner);
      callbackErrorHandler.setCallbackHandler(this);

   }

   private void configureMemCeiling(Map configuration)
   {
      if(configuration != null)
      {
         String ceiling = (String) configuration.get(CALLBACK_MEM_CEILING);
         if(ceiling != null)
         {
            try
            {
               double newCeiling = Double.parseDouble(ceiling);
               setMemPercentCeiling(new Double(newCeiling));
            }
            catch(NumberFormatException e)
            {
               log.warn("Found new store memory ceiling seting (" + ceiling + "), but can not convert to type double.", e);
            }
         }
      }
   }

   public Client getCallbackClient()
   {
      return callBackClient;
   }


   /**
    * Returns an id that can be used to identify this particular callback handler, which should be
    * representative of the client invoker it will make callbacks to.  Currently, this is the
    * session id associated with the invocation request.
    */
   public static String getId(InvocationRequest invocation)
   {
      String sessionId = invocation.getSessionId();
      Map metadata = invocation.getRequestPayload();
      if(metadata != null)
      {
         String listenerId = (String) metadata.get(Client.LISTENER_ID_KEY);
         if(listenerId != null)
         {
            sessionId = sessionId + "+" + listenerId;
         }
      }
      return sessionId;
   }

   /**
    * Returns an id that can be used to identify this particular callback handler, which should be
    * representative of the client invoker it will make callbacks to.
    */
   public String getId()
   {
      return sessionId;
   }

   public List getCallbacks(Map metadata)
   {
      log.trace("entering getCallbacks()");
      
      boolean blocking = false;
      int currentBlockingTimeout = blockingTimeout;
      
      if (metadata != null)
      {
         Object val = metadata.get(ServerInvoker.BLOCKING_MODE);
         if (ServerInvoker.BLOCKING.equals(val))
            blocking = true;
         
         val = metadata.get(ServerInvoker.BLOCKING_TIMEOUT);
         if (val != null)
         {
            if (val instanceof String)
            {
               try
               {
                  currentBlockingTimeout = Integer.parseInt((String) val);
               }
               catch (NumberFormatException e)
               {
                  log.warn("Error converting " + ServerInvoker.BLOCKING_TIMEOUT + " to type long.  " + e.getMessage());
               }
            }
            else
            {
               log.warn("Value for " + ServerInvoker.BLOCKING_TIMEOUT + " configuration must be of type " + String.class.getName() +
                        " and is " + val.getClass().getName());
            }
         }
      }

      if (trace)
      {
         log.trace("block: " + blocking);
         log.trace("blocking timeout: " + currentBlockingTimeout);
      }

      synchronized (callbacks)
      {
         List callbackList = constructCallbackList();
         if (blocking && callbackList.isEmpty())
         {
            try
            {
               callbacks.wait(currentBlockingTimeout);
               callbackList = constructCallbackList();
            }
            catch (InterruptedException e)
            {
               log.debug("unexpected interrupt");
            }
         }
         
         if (trace) log.trace("callbackList.size(): " + callbackList.size());
         return callbackList;
      }
   }
   
   private List constructCallbackList()
   {
      List callbackList = null;
      synchronized(callbacks)
      {
         callbackList = (List) callbacks.clone();
         callbacks.clear();
      }

      // get as many persisted callbacks as possible without over run on memory
      List persistedCallbacks = null;
      try
      {
         persistedCallbacks = getPersistedCallbacks();
      }
      catch(IOException e)
      {
         log.debug("Can not get persisted callbacks.", e);
         throw new RuntimeException("Error getting callbacks", e);
      }
      callbackList.addAll(persistedCallbacks);
      return callbackList;
   }

   private List getPersistedCallbacks() throws IOException
   {
      List callbacks = new ArrayList();

      int size = callbackStore.size();
      for(int x = 0; x < size; x++)
      {
         callbacks.add(callbackStore.getNext());
         // check the amount of mem in use as get callbacks out so
         // don't load so many callbacks from store, that run out of memory.
         if(isMemLow())
         {
            new Thread()
            {
               public void run()
               {
                  System.gc();
               }
            }.start();
            break;
         }
      }

      return callbacks;
   }

   public boolean isPullCallbackHandler()
   {
      return (callBackClient == null);
   }

   /**
    * Will take the callback message and send back to client.
    * If client locator is null, will store them till client polls to get them.
    *
    * @param callback
    * @throws HandleCallbackException
    */
   public void handleCallback(Callback callback) throws HandleCallbackException
   {
      handleCallback(callback, false, false);
   }

   /**
    * For push callbacks, will send the callback to the server invoker on the
    * client side, hand off processing to a separate thread, and return.<p>
    *
    * For pull callbacks, behaves the same as handleCallback(Callback callback).<p>
    *
    * @param callback
    * @throws HandleCallbackException
    */
   public void handleCallbackOneway(Callback callback) throws HandleCallbackException
   {
      handleCallback(callback, true, false);
   }

   /**
    * For push callbacks:<p>
    *   if serverSide == false, will send the callback to the server invoker on
    *   the client side, hand off processing to a separate thread, and return.<p>
    *
    *   if serverside == true, will hand off to a separate thread the sending
    *   of the callback and will then return.<p>
    *
    * For pull callbacks, behaves the same as handleCallback(Callback callback).<p>
    *
    * @param callback
    * @param serverSide
    * @throws HandleCallbackException
    */
   public void handleCallbackOneway(Callback callback, boolean serverSide)
      throws HandleCallbackException
   {
      handleCallback(callback, true, serverSide);
   }

   /**
    * For push callbacks:<p>
    *
    *   if asynch == false, behaves the same as handleCallback(Callback callback).<p>
    *
    *   if asynch == true:<p>
    *
    *     if serverSide == false, will send the callback to the server invoker on the client side,
    *     hand off processing to a separate thread, and return.<p>
    *
    *     if serverside == true, will hand off to a separate thread the sending of the callback and
    *     will then return.<p>
    *
    * For pull callbacks, behaves the same as handleCallback(Callback callback).<p>
    */
   public void handleCallback(Callback callback, boolean asynch, boolean serverSide)
      throws HandleCallbackException
   {
      try
      {
         Object callbackId = checkForCallbackListener(callback);

         if(callBackClient == null)
         {
            // need to check if should persist callback instead of keeping in memory
            if(shouldPersist())
            {
               try
               {
                  persistCallback(callback);
                  synchronized (callbacks)
                  {
                     callbacks.notify();
                  }
                  
                  callback = null;
                  // try to help out with the amount of memory usuage
                  new Thread()
                  {
                     public void run()
                     {
                        System.gc();
                     }
                  }.start();
               }
               catch(IOException e)
               {
                  log.debug("Unable to persist callback", e);
                  throw new HandleCallbackException("Unable to persist callback and will not " +
                                                    "be able to deliver.", e);
               }
            }
            else
            {
               synchronized(callbacks)
               {
                  if(trace){ log.debug(this + " got PULL callback. Adding to callback list ..."); }
                  callbacks.add(callback);
                  callbacks.notify();
               }
            }
         }
         else
         {
            // non null callback client

            try
            {
               if(trace){ log.debug(this + " got PUSH callback " + callback); }

               boolean handleAcknowledgement = false;

               if(callback != null)
               {
                  Map returnPayload = callback.getReturnPayload();

                  if(returnPayload == null)
                  {
                     returnPayload = new HashMap();
                  }
                  else
                  {
                     Object o = returnPayload.remove(REMOTING_ACKNOWLEDGES_PUSH_CALLBACKS);
                     if (!asynch)
                     {
                        if (o instanceof String  && Boolean.valueOf((String)o).booleanValue() ||
                            o instanceof Boolean && ((Boolean)o).booleanValue())
                           handleAcknowledgement = true;
                     }
                  }

                  returnPayload.put(Callback.SERVER_LOCATOR_KEY, serverLocator);
                  callback.setReturnPayload(returnPayload);
               }

               // Sending internal invocation so server invoker we are sending to will know how
               // pass onto it's client callback handler

               InternalInvocation internalInvocation =
                  new InternalInvocation(InternalInvocation.HANDLECALLBACK, new Object[]{callback});

               if (asynch)
               {
                  if(trace){ log.debug(this + " sending ASYNCHRONOUSLY the callback to the client"); }
                  callBackClient.
                     invokeOneway(internalInvocation, callback.getRequestPayload(), serverSide);
               }
               else
               {
                  if(trace){ log.debug(this + " sending SYNCHRONOUSLY the callback to the client"); }
                  callBackClient.invoke(internalInvocation, callback.getRequestPayload());
               }

               handlePushCallbackAcknowledgement(callbackId, handleAcknowledgement);
            }
            catch(Throwable ex)
            {
               if(callbackErrorHandler == null)
               {
                  // no callback handler, rethrowing the exception
                  throw ex;
               }

               if (trace) { log.trace(this + " handing the error over to " + callbackErrorHandler); }

               // a well behaved callback error handler will perform clean up and then rethrow the
               // exception so the client application has a chance to find out about the error
               // condition
               callbackErrorHandler.handleError(ex);
            }
         }
      }
      catch(Throwable t)
      {
         log.debug("Error handling callback", t);
         throw new HandleCallbackException("Error handling callback", t);
      }
   }

   private void persistCallback(InvocationRequest callback) throws IOException
   {
      callbackStore.add(callback);
   }

   /**
    * Calculates the percentage amount of free memory compared to max memory. The calculations for
    * this is not always acurate. The reason is that total memory used is usually less than the max
    * allowed. Thus, the amount of free memory is relative to the total amount allocated at that
    * point in time. It is not until the total amount of memory allocated is equal to the max it
    * will be allowed to allocate. At this point, the amount of free memory becomes relavent.
    * Therefore, if the memory percentage ceiling is high, it might not trigger until after free
    * memory percentage is well below the ceiling.
    */
   private boolean shouldPersist()
   {
      if (shouldPersist)
         return true;
      return isMemLow();
   }

   private boolean isMemLow()
   {
      Runtime runtime = Runtime.getRuntime();
      long max = runtime.maxMemory();
      long total = runtime.totalMemory();
      long free = runtime.freeMemory();
      float percentage = 100 * free / total;
      if(max == total && memPercentCeiling >= percentage)
      {
         return true;
      }
      else
      {
         return false;
      }
   }

   Object checkForCallbackListener(Callback callback)
   {
      Map returnPayload = callback.getReturnPayload();
      if (returnPayload == null)
         return null;

      Object listenerObject = returnPayload.remove(CALLBACK_LISTENER);
      if (listenerObject == null)
         return null;

      Object callbackId = returnPayload.get(CALLBACK_ID);
      if (callbackId == null)
      {
         log.error("CALLBACK_ID is null: unable to acknowledge callback");
         return null;
      }

      if (listenerObject instanceof CallbackListener)
      {
         if(listenerId != null)
         {
            returnPayload.put(Client.LISTENER_ID_KEY, listenerId);
            idToListenerMap.put(callbackId, listenerObject);
            return callbackId;
         }
         else
         {
            log.error("LISTENER_ID_KEY is null: unable to acknowledge callback");
            return null;
         }
      }
      else
      {
         log.error("callback preprocess listener has wrong type: " + listenerObject);
         return null;
      }
   }

   private void handlePushCallbackAcknowledgement(Object callbackId, boolean handleAck)
   {
      if (!handleAck)
         return;

      if (callbackId == null)
      {
         log.error("Unable to acknowledge push callback: callback id is null");
         return;
      }

      CallbackListener listener = (CallbackListener) idToListenerMap.get(callbackId);
      if (listener == null)
      {
         log.error("Unable to acknowledge push callback: listener is null");
         return;
      }

      listener.acknowledgeCallback(this, callbackId, null);
   }

   /**
    * Calls listeners to acknowledge callbacks
    * @param invocation carries identities of Callbacks to acknowledge and,
    *                   optionally, responses
    */
   public void acknowledgeCallbacks(InternalInvocation invocation) throws Exception
   {
      Object[] params = invocation.getParameters();
      if (params == null)
         return;

      List callbackIds = (List) params[0];
      List responses = (List) params[1];
      if (callbackIds == null || callbackIds.size() == 0)
         return;

      Iterator idsIterator = callbackIds.iterator();
      Iterator responseIterator = null;
      if (responses != null)
         responseIterator = responses.iterator();

      Object callbackId = null;
      Object response = null;
      while(idsIterator.hasNext())
      {
         callbackId = idsIterator.next();
         if (responseIterator != null)
            response = responseIterator.next();

         CallbackListener listener = (CallbackListener) idToListenerMap.remove(callbackId);

         if (listener == null)
         {
            log.warn("Cannot acknowledge callback: unrecognized id: " + callbackId);
            continue;
         }

         listener.acknowledgeCallback(this, callbackId, response);
      }
   }

   public String toString()
   {
      return "ServerInvokerCallbackHandler[" + getId() + "]";
   }

   /**
    * This method is required to be called upon removing a callback listener
    * so can clean up resources used by the handler.  In particular, should
    * call disconnect on internal Client.
    */
   public synchronized void destroy()
   {
      if(callBackClient != null)
      {
         callBackClient.disconnect();
      }

      if(callbackStore != null)
      {
         callbackStore.purgeFiles();
      }
   }

   public void shutdown()
   {
      serverInvoker.shutdownCallbackHandler(this, invocation);
      destroy();
      log.debug(this + " shut down");
   }
   
   public void handleConnectionException(Throwable throwable, Client client)
   {
      if (clientSessionId.equals(client.getSessionId()))
      {
         shutdown();
      }
   }

   public boolean isShouldPersist()
   {
      return shouldPersist;
   }

   public void setShouldPersist(boolean shouldPersist)
   {
      this.shouldPersist = shouldPersist;
   }
   
   static private Object getMBeanAttribute(final MBeanServer server, final ObjectName objectName, final String attribute)
   throws Exception
   {
      if (SecurityUtility.skipAccessControl())
      {
         return server.getAttribute(objectName, attribute);
      }
      
      try
      {
         return AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return server.getAttribute(objectName, attribute);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (Exception) e.getCause();
      }  
   }
   
   static private boolean isInstanceOf(final MBeanServer server, final ObjectName objectName, final String className)
   throws InstanceNotFoundException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return server.isInstanceOf(objectName, className);
      }
      
      try
      {
         return ((Boolean)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return new Boolean(server.isInstanceOf(objectName, className));
            }
         })).booleanValue();
      }
      catch (PrivilegedActionException e)
      {
         throw (InstanceNotFoundException) e.getCause();
      }
   }
   
   static private String getSystemProperty(final String name, final String defaultValue)
   {
      if (SecurityUtility.skipAccessControl())
         return System.getProperty(name, defaultValue);
         
      String value = null;
      try
      {
         value = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.getProperty(name, defaultValue);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
      
      return value;
   }
   
   static private String getSystemProperty(final String name)
   {
      if (SecurityUtility.skipAccessControl())
         return System.getProperty(name);
      
      String value = null;
      try
      {
         value = (String)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.getProperty(name);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
      
      return value;
   }
}
