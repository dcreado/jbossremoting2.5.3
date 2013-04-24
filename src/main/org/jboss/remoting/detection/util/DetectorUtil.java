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
package org.jboss.remoting.detection.util;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.apache.log4j.Level;
import org.jboss.logging.Logger;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.Detector;
import org.jboss.remoting.detection.jndi.JNDIDetector;
import org.jboss.remoting.detection.multicast.MulticastDetector;
import org.jboss.remoting.network.NetworkInstance;
import org.jboss.remoting.network.NetworkRegistry;
import org.jboss.remoting.transport.Connector;
import org.jboss.remoting.util.SecurityUtility;


/**
 * A simple utility class that will periodically print out remoting servers
 * running based on detections.  Can specify to either use multicast or jndi
 * detection type.
 *
 * @author <a href="mailto:telrod@vocalocity.net">Tom Elrod</a>
 * @version $Revision: 5001 $
 */
public class DetectorUtil
{
   /**
    * multicast *
    */
   public static final String TYPE_MULTICAST = "multicast";
   /**
    * jndi *
    */
   public static final String TYPE_JNDI = "jndi";
   /**
    * 2410 *
    */
   public static final int DEFAULT_PORT = 2410;
   /**
    * localhost *
    */
   public static final String DEFAULT_HOST = "localhost";

   private String contextFactory = "org.jnp.interfaces.NamingContextFactory";
   private String urlPackage = "org.jboss.naming:org.jnp.interfaces";

   private String type = TYPE_MULTICAST;
   private int port = DEFAULT_PORT;
   private String host = DEFAULT_HOST;

   public DetectorUtil()
   {
   }

   public DetectorUtil(String type)
   {
      this(type, DEFAULT_PORT, DEFAULT_HOST);
   }

   public DetectorUtil(String type, int port)
   {
      this(type, port, DEFAULT_HOST);
   }

   public DetectorUtil(String type, int port, String host)
   {
      this.type = type;
      this.port = port;
      this.host = host;
   }

   public void start()
   {
      try
      {

         org.apache.log4j.BasicConfigurator.configure();
         org.apache.log4j.Category.getRoot().setLevel(Level.DEBUG);
         Logger log = Logger.getLogger(getClass());

         setSystemProperty("jboss.identity", String.valueOf(System.currentTimeMillis()));

         MBeanServer server = createMBeanServer();
         NetworkRegistry registry = NetworkRegistry.getInstance();
         registerMBean(server, registry, new ObjectName("remoting:type=NetworkRegistry"));
        
         InvokerLocator locator = new InvokerLocator("socket://localhost");
         //ClassLoader clsLoader = Thread.currentThread().getContextClassLoader();
         //SocketServerInvoker invokerSvr = new SocketServerInvoker(clsLoader, locator);
         //ObjectName objName = new ObjectName("jboss.remoting:type=SocketServerInvoker");
         //server.registerMBean(invokerSvr, objName);
         //invokerSvr.start();

         Connector connector = new Connector();
         connector.setInvokerLocator(locator.getLocatorURI());
         ObjectName obj = new ObjectName("jboss.remoting:type=Connector,transport=" + locator.getProtocol());
         registerMBean(server, connector, obj);
         //connector.create();
         connector.start();

         Detector detector = null;
         ObjectName objName = null;

         if(type.equals(TYPE_MULTICAST))
         {
            MulticastDetector mdet = new MulticastDetector();
            mdet.setPort(port);
            detector = mdet;
            objName = new ObjectName("remoting:type=Detector,transport=multicast");
         }
         else if(type.equals(TYPE_JNDI))
         {
            JNDIDetector jdet = new JNDIDetector();
            jdet.setPort(port);
            jdet.setHost(host);
            jdet.setContextFactory(contextFactory);
            jdet.setURLPackage(urlPackage);
            detector = jdet;
            objName = new ObjectName("remoting:type=Detector,transport=jndi");
         }
         
         registerMBean(server, detector, objName);
         detector.start();
         System.err.println("Starting Detector");

         while(true)
         {
            Thread.currentThread().sleep(3000);
            NetworkInstance[] instances = registry.getServers();
            for(int x = 0; x < instances.length; x++)
            {
               log.debug(instances[x]);
            }
         }


      }
      catch(Exception ex)
      {
         System.err.println("Error creating and starting DetectorUtil.");
         ex.printStackTrace();
      }
   }

   public static void main(String[] args)
   {
      DetectorUtil test = null;

      if(args.length == 1)
      {
         String type = args[0];
         test = new DetectorUtil(type);
      }
      else if(args.length == 2)
      {
         String type = args[0];
         int port = Integer.parseInt(args[1]);
         test = new DetectorUtil(type, port);
      }
      else if(args.length == 3)
      {
         String type = args[0];
         int port = Integer.parseInt(args[1]);
         String host = args[2];
         test = new DetectorUtil(type, port, host);
      }
      else
      {
         test = new DetectorUtil();
         System.out.println("Using default type (" + test.getTransport() +
                            ") and default port (" + test.getPort() + ") and " +
                            "default host (" + test.getHost() + ")" +
                            "\nCan enter type (multicast, jndi), port, and/or host via command line.");
      }

      test.start();


   }

   private String getHost()
   {
      return host;
   }

   private int getPort()
   {
      return port;
   }

   private String getTransport()
   {
      return type;
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
   
   static private void setSystemProperty(final String name, final String value)
   {
      if (SecurityUtility.skipAccessControl())
      {
         System.setProperty(name, value);
         return;
      }
      
      try
      {
         AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return System.setProperty(name, value);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw (RuntimeException) e.getCause();
      }
   }
}
