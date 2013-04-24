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

package org.jboss.remoting.transport.multiplex;

import java.util.Map;

import org.jboss.logging.Logger;


/**
 * <code>Multiplex</code> is the repository for the names and default values for all of the
 * configurable parameters in the Multiplex system.  The names are meant to
 * be used as keys in a configuration <code>Map</code>.
 * The <code>Multiplex</code> class also contains
 * some keys used internally by the Multiplex classes. 
 * <p>
 * For a discussion of the meaning of these parameters, please see the Multiplex
 * documentation on the labs.jboss.org web site.
 * <p>
 * @author <a href="mailto:ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision$
 * <p>
 * Copyright (c) May 19, 2006
 * </p>
 * @deprecated As of release 2.4.0 the multiplex transport will no longer be actively supported.
 */
public class Multiplex
{
   private static Logger log = Logger.getLogger(Multiplex.class);
   private Multiplex() {}

   
   /////////////////////////////////////////////////////////////////////////////////////
   ////////////////                 invoker parameters                 /////////////////
   /////////////////////////////////////////////////////////////////////////////////////
   // configuration map keys:
   public static final String SERVER_MULTIPLEX_ID    = "serverMultiplexId";
   public static final String CLIENT_MULTIPLEX_ID    = "clientMultiplexId";
   public static final String MULTIPLEX_CONNECT_PORT = "multiplexConnectPort";
   public static final String MULTIPLEX_CONNECT_HOST = "multiplexConnectHost";
   public static final String MULTIPLEX_BIND_HOST    = "multiplexBindHost";
   public static final String MULTIPLEX_BIND_PORT    = "multiplexBindPort";
   public static final String MAX_ACCEPT_ERRORS      = "multiplex.maxAcceptErrors";
   
   // defaults:
   public static final int MAX_ACCEPT_ERRORS_DEFAULT = 10;
   
   
   
   /////////////////////////////////////////////////////////////////////////////////////
   ////////////////          MultiplexingManager parameters            /////////////////
   /////////////////////////////////////////////////////////////////////////////////////
   // configuration map keys:
   public static final String STATIC_THREADS_MONITOR_PERIOD = "multiplex.staticThreadsMonitorPeriod";
   public static final String SHUTDOWN_REQUEST_TIMEOUT = "multiplex.shutdownRequestTimeout";
   public static final String SHUTDOWN_REFUSALS_MAXIMUM = "multiplex.shutdownRefusalsMaximum";
   public static final String SHUTDOWN_MONITOR_PERIOD = "multiplex.shutdownMonitorPeriod";
   
   // defaults:
   public static final int STATIC_THREADS_MONITOR_PERIOD_DEFAULT = 5000;
   public static final int SHUTDOWN_REQUEST_TIMEOUT_DEFAULT = 5000;
   public static final int SHUTDOWN_REFUSALS_MAXIMUM_DEFAULT = 5;
   public static final int SHUTDOWN_MONITOR_PERIOD_DEFAULT = 1000;
   
   
   
   /////////////////////////////////////////////////////////////////////////////////////
   ////////////////             InputMultiplexor parameters            /////////////////
   /////////////////////////////////////////////////////////////////////////////////////
   // configuration map keys:
   public static final String INPUT_BUFFER_SIZE = "multiplex.inputBufferSize";
   public static final String INPUT_MAX_ERRORS = "multiplex.inputMaxErrors";
   
   // defaults:
   public static final int INPUT_BUFFER_SIZE_DEFAULT = 4096;
   public static final int INPUT_MAX_ERRORS_DEFAULT = 3;
   
   
   
   /////////////////////////////////////////////////////////////////////////////////////
   ////////////////            OutputMultiplexor parameters            /////////////////
   /////////////////////////////////////////////////////////////////////////////////////
   // configuration map keys:
   public static final String OUTPUT_MESSAGE_POOL_SIZE = "multiplex.outputMessagePoolSize";
   public static final String OUTPUT_MESSAGE_SIZE = "multiplex.outputMessageSize";
   public static final String OUTPUT_MAX_CHUNK_SIZE = "multiplex.outputMaxChunkSize";
   public static final String OUTPUT_MAX_TIME_SLICE = "multiplex.outputMaxTimeSlice";
   public static final String OUTPUT_MAX_DATA_SLICE = "multiplex.outputMaxDataSlice";
   public static final String OUTPUT_MAX_ERRORS = "multiplex.outputMaxErrors";
   
   // defaults:
   public static final int OUTPUT_MESSAGE_POOL_SIZE_DEFAULT = 1024;
   public static final int OUTPUT_MESSAGE_SIZE_DEFAULT = 256;
   public static final int OUTPUT_MAX_CHUNK_SIZE_DEFAULT = 2048;
   public static final int OUTPUT_MAX_TIME_SLICE_DEFAULT = 500;
   public static final int OUTPUT_MAX_DATA_SLICE_DEFAULT = OUTPUT_MAX_CHUNK_SIZE_DEFAULT * 8;
   public static final int OUTPUT_MAX_ERRORS_DEFAULT = 3;

      
   
   /////////////////////////////////////////////////////////////////////////////////////
   ////////////////                 Internal parameters                /////////////////
   /////////////////////////////////////////////////////////////////////////////////////
   public final static String SOCKET_FACTORY = "multiplex.SocketFactory";
   public final static String SERVER_SOCKET_FACTORY = "multiplex.ServerSocketFactory";
   public final static String SSL_HANDSHAKE_LISTENER = "multiplex.SSLHandshakeListener";
  
      
   
   /////////////////////////////////////////////////////////////////////////////////////
   ////////////////                  Utility methods                   /////////////////
   /////////////////////////////////////////////////////////////////////////////////////
   /**
    * Extracts a parameter value from a configuration <code>Map</code>. 
    * @param configuration configuration <code>Map</code>
    * @param name display form of parameter name
    * @param key key in configuration <code>Map</code>
    * @param defaultValue value to use of key is not among configuration keys
    * @return value in configuration association with key, or defautValue if key is not among
    *         configuration keys
    */
   public static int getOneParameter(Map configuration, String name, String key, int defaultValue)
   {
      if (configuration.containsKey(key))
      {
         Object obj = configuration.get(key);
         if (obj instanceof Integer)
         {
            return ((Integer) obj).intValue();
         }
         else if (obj instanceof String)
         {
            try
            {
               return Integer.parseInt((String) obj);
            }
            catch (NumberFormatException e)
            {
               log.error("invalid value for " + name + ": using default: " + defaultValue);
               return defaultValue;
            }
         }
         else
         {
            log.error("invalid value for " + name + ": using default: " + defaultValue);
            return defaultValue;
         }
      }
      else
         return defaultValue;
   }
}
