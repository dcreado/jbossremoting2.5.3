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

import javax.management.BadAttributeValueExpException;
import javax.management.BadBinaryOpValueExpException;
import javax.management.BadStringOperationException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidApplicationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.jboss.remoting.util.SecurityUtility;

/**
 * NetworkRegistryQuery is a QueryExp that will filter on NetworkRegistryMBean mbeans.
 *
 * @author <a href="mailto:jhaynie@vocalocity.net">Jeff Haynie</a>
 * @version $Revision: 5004 $
 */
public class NetworkRegistryQuery implements QueryExp
{
   private MBeanServer server;
   private static final long serialVersionUID = 2402056810602499064L;

   public boolean apply(final ObjectName objectName) throws BadStringOperationException, BadBinaryOpValueExpException, BadAttributeValueExpException, InvalidApplicationException
   {
      try
      {
         return isInstanceOf(server, objectName, NetworkRegistryMBean.class.getName());
      }
      catch (InstanceNotFoundException e)
      {
      }

      return false;
   }

   public void setMBeanServer(MBeanServer mBeanServer)
   {
      this.server = mBeanServer;
   }
   
   static private boolean isInstanceOf(final MBeanServer server, final ObjectName objectName, final String className)
   throws InstanceNotFoundException
   {
      if (SecurityUtility.skipAccessControl())
      {
         return server.isInstanceOf(objectName, className);
      }
      
      try
      {
         return ((Boolean)AccessController.doPrivileged( new PrivilegedExceptionAction()
         {
            public Object run() throws Exception
            {
               return new Boolean(server.isInstanceOf(objectName, className));
            }
         })).booleanValue();
      }
      catch (PrivilegedActionException e)
      {
         throw (InstanceNotFoundException) e.getCause();
      }
   }
}
