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

/*
 * Created on Oct 28, 2005
 */
 
package org.jboss.remoting.transport.multiplex.utility;

import java.io.IOException;
import java.net.InetAddress;


/**
 * <code>AddressPair</code> is a utility class that represents a pair of socket addresses,
 * each with a host and port.
 *
 * @author <a href="mailto:r.sigal@computer.org">Ron Sigal</a>
 * @version $Revision: 3443 $
 * <p>
 * Copyright (c) 2005
 * </p>
 * 
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */

public class AddressPair
{
   private InetAddress 	localHost;
   private int			localPort;
   private InetAddress	remoteHost;
   private int			remotePort;
   private boolean		hashCodeSet = false;
   private int			hashCode;
   
   
/**
 * 
 * Create a new <code>AddressPair</code>.
 * @param remoteHost
 * @param remotePort
 * @param localHost
 * @param localPort
 */
   public AddressPair(String remoteHost, int remotePort, String localHost, int localPort) throws IOException
   {
      this.localHost = InetAddress.getByName(localHost);
      this.localPort = localPort;
      this.remoteHost = InetAddress.getByName(remoteHost);
      this.remotePort = remotePort;
   }
   
   
/**
 * 
 */
   public boolean equals(Object o)
   {
      if (o == null)
         return false;
      
      if (!(o instanceof AddressPair))
         return false;
      
      AddressPair ap = (AddressPair) o;
      
      return (localHost.equals(ap.localHost) && 
              localPort == ap.localPort &&
              remoteHost.equals(ap.remoteHost) &&
              remotePort == ap.remotePort);
   }
   
   
/**
 * 
 */
   public int hashCode()
   {
      if (!hashCodeSet)
      {
         hashCode = localHost.hashCode() * localPort + remoteHost.hashCode() * remotePort;
         hashCodeSet = true;
      }
      
      return hashCode;
   }
   
   
/**
 * Get the localHost.
 * 
 * @return the localHost.
 */
   public String getLocalHost()
   {
      return localHost.getHostName();
   }
   
   
/**
 * Get the localPort.
 * 
 * @return the localPort.
 */
   public int getLocalPort()
   {
      return localPort;
   }
   
   
/**
 * Get the remoteHost.
 * 
 * @return the remoteHost.
 */
   public String getRemoteHost()
   {
      return remoteHost.getHostName();
   }
   
   
/**
 * Get the remotePort.
 * 
 * @return the remotePort.
 */
   public int getRemotePort()
   {
      return remotePort;
   }
}

