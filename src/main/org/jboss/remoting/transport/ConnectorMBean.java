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

package org.jboss.remoting.transport;

import org.jboss.remoting.ConnectionListener;

/**
 * MBean interface.
 */
public interface ConnectorMBean
{

   /**
    * Starts the connector.
    */
   void start() throws java.lang.Exception;

   /**
    * Starts the connector.
    *
    * @param runAsNewThread indicates if should be started on new thread or the current one
    * @throws java.lang.Exception
    */
   void start(boolean runAsNewThread) throws java.lang.Exception;

   /**
    * Stops the connector.
    */
   void stop();

   /**
    * Creates the connector.
    */
   void create() throws java.lang.Exception;

   /**
    * Destroys the connector.
    */
   void destroy();

   /**
    * Returns the locator to the connector. Locator is the actual InvokerLocator object used to identify and get the ServerInvoker we are wrapping.
    */
   org.jboss.remoting.InvokerLocator getLocator();

   /**
    * Sets the invoker locator. InvokerLocator is the string URI representation of the InvokerLocator used to get and identify the ServerInvoker we are wrapping.
    */
   void setInvokerLocator(java.lang.String locator) throws java.lang.Exception;

   /**
    * Returns the invoker locator. InvokerLocator is the string URI representation of the InvokerLocator used to get and identify the ServerInvoker we are wrapping.
    */
   java.lang.String getInvokerLocator() throws java.lang.Exception;

   /**
    * Configuration is an xml element indicating subsystems to be registered with the ServerInvoker we wrap. Using mbean subsystems that call registerSubsystem is more flexible.
    */
   void setConfiguration(org.w3c.dom.Element xml) throws java.lang.Exception;

   /**
    * Configuration is an xml element indicating subsystems to be registered with the ServerInvoker we wrap. Using mbean subsystems that call registerSubsystem is more flexible.
    */
   org.w3c.dom.Element getConfiguration();

   /**
    * Adds a handler to the connector via OjbectName. This will create a mbean proxy of type of ServerInvocationHandler for the MBean specified by object name passed (so has to implement ServerInvocationHandler interface).
    *
    * @param subsystem
    * @param handlerObjectName
    * @return The previous ServerInvocationHandler with the same subsystem value (case insensitive), if one existed.  Otherwise will return null.
    * @throws Exception
    */
   org.jboss.remoting.ServerInvocationHandler addInvocationHandler(java.lang.String subsystem, javax.management.ObjectName handlerObjectName) throws java.lang.Exception;

   /**
    * Adds an invocation handler for the named subsystem to the invoker we manage, and sets the mbean server on the invocation handler.
    * Will return previous ServerInvocationHandler with same subsystem value (case insensitive), if one existed.  Otherwise will return null.
    */
   org.jboss.remoting.ServerInvocationHandler addInvocationHandler(java.lang.String subsystem, org.jboss.remoting.ServerInvocationHandler handler) throws java.lang.Exception;

   /**
    * Removes an invocation handler for the supplied subsystem from the invoker we manage, and unsets the MBeanServer on the handler.
    */
   void removeInvocationHandler(java.lang.String subsystem) throws java.lang.Exception;

   /**
    * Adds a connection listener to receive notification when a client connection
    * is lost or disconnected.  Will only be triggered for notifications when
    * leasing is turned on (via the lease period attribute being set to > 0).
    * @param listener
    *
    * @jmx.managed-operation description = "Add a connection listener to call when detect that a client has
    * failed or disconnected."
    * impact      = "ACTION"
    * @jmx.managed-parameter name        = "listener"
    * type        = "org.jboss.remoting.ConnectionListener"
    * description = "The connection listener to register"
    */
   void addConnectionListener(ConnectionListener listener);

   /**
    * Removes connection listener from receiving client connection lost/disconnected
    * notifications.
    * @param listener
    *
    * @jmx.managed-operation description = "Remove a client connection listener."
    * impact      = "ACTION"
    * @jmx.managed-parameter name        = "listener"
    * type        = "org.jboss.remoting.ConnectionListener"
    * description = "The client connection listener to remove."
    */
   void removeConnectionListener(ConnectionListener listener);

   /**
    * Sets the lease period for client connections.
    * Value is in milliseconds.
    * @param leasePeriodValue
    *
    * @jmx.managed-attribute description = "The number of milliseconds that should be used
    * when establishing the client lease period (meaning client will need to update its lease
    * within this amount of time or will be considered dead)."
    * access     = "read-write"
    */
   void setLeasePeriod(long leasePeriodValue);

   /**
    * Gets the lease period for client connections.
    * Value in milliseconds.
    * @return
    *
    * @jmx.managed-attribute
    */
   long getLeasePeriod();
}
