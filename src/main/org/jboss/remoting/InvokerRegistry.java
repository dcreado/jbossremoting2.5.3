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
import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.remoting.transport.ClientFactory;
import org.jboss.remoting.transport.ClientInvoker;
import org.jboss.remoting.transport.ServerFactory;
import org.jboss.remoting.transport.local.LocalClientInvoker;
import org.jboss.remoting.util.SecurityUtility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * InvokerRegistry is a simple registery for creating client and server side Invoker implementations,
 * getting information about the invokers and register as a invoker creator for one or more specific
 * transports.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @version $Revision: 5886 $
 */
public class InvokerRegistry
{
   private static final Logger log = Logger.getLogger(InvokerRegistry.class);

   private static boolean trace = log.isTraceEnabled();

   private static final Map clientLocators = new HashMap();
   private static final Map serverLocators = new HashMap();

   private static final Set registeredLocators = new HashSet();
   private static final Object serverLock = new Object();
   private static final Object clientLock = new Object();

   private static final Map transportClientFactoryClasses = new HashMap();
   private static final Map transportServerFactoryClasses = new HashMap();
   
   private static final RuntimePermission INVOKER_REGISTRY_UPDATE_PERMISSION = new RuntimePermission("invokerRegistryUpdate");

   /**
    * return an array of InvokerLocators that are local to this VM (server invokers)
    */
   public static final InvokerLocator[] getRegisteredServerLocators()
   {
      synchronized (serverLock)
      {
         return (InvokerLocator[]) registeredLocators.toArray(new InvokerLocator[registeredLocators.size()]);
      }
   }

   /**
    * return a suitable local server invoker that can service the remote invoker locator based on
    * a compatible transport
    *
    * @param remote
    */
   public static InvokerLocator getSuitableServerLocatorForRemote(InvokerLocator remote)
   {
      synchronized (serverLock)
      {
         Iterator iter = registeredLocators.iterator();
         while(iter.hasNext())
         {
            InvokerLocator l = (InvokerLocator) iter.next();
            if(l.getProtocol().equals(remote.getProtocol()))
            {
               // we found a valid transport match
               return l;
            }
         }
         return null;
      }
   }

   /**
    * return an array of String of the registered transports
    */
   public static final String[] getRegisteredInvokerTransports()
   {
      synchronized(clientLock)
      {
         Set set = transportClientFactoryClasses.keySet();
         String transports[] = new String[set.size()];
         return (String[]) set.toArray(transports);
      }
   }

   /**
    * return an array of ClientInvokers that are connected
    */
   public static final ClientInvoker[] getClientInvokers()
   {
      synchronized(clientLock)
      {
         if(clientLocators.isEmpty())
         {
            return new ClientInvoker[0];
         }
         List clientInvokerList = new ArrayList();
         Collection collection = clientLocators.values();
         Iterator itr = collection.iterator();
         while(itr.hasNext())
         {
            List holderList = (List)itr.next();
            if(holderList != null)
            {
               for(int x = 0; x < holderList.size(); x++)
               {
                  ClientInvokerHolder holder = (ClientInvokerHolder)holderList.get(x);
                  clientInvokerList.add(holder.getClientInvoker());
               }
            }
         }

         return (ClientInvoker[]) clientInvokerList.toArray(new ClientInvoker[clientInvokerList.size()]);
      }
   }

   /**
    * return an array of ServerInvokers that are connected
    */
   public static final ServerInvoker[] getServerInvokers()
   {
      synchronized(serverLock)
      {
         if(serverLocators.isEmpty())
         {
            return new ServerInvoker[0];
         }
         Collection collection = serverLocators.values();
         return (ServerInvoker[]) collection.toArray(new ServerInvoker[collection.size()]);
      }
   }

   /**
    * register a client/server invoker factory Class pair for a given transport
    *
    * @param transport
    * @param clientFactory implementation of org.jboss.remoting.transport.ClientFactory
    * @param serverFactory implementation of org.jboss.remoting.transport.ServerFactory
    */
   public static void registerInvokerFactories(String transport, Class clientFactory, Class serverFactory)
   {
      doSecurityCheck();
      synchronized (clientLock)
      {
         transportClientFactoryClasses.put(transport, clientFactory);
      }
      synchronized (serverLock)
      {
         transportServerFactoryClasses.put(transport, serverFactory);
      }
   }

   /**
    * unregister a client/server invoker factory pair for the given transport
    *
    * @param transport
    */
   public static void unregisterInvokerFactories(String transport)
   {
      doSecurityCheck();
      synchronized (clientLock)
      {
         transportClientFactoryClasses.remove(transport);
      }
      synchronized (serverLock)
      {
         transportServerFactoryClasses.remove(transport);
      }
   }

   public static void unregisterLocator(InvokerLocator locator)
   {
      doSecurityCheck();
      synchronized (serverLock)
      {
         serverLocators.remove(locator);
         registeredLocators.remove(locator);
      }
   }

   /**
    * returns true if the client invoker is registered in the local JVM for a given locator
    *
    * @param locator
    */
   public static boolean isClientInvokerRegistered(InvokerLocator locator)
   {
      synchronized(clientLock)
      {
         return clientLocators.containsKey(locator);
      }
   }

   /**
    * Called to destroy any cached RemoteClientInvoker copies inside the registry. This method
    * must be called when it is determined that a remote server (via the locator) is no
    * longer available.
    */
   public static void destroyClientInvoker(InvokerLocator locator, Map configuration)
   {
      doSecurityCheck();
      synchronized(clientLock)
      {
         if (trace)
         {
            log.trace("destroying client invoker " + locator + ", config " + configuration);
         }

         ClientInvoker invoker = decrementClientInvokerCounter(locator, configuration);

         if(invoker != null)
         {
            if (trace)
            {
               log.trace("disconnecting " + invoker);
            }
            invoker.disconnect();
            invoker = null;
         }
      }
   }
 
   /**
     * create a ClientInvoker instance, using the specific InvokerLocator, which is just a client-side
    * invoker to a remote server.  Will use the default configuration values for the transport.
    *
    * @param locator
    * @return
    * @throws Exception
    */
   public static ClientInvoker createClientInvoker(InvokerLocator locator)
         throws Exception
   {
      return createClientInvoker(locator, null);
   }

   /**
    * create a ClientInvoker instance, using the specific InvokerLocator, which is just a client-side
    * invoker to a remote server
    *
    * @param locator
    * @return
    * @throws Exception
    */
   public static ClientInvoker createClientInvoker(InvokerLocator locator, Map configuration)
         throws Exception
   {
      doSecurityCheck();
      
      if(locator == null)
      {
         throw new NullPointerException("locator cannot be null");
      }
      synchronized(clientLock)
      {
         ClientInvoker invoker = getRegisteredClientInvoker(locator, configuration);
         if(invoker != null)
         {
            if(trace) { log.trace("Found and returning cached client invoker (" + invoker + ")"); }
            return invoker;
         }

         boolean isForceRemote = false;
         boolean isPassByValue = false;
         Map parameters = locator.getParameters();
         if(parameters != null)
         {
            String value = (String) parameters.get(InvokerLocator.BYVALUE);
            if(value != null && Boolean.valueOf(value).booleanValue())
            {
               isPassByValue = true;
            }
            value = (String) parameters.get(InvokerLocator.FORCE_REMOTE);
            if(value != null && Boolean.valueOf(value).booleanValue())
            {
               isForceRemote = true;
            }
         }
         // configuration map will override locator params
         if(configuration != null)
         {
            String value = (String) configuration.get(InvokerLocator.BYVALUE);
            if(value != null && Boolean.valueOf(value).booleanValue())
            {
               isPassByValue = true;
            }
            value = (String) configuration.get(InvokerLocator.FORCE_REMOTE);
            if(value != null && Boolean.valueOf(value).booleanValue())
            {
               isForceRemote = true;
            }
         }

         // Check to see if server invoker is local
         // If in server locators map, then created locally by this class
         ServerInvoker svrInvoker = null;
         if (!isForceRemote)
         {
            synchronized (serverLock)
            {
               svrInvoker = (ServerInvoker) serverLocators.get(locator);
            }
            if(svrInvoker != null)
            {
               LocalClientInvoker localInvoker = new LocalClientInvoker(locator, configuration, isPassByValue);
               // have to set reference to local server invoker so client in invoke directly
               localInvoker.setServerInvoker(svrInvoker);
               invoker = localInvoker;
               InvokerLocator l = invoker.getLocator();

               addRegisteredClientInvoker(invoker, l, configuration);
            }
         }
         
         if (svrInvoker == null) //not local
         {
            String protocol = locator.getProtocol();
            if(protocol == null)
            {
               throw new NullPointerException("protocol cannot be null for the locator");
            }

            invoker = loadClientInvoker(protocol, locator, configuration);

            InvokerLocator l = invoker.getLocator();

            addRegisteredClientInvoker(invoker, l, configuration);
         }
         return invoker;
      }
   }

   private static void addRegisteredClientInvoker(ClientInvoker invoker, InvokerLocator locator, Map configuration)
   {
      ClientInvokerHolder holder = new ClientInvokerHolder(invoker, configuration);
      List holderList = (List) clientLocators.get(locator);
      if (holderList != null)
      {
         if(holderList.contains(holder))
         {
            throw new RuntimeException("Registering new ClientInvoker (" + invoker + "), but it already exists.");
         }
         else
         {
            holderList.add(holder);
         }
      }
      else
      {
         holderList = new ArrayList();
         holderList.add(holder);
         clientLocators.put(locator, holderList);
      }

      incrementClientInvokerCounter(holder);

   }

   /**
    * This will check the internal client invoker registry to see if there is a client invoker for
    * the specified locator that also has the same config map entries.  Will return it if found, null otherwise.
    * Note, this will also increment the internal reference count for the invoker
    * @param locator
    * @param configuration
    */
   private static ClientInvoker getRegisteredClientInvoker(InvokerLocator locator, Map configuration)
   {
      ClientInvoker invoker = null;

      List holderList = (List) clientLocators.get(locator);
      if (holderList != null)
      {
         for (int x = 0; x < holderList.size(); x++)
         {
            ClientInvokerHolder holder = (ClientInvokerHolder) holderList.get(x);
            if (sameInvoker(holder, configuration))
            {
               incrementClientInvokerCounter(holder);
               invoker = holder.getClientInvoker();
            }
         }
      }

      return invoker;

   }

   private static boolean sameInvoker(ClientInvokerHolder holder, Map configuration)
   {
      boolean isSame = false;

      if(holder != null && holder.getClientInvoker() != null)
      {
         Map config = holder.getConfig();
         if(config == null && configuration == null)
         {
            isSame = true;
         }
         else if(config != null && configuration != null)
         {
            isSame = config.equals(configuration);
         }
      }

      return isSame;
   }

   private static void incrementClientInvokerCounter(ClientInvokerHolder holder)
   {
      holder.incrementCount();
   }

   private static ClientInvoker loadClientInvoker(String protocol, InvokerLocator locator, Map configuration) throws Exception
   {
      ClientInvoker clientInvoker = null;

      Class transportFactoryClass = getTransportClientFactory(protocol);
      if(transportFactoryClass != null)
      {
         ClientFactory transportFactory = (ClientFactory)transportFactoryClass.newInstance();
         Method getClientInvokerMethod = getMethod(transportFactoryClass,
                                                   "createClientInvoker",
                                                   new Class[] {InvokerLocator.class, Map.class});
         clientInvoker = (ClientInvoker)getClientInvokerMethod.invoke(transportFactory, new Object[] {locator, configuration});
      }
      else
      {
         throw new ClassNotFoundException("Could not find class " + transportFactoryClass);
      }

      return clientInvoker;
   }

   private static ServerInvoker loadServerInvoker(String protocol, InvokerLocator locator, Map configuration) throws Exception
   {
      ServerInvoker serverInvoker = null;

      Class transportFactoryClass = getTransportServerFactory(protocol);
      if(transportFactoryClass != null)
      {
         ServerFactory transportFactory = (ServerFactory)transportFactoryClass.newInstance();
         Method getServerInvokerMethod = getMethod(transportFactoryClass,
                                                   "createServerInvoker",
                                                   new Class[] {InvokerLocator.class, Map.class});         
         serverInvoker = (ServerInvoker)getServerInvokerMethod.invoke(transportFactory, new Object[] {locator, configuration});
      }
      else
      {
         throw new ClassNotFoundException("Could not find class " + transportFactoryClass);
      }

      return serverInvoker;
   }

   private static Class getTransportClientFactory(String protocol)
         throws ClassNotFoundException
   {
      Class transportFactoryClass = (Class)transportClientFactoryClasses.get(protocol);
      if(transportFactoryClass == null)
      {
         String transportFactoryClassName = "org.jboss.remoting.transport." + protocol + ".TransportClientFactory";
         transportFactoryClass = ClassLoaderUtility.loadClass(InvokerRegistry.class, transportFactoryClassName);
         transportClientFactoryClasses.put(protocol, transportFactoryClass);
      }
      return transportFactoryClass;
   }

   private static Class getTransportServerFactory(String protocol)
         throws ClassNotFoundException
   {
      Class transportFactoryClass = (Class)transportServerFactoryClasses.get(protocol);
      if(transportFactoryClass == null)
      {
         String transportFactoryClassName = "org.jboss.remoting.transport." + protocol + ".TransportServerFactory";
         transportFactoryClass = ClassLoaderUtility.loadClass(transportFactoryClassName, InvokerRegistry.class);
         transportServerFactoryClasses.put(protocol, transportFactoryClass);
      }
      return transportFactoryClass;
   }

   /**
    * returns true if the server invoker is registered in the local JVM for a given locator/handler pair
    *
    * @param locator
    */
   public static boolean isServerInvokerRegistered(InvokerLocator locator)
   {
      synchronized(serverLock)
      {
         return serverLocators.containsKey(locator);
      }
   }

   /**
    * create a ServerInvoker instance, using the specific Invoker locator data and an implementation of the
    * ServerInvocationHandler interface.  Will use the default configuration values for the transport.
    *
    * @param locator
    * @return
    * @throws Exception
    */
   public static ServerInvoker createServerInvoker(InvokerLocator locator)
         throws Exception
   {
      return createServerInvoker(locator, null);
   }

   /**
    * create a ServerInvoker instance, using the specific Invoker locator data and an implementation of the
    * ServerInvocationHandler interface along with
    *
    * @param locator
    * @return
    * @throws Exception
    */
   public static ServerInvoker createServerInvoker(InvokerLocator locator, Map configuration)
         throws Exception
   {
      doSecurityCheck();
      ServerInvoker invoker = null;
      synchronized(serverLock)
      {
         invoker = (ServerInvoker) serverLocators.get(locator);
         if(invoker != null)
         {
            throw new InvalidConfigurationException("The invoker for locator (" + locator + ") is already " +
                                                    "in use by another Connector.  Either change the locator or " +
                                                    "add new handlers to existing Connector.");
         }
         String protocol = locator.getProtocol();

         invoker = loadServerInvoker(protocol, locator, configuration);

         serverLocators.put(locator, invoker);
         registeredLocators.add(invoker.getLocator());
      }
      return invoker;
   }

   public static void destroyServerInvoker(ServerInvoker invoker)
   {
      doSecurityCheck();
      if(invoker != null)
      {
         InvokerLocator locator = invoker.getLocator();
         unregisterLocator(locator);
      }
   }

   private static ClientInvoker decrementClientInvokerCounter(InvokerLocator locator, Map configuration)
   {
      List holderList = (List)clientLocators.get(locator);

      if (holderList == null)
      {
         log.debug("Could not decrement client invoker counter for locator " + locator +
            " as does not exist in invoker registry.");
         return null;
      }

      ClientInvokerHolder holder = null;

      // now look for specific invoker by configuration map
      for(int x = 0; x < holderList.size(); x++)
      {
         holder = (ClientInvokerHolder)holderList.get(x);
         if(holder != null)
         {
            Map config = holder.getConfig();
            if(config == null && configuration == null)
            {
               break;
            }
            else if(config != null && configuration != null)
            {
               if(config.equals(configuration))
               {
                  break;
               }
            }
         }
      }

      if (holder == null)
      {
         log.debug("Could not decrement client invoker counter for locator " + locator +
                   "as does not exist in invoker registry with matching configuraion map.");
         return null;
      }

      ClientInvoker clientInvoker =  null;
      holder.decrementCount();

      if(holder.getCount() == 0)
      {
         clientInvoker = holder.getClientInvoker();
         holderList.remove(holder);
         if(holderList.isEmpty())
         {
            clientLocators.remove(locator);
         }

         log.debug("removed " + clientInvoker + " from registry");
      }
      else
      {
         log.debug("decremented " + holder.getClientInvoker() +
            "'s count, current count " + holder.getCount());
      }

      return clientInvoker;
   }

   /**
    * This is needed by the ServerInvoker since it may change the port being used (if port specified was <= 0) to
    * next available port.
    *
    * @param locator
    * @param newLocator
    */
   public static void updateServerInvokerLocator(InvokerLocator locator, InvokerLocator newLocator)
   {
      doSecurityCheck();
      synchronized (serverLock)
      {
         Object si = serverLocators.get(locator);
         serverLocators.remove(locator);
         registeredLocators.remove(locator);
         serverLocators.put(newLocator, si);
         registeredLocators.add(newLocator);
      }
   }

   /**
    * Indicates if a specific transport protocol type (e.g. socket, sslsocket, rmi, https)
    * supports ssl.  Note: almost all transports are able to support ssl if socket/serversocket
    * factory is set with an ssl version, so this is really just a hint from the invoker implementation.
    *
    * @param transport
    * @return
    * @throws Exception
    */
   public static boolean isSSLSupported(String transport) throws Exception
   {
      doSecurityCheck();
      boolean isSSLSupported = false;
      Class transportFactoryClass = null;
      try
      {
         synchronized (clientLock)
         {
            transportFactoryClass = getTransportClientFactory(transport);
         }
         ClientFactory clientFactory = (ClientFactory)transportFactoryClass.newInstance();
         Method meth = getMethod(transportFactoryClass, "supportsSSL", new Class[]{});         
         Boolean boolVal = (Boolean)meth.invoke(clientFactory, null);
         isSSLSupported = boolVal.booleanValue();
      }
      catch (ClassNotFoundException e)
      {
         Exception ex = new Exception("Can not verify transport (" + transport + ") supports SSL because can not find invoker implementation matching transport.");
         ex.initCause(e);
         throw ex;
      }
      catch (NoSuchMethodException e)
      {
         Exception ex = new Exception("Can not call supportsSSL method on client factory class (" + transportFactoryClass + ") as there is no such method.");
         ex.initCause(e);
         throw ex;
      }
      catch (IllegalAccessException e)
      {
         Exception ex = new Exception("Can not call create instance of client factory class (" + transportFactoryClass + ").");
         ex.initCause(e);
         throw ex;
      }
      catch (InvocationTargetException e)
      {
         Exception ex = new Exception("Can not call supportsSSL method on client factory class (" + transportFactoryClass + ").");
         ex.initCause(e);
         throw ex;
      }
      catch (InstantiationException e)
      {
         Exception ex = new Exception("Can not call supportsSSL method on client factory class (" + transportFactoryClass + ").");
         ex.initCause(e);
         throw ex;
      }

      return isSSLSupported;
   }

   public String toString()
   {
      return "InvokerRegistry[" + Integer.toHexString(hashCode()) + "]";
   }

   private static class ClientInvokerHolder
   {
      private ClientInvoker invoker = null;
      private Map config = null;
      private int counter = 0;

      public ClientInvokerHolder(ClientInvoker invoker, Map config)
      {
         this.invoker = invoker;
         this.config = config;
      }

      public void incrementCount()
      {
         counter++;
      }

      public void decrementCount()
      {
         counter--;
         if(counter < 0)
         {
            throw new RuntimeException("ClientInvokerHolder decremented to negative number for client invoker " + invoker);
         }
      }

      public int getCount()
      {
         return counter;
      }

      public ClientInvoker getClientInvoker()
      {
         return invoker;
      }

      public Map getConfig()
      {
         return config;
      }

      public boolean equals(Object o)
      {
         boolean isEqual = false;

         if(o instanceof ClientInvokerHolder)
         {
            ClientInvokerHolder h = (ClientInvokerHolder)o;
            if(invoker.equals(h.getClientInvoker()))
            {
               Map configuration = h.getConfig();
               if(config == null && configuration == null)
               {
                  isEqual = true;
               }
               else if(config != null && configuration != null)
               {
                  isEqual = config.equals(configuration);
               }
            }
         }
         return isEqual;
      }

   }
   
   static private Method getMethod(final Class c, final String name, final Class[] parameterTypes)
   throws NoSuchMethodException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return c.getMethod(name, parameterTypes);
      }

      try
      {
         return (Method) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws NoSuchMethodException
            {
               return c.getMethod(name, parameterTypes);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (NoSuchMethodException) e.getCause();
      }
   }
   
   static private void doSecurityCheck()
   {
      // If there is no Security Manager, the issue is moot.
      final SecurityManager sm = System.getSecurityManager();
      if (sm == null)
      {
         return;
      }

      // If the calling code is not verifiably in Remoting, then require it to have InvokerRegistryUpdatePermission.
      sm.checkPermission(INVOKER_REGISTRY_UPDATE_PERMISSION);
   }
}
