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
package org.jboss.remoting.detection;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.ident.Identity;

import java.io.Serializable;

/**
 * Detection is an MBean Notification that is fired by
 * Detectors when remote servers are found or lost on the Network.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 1104 $
 */
public class Detection implements Serializable
{
   static final long serialVersionUID = -7560953564286960592L;

   private final ServerInvokerMetadata[] serverInvokers;
   private final Identity identity;
   private final int hashCode;

   public Detection(Identity identity, ServerInvokerMetadata[] serverInvokers)
   {
      this.serverInvokers = serverInvokers;
      this.identity = identity;
      this.hashCode = identity.hashCode();
   }

   public boolean equals(Object obj)
   {
      if(obj instanceof Detection)
      {
         return hashCode == obj.hashCode();
      }
      return false;
   }

   public int hashCode()
   {
      return hashCode;
   }

   public String toString()
   {
      String serverInvokersInfo = "";
      if(serverInvokers != null)
      {
         for(int x = 0; x < serverInvokers.length; x++)
         {
            serverInvokersInfo = serverInvokersInfo + serverInvokers[x] + "\n";
         }
         serverInvokersInfo = serverInvokersInfo.substring(0, serverInvokersInfo.length());
      }

      return "Detection (" + super.toString() + ")\n\tidentity:" + identity + "\n\tlocators:" + (serverInvokers == null ? " null" : serverInvokersInfo);
   }

   /**
    * return the jboss identity
    *
    * @return
    */
   public final Identity getIdentity()
   {
      return identity;
   }

   /**
    * return the locators for the server
    *
    * @return
    */
   public final InvokerLocator[] getLocators()
   {
      InvokerLocator[] locators = new InvokerLocator[serverInvokers == null ? 0 : serverInvokers.length];
      for(int x = 0; x < serverInvokers.length; x++)
      {
         locators[x] = serverInvokers[x].getInvokerLocator();
      }
      return locators;
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
