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

import javax.management.Notification;
import javax.management.ObjectName;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.ServerInvokerMetadata;
import org.jboss.remoting.ident.Identity;

/**
 * NetworkNotification is a JMX Notification that is sent when changes occur to the network layout as
 * tracked by a NetworkRegistryMBean.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 566 $
 */
public class NetworkNotification extends Notification
{
   static final long serialVersionUID = 987487760197074685L;

   public static final String SERVER_ADDED = "jboss.network.server.added";
   public static final String SERVER_UPDATED = "jboss.network.server.updated";
   public static final String SERVER_REMOVED = "jboss.network.server.removed";
   public static final String DOMAIN_CHANGED = "jboss.network.domain.changed";

   private final Identity identity;
   private final ServerInvokerMetadata serverInvokers[];
   private final InvokerLocator locators[];


   public NetworkNotification(ObjectName source, String type, Identity identity, ServerInvokerMetadata serverInvokers[])
   {
      super(type, source, System.currentTimeMillis());
      this.identity = identity;
      this.serverInvokers = serverInvokers;
      this.locators = null;
   }

   public NetworkNotification(ObjectName source, String type, Identity identity, InvokerLocator locators[])
   {
      super(type, source, System.currentTimeMillis());
      this.identity = identity;
      this.serverInvokers = null;
      this.locators = locators;
   }

   /**
    * return the identity of the notification
    *
    * @return
    */
   public final Identity getIdentity()
   {
      return identity;
   }

   /**
    * return the locators affected by the notification
    *
    * @return
    */
   public final InvokerLocator[] getLocator()
   {
      if(locators != null)
      {
         return locators;
      }
      else
      {
         InvokerLocator[] locators = new InvokerLocator[serverInvokers.length];
         for(int x = 0; x < serverInvokers.length; x++)
         {
            locators[x] = serverInvokers[x].getInvokerLocator();
         }
         return locators;
      }
   }

   /**
    * Gets all the server invoker metadata for the servers running on
    * specified server.  Will include locator and supported subsystems
    * for that locator.
    *
    * @return
    */
   public final ServerInvokerMetadata[] getServerInvokers()
   {
      return serverInvokers;
   }
}
