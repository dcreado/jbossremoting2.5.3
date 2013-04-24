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

package org.jboss.remoting;


import java.util.Map;

import org.jboss.logging.Logger;

/**
 * This class extends the MicroRemoteClientInvoker and adds extra
 * functionality that can not be included in a J2ME envrionment, such as
 * setting socket factories.
 *
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 * @version $Revision: 5074 $
 */
public abstract class RemoteClientInvoker extends MicroRemoteClientInvoker
{
   private static Logger log = Logger.getLogger(RemoteClientInvoker.class);
   
   public RemoteClientInvoker(InvokerLocator locator)
   {
      super(locator);
   }

   public RemoteClientInvoker(InvokerLocator locator, Map configuration)
   {
      super(locator, configuration);
      Map parameters = locator.getParameters();
      if (parameters != null)
      {
         String socketFactoryClassName = (String) locator.parameters.get(Remoting.SOCKET_FACTORY_CLASS_NAME);
         if (socketFactoryClassName != null)
         {
            configuration.put(Remoting.SOCKET_FACTORY_CLASS_NAME, socketFactoryClassName);
         }
      }
      if (useAllParams(getConfiguration()))
      {
         // getConfiguration() returns combination of InvokerLocator and configuration parameters.
         socketFactory = createSocketFactory(getConfiguration());
      }
      else
      {
         socketFactory = createSocketFactory(configuration);
      }
   }
   
   protected boolean useAllParams(Map configuration)
   {
      boolean result = false;
      if (configuration != null)
      {
         Object o = configuration.get(Remoting.USE_ALL_SOCKET_FACTORY_PARAMS);
         if (o != null)
         {
            if (o instanceof String)
            {
               result = Boolean.valueOf((String) o).booleanValue();
            }
            else
            {
               log.warn("Value of " + Remoting.USE_ALL_SOCKET_FACTORY_PARAMS + " must be a String: " + o);
            }
         }
      }
      return result;
   }
}
