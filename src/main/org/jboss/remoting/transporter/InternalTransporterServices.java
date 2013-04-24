/*
 * JBoss, Home of Professional Open Source
 */
package org.jboss.remoting.transporter;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.jboss.mx.util.ObjectNameFactory;
import org.jboss.remoting.detection.Detector;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.util.SecurityUtility;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * This is a singleton that maintains internal services required for all transporter servers and clients. The
 * transporter server and client use these services to perform their functions. These "services" include an
 * MBeanServer, a Detector to detect other servers on the network and a Network Registry used to maintain a list of
 * known servers that have been detected.
 * <p/>
 * <p>End-users do not have to interface with this singleton unless they want to customize the services used by the
 * transporter framework. Call {@link #setup(MBeanServer)} to setup this singleton with an MBeanServer that is to
 * be used to house all the internal services. You can then
 * {@link #assignDetector(Detector, ObjectName, boolean) assign a detector} and/or
 * {@link #assignNetworkRegistry(NetworkRegistry, ObjectName, boolean) assign a network registry} to this singleton.
 * You can setup and assign those services all in one call via
 * {@link #setup(MBeanServer, Detector, ObjectName, NetworkRegistry, ObjectName, boolean, boolean)}.</p>
 * <p/>
 * <p>The Network Registry and Detector objects are only needed if you want the transporter server/client to use
 * their clustering feature.</p>
 * <p/>
 * <p>Note that once this singleton is setup, it cannot be setup again. You must first {@link #reset()} it in order
 * to re-setup the singleton.</p>
 *
 * @author <a href="mailto:mazz@jboss.com">John Mazzitelli</a>
 * @version $Revision: 5023 $
 */
public class InternalTransporterServices
{
   /**
    * The default detector name if one is not provided.
    */
   public static final ObjectName DEFAULT_DETECTOR_OBJECTNAME = ObjectNameFactory.create("remoting:type=Detector");

   /**
    * The default network registry name if one is not provided.
    */
   public static final ObjectName DEFAULT_NETWORKREGISTRY_OBJECTNAME = ObjectNameFactory.create("remoting:type=NetworkRegistry");

   /**
    * The singleton instance.
    */
   private static final InternalTransporterServices SINGLETON = new InternalTransporterServices();

   /**
    * Returns the singleton instance containing the services used by all {@link TransporterServer} and
    * {@link TransporterClient} instances.
    *
    * @return the singleton
    */
   public static InternalTransporterServices getInstance()
   {
      return SINGLETON;
   }

   /**
    * The MBeanServer that will house all the internal services.
    */
   private MBeanServer m_mBeanServer;

   /**
    * The detector that will be used to auto-detect other servers out on the network.
    */
   private Detector m_detector;

   /**
    * The name of the detector as it is registered in the {@link #getMBeanServer()}.
    */
   private ObjectName m_detectorName;

   /**
    * The network registry to keep the list of servers found on the network.
    */
   private NetworkRegistry m_networkRegistry;

   /**
    * The name of the network registry as it is registered in the {@link #getMBeanServer()}.
    */
   private ObjectName m_networkRegistryName;

   /**
    * The singleton's constructor.
    */
   private InternalTransporterServices()
   {
      m_mBeanServer = null;
      m_detector = null;
      m_detectorName = null;
      m_networkRegistry = null;
      m_networkRegistryName = null;
   }

   /**
    * Returns the MBeanServer that will house all the internal services.
    *
    * @return mBeanServer
    */
   public MBeanServer getMBeanServer()
   {
      return m_mBeanServer;
   }

   /**
    * Returns the detector used to auto-detect other servers on the network.
    *
    * @return detector
    */
   public Detector getDetector()
   {
      return m_detector;
   }

   /**
    * Returns the name of the {@link #getDetector() detector} used to auto-detect other servers on the network.
    *
    * @return detector name
    */
   public ObjectName getDetectorName()
   {
      return m_detectorName;
   }

   /**
    * Returns the network registry used to maintain the list of known servers.
    *
    * @return network registry
    */
   public NetworkRegistry getNetworkRegistry()
   {
      return m_networkRegistry;
   }

   /**
    * Returns the name of the {@link #getNetworkRegistry() network registry} used to maintain the list of known
    * servers.
    *
    * @return network registry name
    */
   public ObjectName getNetworkRegistryName()
   {
      return m_networkRegistryName;
   }

   /**
    * Returns <code>true</code> if the internal transporter services singleton has been setup. When this returns
    * <code>true</code>, you can be guaranteed that a non-<code>null</code> {@link #getMBeanServer() MBeanServer}
    * has been given to this singleton. A detector and/or network registry may or may not be registered - it all
    * depends on how the singleton was set up and if those services were assigned to this singleton.
    *
    * @return <code>true</code> if this singleton has already been setup; <code>false</code> if it has not been
    *         setup yet
    */
   public boolean isSetup()
   {
      synchronized (this)
      {
         return m_mBeanServer != null;
      }
   }

   /**
    * If the caller wants to reset this singleton (e.g. a new MBeanServer is to be installed), this method must be
    * called before calling one of the setup methods again. Calling this method wipes the original MBeanServer and
    * unassigns any detector or network registry references this singleton has.
    */
   public void reset()
   {
      synchronized (this)
      {
         m_mBeanServer = null;
         m_detector = null;
         m_detectorName = null;
         m_networkRegistry = null;
         m_networkRegistryName = null;
      }
   }

   /**
    * This will assign a detector to this singleton if one has not yet been setup. If there is no
    * {@link #getMBeanServer() MBean Server} yet, this will throw an exception. If a
    * {@link #getDetector() detector} has already been assigned, this will throw an exception. If <code>
    * registerDetector</code> is <code>true</code>, the given detector will be registered on the
    * {@link #getMBeanServer() MBeanServer} under the given name; if it is <code>false</code>, it will be assumed
    * the detector is already registered under that name and does not have to be registered here.
    *
    * @param detector         the detector to assign to this singleton (must not be <code>null</code>)
    * @param detectorName     the name the detector will be registered as (if <code>null</code>, a default name is
    *                         used)
    * @param registerDetector if <code>true</code>, will register the detector with the given name in the
    *                         MBeanServer
    * @throws IllegalStateException    if the MBeanServer is not setup yet or there is already a detector assigned
    * @throws IllegalArgumentException if detector is <code>null</code>
    * @throws Exception                any other exception means the detector failed to get registered in the
    *                                  MBeanServer
    */
   public void assignDetector(Detector detector,
                              ObjectName detectorName,
                              boolean registerDetector)
         throws IllegalArgumentException,
                IllegalStateException,
                Exception
   {
      synchronized (this)
      {
         if (detector == null)
         {
            throw new IllegalArgumentException("Detector was null");
         }

         if (m_mBeanServer == null)
         {
            throw new IllegalStateException("There is no MBeanServer setup yet");
         }

         if (m_detector != null)
         {
            throw new IllegalStateException("A detector is already assigned");
         }

         if (detectorName == null)
         {
            detectorName = DEFAULT_DETECTOR_OBJECTNAME;
         }

         m_detector = detector;
         m_detectorName = detectorName;

         if (registerDetector)
         {  
            registerMBean(m_mBeanServer, m_detector, m_detectorName);
         }
      }

      return;
   }

   /**
    * This will assign a network registry to this singleton if one has not yet been setup. If there is no
    * {@link #getMBeanServer() MBean Server} yet, this will throw an exception. If a
    * {@link #getNetworkRegistry() registry} has already been assigned, this will throw an exception. If <code>
    * registerRegistry</code> is <code>true</code>, the given registry will be registered on the
    * {@link #getMBeanServer() MBeanServer} with the given name; if it is <code>false</code>, it will be assumed
    * the registry is already registered under that name and does not have to be registered here.
    *
    * @param registry         the network registry to assign to this singleton (must not be <code>null</code>)
    * @param registryName     the name the registry will be registered as (if <code>null</code> a default name is
    *                         used)
    * @param registerRegistry if <code>true</code>, will register the network registry with the given name in the
    *                         MBeanServer
    * @throws IllegalStateException    if the MBeanServer is not setup yet or there is already a registry assigned
    * @throws IllegalArgumentException if registry is <code>null</code>
    * @throws Exception                any other exception means the registry failed to get registered in the
    *                                  MBeanServer
    */
   public void assignNetworkRegistry(NetworkRegistry registry,
                                     ObjectName registryName,
                                     boolean registerRegistry)
         throws IllegalArgumentException,
                IllegalStateException,
                Exception
   {
      synchronized (this)
      {
         if (registry == null)
         {
            throw new IllegalArgumentException("Registry was null");
         }

         if (m_mBeanServer == null)
         {
            throw new IllegalStateException("There is no MBeanServer setup yet");
         }

         if (m_networkRegistry != null)
         {
            throw new IllegalStateException("A network registry is already assigned");
         }

         if (registryName == null)
         {
            registryName = DEFAULT_NETWORKREGISTRY_OBJECTNAME;
         }

         m_networkRegistry = registry;
         m_networkRegistryName = registryName;

         if (registerRegistry)
         {  
            registerMBean(m_mBeanServer, m_networkRegistry, m_networkRegistryName);
         }
      }

      return;
   }

   /**
    * Sets the MBeanServer used to house the transporter services. Use this method if the transporter
    * servers/clients do not need clustering and therefore do not need to assign a detector service or the network
    * registry.
    *
    * @param mbs the MBeanServer that will house the internal services
    * @throws IllegalArgumentException if <code>mbs</code> is <code>null</code>
    * @throws IllegalStateException    if the singleton was already setup
    */
   public void setup(MBeanServer mbs)
         throws IllegalArgumentException,
                IllegalStateException
   {
      synchronized (this)
      {
         if (m_mBeanServer != null)
         {
            throw new IllegalStateException("The internal transporter services have already been setup");
         }

         if (mbs == null)
         {
            throw new IllegalArgumentException("MBeanServer must not be null");
         }

         m_mBeanServer = mbs;
         m_detector = null;
         m_detectorName = null;
         m_networkRegistry = null;
         m_networkRegistryName = null;
      }

      return;
   }

   /**
    * Sets the MBeanServer used to house the transporter services, assigns the detector used to auto-detect
    * other servers on the network and assigns the network registry that maintains the list of known servers on the
    * network. If the detector or registry already exist in the MBeanServer and their respective <code>
    * registerXXX</code> parameter is <code>true</code>, an exception is thrown.
    *
    * @param mbs              the MBeanServer that will house the internal services (must not be <code>
    *                         null</code>)
    * @param detector         the detector that will listen for other servers on the network
    * @param detectorName     the name of the detector as it is or will be registered in the MBeanServer (if
    *                         <code>null</code>, a default name will be used)
    * @param registry         the network registry that will maintain the list of known servers on the network
    * @param registryName     the name of the network registry as it is or will be registered in the MBeanServer
    *                         (if <code>null</code>, a default name will be used)
    * @param registerDetector if <code>true</code>, will register the detector with the given name
    * @param registerRegistry if <code>true</code>, will register the network registry with the given name
    * @throws IllegalStateException    if the singleton was already setup
    * @throws IllegalArgumentException if the MBeanServer is <code>null</code> or detector is <code>null</code> but
    *                                  its name is not or registry is <code>null</code> but its name is not
    * @throws Exception                if this method failed to register the detector or registry
    * @see #assignDetector(Detector, ObjectName, boolean)
    * @see #assignNetworkRegistry(NetworkRegistry, ObjectName, boolean)
    */
   public void setup(MBeanServer mbs,
                     Detector detector,
                     ObjectName detectorName,
                     NetworkRegistry registry,
                     ObjectName registryName,
                     boolean registerDetector,
                     boolean registerRegistry)
         throws Exception
   {
      synchronized (this)
      {
         setup(mbs);

         if (detector != null)
         {
            assignDetector(detector, detectorName, registerDetector);
         }

         if (registry != null)
         {
            assignNetworkRegistry(registry, registryName, registerRegistry);
         }
      }

      return;
   }
   
   static private void registerMBean(final MBeanServer server, final Object o, final ObjectName name)
   throws Exception
   {
      if (SecurityUtility.skipAccessControl())
      {
         server.registerMBean(o, name);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               server.registerMBean(o, name);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (Exception) e.getCause();
      }
   }
}