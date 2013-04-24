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

package org.jboss.remoting.transport;

import org.jboss.logging.Logger;
import org.jboss.remoting.ConnectionListener;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.Remoting;
import org.jboss.remoting.ServerConfiguration;
import org.jboss.remoting.ServerInvocationHandler;
import org.jboss.remoting.ServerInvocationHandlerWrapper;
import org.jboss.remoting.ServerInvoker;
import org.jboss.remoting.marshal.MarshalFactory;
import org.jboss.remoting.marshal.MarshallLoaderFactory;
import org.jboss.remoting.serialization.ClassLoaderUtility;
import org.jboss.remoting.util.SecurityUtility;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Connector is an implementation of the ConnectorMBean interface.
 * <p/>
 * The Connector is root component for the remoting server.  It binds the server transport, marshaller,
 * and handler together to form the remoting server instance.
 * <p/>
 * A transport connector is configured via *-service.xml such as:
 * <code>
 * <?xml version="1.0" encoding="UTF-8"?>
 * <!DOCTYPE server>
 * <server>
 * <!-- NOTE: set this up to the path where your libraries are -->
 * <classpath codebase="lib" archives="*"/>
 * <mbean code="org.jboss.remoting.network.NetworkRegistry"
 * name="jboss.remoting:service=NetworkRegistry"/>
 * <p/>
 * <mbean code="org.jboss.remoting.transport.Connector"
 * name="jboss.remoting:service=Connector,transport=Socket"
 * display-name="Socket transport Connector">
 * <p/>
 * <!-- Can either just specify the InvokerLocator attribute and not the invoker element in the -->
 * <!-- Configuration attribute, or do the full invoker configuration in the in invoker element -->
 * <!-- of the Configuration attribute. -->
 * <!-- Remember that if you do use more than one param on the uri, will have to include as a CDATA, -->
 * <!-- otherwise, parser will complain. -->
 * <!-- <attribute name="InvokerLocator"><![CDATA[socket://${jboss.bind.address}:8084/?enableTcpNoDelay=false&clientMaxPoolSize=30]]></attribute>-->
 * <attribute name="Configuration">
 * <config>
 * <invoker transport="socket">
 * <attribute name="numAcceptThreads">1</attribute>
 * <attribute name="maxPoolSize">303</attribute>
 * <attribute name="clientMaxPoolSize" isParam="true">304</attribute>
 * <attribute name="socketTimeout">60000</attribute>
 * <attribute name="serverBindAddress">${jboss.bind.address}</attribute>
 * <attribute name="serverBindPort">6666</attribute>
 * <!--  <attribute name="clientConnectAddress">216.23.33.2</attribute> -->
 * <!--  <attribute name="clientConnectPort">7777</attribute> -->
 * <attribute name="enableTcpNoDelay" isParam="true">false</attribute>
 * <attribute name="backlog">200</attribute>
 * </invoker>
 * <handlers>
 * <handler subsystem="mock">org.jboss.remoting.transport.mock.MockServerInvocationHandler</handler>
 * </handlers>
 * </config>
 * </attribute>
 * </mbean>
 * <p/>
 * <mbean code="org.jboss.remoting.detection.multicast.MulticastDetector"
 * name="jboss.remoting:service=Detector,transport=multicast">
 * <!-- you can specifically bind the detector to a specific IP address here -->
 * <!--  <attribute name="BindAddress">${jboss.bind.address}</attribute> -->
 * <attribute name="Port">2410</attribute>
 * </mbean>
 * <p/>
 * </server>
 * </code>
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @author <a href="mailto:adrian.brock@happeningtimes.com">Adrian Brock</a>
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:juha@jboss.org">Juha Lindfors</a>
 * @author <a href="mailto:tom@jboss.org">Tom Elrod</a>
 * @version $Revision: 5888 $
 * @jmx.mbean description = "An MBean wrapper around a ServerInvoker."
 * @jboss.xmbean
 */
public class Connector implements MBeanRegistration, ConnectorMBean
{
   protected ServerInvoker invoker;

   private String locatorURI;

   private Element xml;
   private ServerConfiguration serverConfiguration;
   
   private Map configuration = new HashMap();

   private MBeanServer server;

   private ServerSocketFactory svrSocketFactory;
   private SocketFactory socketFactory;

   private Connector marshallerLoaderConnector = null;
   private boolean isMarshallerLoader = false;
   private List remoteClassLoaders;

   private boolean isStarted = false;
   private boolean isCreated = false;

   protected final Logger log = Logger.getLogger(getClass());

   /**
    * Empty constructor.
    */
   public Connector()
   {

   }

   /**
    * Creates Connector with specified locator.
    *
    * @param locatorURI
    */
   public Connector(String locatorURI)
   {
      this.locatorURI = locatorURI;
   }

   /**
    * Creates Connector with specified locator.
    *
    * @param locator
    */
   public Connector(InvokerLocator locator)
   {
      if (locator != null)
      {
         this.locatorURI = locator.getLocatorURI();
      }
   }

   /**
    * Constructs connector and populates configuration information.
    *
    * @param configuration
    */
   public Connector(Map configuration)
   {
      this.configuration.putAll(configuration);
   }

   /**
    * Constructs connector for given locator and configuration.
    *
    * @param locatorURI
    * @param configuration
    */
   public Connector(String locatorURI, Map configuration)
   {
      this.locatorURI = locatorURI;
      this.configuration.putAll(configuration);
   }

   /**
    * Constructs connector for given locator and configuration.
    *
    * @param locator
    * @param configuration
    */
   public Connector(InvokerLocator locator, Map configuration)
   {
      if (locator != null)
      {
         this.locatorURI = locator.getLocatorURI();
      }

      if (configuration != null)
      {
         this.configuration.putAll(configuration);
      }
   }

   protected Connector(boolean isMarshallerConnector)
   {
      this();
      this.isMarshallerLoader = isMarshallerConnector;
   }

   /**
    * Indicates if the connector has been started yet.
    *
    * @return
    */
   public boolean isStarted()
   {
      return isStarted;
   }

   /**
    * This method is called by the MBeanServer before registration takes
    * place. The MBean is passed a reference of the MBeanServer it is
    * about to be registered with. The MBean must return the ObjectName it
    * will be registered with. The MBeanServer can pass a suggested object
    * depending upon how the MBean is registered.<p>
    * <p/>
    * The MBean can stop the registration by throwing an exception.The
    * exception is forwarded to the invoker wrapped in an
    * MBeanRegistrationException.
    *
    * @param server the MBeanServer the MBean is about to be
    *               registered with.
    * @param name   the suggested ObjectName supplied by the
    *               MBeanServer.
    * @return the actual ObjectName to register this MBean with.
    * @throws Exception for any error, the MBean is not registered.
    */
   public ObjectName preRegister(MBeanServer server, ObjectName name)
         throws Exception
   {
      this.server = server;
      return name;
   }

   /**
    * This method is called by the MBeanServer after registration takes
    * place or when registration fails.
    *
    * @param registrationDone the MBeanServer passes true when the
    *                         MBean was registered, false otherwise.
    */
   public void postRegister(Boolean registrationDone)
   {
   }

   /**
    * This method is called by the MBeanServer before deregistration takes
    * place.<p>
    * <p/>
    * The MBean can throw an exception, this will stop the deregistration.
    * The exception is forwarded to the invoker wrapped in
    * an MBeanRegistrationException.
    */
   public void preDeregister()
         throws Exception
   {
   }

   /**
    * This method is called by the MBeanServer after deregistration takes
    * place.
    */
   public void postDeregister()
   {
   }

   /**
    * Starts the connector.  This is when configuration will be applied and server invoker created.
    *
    * @jmx.managed-operation description = "Start sets up the ServerInvoker we are wrapping."
    * impact      = "ACTION"
    */
   public void start() throws Exception
   {
      if (!isStarted)
      {

         // doing this for those who use remoting outside of jboss container
         // so don't have to call create() and then start()
         if (!isCreated)
         {
            create();
         }

         // want to have handlers registered before starting, so if someone makes invocation,
         // there is something to handle it.
         if (serverConfiguration != null)
         {
            configureHandlersFromServerConfiguration();
         }
         else if (xml != null)
         {
            configureHandlersFromXML();
         }

         // if marshaller loader not started, start it
         if (!isMarshallerLoader)
         {
            if (marshallerLoaderConnector != null && !marshallerLoaderConnector.isStarted())
            {
               marshallerLoaderConnector.start();
            }
         }

         // if invoker not started, start it
         if (invoker.isStarted() == false)
         {
            try
            {
               invoker.start();
            }
            catch (Exception e)
            {
               if (marshallerLoaderConnector != null)
               {
                  marshallerLoaderConnector.stop();
               }
               log.debug("Error starting connector.", e);
               throw e;
            }
         }
         isStarted = true;

         log.debug(this + " started");
      }

   }

   /**
    * Starts the connector.
    *
    * @param runAsNewThread indicates if should be started on new thread or the current one.  If
    *                       runAsNewThread is true, new thread will not be daemon thread.
    * @throws Exception
    */
   public void start(boolean runAsNewThread) throws Exception
   {

      Runnable r = new Runnable()
      {
         public void run()
         {
            try
            {
               start();
            }
            catch (Exception e)
            {
               log.error("Error starting Connector.", e);
            }
         }
      };
      Thread t = new Thread(r);
      t.setDaemon(false);
      t.start();
   }

   private void init()
         throws Exception
   {
      Map invokerConfig = new HashMap();

      if (locatorURI == null)
      {
         // InvokerLocator attribute not set; check to see if serverConfiguration is set.
         if (serverConfiguration != null)
         {
            getInvokerConfigFromServerConfiguration(invokerConfig);
         }
         // Check to see if Configuration attribute is set.
         else if (xml != null)
         {
            getInvokerConfigFromXML(invokerConfig);
         }

         configuration.putAll(invokerConfig);
      }

      if (locatorURI == null)
      {
         throw new IllegalStateException("Connector not configured with LocatorURI.");
      }

      final InvokerLocator locator = new InvokerLocator(locatorURI);

      if (invoker == null)
      {
         // create the server invoker
         try
         {
            invoker = (ServerInvoker) AccessController.doPrivileged( new PrivilegedExceptionAction()
            {
               public Object run() throws Exception
               {
                  return  InvokerRegistry.createServerInvoker(locator, configuration);
               }
            });
         }
         catch (PrivilegedActionException pae)
         {
            throw pae.getException();
         }
         
         invoker.setMBeanServer(server);

         // set the server socket factory if has been already set on the connector
         invoker.setServerSocketFactory(svrSocketFactory);
         // seting to null as don't want to keep reference in connector, but the server invoker
         // see JBREM-367
         this.svrSocketFactory = null;

         // set the socket factory if has been already set on the connector
         invoker.setSocketFactory(socketFactory);
         this.socketFactory = null;

         invoker.create();
         
         // this will set the mbean server on the invoker and register it with mbean server
         if (server != null)
         {
            try
            {
               final ObjectName objName = new ObjectName(invoker.getMBeanObjectName());

               try
               {
                  AccessController.doPrivileged( new PrivilegedExceptionAction()
                  {
                     public Object run() throws Exception
                     {
                        if (!server.isRegistered(objName))
                        {
                           server.registerMBean(invoker, objName);
                        }
                        else
                        {
                           log.warn(objName + " is already registered with MBeanServer");
                        }
                        return null;
                     }
                  });
               }
               catch (PrivilegedActionException e)
               {
                  throw (Exception) e.getCause();
               }

            }
            catch (Throwable e)
            {
               log.warn("Error registering invoker " + invoker + " with MBeanServer.", e);
            }
         }
      }

      // if using a generic locator (such as socket://localhost:0), the locator may change so
      // keep the local cache in synch
      locatorURI = invoker.getLocator().getLocatorURI();

      if (remoteClassLoaders == null)
      {
         Object o = configuration.get(Remoting.REMOTE_CLASS_LOADERS);
         if (o instanceof List)
         {
            setRemoteClassLoaders((List) o);
         }
         else if (o != null)
         {
            log.warn("value of " + Remoting.REMOTE_CLASS_LOADERS + " must be a List");
         }
      }

      if (!isMarshallerLoader)
      {
         // need to check if should create a marshaller loader on the server side
         if (marshallerLoaderConnector == null)
         {
            marshallerLoaderConnector = createMarshallerLoader(invoker.getLocator());
         }
      }

   }

   private Connector createMarshallerLoader(InvokerLocator locator)
   {
      /**
       * This is a bit of a hack, but have to bootstrap the marshaller/unmarshaller here because
       * need them loaded and added to the MarshalFactory now, because is possible the first client
       * to make a call on this connector may not have the marshaller/unmarshaller.  Therefore, when
       * the MarshallerLoaderHandler goes to load them for the client (MarshallerLoaderClient), they
       * have to be there.  Otherwise, would not be loaded until first client actually reaches the
       * target server invoker, where they would otherwise be loaded.
       */
      ClassLoader classLoader = (ClassLoader) AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return Connector.class.getClassLoader();
         }
      });
      
      MarshalFactory.getMarshaller(locator, classLoader, configuration);

      Connector marshallerLoader = null;
      InvokerLocator loaderLocator = MarshallLoaderFactory.convertLocator(locator);
      // if loaderLocator is null, then probably not defined to have loader service (i.e. no loader port specified)
      if (loaderLocator != null)
      {
         marshallerLoader = MarshallLoaderFactory.createMarshallLoader(loaderLocator, remoteClassLoaders, server);
      }
      return marshallerLoader;
   }

   private void getInvokerConfigFromXML(Map invokerConfig)
   {
      try
      {
         NodeList invokerNodes = xml.getElementsByTagName("invoker");

         if (invokerNodes != null && invokerNodes.getLength() >= 1)
         {
            // only accept on invoker per connector at present
            Node invokerNode = invokerNodes.item(0);

            NamedNodeMap attributes = invokerNode.getAttributes();
            Node transportNode = attributes.getNamedItem("transport");

            if (transportNode != null)
            {
               String transport = transportNode.getNodeValue();

               // need to log warning if there are more than one invoker elements
               if (invokerNodes.getLength() > 1)
               {
                  log.warn("Found more than one invokers defined in configuration.  " +
                           "Will only be using the first one - " + transport);
               }

               // now create a map for all the sub attributes
               Map paramConfig = new HashMap();
               
               // In case of a multihome configuration.
               List homes = new ArrayList();
               List connectHomes = new ArrayList();
    
               NodeList invokerAttributes = invokerNode.getChildNodes();
               int len = invokerAttributes.getLength();
               for (int x = 0; x < len; x++)
               {
                  Node attr = invokerAttributes.item(x);
                  if ("attribute".equals(attr.getNodeName()))
                  {
                     String name = attr.getAttributes().getNamedItem("name").getNodeValue();
                     String value = attr.getFirstChild().getNodeValue();
                     if ("homes".equals(name))
                     {
                        processHomes(attr, "home", homes);
                     }
                     else if ("connecthomes".equals(name))
                     {
                        processHomes(attr, "connecthome", connectHomes);
                     }
                     else
                     {
                        invokerConfig.put(name, value);
                     }
                     Node isParamAttribute = attr.getAttributes().getNamedItem("isParam");
                     if (isParamAttribute != null && Boolean.valueOf(isParamAttribute.getNodeValue()).booleanValue())
                     {
                        paramConfig.put(name, value);
                     }
                  }
               }
               
               if (homes.isEmpty() && !connectHomes.isEmpty())
               {
                  throw new Exception("Configuration has a " + InvokerLocator.CONNECT_HOMES_KEY +
                                      " without a " + InvokerLocator.HOMES_KEY);
               }

               // should now have my map with all my attributes, now need to look for
               // specific attributes that will impact the locator uri.

               String clientConnectAddress = (String) invokerConfig.get("clientConnectAddress");
               String clientConnectPort = (String) invokerConfig.get("clientConnectPort");
               String serverBindAddress = (String) invokerConfig.get("serverBindAddress");
               String serverBindPort = (String) invokerConfig.get("serverBindPort");
               String localHostAddress = getLocalHost().getHostAddress();              
               
               String tempURI = null;
               String path = (String) invokerConfig.get("path");
               PortUtil.updateRange(invokerConfig);
               
               if (homes.isEmpty() && connectHomes.isEmpty())
               {
                  int port = clientConnectPort != null
                                ? Integer.parseInt(clientConnectPort)
                                : serverBindPort != null
                                   ? Integer.parseInt(serverBindPort)
                                   : PortUtil.findFreePort(serverBindAddress != null
                                                              ? serverBindAddress
                                                               : localHostAddress);
                  String host = clientConnectAddress != null
                                   ? clientConnectAddress
                                   : serverBindAddress != null
                                      ? serverBindAddress
                                      : localHostAddress;
   
                  // finally, let's build the invoker uri
                  tempURI = transport + "://" + fixHostnameForURL(host) + ":" + port;
                  if (path != null)
                  {
                     tempURI += "/" + path;
                  }
               }
               else
               {
                  String port = clientConnectPort != null
                                   ? ":" + clientConnectPort
                                   : serverBindPort != null
                                      ? ":" + serverBindPort
                                      : "";
                  tempURI = transport + "://multihome" + port;
                  if (path != null)
                  {
                     tempURI += "/" + path;
                  }
                  tempURI += "/?";
                  
                  Iterator it = homes.iterator();
                  tempURI += "homes=" + it.next();
                  while (it.hasNext())
                  {
                     tempURI += "!" + it.next();
                  }
                  
                  if (!connectHomes.isEmpty())
                  {
                     tempURI += "&connecthomes=";
                     it = connectHomes.iterator();
                     tempURI += it.next();
                     while (it.hasNext())
                     {
                        tempURI += "!" + it.next();
                     }  
                  }
               }

               // any params to add to the uri?
               if (paramConfig.size() > 0)
               {
                  if (tempURI.indexOf("/?") < 0)
                     tempURI += "/?";
                  else
                     tempURI += "&";
                  Iterator keyItr = paramConfig.keySet().iterator();
                  if (keyItr.hasNext())
                  {
                     Object name = keyItr.next();
                     Object value = paramConfig.get(name);
                     tempURI += name + "=" + value;
                  }
                  while (keyItr.hasNext())
                  {
                     tempURI += "&";
                     Object name = keyItr.next();
                     Object value = paramConfig.get(name);
                     tempURI += name + "=" + value;
                  }
               }
               locatorURI = tempURI;
            }
            else
            {
               log.error("Invoker element within Configuration attribute does not contain a transport attribute.");
            }
         }
      }
      catch (Exception e)
      {
         log.error("Error configuring invoker for connector: " + e.getMessage());
         log.debug("Error configuring invoker for connector.", e);
         throw new IllegalStateException("Error configuring invoker for connector.  Can not continue without invoker.");
      }
   }
   
   private void processHomes(Node node, String homeType, List homes)
   {
      NodeList nodes = node.getChildNodes();
      for (int i = 0; i < nodes.getLength(); i++)
      {
         Node child = nodes.item(i);
         if (Node.ELEMENT_NODE == child.getNodeType())
         {
            if (homeType.equals(child.getNodeName()))
            {
               NodeList children = child.getChildNodes();
               for (int k = 0; k < children.getLength(); k++)
               {
                  Node grandchild = children.item(k);
                  if (Node.TEXT_NODE == grandchild.getNodeType())
                  {
                     homes.add(grandchild.getNodeValue());
                  } 
               }
            }
         }
      }
   }

   private void getInvokerConfigFromServerConfiguration(Map invokerConfig) throws Exception
   {
      try
      {
         String transport = serverConfiguration.getTransport();
         
         if (transport == null)
         {
            log.error("ServerConfiguration must contain a non-null transport attribute.");
            return;
         }
         
         Map locatorParameters = serverConfiguration.getInvokerLocatorParameters();
         Map serverParameters = serverConfiguration.getServerParameters();
         
         // Remove single home elements meant to be ignored.
         String temp = (String) serverParameters.remove("clientConnectAddress");
         if (temp != null) log.warn("clientConnectAddress in server parameters will be ignored");
         temp = (String) serverParameters.remove("clientConnectPort");
         if (temp != null) log.warn("clientConnectPort in server parameters will be ignored");
         temp = (String) serverParameters.get("serverBindAddress");
         if (temp != null)
         {
            locatorParameters.remove("serverBindAddress");
            log.warn("serverBindAddress in locator parameters will be ignored");
         }
         temp = (String) serverParameters.get("serverBindPort");
         if (temp != null)
         {
            locatorParameters.remove("serverBindPort");
            log.warn("serverBindPort in locator parameters will be ignored");
         }

         // Adjust multihome elements.
         temp = (String) serverParameters.remove(InvokerLocator.CONNECT_HOMES_KEY);
         if (temp != null) log.warn(InvokerLocator.CONNECT_HOMES_KEY + " in server parameters will be ignored");
         temp = (String) serverParameters.remove(InvokerLocator.HOMES_KEY);
         if (temp != null)
         {
            if (locatorParameters.get(InvokerLocator.HOMES_KEY) == null)
               locatorParameters.put(InvokerLocator.HOMES_KEY, temp);
            else
               log.warn(InvokerLocator.HOMES_KEY + " in server parameters will be ignored");
         }

         // If there is a connect homes value, then there must be a homes value.
         if (locatorParameters.get(InvokerLocator.CONNECT_HOMES_KEY) != null &&
             locatorParameters.get(InvokerLocator.HOMES_KEY) == null)
         {
            throw new Exception("Configuration has a " + InvokerLocator.CONNECT_HOMES_KEY +
                                " without a " + InvokerLocator.HOMES_KEY);
         }
         
         // Get multihome parameters.
         String connectHomes = (String) locatorParameters.remove(InvokerLocator.CONNECT_HOMES_KEY);
         String homes = (String) serverParameters.remove(InvokerLocator.HOMES_KEY);
         temp = (String) locatorParameters.remove(InvokerLocator.HOMES_KEY);
         if (homes == null) homes = temp;
         
         locatorParameters = new HashMap(serverConfiguration.getInvokerLocatorParameters());
         serverParameters = new HashMap(serverConfiguration.getServerParameters());
         
         // Get single home parameters.
         String clientConnectAddress = (String) locatorParameters.remove("clientConnectAddress");
         String clientConnectPort = (String) locatorParameters.remove("clientConnectPort");
         String serverBindAddress = (String) serverParameters.get("serverBindAddress");
         String defaultPortString = (String) serverParameters.get("serverBindPort");
         temp = (String) locatorParameters.remove("serverBindAddress");
         if (serverBindAddress == null) serverBindAddress = temp;
         temp = (String) locatorParameters.remove("serverBindPort");
         if (defaultPortString == null) defaultPortString = temp;
         
         String path = (String) locatorParameters.remove("path");
         PortUtil.updateRange(invokerConfig);
         
         String tempURI = null;
         boolean parametersStarted = false;
         if (connectHomes == null && homes == null)
         {
            String localHostAddress = getLocalHost().getHostAddress();
            
            // A single home configuration.
            String host = clientConnectAddress != null 
                             ? clientConnectAddress
                             : serverBindAddress != null
                                ? serverBindAddress
                                : localHostAddress;

            int port = clientConnectPort != null
                          ? Integer.parseInt(clientConnectPort)
                          : defaultPortString != null
                             ? Integer.parseInt(defaultPortString)
                             : PortUtil.findFreePort(serverBindAddress != null
                                                        ? serverBindAddress
                                                        : localHostAddress);

            tempURI = transport + "://" + fixHostnameForURL(host) + ":" + port + ((path != null) ? ("/" + path) : "");
         }
         else
         {
            // A multihome configuration.
            tempURI = transport + "://multihome" + ((path != null) ? ("/" + path) : "");
            parametersStarted = true;
            tempURI += "/?";
            if (connectHomes != null)
            {
               tempURI += InvokerLocator.CONNECT_HOMES_KEY + "=" + connectHomes;
               if (homes != null)
                  tempURI += "&" + InvokerLocator.HOMES_KEY + "=" + homes;
            }
            else if (homes != null)
               tempURI += InvokerLocator.HOMES_KEY + "=" + homes;
         }
         
         // any params to add to the uri?
         if (locatorParameters.size() > 0)
         {
            if (!parametersStarted)
               tempURI += "/?";
            else
               tempURI += "&";
            Iterator keyItr = locatorParameters.keySet().iterator();
            if (keyItr.hasNext())
            {
               Object name = keyItr.next();
               Object value = locatorParameters.get(name);
               tempURI += name + "=" + value;
            }
            while (keyItr.hasNext())
            {
               tempURI += "&";
               Object name = keyItr.next();
               Object value = locatorParameters.get(name);
               tempURI += name + "=" + value;
            }
         }
         
         locatorURI = tempURI;
         
         invokerConfig.putAll(serverConfiguration.getServerParameters());
         invokerConfig.putAll((serverConfiguration.getInvokerLocatorParameters()));
      }
      catch (Exception e)
      {
         log.error("Error configuring invoker for connector: " + e.getMessage());
         log.debug("Error configuring invoker for connector.", e);
         throw new IllegalStateException("Error configuring invoker from configuration POJO.  Can not continue without invoker.");
      }
   }
   
   // adds enclosing brackets if an IPv6 literal address
   private String fixHostnameForURL(String address)
   {
      if (address == null) 
         return address ;

      if (address.indexOf(':') != -1 && address.indexOf("[") == -1)
         return "[" + address + "]" ;
      else 
         return address ;
   }
   
   private void configureHandlersFromServerConfiguration() throws Exception
   {
      Map handlerMap = serverConfiguration.getInvocationHandlers();
      if (handlerMap.size() == 0 &&
            (getInvocationHandlers() == null || getInvocationHandlers().length == 0))
      {
         throw new IllegalArgumentException("invocationHandlers list empty and are no registered handlers found.");
      }


      Iterator it = handlerMap.keySet().iterator();
      while (it.hasNext())
      {
         String subsystems = (String) it.next();
         Object value = handlerMap.get(subsystems);
         ServerInvocationHandler handler = null;
         
         if (value instanceof ServerInvocationHandler)
         {
            handler = (ServerInvocationHandler) value;
         }
         else if (value instanceof String)
         {
            //first check to see if this is an ObjectName
            String valueString = (String) value;
            boolean isObjName = false;
            try
            {
               ObjectName objName = new ObjectName(valueString);
               handler = createHandlerProxy(objName);
               isObjName = true;
            }
            catch (MalformedObjectNameException e)
            {
               log.debug("Handler supplied is not an object name.");
            }

            if (!isObjName)
            {
               Class serverInvocationHandlerClass = ClassLoaderUtility.loadClass(valueString, Connector.class);
               handler = (ServerInvocationHandler) serverInvocationHandlerClass.newInstance();
            }
         }
         else
         {
            throw new IllegalArgumentException("handler has invalid type: " + value);
         }

         StringTokenizer tok = new StringTokenizer(subsystems, ",");

         while (tok.hasMoreTokens())
         {
            String subsystem = tok.nextToken();
            addInvocationHandler(subsystem, handler);
         }
      }
   }
   
   private void configureHandlersFromXML() throws Exception
   {
      NodeList handlersNodes = xml.getElementsByTagName("handler");

      if ((handlersNodes == null || handlersNodes.getLength() <= 0) &&
            (getInvocationHandlers() == null || getInvocationHandlers().length == 0))
      {
         throw new IllegalArgumentException("required 'handler' element not found and are no registered handlers found.");
      }

      int len = handlersNodes.getLength();

      for (int c = 0; c < len; c++)
      {
         Node node = handlersNodes.item(c);
         Node subNode = node.getAttributes().getNamedItem("subsystem");

         if (subNode == null)
         {
            throw new IllegalArgumentException("Required 'subsystem' attribute on 'handler' element");
         }

         String handlerClass = node.getFirstChild().getNodeValue();

         boolean isObjName = false;
         ServerInvocationHandler handler = null;

         //first check to see if this is an ObjectName
         try
         {
            ObjectName objName = new ObjectName(handlerClass);
            handler = createHandlerProxy(objName);
            isObjName = true;
         }
         catch (MalformedObjectNameException e)
         {
            log.debug("Handler supplied is not an object name.");
         }

         if (!isObjName)
         {
            Class serverInvocationHandlerClass = ClassLoaderUtility.loadClass(handlerClass, Connector.class);
            handler = (ServerInvocationHandler) serverInvocationHandlerClass.newInstance();
//          handler = (ServerInvocationHandler) cl.loadClass(handlerClass).newInstance();
         }

         StringTokenizer tok = new StringTokenizer(subNode.getNodeValue(), ",");

         while (tok.hasMoreTokens())
         {
            String subsystem = tok.nextToken();
            addInvocationHandler(subsystem, handler);
         }
      }
   }

   private ServerInvocationHandler createHandlerProxy(ObjectName objName)
   {
      ServerInvocationHandler handler;
      if (server != null)
      {
         handler = (ServerInvocationHandler)
               MBeanServerInvocationHandler.newProxyInstance(server,
                                                             objName,
                                                             ServerInvocationHandler.class,
                                                             false);
         handler = new ServerInvocationHandlerWrapper(handler);
      }
      else
      {
         throw new RuntimeException("Can not register MBean invocation handler as the Connector has not been registered with a MBeanServer.");
      }
      return handler;
   }

   /**
    * Adds a connection listener to receive notification when a client connection
    * is lost or disconnected.  Will only be triggered for notifications when
    * leasing is turned on (via the lease period attribute being set to > 0).
    *
    * @param listener
    * @jmx.managed-operation description = "Add a connection listener to call when detect that a client has
    * failed or disconnected."
    * impact      = "ACTION"
    * @jmx.managed-parameter name        = "listener"
    * type        = "org.jboss.remoting.ConnectionListener"
    * description = "The connection listener to register"
    */
   public void addConnectionListener(ConnectionListener listener)
   {
      if (invoker != null)
      {
         invoker.addConnectionListener(listener);
      }
   }

   /**
    * Removes connection listener from receiving client connection lost/disconnected
    * notifications.
    *
    * @param listener
    * @jmx.managed-operation description = "Remove a client connection listener."
    * impact      = "ACTION"
    * @jmx.managed-parameter name        = "listener"
    * type        = "org.jboss.remoting.ConnectionListener"
    * description = "The client connection listener to remove."
    */
   public void removeConnectionListener(ConnectionListener listener)
   {
      if (invoker != null)
      {
         invoker.removeConnectionListener(listener);
      }
   }

   /**
    * Sets the lease period for client connections.
    * Value is in milliseconds.
    *
    * @param leasePeriodValue
    * @jmx.managed-attribute description = "The number of milliseconds that should be used
    * when establishing the client lease period (meaning client will need to update its lease
    * within this amount of time or will be considered dead)."
    * access     = "read-write"
    */
   public void setLeasePeriod(long leasePeriodValue)
   {
      if (invoker != null)
      {
         invoker.setLeasePeriod(leasePeriodValue);
      }
   }

   /**
    * Gets the lease period for client connections.
    * Value in milliseconds.
    *
    * @return
    * @jmx.managed-attribute
    */
   public long getLeasePeriod()
   {
      if (invoker != null)
      {
         return invoker.getLeasePeriod();
      }
      else
      {
         return -1;
      }
   }


   /**
    * Stops the connector.  Will also stop and destroy server invoker (transport)
    *
    * @jmx.managed-operation description = "Stop tears down the ServerInvoker we are wrapping."
    * impact      = "ACTION"
    */
   public void stop()
   {
      if (isStarted)
      {
         if (invoker != null)
         {
            if (server != null)
            {
               try
               {
                  ObjectName objName = new ObjectName(invoker.getMBeanObjectName());
                  unregisterMBean(server, objName);
               }
               catch (Exception e)
               {
                  log.error("invalid Object Name", e);
               }  
            }
            log.trace(this + " shutting down server invoker");
            invoker.stop();
            invoker.destroy();
            
            AccessController.doPrivileged( new PrivilegedAction()
            {
               public Object run()
               {
                  InvokerRegistry.destroyServerInvoker(invoker);
                  return null;
               }
            });
            
            invoker = null;
         }
         if (marshallerLoaderConnector != null && marshallerLoaderConnector.isStarted)
         {
            marshallerLoaderConnector.stop();
            marshallerLoaderConnector = null;
         }
         isStarted = false;
      }
      
      log.trace(this + " is stopped");
   }

   /**
    * Creates the connector.
    *
    * @jmx.managed-operation
    */
   public void create()
         throws Exception
   {
      if (!isCreated)
      {
         try
         {
            init();
            isCreated = true;
         }
         catch (Exception e)
         {
            // unwind create process
            if (invoker != null)
            {
               invoker.stop();
               invoker.destroy();
               
               AccessController.doPrivileged( new PrivilegedAction()
               {
                  public Object run()
                  {
                     InvokerRegistry.destroyServerInvoker(invoker);
                     return null;
                  }
               });
               
               invoker = null;
            }
            isCreated = false;
            throw e;
         }
      }
   }

   /**
    * Destroys the connector.
    *
    * @jmx.managed-operation
    */
   public void destroy()
   {
      if (isStarted)
      {
         stop();
      }
      if (invoker != null)
      {
         invoker.stop();
         invoker.destroy();
         
         AccessController.doPrivileged( new PrivilegedAction()
         {
            public Object run()
            {
               InvokerRegistry.destroyServerInvoker(invoker);
               return null;
            }
         });
         
         invoker = null;
      }
      isCreated = false;
   }

   public ServerInvoker getServerInvoker()
   {
      return invoker;
   }

   /**
    * Will get array of all the handlers registered with the connector's server invoker.
    *
    * @return
    */
   public ServerInvocationHandler[] getInvocationHandlers()
   {
      ServerInvocationHandler[] handlers = null;
      if (invoker != null)
      {
         handlers = invoker.getInvocationHandlers();
      }

      return handlers;
   }

   /**
    * Returns the locator to the connector. Locator is the actual InvokerLocator
    * object used to identify and get the ServerInvoker we are wrapping.
    *
    * @jmx.managed-attribute description = "Locator is the actual InvokerLocator object used to
    * identify and get the ServerInvoker we are wrapping."
    * access      = "read-only"
    */
   public InvokerLocator getLocator()
   {
      return invoker.getLocator();
   }

   /**
    * Sets the invoker locator. InvokerLocator is the string URI representation
    * of the InvokerLocator used to get and identify the ServerInvoker
    * we are wrapping.
    *
    * @jmx.managed-attribute description = "InvokerLocator is the string URI representation of the
    * InvokerLocator used to get and identify the ServerInvoker
    * we are wrapping."
    * access     = "read-write"
    */
   public void setInvokerLocator(String locator)
         throws Exception
   {
      if (!isCreated)
      {
         locatorURI = locator;
      }
      else
      {
         throw new RuntimeException("Can not set the invoker locator on this Connector " +
                                    "as has already been created with a different locator.");
      }
   }


   /**
    * Returns the invoker locator. InvokerLocator is the string URI representation
    * of the InvokerLocator used to get and identify the ServerInvoker
    * we are wrapping.
    *
    * @jmx.managed-attribute
    */
   public String getInvokerLocator() throws Exception
   {
      return locatorURI;
   }

   /**
    * Configuration is an xml element indicating subsystems to be registered
    * with the ServerInvoker we wrap. Using mbean subsystems that call
    * registerSubsystem is more flexible.
    *
    * @jmx.managed-attribute description = "Configuration is an xml element indicating subsystems
    * to be registered with the ServerInvoker we wrap. Using
    * mbean subsystems that call registerSubsystem is more
    * flexible."
    * access     = "read-write"
    */
   public void setConfiguration(Element xml)
         throws Exception
   {
      this.xml = xml;
   }

   /**
    * Configuration is an xml element indicating subsystems to be registered
    * with the ServerInvoker we wrap. Using mbean subsystems that call
    * registerSubsystem is more flexible.
    *
    * @jmx.managed-attribute
    */
   public Element getConfiguration()
   {
      return xml;
   }

   /**
    * Adds a handler to the connector via OjbectName.  This will create a mbean proxy of
    * type of ServerInvocationHandler for the MBean specified by object name passed (so has
    * to implement ServerInvocationHandler interface).
    *
    * @param subsystem
    * @param handlerObjectName
    * @return Previous ServerInvocatioHandler with the same subsystem value (case insensitive) or null if one did not previously exist.
    * @throws Exception
    * @jmx.managed-operation description = "Add a subsystem invocation handler to the ServerInvoker
    * we wrap, identified by the subsystem parameter."
    * impact      = "ACTION"
    * @jmx.managed-parameter name        = "subsystem"
    * type        = "java.lang.String"
    * description = "The subsystem this handler is for."
    * @jmx.managed-parameter name        = "handlerObjectName"
    * type        = "javax.management.ObjectName"
    * description = "The ServerInvocationHandler MBean we are registering
    * for the subsystem"
    */
   public ServerInvocationHandler addInvocationHandler(String subsystem, ObjectName handlerObjectName) throws Exception
   {
      ServerInvocationHandler invocationHandler = createHandlerProxy(handlerObjectName);
      return addInvocationHandler(subsystem, invocationHandler);
   }

   /**
    * Adds an invocation handler for the named subsystem to the invoker we
    * manage, and sets the mbean server on the invocation handler.
    *
    * @return Previous ServerInvocatioHandler with the same subsystem value (case insensitive) or null if one did not previously exist.
    * @jmx.managed-operation description = "Add a subsystem invocation handler to the ServerInvoker
    * we wrap, identified by the subsystem parameter."
    * impact      = "ACTION"
    * @jmx.managed-parameter name        = "subsystem"
    * type        = "java.lang.String"
    * description = "The subsystem this handler is for."
    * @jmx.managed-parameter name        = "handler"
    * type        = "org.jboss.remoting.ServerInvocationHandler"
    * description = "The ServerInvocationHandler we are registering
    * for the subsystem"
    */
   public ServerInvocationHandler addInvocationHandler(String subsystem, final ServerInvocationHandler handler)
         throws Exception
   {
      if (invoker == null)
      {
         throw new IllegalStateException("You may only add handlers once the Connector is created (via create() method).");
      }

      // In case handler is an MBean.
      AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            handler.setMBeanServer(server);
            return null;
         }
      });
      return invoker.addInvocationHandler(subsystem, handler);
   }

   /**
    * Removes an invocation handler for the supplied subsystem from the invoker
    * we manage, and unsets the MBeanServer on the handler.
    *
    * @jmx.managed-operation description = "Remove a subsystem invocation handler to the
    * ServerInvoker we wrap, identified by the subsystem
    * parameter."
    * impact      = "ACTION"
    * @jmx.managed-parameter name        = "subsystem"
    * type        = "java.lang.String"
    * description = "The subsystem this handler is for."
    */
   public void removeInvocationHandler(String subsystem) throws Exception
   {
      ServerInvocationHandler handler = invoker.removeInvocationHandler(subsystem);

      if (handler != null)
      {
         handler.setMBeanServer(null);
      }
   }

   /**
    * The server socket factory can only be set on the Connector before the create() method
    * has been called.  Otherwise, a runtime exception will be thrown.
    * @param serverSocketFactory
    */
   public void setServerSocketFactory(ServerSocketFactory serverSocketFactory)
   {
      if(isCreated)
      {
         throw new RuntimeException("Can not set server socket factory on Connector after the create() method has been called.");
      }

      if (invoker != null)
      {
         invoker.setServerSocketFactory(serverSocketFactory);
      }
      else
      {
         this.svrSocketFactory = serverSocketFactory;
      }
   }

   public ServerSocketFactory getServerSocketFactory()
   {
      if (invoker != null)
      {
         return invoker.getServerSocketFactory();
      }
      else
      {
         return svrSocketFactory;
      }
   }

   /**
    * The socket factory (for callbacks) can only be set on the Connector before the
    * create() method has been called.  Otherwise, a runtime exception will be thrown.
    * @param socketFactory
    */
   public void setSocketFactory(SocketFactory socketFactory)
   {
      if(isCreated)
      {
         throw new RuntimeException("Can not set socket factory on Connector after the create() method has been called.");
      }

      if (invoker != null)
      {
         invoker.setSocketFactory(socketFactory);
      }
      else
      {
         this.socketFactory = socketFactory;
      }
   }

   public SocketFactory getSocketFactory()
   {
      if (invoker != null)
      {
         return invoker.getSocketFactory();
      }
      else
      {
         return socketFactory;
      }
   }

   public ServerConfiguration getServerConfiguration()
   {
      return serverConfiguration;
   }

   public void setServerConfiguration(ServerConfiguration serverConfig)
   {
      this.serverConfiguration = serverConfig;
   }
   
   public void setRemoteClassLoaders(List classLoaders)
   {
      if (classLoaders == null)
         return;

      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         sm.checkPermission(new RuntimePermission("setContextClassLoader"));
      }
      this.remoteClassLoaders = classLoaders;
   }
   
   static private void unregisterMBean(final MBeanServer server, final ObjectName name)
   throws Exception
   {
      if (SecurityUtility.skipAccessControl())
      {
         server.unregisterMBean(name);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               server.unregisterMBean(name);
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (Exception) e.getCause();
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
}
