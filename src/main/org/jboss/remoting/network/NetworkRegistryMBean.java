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

import javax.management.MBeanRegistration;
import javax.management.NotificationBroadcaster;
import org.jboss.remoting.detection.ServerInvokerMetadata;
import org.jboss.remoting.ident.Identity;

/**
 * NetworkRegistryMBean is a managed bean that keeps track of all the servers on a
 * JBOSS network, and associates all the valid invokers on each server that are
 * available.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 566 $
 */
public interface NetworkRegistryMBean extends NotificationBroadcaster, MBeanRegistration
{
   /**
    * return the servers on the network
    *
    * @return
    */
   public NetworkInstance[] getServers();

   /**
    * add a server for a given identity that is available on the network
    *
    * @param identity
    * @param invokers
    */
   public void addServer(Identity identity, ServerInvokerMetadata invokers[]);

   /**
    * remove a server no longer available on the network
    *
    * @param identity
    */
   public void removeServer(Identity identity);

   /**
    * update the invokers for a given server
    *
    * @param identity
    * @param invokers
    */
   public void updateServer(Identity identity, ServerInvokerMetadata invokers[]);

   /**
    * returns true if the server with the identity is available
    *
    * @param identity
    * @return
    */
   public boolean hasServer(Identity identity);

   /**
    * query the network registry for <tt>0..*</tt> of servers based on a
    * filter
    *
    * @param filter
    * @return
    */
   public NetworkInstance[] queryServers(NetworkFilter filter);

   /**
    * change the main domain of the local server
    *
    * @param newDomain
    */
   public void changeDomain(String newDomain);
}
