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

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.util.SecurityUtility;
import org.w3c.dom.Element;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The remoting server to expose the target POJO.  This should be called on as a factory via
 * static methods.
 *
 * @author <a href="mailto:tom.elrod@jboss.com">Tom Elrod</a>
 */
public class TransporterServer
{
   private Connector connector = null;

   /**
    * Creates a remoting server using the provided locator and subsytem and creating a TransporterHandler which
    * takes the specified target object.
    *
    * @param locator
    * @param target
    * @param subsystem
    * @throws Exception
    */
   public TransporterServer(InvokerLocator locator, Object target, String subsystem) throws Exception
   {
      connector = getConnector(locator, null, null);
      ServerInvocationHandler handler = new TransporterHandler(target);
      if (subsystem != null)
      {
         connector.addInvocationHandler(subsystem.toUpperCase(), handler);
      }
      else
      {
         addInterfaceSubsystems(connector, handler, target);
      }
   }

   /**
    * Creates a remoting server using the provided locator and subsytem and creating a TransporterHandler which
    * takes the specified target object.
    *
    * @param xmlConfig
    * @param target
    * @param subsystem
    * @throws Exception
    */
   public TransporterServer(Element xmlConfig, Object target, String subsystem) throws Exception
   {
      connector = getConnector(null, null, xmlConfig);
      ServerInvocationHandler handler = new TransporterHandler(target);
      if (subsystem != null)
      {
         connector.addInvocationHandler(subsystem.toUpperCase(), handler);
      }
      else
      {
         addInterfaceSubsystems(connector, handler, target);
      }
   }

   /**
    * Creates a remoting server using the provided locator and subsytem and creating a TransporterHandler which
    * takes the specified target object.
    *
    * @param locator
    * @param target
    * @param subsystem
    * @param config    - configuration data for Connector
    * @throws Exception
    */
   public TransporterServer(InvokerLocator locator, Object target, String subsystem, Map config) throws Exception
   {
      connector = getConnector(locator, config, null);
      ServerInvocationHandler handler = new TransporterHandler(target);
      if (subsystem != null)
      {
         connector.addInvocationHandler(subsystem.toUpperCase(), handler);
      }
      else
      {
         addInterfaceSubsystems(connector, handler, target);
      }
   }

   private void addInterfaceSubsystems(Connector connector, ServerInvocationHandler handler, Object target) throws Exception
   {
      Class targetClass = target.getClass();

      //first have to build list of interface names
      List interfaceNames = new ArrayList();
      populateInterfaceNames(interfaceNames, targetClass);

      for (int i = 0; i < interfaceNames.size(); i++)
      {
         String interfaceClassName = (String) interfaceNames.get(i);
         connector.addInvocationHandler(interfaceClassName.toUpperCase(), handler);
      }
   }

   private void populateInterfaceNames(List interfaceNames, Class targetClass)
   {
      Class[] interfaces = targetClass.getInterfaces();
      if (interfaces != null)
      {
         for (int x = 0; x < interfaces.length; x++)
         {
            interfaceNames.add(interfaces[x].getName());
            populateInterfaceNames(interfaceNames, interfaces[x]);
         }
      }
   }

   /**
    * Returns the connector that this transporter server will use.  Subclasses are free to override this method in order
    * to create a more customized connector.
    *
    * @param locator
    * @param config    configuration data for connector
    * @param xmlConfig configuration data for connector (in xml form)
    * @return the connector to be used by this transporter server
    * @throws Exception
    */
   protected Connector getConnector(InvokerLocator locator, Map config, Element xmlConfig)
         throws Exception
   {
      Connector c = new Connector(locator, config);
      if (xmlConfig != null)
      {
         c.setConfiguration(xmlConfig);
      }
      c.create();

      return c;
   }

   /**
    * Adds a transporter handler to receive remote invocations on the target object passed.
    *
    * @param target         the target implementation to call on
    * @param proxyclassname the fully qualified classname of the interface that clients will use to call on
    */
   public void addHandler(Object target, String proxyclassname) throws Exception
   {
      if (connector != null)
      {
         connector.addInvocationHandler(proxyclassname, new TransporterHandler(target));
      }
      else
      {
         throw new Exception("Can not add handler to transporter server as has not be initialized yet.");
      }
   }

   /**
    * Starts the remoting server.  This is called automatically upon any of the static createTransporterServer() methods.
    *
    * @throws Exception
    */
   public void start() throws Exception
   {
      connector.start();
   }

   /**
    * Stops the remoting server.  This must be called when no longer want to expose the target POJO for remote
    * method calls.
    */
   public void stop()
   {
      connector.stop();
   }

   /**
    * Creates a MBeanServer and MulticastDetector to start publishing detection messages so
    * other detectors will be aware this server is available.
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
         services.setup(server, detector, null, null, null, true, false);
         detector.start();
      }
      else if (services.getDetector() == null)
      {
         // the internal services singleton is already setup, make sure it has a detector because we need it
         MulticastDetector detector = new MulticastDetector();
         services.assignDetector(detector, null, true);
         detector.start();
      }

      return;
   }

   /**
    * Creates a remoting server based on given locator.  Will convert any remote invocation requests into
    * method calls on the given target object.Note: the TransporterServer instance returned will be a live (started)
    * instance.
    *
    * Once the TransporterServer has been returned, it will have already been started automatically, so is a live server
    * ready to receive requests.
    *
    * @param locator     - specifies what transport, host and port binding, etc. to use by the remoting server.
    * @param target      - the target POJO to receive the method call upon getting remote invocation requests.
    * @param subsystem   - the name under which to register the handler within the remoting server.  <b>This must be
    *                    the fully qualified name of the interface for clients to use a the remote proxy to the target POJO.  Otherwise,
    *                    clustering will not work, as this is the value used to identifiy remote POJOs on the client side.</b>  If not clustered,
    *                    this is not as critical, and simply use the fully qualified class name of the POJO if desired.
    * @param isClustered - true indicates that would like this server to be considered available for
    *                    failover from clients calling on the same interface as exposed by the subsystem value.  False will only allow
    *                    those client that explicitly targeting this server to make calls on it.
    * @return TransporterServer.  Note, it will already be started upon return.
    * @throws Exception
    */
   public static TransporterServer createTransporterServer(InvokerLocator locator, Object target,
                                                           String subsystem, boolean isClustered) throws Exception
   {
      return createTransporterServer(locator, target, subsystem, null, isClustered);
   }

   /**
    * Creates a remoting server based on given locator.  Will convert any remote invocation requests into
    * method calls on the given target object.Note: the TransporterServer instance returned will be a live (started)
    * instance.
    *
    * Once the TransporterServer has been returned, it will have already been started automatically, so is a live server
    * ready to receive requests.
    *
    * @param locator     - specifies what transport, host and port binding, etc. to use by the remoting server.
    * @param target      - the target POJO to receive the method call upon getting remote invocation requests.
    * @param subsystem   - the name under which to register the handler within the remoting server.  <b>This must be
    *                    the fully qualified name of the interface for clients to use a the remote proxy to the target POJO.  Otherwise,
    *                    clustering will not work, as this is the value used to identifiy remote POJOs on the client side.</b>  If not clustered,
    *                    this is not as critical, and simply use the fully qualified class name of the POJO if desired.
    * @param config      - the configuration to be used in setting up the Connector.
    * @param isClustered - true indicates that would like this server to be considered available for
    *                    failover from clients calling on the same interface as exposed by the subsystem value.  False will only allow
    *                    those client that explicitly targeting this server to make calls on it.
    * @return TransporterServer.  Note, it will already be started upon return.
    * @throws Exception
    */
   public static TransporterServer createTransporterServer(InvokerLocator locator, Object target,
                                                           String subsystem, Map config, boolean isClustered) throws Exception
   {
      if (isClustered && (InternalTransporterServices.getInstance().getDetector() == null))
      {
         setupDetector();
      }

      TransporterServer server = new TransporterServer(locator, target, subsystem, config);
      server.start();
      return server;
   }

   /**
    * Creates a remoting server based on given locator.  Will convert any remote invocation requests into
    * method calls on the given target object.Note: the TransporterServer instance returned will be a live (started)
    * instance.
    *
    * Once the TransporterServer has been returned, it will have already been started automatically, so is a live server
    * ready to receive requests.
    *
    * @param locatorURI  - specifies what transport, host and port binding, etc. to use by the remoting server.
    * @param target      - the target POJO to receive the method call upon getting remote invocation requests.
    * @param subsystem   - the name under which to register the handler within the remoting server.  <b>This must be
    *                    the fully qualified name of the interface for clients to use a the remote proxy to the target POJO.  Otherwise,
    *                    clustering will not work, as this is the value used to identifiy remote POJOs on the client side.</b>  If not clustered,
    *                    this is not as critical, and simply use the fully qualified class name of the POJO if desired.
    * @param isClustered - true indicates that would like this server to be considered available for
    *                    failover from clients calling on the same interface as exposed by the subsystem value.  False will only allow
    *                    those client that explicitly targeting this server to make calls on it.
    * @return TransporterServer.  Note, it will already be started upon return.
    * @throws Exception
    */
   public static TransporterServer createTransporterServer(String locatorURI, Object target,
                                                           String subsystem, boolean isClustered) throws Exception
   {
      return createTransporterServer(new InvokerLocator(locatorURI), target, subsystem, null, isClustered);
   }

   /**
    * Creates a remoting server based on given locator.  Will convert any remote invocation requests into
    * method calls on the given target object.Note: the TransporterServer instance returned will be a live (started)
    * instance.
    *
    * Once the TransporterServer has been returned, it will have already been started automatically, so is a live server
    * ready to receive requests.
    *
    * @param locatorURI  - specifies what transport, host and port binding, etc. to use by the remoting server.
    * @param target      - the target POJO to receive the method call upon getting remote invocation requests.
    * @param subsystem   - the name under which to register the handler within the remoting server.  <b>This must be
    *                    the fully qualified name of the interface for clients to use a the remote proxy to the target POJO.  Otherwise,
    *                    clustering will not work, as this is the value used to identifiy remote POJOs on the client side.</b>  If not clustered,
    *                    this is not as critical, and simply use the fully qualified class name of the POJO if desired.
    * @param config      - the configuration data for the Connector.
    * @param isClustered - true indicates that would like this server to be considered available for
    *                    failover from clients calling on the same interface as exposed by the subsystem value.  False will only allow
    *                    those client that explicitly targeting this server to make calls on it.
    * @return TransporterServer.  Note, it will already be started upon return.
    * @throws Exception
    */
   public static TransporterServer createTransporterServer(String locatorURI, Object target,
                                                           String subsystem, Map config, boolean isClustered) throws Exception
   {
      return createTransporterServer(new InvokerLocator(locatorURI), target, subsystem, config, isClustered);
   }

   /**
    * Creates a remoting server based on given locator.  Will convert any remote invocation requests into
    * method calls on the given target object.  Note: the TransporterServer instance returned will be a live (started)
    * instance.
    *
    * Once the TransporterServer has been returned, it will have already been started automatically, so is a live server
    * ready to receive requests.
    *
    * @param xmlconfig   - specifies config for Connector
    * @param target      - the target POJO to receive the method call upon getting remote invocation requests.
    * @param subsystem   - the name under which to register the handler within the remoting server.  <b>This must be
    *                    the fully qualified name of the interface for clients to use a the remote proxy to the target POJO.  Otherwise,
    *                    clustering will not work, as this is the value used to identifiy remote POJOs on the client side.</b>  If not clustered,
    *                    this is not as critical, and simply use the fully qualified class name of the POJO if desired.
    * @param isClustered - true indicates that would like this server to be considered available for
    *                    failover from clients calling on the same interface as exposed by the subsystem value.  False will only allow
    *                    those client that explicitly targeting this server to make calls on it.
    * @return TransporterServer.  Note, it will already be started upon return.
    * @throws Exception
    */
   public static TransporterServer createTransporterServer(Element xmlconfig, Object target,
                                                           String subsystem, boolean isClustered) throws Exception
   {
      if (isClustered && (InternalTransporterServices.getInstance().getDetector() == null))
      {
         setupDetector();
      }

      TransporterServer server = new TransporterServer(xmlconfig, target, subsystem);
      server.start();
      return server;
   }

   /**
    * Creates a remoting server based on given locator.  Will convert any remote invocation requests into
    * method calls on the given target object.Note: the TransporterServer instance returned will be a live (started)
    * instance.
    *
    * Once the TransporterServer has been returned, it will have already been started automatically, so is a live server
    * ready to receive requests.
    *
    * @param locator   - specifies what transport, host and port binding, etc. to use by the remoting server.
    * @param target    - the target POJO to receive the method call upon getting remote invocation requests.
    * @param subsystem - the name under which to register the handler within the remoting server.  Can
    *                  simply use the fully qualified class name of the POJO if desired.
    * @return TransporterServer.  Note, it will already be started upon return.
    * @throws Exception
    */
   public static TransporterServer createTransporterServer(InvokerLocator locator, Object target, String subsystem) throws Exception
   {
      return createTransporterServer(locator, target, subsystem, false);
   }

   /**
    * Creates a remoting server based on given locator.  Will convert any remote invocation requests into
    * method calls on the given target object.Note: the TransporterServer instance returned will be a live (started)
    * instance.
    *
    * Once the TransporterServer has been returned, it will have already been started automatically, so is a live server
    * ready to receive requests.
    *
    * @param locator - specifies what transport, host and port binding, etc. to use by the remoting server.
    * @param target  - the target POJO to receive the method call upon getting remote invocation requests.
    * @return TransporterServer.  Note, it will already be started upon return.
    * @throws Exception
    */
   public static TransporterServer createTransporterServer(InvokerLocator locator, Object target) throws Exception
   {
      return createTransporterServer(locator, target, false);
   }

   /**
    * Creates a remoting server based on given locator.  Will convert any remote invocation requests into
    * method calls on the given target object.Note: the TransporterServer instance returned will be a live (started)
    * instance.
    *
    * Once the TransporterServer has been returned, it will have already been started automatically, so is a live server
    * ready to receive requests.
    *
    * @param locator     - specifies what transport, host and port binding, etc. to use by the remoting server.
    * @param target      - the target POJO to receive the method call upon getting remote invocation requests.
    * @param isClustered - indicates if want automatic failover on calls to remote servers.
    * @return TransporterServer.  Note, it will already be started upon return.
    * @throws Exception
    */
   public static TransporterServer createTransporterServer(InvokerLocator locator, Object target, boolean isClustered) throws Exception
   {
      if (isClustered && (InternalTransporterServices.getInstance().getDetector() == null))
      {
         setupDetector();
      }

      TransporterServer server = new TransporterServer(locator, target, null, null);
      server.start();
      return server;

   }

   /**
    * Creates a remoting server based on given locator.  Will convert any remote invocation requests into
    * method calls on the given target object.Note: the TransporterServer instance returned will be a live (started)
    * instance.
    *
    * Once the TransporterServer has been returned, it will have already been started automatically, so is a live server
    * ready to receive requests.
    *
    * @param locator - specifies what transport, host and port binding, etc. to use by the remoting server.
    * @param target  - the target POJO to receive the method call upon getting remote invocation requests.
    * @return TransporterServer.  Note, it will already be started upon return.
    * @throws Exception
    */
   public static TransporterServer createTransporterServer(String locator, Object target) throws Exception
   {
      return createTransporterServer(new InvokerLocator(locator), target, false);
   }

   /**
    * Creates a remoting server based on given locator.  Will convert any remote invocation requests into
    * method calls on the given target object.Note: the TransporterServer instance returned will be a live (started)
    * instance.
    *
    * Once the TransporterServer has been returned, it will have already been started automatically, so is a live server
    * ready to receive requests.
    *
    * @param locator     - specifies what transport, host and port binding, etc. to use by the remoting server.
    * @param target      - the target POJO to receive the method call upon getting remote invocation requests.
    * @param isClustered - indicates if want automatic failover on calls to remote servers.
    * @return TransporterServer.  Note, it will already be started upon return.
    * @throws Exception
    */
   public static TransporterServer createTransporterServer(String locator, Object target, boolean isClustered) throws Exception
   {
      return createTransporterServer(new InvokerLocator(locator), target, isClustered);
   }

   /**
    * Creates a remoting server based on given locator.  Will convert any remote invocation requests into
    * method calls on the given target object.Note: the TransporterServer instance returned will be a live (started)
    * instance.
    *
    * Once the TransporterServer has been returned, it will have already been started automatically, so is a live server
    * ready to receive requests.
    *
    * @param locatorURI - specifies what transport, host and port binding, etc. to use by the remoting server.
    * @param target     - the target POJO to receive the method call upon getting remote invocation requests.
    * @param subsystem  - the name under which to register the handler within the remoting server.  Can
    *                   simply use the fully qualified class name of the POJO if desired.
    * @return TransporterServer.  Note, it will already be started upon return.
    * @throws Exception
    */
   public static TransporterServer createTransporterServer(String locatorURI, Object target, String subsystem) throws Exception
   {
      return createTransporterServer(new InvokerLocator(locatorURI), target, subsystem, false);
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