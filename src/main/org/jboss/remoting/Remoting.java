/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and individual contributors as indicated
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


public class Remoting
{
   private Remoting() {}
   

   /**
    * Key for the configuration map passed to a Connector to indicate the server
    * socket factory to be used.  This will override the creation of any other socket factory.
    */
   public static final String CUSTOM_SERVER_SOCKET_FACTORY = "customServerSocketFactory";
   

   /**
    * Key for the configuration map passed to a Client to indicate the socket factory to
    * be used.  This will override the creation of any other socket factory.
    */
   public static final String CUSTOM_SOCKET_FACTORY = "customSocketFactory";
   
   /**
    * Key for the configuration map passed to a Client to indicate the classname of
    * the socket factory to be used.
    */
   public static final String SOCKET_FACTORY_NAME = "socketFactory";
   
   /**
    * Key for the configuration map passed to a Client to indicate the classname of
    * the socket factory to be used.  This one is distinct from the bean property "socketFactory".
    */
   public static final String SOCKET_FACTORY_CLASS_NAME = "socketFactoryClassName";
   
   /**
    * Key for the configuration map passed to a Client or Connector to indicate
    * a socket creation listener for sockets created by a SocketFactory.
    */
   public static final String SOCKET_CREATION_CLIENT_LISTENER = "socketCreationClientListener";
   
   /**
    * Key for the configuration map passed to a Client or Connector to indicate
    * a socket creation listener for sockets created by a ServerSocket.
    */
   public static final String SOCKET_CREATION_SERVER_LISTENER = "socketCreationServerListener";

   /**
    * Key with which host address of client will be associated in InvocationRequest
    * request payload.
    */
   public static final String CLIENT_ADDRESS = "clientAddress";
   
   /**
    * Key for configuring Remoting wire version.
    */
   public static final String REMOTING_VERSION = "remotingVersion";
   
   /**
    * Key for telling Remoting to execute security sensitive code outside of
    * java.security.AccessController.doPrivileged() calls.
    */
   public static final String SKIP_ACCESS_CONTROL = "skipAccessControl";

   /**
    * A flag indicating whether the RemotingClassLoader uses parent first (=true)
    * or user class loader first delegation.
    */
   public static final String CLASSLOADING_PARENT_FIRST_DELEGATION = "classloadingParentFirstDelegation";
   public static final String CLASSLOADING_PARENT_FIRST_DELEGATION_PROP = "org.jboss.remoting.classloadingParentFirstDelegation";

   /**
    * Key for injecting into Connector a list of classloaders for remote
    * classloading facility.
    */
   public static final String REMOTE_CLASS_LOADERS = "remoteClassLoaders";
   
   /**
    * A flag indicating whether org.jboss.remoting.MicroRemoteClientInvoker should translate an 
    * org.jboss.remoting.ServerInvoker.InvalidStateException to an org.jboss.remoting.CannotConnectException.
    */
   public static final String CHANGE_INVALID_STATE_TO_CANNOT_CONNECT = "changeInvalidStateToCannotConnect";
   
   /**
    * A flag indicating that AbstractInvoker should give priority to values in InvokerLocator over
    * values in configuration map.
    */
   public static final String CONFIG_OVERRIDES_LOCATOR = "configOverridesLocator";
   
   /**
    * A flag indicating that RemoteClientInvoker should use parameters in the InvokerLocator as
    * well as the configuration map when creating a SocketFactory.
    */
   public static final String USE_ALL_SOCKET_FACTORY_PARAMS = "useAllSocketFactoryParams";

   /**
    * Flags indicating that connection monitoring should treat a connection as being defined
    * by one or two of its endpoints.  I.e., if a [client invoker or] server invoker stops and restarts, then
    * all connections it participated in are now gone.
    */
   public static final String USE_CLIENT_CONNECTION_IDENTITY = "useClientConnectionIdentity";
//   public static final String USE_SERVER_CONNECTION_IDENTITY = "useServerConnectionIdentity";

   /**
    * A flag for indicating that the Client configuration map should be used to configure
    * marshallers and unmarshallers.  If set to false (the default value), then parameters
    * will be taken only from the InvokerLocator.
    */
   public static final String PASS_CONFIG_MAP_TO_MARSHAL_FACTORY = "passConfigMapToMarshalFactory";
}

