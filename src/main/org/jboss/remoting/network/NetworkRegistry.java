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

package org.jboss.remoting.network;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.jboss.logging.Logger;
import org.jboss.mx.util.JBossNotificationBroadcasterSupport;
import org.jboss.remoting.InvokerRegistry;
import org.jboss.remoting.detection.ServerInvokerMetadata;
import org.jboss.remoting.ident.Identity;
import org.jboss.remoting.util.SecurityUtility;

/**
 * NetworkRegistry is a concrete implemenation of the NetworkRegistryMBean
 * interface. The NetworkRegistry will keep a list of all the detected
 * JBoss servers on the network and provide a local facility for querying
 * for different servers.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 5004 $
 */
public class NetworkRegistry implements NetworkRegistryMBean
{
   private static final Logger log = Logger.getLogger(NetworkRegistry.class);
   private MBeanServer mBeanServer;
   private ObjectName objectName;
   private final JBossNotificationBroadcasterSupport broadcaster = new JBossNotificationBroadcasterSupport();
   private final Map servers = new HashMap();
   private static NetworkRegistry singleton;

   public NetworkRegistry()
   {
      super();
      singleton = this;
   }

   /**
    * return the singleton instance
    *
    * @return
    */
   public static final NetworkRegistry getInstance()
   {
      if(singleton == null)
      {
         new NetworkRegistry();
      }
      return singleton;
   }

   /**
    * add a server for a given identity that is available on the network
    *
    * @param identity
    * @param invokers
    */
   public void addServer(final Identity identity, final ServerInvokerMetadata invokers[])
   {
      boolean found = false;
      synchronized(servers)
      {
         if(servers.containsKey(identity) == false)
         {
            servers.put(identity, new NetworkInstance(identity, invokers));
            found = true;
         }
      }
      if(found)
      {
         log.debug(this + " addServer - " + identity);

         // put this on a separate thread so we don't block further detection ...
         // TODO: this needs to go into a thread pool thread -JGH
         new Thread()
         {
            public void run()
            {
               broadcaster.sendNotification(new NetworkNotification(objectName, NetworkNotification.SERVER_ADDED, identity, invokers));
            }
         }.start();
      }
   }

   /**
    * update the invokers for a given server
    *
    * @param identity
    * @param invokers
    */
   public void updateServer(final Identity identity, final ServerInvokerMetadata invokers[])
   {
      boolean found = false;

      synchronized(servers)
      {
         if(servers.containsKey(identity))
         {
            servers.put(identity, new NetworkInstance(identity, invokers));
            found = true;
         }
      }
      if(found)
      {
         // TODO: let's put this in a thread pool thread -JGH
         // put this on a separate thread so we don't block further detection ...
         new Thread()
         {
            public void run()
            {
               broadcaster.sendNotification(new NetworkNotification(objectName, NetworkNotification.SERVER_UPDATED, identity, invokers));
            }
         }.start();
      }
   }

   /**
    * return the servers on the network
    *
    * @return
    */
   public NetworkInstance[] getServers()
   {
      synchronized(servers)
      {
         return (NetworkInstance[]) servers.values().toArray(new NetworkInstance[servers.size()]);
      }
   }

   /**
    * returns true if the server with the identity is available
    *
    * @param identity
    * @return
    */
   public boolean hasServer(Identity identity)
   {
      synchronized(servers)
      {
         return servers.containsKey(identity);
      }
   }

   /**
    * query the network registry for <tt>0..*</tt> of servers based on a
    * filter. if the filter is null, it is considered a wildcard.
    *
    * @param filter
    * @return
    */
   public NetworkInstance[] queryServers(NetworkFilter filter)
   {
      NetworkInstance servers[] = getServers();
      if(servers == null || servers.length <= 0)
      {
         return new NetworkInstance[0];
      }
      Set result = new HashSet();
      for(int c = 0; c < servers.length; c++)
      {
         NetworkInstance instance = (NetworkInstance) this.servers.get(servers[c]);
         if(filter == null ||
            filter.filter(servers[c].getIdentity(), instance.getLocators()))
         {
            if(result.contains(servers[c]) == false)
            {
               // the filter passed, add it
               result.add(servers[c]);
            }
         }
      }
      return (NetworkInstance[]) result.toArray(new NetworkInstance[result.size()]);
   }

   /**
    * remove a server no longer available on the network
    *
    * @param identity
    */
   public void removeServer(final Identity identity)
   {
      NetworkInstance instance = null;

      synchronized(servers)
      {
         instance = (NetworkInstance) servers.remove(identity);
      }
      if(instance != null)
      {
         log.debug(this + " removeServer - " + identity);

         final ServerInvokerMetadata il[] = instance.getServerInvokers();
         // put this on a separate thread so we don't block further detection ...
         // TODO: let's put this is a thread pool thread -JGH
         new Thread()
         {
            public void run()
            {
               broadcaster.sendNotification(new NetworkNotification(objectName, NetworkNotification.SERVER_REMOVED, identity, il));
            }
         }.start();
      }
   }

   public void addNotificationListener(NotificationListener notificationListener, NotificationFilter notificationFilter, Object o) throws IllegalArgumentException
   {
      broadcaster.addNotificationListener(notificationListener, notificationFilter, o);
   }

   public MBeanNotificationInfo[] getNotificationInfo()
   {
      MBeanNotificationInfo info[] = new MBeanNotificationInfo[3];
      info[0] = new MBeanNotificationInfo(new String[]{NetworkNotification.SERVER_ADDED}, NetworkNotification.class.getName(), "Fired when Server is added");
      info[1] = new MBeanNotificationInfo(new String[]{NetworkNotification.SERVER_UPDATED}, NetworkNotification.class.getName(), "Fired when Server is updated");
      info[2] = new MBeanNotificationInfo(new String[]{NetworkNotification.SERVER_REMOVED}, NetworkNotification.class.getName(), "Fired when Server is removed");
      return info;
   }

   public void removeNotificationListener(NotificationListener notificationListener) throws ListenerNotFoundException
   {
      broadcaster.removeNotificationListener(notificationListener);
   }

   public void postDeregister()
   {
   }

   public void postRegister(Boolean aBoolean)
   {
   }

   public void preDeregister() throws Exception
   {
   }

   public ObjectName preRegister(MBeanServer mBeanServer, ObjectName objectName) throws Exception
   {
      this.mBeanServer = mBeanServer;
      this.objectName = objectName;
      // make sure our identity system property is properly set
      Identity identity = Identity.get(this.mBeanServer);
      // this is a slight hack, but we have to have some way to know the main
      // JBoss MBeanServer data
      setSystemProperty("jboss.remoting.jmxid", identity.getJMXId());
      setSystemProperty("jboss.remoting.instanceid", identity.getInstanceId());
      setSystemProperty("jboss.remoting.domain", identity.getDomain());
      return objectName;
   }

   /**
    * change the main domain of the local server
    *
    * @param newDomain
    */
   public synchronized void changeDomain(String newDomain)
   {
      setSystemProperty("jboss.remoting.domain", newDomain);
      NetworkInstance servers[] = getServers();
      if(servers == null || servers.length <= 0)
      {
         return;
      }
      // remove entries that don't match out new domain
      for(int c = 0; c < servers.length; c++)
      {
         NetworkInstance instance = (NetworkInstance) this.servers.get(servers[c]);
         if(newDomain.equals(instance.getIdentity().getDomain()) == false)
         {
            this.servers.remove(servers[c]);
         }
      }
      new Thread()
      {
         public void run()
         {
            broadcaster.sendNotification(new NetworkNotification(objectName, NetworkNotification.DOMAIN_CHANGED, Identity.get(mBeanServer), InvokerRegistry.getRegisteredServerLocators()));
         }
      }.start();
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
