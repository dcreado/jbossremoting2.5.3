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

package org.jboss.remoting.transporter;

import org.jboss.logging.Logger;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.Client;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.ServerInvokerMetadata;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.jboss.remoting.network.NetworkInstance;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.util.SecurityUtility;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;

/**
 * Class to be used as a factory via static method calls to get
 * remote proxy to POJO that exists within a external process.
 * Note, if using clustered, will use the multicast detector by default.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TransporterClient implements InvocationHandler, Serializable
{
   private Client remotingClient = null;
   private boolean isClustered = false;
   private String subSystem = null;

   private LoadBalancer loadBalancer = new DefaultLoadBalancer();

   private final Logger log = Logger.getLogger(TransporterClient.class);
   private static final long serialVersionUID = 7418567482011657189L;

   /**
    * Creates the remoting client to server POJO.
    * Is not clustered.
    *
    * @param locator
    * @throws Exception
    */
   private TransporterClient(InvokerLocator locator) throws Exception
   {
      remotingClient = new Client(locator);
      remotingClient.connect();
   }

   /**
    * Creates the remoting client to server POJO.
    * Is clustered
    *
    * @param locator
    * @param targetSubsystem
    * @throws Exception
    */
   private TransporterClient(InvokerLocator locator, String targetSubsystem) throws Exception
   {
      remotingClient = new Client(locator, targetSubsystem);
      remotingClient.connect();
      this.isClustered = true;
      this.subSystem = targetSubsystem;
   }

   /**
    * Creates the remoting client to server POJO.
    * Is clustered
    *
    * @param locator
    * @param targetSubsystem
    * @param loadbalancer    policy
    * @throws Exception
    */
   private TransporterClient(InvokerLocator locator, String targetSubsystem, LoadBalancer loadbalancer) throws Exception
   {
      this.loadBalancer = loadbalancer;
      remotingClient = new Client(locator, targetSubsystem);
      remotingClient.connect();
      this.isClustered = true;
      this.subSystem = targetSubsystem;
   }

   /**
    * Disconnects the remoting client
    */
   private void disconnect()
   {
      if (remotingClient != null)
      {
         remotingClient.disconnect();
      }
   }

   /**
    * Will set up network registry and detector for clustering (to identify other
    * remoting servers running on network).
    *
    * @throws Exception
    */
   private static void setupDetector() throws Exception
   {
      InternalTransporterServices services = InternalTransporterServices.getInstance();

      // if no one has setup our internal services yet, let's do it now
      if (!services.isSetup())
      {
         // we need an MBeanServer to store our network registry and multicast detector services
         MBeanServer server = createMBeanServer();

         // multicast detector will detect new network registries that come online
         MulticastDetector detector = new MulticastDetector();
         NetworkRegistry registry = NetworkRegistry.getInstance();
         services.setup(server, detector, null, registry, null, true, true);
         detector.start();
      }
      else
      {
         // the internal services singleton is already setup, but make sure it has the services we need
         if (services.getDetector() == null)
         {
            MulticastDetector detector = new MulticastDetector();
            services.assignDetector(detector, null, true);
            detector.start();
         }

         if (services.getNetworkRegistry() == null)
         {
            NetworkRegistry registry = NetworkRegistry.getInstance();
            services.assignNetworkRegistry(registry, null, true);
         }
      }

      return;
   }

   /**
    * Create a remote proxy to a POJO on a remote server.
    *
    * @param locatorURI  - the remoting locator uri to the target server where the target POJO exists.
    * @param targetClass - the interface class of the POJO will be calling upon.
    * @param clustered   - true will cause the transporter to look for other remoting serves that have the POJO running
    *                    and include it in the client's target list.  If a call on first target fails, will seamlessly fail over to one
    *                    of the other discovered targets.
    * @return dynamic remote proxy typed to the interface specified by the targetClass param.
    * @throws Exception
    */
   public static Object createTransporterClient(String locatorURI, Class targetClass, boolean clustered) throws Exception
   {
      if (!clustered)
      {
         return createTransporterClient(locatorURI, targetClass);
      }
      else
      {
         if (InternalTransporterServices.getInstance().getNetworkRegistry() == null)
         {
            setupDetector();
         }
         InvokerLocator locator = new InvokerLocator(locatorURI);
         TransporterClient client = new TransporterClient(locator, targetClass.getName());
         ClassLoader tcl = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return Thread.currentThread().getContextClassLoader();
            }
         });
         return Proxy.newProxyInstance(tcl, new Class[]{targetClass}, client);
      }
   }

   /**
    * Create a remote proxy to a POJO on a remote server.
    *
    * @param locatorURI  - the remoting locator uri to the target server where the target POJO exists.
    * @param targetClass - the interface class of the POJO will be calling upon.
    * @param loadBalancer - policy for selecting which target server to use from list of available servers.
    * @return dynamic remote proxy typed to the interface specified by the targetClass param.
    * @throws Exception
    */
   public static Object createTransporterClient(String locatorURI, Class targetClass, LoadBalancer loadBalancer) throws Exception
   {
         if (InternalTransporterServices.getInstance().getNetworkRegistry() == null)
         {
            setupDetector();
         }
         InvokerLocator locator = new InvokerLocator(locatorURI);
         TransporterClient client = new TransporterClient(locator, targetClass.getName(), loadBalancer);
         ClassLoader tcl = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               return Thread.currentThread().getContextClassLoader();
            }
         });
         return Proxy.newProxyInstance(tcl, new Class[]{targetClass}, client);
   }

   /**
    * Create a remote proxy to a POJO on a remote server.
    *
    * @param locatorURI  - the remoting locator uri to the target server where the target POJO exists.
    * @param targetClass - the interface class of the POJO will be calling upon.
    * @return dynamic remote proxy typed to the interface specified by the targetClass param.
    * @throws Exception
    */
   public static Object createTransporterClient(String locatorURI, Class targetClass) throws Exception
   {
      InvokerLocator locator = new InvokerLocator(locatorURI);
      return createTransporterClient(locator, targetClass);
   }

   /**
    * Create a remote proxy to a POJO on a remote server.
    *
    * @param locator     - the remoting locator to the target server where the target POJO exists.
    * @param targetClass - the interface class of the POJO will be calling upon.
    * @return dynamic remote proxy typed to the interface specified by the targetClass param.
    * @throws Exception
    */
   public static Object createTransporterClient(InvokerLocator locator, Class targetClass) throws Exception
   {
      TransporterClient client = new TransporterClient(locator, targetClass.getName());
      ClassLoader tcl = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return Thread.currentThread().getContextClassLoader();
         }
      });
      return Proxy.newProxyInstance(tcl, new Class[]{targetClass}, client);
   }

   /**
    * Needs to be called by user when no longer need to make calls on remote POJO.  Otherwise will
    * maintain remote connection until this is called.
    *
    * @param transporterClient
    */
   public static void destroyTransporterClient(Object transporterClient)
   {
      if (transporterClient instanceof Proxy)
      {
         InvocationHandler handler = Proxy.getInvocationHandler(transporterClient);
         if (handler instanceof TransporterClient)
         {
            TransporterClient client = (TransporterClient) handler;
            client.disconnect();
         }
         else
         {
            throw new IllegalArgumentException("Object is not a transporter client.");
         }
      }
      else
      {
         throw new IllegalArgumentException("Object is not a transporter client.");
      }
   }

   /**
    * The method called when anyone calls on the dynamic proxy returned by getProcessor().
    * This method will simply convert the proxy call info into a remoting invocation on the
    * target remoting server (using a NameBaseInvocation).
    *
    * @param proxy
    * @param method
    * @param args
    * @return
    * @throws Throwable
    */
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      String methodName = method.getName();
      String[] paramSig = createParamSignature(method.getParameterTypes());

      NameBasedInvocation request = new NameBasedInvocation(methodName,
                                                            args,
                                                            paramSig);
      Object response = null;

      boolean failOver = false;

      do
      {
         try
         {
            failOver = false;
            response = remotingClient.invoke(request);
         }
         catch (CannotConnectException cnc)
         {
            failOver = findAlternativeTarget();
            if (!failOver)
            {
               throw cnc;
            }
         }
         catch (InvocationTargetException itex)
         {
            Throwable rootEx = itex.getCause();
            throw rootEx;
         }
      }
      while (failOver);

      return response;
   }

   /**
    * Will check to see if the network registry has found any other remoting servers.  Then will check
    * to see if any of them contain the subsystem we are interested in (which will corespond to the proxy type we
    * are using).  If one is found, will try to create a remoting client and connect to it.
    * If can't find one, will return fasle.
    *
    * @return
    */
   private boolean findAlternativeTarget()
   {
      boolean failover = false;
      ArrayList availableList = new ArrayList();
      NetworkRegistry registry = InternalTransporterServices.getInstance().getNetworkRegistry();
      if (registry != null)
      {
         NetworkInstance[] instances = registry.getServers();
         if (instances != null)
         {
            for (int x = 0; x < instances.length; x++)
            {
               NetworkInstance netInstance = instances[x];
               ServerInvokerMetadata[] metadata = netInstance.getServerInvokers();
               for (int i = 0; i < metadata.length; i++)
               {
                  ServerInvokerMetadata data = metadata[i];
                  String[] subsystems = data.getSubSystems();
                  for (int z = 0; z < subsystems.length; z++)
                  {
                     if (subSystem.equalsIgnoreCase(subsystems[z]))
                     {
                        availableList.add(data);

                     }
                  }
               }
            }
            //If alternative servers are found
            if (availableList.size() > 0)
            {
               int index = loadBalancer.selectServer(availableList);
               if (log.isDebugEnabled())
               {
                  log.debug("Total of " + availableList.size() + " available servers found.");
                  log.debug("Using server number " + index);
               }
               //reconnect to the new server
               ServerInvokerMetadata data = (ServerInvokerMetadata) availableList.get(index);
               InvokerLocator newLocator = data.getInvokerLocator();

               if (!remotingClient.getInvoker().getLocator().equals(newLocator))
               {
                  try
                  {
                     remotingClient = new Client(newLocator);
                     remotingClient.connect();
                     return true;
                  }
                  catch (Exception e)
                  {
                     log.warn("Problem connecting to newly found alternate target.", e);
                  }
               }

            }
         }
      }
      return failover;

   }

   /**
    * Converts the Class array supplied via the dynamic proxy to
    * a String array of the respective class names, which is need by
    * the NameBasedInvocation object.
    *
    * @param args
    * @return
    */
   private String[] createParamSignature(Class[] args)
   {
      if (args == null || args.length == 0)
      {
         return new String[]{};
      }
      String[] paramSig = new String[args.length];
      for (int x = 0; x < args.length; x++)
      {
         paramSig[x] = args[x].getName();
      }
      return paramSig;
   }

   static private MBeanServer createMBeanServer() throws Exception
   {
      if (SecurityUtility.skipAccessControl())
      {
         return MBeanServerFactory.createMBeanServer();
      }
      
      try
      {
         return (MBeanServer) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return MBeanServerFactory.createMBeanServer();
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (Exception) e.getCause();
      }   
   }
}