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
import java.security.PrivilegedAction;
import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

/**
 * NetworkRegistryFinder is a utility class for finding the ObjectName to the NetworkRegistryMBean
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 3837 $
 */
public class NetworkRegistryFinder
{
   /**
    * find the ObjectName to the NetworkRegistryMBean instance on the MBeanServer
    *
    * @param server
    * @return
    */
   public static ObjectName find(final MBeanServer server)
   {
      Set set = (Set)AccessController.doPrivileged( new PrivilegedAction()
      {
         public Object run()
         {
            return server.queryMBeans(null, new NetworkRegistryQuery());
         }
      });
      
      if(set.isEmpty())
      {
         return null;
      }
      return ((ObjectInstance) set.iterator().next()).getObjectName();
   }
}
