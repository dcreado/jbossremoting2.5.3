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

import java.io.IOException;
import java.util.Map;

/**
 * @author <a href="mailto:telrod@e2technologies.net">Tom Elrod</a>
 */
public interface ServerInvokerMBean
{
   /**
    * Will get the data type for the marshaller factory so know which marshaller to
    * get to marshal the data.  Will first check the locator uri for a 'datatype'
    * parameter and take that value if it exists.  Otherwise, will use the
    * default datatype for the client invoker, based on transport.
    *
    * @return
    */
   String getDataType();

   /**
    * returns true if the transport is bi-directional in nature, for example,
    * SOAP in unidirectional and SOCKETs are bi-directional (unless behind a firewall
    * for example).
    *
    * @return
    */
   boolean isTransportBiDirectional();

   void create();

   /**
    * subclasses should override to provide any specific start logic
    *
    * @throws java.io.IOException
    */
   void start() throws IOException;

   /**
    * return true if the server invoker is started, false if not
    *
    * @return
    */
   boolean isStarted();

   /**
    * subclasses should override to provide any specific stop logic
    */
   void stop();

   /**
    * destory the invoker permanently
    */
   void destroy();

   /**
    * Sets the server invoker's transport specific configuration.  Will need to set before calling
    * start() method (or at least stop() and start() again) before configurations will take affect.
    *
    * @param configuration
    */
   void setConfiguration(Map configuration);

   /**
    * Gets teh server invoker's transport specific configuration.
    *
    * @return
    */
   Map getConfiguration();

   /**
    * @jmx:managed-attribute
    */
   String getClientConnectAddress();

   int getClientConnectPort();

   /**
    * This method should only be called by the service controller when this invoker is
    * specified within the Connector configuration of a service xml.  Calling this directly
    * will have no effect, as will be used in building the locator uri that is published
    * for detection and this happens when the invoker is first created and started (after that, no one
    * will be aware of a change).
    *
    * @jmx:managed-attribute
    */
   void setClientConnectAddress(String clientConnectAddress);

   String getServerBindAddress();

   int getServerBindPort();

   void setClientConnectPort(int clientConnectPort);

   void setTimeout(int timeout);

   int getTimeout();
}
