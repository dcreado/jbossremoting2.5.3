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
package org.jboss.remoting.transport.bisocket;

/** 
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 3259 $
 * <p>
 * Copyright Nov 22, 2006
 * </p>
 */
public class Bisocket
{
   public static final String GET_SECONDARY_INVOKER_LOCATOR = "getSecondaryInvokerLocator";

   public static final String IS_CALLBACK_SERVER = "isCallbackServer";
   
   public static final byte PING                     = 1;
   public static final byte CREATE_CONTROL_SOCKET    = 2;
   public static final byte RECREATE_CONTROL_SOCKET  = 3;
   public static final byte CREATE_ORDINARY_SOCKET   = 4;
   
   /**
    * Configuration key and default value for frequency with which pings are sent
    * on a control connection.
    */
   public static final String PING_FREQUENCY = "pingFrequency";
   public static final int PING_FREQUENCY_DEFAULT = 5000;
   
   /**
    * Configuration key and default value for window within which a ping on a 
    * control connection must be receeived for the connection to be considered
    * alive.
    */
   public static final String PING_WINDOW_FACTOR = "pingWindowFactor";
   public static final int PING_WINDOW_FACTOR_DEFAULT = 2;
   
   /**
    * Configuration key and default value for number of retries
    * BisocketServerInvoker.ControlConnectionThread and 
    * BisocketServerInvoker.createControlConnection should attempt while creating
    * sockets.
    */
   public static final String MAX_RETRIES = "maxRetries";
   public static final int MAX_RETRIES_DEFAULT = 10;
   
   /**
    * Configuration key and default value for number of times a control connection
    * will be restarted.
    */
   public static final String MAX_CONTROL_CONNECTION_RESTARTS = "maxControlConnectionRestarts";
   public static final int MAX_CONTROL_CONNECTION_RESTARTS_DEFAULT = 10;
   
   
   /**
    * Configuration keys for secondary ServerSocket.
    */
   public static final String SECONDARY_BIND_PORT = "secondaryBindPort";
   public static final String SECONDARY_CONNECT_PORT = "secondaryConnectPort";
   public static final String SECONDARY_BIND_PORTS = "secondaryBindPorts";
   public static final String SECONDARY_CONNECT_PORTS = "secondaryConnectPorts";
}
