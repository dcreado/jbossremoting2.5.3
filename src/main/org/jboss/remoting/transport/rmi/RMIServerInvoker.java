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

import org.jboss.remoting.Home;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.Marshaller;
import org.jboss.remoting.marshal.MarshallerDecorator;
import org.jboss.remoting.marshal.UnMarshaller;
import org.jboss.remoting.marshal.UnMarshallerDecorator;
import org.jboss.remoting.marshal.VersionedMarshaller;
import org.jboss.remoting.marshal.VersionedUnMarshaller;
import org.jboss.remoting.marshal.rmi.RMIMarshaller;
import org.jboss.remoting.marshal.rmi.RMIUnMarshaller;
import org.jboss.remoting.marshal.serializable.SerializableMarshaller;
import org.jboss.remoting.serialization.SerializationManager;
import org.jboss.remoting.serialization.SerializationStreamFactory;
import org.jboss.remoting.util.SecurityUtility;
import org.jboss.serial.io.JBossObjectOutputStream;
import org.jboss.util.propertyeditor.PropertyEditors;
import org.jboss.logging.Logger;

import javax.net.SocketFactory;

import java.beans.IntrospectionException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * RMIServerInvoker
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 * @version $Revision: 5255 $
 */
public class RMIServerInvoker extends ServerInvoker implements RMIServerInvokerInf, Cloneable
{
   public static String RMI_ONEWAY_MARSHALLING = "rmiOnewayMarshalling";
   
   private static final Logger log = Logger.getLogger(RMIServerInvoker.class);

   protected boolean isPrimaryServer = true;
   protected Set secondaryServers = new HashSet();
   protected boolean rmiOnewayMarshalling;
   
   private Remote stub;
   private RemotingRMIClientSocketFactory csf;
   

   /**
    * Default for how many connections are queued.  Value is 200.
    */
   public static final int BACKLOG_DEFAULT = 200;

   /**
    * Default port on which rmi registry will be started.  Value is 3455.
    */
   public static final int DEFAULT_REGISTRY_PORT = 3455;

   /**
    * Key for port on which rmi registry should be started on.
    */
   public static final String REGISTRY_PORT_KEY = "registryPort";

   private Marshaller marshaller = null;
   private UnMarshaller unmarshaller = null;

   public RMIServerInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public void start() throws IOException
   {
      super.start();

      if (isPrimaryServer)
      {
         List connectHomes = getConnectHomes();
         List homes = getHomes();
         
         if (connectHomes.size() != homes.size())
            throw new IOException("number of connect homes and bind homes must match in RMI transport");
         
         Home bindHome = (Home) homes.get(0);
         Home connectHome = (Home) connectHomes.get(0);
         initRMI(bindHome, connectHome);

         for (int i = 1; i < homes.size(); i++)
         {
            bindHome = (Home) homes.get(i);
            connectHome = (Home) connectHomes.get(i);
            RMIServerInvoker copy = copy();
            secondaryServers.add(copy);
            copy.initRMI(bindHome, connectHome);
         }
      }
   }
   
   protected void setup() throws Exception
   {
      Properties props = new Properties();
      props.putAll(getConfiguration());
      mapJavaBeanProperties(RMIServerInvoker.this, props, false);
      super.setup();
   }
   
   protected RMIServerInvoker copy() throws IOException
   {
      Object o = null;
      try
      {
         o = clone();
      }
      catch (CloneNotSupportedException e)
      {
         log.error("This should not happen", e);
         return this;
      }
      RMIServerInvoker server = (RMIServerInvoker) o;
      server.locator = locator;
      server.locator = new InvokerLocator(locator.getLocatorURI());
      server.locator.setHomeInUse(locator.getHomeInUse());
      server.isPrimaryServer = false;
      server.start();
      return server;
   }
   
   protected void initRMI(Home bindHome, Home connectHome) throws IOException
   {
      Registry registry = null;
      try
      {
         registry = getRegistry();
      }
      catch (Exception e)
      {
         throw new IOException(e.getMessage());
      }

      String bindHost = bindHome.host;
      int bindPort = bindHome.port;
      String clientConnectHost = connectHome.host;

      if(clientConnectHost == null)
      {
         clientConnectHost = bindHost;
      }

      locator.setHomeInUse(bindHome);
      RMIServerSocketFactory ssf = new RemotingRMIServerSocketFactory(getServerSocketFactory(), BACKLOG_DEFAULT, bindHost, getTimeout());
      csf = getRMIClientSocketFactory(clientConnectHost);
      stub = exportObject(this, bindPort, csf, ssf);

      log.debug("Binding server to \"remoting/RMIServerInvoker/" + bindPort + "\" in registry");
      rebind(registry, "remoting/RMIServerInvoker/" + bindPort, this);
      ClassLoader classLoader = getClassLoader(RMIServerInvoker.class);
      Map map = passConfigMapToMarshalFactory ? configuration : null;
      unmarshaller = MarshalFactory.getUnMarshaller(getLocator(), classLoader, map);
      marshaller = MarshalFactory.getMarshaller(getLocator(), classLoader, map);
   }


   protected RemotingRMIClientSocketFactory getRMIClientSocketFactory(String clientConnectHost)
   {
      // Remove server side socket creation listeners.
      HashMap remoteConfig = new HashMap(configuration);
      remoteConfig.remove(Remoting.CUSTOM_SERVER_SOCKET_FACTORY);
      remoteConfig.remove(Remoting.SOCKET_CREATION_CLIENT_LISTENER);
      remoteConfig.remove(Remoting.SOCKET_CREATION_SERVER_LISTENER);
      return new RemotingRMIClientSocketFactory(locator, clientConnectHost, getTimeout(), remoteConfig);
   }
   
   protected SocketFactory getDefaultSocketFactory()
   {
//      return SocketFactory.getDefault();
      /**
       * Returning null because by default, this socket factory
       * will be need to be serialized when exported.  Since the
       * default factory implementation returned from SocketFactory.getDefault()
       * is not serializable, it will not work.  Therefore, if return null,
       * will delay the creation of the socket factory until the RMIClientSocketFactory is
       * exported.
       */
      return null;
   }


   public RMIServerInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
   }

   private Registry getRegistry() throws Exception
   {
      Registry registry = null;

      int port = DEFAULT_REGISTRY_PORT;

      // See if locator contains a specific registry port
      Map params = getConfiguration();
      if(params != null)
      {
         String value = (String) params.get(REGISTRY_PORT_KEY);
         if(value != null)
         {
            try
            {
               port = Integer.parseInt(value);
               log.debug("Using port " + port + " for rmi registry.");
            }
            catch(NumberFormatException e)
            {
               throw new Exception("Can not set the RMIServerInvoker RMI registry to port " + value + ".  This is not a valid port number.");
            }
         }
      }

      try
      {
         log.debug("Creating registry for " + port);

         registry = createRegistry(port);
      }
      catch(ExportException exportEx)
      {
         log.debug("Locating registry for " + port);

         // Probably means that the registry already exists, so just get it.
         registry = getRegistry(port);
      }
      if(log.isTraceEnabled())
      {
         log.trace("Got registry: " + registry);
      }
      return registry;
   }

   protected String getDefaultDataType()
   {
      return SerializableMarshaller.DATATYPE;
   }

   /**
    * destroy the RMI Server Invoker, which will unexport the RMI server
    */
   public void destroy()
   {
      super.destroy();
      try
      {
         try
         {
            log.debug("locator: " + locator + ", home: " + locator.getHomeInUse());
            log.debug(this + " primary: " + isPrimaryServer + " unbinding " + "remoting/RMIServerInvoker/" + locator.getPort() + " from registry");
            Registry registry = getRegistry();
            unbind(registry, "remoting/RMIServerInvoker/" + locator.getPort());
            log.debug("unbound " + "remoting/RMIServerInvoker/" + locator.getPort() + " from registry");
         }
         catch(Exception e)
         {
            if ("Finalizer".equalsIgnoreCase(Thread.currentThread().getName()))
               log.debug("thread: " + Thread.currentThread().getName() + " Error unbinding RMIServerInvoker from RMI registry.", e);
            else
               log.error("thread: " + Thread.currentThread().getName() + " Error unbinding RMIServerInvoker from RMI registry.", e);
         }

         UnicastRemoteObject.unexportObject(this, true);

      }
      catch(java.rmi.NoSuchObjectException e)
      {

      }
      
      if (csf != null)
         csf.clear();
      
      if (isPrimaryServer)
      {
         Iterator it = secondaryServers.iterator();
         while (it.hasNext())
         {
            RMIServerInvoker server = (RMIServerInvoker) it.next();
            it.remove();
            server.destroy();
         }
      }
   }

   protected void finalize() throws Throwable
   {
      destroy();
      super.finalize();
   }

   /**
    * returns true if the transport is bi-directional in nature, for example,
    * SOAP in unidirectional and SOCKETs are bi-directional (unless behind a firewall
    * for example).
    */
   public boolean isTransportBiDirectional()
   {
      return true;
   }

   public final Remote getStub()
   {
      return stub;
   }

   public Object transport(Object invocation)
         throws RemoteException, IOException
   {

      Object payload = invocation;
      if(unmarshaller != null && !(unmarshaller instanceof RMIUnMarshaller))
      {
         if(unmarshaller instanceof UnMarshallerDecorator)
         {
            payload = ((UnMarshallerDecorator) unmarshaller).removeDecoration(payload);
         }
         else
         {
            ByteArrayInputStream is = null;
            if (rmiOnewayMarshalling)
            {
               // Legacy treatment, pre 2.4.0
               ByteArrayOutputStream baos = new ByteArrayOutputStream();
               SerializationManager manager = SerializationStreamFactory.getManagerInstance(getSerializationType());
               ObjectOutputStream oos = manager.createOutput(baos);
               writeObject(oos, payload);
               oos.flush();
               oos.close();
               is = new ByteArrayInputStream(baos.toByteArray());
            }
            else
            {
               is = new ByteArrayInputStream((byte[]) payload);
            }

            try
            {
               if (unmarshaller instanceof VersionedUnMarshaller)
               {
                  payload = ((VersionedUnMarshaller) unmarshaller).read(is, null, getVersion());
                  is.close();
               }
               else
               {
                  payload = unmarshaller.read(is, null);
                  is.close();
               }
            }
            catch(ClassNotFoundException e)
            {
               log.debug("Could not unmarshall invocation request" + payload, e);
               throw new IOException(e.getMessage());
            }
         }
      }

      if (payload instanceof InvocationRequest)
      {
         InvocationRequest ir = (InvocationRequest) payload;
         Map metadata = ir.getRequestPayload();
         if (metadata == null)
         {
            metadata = new HashMap();
            ir.setRequestPayload(metadata);
         }
         try
         {
            String clientHost = RemoteServer.getClientHost();
            InetAddress clientAddress = getAddressByName(clientHost);
            metadata.put(Remoting.CLIENT_ADDRESS, clientAddress);
         }
         catch (ServerNotActiveException e)
         {
            throw new RemoteException(e.getMessage());
         }
      }
      Object response = invoke(payload);

      if(marshaller != null && !(marshaller instanceof RMIMarshaller) && !rmiOnewayMarshalling)
      {
         if(marshaller instanceof MarshallerDecorator)
         {
            response = ((MarshallerDecorator) marshaller).addDecoration(response);
         }
         else
         {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            if (marshaller instanceof VersionedMarshaller)
               ((VersionedMarshaller) marshaller).write(response, byteOut, getVersion());
            else
               marshaller.write(response, byteOut);
            
            byteOut.close();
            response = byteOut.toByteArray();
         }
      }
      return response;
   }

   public boolean isRmiOnewayMarshalling()
   {
      return rmiOnewayMarshalling;
   }

   public void setRmiOnewayMarshalling(boolean rmiOnewayMarshalling)
   {
      this.rmiOnewayMarshalling = rmiOnewayMarshalling;
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
   
   static private void writeObject(final ObjectOutputStream oos, final Object o)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl() || !(oos instanceof JBossObjectOutputStream))
      {
         oos.writeObject(o);
         return;
      }

      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               oos.writeObject(o);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof IOException)
            throw (IOException) cause;
         else
            throw (RuntimeException) cause;
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

   static private Registry createRegistry(final int port) throws RemoteException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return LocateRegistry.createRegistry(port);
      }
      
      try
      {
         return (Registry) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws RemoteException
            {
               return LocateRegistry.createRegistry(port);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RemoteException) e.getCause();
      } 
   }
   
   static private Remote exportObject(final Remote object,
                                     final int port,
                                     final RMIClientSocketFactory csf,
                                     final RMIServerSocketFactory ssf)
   throws RemoteException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return UnicastRemoteObject.exportObject(object, port, csf, ssf);
      }
      
      try
      {
         return (Remote) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws RemoteException
            {
               return UnicastRemoteObject.exportObject(object, port, csf, ssf);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RemoteException) e.getCause();
      }
   }
   
   static private Registry getRegistry(final int port) throws RemoteException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return LocateRegistry.getRegistry(port);
      }
      
      try
      {
         return (Registry) AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws RemoteException
            {
               return LocateRegistry.getRegistry(port);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RemoteException) e.getCause();
      } 
   }
   
   static private void rebind(final Registry registry, final String name, final Remote object)
   throws IOException
   {
      if (SecurityUtility.skipAccessControl())
      {
         registry.rebind(name, object);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws IOException
            {
               registry.rebind(name, object);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (IOException) e.getCause();
      }
   }
   
   static private void unbind(final Registry registry, final String name)
   throws  AccessException, RemoteException, NotBoundException
   {
      if (SecurityUtility.skipAccessControl())
      {
         registry.unbind(name);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws AccessException, RemoteException, NotBoundException
            {
               registry.unbind(name);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof AccessException)
            throw (AccessException) cause;
         else if (cause instanceof RemoteException)
            throw (RemoteException) cause;
         else
            throw (NotBoundException) cause;
      }
   }
}
