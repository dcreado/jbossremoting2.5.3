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

import java.io.Serializable;
import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.detection.ServerInvokerMetadata;
import org.jboss.remoting.ident.Identity;

/**
 * NetworkInstance is an object that represents an Identity and its InvokerLocators.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 566 $
 */
public class NetworkInstance implements Serializable
{
   static final long serialVersionUID = -1745108606611832280L;

   private final Identity identity;
   private final ServerInvokerMetadata serverInvokers[];
   private final InvokerLocator locators[];
   private final int hashCode;

   public NetworkInstance(Identity identity, InvokerLocator locators[])
   {
      this.identity = identity;
      this.locators = locators;
      this.hashCode = this.identity.hashCode();
      this.serverInvokers = null;
   }

   public NetworkInstance(Identity identity, ServerInvokerMetadata serverInvokers[])
   {
      this.identity = identity;
      this.locators = null;
      this.hashCode = this.identity.hashCode();
      this.serverInvokers = serverInvokers;
   }

   public final Identity getIdentity()
   {
      return identity;
   }

   public final InvokerLocator[] getLocators()
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


   public String toString()
   {
      return "NetworkInstance [identity:" + identity + ",locator count:" + (serverInvokers == null ? 0 : serverInvokers.length) + "]";
   }

   public boolean equals(Object obj)
   {
      return hashCode == obj.hashCode();
   }

   public int hashCode()
   {
      return hashCode;
   }
}
